package de.berlios.vch.osdserver.osd.menu.actions;

import java.util.ResourceBundle;

import org.osgi.framework.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.berlios.vch.osdserver.Activator;
import de.berlios.vch.osdserver.OsdSession;
import de.berlios.vch.osdserver.io.command.OsdMessage;
import de.berlios.vch.osdserver.io.response.Event;
import de.berlios.vch.osdserver.osd.Osd;
import de.berlios.vch.osdserver.osd.OsdItem;
import de.berlios.vch.osdserver.osd.OsdObject;
import de.berlios.vch.osdserver.osd.menu.Menu;
import de.berlios.vch.osdserver.osd.menu.OverviewMenu;
import de.berlios.vch.parser.IOverviewPage;
import de.berlios.vch.parser.service.IParserService;

public class OpenMenuAction implements IOsdAction {

    private static transient Logger logger = LoggerFactory.getLogger(OpenMenuAction.class);

    private OsdSession session;

    private ResourceBundle rb;

    public OpenMenuAction(OsdSession session) {
        this.session = session;
        this.rb = session.getResourceBundle();
    }

    @Override
    public void execute(OsdSession sess, OsdObject oo) {
        OsdItem item = (OsdItem) oo;
        IOverviewPage page = (IOverviewPage) item.getUserData();
        try {
            Osd osd = session.getOsd();
            osd.showMessage(new OsdMessage(rb.getString("loading"), OsdMessage.STATUS));
            IParserService parserService = (IParserService) Activator.parserServiceTracker.getService();
            if (parserService == null) {
                throw new ServiceException("ParserService not available");
            }
            page = (IOverviewPage) parserService.parse(page.getVchUri());

            Menu siteMenu = new OverviewMenu(session, page);
            osd.createMenu(siteMenu);
            osd.appendToFocus(siteMenu);
            osd.showMessage(new OsdMessage("", OsdMessage.STATUSCLEAR));
            osd.show(siteMenu);
        } catch (Exception e) {
            logger.error("Couldn't create osd menu", e);
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
