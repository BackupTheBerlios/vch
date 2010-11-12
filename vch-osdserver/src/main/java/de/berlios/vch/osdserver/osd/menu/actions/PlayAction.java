package de.berlios.vch.osdserver.osd.menu.actions;

import java.io.IOException;
import java.net.URISyntaxException;

import de.berlios.vch.osdserver.OsdSession;
import de.berlios.vch.osdserver.io.command.OsdMessage;
import de.berlios.vch.osdserver.io.response.Event;
import de.berlios.vch.osdserver.osd.Osd;
import de.berlios.vch.osdserver.osd.OsdException;
import de.berlios.vch.osdserver.osd.OsdItem;
import de.berlios.vch.osdserver.osd.OsdObject;
import de.berlios.vch.parser.IVideoPage;
import de.berlios.vch.playlist.Playlist;
import de.berlios.vch.playlist.PlaylistEntry;

public class PlayAction implements IOsdAction {

    private OsdSession session;
    
    public PlayAction(OsdSession session) {
        this.session = session;
    }

    @Override
    public void execute(OsdSession sess, OsdObject oo) throws IOException, OsdException, URISyntaxException {
    	Osd osd = session.getOsd();
        OsdItem osditem = osd.getCurrentItem();
        IVideoPage page = (IVideoPage) osditem.getUserData();
        osd.showMessageSilent(new OsdMessage(session.getI18N().translate("starting_playback"), OsdMessage.STATUS));
        Playlist pl = new Playlist();
        pl.add(new PlaylistEntry(page));
        session.play(pl);
        session.stop();
        osd.closeMenu();
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
        return session.getI18N().translate("play");
    }

}
