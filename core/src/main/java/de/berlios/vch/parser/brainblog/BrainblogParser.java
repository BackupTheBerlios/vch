package de.berlios.vch.parser.brainblog;

import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.tags.ImageTag;
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

import de.berlios.vch.http.filter.ParameterParser;
import de.berlios.vch.i18n.Messages;
import de.berlios.vch.model.Group;
import de.berlios.vch.parser.AbstractPageParser;
import de.berlios.vch.rome.videocast.Videocast;
import de.berlios.vch.rome.videocast.VideocastImpl;
import de.berlios.vch.utils.HtmlParserUtils;
import de.berlios.vch.utils.HttpUtils;

public class BrainblogParser extends AbstractPageParser {

    private static transient Logger logger = LoggerFactory.getLogger(BrainblogParser.class);
    
    private final String CHARSET = "iso-8859-1";
    
    @SuppressWarnings("unchecked")
    @Override
    public void run() {
        try {

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

            // löschliste holen für nicht video inhalte
            List<SyndEntry> toRemove = new ArrayList<SyndEntry>();

            while (i.hasNext()) {
                SyndEntryImpl current = (SyndEntryImpl) i.next();
                SyndEnclosureImpl enc = new SyndEnclosureImpl();
                
                // only parse video pages
                boolean isVideo = false;
                for (Iterator iterator = current.getCategories().iterator(); iterator.hasNext();) {
                    SyndCategory cat = (SyndCategory) iterator.next();
                    if ("videos".equalsIgnoreCase(cat.getName()) ) {
                        isVideo = true;
                    }
                }
                
                if (isVideo) {
                    // System.out.println(getBrainblogEnclosureLink(current.getDescription().getValue().split("url=")[1].split("\"")[0]));
                    enc.setUrl(getBrainblogEnclosureLink(current.getLink()));
                    enc.setType("video/flv");
                    
                    if(enc.getUrl() == null || "".equals(enc.getUrl())) {
                        toRemove.add(current);
                    }
                    
                    // thumb aus beschreibung holen und beschreibung löschen
                    ImageTag img = (ImageTag) HtmlParserUtils.getTag(current.getDescription().getValue(), CHARSET, "img");
                    if(img != null) {
                        Videocast myvidcast = new VideocastImpl();
                        myvidcast.setImage(img.getImageURL());
                        current.getModules().add(myvidcast);
                    }
                    current.setDescription(null);
                } else {
                    // Wenn das Video nicht auf brainblog liegt --> raus damit
                    toRemove.add(current);

                }

                List<SyndEnclosure> enclist = current.getEnclosures();
                enclist.add(enc);
                current.setEnclosures(enclist);
            }
            // Alle nicht video inhalte löschen
            items.removeAll(toRemove);
            
            // save the feed
            if(feed.getEntries().size() > 0) {
                saveFeed2DB(feed);
                addRssFeed(feed.getLink());
                
                // add all feeds to Group
                Group brainblog = new Group();
                brainblog.setName(Messages.translate(getClass(), "group_name"));
                brainblog.setDescription(Messages.translate(getClass(), "group_desc"));
                addFeedsToGroup(brainblog);
            }
        } catch (Exception e) {
            logger.error("Couldn't parse brainblog webpage", e);
        }
    }
    
    private String getBrainblogEnclosureLink(String itempage) {
        String medialink = new String();
        try {
            String pageContent = HttpUtils.get(itempage, null, CHARSET);
            Pattern p = Pattern.compile("so.addParam\\(\\s*\"flashvars\"\\s*,\\s*\"(.*)\"\\s*\\);");
            Matcher m = p.matcher(pageContent);
            if(m.find()) {
                Map<String, Object> params = new HashMap<String, Object>();
                ParameterParser.parseQuery(m.group(1), params);
                medialink = (String) params.get("file");
                logger.info("Found video: {}", medialink);
            } else {
                logger.debug("No video found on page {}", itempage);
            }
        } catch (Exception e) {
            logger.error("Couldn't parse enclosure link", e);
        }
        return medialink;
    }
}
