package de.berlios.vch.osdserver.osd.menu.actions;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import de.berlios.vch.i18n.Messages;
import de.berlios.vch.net.INetworkProtocol;
import de.berlios.vch.osdserver.OsdSession;
import de.berlios.vch.osdserver.PlaylistEntry;
import de.berlios.vch.osdserver.io.response.Event;
import de.berlios.vch.osdserver.osd.Osd;
import de.berlios.vch.osdserver.osd.OsdException;
import de.berlios.vch.osdserver.osd.OsdItem;
import de.berlios.vch.osdserver.osd.OsdObject;
import de.berlios.vch.parser.IVideoPage;

public class PlayAction implements IOsdAction {

    private Messages i18n;
    
    private ServiceTracker protos;
    
    private Osd osd = Osd.getInstance();
    
    public PlayAction(BundleContext ctx, Messages i18n) {
        this.i18n = i18n;
        
        protos = new ServiceTracker(ctx, INetworkProtocol.class.getName(), null);
        protos.open();
    }

    @Override
    public void execute(OsdObject oo) throws IOException, OsdException, URISyntaxException {
        OsdItem osditem = osd.getCurrentItem();
        IVideoPage page = (IVideoPage) osditem.getUserData();
        URI video = page.getVideoUri();
        Object[] protocols = protos.getServices();
        for (Object object : protocols) {
            INetworkProtocol proto = (INetworkProtocol) object;
            String scheme = page.getVideoUri().getScheme();
            if(proto.getSchemes().contains(scheme)) {
                if(proto.isBridgeNeeded()) {
                    video = proto.toBridgeUri(video, page.getUserData());
                }
            }
        }
        OsdSession.play(new PlaylistEntry(page.getTitle(), video.toString()));
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
        return i18n.translate("play");
    }

}
