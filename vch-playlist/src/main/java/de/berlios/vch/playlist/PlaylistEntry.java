package de.berlios.vch.playlist;

import java.io.Serializable;

public class PlaylistEntry implements Serializable {
    
    private static final long serialVersionUID = 1L;
    private String title;
    private String url;

    public PlaylistEntry(String title, String url) {
        super();
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
}
