package de.berlios.vch.osdserver.osd;

import java.io.IOException;

public class StringEditOsdItem extends OsdItem {

    public StringEditOsdItem(String id, String title) {
        super(id, title);

    }
    
    public String getValue() throws IOException, OsdException {
        return Osd.getInstance().getStringItemValue(this);
    }
}
