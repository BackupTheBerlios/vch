package de.berlios.vch.osdserver.osd;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.berlios.vch.osdserver.io.response.Event;

public abstract class InteractiveOsdObject extends OsdObject implements IEventBased, IEventListener {

    private static transient Logger logger = LoggerFactory.getLogger(InteractiveOsdObject.class);
    
    private Set<Event> registeredEvents = new HashSet<Event>();
    
    private Set<IEventListener> listeners = new HashSet<IEventListener>();
    
    public InteractiveOsdObject(String id) {
        super(id);
    }    
    
    @Override
    public void addEventListener(IEventListener l) {
        listeners.add(l);
    }
    
    @Override
    public void removeEventListener(IEventListener l) {
        listeners.remove(l);
    }
    
    @Override
    public void eventHappened(Event event) {
        logger.debug("Event happened {} - {}", event.getSourceId(), event.getId());
        if(registeredEvents.contains(event)) {
            for (IEventListener l : listeners) {
                l.eventHappened(event);
            }
        }
    }
    
    @Override
    public void registerEvent(Event event) {
        registeredEvents.add(event);
    }
    
    @Override
    public void unregisterEvent(Event event) {
        registeredEvents.remove(event);
    }
    
    @Override
    public Set<Event> getRegisteredEvents() {
        return registeredEvents;
    }
    
    @Override
    public Set<IEventListener> getEventListeners() {
        return listeners;
    }
}
