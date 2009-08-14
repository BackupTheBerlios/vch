package de.berlios.vch.osdserver.osd.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.berlios.vch.osdserver.io.response.Event;
import de.berlios.vch.osdserver.osd.IEventListener;
import de.berlios.vch.osdserver.osd.Osd;
import de.berlios.vch.osdserver.osd.menu.DownloadsMenu;
import de.berlios.vch.osdserver.osd.menu.GroupsMenu;

public class OpenDownloadsListener implements IEventListener {

    private static transient Logger logger = LoggerFactory.getLogger(OpenDownloadsListener.class);
    
    private Osd osd = Osd.getInstance();
    
    private GroupsMenu groupMenu;
    
    public OpenDownloadsListener(GroupsMenu groupMenu) {
        this.groupMenu = groupMenu;
    }

    @Override
    public void eventHappened(Event event) {
        if (event.getSource() == groupMenu && event.getId().equals(Event.KEY_BLUE)) {
            DownloadsMenu downloadsMenu = new DownloadsMenu();
            
            try {
                osd.createMenu(downloadsMenu);
                osd.appendTo(groupMenu, downloadsMenu);
                osd.show(downloadsMenu);
            } catch (Exception e) {
                logger.error("Couldn't create downloads menu", e);
            }
        }
    }
}
