package de.berlios.vch.playlist.osd;

import de.berlios.vch.i18n.Messages;
import de.berlios.vch.osdserver.io.command.OsdMessage;
import de.berlios.vch.osdserver.io.response.Event;
import de.berlios.vch.osdserver.osd.Osd;
import de.berlios.vch.osdserver.osd.OsdItem;
import de.berlios.vch.osdserver.osd.OsdObject;
import de.berlios.vch.osdserver.osd.menu.Menu;
import de.berlios.vch.osdserver.osd.menu.actions.ItemDetailsAction;
import de.berlios.vch.playlist.PlaylistService;

public class DeleteAction implements ItemDetailsAction {

    private Messages i18n;
    
    private PlaylistService pls;
    
    public DeleteAction(Messages i18n, PlaylistService pls) {
        super();
        this.i18n = i18n;
        this.pls = pls;
    }

    @Override
    public void execute(OsdObject oo) throws Exception {
        Osd osd = Osd.getInstance();
        OsdItem item = osd.getCurrentItem();
        if(item != null) {
            pls.getPlaylist().remove(item.getUserData());
            Menu current = osd.getCurrentMenu();
            current.removeOsdItem(item);
            osd.refreshMenu(current);
            osd.showMessageSilent(new OsdMessage(i18n.translate("I18N_ENTRY_DELETED"), OsdMessage.INFO));
        } else {
            osd.showMessageSilent(new OsdMessage(i18n.translate("I18N_NO_ENTRY_SELECTED"), OsdMessage.WARN));
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
        return i18n.translate("I18N_DELETE");
    }

}
