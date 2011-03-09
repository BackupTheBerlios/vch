package de.berlios.vch.playlist.osd;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.osgi.service.log.LogService;

import de.berlios.vch.i18n.ResourceBundleProvider;
import de.berlios.vch.osdserver.OsdSession;
import de.berlios.vch.osdserver.io.command.OsdMessage;
import de.berlios.vch.osdserver.io.response.Event;
import de.berlios.vch.osdserver.osd.Osd;
import de.berlios.vch.osdserver.osd.OsdItem;
import de.berlios.vch.osdserver.osd.OsdObject;
import de.berlios.vch.osdserver.osd.menu.ItemDetailsMenu;
import de.berlios.vch.osdserver.osd.menu.actions.ItemDetailsAction;
import de.berlios.vch.parser.IVideoPage;
import de.berlios.vch.playlist.Playlist;
import de.berlios.vch.playlist.PlaylistEntry;
import de.berlios.vch.playlist.PlaylistService;

@Component
@Provides
public class AddToPlaylistAction implements ItemDetailsAction {

    @Requires(filter = "(instance.name=vch.osd.playlist)")
    private ResourceBundleProvider rbp;

    @Requires
    private PlaylistService playlistService;

    @Requires
    private LogService logger;

    @Override
    public String getName() {
        return rbp.getResourceBundle().getString("I18N_ADD_TO_PLAYLIST");
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
        Playlist pl = playlistService.getPlaylist();
        if (pl == null) {
            pl = new Playlist();
            playlistService.setPlaylist(pl);
        }

        Osd osd = session.getOsd();
        ItemDetailsMenu menu = (ItemDetailsMenu) oo;
        OsdItem item = menu.getItems().get(0);
        if (item.getUserData() instanceof IVideoPage) {
            IVideoPage page = (IVideoPage) item.getUserData();
            pl.add(new PlaylistEntry(page));
            osd.showMessageSilent(new OsdMessage(rbp.getResourceBundle().getString("I18N_VIDEO_ENQUEUED"),
                    OsdMessage.INFO));
        } else {
            logger.log(LogService.LOG_WARNING, "Nothing to add");
        }
    }
}
