package de.berlios.vch.osdserver.osd;

import de.berlios.vch.osdserver.io.response.Event;

public interface IEventListener {
    public void eventHappened(Event event);
}
