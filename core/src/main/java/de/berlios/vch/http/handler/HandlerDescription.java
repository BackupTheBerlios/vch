package de.berlios.vch.http.handler;

public class HandlerDescription {
    private String description;

    private String url;

    /**
     * @param description
     *            A description what this handler does
     * @param url
     *            The URL to invoke this handler. Can be null, if the handler is
     *            used internally only
     */
    public HandlerDescription(String description, String url) {
        super();
        this.description = description;
        this.url = url;
    }
    
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
