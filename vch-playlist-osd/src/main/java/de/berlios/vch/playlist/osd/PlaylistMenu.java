package de.berlios.vch.playlist.osd;

import org.osgi.service.log.LogService;

import de.berlios.vch.i18n.Messages;
import de.berlios.vch.osdserver.ID;
import de.berlios.vch.osdserver.io.StringUtils;
import de.berlios.vch.osdserver.osd.OsdItem;
import de.berlios.vch.osdserver.osd.menu.Menu;
import de.berlios.vch.playlist.PlaylistEntry;
import de.berlios.vch.playlist.PlaylistService;

public class PlaylistMenu extends Menu {

    private PlaylistService pls;
    
    public PlaylistMenu(PlaylistService pls, LogService logger, Messages i18n) {
        super("playlist", i18n.translate("I18N_PLAYLIST"));
        
        this.pls = pls;
        
        registerAction(new DeleteAction(i18n, pls));
        registerAction(new MoveUpAction(i18n, pls));
        registerAction(new MoveDownAction(i18n, pls));
        registerAction(new PlaybackAction(i18n, pls));
        
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
