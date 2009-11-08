package de.berlios.vch.osdserver.osd;

import java.util.Set;

import de.berlios.vch.osdserver.osd.menu.actions.IOsdAction;

public interface IEventBased {
    public void registerAction(IOsdAction action);
    
    public void unregisterAction(IOsdAction action);
    
    public Set<IOsdAction> getRegisteredActions();
}
