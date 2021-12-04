package com.dmytrohoi.pyplugins;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.python.core.Py;
import org.python.core.PyObject;

/**
 * Special listener to handle events that were registered with PythonHooks.
 *
 */
public class PythonListener implements Listener {

    /**
     * handlers registered that this listener needs to handle
     */
    HashMap<Class<? extends Event>, Set<PythonEventHandler>> handlers = new HashMap<Class<? extends Event>, Set<PythonEventHandler>>();


    void fireEvent(Event e, PythonEventHandler handler) {
        handler.handler.__call__(Py.java2py(e));
    }

    public  void addHandler(PyObject handler, Class<? extends Event> type, EventPriority priority) {
        Set<PythonEventHandler> set = this.handlers.get(type);
        PythonEventHandler pythonHandler = new PythonEventHandler(handler, type, priority);
        if(set == null) {
            set = new HashSet<PythonEventHandler>();
            handlers.put(type, set);
        }

        set.add(pythonHandler);
    }
}
