package de.berlios.vch.osdserver.osd.menu;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.dbutils.DbUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.berlios.vch.db.ConnectionManager;
import de.berlios.vch.db.dao.DownloadDAO;
import de.berlios.vch.download.AbstractDownload;
import de.berlios.vch.download.DownloadManager;
import de.berlios.vch.i18n.Messages;
import de.berlios.vch.model.Download;
import de.berlios.vch.osdserver.io.StringUtils;
import de.berlios.vch.osdserver.io.response.Event;
import de.berlios.vch.osdserver.osd.IEventListener;
import de.berlios.vch.osdserver.osd.Menu;
import de.berlios.vch.osdserver.osd.OsdItem;
import de.berlios.vch.osdserver.osd.listener.DownloadsMenuListener;

public class DownloadsMenu extends Menu {

    private static transient Logger logger = LoggerFactory.getLogger(DownloadsMenu.class);
    
    private IEventListener downloadsListener = new DownloadsMenuListener();
    
    public DownloadsMenu() {
        super("downloads", Messages.translate(DownloadsMenu.class, "downloads"));
        registerEvent(new Event(getId(), Event.KEY_RED, null));
        registerEvent(new Event(getId(), Event.KEY_GREEN, null));
        registerEvent(new Event(getId(), Event.KEY_BLUE, null));
        addEventListener(downloadsListener);

        // add the active downloads to the menu
        List<AbstractDownload> activeDownloads = DownloadManager.getInstance().getDownloads();
        if(!activeDownloads.isEmpty()) {
            // headline for active downloads
            OsdItem headActive = new OsdItem("headActive", Messages.translate(getClass(), "active"));
            headActive.setSelectable(false);
            addOsdItem(headActive);
        }
        for (int i = 0; i < activeDownloads.size(); i++) {
            AbstractDownload download = activeDownloads.get(i);
            String downloadId = "download"+i;
            String line = formatActiveDownload(download);
            OsdItem osditem = new OsdItem(downloadId, line);
            osditem.setUserData(download);
            osditem.registerEvent(new Event(downloadId, Event.FOCUS, null));
            osditem.registerEvent(new Event(getId(), Event.KEY_OK, null));
            osditem.addEventListener(downloadsListener);
            addOsdItem(osditem);
        }
        
        // add the finished downloads to the menu
        Connection conn = null;
        List<Download> finishedDownloads;
        try {
            conn = ConnectionManager.getInstance().getConnection();
            DownloadDAO ddao = new DownloadDAO(conn);
            finishedDownloads = ddao.getAll(true);
            if(!finishedDownloads.isEmpty()) {
                // headline for active downloads
                OsdItem headFinished = new OsdItem("headFinished", Messages.translate(getClass(), "finished"));
                headFinished.setSelectable(false);
                addOsdItem(headFinished);
            }
            
            for (int i = 0; i < finishedDownloads.size(); i++) {
                Download download = finishedDownloads.get(i);
                String downloadId = "finished_download"+i;
                String title = download.getItem().getTitle();
                OsdItem osditem = new OsdItem(downloadId, StringUtils.escape(title));
                osditem.setUserData(download);
                osditem.registerEvent(new Event(downloadId, Event.FOCUS, null));
                osditem.registerEvent(new Event(getId(), Event.KEY_OK, null));
                osditem.addEventListener(downloadsListener);
                addOsdItem(osditem);
            }
        } catch (SQLException e) {
            logger.error("Couldn't load finished downloads from DB ", e);
        } finally {
            try {
                DbUtils.close(conn);
            } catch (SQLException e) {
                logger.error("Couldn't close database connection", e);
            }
        }
    }
    
    public static String formatActiveDownload(AbstractDownload d) {
        return d.getProgress() + "% " + d.getStatus() + " " + d.getItem().getTitle();
    }
}
