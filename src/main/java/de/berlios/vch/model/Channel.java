package de.berlios.vch.model;

import java.util.Date;
import java.util.List;

public class Channel {
    private String title;

    private String link;
    
    private String description;
    
    private String thumbnail;
    
    private String copyright;
    
    private Date pubDate;
    
    private String language;
    
    private List<Item> items;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCopyright() {
        return copyright;
    }

    public void setCopyright(String copyRight) {
        this.copyright = copyRight;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public Date getPubDate() {
        return pubDate;
    }

    public void setPubDate(Date pubDate) {
        this.pubDate = pubDate;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

	public String getThumbnail() {
		return thumbnail;
	}

	public void setThumbnail(String thumbnail) {
		this.thumbnail = thumbnail;
	}
	
	@Override
	public boolean equals(Object obj) {
	    if(obj != null && obj instanceof Channel) {
	        Channel other = (Channel) obj;
	        if(this.getLink() != null && other.getLink() != null) {
	            return this.getLink().equals(other.getLink());
	        }
	    }
	    
	    return false;
	}
}
