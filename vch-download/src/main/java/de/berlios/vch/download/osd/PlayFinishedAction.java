package de.berlios.vch.download.osd;

import de.berlios.vch.download.jaxb.DownloadDTO;
import de.berlios.vch.i18n.Messages;
import de.berlios.vch.osdserver.OsdSession;
import de.berlios.vch.osdserver.PlaylistEntry;
import de.berlios.vch.osdserver.io.response.Event;
import de.berlios.vch.osdserver.osd.Osd;
import de.berlios.vch.osdserver.osd.OsdItem;
import de.berlios.vch.osdserver.osd.OsdObject;
import de.berlios.vch.osdserver.osd.menu.actions.ItemDetailsAction;

public class PlayFinishedAction implements ItemDetailsAction {

    private Messages i18n;
     
    public PlayFinishedAction(Messages i18n) {
        super();
        this.i18n = i18n;
    }

    @Override
    public void execute(OsdObject oo) throws Exception {
        Osd osd = Osd.getInstance();
        OsdItem item = osd.getCurrentItem();
        DownloadDTO dto = (DownloadDTO)item.getUserData();
        OsdSession.play(new PlaylistEntry(dto.getTitle(), dto.getVideoFile().getAbsolutePath()));
    }

    @Override
    public String getEvent() {
        return Event.KEY_BLUE;
    }

    @Override
    public String getModifier() {
        return null;
    }

    @Override
    public String getName() {
        return i18n.translate("play");
    }

}
