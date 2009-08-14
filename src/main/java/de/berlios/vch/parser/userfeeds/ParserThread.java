package de.berlios.vch.parser.userfeeds;

import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.syndication.feed.synd.SyndFeed;

import de.berlios.vch.parser.rss.RssParser;

public class ParserThread implements Runnable {

    private static transient Logger logger = LoggerFactory.getLogger(ParserThread.class);
    
    private Queue<String> queue;
    
    public ParserThread(Queue<String> queue) {
        this.queue = queue;
    }

    @Override
    public void run() {
        logger.debug("ParserThread started");
        while(true) {
            // if the thread has been interrupted, end thread
            if(Thread.currentThread().isInterrupted()) {
                return;
            }
            
            String uri = null;
            try {
                uri = queue.poll();
                
                // if queue is empty, all feeds have been parsed -> end thread
                if(uri == null) {
                    logger.debug("No more feeds to parse. Stopping parser thread");
                    return;
                }
                
                SyndFeed feed = RssParser.parse(uri);
                UserFeedsParser.channels.add(feed);
            } catch (Exception e) {
                logger.error("Couldn't parse program page " + uri, e);
            }
        }
    }
}
