package de.berlios.vch.osdserver.osd.menu.actions;

import java.io.IOException;
import java.net.URISyntaxException;

import org.osgi.framework.BundleContext;

import de.berlios.vch.i18n.Messages;
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
import de.berlios.vch.playlist.PlaylistService;

public class PlayAction implements IOsdAction {

    private Messages i18n;
    
    private Osd osd = Osd.getInstance();
    
    private PlaylistService playlistService;
    
    public PlayAction(BundleContext ctx, Messages i18n, PlaylistService playlistService) {
        this.i18n = i18n;
        this.playlistService = playlistService;
    }

    @Override
    public void execute(OsdObject oo) throws IOException, OsdException, URISyntaxException {
        OsdItem osditem = osd.getCurrentItem();
        IVideoPage page = (IVideoPage) osditem.getUserData();
        Osd.getInstance().showMessageSilent(new OsdMessage(i18n.translate("starting_playback"), OsdMessage.STATUS));
        Playlist pl = new Playlist();
        pl.add(new PlaylistEntry(page));
        playlistService.play(pl);
        OsdSession.stop();
        Osd.getInstance().closeMenu();
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
