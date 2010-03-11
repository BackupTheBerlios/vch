package de.berlios.vch.parser.dmax;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.prefs.Preferences;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.htmlparser.tags.Div;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.Translate;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;
import org.osgi.service.log.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.berlios.vch.config.ConfigService;
import de.berlios.vch.http.client.HttpUtils;
import de.berlios.vch.i18n.Messages;
import de.berlios.vch.i18n.ResourceBundleLoader;
import de.berlios.vch.i18n.ResourceBundleProvider;
import de.berlios.vch.parser.HtmlParserUtils;
import de.berlios.vch.parser.IOverviewPage;
import de.berlios.vch.parser.IVideoPage;
import de.berlios.vch.parser.IWebPage;
import de.berlios.vch.parser.IWebParser;
import de.berlios.vch.parser.OverviewPage;
import de.berlios.vch.parser.VideoPage;
import de.berlios.vch.parser.WebPageTitleComparator;
import de.berlios.vch.parser.dmax.pages.EpisodePage;
import de.berlios.vch.parser.dmax.pages.ProgramListing;
import de.berlios.vch.parser.dmax.pages.RootPage;
import de.berlios.vch.web.TemplateLoader;
import de.berlios.vch.web.menu.IWebMenuEntry;
import de.berlios.vch.web.menu.WebMenuEntry;

@Component
@Provides
public class DmaxParser implements IWebParser, ResourceBundleProvider {

    private static transient Logger logger = LoggerFactory.getLogger(DmaxParser.class);

    final static String CHARSET = "utf-8";

    final static String BASE_URI = "http://www.dmax.de";
    
    public static final String ID = DmaxParser.class.getName();

    private ProgramParser programParser = new ProgramParser();
    private VideoPageParser videoParser = new VideoPageParser();
    
    private BundleContext ctx;
    
    private ResourceBundle resourceBundle;
    
    @Requires
    private Messages i18n;
    
    @Requires
    private LogService log;
    
    @Requires 
    private ConfigService config;
    private Preferences prefs;
    
    @Requires
    private HttpService http;
    
    @Requires
    private TemplateLoader templateLoader;
    
    private ServiceRegistration menuReg;
    
    public DmaxParser(BundleContext ctx) {
        this.ctx = ctx;
    }
    
    @Override
    public IOverviewPage getRoot() throws Exception {
        IOverviewPage overview = new RootPage();
        overview.setUri(new URI("vchpage://" + getId()));
        overview.setTitle(getTitle());
        
        int maxVideos = prefs.getInt("max.videos", 400);
        String landingPage = BASE_URI + "/video/morevideo.shtml?name=longform&sort=date&contentSize=" + maxVideos
                + "&pageType=longFormHub&displayBlockName=popularLong";
        
        final Set<IWebPage> categories = new HashSet<IWebPage>();
        List<Thread> threads = new LinkedList<Thread>();
        for (int i = 1; i <= maxVideos / 20; i++) {
            final String URI = landingPage + "&page=" + i;
            Thread t = new Thread() {
                public void run() {
                    try {
                        String content = HttpUtils.get(URI, null, CHARSET);
                        NodeList itemCells = HtmlParserUtils.getTags(content, CHARSET,
                                "div#vp-perpage-promolist div[class~=vp-promo-item]");
                        for (NodeIterator iterator = itemCells.elements(); iterator.hasMoreNodes();) {
                            final Div itemCell = (Div) iterator.nextNode();
                            String cellHtml = itemCell.toHtml();

                            // parse the page title
                            String title = Translate.decode(HtmlParserUtils.getText(cellHtml,
                                    DmaxParser.CHARSET, "a.vp-promo-title").trim());

                            OverviewPage page = new ProgramListing();
                            page.setUri(new URI(URI));
                            page.setTitle(title);
                            page.getUserData().put("itemCell", cellHtml);
                            page.setParser(ID);
                            categories.add(page);
                        }
                    } catch (Exception e) {
                        logger.error("Couldn't parse overview page", e);
                    }
                }
            };
            threads.add(t);
            t.start();
        }
        
        // wait for all threads to finish
        for (Thread thread : threads) {
            thread.join();
        }
        overview.getPages().addAll(categories);
        Collections.sort(overview.getPages(), new WebPageTitleComparator());
        return overview;
    }

    @Override
    public IWebPage parse(IWebPage page) throws Exception {
        String type = (String) page.getUserData().get("dmax.type");
        if(ProgramListing.class.getSimpleName().equals(type)) {
            return programParser.parse(page);
        } else if(EpisodePage.class.getSimpleName().equals(type)) {
            int episodeChunkCount = Integer.parseInt((String)page.getUserData().get("episodeChunkCount"));
            logger.info("Selected episode has {} videos", episodeChunkCount);
            String videoPageUri = page.getUri().toString();
            String videoPageUriTemplate = videoPageUri.substring(0, videoPageUri.lastIndexOf('-')+1);
            IOverviewPage opage = (IOverviewPage) page;
            for (int i = 1; i <= episodeChunkCount ; i++) {
                VideoPage videoPage = new VideoPage();
                videoPage.setParser(ID);
                String title = page.getTitle();
                title += " " + i;
                videoPage.setTitle(title);
                videoPage.setUri(new URI(videoPageUriTemplate + i + '/'));
                opage.getPages().add(videoPage);
            }
            return page;
        } else if(page instanceof IVideoPage) {
            return videoParser.parse((VideoPage) page);
        }

        throw new Exception("Not yet implemented!");
    }

    @Override
    public String getTitle() {
        return "DMAX Videotheke";
    }

    @Validate
    public void start() throws Exception {
        prefs = config.getUserPreferences(ctx.getBundle().getSymbolicName());
        
        // register the config servlet
        registerServlet();
    }
    
    @Invalidate
    public void stop() {
        prefs = null;
        
        // unregister the config servlet
        if(http != null) {
            http.unregister(ConfigServlet.PATH);
        }
        
        // unregister the web menu
        if(menuReg != null) {
            menuReg.unregister();
        }
    }
    
    private void registerServlet() {
        ConfigServlet servlet = new ConfigServlet(prefs);
        servlet.setLogger(log);
        servlet.setBundleContext(ctx);
        servlet.setMessages(i18n);
        servlet.setTemplateLoader(templateLoader);
        try {
            // register the servlet
            http.registerServlet(ConfigServlet.PATH, servlet, null, null);
            
            // register web interface menu
            IWebMenuEntry menu = new WebMenuEntry("Parser");
            menu.setLinkUri("#");
            SortedSet<IWebMenuEntry> childs = new TreeSet<IWebMenuEntry>();
            IWebMenuEntry entry = new WebMenuEntry();
            entry.setTitle(getTitle());
            entry.setLinkUri("/parser?id=" + getClass().getName());
            childs.add(entry);
            menu.setChilds(childs);
            IWebMenuEntry config = new WebMenuEntry();
            config.setTitle(i18n.translate("I18N_CONFIGURATION"));
            config.setLinkUri(ConfigServlet.PATH);
            childs = new TreeSet<IWebMenuEntry>();
            childs.add(config);
            entry.setChilds(childs);
            menuReg = ctx.registerService(IWebMenuEntry.class.getName(), menu, null);
        } catch (Exception e) {
            log.log(LogService.LOG_ERROR, "Couldn't register "+getTitle()+" config servlet", e);
        }
    }

    @Override
    public String getId() {
        return ID;
    }
    
    @Override
    public ResourceBundle getResourceBundle() {
        if(resourceBundle == null) {
            try {
                log.log(LogService.LOG_DEBUG, "Loading resource bundle for " + getClass().getSimpleName());
                resourceBundle = ResourceBundleLoader.load(ctx, Locale.getDefault());
            } catch (IOException e) {
                log.log(LogService.LOG_ERROR, "Couldn't load resource bundle", e);
            }
        }
        return resourceBundle;
    }
}
