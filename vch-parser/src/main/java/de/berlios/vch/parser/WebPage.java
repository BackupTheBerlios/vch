package de.berlios.vch.parser;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class WebPage implements IWebPage {
    private String title;

    private URI uri;
    
    private URI vchUri;

    public URI getVchUri() {
        return vchUri;
    }

    public void setVchUri(URI vchUri) {
        this.vchUri = vchUri;
    }

    private Map<String, Object> userData;
    
    private String parser;

    @Override
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    @Override
    public Map<String, Object> getUserData() {
        if (userData == null) {
            userData = new HashMap<String, Object>();
        }
        return userData;
    }
    
    @Override
    public String getParser() {
        return this.parser;
    }
    
    @Override
    public void setParser(String parser) {
        this.parser = parser;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((title == null) ? 0 : title.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        WebPage other = (WebPage) obj;
        if (title == null) {
            if (other.title != null)
                return false;
        } else if (!title.equals(other.title))
            return false;
        return true;
    }
}
