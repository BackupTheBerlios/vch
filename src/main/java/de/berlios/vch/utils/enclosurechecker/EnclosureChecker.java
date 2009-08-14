package de.berlios.vch.utils.enclosurechecker;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.dbutils.DbUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.berlios.vch.db.ConnectionManager;
import de.berlios.vch.db.dao.ItemDAO;
import de.berlios.vch.model.Item;
import de.berlios.vch.utils.concurrent.ParserThreadFactory;

public class EnclosureChecker {
	
    private static transient Logger logger = LoggerFactory.getLogger(EnclosureChecker.class);
	
    private static EnclosureChecker instance;
    
    private boolean running = false;
    
    private Set<Item> deleteSet = new HashSet<Item>();
    
    private int numberOfEnclosures;
    
    private int numberOfProcessed;
    
    private EnclosureChecker() {
    }

    public static synchronized EnclosureChecker getInstance() {
        if (instance == null) {
            instance = new EnclosureChecker();
        }
        return instance;
    }
    
	public boolean start() {
	    if(running) {
	        return false;
	    }
	    
	    reset();
	    running = true;
	    Connection conn = null;
        try {
            ConnectionManager ds = ConnectionManager.getInstance();
            
            // create db connection
            conn = ds.getConnection();

            List<Item> items = new ItemDAO(conn).getAll();
            numberOfEnclosures = items.size();
            
            // the items in this queue will be processed by several threads
            Queue<Item> queue = new ConcurrentLinkedQueue<Item>(items);
            
            // create a thread pool and process the link queue
            int maxThreads = 10; //Config.getInstance().getIntValue("enclosurechecker.maxthreads");
            ExecutorService executorService = Executors.newFixedThreadPool(maxThreads, new ParserThreadFactory("EnclosureChecker"));
            for (int i = 0; i < maxThreads; i++) {
                executorService.execute(new EnclosureCheckWorker(queue));
            }
            
            // shutdown executor service:
            // all active task will be finished, then the executor service
            // will be shut down, so that no thread keeps alive 
            executorService.shutdown();
            
            // wait 60 minutes for the threads to finish
            // and then shutdown all threads immediately
            executorService.awaitTermination(24l, TimeUnit.HOURS);
            executorService.shutdownNow();
        } catch (Exception e) {
            logger.error("Couldn't check enclosure", e);
        } finally {
            running = false;
            try {
                DbUtils.close(conn);
            } catch (SQLException e) {
                logger.error("Couldn't close database connection", e);
            }
        }
        return true;
	}
	
	public boolean isRunning() {
        return running;
    }
	
	public int getProgress() {
        return (int) ((float)numberOfProcessed / (float)numberOfEnclosures * 100);
    }
	
	public int getNumberOfEnclosures() {
        return numberOfEnclosures;
    }
	
	public int getNumberOfProcessed() {
        return numberOfProcessed;
    }
	
	public synchronized void increaseNumberOfProcessed() {
	    numberOfProcessed++;
	}
	
	public synchronized void addBrokenItem(Item item) {
	    deleteSet.add(item);
	}
	
	public int getBrokenCount() {
	    return deleteSet.size();
	}
	
	private void reset() {
	    deleteSet.clear();
	}
}