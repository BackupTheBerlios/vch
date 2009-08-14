package de.berlios.vch.model;



public class Enclosure {
    private String link;

    private String type;
    
    /**
     * Size in bytes
     */
    private long length;
    
    /**
     * Duration in seconds
     */
    private long duration;
    
    /**
     * Enclosure has to be parsed ondemand (due to ticket systems e.g. Youtube videos)
     */
    private boolean ondemand = false;

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public boolean isOndemand() {
        return this.ondemand;
    }

    public void setOndemand(boolean ondemand) {
        this.ondemand = ondemand;
    }
}
