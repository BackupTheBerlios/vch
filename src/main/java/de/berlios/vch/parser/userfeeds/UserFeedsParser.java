package de.berlios.vch.parser.userfeeds;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Queue;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.dbutils.DbUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.syndication.feed.synd.SyndFeed;

import de.berlios.vch.Config;
import de.berlios.vch.db.ConnectionManager;
import de.berlios.vch.db.dao.UserFeedDAO;
import de.berlios.vch.i18n.Messages;
import de.berlios.vch.model.Group;
import de.berlios.vch.parser.AbstractPageParser;
import de.berlios.vch.utils.concurrent.ParserThreadFactory;

public class UserFeedsParser extends AbstractPageParser {
	
	private static transient Logger logger = LoggerFactory.getLogger(UserFeedsParser.class);
	
	public static List<SyndFeed> channels;
	
	public void run (){ 
		logger.info("UserFeedsParser started");
		channels = new Vector<SyndFeed>();
		
		List<String> feedUris = null;
		ConnectionManager ds = ConnectionManager.getInstance();
        Connection conn = null;
        try {
            // create db connection
            conn = ds.getConnection();
            feedUris = new UserFeedDAO(conn).getFeedUris();
        } catch (Exception e) {
            logger.error("Couldn't load user feed URIs", e);
        } finally {
            try {
                DbUtils.close(conn);
            } catch (SQLException e) {
                logger.error("Couldn't close DB connection", e);
            }
        }

        // the links in this queue will be processed by several threads
        Queue<String> queue = new ConcurrentLinkedQueue<String>(feedUris);
        
        // create a thread pool and process the link queue
        int maxThreads = Config.getInstance().getIntValue("parser.userfeeds.maxthreads");
        ExecutorService executorService = Executors.newFixedThreadPool(maxThreads, new ParserThreadFactory(Messages.translate(getClass(), "group_name")));
        for (int i = 0; i < maxThreads; i++) {
            executorService.execute(new ParserThread(queue));
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
        for (SyndFeed feed : channels) {
            saveFeed2DB(feed);
            addRssFeed(feed.getLink());
        }
        
        // add all feeds to Group
        Group userfeeds = new Group();
        userfeeds.setName(Messages.translate(getClass(), "group_name"));
        userfeeds.setDescription(Messages.translate(getClass(), "group_desc"));
        addFeedsToGroup(userfeeds);
	}
}