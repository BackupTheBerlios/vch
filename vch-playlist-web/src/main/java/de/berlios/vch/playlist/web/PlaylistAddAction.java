package de.berlios.vch.playlist.web;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;

import de.berlios.vch.parser.IVideoPage;
import de.berlios.vch.parser.IWebPage;
import de.berlios.vch.web.IWebAction;;

@Component
@Provides
public class PlaylistAddAction implements IWebAction {

    @Override
    public String getUri(IWebPage page) throws UnsupportedEncodingException {
        IVideoPage video = (IVideoPage) page;
        String title = URLEncoder.encode(video.getTitle(), "UTF-8");
        String uri = URLEncoder.encode(video.getVideoUri().toString(), "UTF-8");
        return PlaylistServlet.PATH + "?action=add&title=" + title + "&uri=" + uri;
    }

    @Override
    public String getTitle() {
        return "add to playlist";
    }

}
