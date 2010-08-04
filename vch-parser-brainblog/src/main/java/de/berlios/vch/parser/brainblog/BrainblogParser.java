package de.berlios.vch.parser.brainblog;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.htmlparser.tags.ImageTag;
import org.htmlparser.util.Translate;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.syndication.feed.synd.SyndCategory;
import com.sun.syndication.feed.synd.SyndEnclosure;
import com.sun.syndication.feed.synd.SyndEnclosureImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

import de.berlios.vch.http.client.HttpUtils;
import de.berlios.vch.i18n.Messages;
import de.berlios.vch.i18n.ResourceBundleLoader;
import de.berlios.vch.i18n.ResourceBundleProvider;
import de.berlios.vch.parser.HtmlParserUtils;
import de.berlios.vch.parser.IOverviewPage;
import de.berlios.vch.parser.IWebPage;
import de.berlios.vch.parser.IWebParser;
import de.berlios.vch.parser.OverviewPage;
import de.berlios.vch.parser.VideoPage;

@Component
@Provides
public class BrainblogParser implements IWebParser, ResourceBundleProvider {
private static transient Logger logger = LoggerFactory.getLogger(BrainblogParser.class);
    
    public static final String CHARSET = "UTF-8";
    
    public static final String ID = BrainblogParser.class.getName();
    
    public static Map<String,String> HTTP_HEADERS = new HashMap<String, String>();
    static {
        HTTP_HEADERS.put("User-Agent", "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.1.2) Gecko/20090821 Gentoo Firefox/3.5.2");
        HTTP_HEADERS.put("Accept-Language", "de-de,de;q=0.8,en-us;q=0.5,en;q=0.3");
    }

    private ResourceBundle resourceBundle;
    
    private BundleContext ctx;
    
    public BrainblogParser(BundleContext ctx) {
        this.ctx = ctx;
    }
    
    @Requires
    private Messages i18n;
    
    @Requires
    private LogService log;
    
    @SuppressWarnings("unchecked")
    @Override
    public IOverviewPage getRoot() throws Exception {
        OverviewPage page = new OverviewPage();
        page.setParser(ID);
        page.setTitle(getTitle());
        page.setUri(new URI("vchpage://localhost/" + getId()));
        
        // Hole RSS xml aus den geparsten links
        URL feedUrl = new URL("http://www.brainblog.to/xml-rss2.php");
        SyndFeedInput input = new SyndFeedInput();

        // RSS in das SyndFeed Object Parsen
        SyndFeed feed = input.build(new XmlReader(feedUrl));
        
        //pub date
        Date date = new Date();
        feed.setPublishedDate(date);
        
        // Über den Feed loopen
        List<SyndEntry> items = feed.getEntries();
        Iterator<SyndEntry> i = items.iterator();

        // löschliste für nicht video inhalte
        List<SyndEntry> toRemove = new ArrayList<SyndEntry>();

        while (i.hasNext()) {
            SyndEntryImpl current = (SyndEntryImpl) i.next();
            SyndEnclosureImpl enc = new SyndEnclosureImpl();
            
            // only parse video pages
            boolean isVideo = false;
            for (Iterator<?> iterator = current.getCategories().iterator(); iterator.hasNext();) {
                SyndCategory cat = (SyndCategory) iterator.next();
                if ("videos".equalsIgnoreCase(cat.getName()) ) {
                    isVideo = true;
                }
            }
            
            if (!isVideo) {
                // wenn das item nicht in kategorie videos ist -> raus damit
                toRemove.add(current);
            }

            List<SyndEnclosure> enclist = current.getEnclosures();
            enclist.add(enc);
            current.setEnclosures(enclist);
        }
        // Alle nicht video inhalte löschen
        items.removeAll(toRemove);
        
        for (SyndEntry item : items) {
            VideoPage video = new VideoPage();
            video.setParser(ID);
            video.setTitle(item.getTitle().replaceAll("Videos: ", ""));
            video.setUri(new URI(item.getLink()));
            
            // description
            String desc = "";
            if(item.getDescription() != null) {
                desc = HtmlParserUtils.getText(item.getDescription().getValue(), CHARSET, "div");
                desc = Translate.decode(desc);
            }
            video.setDescription(desc);
            
            // publish date
            Calendar pubDate = Calendar.getInstance();
            pubDate.setTime(item.getPublishedDate());
            video.setPublishDate(pubDate);
            
            // thumb aus beschreibung holen und beschreibung löschen
            ImageTag img = (ImageTag) HtmlParserUtils.getTag(item.getDescription().getValue(), CHARSET, "img");
            if(img != null) {
                video.setThumbnail(new URI(img.getImageURL()));
            }
            
            page.getPages().add(video);
        }
        
        return page;
    }
    
    private String getBrainblogEnclosureLink(String itempage) {
        String medialink = new String();
        try {
            String pageContent = HttpUtils.get(itempage, null, CHARSET);
            Pattern p = Pattern.compile("so.addParam\\(\\s*\"flashvars\"\\s*,\\s*\"(.*)\"\\s*\\);");
            Matcher m = p.matcher(pageContent);
            if(m.find()) {
                Map<String, List<String>> params = HttpUtils.parseQuery(m.group(1));
                medialink = params.get("file").get(0);
                logger.info("Found video: {}", medialink);
            } else {
                logger.debug("No video found on page {}", itempage);
            }
        } catch (Exception e) {
            logger.error("Couldn't parse enclosure link", e);
        }
        return medialink;
    }

    @Override
    public String getTitle() {
        return "Brainblog";
    }

    @Override
    public IWebPage parse(IWebPage page) throws Exception {
        if(page instanceof VideoPage) {
            VideoPage vpage = (VideoPage) page;
            vpage.setVideoUri(new URI(getBrainblogEnclosureLink(vpage.getUri().toString())));
            if(vpage.getVideoUri() == null || vpage.getVideoUri().toString().isEmpty()) {
                vpage.setDescription(i18n.translate("no_video_available"));
            }
        }
        return page;
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