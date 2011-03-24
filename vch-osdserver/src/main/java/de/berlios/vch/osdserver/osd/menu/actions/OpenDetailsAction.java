package de.berlios.vch.osdserver.osd.menu.actions;

import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.berlios.vch.osdserver.OsdSession;
import de.berlios.vch.osdserver.io.command.OsdMessage;
import de.berlios.vch.osdserver.io.response.Event;
import de.berlios.vch.osdserver.osd.Osd;
import de.berlios.vch.osdserver.osd.OsdItem;
import de.berlios.vch.osdserver.osd.OsdObject;
import de.berlios.vch.osdserver.osd.menu.ItemDetailsMenu;
import de.berlios.vch.osdserver.osd.menu.Menu;
import de.berlios.vch.parser.IVideoPage;
import de.berlios.vch.uri.IVchUriResolveService;

public class OpenDetailsAction implements IOsdAction {

    private static transient Logger logger = LoggerFactory.getLogger(OpenDetailsAction.class);

    private OsdSession session;

    public OpenDetailsAction(OsdSession session) {
        this.session = session;
    }

    @Override
    public void execute(OsdSession sess, OsdObject oo) {
        OsdItem item = (OsdItem) oo;
        IVideoPage page = (IVideoPage) item.getUserData();
        Osd osd = session.getOsd();
        ResourceBundle rb = session.getResourceBundle();
        try {
            IVchUriResolveService resolverService = session.getResolverService();
            if (resolverService == null) {
                osd.showMessage(new OsdMessage(rb.getString("resolver_missing"), OsdMessage.ERROR));
                return;
            }

            page = (IVideoPage) resolverService.resolve(page.getVchUri());
            Menu itemDetailsMenu = new ItemDetailsMenu(session, page);
            osd.createMenu(itemDetailsMenu);
            osd.appendToFocus(itemDetailsMenu);
            osd.showMessage(new OsdMessage("", OsdMessage.STATUSCLEAR));
            osd.show(itemDetailsMenu);
        } catch (Exception e) {
            osd.showMessageSilent(new OsdMessage(e.getLocalizedMessage(), OsdMessage.ERROR));
            logger.error("Couldn't create osd menu", e);
        }
    }

    @Override
    public String getEvent() {
        return Event.KEY_OK;
    }

    @Override
    public String getModifier() {
        return null;
    }

    @Override
    public String getName() {
        return session.getResourceBundle().getString("show_details");
    }
}
