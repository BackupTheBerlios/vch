package de.berlios.vch.parser.rss;

public class Feed {
    private String id;
    private String title;
    private String uri;

    public Feed(String id, String title, String uri) {
        super();
        this.id = id;
        this.title = title;
        this.uri = uri;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }
}
