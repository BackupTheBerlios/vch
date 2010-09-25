package de.berlios.vch.playlist.osd;

import de.berlios.vch.i18n.Messages;
import de.berlios.vch.osdserver.io.response.Event;
import de.berlios.vch.osdserver.osd.Osd;
import de.berlios.vch.osdserver.osd.OsdItem;
import de.berlios.vch.osdserver.osd.OsdObject;
import de.berlios.vch.osdserver.osd.menu.actions.ItemDetailsAction;
import de.berlios.vch.playlist.Playlist;
import de.berlios.vch.playlist.PlaylistEntry;
import de.berlios.vch.playlist.PlaylistService;

public class MoveUpAction implements ItemDetailsAction {

    private Messages i18n;
    
    private PlaylistService pls;

    public MoveUpAction(Messages i18n, PlaylistService pls) {
        super();
        this.i18n = i18n;
        this.pls = pls;
    }

    @Override
    public void execute(OsdObject oo) throws Exception {
        Osd osd = Osd.getInstance();
        OsdItem item = osd.getCurrentItem();
        PlaylistEntry entry = (PlaylistEntry) item.getUserData();
        Playlist pl = pls.getPlaylist();
        int index = -1;
        if( (index = pl.indexOf(entry)) > 0) {
            pl.remove(index--);
            pl.add(index, entry);
        }
        
        PlaylistMenu playlistMenu = (PlaylistMenu) osd.getCurrentMenu();
        playlistMenu.reorder();
        osd.refreshMenu(playlistMenu);
        osd.getConnection().send(playlistMenu.getId()+".SETCURRENT " + index);
        osd.show(playlistMenu);
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
        return i18n.translate("I18N_MOVE_UP");
    }

}
