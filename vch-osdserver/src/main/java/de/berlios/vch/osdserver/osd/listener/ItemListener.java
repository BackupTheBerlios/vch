package de.berlios.vch.osdserver.osd.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.berlios.vch.i18n.Messages;
import de.berlios.vch.osdserver.OsdSession;
import de.berlios.vch.osdserver.io.command.OsdMessage;
import de.berlios.vch.osdserver.io.response.Event;
import de.berlios.vch.osdserver.osd.IEventListener;
import de.berlios.vch.osdserver.osd.Osd;
import de.berlios.vch.osdserver.osd.OsdItem;
import de.berlios.vch.osdserver.osd.menu.ItemDetailsMenu;
import de.berlios.vch.osdserver.osd.menu.Menu;
import de.berlios.vch.parser.IVideoPage;

public class ItemListener implements IEventListener {

    private static transient Logger logger = LoggerFactory.getLogger(ItemListener.class);

    private Osd osd = Osd.getInstance();

    private Messages i18n;

    @Override
    public void eventHappened(Event event) {
        if (event.getType().equals(Event.KEY_OK)) {
            try {
                osd.showMessage(new OsdMessage(i18n.translate("loading"), OsdMessage.STATUS));
                IVideoPage item = (IVideoPage) ((OsdItem) event.getSource()).getUserData();
                Menu itemDetailsMenu = new ItemDetailsMenu(item);
                osd.createMenu(itemDetailsMenu);
                if(item.getVideoUri() != null && !item.getVideoUri().toString().isEmpty()) {
                    osd.setColorKeyText(itemDetailsMenu, i18n.translate("play"), Event.KEY_GREEN);
                }
                // TODO activate for downloads osd.setColorKeyText(itemDetailsMenu, i18n.translate("download"), Event.KEY_BLUE);
                osd.appendToFocus(itemDetailsMenu);
                osd.showMessage(new OsdMessage("", OsdMessage.STATUSCLEAR));
                osd.show(itemDetailsMenu);
            } catch (Exception e) {
                logger.error("Couldn't load item", e);
                osd.showMessageSilent(new OsdMessage("", OsdMessage.STATUSCLEAR));
                osd.showMessageSilent(new OsdMessage(i18n.translate("error_load_item"), OsdMessage.ERROR));
            }
        } else if (event.getType().equals(Event.KEY_GREEN)) {
            try {
                OsdItem osditem = osd.getCurrentItem();
                IVideoPage page = (IVideoPage) osditem.getUserData();
                OsdSession.play(page);
            } catch (Exception e) {
                logger.error("Couldn't start video playback", e);
                osd.showMessageSilent(new OsdMessage("", OsdMessage.STATUSCLEAR));
                osd.showMessageSilent(new OsdMessage(i18n.translate("error_start_playback") + ": " + e.getMessage(),
                        OsdMessage.ERROR));
            }
        } else if (event.getType().equals(Event.KEY_BLUE)) {
            // TODO activate for downloads
//            try {
//                logger.info("Downloads not yet implemented");
//                osd.showMessageSilent(new OsdMessage(i18n.translate("starting_download"), OsdMessage.INFO));
//                //OsdItem osditem = osd.getCurrentItem();
//                // final Item item = (Item) osditem.getUserData();
//                // DownloadManager.getInstance().addDownload(item);
//            } catch (Exception e) {
//                logger.error("Couldn't start download", e);
//                osd.showMessageSilent(new OsdMessage(i18n.translate("error_start_download") + ": " + e.getMessage(),
//                        OsdMessage.ERROR));
//            }
        }
    }
}
