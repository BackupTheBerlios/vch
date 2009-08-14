package de.berlios.vch.parser.youporn;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.htmlparser.Tag;
import org.htmlparser.tags.ImageTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEnclosure;
import com.sun.syndication.feed.synd.SyndEnclosureImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;
import com.sun.syndication.feed.synd.SyndImageImpl;

import de.berlios.vch.i18n.Messages;
import de.berlios.vch.model.Group;
import de.berlios.vch.parser.AbstractPageParser;
import de.berlios.vch.rome.videocast.Videocast;
import de.berlios.vch.rome.videocast.VideocastImpl;
import de.berlios.vch.utils.HtmlParserUtils;
import de.berlios.vch.utils.HttpUtils;

public class YoupornParser extends AbstractPageParser {

    private static transient Logger logger = LoggerFactory.getLogger(YoupornParser.class);
    
    private final String CHARSET = "UTF-8";
    
    private final String COOKIE = "__utma=60671397.1341618993.1198254651.1198254651.1198254651.1; __utmb=60671397; __utmc=60671397; __utmz=60671397.1198254651.1.1.utmccn=(direct)|utmcsr=(direct)|utmcmd=(none); age_check=1";
    
    private final String BASEURL = "http://www.youporn.com";
    
    @SuppressWarnings("unchecked")
    @Override
    public void run() {
        try {
            Map<String,String> headers = new HashMap<String, String>();
            headers.put("Cookie", COOKIE);
            String content = HttpUtils.get(BASEURL, headers, CHARSET);

            SyndFeed feed = new SyndFeedImpl();
            feed.setEncoding(CHARSET);
            feed.setFeedType("rss_2.0");
            feed.setTitle("Youporn RSS Feed");
            feed.setLink("www.youporn.com");
            feed.setDescription("Current Youporn.com entries");
            SyndImageImpl image = new SyndImageImpl();
            image.setUrl("http://files.youporn.com/images/logoblack.png");
            feed.setImage(image);
            
            //pub Date hinzufügen
            feed.setPublishedDate(new Date());
            
            List<SyndEntry> entries = new ArrayList<SyndEntry>();
            NodeList pageLinks = HtmlParserUtils.getTags(content, CHARSET, "div#video-listing ul li > a");
            for (NodeIterator iterator = pageLinks.elements(); iterator.hasMoreNodes();) {
                LinkTag link = (LinkTag)iterator.nextNode();
                try {
                    SyndEntry entry = new SyndEntryImpl();
    
                    // parse image
                    ImageTag img = (ImageTag) HtmlParserUtils.getTag(link.toHtml(), CHARSET, "img");
                    Videocast myvidcast = new VideocastImpl();
                    myvidcast.setImage(img.getImageURL());
                    entry.getModules().add(myvidcast);
                    
                    // get video page
                    // sometimes an empty page is returned, so we try to download
                    // the page at most 3 times
                    String pageUri = BASEURL + link.getLink();
                    logger.info("Parsing page " + pageUri);
                    content = "";
                    for (int i = 0; i < 3 && content.length() == 0; i++) {
                        content = HttpUtils.get(pageUri, headers, CHARSET);
                    }
                    
                    // continue with the next page, if the download didn't work
                    if(content.length() == 0) {
                        logger.warn("Couldn't download page {}", pageUri);
                        continue;
                    }
                    
                    // parse enclosure
                    LinkTag download = (LinkTag) HtmlParserUtils.getTag(content, CHARSET, "div#download a");
                    SyndEnclosure enclosure = new SyndEnclosureImpl();
                    enclosure.setUrl(download.getLink());
                    enclosure.setType("video/flv");
                    entry.getEnclosures().add(enclosure);
                    
                    // parse title
                    entry.setTitle(HtmlParserUtils.getText(content, CHARSET, "div#videoArea h1").trim());
                    
                    // parse description
                   String description = ((Tag)HtmlParserUtils.getTag(content, CHARSET, "div#details")).toPlainTextString().trim();
                   description = description.replaceAll("^\\s+", "");
                   SyndContent desc = new SyndContentImpl();
                   desc.setValue(description);
                   entry.setDescription(desc);
                   
                   // parse putdate
                   Locale currentLocale = Locale.getDefault();
                   try {
                       String d = description.substring(description.indexOf("Date:")+5).trim();
                       Locale.setDefault(Locale.ENGLISH);
                       SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy");
                       Date date = sdf.parse(d);
                       entry.setPublishedDate(date);
                   } catch(Exception e) {
                       e.printStackTrace();
                       entry.setPublishedDate(new Date());
                   } finally {
                       Locale.setDefault(currentLocale);
                   }
                    
                    entries.add(entry);
                } catch (Exception e) {
                    logger.error("Error while parsing page " + link.getLink(), e);
                }
            }
            feed.setEntries(entries);

            //saveFeed(feed);
            saveFeed2DB(feed);
            addRssFeed(feed.getLink());

            // add all feeds to Group
            Group youporn = new Group();
            youporn.setName(Messages.translate(getClass(), "group_name"));
            youporn.setDescription(Messages.translate(getClass(), "group_desc"));
            addFeedsToGroup(youporn);
        } catch (Exception e) {
            logger.error("Couldn't parse Youporn webpage",e);
        }

    }

}