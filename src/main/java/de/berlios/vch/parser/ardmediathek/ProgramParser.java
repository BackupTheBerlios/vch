package de.berlios.vch.parser.ardmediathek;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Queue;

import org.apache.commons.dbutils.DbUtils;
import org.htmlparser.Node;
import org.htmlparser.Tag;
import org.htmlparser.tags.ImageTag;
import org.htmlparser.tags.InputTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.util.Translate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;
import com.sun.syndication.feed.synd.SyndImage;
import com.sun.syndication.feed.synd.SyndImageImpl;

import de.berlios.vch.db.ConnectionManager;
import de.berlios.vch.db.dao.ChannelDAO;
import de.berlios.vch.model.Channel;
import de.berlios.vch.model.Item;
import de.berlios.vch.parser.AbstractPageParser;
import de.berlios.vch.parser.rss.RssParser;
import de.berlios.vch.utils.HtmlParserUtils;
import de.berlios.vch.utils.HttpUtils;
import de.berlios.vch.utils.comparator.ItemPubdateComparator;


public class ProgramParser implements Runnable {
    private static transient Logger logger = LoggerFactory.getLogger(ProgramParser.class);
    
    private int pageCount = 1;
    
    private AbstractPageParser parser;
    
    private Queue<String> pageUris;
    
    public ProgramParser(AbstractPageParser parser, Queue<String> pageUris) {
        this.parser = parser;
        this.pageUris = pageUris;
    }
    
    @SuppressWarnings("unchecked")
    public void parse() {
        while(!Thread.currentThread().isInterrupted()) {
            String programUri = pageUris.poll();
            if(programUri == null) {
                logger.debug("No more program pages. ParserThread {} stops", Thread.currentThread().getName());
                break;
            }
            
            SyndFeed feed = null;
            
            try {
                logger.info("Parsing program page {}. {} pages remaining", programUri, pageUris.size());
                String content = HttpUtils.get(programUri, ARDMediathekParser.HTTP_HEADERS, ARDMediathekParser.CHARSET);
                
                // check if there is a podcast, which can be parsed
                parsePodcast(content);
                
                feed = new SyndFeedImpl();
                feed.setFeedType("rss_2.0");
                feed.setTitle(Translate.decode(HtmlParserUtils.getText(content, ARDMediathekParser.CHARSET, "h4[class=sendung]")));
                feed.setLink(programUri);
                feed.setDescription(Translate.decode(HtmlParserUtils.getText(content, ARDMediathekParser.CHARSET, "div[class~=daten] span[class=sendetitel]")));
                feed.setEncoding(ARDMediathekParser.CHARSET);
                feed.setLanguage("de-de");
                
                // try to parse a feed image
                ImageTag img = (ImageTag) HtmlParserUtils.getTag(content, ARDMediathekParser.CHARSET, "div[class~=sendungAM] div.passepartout img");
                if(img != null) {
                    SyndImage image = new SyndImageImpl();
                    image.setUrl(ARDMediathekParser.BASE_URL + img.getImageURL());
                    feed.setImage(image);
                }

                // get the date of the latest item of this channel
                Date latestItemDate = null;
                Connection conn = null; 
                try {
                    conn = ConnectionManager.getInstance().getConnection();
                    Channel chan = (Channel) new ChannelDAO(conn).findByKey(feed.getLink());
                    if(chan != null) {
                        List<Item> items = chan.getItems();
                            if(items.size() > 0) {
                            Collections.sort(items, new ItemPubdateComparator());
                            Collections.reverse(items);
                            latestItemDate = items.get(0).getPubDate();
                        }
                    }
                } catch (SQLException e) {
                    logger.warn("Couldn't get channel from DB. Can't check for parsed items", e);
                } finally {
                    try {
                        DbUtils.close(conn);
                    } catch (SQLException e) {
                        logger.error("Couldn't close DB connection", e);
                    }
                }
                
                pageCount = determinePageCount(content);
                logger.debug("Program {} has {} pages", programUri, pageCount);
                boolean foundOldItem = false;
                for (int i = 1; i <= pageCount && !foundOldItem; i++) {
                    List<SyndEntry> entries = ProgramPageParser.parse(programUri, i);
                    logger.trace("Found {} video items on program page {} - {}", new Object[] {entries.size(), feed.getTitle(), i});
                    for (SyndEntry syndEntry : entries) {
                        if(latestItemDate == null || syndEntry.getPublishedDate() == null || syndEntry.getPublishedDate().after(latestItemDate)) {
                            feed.getEntries().add(syndEntry);
                        } else {
                            logger.info("Found an old entry on page {}. Stopping parsing of program {}", i, programUri);
                            foundOldItem = true;
                            break;
                        }
                    }
                }
                
                if (feed.getEntries().size() > 0) {
                    // den feed in die DB schreiben
                    parser.saveFeed2DB(feed);
                    // feed url speichern
                    parser.addRssFeed(feed.getLink());
                }
            } catch (ParserException e) {
                logger.error("Couldn't parse page " + programUri, e);
            } catch (IOException e) {
                logger.error("Couldn't load page " + programUri, e);
            } catch (Throwable t) {
                logger.error("Unexpected error while parsing " + programUri, t);
            }
        }
    }

    private void parsePodcast(String content) throws ParserException, IOException {
        NodeList tags = HtmlParserUtils.getTags(content, ARDMediathekParser.CHARSET, "div[class~=buttons] a");
        NodeIterator iter = tags.elements();
        while(iter.hasMoreNodes()) {
            Node node = iter.nextNode();
            if(node instanceof LinkTag) {
                LinkTag link = (LinkTag) node;
                if(link.getAttribute("title") != null && "Diesen Podcast jetzt abonnieren".equalsIgnoreCase(link.getAttribute("title"))) {
                    String aboPage = HttpUtils.get(ARDMediathekParser.BASE_URL + link.getLink(), ARDMediathekParser.HTTP_HEADERS, ARDMediathekParser.CHARSET);
                    Tag podcastField = HtmlParserUtils.getTag(aboPage, ARDMediathekParser.CHARSET, "div[class=podcatcher] input");
                    if(podcastField != null) {
                        InputTag input = (InputTag) podcastField;
                        String podcastUri = input.getAttribute("value");
                        logger.debug("Found podcast {}", podcastUri);
                        try {
                            SyndFeed feed = RssParser.parse(podcastUri);
                            if(!feed.getTitle().toLowerCase().contains("podcast")) {
                                feed.setTitle(feed.getTitle() + " - Podcast");
                            }
                            if(feed.getEntries().size() > 0) {
                                parser.saveFeed2DB(feed);
                                parser.addRssFeed(feed.getLink());
                            }
                        } catch (Exception e) {
                            logger.error("Couldn't parse podcast " + podcastUri, e);
                        }
                    }
                }
            }
        }
    }

    private int determinePageCount(String pageContent) throws IOException, ParserException {
        NodeList pages = HtmlParserUtils.getTags(pageContent, ARDMediathekParser.CHARSET, "div[class~=navi_folgeseiten] li strong");
        return pages.size();
    }
    
    @Override
    public void run() {
        parse();
    }
}
