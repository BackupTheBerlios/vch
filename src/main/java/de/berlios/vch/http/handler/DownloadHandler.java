package de.berlios.vch.http.handler;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.dbutils.DbUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;

import de.berlios.vch.Config;
import de.berlios.vch.db.ConnectionManager;
import de.berlios.vch.db.dao.DownloadDAO;
import de.berlios.vch.db.dao.ItemDAO;
import de.berlios.vch.download.AbstractDownload;
import de.berlios.vch.download.DownloadManager;
import de.berlios.vch.http.TemplateLoader;
import de.berlios.vch.i18n.Messages;
import de.berlios.vch.model.Download;
import de.berlios.vch.model.Item;

public class DownloadHandler extends AbstractHandler {

    private static transient Logger logger = LoggerFactory.getLogger(DownloadHandler.class);
    
    @Override
    void doHandle(HttpExchange exchange) throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) exchange.getAttribute("parameters");
        String path = exchange.getRequestURI().getPath();
        
        Map<String, Object> tplParams = new HashMap<String, Object>();
        tplParams.put("ACTION", path);
        tplParams.put("TITLE", Messages.translate(getClass(), "downloads"));
        tplParams.put("I18N_START", getClass().getName() + ".start");
        tplParams.put("I18N_START_ALL", getClass().getName() + ".start_all");
        tplParams.put("I18N_STOP", getClass().getName() + ".stop");
        tplParams.put("I18N_STOP_ALL", getClass().getName() + ".stop_all");
        tplParams.put("I18N_DELETE", getClass().getName() + ".delete");
        tplParams.put("I18N_TITLE", getClass().getName() + ".title");
        tplParams.put("I18N_PROGRESS", getClass().getName() + ".progress");
        tplParams.put("I18N_SPEED", getClass().getName() + ".speed");
        tplParams.put("I18N_STATUS", getClass().getName() + ".status");
        tplParams.put("I18N_OPTIONS", getClass().getName() + ".options");
        tplParams.put("I18N_REFRESH", getClass().getName() + ".refresh");
        tplParams.put("I18N_AUTO_REFRESH", getClass().getName() + ".auto_refresh");
        tplParams.put("I18N_N_A", getClass().getName() + ".n_a");
        tplParams.put("I18N_FINISHED_DOWNLOADS", getClass().getName() + ".finished_downloads");
        tplParams.put("I18N_FILE", getClass().getName() + ".file");
        tplParams.put("I18N_COULDNT_DELETE", getClass().getName() + ".error_delete_download");
        // TODO i18n for status value
        
        // enable ajax?
        tplParams.put("AJAX_ENABLED", Config.getInstance().getBoolValue("ajax.enabled"));
        
        // add errors and messages
        tplParams.put("ERRORS", exchange.getAttribute("errors"));
        tplParams.put("MESSAGES", exchange.getAttribute("messages"));
        
        // action
        String action = (String) params.get("action");
        if("stop".equals(action)) {
            String id = (String) params.get("id");
            DownloadManager.getInstance().stopDownload(id);
            params.remove("action");
            doHandle(exchange);
        } else if("start".equals(action)) {
            String id = (String) params.get("id");
            DownloadManager.getInstance().startDownload(id);
            params.remove("action");
            doHandle(exchange);
        } else if("delete".equals(action)) {
            String id = (String) params.get("id");
            DownloadManager.getInstance().cancelDownload(id);
            params.remove("action");
            doHandle(exchange);
        } else if("start_all".equals(action)) {
            DownloadManager.getInstance().startDownloads();
            params.remove("action");
            doHandle(exchange);
        } else if("stop_all".equals(action)) {
            DownloadManager.getInstance().stopDownloads();
            params.remove("action");
            doHandle(exchange);
        } else if ("download_item".equals(action)) {
            downloadItem(exchange);
        } else if ("delete_finished".equals(action)) {
            params.remove("action");
            String itemKey = (String) params.get("id");
            deleteDownload(itemKey);
            addMessage(Messages.translate(getClass(), "file_deleted"));
            doHandle(exchange);
        } else {
            listDownloads(tplParams);
        }
    }

    public static void deleteDownload(String itemKey) throws SQLException, IOException {
        Connection conn = null;
        try {
            // open the connection
            conn = ConnectionManager.getInstance().getConnection();
            
            // load the finished downloads
            DownloadDAO dao = new DownloadDAO(conn);
            Download d = (Download) dao.findByKey(itemKey);
            
            if(d == null) {
                throw new RuntimeException(Messages.translate(Messages.class, "entity_not_found", new Object[] {"Download", itemKey}));
            }
            
            // try to delete the file
            File file = new File(d.getLocalFile());
            if(file.exists()) {
                if(!file.delete()) {
                    throw new IOException(Messages.translate(DownloadHandler.class, "error_delete_file"));
                }
            } else {
                logger.warn("File doesn't exist. Going to delete the DB entry, though");
            }
            
            // try to delete the nfo file
            if(file != null) {
                File nfoFile = new File(file.getParentFile(), file.getName() + ".nfo");
                if(nfoFile != null && nfoFile.exists()) {
                    boolean deleted = nfoFile.delete();
                    if(!deleted) {
                        logger.warn("Couldn't delete file " + nfoFile.getAbsolutePath());
                    }
                }
            }
                        
            // delete the database entry
            dao.delete(d);
        } finally {
            try {
                DbUtils.close(conn);
            } catch (SQLException e) {
                logger.error("Couldn't close DB connection", e);
            }
        }
    }

    private void listDownloads(Map<String, Object> tplParams) throws SQLException {
        List<AbstractDownload> downloads = DownloadManager.getInstance().getDownloads();
        
        Connection conn = null;
        try {
            // open the connection
            conn = ConnectionManager.getInstance().getConnection();
            
            // load the finished downloads
            List<Download> finishedDownloads = new DownloadDAO(conn).getAll(true);
            tplParams.put("FINISHED_DOWNLOADS", finishedDownloads);
            
            // load the items corresponding to the downloads
            List<Item> items = null;
            for (AbstractDownload download : downloads) {
                if(download.getItem() == null) {
                    if(items == null) {
                        items = new ItemDAO(conn).getAll();
                    }
                    for (Item item : items) {
                        if(item.getEnclosure() != null && download.getId().equals(item.getEnclosure().getLink())) {
                            download.setItem(item);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Couldn't load downloads", e);
        } finally {
            try {
                DbUtils.close(conn);
            } catch (SQLException e) {
                logger.error("Couldn't close database connection", e);
            }
        }
        
        tplParams.put("DOWNLOADS", downloads);
        String template = TemplateLoader.loadTemplate("downloads.ftl", tplParams);
        sendResponse(200, template, "text/html");
    }

    @Override
    protected String getDescriptionKey() {
        return "downloads";
    }
    
    private void downloadItem(HttpExchange exchange) throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) exchange.getAttribute("parameters");
        try {
            if(params.get("guid") != null) {
                DownloadManager.getInstance().addDownloadByGuid((String)params.get("guid"));
                addMessage(Messages.translate(getClass(), "download_added"));
            } else if(params.get("url") != null) {
                DownloadManager.getInstance().addDownloadByUrl((String)params.get("url"));
                addMessage(Messages.translate(getClass(), "download_added"));
            } else {
                throw new Exception("Missing parameter url/guid");
            }
        } catch (Exception e) {
            logger.warn("Couldn't add download", e);
            addError(e);
        }
        params.remove("action");
        doHandle(exchange);
    }
}
