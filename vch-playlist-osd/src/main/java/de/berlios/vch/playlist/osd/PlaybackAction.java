package de.berlios.vch.playlist.osd;

import de.berlios.vch.i18n.Messages;
import de.berlios.vch.osdserver.OsdSession;
import de.berlios.vch.osdserver.io.command.OsdMessage;
import de.berlios.vch.osdserver.io.response.Event;
import de.berlios.vch.osdserver.osd.Osd;
import de.berlios.vch.osdserver.osd.OsdObject;
import de.berlios.vch.osdserver.osd.menu.actions.ItemDetailsAction;
import de.berlios.vch.playlist.PlaylistService;

public class PlaybackAction implements ItemDetailsAction {

    private Messages i18n;
    
    private PlaylistService pls;
    
    public PlaybackAction(Messages i18n, PlaylistService pls) {
        super();
        this.i18n = i18n;
        this.pls = pls;
    }

    @Override
    public void execute(OsdSession session, OsdObject oo) throws Exception {
        Osd osd = session.getOsd();
        if(pls.getPlaylist().size() > 0) {
            osd.showMessageSilent(new OsdMessage(i18n.translate("I18N_STARTING_PLAYBACK"), OsdMessage.INFO));
            session.play(pls.getPlaylist());
        } else {
            osd.showMessageSilent(new OsdMessage(i18n.translate("I18N_PLAYLIST_EMPTY"), OsdMessage.WARN));
        }
    }

    @Override
    public String getEvent() {
        return Event.KEY_GREEN;
    }

    @Override
    public String getModifier() {
        return null;
    }

    @Override
    public String getName() {
        return i18n.translate("I18N_PLAY");
    }

}
