package de.berlios.vch.search.osd;

import java.util.ResourceBundle;

import org.osgi.service.log.LogService;

import de.berlios.vch.osdserver.OsdSession;
import de.berlios.vch.osdserver.io.command.OsdMessage;
import de.berlios.vch.osdserver.io.response.Event;
import de.berlios.vch.osdserver.osd.Osd;
import de.berlios.vch.osdserver.osd.OsdItem;
import de.berlios.vch.osdserver.osd.OsdObject;
import de.berlios.vch.osdserver.osd.menu.Menu;
import de.berlios.vch.osdserver.osd.menu.actions.IOsdAction;
import de.berlios.vch.parser.IOverviewPage;
import de.berlios.vch.search.ISearchService;

class OpenSearchResultsAction implements IOsdAction {

    private OsdSession session;

    private ResourceBundle rb;

    private ISearchService searchService;

    public OpenSearchResultsAction(OsdSession session, ISearchService searchService) {
        this.session = session;
        this.rb = session.getResourceBundle();
        this.searchService = searchService;
    }

    @Override
    public void execute(OsdSession sess, OsdObject oo) {
        OsdItem item = (OsdItem) oo;
        IOverviewPage page = (IOverviewPage) item.getUserData();
        try {
            Osd osd = session.getOsd();
            osd.showMessage(new OsdMessage(rb.getString("loading"), OsdMessage.STATUS));
            Menu siteMenu = new SearchResultMenu(session, page, searchService);
            osd.createMenu(siteMenu);
            osd.appendToFocus(siteMenu);
            osd.showMessage(new OsdMessage("", OsdMessage.STATUSCLEAR));
            osd.show(siteMenu);
        } catch (Exception e) {
            session.getLogger().log(LogService.LOG_ERROR, "Couldn't create osd menu", e);
        }
    }

    @Override
    public String getName() {
        return rb.getString("open_menu");
    }

    @Override
    public String getModifier() {
        return null;
    }

    @Override
    public String getEvent() {
        return Event.KEY_OK;
    }
}
