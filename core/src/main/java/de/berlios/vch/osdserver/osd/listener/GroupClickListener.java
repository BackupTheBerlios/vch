package de.berlios.vch.osdserver.osd.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.berlios.vch.i18n.Messages;
import de.berlios.vch.model.Group;
import de.berlios.vch.osdserver.io.command.OsdMessage;
import de.berlios.vch.osdserver.io.response.Event;
import de.berlios.vch.osdserver.osd.IEventListener;
import de.berlios.vch.osdserver.osd.Menu;
import de.berlios.vch.osdserver.osd.Osd;
import de.berlios.vch.osdserver.osd.OsdItem;
import de.berlios.vch.osdserver.osd.menu.ChannelsMenu;

public class GroupClickListener implements IEventListener {

    private static transient Logger logger = LoggerFactory.getLogger(GroupClickListener.class);
    
    private Osd osd = Osd.getInstance();
    
    @Override
    public void eventHappened(Event event) {
        if(event.getId().equals(Event.KEY_OK)) {
            OsdItem item = (OsdItem) event.getSource();
            Group g = (Group) item.getUserData();
            try {
                osd.showMessage(new OsdMessage(Messages.translate(getClass(), "loading"), OsdMessage.STATUS));
                Menu channelMenu = new ChannelsMenu(g);
                osd.createMenu(channelMenu);
                osd.appendToFocus(channelMenu);
                osd.showMessage(new OsdMessage("", OsdMessage.STATUSCLEAR));
                osd.show(channelMenu);
            } catch (Exception e) {
                logger.error("Couldn't load group entries", e);
            }
        }
    }
}
