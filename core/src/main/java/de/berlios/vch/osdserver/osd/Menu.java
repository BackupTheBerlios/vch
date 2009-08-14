package de.berlios.vch.osdserver.osd;

import java.util.ArrayList;
import java.util.List;

public class Menu extends InteractiveOsdObject {

    private String title;
    
    private List<OsdItem> items = new ArrayList<OsdItem>();
    
    public Menu(String id, String title) {
        super(id);
        this.title = title;
    }
    
    public void addOsdItem(OsdItem item) {
        items.add(item);
    }
    
    public void removeOsdItem(OsdItem item) {
        items.remove(item);
    }

    public List<OsdItem> getItems() {
        return items;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
    
    @Override
    public String toString() {
        return getTitle();
    }
}