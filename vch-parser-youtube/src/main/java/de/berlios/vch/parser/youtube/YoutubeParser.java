package de.berlios.vch.parser.youtube;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.SortedSet;
import java.util.StringTokenizer;
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
import org.jdom.Element;
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
import de.berlios.vch.rss.RssParser;
import de.berlios.vch.web.TemplateLoader;
import de.berlios.vch.web.menu.IWebMenuEntry;
import de.berlios.vch.web.menu.WebMenuEntry;

@Component
@Provides
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
        addUserFeeds(root);
        return root;
    }

    private void addUserFeeds(IOverviewPage root) throws Exception {
        String user = prefs.get("user", "");
        if(user.length() > 0) {
            IOverviewPage userPage = new OverviewPage();
            userPage.setTitle(getResourceBundle().getString("I18N_USER") + " " + user);
            userPage.setParser(getId());
            String urlEncodedUser = URLEncoder.encode(user, "UTF-8");
            userPage.setUri(new URI("youtube://user/" + urlEncodedUser));
            
            // add favorites
            IOverviewPage favorites = new OverviewPage();
            favorites.setParser(getId());
            favorites.setTitle(getResourceBundle().getString("I18N_FAVORITES"));
            favorites.setUri(new URI("http://gdata.youtube.com/feeds/base/users/"+urlEncodedUser+"/favorites?alt=rss"));
            userPage.getPages().add(favorites);
            
            // add playlists
            IOverviewPage playlists = new OverviewPage();
            playlists.setParser(getId());
            playlists.setTitle(getResourceBundle().getString("I18N_PLAYLISTS"));
            playlists.setUri(new URI("http://gdata.youtube.com/feeds/base/users/"+urlEncodedUser+"/playlists?alt=rss"));
            userPage.getPages().add(playlists);
            
            // add subscriptions
            IOverviewPage subscriptions = new OverviewPage();
            subscriptions.setParser(getId());
            subscriptions.setTitle(getResourceBundle().getString("I18N_SUBSCRIPTIONS"));
            userPage.getPages().add(subscriptions);
            subscriptions.setUri(new URI("http://gdata.youtube.com/feeds/api/users/"+urlEncodedUser+"/subscriptions?alt=rss"));

            // add uploads
            IOverviewPage uploads = new OverviewPage();
            uploads.setParser(getId());
            uploads.setTitle(getResourceBundle().getString("I18N_UPLOADS"));
            uploads.setUri(new URI("http://gdata.youtube.com/feeds/base/users/"+urlEncodedUser+"/uploads?alt=rss"));
            userPage.getPages().add(uploads);
            
            root.getPages().add(userPage);
        }
    }

    @Override
    public String getTitle() {
        return "Youtube";
    }

    @Override
    public IWebPage parse(IWebPage page) throws Exception {
        if(page instanceof IVideoPage) {
            return page;
        } else if (page instanceof IOverviewPage) {
            if("youtube".equals(page.getUri().getScheme())) {
                return page;
            } else if(page.getUri().getPath().endsWith("playlists") || page.getUri().getPath().endsWith("subscriptions")) {
                /* We first parse the page as a normal feed, but we will get
                 * an IOverviewPage with IVideoPage childs. But the playlists / subscriptions
                 * have children, too, so we have to transform the IVideoPages
                 * to IOverviewPages */
                IOverviewPage feedPage = parseFeed(page.getUri());
                feedPage.setTitle(page.getTitle());
                feedPage.setUri(page.getUri());
                videoToOverview(feedPage);
                
                // add new subscription videos, if it is the subscriptiosn page
                if(page.getUri().getPath().endsWith("subscriptions")) {
                    // add new subscriptions
                    IOverviewPage newSubscriptions = new OverviewPage();
                    newSubscriptions.setParser(getId());
                    newSubscriptions.setTitle(getResourceBundle().getString("I18N_NEW_SUBSCRIPTIONS"));
                    String user = prefs.get("user", "");
                    String urlEncodedUser = URLEncoder.encode(user, "UTF-8");
                    newSubscriptions.setUri(new URI("http://gdata.youtube.com/feeds/base/users/"+urlEncodedUser+"/newsubscriptionvideos?alt=rss"));
                    feedPage.getPages().add(0, newSubscriptions);
                }
                
                return feedPage;
            } else if(page.getUri().toString().contains("view_play_list")) {
                // the playlists feed contains URIs to the playlist web pages and not to another rss feed,
                // so we have to transform the web link to a rss link
                String query = page.getUri().getQuery();
                if(query != null) {
                    // replace the URI with the rss URI
                    Map<String, Object> params = parseQuery(query);
                    String playlistId = (String) params.get("p");
                    String uri = "http://gdata.youtube.com/feeds/api/playlists/" + playlistId;
                    page.setUri(new URI(uri));
                }
            } 
            
            logger.log(LogService.LOG_INFO, "Parsing youtube rss feed " + page.getUri());
            IOverviewPage feedPage = parseFeed(page.getUri());
            feedPage.setTitle(page.getTitle());
            feedPage.setUri(page.getUri());
            addRelatedVideosPage(feedPage);
            return feedPage;
        }
        
        return page;
    }
    
    private void addRelatedVideosPage(IOverviewPage feedPage) throws Exception {
        List<IWebPage> newPages = new ArrayList<IWebPage>();
        for (IWebPage page : feedPage.getPages()) {
            if(page instanceof IVideoPage) {
                // determine the video id
                Map<String, Object> params = parseQuery(page.getUri().getQuery());
                String v = (String) params.get("v");
                
                // create the overview page for the video and related videos
                IOverviewPage opage = new OverviewPage();
                opage.setParser(getId());
                opage.setTitle(page.getTitle());
                opage.setUri(new URI("youtube://video/"+v));
                newPages.add(opage);
                
                // add the video page
                opage.getPages().add(page);
                
                // add the related videos
                IOverviewPage related = new OverviewPage();
                related.setParser(getId());
                related.setTitle(getResourceBundle().getString("I18N_RELATED"));
                related.setUri(new URI("http://gdata.youtube.com/feeds/api/videos/"+v+"/related?alt=rss"));
                opage.getPages().add(related);
            } else {
                newPages.add(page);
            }
        }
        feedPage.getPages().clear();
        feedPage.getPages().addAll(newPages);
    }

    private void videoToOverview(IOverviewPage feedPage) throws Exception {
        List<IWebPage> newPages = new ArrayList<IWebPage>(feedPage.getPages().size());
        for (Iterator<IWebPage> iterator = feedPage.getPages().iterator(); iterator.hasNext();) {
            IVideoPage _playlist = (IVideoPage) iterator.next();
            IOverviewPage playlist = new OverviewPage();
            playlist.setParser(getId());
            playlist.setTitle(_playlist.getTitle());
            playlist.setUri(_playlist.getUri());
            newPages.add(playlist);
        }
        feedPage.getPages().clear();
        feedPage.getPages().addAll(newPages);
    }
    
    public IOverviewPage parseFeed(URI feedURI) throws IOException, ParserException, IllegalArgumentException, FeedException, URISyntaxException {
        // RSS in das SyndFeed Object Parsen
        SyndFeed feed = RssParser.parseUri(feedURI.toString());
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
            if (rawDescription != null) {
                if (rawDescription.startsWith("&lt;")) {
                    String desc = HtmlParserUtils.getText(rawDescription, "UTF-8", "div span");
                    video.setDescription(Translate.decode(desc));
                } else if (rawDescription.startsWith("<div")) {
                    String desc = HtmlParserUtils.getText(rawDescription, "UTF-8", "div span");
                    video.setDescription(desc);
                } else {
                    String desc = HtmlParserUtils.getText("<div>"+rawDescription+"</div>", "UTF-8", "div");
                    video.setDescription(desc);
                }
            }
            
            // parse publish date
            if(entry.getPublishedDate() != null) {
                Calendar pubCal = Calendar.getInstance();
                pubCal.setTime(entry.getPublishedDate());
                video.setPublishDate(pubCal);
            } else if(entry.getUpdatedDate() != null) {
                Calendar pubCal = Calendar.getInstance();
                pubCal.setTime(entry.getUpdatedDate());
                video.setPublishDate(pubCal);
            }
            
            // parse the thumbnail
            @SuppressWarnings("unchecked")
            List<Element> elements = (List<Element>) entry.getForeignMarkup();
            for (Element element : elements) {
                if("thumbnail".equals(element.getName())) {
                    video.setThumbnail(new URI(element.getText()));
                }
            }
            
            // parse uri
            video.setUri(new URI(entry.getLink()));
            
            // if we parse a subscription feed, we have to adjust the uri to point 
            // to the uploads page of yt:username
            if(entry.getTitle().startsWith("Activity of : ")) {
                @SuppressWarnings("unchecked")
                List<Element> fm = (List<Element>) entry.getForeignMarkup();
                for (Element element : fm) {
                    if("username".equals(element.getName()) && "yt".equals(element.getNamespacePrefix())) {
                        String username = element.getTextTrim().toLowerCase();
                        String urlEncodedUser = URLEncoder.encode(username, "UTF-8");
                        URI uri = new URI("http://gdata.youtube.com/feeds/base/users/"+urlEncodedUser+"/uploads?alt=rss");
                        video.setUri(uri);
                    }
                }
            }
            
            feedPage.getPages().add(video);
        }
        return feedPage;
    }
    
    @Validate
    public void start() {
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

    public Map<String, Object> parseQuery(String query) throws UnsupportedEncodingException {
        Map<String, Object> parameters = new HashMap<String, Object>();
        if(query != null) {
            StringTokenizer st = new StringTokenizer(query, "&");
            while (st.hasMoreTokens()) {
                String keyValue = st.nextToken();
                StringTokenizer st2 = new StringTokenizer(keyValue, "=");
                String key = null;
                String value = "";
                if (st2.hasMoreTokens()) {
                    key = st2.nextToken();
                    key = URLDecoder.decode(key, "UTF-8");
                }

                if (st2.hasMoreTokens()) {
                    value = st2.nextToken();
                    value = URLDecoder.decode(value, "UTF-8");
                }

                logger.log(LogService.LOG_DEBUG, "Found key value pair: " + key + "," + value);
                if(parameters.containsKey(key)) {
                    logger.log(LogService.LOG_DEBUG, "Key already exists. Assuming array of values. Will bes tored in a list");
                    Object o = parameters.get(key);
                    if(o instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<String> values = (List<String>) o;
                        values.add(value);
                    } else if(o instanceof String) {
                        List<String> values = new ArrayList<String>();
                        values.add((String)o);
                        values.add(value);
                        parameters.put(key, values);
                    }
                } else {
                    parameters.put(key, value);
                }
            }
        }
        return parameters;
    }
}
