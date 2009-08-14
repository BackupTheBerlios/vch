package de.berlios.vch.osdserver.osd;


public class OsdItem extends InteractiveOsdObject {

    private Object userData;
    
    private String title;
    
    private boolean selectable = true;
    
    public OsdItem(String id, String title) {
        super(id);
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Object getUserData() {
        return userData;
    }

    public void setUserData(Object userData) {
        this.userData = userData;
    }

    public boolean isSelectable() {
        return selectable;
    }

    public void setSelectable(boolean selectable) {
        this.selectable = selectable;
    }
}