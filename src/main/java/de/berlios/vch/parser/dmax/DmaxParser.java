package de.berlios.vch.parser.dmax;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.htmlparser.Node;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.syndication.feed.synd.SyndFeed;

import de.berlios.vch.Config;
import de.berlios.vch.i18n.Messages;
import de.berlios.vch.model.Group;
import de.berlios.vch.parser.AbstractPageParser;
import de.berlios.vch.utils.HtmlParserUtils;
import de.berlios.vch.utils.HttpUtils;
import de.berlios.vch.utils.concurrent.ParserThreadFactory;

public class DmaxParser extends AbstractPageParser {

    private static transient Logger logger = LoggerFactory.getLogger(DmaxParser.class);
    
    final static String CHARSET = "utf-8";
    
    private final int MAX_ITEMS = 100;
    
    final static String BASE_URI = "http://www.dmax.de"; 
    
    private final String LANDING_PAGE = BASE_URI + "/video/morevideo.shtml?name=longform&sort=date&contentSize="+MAX_ITEMS+"&pageType=longFormHub&displayBlockName=popularLong";
    
    static Map<String, SyndFeed> channels;
    
    @Override
    public void run() {
        channels = Collections.synchronizedMap(new HashMap<String, SyndFeed>());
        Queue<Node> queue = new ConcurrentLinkedQueue<Node>();
        try {
            for (int i = 1; i <= MAX_ITEMS/20; i++) {
                String URI = LANDING_PAGE + "&page=" + i; 
                String content = HttpUtils.get(URI, null, CHARSET);
                NodeList itemCells = HtmlParserUtils.getTags(content, CHARSET, "div#vp-perpage-promolist div[class~=vp-promo-item]");
                for (NodeIterator iterator = itemCells.elements(); iterator.hasMoreNodes();) {
                    queue.add(iterator.nextNode());
                }
            }
        } catch (IOException e) {
            logger.error("Couldn't load overview page", e);
            return;
        } catch (ParserException e) {
            logger.error("Couldn't parse overview page", e);
            return;
        }
                
        // create a thread pool and process the itemCell queue
        int maxThreads = Config.getInstance().getIntValue("parser.dmax.maxthreads");
        ExecutorService executorService = Executors.newFixedThreadPool(maxThreads, new ParserThreadFactory(Messages.translate(getClass(), "group_name")));
        for (int j = 0; j < maxThreads; j++) {
            executorService.execute(new ItemCellParser(queue));
        }
        
        try {
            // shutdown executor service:
            // all active task will be finished, then the executor service
            // will be shut down, so that no thread keeps alive and the
            // RSSFeedCatcher will terminate as expected
            executorService.shutdown();
            
            // wait 5 minutes for the threads to finish
            // and then shutdown all threads immediately
            executorService.awaitTermination(5l, TimeUnit.MINUTES);
            executorService.shutdownNow();
        } catch (InterruptedException e) {
            logger.error("ExecutorService interrupted", e);
        }
        
        // save the feeds
        for (SyndFeed feed : channels.values()) {
            saveFeed2DB(feed);
            addRssFeed(feed.getLink());
        }
        
        // add all feeds to Group
        Group dmax = new Group();
        dmax.setName(Messages.translate(DmaxParser.class, "group_name"));
        dmax.setDescription(Messages.translate(DmaxParser.class, "group_desc"));
        addFeedsToGroup(dmax);
    }
}
