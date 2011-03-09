package de.berlios.vch.download.osd;

import java.io.File;
import java.util.ResourceBundle;

import de.berlios.vch.download.AbstractDownload;
import de.berlios.vch.osdserver.OsdSession;
import de.berlios.vch.osdserver.io.response.Event;
import de.berlios.vch.osdserver.osd.Osd;
import de.berlios.vch.osdserver.osd.OsdItem;
import de.berlios.vch.osdserver.osd.OsdObject;
import de.berlios.vch.osdserver.osd.menu.actions.ItemDetailsAction;
import de.berlios.vch.parser.IVideoPage;
import de.berlios.vch.playlist.Playlist;
import de.berlios.vch.playlist.PlaylistEntry;

public class PlayActiveAction implements ItemDetailsAction {

    private ResourceBundle rb;

    public PlayActiveAction(ResourceBundle rb) {
        super();
        this.rb = rb;
    }

    @Override
    public void execute(OsdSession session, OsdObject oo) throws Exception {
        Osd osd = session.getOsd();
        OsdItem item = osd.getCurrentItem();
        AbstractDownload download = (AbstractDownload) item.getUserData();
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
        return rb.getString("play");
    }

}
