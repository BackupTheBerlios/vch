package de.berlios.vch.download.osd;

import java.util.prefs.Preferences;

import org.osgi.service.log.LogService;

import de.berlios.vch.download.DownloadManager;
import de.berlios.vch.download.DownloadManagerImpl;
import de.berlios.vch.download.sorting.SortStrategy;
import de.berlios.vch.i18n.Messages;
import de.berlios.vch.osdserver.OsdSession;
import de.berlios.vch.osdserver.io.response.Event;
import de.berlios.vch.osdserver.osd.Osd;
import de.berlios.vch.osdserver.osd.OsdItem;
import de.berlios.vch.osdserver.osd.OsdObject;
import de.berlios.vch.osdserver.osd.menu.Menu;
import de.berlios.vch.osdserver.osd.menu.actions.IOsdAction;
import de.berlios.vch.playlist.PlaylistService;

public class ChangeOrderMenu extends Menu {

    public ChangeOrderMenu(final DownloadManager dm, final LogService logger, final Messages i18n, final Preferences prefs, final PlaylistService pls) {
        super("changeOrder", i18n.translate("I18N_SORT"));

        int i = 0;
        for (final SortStrategy strategy : DownloadManagerImpl.sortStrategies) {
            OsdItem osditem = new OsdItem("sort" + i++, strategy.getName());
            osditem.registerAction(new IOsdAction() {
                @Override
                public String getName() {
                    return strategy.getName();
                }
                
                @Override
                public String getModifier() {
                    return null;
                }
                
                @Override
                public String getEvent() {
                    return Event.KEY_OK;
                }
                
                @Override
                public void execute(OsdSession session, OsdObject oo) throws Exception {
                    prefs.put("sort.strategy", strategy.getClass().getName());
                    Osd osd = session.getOsd();
                    osd.closeMenu(); // close the sort menu
                    osd.closeMenu(); // close the downloads menu
                    // recreate the downloads menu with new sort strategy
                    DownloadsMenu menu = new DownloadsMenu(dm, logger, i18n, prefs, pls);
                    osd.createMenu(menu);
                    osd.show(menu);
                }
            });
            addOsdItem(osditem);
        }
    }
}
