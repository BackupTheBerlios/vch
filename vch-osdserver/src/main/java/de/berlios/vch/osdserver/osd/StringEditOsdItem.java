package de.berlios.vch.osdserver.osd;

import java.io.IOException;

import de.berlios.vch.osdserver.OsdSession;

public class StringEditOsdItem extends OsdItem {

    private String defaultValue = "";

    private OsdSession session;

    public StringEditOsdItem(String id, String title, OsdSession session) {
        this(id, title, "", session);
    }

    public StringEditOsdItem(String id, String title, String defaultValue, OsdSession session) {
        super(id, title);
        this.defaultValue = defaultValue;
        this.session = session;
    }

    public String getText() throws IOException, OsdException {
        return session.getOsd().getText(this, true);
    }

    public String getDefaultValue() {
        return defaultValue;
    }
}
