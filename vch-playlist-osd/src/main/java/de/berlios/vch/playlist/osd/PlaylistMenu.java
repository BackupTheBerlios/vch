package de.berlios.vch.playlist.osd;

import java.util.ResourceBundle;

import org.osgi.service.log.LogService;

import de.berlios.vch.osdserver.ID;
import de.berlios.vch.osdserver.io.StringUtils;
import de.berlios.vch.osdserver.osd.OsdItem;
import de.berlios.vch.osdserver.osd.menu.Menu;
import de.berlios.vch.playlist.PlaylistEntry;
import de.berlios.vch.playlist.PlaylistService;

public class PlaylistMenu extends Menu {

    private PlaylistService pls;

    public PlaylistMenu(PlaylistService pls, LogService logger, ResourceBundle rb) {
        super("playlist", rb.getString("I18N_PLAYLIST"));

        this.pls = pls;

        registerAction(new DeleteAction(rb, pls));
        registerAction(new MoveUpAction(rb, pls));
        registerAction(new MoveDownAction(rb, pls));
        registerAction(new PlaybackAction(rb, pls));

        addItems();
    }

    private void addItems() {
        for (PlaylistEntry entry : pls.getPlaylist()) {
            OsdItem item = new OsdItem(ID.randomId(), StringUtils.escape(entry.getVideo().getTitle()));
            item.setUserData(entry);
            addOsdItem(item);
        }
    }

    public void reorder() {
        getItems().clear();
        addItems();
    }
}
