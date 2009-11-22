package de.berlios.vch.osdserver.osd.menu.actions;

import java.io.IOException;

import de.berlios.vch.i18n.Messages;
import de.berlios.vch.osdserver.OsdSession;
import de.berlios.vch.osdserver.PlaylistEntry;
import de.berlios.vch.osdserver.io.response.Event;
import de.berlios.vch.osdserver.osd.Osd;
import de.berlios.vch.osdserver.osd.OsdException;
import de.berlios.vch.osdserver.osd.OsdItem;
import de.berlios.vch.osdserver.osd.OsdObject;
import de.berlios.vch.parser.IVideoPage;

public class PlayAction implements IOsdAction {

    private Messages i18n;
    
    private Osd osd = Osd.getInstance();
    
    public PlayAction(Messages i18n) {
        this.i18n = i18n;
    }

    @Override
    public void execute(OsdObject oo) throws IOException, OsdException {
        OsdItem osditem = osd.getCurrentItem();
        IVideoPage page = (IVideoPage) osditem.getUserData();
        OsdSession.play(new PlaylistEntry(page.getTitle(), page.getVideoUri().toString()));
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
