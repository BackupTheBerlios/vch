package de.berlios.vch.utils.enclosurechecker;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.berlios.vch.model.Item;

public class EnclosureCheckWorker implements Runnable {

    private static transient Logger logger = LoggerFactory.getLogger(EnclosureCheckWorker.class);
    
    private Queue<Item> queue;
    
    public EnclosureCheckWorker(Queue<Item> queue) {
        this.queue = queue;
    }

    @Override
    public void run() {
        logger.debug("EnclosureCheckWorker started");
        while(true) {
            // if the thread has been interrupted, end thread
            if(Thread.currentThread().isInterrupted()) {
                return;
            }
            
            Item item = null;
            try {
                item = queue.poll();
                logger.debug("Checking Enclosure: " + item.getTitle());
                URI uri = new URI(item.getEnclosureKey());
                boolean available = true;
                if(uri.getScheme().startsWith("http")) { // includes http and https
                    continue;
                    //available = checkHttpEnclosure(uri);
                } else if(uri.getScheme().startsWith("mms")) {
                    available = checkMmsEnclosure(uri);
                } else if(uri.getScheme().startsWith("rtsp")) {
                    // TODO JMF (java media framework supports rtsp)
                    continue;
                }

                if(available) {
                    logger.trace("Enclosure {} is available", uri);
                } else {
                    EnclosureChecker.getInstance().addBrokenItem(item);
                }
                
                EnclosureChecker.getInstance().increaseNumberOfProcessed();
            } catch (Exception e) {
                logger.error("Couldn't check enclosure " + item.getEnclosure().getLink(), e);
            }
        }
    }
    
    private static boolean checkMmsEnclosure(URI uri) {
        MMSCheck check = null;
        try {
            check = new MMSCheck(uri);
            check.startDownload();
            return check.isAvailable();
        } catch (Exception e) {
            logger.error("Couldn't check mms enclosure", e);
        } 
        return false;
    }

    // TODO use HEAD
    public static boolean checkHttpEnclosure(URI uri) {
        HttpURLConnection con = null;
        try {
            URL url = uri.toURL();
            con = (HttpURLConnection) url.openConnection();
            if (con.getResponseCode() == 200) {
                if(con.getContentLength() == -1) {
                    return false;
                } else {
                    return true;
                }
            } else {
                return false;
            }
        } catch (IOException e) {
            logger.error("Couldn't open connection to enclosure " + uri.toString(), e);
            return false;
        }
    }
}
