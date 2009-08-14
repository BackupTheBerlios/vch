package de.berlios.vch.parser.dreisat;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;

import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.Translate;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.syndication.feed.synd.SyndEnclosure;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;

import de.berlios.vch.parser.AbstractPageParser;
import de.berlios.vch.parser.rss.RssParser;
import de.berlios.vch.utils.AsxParser;


public class DreisatFeedParser implements Runnable {
    private static transient Logger logger = LoggerFactory.getLogger(DreisatFeedParser.class);
    
    private AbstractPageParser parser;
    
    private Queue<LinkTag> feedUris;
    
    private Comparator<SyndEnclosure> comparator = new SyndEnclosureComparator();
    
    public DreisatFeedParser(AbstractPageParser parser, Queue<LinkTag> feedUris) {
        this.parser = parser;
        this.feedUris = feedUris;
    }
    
    @SuppressWarnings("unchecked")
    public void parse() {
        while(!Thread.currentThread().isInterrupted()) {
            LinkTag link = feedUris.poll();
            if(link == null) {
                logger.debug("No more feeds. ParserThread {} stops", Thread.currentThread().getName());
                break;
            }

            try {
                String feedUri = DreisatMediathekParser.BASE_URL + "/" + link.getLink();
                feedUri = feedUri.replaceAll(" ", "%20");
                
                logger.info("Parsing rss feed {}", feedUri);
                SyndFeed feed = RssParser.parse(feedUri);
                feed.setLink(feedUri);
                feed.setTitle(Translate.decode(link.getLinkText()).substring(1));
                
                for (Iterator iterator = feed.getEntries().iterator(); iterator.hasNext();) {
                    SyndEntry entry = (SyndEntry) iterator.next();
                    // sort enclosures, so that the best quality is enclosure[0],
                    // because the Rome2ModelConverter only saves the first
                    // enclosure
                    Collections.sort(entry.getEnclosures(), comparator);
                    Collections.reverse(entry.getEnclosures());

                    // add guid to freign markup, so that RomeToModelConverter uses that guid
                    Element elem = new Element("guid");
                    elem.setText(entry.getLink());
                    ((List<Element>)entry.getForeignMarkup()).add(elem);
                    
                    // if the first enclosure is an asx file, parse the asx file
                    SyndEnclosure first = (SyndEnclosure) entry.getEnclosures().get(0); 
                    if("video/x-ms-asf".equals(first.getType())) {
                        String uri = AsxParser.getUri(first.getUrl());
                        first.setUrl(uri);
                        first.setType("video/wmv");
                    }
                }
                
                logger.trace("Saving feed {}", feed.getTitle());
                parser.saveFeed2DB(feed);
                parser.addRssFeed(feed.getLink());
            } catch (Exception e) {
                logger.error("Couldn't parse RSS feed", e);
            }
        }
    }
    
    @Override
    public void run() {
        parse();
    }
}
