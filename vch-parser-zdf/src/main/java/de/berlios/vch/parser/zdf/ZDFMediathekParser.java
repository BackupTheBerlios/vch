package de.berlios.vch.parser.zdf;

import java.net.URI;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.htmlparser.Node;
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

public class ZDFMediathekParser implements IWebParser, BundleActivator {
    private static final String RSS_OVERVIEW_PAGE = "http://www.zdf.de/ZDFmediathek/inhalt";
    
    public static final String CHARSET = "utf-8";
    
    public static final String ID = ZDFMediathekParser.class.getName();
    
    private ZdfFeedParser feedParser = new ZdfFeedParser();
    
    @Override
    public IOverviewPage getRoot() throws Exception {
        OverviewPage page = new OverviewPage();
        page.setParser(ID);
        page.setTitle(getTitle());
        
        // add all rss feeds to 
        String landingPage = HttpUtils.get(RSS_OVERVIEW_PAGE, null, CHARSET);
        NodeList links = HtmlParserUtils.getTags(landingPage, CHARSET, "li.channel");
        NodeIterator iter = links.elements();
        Set<IWebPage> pages = new HashSet<IWebPage>();
        while(iter.hasMoreNodes()) {
            Node child = iter.nextNode();
            LinkTag contentPageLink = (LinkTag) HtmlParserUtils.getTag(child.toHtml(), CHARSET, "a");
            LinkTag rssLink = (LinkTag) HtmlParserUtils.getTag(child.toHtml(), CHARSET, "a.rss");
            
            String pageUri = rssLink.extractLink();
            String title = Translate.decode(contentPageLink.getLinkText()).trim();
            OverviewPage feedPage = new OverviewPage();
            feedPage.setParser(ID);
            feedPage.setTitle(title);
            feedPage.setUri(new URI(pageUri.replaceAll(" ", "+")));
            pages.add(feedPage);
        }
        
        page.getPages().addAll(pages);
        Collections.sort(page.getPages(), new WebPageTitleComparator());
        return page;
    }

    @Override
    public String getTitle() {
        return "ZDFmediathek";
    }

    @Override
    public IWebPage parse(IWebPage page) throws Exception {
        
        if(page instanceof VideoPage) {
            VideoPage video = (VideoPage) page;
            String videoUri = video.getVideoUri().toString();
            if(videoUri.endsWith("asx")) {
                videoUri = AsxParser.getUri(videoUri);
                video.setVideoUri(new URI(videoUri));
                page.getUserData().remove("video");
            }
            
            // parse duration
            
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