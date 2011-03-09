package de.berlios.vch.playlist.osd;

import java.util.ResourceBundle;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.osgi.service.log.LogService;

import de.berlios.vch.i18n.ResourceBundleProvider;
import de.berlios.vch.osdserver.OsdSession;
import de.berlios.vch.osdserver.io.command.OsdMessage;
import de.berlios.vch.osdserver.io.response.Event;
import de.berlios.vch.osdserver.osd.Osd;
import de.berlios.vch.osdserver.osd.OsdObject;
import de.berlios.vch.osdserver.osd.menu.Menu;
import de.berlios.vch.osdserver.osd.menu.actions.OverviewAction;
import de.berlios.vch.playlist.PlaylistService;

@Component
@Provides
public class OpenPlaylistAction implements OverviewAction {

    @Requires(filter = "(instance.name=vch.osd.playlist)")
    private ResourceBundleProvider rbp;

    @Requires
    private PlaylistService playlistService;

    @Requires
    private LogService logger;

    @Override
    public String getName() {
        return rbp.getResourceBundle().getString("I18N_PLAYLIST");
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
    public void execute(OsdSession session, OsdObject oo) throws Exception {
        Osd osd = session.getOsd();
        ResourceBundle rb = rbp.getResourceBundle();
        osd.showMessage(new OsdMessage(rb.getString("I18N_LOADING"), OsdMessage.STATUS));
        Menu playlistMenu = new PlaylistMenu(playlistService, logger, rb);
        osd.createMenu(playlistMenu);
        osd.appendToFocus(playlistMenu);
        osd.showMessage(new OsdMessage("", OsdMessage.STATUSCLEAR));
        osd.show(playlistMenu);
    }
}
