package de.berlios.vch.download.osd;

import java.io.IOException;
import java.util.prefs.Preferences;

import org.osgi.service.log.LogService;

import de.berlios.vch.download.DownloadManager;
import de.berlios.vch.i18n.Messages;
import de.berlios.vch.osdserver.io.command.OsdMessage;
import de.berlios.vch.osdserver.io.response.Event;
import de.berlios.vch.osdserver.osd.Osd;
import de.berlios.vch.osdserver.osd.OsdException;
import de.berlios.vch.osdserver.osd.OsdObject;
import de.berlios.vch.osdserver.osd.menu.Menu;
import de.berlios.vch.osdserver.osd.menu.actions.OverviewAction;

public class OpenDownloadsAction implements OverviewAction {

    private Messages i18n;
    
    private DownloadManager dm;
    
    private Osd osd = Osd.getInstance();
    
    private LogService logger;
    
    private Preferences prefs;
    
    public OpenDownloadsAction(Messages i18n, DownloadManager dm, LogService logger, Preferences prefs) {
        super();
        this.i18n = i18n;
        this.dm = dm;
        this.logger = logger;
        this.prefs = prefs;
    }

    @Override
    public void execute(OsdObject oo) throws IOException, OsdException {
        osd.showMessage(new OsdMessage(i18n.translate("loading"), OsdMessage.STATUS));
        Menu downloadsMenu = new DownloadsMenu(dm, logger, i18n, prefs);
        osd.createMenu(downloadsMenu);
        osd.appendToFocus(downloadsMenu);
        osd.showMessage(new OsdMessage("", OsdMessage.STATUSCLEAR));
        osd.show(downloadsMenu);
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
        return i18n.translate("I18N_DOWNLOADS");
    }

}
