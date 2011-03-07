package de.berlios.vch.parser.dmax;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.htmlparser.Node;
import org.htmlparser.Tag;
import org.htmlparser.tags.ImageTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.Translate;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

import de.berlios.vch.config.ConfigService;
import de.berlios.vch.http.client.HttpUtils;
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

@Component
@Provides
public class DmaxParser implements IWebParser, ResourceBundleProvider {

    final static String CHARSET = "utf-8";

    final static String BASE_URI = "http://www.dmax.de";
    
    public static final String ID = DmaxParser.class.getName();

    private VideoPageParser videoParser = new VideoPageParser();
    
    private BundleContext ctx;
    
    private ResourceBundle resourceBundle;
    
    @Requires
    private LogService logger;
    
    @Requires 
    private ConfigService config;
    private Preferences prefs;
    
    public DmaxParser(BundleContext ctx) {
        this.ctx = ctx;
    }
    
    @Override
    public IOverviewPage getRoot() throws Exception {
        IOverviewPage overview = new OverviewPage();
        overview.setUri(new URI("vchpage://localhost/" + getId()));
        overview.setTitle(getTitle());
        overview.setParser(ID);
        
        try {
            String content = HttpUtils.get(BASE_URI + "/video/shows/", null, CHARSET);
            NodeList itemCells = HtmlParserUtils.getTags(content, CHARSET, "div#video-allshows ol li a");
            for (NodeIterator iterator = itemCells.elements(); iterator.hasMoreNodes();) {
                LinkTag link = (LinkTag) iterator.nextNode();
                
                IOverviewPage programPage = new OverviewPage();
                programPage.setParser(getId());
                programPage.setTitle(Translate.decode(link.getLinkText()));
                programPage.setUri(new URI(BASE_URI + link.getLink()));
                overview.getPages().add(programPage);
                logger.log(LogService.LOG_DEBUG, "Added "+link.getLinkText()+" at " + link.getLink());
            }
        } catch (Exception e) {
            logger.log(LogService.LOG_ERROR, "Couldn't parse overview page", e);
        }
                
        Collections.sort(overview.getPages(), new WebPageTitleComparator());
        return overview;
    }

    @Override
    public IWebPage parse(IWebPage page) throws Exception {
        if(page instanceof IOverviewPage) {
            IOverviewPage opage = (IOverviewPage) page;
            String uri = opage.getUri().toString();
            String path = opage.getUri().getPath();
            if(countSlashes(path) == 4 && uri.contains("video/shows")) {
                opage.getPages().clear();
                return parseProgramPage(opage);
            } else if(uri.contains("/moreepisodes/?")) {
                logger.log(LogService.LOG_INFO, "Parsing videos on " + uri);
                opage.getPages().clear();
                parseEpisodesOverview(opage);
                return opage;
            } else if(uri.contains("/morevideo/?")) {
                logger.log(LogService.LOG_INFO, "Parsing videos on " + uri);
                opage.getPages().clear();
                parseVideoOverview(opage);
                return opage;
            } else if("dummy".equals(page.getUri().getScheme())) {
                return page;
            }
        } else if(page instanceof IVideoPage) {
            return videoParser.parse((VideoPage) page);
        }

        throw new Exception("Not yet implemented!");
    }

    private void parseEpisodesOverview(IOverviewPage opage) throws Exception {
        String content = HttpUtils.get(opage.getUri().toString(), null, CHARSET);
        NodeList items = HtmlParserUtils.getTags(content, CHARSET, "dl[class~=item]");
        NodeIterator iter = items.elements();
        while(iter.hasMoreNodes()) {
            Node node = iter.nextNode();
            String nodeHtml = node.toHtml();
            String title = HtmlParserUtils.getText(nodeHtml, CHARSET, "dd.description");
            LinkTag link = (LinkTag) HtmlParserUtils.getTag(nodeHtml, CHARSET, "dt.title a");
            String description = HtmlParserUtils.getText(nodeHtml, CHARSET, "dd.summary");
            String uri = BASE_URI + link.getLink();
            String baseuri = uri.substring(0, uri.lastIndexOf('-'));
            
            IOverviewPage episodesContainer = new OverviewPage();
            episodesContainer.setParser(getId());
            episodesContainer.setTitle(title.substring(0, title.lastIndexOf(' ')));
            episodesContainer.setUri(new URI(baseuri.replaceAll("http", "dummy"))); // dummy uri
            
            // determine the number of parts
            String parts = HtmlParserUtils.getText(nodeHtml, CHARSET, "dd.part");
            Matcher m = Pattern.compile("\\(Teil \\d von (\\d)\\)").matcher(parts);
            if(m.matches()) {
                int numberOfParts = Integer.parseInt(m.group(1));
                for (int i = 0; i < numberOfParts; i++) {
                    IVideoPage video = new VideoPage();
                    video.setParser(getId());
                    video.setTitle(episodesContainer.getTitle() + " " + (i+1));
                    video.setUri(new URI(baseuri + '-' + (i+1) + "/"));
                    video.setDescription(description);
                    ImageTag img = (ImageTag) HtmlParserUtils.getTag(nodeHtml, CHARSET, "dd.thumbnail img");
                    video.setThumbnail(new URI(img.extractImageLocn()));
                    episodesContainer.getPages().add(video);
                }
            } else {
                logger.log(LogService.LOG_WARNING, "Couldn't determine number of episode parts.");
                continue;
            }
            
            opage.getPages().add(episodesContainer);
        }
        logger.log(LogService.LOG_INFO, "Found " + items.size() + " items");
    }

    private void parseVideoOverview(IOverviewPage opage) throws Exception {
        String content = HttpUtils.get(opage.getUri().toString(), null, CHARSET);
        NodeList items = HtmlParserUtils.getTags(content, CHARSET, "dl[class~=item]");
        NodeIterator iter = items.elements();
        while(iter.hasMoreNodes()) {
            Node node = iter.nextNode();
            String nodeHtml = node.toHtml();
            String title = HtmlParserUtils.getText(nodeHtml, CHARSET, "dd.description");
            LinkTag link = (LinkTag) HtmlParserUtils.getTag(nodeHtml, CHARSET, "dt.title a");
            String description = HtmlParserUtils.getText(nodeHtml, CHARSET, "dd.summary");
            URI uri = new URI(BASE_URI + link.getLink());
            
            IVideoPage video = new VideoPage();
            video.setParser(getId());
            video.setTitle(title);
            video.setUri(uri);
            video.setDescription(description);
            ImageTag img = (ImageTag) HtmlParserUtils.getTag(nodeHtml, CHARSET, "dd.thumbnail img");
            video.setThumbnail(new URI(img.extractImageLocn()));
            opage.getPages().add(video);
        }
        logger.log(LogService.LOG_INFO, "Found " + items.size() + " items");
    }

    private IOverviewPage parseProgramPage(IOverviewPage opage) throws Exception {
        String content = HttpUtils.get(opage.getUri().toString(), null, CHARSET);
        Tag episodesDiv = HtmlParserUtils.getTag(content, CHARSET, "div#video-show-longform");
        if(episodesDiv != null) {
            LinkTag episodesLink = (LinkTag) HtmlParserUtils.getTag(episodesDiv.toHtml(), CHARSET, "a[class=section-more-link][title=Alle]");
            IOverviewPage episodes = new OverviewPage();
            episodes.setParser(getId());
            episodes.setTitle(getResourceBundle().getString("I18N_EPISODES"));
            String uri = HttpUtils.addParameter(BASE_URI + episodesLink.getLink(), "sort", "date");
            episodes.setUri(new URI(uri));
            opage.getPages().add(episodes);
        }
        
        Tag clipsDiv = HtmlParserUtils.getTag(content, CHARSET, "div#video-show-videos");
        if(clipsDiv != null) {
            LinkTag clipsLink = (LinkTag) HtmlParserUtils.getTag(clipsDiv.toHtml(), CHARSET, "a[class=section-more-link][title=Alle]");
            IOverviewPage clips = new OverviewPage();
            clips.setParser(getId());
            clips.setTitle(getResourceBundle().getString("I18N_CLIPS"));
            String uri = HttpUtils.addParameter(BASE_URI + clipsLink.getLink(), "sort", "date");
            clips.setUri(new URI(uri));
            opage.getPages().add(clips);
        }
        return opage;
    }

    @Override
    public String getTitle() {
        return "DMAX Videotheke";
    }

    @Validate
    public void start() throws Exception {
        prefs = config.getUserPreferences(ctx.getBundle().getSymbolicName());
        prefs.remove("max.videos");
    }
    
    @Invalidate
    public void stop() {
        prefs = null;
    }

    @Override
    public String getId() {
        return ID;
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
    
    private int countSlashes(String path) {
        int count = 0;
        int pos = 0;
        while( (pos = path.indexOf('/', pos)) >= 0) {
            count++;
            pos++;
        }
        return count;
    }
}
