package de.berlios.vch.parser.rtlnow;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;

import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.Translate;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.syndication.feed.synd.SyndEnclosure;
import com.sun.syndication.feed.synd.SyndEnclosureImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;
import com.sun.syndication.feed.synd.SyndImage;
import com.sun.syndication.feed.synd.SyndImageImpl;

import de.berlios.vch.Config;
import de.berlios.vch.http.handler.OndemandStreamHandler;
import de.berlios.vch.parser.AbstractPageParser;

public class ParserThread implements Runnable {

    private static transient Logger logger = LoggerFactory.getLogger(ParserThread.class);
    
    private AbstractPageParser parser;
    private Queue<LinkTag> queue;
    
    
    public ParserThread(AbstractPageParser parser, Queue<LinkTag> queue) {
        this.parser = parser;
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
            
            LinkTag linkTag = queue.poll(); 
            
            // if queue is empty, all feeds have been parsed -> end thread
            if(linkTag == null) {
                logger.debug("No more links available. Stopping parser thread");
                return;
            }
            
            String link = linkTag.getLink();
            SyndFeed feed = new SyndFeedImpl();
            
            try {
                // set feed name 
                ProgramPageParser ppp = new ProgramPageParser(link, RTLnowParser.CHARSET);
                String title  = Translate.decode(linkTag.getLinkText().trim());
                feed.setTitle(title);
                logger.info("Parsing program {}. {} programs remaining", title, queue.size());
                
                // set the title as description
                feed.setDescription(title);
                
                // set feed pubDate
                feed.setPublishedDate(new Date());
                
                // set feed link
                feed.setLink(link);
                
                // set the feed image
                String imgUrl = ppp.parseImage();
                SyndImage img = new SyndImageImpl();
                img.setUrl(imgUrl);
                img.setLink(feed.getLink());
                feed.setImage(img);
                
                // parse entries
                List<SyndEntry> entries = ppp.getEntries();
                feed.setEntries(entries);
                
                // create the enclosures
                for (Iterator<SyndEntry> iterator2 = entries.iterator(); iterator2.hasNext();) {
                    SyndEntry entry = iterator2.next();
                    SyndEnclosure enclosure = new SyndEnclosureImpl();
                    enclosure.setType("video/wmv");
                    
                    // set ondemand flag in foreign markup
                    Element elem = new Element("ondemand");
                    elem.setText("true");
                    ((List<Element>)entry.getForeignMarkup()).add(elem);
                    
                    String path = Config.getInstance().getHandlerMapping().getPath(OndemandStreamHandler.class);
                    StringBuilder url = new StringBuilder(Config.getInstance().getBaseUrl());
                    url.append(path);
                    url.append("?provider="); url.append(RTLnowParser.class.getName());
                    url.append("&url="); url.append(URLEncoder.encode(entry.getLink(), "UTF-8"));
                    enclosure.setUrl(url.toString());
                    List<SyndEnclosure> enclosures = new ArrayList<SyndEnclosure>();
                    enclosures.add(enclosure);
                    entry.setEnclosures(enclosures);
                }
                
                parser.saveFeed2DB(feed);
                parser.addRssFeed(feed.getLink());
            } catch (Exception e) {
                logger.error("Couldn't parse program page " + link, e);
            }
        }
    }
}
