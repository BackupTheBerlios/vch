package de.berlios.vch.download.osd;

import java.util.ResourceBundle;

import org.osgi.service.log.LogService;

import de.berlios.vch.download.DownloadManager;
import de.berlios.vch.osdserver.OsdSession;
import de.berlios.vch.osdserver.io.command.OsdMessage;
import de.berlios.vch.osdserver.io.response.Event;
import de.berlios.vch.osdserver.osd.Osd;
import de.berlios.vch.osdserver.osd.OsdItem;
import de.berlios.vch.osdserver.osd.OsdObject;
import de.berlios.vch.osdserver.osd.menu.ItemDetailsMenu;
import de.berlios.vch.osdserver.osd.menu.actions.ItemDetailsAction;
import de.berlios.vch.parser.IVideoPage;

public class DownloadAction implements ItemDetailsAction {

    private transient LogService logger;

    private ResourceBundle rb;

    private DownloadManager dm;

    public DownloadAction(ResourceBundle resourceBundle, DownloadManager dm, LogService logger) {
        super();
        this.rb = resourceBundle;
        this.dm = dm;
        this.logger = logger;
    }

    @Override
    public void execute(OsdSession session, OsdObject oo) throws Exception {
        Osd osd = session.getOsd();
        ItemDetailsMenu menu = (ItemDetailsMenu) oo;
        OsdItem item = menu.getItems().get(0);
        if (item.getUserData() instanceof IVideoPage) {
            IVideoPage page = (IVideoPage) item.getUserData();
            dm.downloadItem(page);
            osd.showMessageSilent(new OsdMessage(rb.getString("I18N_DL_DOWNLOAD_ADDED"), OsdMessage.INFO));
        } else {
            logger.log(LogService.LOG_WARNING, "Nothing to download");
        }
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
        return rb.getString("I18N_DL_OSD_DOWNLOAD");
    }

}
