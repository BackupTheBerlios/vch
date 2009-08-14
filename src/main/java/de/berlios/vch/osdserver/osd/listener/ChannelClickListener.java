package de.berlios.vch.osdserver.osd.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.berlios.vch.i18n.Messages;
import de.berlios.vch.model.Channel;
import de.berlios.vch.osdserver.io.command.OsdMessage;
import de.berlios.vch.osdserver.io.response.Event;
import de.berlios.vch.osdserver.osd.IEventListener;
import de.berlios.vch.osdserver.osd.Menu;
import de.berlios.vch.osdserver.osd.Osd;
import de.berlios.vch.osdserver.osd.OsdItem;
import de.berlios.vch.osdserver.osd.menu.ItemsMenu;

public class ChannelClickListener implements IEventListener {

    private static transient Logger logger = LoggerFactory.getLogger(ChannelClickListener.class);
    
    private Osd osd = Osd.getInstance();
    
    @Override
    public void eventHappened(Event event) {
        if(event.getId().equals(Event.KEY_OK)) {
            try {
                osd.showMessage(new OsdMessage(Messages.translate(getClass(), "loading"), OsdMessage.STATUS));
                Channel chan = (Channel) ((OsdItem)event.getSource()).getUserData();
                Menu itemMenu = new ItemsMenu(chan);
                osd.createMenu(itemMenu);
                osd.appendToFocus(itemMenu);
                osd.setColorKeyText(itemMenu, Messages.translate(ItemListener.class, "play"), Event.KEY_GREEN);
                osd.setColorKeyText(itemMenu, Messages.translate(ItemListener.class, "download"), Event.KEY_BLUE);
                osd.showMessage(new OsdMessage("", OsdMessage.STATUSCLEAR));
                osd.show(itemMenu);
            } catch (Exception e) {
                logger.error("Couldn't load channel entries", e);
            }
        }
    }
}
