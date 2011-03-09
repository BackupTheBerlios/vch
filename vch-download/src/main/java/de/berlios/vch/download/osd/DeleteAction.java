package de.berlios.vch.download.osd;

import java.util.ResourceBundle;

import de.berlios.vch.download.DownloadManager;
import de.berlios.vch.download.jaxb.DownloadDTO;
import de.berlios.vch.osdserver.OsdSession;
import de.berlios.vch.osdserver.io.command.OsdMessage;
import de.berlios.vch.osdserver.io.response.Event;
import de.berlios.vch.osdserver.osd.Osd;
import de.berlios.vch.osdserver.osd.OsdItem;
import de.berlios.vch.osdserver.osd.OsdObject;
import de.berlios.vch.osdserver.osd.menu.Menu;
import de.berlios.vch.osdserver.osd.menu.actions.ItemDetailsAction;

public class DeleteAction implements ItemDetailsAction {

    private ResourceBundle rb;

    private DownloadManager dm;

    public DeleteAction(ResourceBundle rb, DownloadManager dm) {
        super();
        this.rb = rb;
        this.dm = dm;
    }

    @Override
    public void execute(OsdSession session, OsdObject oo) throws Exception {
        Osd osd = session.getOsd();
        OsdItem item = osd.getCurrentItem();
        dm.deleteDownload(((DownloadDTO) item.getUserData()).getId());
        Menu current = osd.getCurrentMenu();
        current.removeOsdItem(item);
        osd.refreshMenu(current);
        osd.showMessageSilent(new OsdMessage(rb.getString("I18N_DL_FILE_DELETED"), OsdMessage.INFO));
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
        return rb.getString("I18N_DL_DELETE");
    }

}
