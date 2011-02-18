package de.berlios.vch.osdserver.osd.menu;

import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import de.berlios.vch.osdserver.ID;
import de.berlios.vch.osdserver.OsdSession;
import de.berlios.vch.osdserver.osd.OsdItem;
import de.berlios.vch.osdserver.osd.menu.actions.IOsdAction;
import de.berlios.vch.osdserver.osd.menu.actions.OpenDetailsAction;
import de.berlios.vch.osdserver.osd.menu.actions.OpenMenuAction;
import de.berlios.vch.osdserver.osd.menu.actions.OverviewAction;
import de.berlios.vch.parser.IOverviewPage;
import de.berlios.vch.parser.IWebPage;

public class OverviewMenu extends Menu {

    public OverviewMenu(OsdSession session, IOverviewPage overviewPage) throws Exception {
        super(ID.randomId(), overviewPage.getTitle());
        
        // create overview menu entries
        for (int i = 0; i < overviewPage.getPages().size(); i++) {
            IWebPage page = overviewPage.getPages().get(i);
            String id = ID.randomId();
            OsdItem item = new OsdItem(id, page.getTitle());
            item.setUserData(page);
            if(page instanceof IOverviewPage) {
                item.registerAction(new OpenMenuAction(session));
            } else {
                item.registerAction(new OpenDetailsAction(session));
            }
            addOsdItem(item);
        }
        
        // register actions from other osgi bundles
        Object[] actions = getOsdActions(session.getBundleContext());
        if(actions != null) {
            for (Object a : actions) {
                IOsdAction action = (IOsdAction) a;
                registerAction(action);
            }
        }
    }
    
    private Object[] getOsdActions(BundleContext ctx) {
        ServiceTracker st = new ServiceTracker(ctx, OverviewAction.class.getName(), null);
        st.open();
        Object[] actions = st.getServices();
        st.close();
        return actions;
    }
}
