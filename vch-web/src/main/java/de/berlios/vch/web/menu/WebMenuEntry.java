package de.berlios.vch.web.menu;

import java.util.SortedSet;
import java.util.TreeSet;

public class WebMenuEntry implements IWebMenuEntry {

    private String linkUri;
    private String title;
    private SortedSet<IWebMenuEntry> childs;
    private int preferredPosition;

    public WebMenuEntry() {}
    
    public WebMenuEntry(String title) {
        this.title = title;
    }
    
    @Override
    public String getLinkUri() {
        return linkUri;
    }

    @Override
    public void setLinkUri(String linkUri) {
        this.linkUri = linkUri;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public int getPreferredPosition() {
        return preferredPosition;
    }

    @Override
    public void setPreferredPosition(int preferredPosition) {
        this.preferredPosition = preferredPosition;
    }

    @Override
    public SortedSet<IWebMenuEntry> getChilds() {
        if(childs == null) {
            childs = new TreeSet<IWebMenuEntry>();
        }
        return childs;
    }

    @Override
    public void setChilds(SortedSet<IWebMenuEntry> childs) {
        this.childs = childs;
    }

    @Override
    public int compareTo(IWebMenuEntry other) {
        if(this.getPreferredPosition() < other.getPreferredPosition()) {
            return -1;
        } else if(this.getPreferredPosition() > other.getPreferredPosition()) {
            return 1;
        } else {
            return this.getTitle().compareTo(other.getTitle());
        }
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
        WebMenuEntry other = (WebMenuEntry) obj;
        if (title == null) {
            if (other.title != null)
                return false;
        } else if (!title.equals(other.title))
            return false;
        return true;
    }
    
    public IWebMenuEntry clone() {
        IWebMenuEntry clone = new WebMenuEntry();
        clone.setTitle(getTitle());
        clone.setLinkUri(getLinkUri());
        clone.setPreferredPosition(getPreferredPosition());
        if(childs != null) {
            for (IWebMenuEntry child : childs) {
                clone.getChilds().add(child.clone());
            }
        }
        return clone;
    }
}
