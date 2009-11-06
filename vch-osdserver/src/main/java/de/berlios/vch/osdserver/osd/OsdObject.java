package de.berlios.vch.osdserver.osd;

public abstract class OsdObject {
    
    private String id;
    
    public OsdObject(String id) {
        super();
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
