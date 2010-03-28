package de.berlios.vch.osdserver.osd.menu.actions;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.berlios.vch.i18n.Messages;
import de.berlios.vch.osdserver.Activator;
import de.berlios.vch.osdserver.io.command.OsdMessage;
import de.berlios.vch.osdserver.io.response.Event;
import de.berlios.vch.osdserver.osd.Osd;
import de.berlios.vch.osdserver.osd.OsdItem;
import de.berlios.vch.osdserver.osd.OsdObject;
import de.berlios.vch.osdserver.osd.menu.Menu;
import de.berlios.vch.osdserver.osd.menu.OverviewMenu;
import de.berlios.vch.parser.IOverviewPage;
import de.berlios.vch.parser.IParserService;

public class OpenMenuAction implements IOsdAction {

    private static transient Logger logger = LoggerFactory.getLogger(OpenMenuAction.class);

    private Messages i18n;
    
    private Osd osd = Osd.getInstance();
    
    private BundleContext ctx;

    public OpenMenuAction(BundleContext ctx, Messages i18n) {
        this.i18n = i18n;
        this.ctx = ctx;
    }
    
    @Override
    public void execute(OsdObject oo) {
        OsdItem item = (OsdItem) oo;
        IOverviewPage page = (IOverviewPage) item.getUserData();
        try {
            osd.showMessage(new OsdMessage(i18n.translate("loading"), OsdMessage.STATUS));
            IParserService parserService = (IParserService) Activator.parserServiceTracker.getService();
            if(parserService == null) {
                throw new ServiceException("ParserService not available");
            }
            page = (IOverviewPage) parserService.parse(page.getVchUri());
            
            Menu siteMenu = new OverviewMenu(ctx, (IOverviewPage) page, i18n);
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
        return i18n.translate("open_menu");
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
