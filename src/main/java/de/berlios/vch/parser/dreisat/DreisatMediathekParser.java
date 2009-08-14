package de.berlios.vch.parser.dreisat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.htmlparser.Node;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.berlios.vch.Config;
import de.berlios.vch.i18n.Messages;
import de.berlios.vch.model.Group;
import de.berlios.vch.parser.AbstractPageParser;
import de.berlios.vch.utils.HtmlParserUtils;
import de.berlios.vch.utils.HttpUtils;
import de.berlios.vch.utils.concurrent.ParserThreadFactory;

public class DreisatMediathekParser extends AbstractPageParser {

    private static transient Logger logger = LoggerFactory.getLogger(DreisatMediathekParser.class);
    
    public static final String BASE_URL = "http://www.3sat.de/mediathek";
    
    private static final String LANDING_PAGE = BASE_URL + "/mediathek.php?mode=rss";
    
    public static final String CHARSET = "iso-8859-15";
    
    @Override
    public void run() {
        List<LinkTag> rssFeedUris = getFeedUris();
        logger.info("Found {} feeds", rssFeedUris.size());
        
        // the links in this queue will be processed by several threads
        Queue<LinkTag> queue = new ConcurrentLinkedQueue<LinkTag>(rssFeedUris);
        
        
        // create a thread pool and process the link queue
        int maxThreads = Config.getInstance().getIntValue("parser.dreisatmediathek.maxthreads");
        ExecutorService executorService = Executors.newFixedThreadPool(maxThreads, new ParserThreadFactory(Messages.translate(getClass(), "group_name")));
        for (int i = 0; i < maxThreads; i++) {
            executorService.execute(new DreisatFeedParser(this, queue));
        }
        
        try {
            // shutdown executor service:
            // all active task will be finished, then the executor service
            // will be shut down, so that no thread keeps alive and the
            // RSSFeedCatcher will terminate as expected
            executorService.shutdown();
            
            // wait 5 minutes for the threads to finish
            // and then shutdown all threads immediately
            executorService.awaitTermination(1l, TimeUnit.HOURS);
            executorService.shutdownNow();
        } catch (InterruptedException e) {
            logger.error("ExecutorService interrupted", e);
        }
        
        // add all feeds to Group
        Group dreisat = new Group();
        dreisat.setName(Messages.translate(getClass(), "group_name"));
        dreisat.setDescription(Messages.translate(getClass(), "group_desc"));
        addFeedsToGroup(dreisat);
    }

    private List<LinkTag> getFeedUris() {
        List<LinkTag> pageUris = new ArrayList<LinkTag>();
        try {
            String landingPage = HttpUtils.get(LANDING_PAGE, null, CHARSET);
            NodeList links = HtmlParserUtils.getTags(landingPage, CHARSET, "div[class=rss] a[class=link]");
            NodeIterator iter = links.elements();
            while(iter.hasMoreNodes()) {
                Node child = iter.nextNode();
                if(child instanceof LinkTag) {
                    LinkTag link = (LinkTag) child;
                    pageUris.add(link);
                }
            }
            
            // add the general 3sat mediathek feed
            String html = "<a href=\"rss/mediathek.xml\">" +
            		" 3sat-Mediathek allgemein</a>";
            pageUris.add((LinkTag) HtmlParserUtils.getTag(html, CHARSET, "a"));
        } catch (ParserException e) {
            logger.error("Couldn't parse landing page", e);
        } catch (IOException e) {
            logger.error("Couldn't load the landing page", e);
        }
        return pageUris;
    }

}