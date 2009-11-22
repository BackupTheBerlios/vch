package de.berlios.vch.download.osd;

import java.io.File;

import de.berlios.vch.download.AbstractDownload;
import de.berlios.vch.i18n.Messages;
import de.berlios.vch.osdserver.OsdSession;
import de.berlios.vch.osdserver.PlaylistEntry;
import de.berlios.vch.osdserver.io.response.Event;
import de.berlios.vch.osdserver.osd.Osd;
import de.berlios.vch.osdserver.osd.OsdItem;
import de.berlios.vch.osdserver.osd.OsdObject;
import de.berlios.vch.osdserver.osd.menu.actions.ItemDetailsAction;
import de.berlios.vch.parser.IVideoPage;

public class PlayActiveAction implements ItemDetailsAction {

    private Messages i18n;
     
    public PlayActiveAction(Messages i18n) {
        super();
        this.i18n = i18n;
    }

    @Override
    public void execute(OsdObject oo) throws Exception {
        Osd osd = Osd.getInstance();
        OsdItem item = osd.getCurrentItem();
        AbstractDownload download = (AbstractDownload)item.getUserData();
        IVideoPage page = download.getVideoPage();
        File videoFile = new File(download.getLocalFile());
        OsdSession.play(new PlaylistEntry(page.getTitle(), videoFile.getAbsolutePath()));
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
