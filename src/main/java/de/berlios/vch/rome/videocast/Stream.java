package de.berlios.vch.rome.videocast;

public class Stream implements Cloneable {
    private String url;
    
    private String quality;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getQuality() {
        return quality;
    }

    public void setQuality(String quality) {
        this.quality = quality;
    }
    
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
