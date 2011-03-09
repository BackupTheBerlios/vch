package de.berlios.vch.playlist.osd;

import java.util.ResourceBundle;

import de.berlios.vch.osdserver.OsdSession;
import de.berlios.vch.osdserver.io.command.OsdMessage;
import de.berlios.vch.osdserver.io.response.Event;
import de.berlios.vch.osdserver.osd.Osd;
import de.berlios.vch.osdserver.osd.OsdItem;
import de.berlios.vch.osdserver.osd.OsdObject;
import de.berlios.vch.osdserver.osd.menu.actions.ItemDetailsAction;
import de.berlios.vch.playlist.Playlist;
import de.berlios.vch.playlist.PlaylistEntry;
import de.berlios.vch.playlist.PlaylistService;

public class MoveUpAction implements ItemDetailsAction {

    private ResourceBundle rb;

    private PlaylistService pls;

    public MoveUpAction(ResourceBundle rb, PlaylistService pls) {
        super();
        this.rb = rb;
        this.pls = pls;
    }

    @Override
    public void execute(OsdSession session, OsdObject oo) throws Exception {
        Osd osd = session.getOsd();
        OsdItem item = osd.getCurrentItem();
        if (item != null) {
            PlaylistEntry entry = (PlaylistEntry) item.getUserData();
            Playlist pl = pls.getPlaylist();
            int index = -1;
            if ((index = pl.indexOf(entry)) > 0) {
                pl.remove(index--);
                pl.add(index, entry);
            }

            PlaylistMenu playlistMenu = (PlaylistMenu) osd.getCurrentMenu();
            playlistMenu.reorder();
            osd.refreshMenu(playlistMenu);
            osd.getConnection().send(playlistMenu.getId() + ".SETCURRENT " + index);
            osd.show(playlistMenu);
        } else {
            osd.showMessageSilent(new OsdMessage(rb.getString("I18N_NO_ENTRY_SELECTED"), OsdMessage.WARN));
        }
    }

    @Override
    public String getEvent() {
        return Event.KEY_YELLOW;
    }

    @Override
    public String getModifier() {
        return null;
    }

    @Override
    public String getName() {
        return rb.getString("I18N_MOVE_UP");
    }

}
