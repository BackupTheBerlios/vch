package de.berlios.vch.download.osd;

import java.util.ResourceBundle;

import de.berlios.vch.download.AbstractDownload;
import de.berlios.vch.download.DownloadManager;
import de.berlios.vch.osdserver.OsdSession;
import de.berlios.vch.osdserver.io.command.OsdMessage;
import de.berlios.vch.osdserver.io.response.Event;
import de.berlios.vch.osdserver.osd.Osd;
import de.berlios.vch.osdserver.osd.OsdItem;
import de.berlios.vch.osdserver.osd.OsdObject;
import de.berlios.vch.osdserver.osd.menu.actions.ItemDetailsAction;

public class StartStopAction implements ItemDetailsAction {

    private ResourceBundle rb;

    private DownloadManager dm;

    private String name;

    public StartStopAction(ResourceBundle rb, DownloadManager dm) {
        super();
        this.rb = rb;
        this.dm = dm;
        name = rb.getString("I18N_DL_STOP");
    }

    @Override
    public void execute(OsdSession session, OsdObject oo) throws Exception {
        Osd osd = session.getOsd();
        OsdItem item = osd.getCurrentItem();
        AbstractDownload download = (AbstractDownload) item.getUserData();
        if (download.isRunning()) {
            dm.stopDownload(download.getId());
            name = rb.getString("I18N_DL_START");
            osd.setColorKeyText(osd.getCurrentMenu(), name, Event.KEY_GREEN);
            osd.showMessageSilent(new OsdMessage(rb.getString("I18N_DL_STOPPED"), OsdMessage.INFO));
        } else {
            dm.startDownload(download.getId());
            name = rb.getString("I18N_DL_STOP");
            osd.setColorKeyText(osd.getCurrentMenu(), name, Event.KEY_GREEN);
            osd.showMessageSilent(new OsdMessage(rb.getString("I18N_DL_STARTED"), OsdMessage.INFO));
        }
        String line = DownloadsMenu.formatActiveDownload(download);
        osd.setText(item, line);
        osd.show(osd.getCurrentMenu());
    }

    @Override
    public String getEvent() {
        return Event.KEY_GREEN;
    }

    @Override
    public String getModifier() {
        return null;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
