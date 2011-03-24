package de.berlios.vch.search.osd;

import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import de.berlios.vch.osdserver.ID;
import de.berlios.vch.osdserver.OsdSession;
import de.berlios.vch.osdserver.osd.OsdItem;
import de.berlios.vch.osdserver.osd.menu.Menu;
import de.berlios.vch.osdserver.osd.menu.actions.IOsdAction;
import de.berlios.vch.osdserver.osd.menu.actions.OverviewAction;
import de.berlios.vch.parser.IOverviewPage;
import de.berlios.vch.parser.IWebPage;
import de.berlios.vch.search.ISearchService;

public class SearchResultMenu extends Menu {

    public SearchResultMenu(OsdSession session, IOverviewPage searchProviders, ISearchService searchService) throws Exception {
        super(ID.randomId(), searchProviders.getTitle());

        // create overview menu entries
        for (int i = 0; i < searchProviders.getPages().size(); i++) {
            IWebPage page = searchProviders.getPages().get(i);
            String id = ID.randomId();
            OsdItem item = new OsdItem(id, page.getTitle());
            item.setUserData(page);
            if (page instanceof IOverviewPage) {
                item.registerAction(new OpenSearchResultsAction(session, searchService));
            } else {
                item.registerAction(new OpenSearchResultDetailsAction(session, searchService));
            }
            addOsdItem(item);
        }

        // register actions from other osgi bundles
        Object[] actions = getOsdActions(session.getBundleContext());
        if (actions != null) {
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
