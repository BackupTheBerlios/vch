package de.berlios.vch.osdserver.osd;

import java.util.HashSet;
import java.util.Set;

import de.berlios.vch.osdserver.osd.menu.actions.IOsdAction;

public abstract class InteractiveOsdObject extends OsdObject implements IEventBased {

    private Set<IOsdAction> registeredActions = new HashSet<IOsdAction>();
    
    public InteractiveOsdObject(String id) {
        super(id);
    }    
    
    @Override
    public void registerAction(IOsdAction action) {
        registeredActions.add(action);
    }
    
    @Override
    public void unregisterAction(IOsdAction action) {
        registeredActions.remove(action);
    }
    
    @Override
    public Set<IOsdAction> getRegisteredActions() {
        return registeredActions;
    }
}
