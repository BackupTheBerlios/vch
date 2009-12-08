package de.berlios.vch.parser.rss;

import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

import com.sun.syndication.feed.synd.SyndEnclosure;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;

import de.berlios.vch.config.ConfigService;
import de.berlios.vch.parser.IOverviewPage;
import de.berlios.vch.parser.IWebPage;
import de.berlios.vch.parser.IWebParser;
import de.berlios.vch.parser.OverviewPage;
import de.berlios.vch.parser.VideoPage;
import de.berlios.vch.rss.RssParser;

@Component
@Provides
public class RssFeedParser implements IWebParser {

    @Requires
    private LogService log;
    
    @Requires
    private ConfigService cs;
    
    private Preferences prefs;
    
    private BundleContext ctx;
    
    public RssFeedParser(BundleContext ctx) {
        this.ctx = ctx;
    }
    
    @Override
    public String getId() {
        return RssFeedParser.class.getName();
    }

    @Override
    public IOverviewPage getRoot() throws Exception {
        OverviewPage root = new OverviewPage();
        root.setParser(getId());
        root.setTitle("RSS Feeds");
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
        return "RSS Feed Parser";
    }

    @Override
    public IWebPage parse(IWebPage page) throws Exception {
        if(page instanceof VideoPage) {
            return page;
        } else {
            String feedUri = page.getUri().toString();
            log.log(LogService.LOG_INFO, "Parsing rss feed " + feedUri);
            //String rss = HttpUtils.get(feedUri, null, "UTF-8");
            SyndFeed feed = RssParser.parseUri(feedUri);
            feed.setLink(feedUri);
            feed.setTitle(page.getTitle());
            
            OverviewPage feedPage = new OverviewPage();
            feedPage.setParser(getId());
            feedPage.setTitle(page.getTitle());
            feedPage.setUri(page.getUri());
            for (Iterator<?> iterator = feed.getEntries().iterator(); iterator.hasNext();) {
                SyndEntry entry = (SyndEntry) iterator.next();
                VideoPage video = new VideoPage();
                video.setParser(getId());
                video.setTitle(entry.getTitle());
                video.setDescription(entry.getDescription().getValue());
                Calendar pubCal = Calendar.getInstance();
                pubCal.setTime(entry.getPublishedDate());
                video.setPublishDate(pubCal);
                video.setVideoUri( new URI( ((SyndEnclosure)entry.getEnclosures().get(0)).getUrl() ) );
                feedPage.getPages().add(video);
            }
            return feedPage;
        }
    }
    
    @Validate
    public void start() {
        prefs = cs.getUserPreferences(ctx.getBundle().getSymbolicName());
        
//        addFeed("CC2", "http://www.cczwei.de/rss_tvissues_all.php");
    }
    
    @Invalidate
    public void stop() {
        prefs = null;
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
            log.log(LogService.LOG_ERROR, "Couldn't load preferences", e);
        }
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
        feeds.remove(id);
    }
}
