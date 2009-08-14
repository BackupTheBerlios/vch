package de.berlios.vch.osdserver;

import de.berlios.vch.osdserver.io.response.Event;

public interface IEventDispatcher {
    public void dispatchEvent(Event event); 
}
