package de.berlios.vch.osdserver.osd.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.berlios.vch.download.DownloadManager;
import de.berlios.vch.i18n.Messages;
import de.berlios.vch.model.Item;
import de.berlios.vch.osdserver.OsdSession;
import de.berlios.vch.osdserver.io.command.OsdMessage;
import de.berlios.vch.osdserver.io.response.Event;
import de.berlios.vch.osdserver.osd.IEventListener;
import de.berlios.vch.osdserver.osd.Menu;
import de.berlios.vch.osdserver.osd.Osd;
import de.berlios.vch.osdserver.osd.OsdItem;
import de.berlios.vch.osdserver.osd.menu.ItemDetailsMenu;

public class ItemListener implements IEventListener {

    private static transient Logger logger = LoggerFactory.getLogger(ItemListener.class);
    
    private Osd osd = Osd.getInstance();
    
    @Override
    public void eventHappened(Event event) {
        if(event.getId().equals(Event.KEY_OK)) {
            try {
                osd.showMessage(new OsdMessage(Messages.translate(getClass(), "loading"), OsdMessage.STATUS));
                Item item = (Item) ((OsdItem)event.getSource()).getUserData();
                Menu itemDetailsMenu = new ItemDetailsMenu(item);
                osd.createMenu(itemDetailsMenu);
                osd.setColorKeyText(itemDetailsMenu, Messages.translate(getClass(), "play"), Event.KEY_GREEN);
                osd.setColorKeyText(itemDetailsMenu, Messages.translate(getClass(), "download"), Event.KEY_BLUE);
                osd.appendToFocus(itemDetailsMenu);
                osd.showMessage(new OsdMessage("", OsdMessage.STATUSCLEAR));
                osd.show(itemDetailsMenu);
            } catch (Exception e) {
                logger.error("Couldn't load item", e);
                osd.showMessageSilent(new OsdMessage("", OsdMessage.STATUSCLEAR));
                osd.showMessageSilent(new OsdMessage(Messages.translate(getClass(), "error_load_item"), OsdMessage.ERROR));
            }
        } else if(event.getId().equals(Event.KEY_GREEN)) {
            try {
                OsdItem osditem = osd.getCurrentItem();
                final Item item = (Item) osditem.getUserData();
                OsdSession.play(item);
            } catch (Exception e) {
                logger.error("Couldn't start video playback", e);
                osd.showMessageSilent(new OsdMessage("", OsdMessage.STATUSCLEAR));
                osd.showMessageSilent(new OsdMessage(Messages.translate(getClass(), "error_start_playback") + ": " +e.getMessage(), OsdMessage.ERROR));
            }
        } else if(event.getId().equals(Event.KEY_BLUE)) {
            try {
                osd.showMessageSilent(new OsdMessage(Messages.translate(getClass(), "starting_download"), OsdMessage.INFO));
                OsdItem osditem = osd.getCurrentItem();
                final Item item = (Item) osditem.getUserData();
                DownloadManager.getInstance().addDownload(item);
            } catch (Exception e) {
                logger.error("Couldn't start download", e);
                osd.showMessageSilent(new OsdMessage(Messages.translate(getClass(), "error_start_download") + ": " + e.getMessage(), OsdMessage.ERROR));
            }
        } 
    }
}
