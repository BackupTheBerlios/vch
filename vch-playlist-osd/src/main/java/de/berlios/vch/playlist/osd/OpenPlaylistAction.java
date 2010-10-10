package de.berlios.vch.playlist.osd;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.osgi.service.log.LogService;

import de.berlios.vch.i18n.Messages;
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

    @Requires
    private Messages i18n;
    
    @Requires
    private PlaylistService playlistService;
    
    @Requires
    private LogService logger;
    
    @Override
    public String getName() {
        return i18n.translate("I18N_PLAYLIST");
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
    public void execute(OsdObject oo) throws Exception {
        Osd osd = Osd.getInstance();
        osd.showMessage(new OsdMessage(i18n.translate("loading"), OsdMessage.STATUS));
        Menu playlistMenu = new PlaylistMenu(playlistService, logger, i18n);
        osd.createMenu(playlistMenu);
        osd.appendToFocus(playlistMenu);
        osd.showMessage(new OsdMessage("", OsdMessage.STATUSCLEAR));
        osd.show(playlistMenu);
    }
}
