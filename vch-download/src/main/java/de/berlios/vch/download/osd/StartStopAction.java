package de.berlios.vch.download.osd;

import de.berlios.vch.download.AbstractDownload;
import de.berlios.vch.download.DownloadManager;
import de.berlios.vch.i18n.Messages;
import de.berlios.vch.osdserver.io.command.OsdMessage;
import de.berlios.vch.osdserver.io.response.Event;
import de.berlios.vch.osdserver.osd.Osd;
import de.berlios.vch.osdserver.osd.OsdItem;
import de.berlios.vch.osdserver.osd.OsdObject;
import de.berlios.vch.osdserver.osd.menu.actions.ItemDetailsAction;

public class StartStopAction implements ItemDetailsAction {

    private Messages i18n;
    
    private DownloadManager dm;
    
    private String name;
    
    public StartStopAction(Messages i18n, DownloadManager dm) {
        super();
        this.i18n = i18n;
        this.dm = dm;
        name = i18n.translate("I18N_DL_STOP");
    }

    @Override
    public void execute(OsdObject oo) throws Exception {
        Osd osd = Osd.getInstance();
        OsdItem item = osd.getCurrentItem();
        AbstractDownload download = (AbstractDownload) item.getUserData();
        if(download.isRunning()) {
            dm.stopDownload(download.getId());
            name = i18n.translate("I18N_DL_START");
            osd.setColorKeyText(osd.getCurrentMenu(), name, Event.KEY_GREEN);
            osd.showMessageSilent(new OsdMessage(i18n.translate("I18N_DL_STOPPED"), OsdMessage.INFO));
        } else {
            dm.startDownload(download.getId());
            name = i18n.translate("I18N_DL_STOP");
            osd.setColorKeyText(osd.getCurrentMenu(), name, Event.KEY_GREEN);
            osd.showMessageSilent(new OsdMessage(i18n.translate("I18N_DL_STARTED"), OsdMessage.INFO));
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
