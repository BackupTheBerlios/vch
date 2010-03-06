package de.berlios.vch.parser;

import java.net.URI;
import java.util.Calendar;


public class VideoPage extends WebPage implements IVideoPage {

    private String description;
    
    private long duration;
    
    private Calendar publishDate;
    
    private URI thumbUri;
    
    private URI videoUri;
    
    @Override
    public String getDescription() {
        return description;
    }
    
    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public long getDuration() {
        return duration;
    }
    
    @Override
    public void setDuration(long duration) {
        this.duration = duration;
    }

    @Override
    public Calendar getPublishDate() {
        return publishDate;
    }
    
    @Override
    public void setPublishDate(Calendar publishDate) {
        this.publishDate = publishDate;
    }

    @Override
    public URI getThumbnail() {
        return thumbUri;
    }
    
    @Override
    public void setThumbnail(URI uri) {
        this.thumbUri = uri;
    }

    @Override
    public URI getVideoUri() {
        return videoUri;
    }
    
    @Override
    public void setVideoUri(URI videoUri) {
        this.videoUri = videoUri;
    }
}
