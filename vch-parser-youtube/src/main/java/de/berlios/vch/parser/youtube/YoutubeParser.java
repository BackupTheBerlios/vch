package de.berlios.vch.parser.youtube;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.htmlparser.util.ParserException;
import org.htmlparser.util.Translate;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;
import org.osgi.service.log.LogService;

import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndImage;
import com.sun.syndication.feed.synd.SyndImageImpl;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

import de.berlios.vch.config.ConfigService;
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
import de.berlios.vch.web.TemplateLoader;
import de.berlios.vch.web.menu.IWebMenuEntry;
import de.berlios.vch.web.menu.WebMenuEntry;

@Component
@Provides(specifications= {IWebParser.class})
public class YoutubeParser implements IWebParser, ResourceBundleProvider {

    @Requires
    private ConfigService cs;
    
    private Preferences prefs;
    
    @Requires
    private HttpService http;
    
    @Requires
    private Messages i18n;
    
    @Requires
    private TemplateLoader templateLoader;
    
    @Requires
    private LogService logger;
    
    private BundleContext ctx;
    
    private ResourceBundle resourceBundle;
    
    private ServiceRegistration menuReg;
    
    public YoutubeParser(BundleContext ctx) {
        this.ctx = ctx;
    }
    
    @Override
    public String getId() {
        return YoutubeParser.class.getName();
    }

    @Override
    public IOverviewPage getRoot() throws Exception {
        OverviewPage root = new OverviewPage();
        root.setParser(getId());
        root.setTitle("Youtube");
        root.setUri(new URI("vchpage://localhost/" + getId()));
        for (Feed feed : getFeeds()) {
            OverviewPage page = new OverviewPage();
            page.setParser(getId());
            page.setTitle(feed.getTitle());
            page.setUri(new URI(feed.getUri()));
            root.getPages().add(page);
        }
        return root;
    }

    @Override
    public String getTitle() {
        return "Youtube";
    }

    @Override
    public IWebPage parse(IWebPage page) throws Exception {
        if(page instanceof IVideoPage) {
            return page;
        } else {
            logger.log(LogService.LOG_INFO, "Parsing youtube rss feed " + page.getUri());
            IOverviewPage feedPage = parseFeed(page.getUri());
            feedPage.setTitle(page.getTitle());
            feedPage.setUri(page.getUri());
            return feedPage;
        }
    }
    
    public IOverviewPage parseFeed(URI feedURI) throws IOException, ParserException, IllegalArgumentException, FeedException, URISyntaxException {
        // RSS in das SyndFeed Object Parsen
        XmlReader xmlReader = new XmlReader(feedURI.toURL());
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = input.build(xmlReader);
        feed.setEncoding(xmlReader.getEncoding());
        String title = feed.getTitle();
        feed.setTitle("Youtube - " + title);
        feed.setDescription(title);
        SyndImage image = new SyndImageImpl();
        image.setUrl("http://www.youtube.com/img/pic_youtubelogo_123x63.gif");
        feed.setImage(image);
        
        OverviewPage feedPage = new OverviewPage();
        feedPage.setParser(getId());
        for (Iterator<?> iterator = feed.getEntries().iterator(); iterator.hasNext();) {
            SyndEntry entry = (SyndEntry) iterator.next();
            VideoPage video = new YoutubeVideoPageProxy(logger, prefs);
            video.setParser(getId());
            video.setTitle(entry.getTitle());
            
            // parse description
            String rawDescription = null;
            if(entry.getDescription() != null) {
                rawDescription = entry.getDescription().getValue();
            } else if(entry.getContents().size() > 0) {
                rawDescription = ((SyndContent)entry.getContents().get(0)).getValue(); 
            }
            if(rawDescription != null) {
                String desc = HtmlParserUtils.getText(rawDescription, "UTF-8", "div span");
                video.setDescription(Translate.decode(desc));
            }
            
            // parse publish date
            Calendar pubCal = Calendar.getInstance();
            pubCal.setTime(entry.getPublishedDate());
            video.setPublishDate(pubCal);
            
            // parse video uri
            video.setUri(new URI(entry.getLink()));
            
            feedPage.getPages().add(video);
        }
        return feedPage;
    }
    
    @Validate
    public void start() {
        i18n.addProvider(this);
        prefs = cs.getUserPreferences(ctx.getBundle().getSymbolicName());
        registerServlet();
    }
    
    private void registerServlet() {
        ConfigServlet servlet = new ConfigServlet(this, prefs);
        servlet.setLogger(logger);
        servlet.setBundleContext(ctx);
        servlet.setMessages(i18n);
        servlet.setTemplateLoader(templateLoader);
        try {
            // register the servlet
            http.registerServlet(ConfigServlet.PATH, servlet, null, null);
            
            // register web interface menu
            IWebMenuEntry menu = new WebMenuEntry(getResourceBundle().getString("I18N_BROWSE"));
            menu.setPreferredPosition(Integer.MIN_VALUE);
            menu.setLinkUri("#");
            SortedSet<IWebMenuEntry> childs = new TreeSet<IWebMenuEntry>();
            IWebMenuEntry entry = new WebMenuEntry();
            entry.setTitle(getTitle());
            entry.setLinkUri("/parser?id=" + getClass().getName());
            childs.add(entry);
            menu.setChilds(childs);
            childs = new TreeSet<IWebMenuEntry>();
            IWebMenuEntry open = new WebMenuEntry();
            open.setTitle(getResourceBundle().getString("I18N_OPEN"));
            open.setLinkUri(entry.getLinkUri());
            childs.add(open);
            IWebMenuEntry config = new WebMenuEntry();
            config.setTitle(getResourceBundle().getString("I18N_CONFIGURATION"));
            config.setLinkUri(ConfigServlet.PATH);
            config.setPreferredPosition(Integer.MAX_VALUE);
            childs.add(config);
            entry.setChilds(childs);
            menuReg = ctx.registerService(IWebMenuEntry.class.getName(), menu, null);
        } catch (Exception e) {
            logger.log(LogService.LOG_ERROR, "Couldn't register youtube parser config servlet", e);
        }
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
        
        i18n.removeProvider(this);
    }
    
    public List<Feed> getFeeds() {
        List<Feed> feeds = new ArrayList<Feed>();
        try {
            Preferences persitentFeeds = prefs.node("feeds");
            String[] feedIds = persitentFeeds.childrenNames();
            for (String id : feedIds) {
                Preferences feed = persitentFeeds.node(id);
                String title = feed.get("title", "N/A");
                String uri = feed.get("uri", "");
                feeds.add(new Feed(id, title, uri));
            }
        } catch (BackingStoreException e) {
            logger.log(LogService.LOG_ERROR, "Couldn't load preferences", e);
        }
        Collections.sort(feeds);
        return feeds;
    }
    
    public void addFeed(String title, String uri) {
        Preferences feeds = prefs.node("feeds");
        String id = UUID.randomUUID().toString();
        Preferences feed = feeds.node(id);        
        feed.put("title", title);
        feed.put("uri", uri);
    }
    
    public void removeFeed(String id) {
        Preferences feeds = prefs.node("feeds");
        Preferences feed = feeds.node(id);     
        try {
            feed.removeNode();
        } catch (BackingStoreException e) {
            logger.log(LogService.LOG_ERROR, "Couldn't remove feed", e);
        }
    }

    @Override
    public ResourceBundle getResourceBundle() {
        if(resourceBundle == null) {
            try {
                logger.log(LogService.LOG_DEBUG, "Loading resource bundle for " + getClass().getSimpleName());
                resourceBundle = ResourceBundleLoader.load(ctx, Locale.getDefault());
            } catch (IOException e) {
                logger.log(LogService.LOG_ERROR, "Couldn't load resource bundle", e);
            }
        }
        return resourceBundle;
    }

//    @Override
//    public IOverviewPage search(String query) throws Exception {
//        String _uri = "http://gdata.youtube.com/feeds/base/videos?client=ytapi-youtube-search&alt=rss&v=2&q="
//            + URLEncoder.encode(query, "UTF-8");
//        URI uri = new URI(_uri);
//        IOverviewPage result = parseFeed(uri);
//        result.setUri(uri);
//        result.setTitle("Search results for \""+query+"\"");
//        return result;
//    }
}
