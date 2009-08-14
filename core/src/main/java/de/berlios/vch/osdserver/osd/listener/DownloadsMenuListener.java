package de.berlios.vch.osdserver.osd.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.berlios.vch.download.AbstractDownload;
import de.berlios.vch.download.DownloadManager;
import de.berlios.vch.http.handler.DownloadHandler;
import de.berlios.vch.i18n.Messages;
import de.berlios.vch.model.Download;
import de.berlios.vch.model.Item;
import de.berlios.vch.osdserver.OsdSession;
import de.berlios.vch.osdserver.io.command.OsdMessage;
import de.berlios.vch.osdserver.io.response.Event;
import de.berlios.vch.osdserver.osd.IEventListener;
import de.berlios.vch.osdserver.osd.Menu;
import de.berlios.vch.osdserver.osd.Osd;
import de.berlios.vch.osdserver.osd.OsdItem;
import de.berlios.vch.osdserver.osd.menu.DownloadsMenu;
import de.berlios.vch.osdserver.osd.menu.ItemDetailsMenu;

public class DownloadsMenuListener implements IEventListener {
    
    private static transient Logger logger = LoggerFactory.getLogger(DownloadsMenuListener.class);
    
    private Osd osd = Osd.getInstance();
    
    @Override
    public void eventHappened(Event event) {
        if(event.getId().equals(Event.KEY_RED)) {
            try {
                OsdItem item = osd.getCurrentItem();
                if(item.getUserData() instanceof AbstractDownload) {
                    AbstractDownload download = (AbstractDownload) item.getUserData();
                    DownloadManager.getInstance().cancelDownload(download.getId());
                    Menu current = osd.getCurrentMenu();
                    current.removeOsdItem(item);
                    osd.refreshMenu(current);
                    osd.showMessageSilent(new OsdMessage(Messages.translate(getClass(), "download_deleted"), OsdMessage.INFO));
                } else {
                    try {
                        DownloadHandler.deleteDownload(((Download)item.getUserData()).getItemKey());
                        Menu current = osd.getCurrentMenu();
                        current.removeOsdItem(item);
                        osd.refreshMenu(current);
                        osd.showMessageSilent(new OsdMessage(Messages.translate(getClass(), "download_deleted"), OsdMessage.INFO));
                    } catch (Exception e) {
                        logger.error("Couldn't delete finished download", e);
                        osd.showMessageSilent(new OsdMessage(Messages.translate(getClass(), "error_delete_download") + e.getMessage(), OsdMessage.INFO));
                    }
                }
            } catch (Exception e) {
                logger.error("Couldn't delete download", e);
                osd.showMessageSilent(new OsdMessage(Messages.translate(getClass(), "error_delete_download"), OsdMessage.ERROR));
            }
        } else if(event.getId().equals(Event.KEY_GREEN)) {
            try {
                OsdItem item = osd.getCurrentItem();
                if(item.getUserData() instanceof AbstractDownload) {
                    AbstractDownload download = (AbstractDownload) item.getUserData();
                    if(download.isRunning()) {
                        DownloadManager.getInstance().stopDownload(download.getId());
                        osd.setColorKeyText(osd.getCurrentMenu(), Messages.translate(getClass(), "dl_start"), Event.KEY_GREEN);
                        osd.showMessageSilent(new OsdMessage(Messages.translate(getClass(), "download_stopped"), OsdMessage.INFO));
                    } else {
                        DownloadManager.getInstance().startDownload(download.getId());
                        osd.setColorKeyText(osd.getCurrentMenu(), Messages.translate(getClass(), "dl_stop"), Event.KEY_GREEN);
                        osd.showMessageSilent(new OsdMessage(Messages.translate(getClass(), "download_started"), OsdMessage.INFO));
                    }
                    String line = DownloadsMenu.formatActiveDownload(download);
                    osd.setText(item, line);
                    osd.show(osd.getCurrentMenu());
                }
            } catch (Exception e) {
                logger.error("Couldn't start download", e);
                osd.showMessageSilent(new OsdMessage(Messages.translate(getClass(), "error_start_download"), OsdMessage.ERROR));
            }
        } else if(event.getId().equals(Event.KEY_BLUE)) {
            try {
                OsdItem osditem = osd.getCurrentItem();
                OsdSession.play(osditem.getUserData());
            } catch (Exception e) {
                logger.error("Couldn't start playback", e);
                osd.showMessageSilent(new OsdMessage(Messages.translate(getClass(), "error_start_playback") + e.getMessage(), OsdMessage.ERROR));
            }
        } else if(event.getId().equals(Event.FOCUS)) {
            try {
                OsdItem item = (OsdItem) osd.getObjectById(event.getSourceId());
                if(item.getUserData() instanceof AbstractDownload) {
                    AbstractDownload download = (AbstractDownload) item.getUserData();
                    osd.setColorKeyText(osd.getCurrentMenu(), Messages.translate(getClass(), "delete"), Event.KEY_RED);
                    if(download.isPauseSupported()) {
                        if(download.isRunning()) {
                            osd.setColorKeyText(osd.getCurrentMenu(), Messages.translate(getClass(), "dl_stop"), Event.KEY_GREEN);
                        } else {
                            osd.setColorKeyText(osd.getCurrentMenu(), Messages.translate(getClass(), "dl_start"), Event.KEY_GREEN);
                        }
                    } else {
                        osd.setColorKeyText(osd.getCurrentMenu(), "", Event.KEY_GREEN);
                    }
                    osd.setColorKeyText(osd.getCurrentMenu(), Messages.translate(getClass(), "playback"), Event.KEY_BLUE);
                } else {
                    osd.setColorKeyText(osd.getCurrentMenu(), Messages.translate(getClass(), "delete"), Event.KEY_RED);
                    osd.setColorKeyText(osd.getCurrentMenu(), "", Event.KEY_GREEN);
                    osd.setColorKeyText(osd.getCurrentMenu(), Messages.translate(getClass(), "playback"), Event.KEY_BLUE);
                }
                osd.show(osd.getCurrentMenu());
            } catch (Exception e) {
                logger.error("Couldn't change color keys", e);
            }
        } else if(event.getId().equals(Event.KEY_OK)) {
            try {
                osd.showMessage(new OsdMessage(Messages.translate(ItemListener.class, "loading"), OsdMessage.STATUS));
                Item item = null;
                Object source = ((OsdItem)event.getSource()).getUserData();
                if(source instanceof AbstractDownload) {
                    item = ((AbstractDownload)source).getItem();
                } else if(source instanceof Download) {
                    item = ((Download)source).getItem();
                }
                 
                Menu itemDetailsMenu = new ItemDetailsMenu(item);
                osd.createMenu(itemDetailsMenu);
                osd.setColorKeyText(itemDetailsMenu, Messages.translate(ItemListener.class, "play"), Event.KEY_GREEN);
                osd.appendToFocus(itemDetailsMenu);
                osd.showMessage(new OsdMessage("", OsdMessage.STATUSCLEAR));
                osd.show(itemDetailsMenu);
            } catch (Exception e) {
                logger.error("Couldn't load item", e);
                osd.showMessageSilent(new OsdMessage("", OsdMessage.STATUSCLEAR));
                osd.showMessageSilent(new OsdMessage(Messages.translate(ItemListener.class, "error_load_item"), OsdMessage.ERROR));
            }
        }
    }
}
