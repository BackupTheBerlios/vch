package de.berlios.vch.model;


public class UserFeed extends Channel {
    private String feedUri;
    
    public UserFeed() {} // necessary for dbutils BeanHandler
    
    public UserFeed(String feedUri, Channel chan) {
        this.feedUri = feedUri;
        
        setCopyright(chan.getCopyright());
        setDescription(chan.getDescription());
        setItems(chan.getItems());
        setLanguage(chan.getLanguage());
        setLink(chan.getLink());
        setPubDate(chan.getPubDate());
        setThumbnail(chan.getThumbnail());
        setTitle(chan.getTitle());
    }
        
    public String getFeedUri() {
        return feedUri;
    }
    
    public void setFeedUri(String feedUri) {
        this.feedUri = feedUri;
    }
}