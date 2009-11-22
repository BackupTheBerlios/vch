package de.berlios.vch.download.osd;

import org.osgi.service.log.LogService;

import de.berlios.vch.osdserver.io.response.Event;
import de.berlios.vch.osdserver.osd.Osd;
import de.berlios.vch.osdserver.osd.OsdItem;
import de.berlios.vch.osdserver.osd.OsdObject;
import de.berlios.vch.osdserver.osd.menu.actions.IOsdAction;

public class UpdateColorButtonsAction implements IOsdAction {

    private Osd osd = Osd.getInstance();
    
    private LogService logger;
    
    public UpdateColorButtonsAction(LogService logger) {
        this.logger = logger;
    }
    
    @Override
    public void execute(OsdObject oo) {
        OsdItem item = (OsdItem) oo;
        try {
            // clear all color keys
            osd.setColorKeyText(osd.getCurrentMenu(), "", Event.KEY_RED);
            osd.setColorKeyText(osd.getCurrentMenu(), "", Event.KEY_GREEN);
            osd.setColorKeyText(osd.getCurrentMenu(), "", Event.KEY_YELLOW);
            osd.setColorKeyText(osd.getCurrentMenu(), "", Event.KEY_BLUE);
            
            // register actions for the selected item
            for (IOsdAction action : item.getRegisteredActions()) {
                osd.registerEvent(osd.getCurrentMenu(), action);
            }
            
            // rerender the menu
            osd.show(osd.getCurrentMenu());
        } catch (Exception e) {
            logger.log(LogService.LOG_ERROR, "Couldn't update color buttons", e);
        }
    }

    @Override
    public String getEvent() {
        return Event.FOCUS;
    }

    @Override
    public String getModifier() {
        return null;
    }

    @Override
    public String getName() {
        return "updates the color buttons for downloads";
    }

}
