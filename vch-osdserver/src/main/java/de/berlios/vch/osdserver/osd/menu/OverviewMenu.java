package de.berlios.vch.osdserver.osd.menu;

import de.berlios.vch.i18n.Messages;
import de.berlios.vch.osdserver.ID;
import de.berlios.vch.osdserver.osd.OsdItem;
import de.berlios.vch.osdserver.osd.menu.actions.OpenDetailsAction;
import de.berlios.vch.osdserver.osd.menu.actions.OpenMenuAction;
import de.berlios.vch.parser.IOverviewPage;
import de.berlios.vch.parser.IWebPage;

public class OverviewMenu extends Menu {

    public OverviewMenu(IOverviewPage overviewPage, Messages i18n) throws Exception {
        super(ID.randomId(), overviewPage.getTitle());
        
        // create overview menu entries
        for (int i = 0; i < overviewPage.getPages().size(); i++) {
            IWebPage page = overviewPage.getPages().get(i);
            String id = ID.randomId();
            OsdItem item = new OsdItem(id, page.getTitle());
            item.setUserData(page);
            if(page instanceof IOverviewPage) {
                item.registerAction(new OpenMenuAction(i18n));
            } else {
                item.registerAction(new OpenDetailsAction(i18n));
            }
            addOsdItem(item);
        }
    }
}
