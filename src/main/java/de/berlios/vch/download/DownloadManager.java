package de.berlios.vch.download;

import java.io.File;
import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.dbutils.DbUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.berlios.vch.Config;
import de.berlios.vch.db.ConnectionManager;
import de.berlios.vch.db.dao.DownloadDAO;
import de.berlios.vch.db.dao.ItemDAO;
import de.berlios.vch.download.AbstractDownload.Status;
import de.berlios.vch.http.filter.ParameterParser;
import de.berlios.vch.http.handler.StreamBridgeHandler;
import de.berlios.vch.i18n.Messages;
import de.berlios.vch.model.Download;
import de.berlios.vch.model.Item;
import de.berlios.vch.parser.OndemandParser;

public class DownloadManager implements DownloadStateListener {
    
    private static transient Logger logger = LoggerFactory.getLogger(DownloadManager.class);
    
    private static DownloadManager instance;
    
    private List<AbstractDownload> downloads = new ArrayList<AbstractDownload>();
    
    private ExecutorService executor;
    
    private int numberOfConcurrentDownloads = 2;

    private DownloadManager() {
        numberOfConcurrentDownloads = Config.getInstance().getIntValue("downloader.concurrent_downloads");
        executor = Executors.newFixedThreadPool(numberOfConcurrentDownloads);
    }

    public static DownloadManager getInstance() {
        if (instance == null) {
            instance = new DownloadManager();
        }
        return instance;
    }
    
    public synchronized void addDownload(Item item) throws Exception {
        String u = item.getEnclosure().getLink();
        
        // do we have an ondemand video? than we have to do the parsing first
        if(item.getEnclosure().isOndemand()) {
            URI uri = new URI(item.getEnclosure().getLink());
            Map<String, Object> params = new HashMap<String, Object>();
            ParameterParser.parseQuery(uri.getQuery(), params);
            String provider = (String) params.get("provider");
            String videoUri = (String) params.get("url");
            OndemandParser parser = (OndemandParser) Class.forName(provider).newInstance();
            u = parser.parseOnDemand(videoUri);
        }
        
        // check, if the uri is StreamBridge uri
        String base = Config.getInstance().getBaseUrl();
        String bridgeh = Config.getInstance().getHandlerMapping().getPath(StreamBridgeHandler.class);
        if(u.startsWith(base + bridgeh)) {
            URI uri = new URI(u);
            Map<String, Object> params = new HashMap<String, Object>();
            ParameterParser.parseQuery(uri.getQuery(), params);
            u = (String) params.get("uri");
        }

        // check, if this download doesn't exist already
        if(getDownload(u) != null) {
            throw new Exception(Messages.translate(getClass(), "download_exists"));
        }

        // start the download
        logger.debug("Adding download for " + u);
        URI uri = new URI(u);
        AbstractDownload d = DownloadFactory.createDownload(uri);
        d.setItem(item);
        d.addDownloadStateListener(this);
        File destinationDir = new File(Config.getInstance().getProperty("video.cache.dir"));
        d.setDestinationDir(destinationDir);
        downloads.add(d);
        d.setStatus(Status.WAITING);
        executor.submit(d);
    }
    
    public synchronized void addDownloadByGuid(String guid) throws Exception {
        Connection conn = null;
        try {
            conn = ConnectionManager.getInstance().getConnection();
            Item item = (Item) new ItemDAO(conn).findByKey(guid);
            if(item != null) {
                addDownload(item);
            } else {
                logger.warn("No item for guid found:" + guid);
            }
        } catch (SQLException e) {
            logger.error("Couldn't lookup item for GUID " + guid, e);
        } finally {
            try {
                DbUtils.close(conn);
            } catch (SQLException e) {
                logger.error("Couldn't close database connection", e);
            }
        }
    }
    
    public synchronized void addDownloadByUrl(String url) throws Exception {
        Connection conn = null;
        try {
            conn = ConnectionManager.getInstance().getConnection();
            Item item = (Item) new ItemDAO(conn).findByProperty("enclosurekey", url);
            if(item != null) {
                addDownload(item);
            } else {
                logger.warn("No item for url {} found", url);
            }
        } catch (SQLException e) {
            logger.error("Couldn't lookup item for url " + url, e);
        } finally {
            try {
                DbUtils.close(conn);
            } catch (SQLException e) {
                logger.error("Couldn't close database connection", e);
            }
        }
    }
    
    public void startDownloads() {
        for (AbstractDownload d : downloads) {
            if(d.isStartable()) {
                d.setStatus(Status.WAITING);
                executor.submit(d);
            }
        }
    }
    
    public void stopDownloads() {
        executor.shutdownNow();
        executor = Executors.newFixedThreadPool(numberOfConcurrentDownloads);
        for (AbstractDownload d : downloads) {
            if(!d.isStartable()) {
                d.stop();
            }
        }
    }

    public List<AbstractDownload> getDownloads() {
        return downloads;
    }
    
    public void startDownload(String id) {
        AbstractDownload d = getDownload(id);
        if(d != null) {
            if(d.isStartable()) { // dont start a download twice
                d.setStatus(Status.WAITING);
                executor.submit(d);
            } else {
                logger.info("AbstractDownload already running or queued");
            }
        }
    }
    
    public void stopDownload(String id) {
        AbstractDownload d = getDownload(id);
        if(d != null) d.stop();
    }
    
    private AbstractDownload getDownload(String id) {
        for (AbstractDownload d : downloads) {
            if(d.getId().equals(id)) {
                return d;
            }
        }
        
        return null;
    }

    public void cancelDownload(String id) {
        stopDownload(id);
        AbstractDownload d = getDownload(id);
        if(d != null) {
            d.cancel();
            downloads.remove(d);
        }
    }

    @Override
    public void downloadStateChanged(AbstractDownload download) {
        if(download.getStatus() == AbstractDownload.Status.FINISHED) {
            ConnectionManager ds = ConnectionManager.getInstance();
            Connection conn = null;
            try {
                // create db connection
                conn = ds.getConnection();
                
                // save the download
                Download d = new Download(download);
                new DownloadDAO(conn).saveOrUpdate(d);
                
                downloads.remove(download);
            } catch (SQLException e) {
                logger.error("Couldn't save download", e);
                if(conn != null) {
                    try {
                        conn.rollback();
                    } catch (SQLException e1) {
                        logger.error("Couldn't rollback transaction",e);
                    }
                }
            } finally {
                try {
                    DbUtils.close(conn);
                } catch (SQLException e) {
                    logger.error("Couldn't close database connection", e);
                }
            }
        }
    }
}