package de.berlios.vch.model;

import de.berlios.vch.download.AbstractDownload;

public class Download {
    private String itemKey;

    private String localFile;
    
    private Item item;

    public Download() {}
    
    public Download(AbstractDownload d) {
        setItemKey(d.getItem().getGuid());
        setLocalFile(d.getLocalFile());
    }
    
    public String getItemKey() {
        return itemKey;
    }

    public void setItemKey(String itemId) {
        this.itemKey = itemId;
    }

    public String getLocalFile() {
        return localFile;
    }

    public void setLocalFile(String localFile) {
        this.localFile = localFile;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }
}