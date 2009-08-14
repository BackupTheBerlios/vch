package de.berlios.vch.osdserver.osd;

import java.util.Set;

import de.berlios.vch.osdserver.io.response.Event;

public interface IEventBased {
    public void addEventListener(IEventListener l);
    
    public void removeEventListener(IEventListener l);
    
    public void registerEvent(Event event);
    
    public void unregisterEvent(Event event);
    
    public Set<Event> getRegisteredEvents();
    
    public Set<IEventListener> getEventListeners();
}
