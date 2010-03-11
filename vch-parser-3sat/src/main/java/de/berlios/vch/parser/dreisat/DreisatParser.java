package de.berlios.vch.parser.dreisat;

import java.net.URI;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.htmlparser.Node;
import org.htmlparser.tags.ImageTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.Translate;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.sun.syndication.feed.synd.SyndEnclosure;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;

import de.berlios.vch.http.client.HttpUtils;
import de.berlios.vch.parser.AsxParser;
import de.berlios.vch.parser.HtmlParserUtils;
import de.berlios.vch.parser.IOverviewPage;
import de.berlios.vch.parser.IWebPage;
import de.berlios.vch.parser.IWebParser;
import de.berlios.vch.parser.OverviewPage;
import de.berlios.vch.parser.VideoPage;
import de.berlios.vch.parser.WebPageTitleComparator;

public class DreisatParser implements IWebParser, BundleActivator {

    public static final String BASE_URL = "http://www.3sat.de";
    
    public static final String MEDIATHEK_URL = BASE_URL + "/mediathek/";
    
    private static final String LANDING_PAGE = MEDIATHEK_URL + "mediathek.php?mode=rss";
    
    public static final String CHARSET = "iso-8859-15";
    
    public static final String ID = DreisatParser.class.getName();
    
    public static Map<String,String> HTTP_HEADERS = new HashMap<String, String>();
    static {
        HTTP_HEADERS.put("User-Agent", "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.1.2) Gecko/20090821 Gentoo Firefox/3.5.2");
        HTTP_HEADERS.put("Accept-Language", "de-de,de;q=0.8,en-us;q=0.5,en;q=0.3");
    }
    
    public DreisatFeedParser feedParser = new DreisatFeedParser();

    @Override
    public IOverviewPage getRoot() throws Exception {
        OverviewPage page = new OverviewPage();
        page.setParser(ID);
        page.setTitle(getTitle());
        page.setUri(new URI("vchpage://localhost/" + getId()));
        
        // add all rss feeds to 
        String landingPage = HttpUtils.get(LANDING_PAGE, HTTP_HEADERS, CHARSET);
        NodeList links = HtmlParserUtils.getTags(landingPage, CHARSET, "div.rss a.link");
        NodeIterator iter = links.elements();
        while(iter.hasMoreNodes()) {
            Node child = iter.nextNode();
            if(child instanceof LinkTag) {
                LinkTag link = (LinkTag) child;
                String pageUri = MEDIATHEK_URL + link.extractLink();
                String title = Translate.decode(link.getLinkText()).trim().substring(1);
                OverviewPage feedPage = new OverviewPage();
                feedPage.setParser(ID);
                feedPage.setTitle(title);
                feedPage.setUri(new URI(pageUri.replaceAll(" ", "+")));
                page.getPages().add(feedPage);
            }
        }
        
        // add the general 3sat mediathek feed
        OverviewPage feedPage = new OverviewPage();
        feedPage.setParser(ID);
        feedPage.setTitle("3sat-Mediathek allgemein");
        feedPage.setUri(new URI(MEDIATHEK_URL + "rss/mediathek.xml"));
        page.getPages().add(feedPage);
        Collections.sort(page.getPages(), new WebPageTitleComparator());
        return page;
    }

    @Override
    public String getTitle() {
        return "3sat Mediathek";
    }

    @Override
    public IWebPage parse(IWebPage page) throws Exception {
        if(page instanceof VideoPage) {
            VideoPage vpage = (VideoPage) page;
            if (vpage.getVideoUri().toString().endsWith(".asx")) {
                String uri = AsxParser.getUri(vpage.getVideoUri().toString());
                vpage.setVideoUri(new URI(uri));
                page.getUserData().remove("video");
            }
            
            String content = HttpUtils.get(vpage.getUri().toString(), null, CHARSET);
            ImageTag img = (ImageTag) HtmlParserUtils.getTag(content, CHARSET, "div.seite div.media img.still");
            if(img != null) {
                vpage.setThumbnail(new URI(BASE_URL + img.getImageURL()));
            }
            
            return page;
        } else {
            SyndFeed feed = feedParser.parse(page);
            OverviewPage feedPage = new OverviewPage();
            feedPage.setParser(ID);
            feedPage.setTitle(page.getTitle());
            feedPage.setUri(page.getUri());
            
            for (Iterator<?> iterator = feed.getEntries().iterator(); iterator.hasNext();) {
                SyndEntry entry = (SyndEntry) iterator.next();
                VideoPage video = new VideoPage();
                video.setParser(ID);
                video.setTitle(entry.getTitle());
                if(entry.getDescription() != null) {
                	video.setDescription(entry.getDescription().getValue());
                }
                video.setUri(new URI(entry.getLink()));
                Calendar pubCal = Calendar.getInstance();
                pubCal.setTime(entry.getPublishedDate());
                video.setPublishDate(pubCal);
                video.setVideoUri( new URI( ((SyndEnclosure)entry.getEnclosures().get(0)).getUrl() ) );
                feedPage.getPages().add(video);
            }
            return feedPage;
        }
    }

    @Override
    public void start(BundleContext ctx) throws Exception {
        ctx.registerService(IWebParser.class.getName(), this, null);
    }

    @Override
    public void stop(BundleContext ctx) throws Exception {
    }
    
    @Override
    public String getId() {
        return ID;
    }
}