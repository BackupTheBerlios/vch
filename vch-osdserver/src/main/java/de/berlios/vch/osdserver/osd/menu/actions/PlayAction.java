package de.berlios.vch.osdserver.osd.menu.actions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.berlios.vch.i18n.Messages;
import de.berlios.vch.osdserver.OsdSession;
import de.berlios.vch.osdserver.io.command.OsdMessage;
import de.berlios.vch.osdserver.io.response.Event;
import de.berlios.vch.osdserver.osd.Osd;
import de.berlios.vch.osdserver.osd.OsdItem;
import de.berlios.vch.osdserver.osd.OsdObject;
import de.berlios.vch.parser.IVideoPage;

public class PlayAction implements IOsdAction {

    private static transient Logger logger = LoggerFactory.getLogger(PlayAction.class);
    
    private Messages i18n;
    
    private Osd osd = Osd.getInstance();
    
    public PlayAction(Messages i18n) {
        this.i18n = i18n;
    }

    @Override
    public void execute(OsdObject oo) {
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
    }

    @Override
    public String getEvent() {
        return Event.KEY_GREEN;
    }

    @Override
    public String getModifier() {
        return null;
    }

    @Override
    public String getName() {
        return i18n.translate("play");
    }

}
