package de.berlios.vch.playlist.web;

import java.util.UUID;

import de.berlios.vch.playlist.PlaylistEntry;

public class PlaylistEntryIdDecorator extends PlaylistEntry {

    private static final long serialVersionUID = 1L;
    
    private String id;
    
    public PlaylistEntryIdDecorator(String title, String url) {
        super(title, url);
        id = UUID.randomUUID().toString();
    }

    public String getId() {
        return id;
    }
}
