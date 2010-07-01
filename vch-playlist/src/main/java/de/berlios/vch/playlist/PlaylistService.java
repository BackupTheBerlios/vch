package de.berlios.vch.playlist;

import java.io.IOException;
import java.net.UnknownHostException;


public interface PlaylistService {
    public Playlist getPlaylist();
    public void setPlaylist(Playlist playlist);
    public void play(Playlist playlist) throws UnknownHostException, IOException;
}
