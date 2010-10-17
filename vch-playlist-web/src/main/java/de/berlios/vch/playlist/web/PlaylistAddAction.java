package de.berlios.vch.playlist.web;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import de.berlios.vch.i18n.Messages;
import de.berlios.vch.parser.IVideoPage;
import de.berlios.vch.parser.IWebPage;
import de.berlios.vch.web.IWebAction;

@Component
@Provides
public class PlaylistAddAction implements IWebAction {

    @Requires
    private Messages i18n;
    
    @Override
    public String getUri(IWebPage page) throws UnsupportedEncodingException {
        IVideoPage video = (IVideoPage) page;
        String uri = URLEncoder.encode(video.getVchUri().toString(), "UTF-8");
        return PlaylistServlet.PATH + "?action=add&uri=" + uri;
    }

    @Override
    public String getTitle() {
        return i18n.translate("I18N_ADD_TO_PLAYLIST");
    }

}
