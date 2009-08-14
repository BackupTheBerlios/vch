package de.berlios.vch.parser.zdfmediathek;

import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.syndication.feed.synd.SyndEnclosure;
import com.sun.syndication.feed.synd.SyndEnclosureImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

import de.berlios.vch.parser.AbstractPageParser;

public class ParserThread implements Runnable {

    private static transient Logger logger = LoggerFactory.getLogger(ParserThread.class);
    
    private WMVLinkgetter wmvLinkgetter = new WMVLinkgetter();
    
    private Queue<String> linkQueue;
    
    private AbstractPageParser pageParser;
    
    protected ParserThread(AbstractPageParser pageParser, Queue<String> linkQueue) {
        this.pageParser = pageParser;
        this.linkQueue = linkQueue;
    }
    
    @Override
    public void run() {
        logger.debug("ParserThread started");
        while(true) {
            // if the thread has been interrupted, end thread
            if(Thread.currentThread().isInterrupted()) {
                return;
            }
            
            String link = "";
            try {
                link = linkQueue.poll();
                
                // if queue is empty, all feeds have been parsed -> end thread
                if(link == null) {
                    logger.debug("No more links available. Stopping parser thread");
                    return;
                }
            
                logger.debug("Getting RSS feed " + link);
                
                // Hole RSS xml aus den geparsten links
                URL feedUrl = new URL(link);
                SyndFeedInput input = new SyndFeedInput();
    
                // RSS in das SyndFeed Object Parsen
                XmlReader xmlReader = new XmlReader(feedUrl);
                SyndFeed feed = input.build(xmlReader);
                feed.setEncoding(xmlReader.getEncoding());
                
                // dont use the link provided in the rss feed,
                // but the original url
                //feed.setLink(link);
                
                // Über den Feed loopen
                @SuppressWarnings("unchecked")
                List<SyndEntry> items = feed.getEntries();
                Iterator<SyndEntry> i = items.iterator();
    
                // löschliste holen für nicht video inhalte
                List<SyndEntry> toRemove = new ArrayList<SyndEntry>();
    
                while (i.hasNext()) {
                    SyndEntryImpl current = (SyndEntryImpl) i.next();
                    SyndEnclosureImpl enc = new SyndEnclosureImpl();
                    String uri = wmvLinkgetter.GetWMVLink(current.getLink());
                    
                    if(uri == null) {
                        i.remove();
                        continue;
                    }
                    
                    enc.setUrl(uri);
                    enc.setType("video/wmv");
    
                    // Wenn der link leer ist das item in die löschliste
                    // einfügen
                    if (enc.getUrl().matches("")) {
                        toRemove.add(current);
                    } else {
                        // Den Enclosure einfügen
                        @SuppressWarnings("unchecked")
                        List<SyndEnclosure> enclist = current.getEnclosures();
                        enclist.add(enc);
                        current.setEnclosures(enclist);
                    }
                }
    
                // Alle nicht video inhalte löschen
                items.removeAll(toRemove);
    
                if (feed.getEntries().size() > 0) {
                    // den Feed in datei schreiben
                    //ds.saveFeed(feed);
                    // den feed in die DB schreiben
                    pageParser.saveFeed2DB(feed);
                    // feed url speichern
                    pageParser.addRssFeed(feed.getLink());
                }
            } catch (Exception ex) {
                logger.error("Couldn't parse link " + link, ex);
            }
        }
    }
}