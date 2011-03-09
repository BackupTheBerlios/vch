package de.berlios.vch.playlist.osd;

import java.util.ResourceBundle;

import de.berlios.vch.osdserver.OsdSession;
import de.berlios.vch.osdserver.io.command.OsdMessage;
import de.berlios.vch.osdserver.io.response.Event;
import de.berlios.vch.osdserver.osd.Osd;
import de.berlios.vch.osdserver.osd.OsdItem;
import de.berlios.vch.osdserver.osd.OsdObject;
import de.berlios.vch.osdserver.osd.menu.Menu;
import de.berlios.vch.osdserver.osd.menu.actions.ItemDetailsAction;
import de.berlios.vch.playlist.PlaylistService;

public class DeleteAction implements ItemDetailsAction {

    private ResourceBundle rb;

    private PlaylistService pls;

    public DeleteAction(ResourceBundle rb, PlaylistService pls) {
        super();
        this.rb = rb;
        this.pls = pls;
    }

    @Override
    public void execute(OsdSession session, OsdObject oo) throws Exception {
        Osd osd = session.getOsd();
        OsdItem item = osd.getCurrentItem();
        if (item != null) {
            pls.getPlaylist().remove(item.getUserData());
            Menu current = osd.getCurrentMenu();
            current.removeOsdItem(item);
            osd.refreshMenu(current);
            osd.showMessageSilent(new OsdMessage(rb.getString("I18N_ENTRY_DELETED"), OsdMessage.INFO));
        } else {
            osd.showMessageSilent(new OsdMessage(rb.getString("I18N_NO_ENTRY_SELECTED"), OsdMessage.WARN));
        }
    }

    @Override
    public String getEvent() {
        return Event.KEY_RED;
    }

    @Override
    public String getModifier() {
        return null;
    }

    @Override
    public String getName() {
        return rb.getString("I18N_DELETE");
    }

}
