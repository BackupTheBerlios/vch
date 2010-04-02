package de.berlios.vch.download.osd;

import java.util.prefs.Preferences;

import org.osgi.service.log.LogService;

import de.berlios.vch.download.DownloadManager;
import de.berlios.vch.i18n.Messages;
import de.berlios.vch.osdserver.io.response.Event;
import de.berlios.vch.osdserver.osd.Osd;
import de.berlios.vch.osdserver.osd.OsdObject;
import de.berlios.vch.osdserver.osd.menu.Menu;
import de.berlios.vch.osdserver.osd.menu.actions.ItemDetailsAction;

public class ChangeOrderAction implements ItemDetailsAction {

    private String name;
    
    private Osd osd = Osd.getInstance();
    
    private Messages i18n;
    
    private Preferences prefs;
    
    private DownloadManager dm;
    
    private LogService logger;
    
    public ChangeOrderAction(DownloadManager dm, LogService logger, Messages i18n, Preferences prefs) {
        super();
        name = i18n.translate("I18N_SORT");
        this.i18n = i18n;
        this.prefs = prefs;
        this.dm = dm;
        this.logger = logger;
    }
    
    @Override
    public void execute(OsdObject oo) throws Exception {
        Menu sortMenu = new ChangeOrderMenu(dm, logger, i18n, prefs);
        osd.createMenu(sortMenu);
        osd.appendToFocus(sortMenu);
        osd.show(sortMenu);
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
        return name;
    }

}