package de.berlios.vch.download.osd;

import java.io.File;

import de.berlios.vch.download.AbstractDownload;
import de.berlios.vch.i18n.Messages;
import de.berlios.vch.osdserver.OsdSession;
import de.berlios.vch.osdserver.io.response.Event;
import de.berlios.vch.osdserver.osd.Osd;
import de.berlios.vch.osdserver.osd.OsdItem;
import de.berlios.vch.osdserver.osd.OsdObject;
import de.berlios.vch.osdserver.osd.menu.actions.ItemDetailsAction;
import de.berlios.vch.parser.IVideoPage;
import de.berlios.vch.playlist.Playlist;
import de.berlios.vch.playlist.PlaylistEntry;
import de.berlios.vch.playlist.PlaylistService;

public class PlayActiveAction implements ItemDetailsAction {

    private Messages i18n;
    
    private PlaylistService playlistService;
     
    public PlayActiveAction(PlaylistService playlistService, Messages i18n) {
        super();
        this.i18n = i18n;
        this.playlistService = playlistService;
    }

    @Override
    public void execute(OsdSession session, OsdObject oo) throws Exception {
        Osd osd = session.getOsd();
        OsdItem item = osd.getCurrentItem();
        AbstractDownload download = (AbstractDownload)item.getUserData();
        IVideoPage page = download.getVideoPage();
        File videoFile = new File(download.getLocalFile());
        Playlist pl = new Playlist();
        IVideoPage clone = (IVideoPage) page.clone();
        clone.setVideoUri(videoFile.getAbsoluteFile().toURI());
        pl.add(new PlaylistEntry(clone));
        session.play(pl);
    }

    @Override
    public String getEvent() {
        return Event.KEY_BLUE;
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
