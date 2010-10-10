package de.berlios.vch.playlist;

import java.io.Serializable;
import java.util.UUID;

public class PlaylistEntry implements Serializable {
    
    private static final long serialVersionUID = 1L;
    private String title;
    private String url;
    private String id;

    public PlaylistEntry(String title, String url) {
        super();
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
    
    public String getId() {
        return id;
    }
}
