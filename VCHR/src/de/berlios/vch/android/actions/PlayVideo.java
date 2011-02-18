package de.berlios.vch.android.actions;

import java.net.URI;

public class PlayVideo extends CompositeAction {

    public PlayVideo(String playlistUri, URI vchuri) {
        addAction(new ClearPlaylist(playlistUri));
        addAction(new AddToPlaylist(playlistUri, vchuri));
        addAction(new StartPlaylist(playlistUri));
    }
}
