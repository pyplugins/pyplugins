package com.dmytrohoi.pyplugins;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Superclass for custom events in python
 */
public abstract class PythonCustomEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    
    public HandlerList getHandlers() {
        return handlers;
    }
 
    public static HandlerList getHandlerList() {
        return handlers;
    }
}
