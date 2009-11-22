package de.berlios.vch.download.osd;

import de.berlios.vch.download.DownloadManager;
import de.berlios.vch.download.jaxb.DownloadDTO;
import de.berlios.vch.i18n.Messages;
import de.berlios.vch.osdserver.io.command.OsdMessage;
import de.berlios.vch.osdserver.io.response.Event;
import de.berlios.vch.osdserver.osd.Osd;
import de.berlios.vch.osdserver.osd.OsdItem;
import de.berlios.vch.osdserver.osd.OsdObject;
import de.berlios.vch.osdserver.osd.menu.Menu;
import de.berlios.vch.osdserver.osd.menu.actions.ItemDetailsAction;

public class DeleteAction implements ItemDetailsAction {

    private Messages i18n;
    
    private DownloadManager dm;
    
    
    public DeleteAction(Messages i18n, DownloadManager dm) {
        super();
        this.i18n = i18n;
        this.dm = dm;
    }

    @Override
    public void execute(OsdObject oo) throws Exception {
        Osd osd = Osd.getInstance();
        OsdItem item = osd.getCurrentItem();
        dm.deleteDownload( ((DownloadDTO)item.getUserData()).getId() );
        Menu current = osd.getCurrentMenu();
        current.removeOsdItem(item);
        osd.refreshMenu(current);
        osd.showMessageSilent(new OsdMessage(i18n.translate("I18N_DL_FILE_DELETED"), OsdMessage.INFO));
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
        return i18n.translate("I18N_DL_DELETE");
    }

}
