package de.berlios.vch.osdserver.osd.menu;

import de.berlios.vch.i18n.Messages;
import de.berlios.vch.osdserver.ID;
import de.berlios.vch.osdserver.io.response.Event;
import de.berlios.vch.osdserver.osd.OsdItem;
import de.berlios.vch.osdserver.osd.listener.OverviewClickListener;
import de.berlios.vch.parser.IOverviewPage;
import de.berlios.vch.parser.IWebPage;

public class OverviewMenu extends Menu {

    private OverviewClickListener overviewClickListener;
    
    //private OpenDownloadsListener openDownloadsListener;
    
    public OverviewMenu(IOverviewPage overviewPage, Messages i18n) throws Exception {
        super(ID.randomId(), overviewPage.getTitle());
        
        //openDownloadsListener = new OpenDownloadsListener(this);
        
        // TODO activate for downloads registerEvent(new Event(getId(), Event.KEY_BLUE, null));
        //addEventListener(openDownloadsListener);
        
        // create overview menu entries
        overviewClickListener = new OverviewClickListener(i18n);
        for (int i = 0; i < overviewPage.getPages().size(); i++) {
            IWebPage page = overviewPage.getPages().get(i);
            String id = ID.randomId();
            OsdItem item = new OsdItem(id, page.getTitle());
            item.registerEvent(new Event(id, Event.KEY_OK, null));
            item.setUserData(page);
            item.addEventListener(overviewClickListener);
            addOsdItem(item);
        }
    }
}
