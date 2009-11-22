package de.berlios.vch.download.osd;

import java.util.List;

import org.osgi.service.log.LogService;

import de.berlios.vch.download.Download;
import de.berlios.vch.download.DownloadManager;
import de.berlios.vch.download.jaxb.DownloadDTO;
import de.berlios.vch.i18n.Messages;
import de.berlios.vch.osdserver.io.StringUtils;
import de.berlios.vch.osdserver.osd.OsdItem;
import de.berlios.vch.osdserver.osd.menu.Menu;

public class DownloadsMenu extends Menu {

    public DownloadsMenu(DownloadManager dm, LogService logger, Messages i18n) {
        super("downloads", i18n.translate("I18N_DOWNLOADS"));

        DeleteAction deleteAction = new DeleteAction(i18n, dm);
        CancelAction cancelAction = new CancelAction(i18n, dm);
        UpdateColorButtonsAction updateColorButtons = new UpdateColorButtonsAction(logger);
        PlayFinishedAction playFinished = new PlayFinishedAction(i18n);
        PlayActiveAction playActive = new PlayActiveAction(i18n);
        StartStopAction startStop = new StartStopAction(i18n, dm);
        
        // add the active downloads to the menu
        List<Download> activeDownloads = dm.getActiveDownloads();
        if(!activeDownloads.isEmpty()) {
            // headline for active downloads
            OsdItem headActive = new OsdItem("headActive", i18n.translate("I18N_DOWNLOADS"));
            headActive.setSelectable(false);
            addOsdItem(headActive);
        }
        for (int i = 0; i < activeDownloads.size(); i++) {
            Download download = activeDownloads.get(i);
            String downloadId = "download"+i;
            String line = formatActiveDownload(download);
            OsdItem osditem = new OsdItem(downloadId, line);
            osditem.setUserData(download);
            osditem.registerAction(updateColorButtons);
            osditem.registerAction(playActive);
            osditem.registerAction(cancelAction);
            if(download.isPauseSupported()) {
                startStop.setName(download.isRunning() ? i18n.translate("I18N_DL_STOP") : i18n.translate("I18N_DL_START"));
                osditem.registerAction(startStop);
            } 
            addOsdItem(osditem);
        }
        
        // add the finished downloads to the menu
        List<DownloadDTO> finishedDownloads = dm.getFinishedDownloads();
        if(!finishedDownloads.isEmpty()) {
            // headline for active downloads
            OsdItem headFinished = new OsdItem("headFinished", i18n.translate("I18N_DL_FINISHED_DOWNLOADS"));
            headFinished.setSelectable(false);
            addOsdItem(headFinished);
        }
        for (int i = 0; i < finishedDownloads.size(); i++) {
            DownloadDTO download = finishedDownloads.get(i);
            String downloadId = "finished_download"+i;
            String title = download.getTitle();
            OsdItem osditem = new OsdItem(downloadId, StringUtils.escape(title));
            osditem.setUserData(download);
            osditem.registerAction(updateColorButtons);
            osditem.registerAction(deleteAction);
            osditem.registerAction(playFinished);
            addOsdItem(osditem);
        }
    }
    
    public static String formatActiveDownload(Download d) {
        return d.getProgress() + "% " + d.getStatus() + " " + d.getVideoPage().getTitle();
    }
}
