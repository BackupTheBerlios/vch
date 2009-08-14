package de.berlios.vch.parser.zdfmediathek;

import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.berlios.vch.Config;
import de.berlios.vch.i18n.Messages;
import de.berlios.vch.model.Group;
import de.berlios.vch.parser.AbstractPageParser;
import de.berlios.vch.utils.concurrent.ParserThreadFactory;

public class ZDFMediathekParser extends AbstractPageParser {

    private static transient Logger logger = LoggerFactory.getLogger(ZDFMediathekParser.class);
    
    @Override
    public void run() {
        RSSLinkGetter current_links = new RSSLinkGetter();
        Set<String> links = current_links.getLinks();
        logger.debug("Found " + links.size() + " RSS feeds");
        
        // the links in this queue will be processed by several threads
        Queue<String> queue = new ConcurrentLinkedQueue<String>(links);
        
        // create a thread pool and process the link queue
        int maxThreads = Config.getInstance().getIntValue("parser.zdfmediathek.maxthreads");
        ExecutorService executorService = Executors.newFixedThreadPool(maxThreads, new ParserThreadFactory(Messages.translate(getClass(), "group_name")));
        for (int i = 0; i < maxThreads; i++) {
            executorService.execute(new ParserThread(this, queue));
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
        
        // add all feeds to Group
        Group zdf = new Group();
        zdf.setName(Messages.translate(getClass(), "group_name"));
        zdf.setDescription(Messages.translate(getClass(), "group_desc"));
        addFeedsToGroup(zdf);
    }
}