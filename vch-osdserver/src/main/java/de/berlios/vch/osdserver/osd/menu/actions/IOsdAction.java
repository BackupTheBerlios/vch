package de.berlios.vch.osdserver.osd.menu.actions;

import de.berlios.vch.osdserver.osd.OsdObject;

public interface IOsdAction {
    public String getName();
    public String getEvent();
    public String getModifier();
    public void execute(OsdObject oo);
}
