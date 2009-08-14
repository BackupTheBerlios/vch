package de.berlios.vch.parser.dmax;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Queue;

import org.apache.commons.dbutils.DbUtils;
import org.htmlparser.Node;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.ImageTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.util.Translate;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEnclosure;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;

import de.berlios.vch.db.ConnectionManager;
import de.berlios.vch.db.dao.ChannelDAO;
import de.berlios.vch.model.Channel;
import de.berlios.vch.rome.videocast.Videocast;
import de.berlios.vch.rome.videocast.VideocastImpl;
import de.berlios.vch.utils.HtmlParserUtils;
import de.berlios.vch.utils.ModelToRomeConverter;

public class ItemCellParser implements Runnable {

    private static transient Logger logger = LoggerFactory.getLogger(ItemCellParser.class);
    
    private Queue<Node> queue;
    
    public ItemCellParser(Queue<Node> queue) {
        this.queue = queue;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void run() {
        logger.debug("ParserThread started");
        while(true) {
            // if the thread has been interrupted, end thread
            if(Thread.currentThread().isInterrupted()) {
                return;
            }
            
            Div itemCell = null;
            try {
                itemCell = (Div) queue.poll();
                
                // if queue is empty, all feeds have been parsed -> end thread
                if(itemCell == null) {
                    logger.debug("No more items available. Stopping parser thread");
                    return;
                }
                
                logger.info("Parsing next item cell. {} remaining", queue.size());
                
                // parse the feed title
                String feedTitle = Translate.decode( HtmlParserUtils.getText(itemCell.toHtml(), DmaxParser.CHARSET, "a.vp-promo-title").trim() );
                
                // parse the item cell
                SyndEntry entry = parseItemCell(itemCell.toHtml());
                
                // parse the video page to get the pubDate and further chapters
                VideoPageParser parser = new VideoPageParser(entry.getLink());
                try {
                    parser.loadPage();
                    
                    // parse the publish date
                    entry.setPublishedDate(parser.parsePubDate());
                    
                    // parse the enclosure
                    SyndEnclosure encl = parser.getEnclosure();
                    entry.getEnclosures().add(encl);
                } catch (IOException e) {
                    logger.error("Couldn't load video page " + entry.getLink(), e);
                    continue;
                } catch (ParseException e) {
                    logger.warn("Couldn't parse video page.", e);
                    entry.setPublishedDate(new Date());
                } catch (ParserException e) {
                	logger.warn("Couldn't parse video page.", e);
                	entry.setPublishedDate(new Date());
                }
                
                // add entry to the feed
                SyndFeed feed = getFeed(feedTitle);
                feed.getEntries().add(entry);
                
                try {
                    String feedLink = parser.getFeedLink();
                    feed.setLink(feedLink);
                } catch (Exception e1) {
                    logger.error("Couldn't parse feed link", e1);
                    continue;
                }
                
                // parse further chapters
                try {
                    NodeList chapterItemCells = parser.getChapterItemCells();
                    for (NodeIterator iter = chapterItemCells.elements(); iter.hasMoreNodes();) {
                        Div chapterItemCell = (Div) iter.nextNode();
                        // parse the item cell
                        entry = parseItemCell(chapterItemCell.toHtml());
                        
                        // parse the video page to get the pubDate and further chapters
                        parser = new VideoPageParser(entry.getLink());
                        try {
                            parser.loadPage();
                            
                            // parse the publish date
                            entry.setPublishedDate(parser.parsePubDate());
                            
                            // parse the enclosure
                            SyndEnclosure encl = parser.getEnclosure();
                            entry.getEnclosures().add(encl);
                        } catch (IOException e) {
                            logger.error("Couldn't load video page " + entry.getLink(), e);
                            continue;
                        } catch (ParseException e) {
                            logger.warn("Couldn't parse video page.", e);
                            entry.setPublishedDate(new Date());
                        } catch (ParserException e) {
                            logger.warn("Couldn't parse video page.", e);
                            entry.setPublishedDate(new Date());
                        }
                        feed.getEntries().add(entry);
                    }
                } catch (ParserException e) {
                    logger.error("Couldn't parse further episode chapters", e);
                }
            } catch (Exception e) {
                logger.error("Couldn't parse item cell", e);
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    public SyndEntry parseItemCell(String cellHtml) throws Exception {
        LinkTag a = (LinkTag) HtmlParserUtils.getTag(cellHtml, DmaxParser.CHARSET, "div.vp-promo-image a");
        ImageTag img = (ImageTag) HtmlParserUtils.getTag(cellHtml, DmaxParser.CHARSET, "div.vp-promo-image img");
        String programPageUri = DmaxParser.BASE_URI + a.getLink();
        String thumbnail = img.getImageURL();
        String videoTitle = Translate.decode( HtmlParserUtils.getText(cellHtml, DmaxParser.CHARSET, "span.vp-promo-subtitle-title").trim() );
        String _duration = Translate.decode( HtmlParserUtils.getText(cellHtml, DmaxParser.CHARSET, "span.vp-promo-subtitle-time").trim() );
        _duration = _duration.substring(1, _duration.length()-1);
        int mins = Integer.parseInt(_duration.split(":")[0]);
        int secs = Integer.parseInt(_duration.split(":")[1]);
        String desc = Translate.decode( HtmlParserUtils.getText(cellHtml, DmaxParser.CHARSET, "div.vp-tooltip-text").trim() );
        
        SyndEntry entry = new SyndEntryImpl();
        entry.setTitle(videoTitle);
        entry.setLink(programPageUri);
        
        // set the description
        SyndContent description = new SyndContentImpl();
        description.setValue(desc);
        entry.setDescription(description);
        
        // add the thumbnail
        Videocast myvidcast = new VideocastImpl();
        myvidcast.setImage(thumbnail);
        entry.getModules().add(myvidcast);
        
        // set duration in foreign markup
        Element elem = new Element("duration");
        elem.setText(Long.toString(mins * 60 + secs));
        ((List<Element>)entry.getForeignMarkup()).add(elem);
        
        return entry;
    }
    
    public SyndFeed getFeed(String feedName) throws SQLException {
        SyndFeed feed = DmaxParser.channels.get(feedName);
        if(feed == null) {
            // look up this feed in the DB. in this case we search by name and not by link
            // because the link for each arte channel may change
            Connection conn = ConnectionManager.getInstance().getConnection();
            Channel channel = new ChannelDAO(conn).findByName(feedName);
            DbUtils.close(conn);
            if(channel != null) {
                feed = ModelToRomeConverter.convert(channel);
            }
            if(feed == null) {
                logger.info("Create new feed {}", feedName);
                feed = new SyndFeedImpl();
                feed.setTitle(feedName);
                feed.setLink("http://www.dmax.de/video/");
                feed.setFeedType("rss_2.0");
                feed.setDescription("");
                feed.setPublishedDate(new Date());
            }
            DmaxParser.channels.put(feedName, feed);
        }
        return feed;
    }
}
