package com.dmytrohoi.pyplugins;

import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.python.core.PyObject;

/**
 * Class to wrap python functions so they can be used to handle events
 *
 */
public class PythonEventHandler {
    /**
     * Python function to call
     */
    final PyObject handler;

    /**
     * Event type this handler is listening for
     */
    final Class<? extends Event> type;

    /**
     * Priority to register the handler at
     */
    final EventPriority priority;

    /**
     * Whether we've registered yet
     */
    boolean currentlyRegistered = false;

    /**
     * @param handler Python function to call
     * @param type Event type this handler is listening for
     * @param priority Priority to register the handler at
     */
    public PythonEventHandler(PyObject handler, Class<? extends Event> type, EventPriority priority) {
        if(handler.isCallable())
        {
            this.handler = handler;
        }
        else 
        {
            throw new IllegalArgumentException("Tried to register event handler with an invalid type " + handler.getClass().getName());
        }
        this.type = type;
        this.priority = priority;
    }
}
