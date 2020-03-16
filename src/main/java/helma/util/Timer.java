/*
 * Helma License Notice
 *
 * The contents of this file are subject to the Helma License
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://adele.helma.org/download/helma/license.txt
 *
 * Copyright 1998-2003 Helma Software. All Rights Reserved.
 *
 * $RCSfile$
 * $Author$
 * $Revision$
 * $Date$
 */

package helma.util;

import java.io.PrintStream;
import java.util.*;

/**
 * Utility class for timing a series of events
 */
public class Timer {
    private Vector timeline;
    private Hashtable events;

    /**
     * Creates a new Timer object.
     */
    public Timer() {
        timeline = new Vector();
        events = new Hashtable();
    }

    /**
     *
     */
    public void reset() {
        timeline.setSize(0);
        events.clear();
    }

    /**
     *
     *
     * @param name ...
     */
    public void beginEvent(String name) {
        timeline.addElement(name);
        events.put(name, new Event(name));
    }

    /**
     *
     *
     * @param name ...
     */
    public void endEvent(String name) {
        Event event = (Event) events.get(name);

        if (event != null) {
            event.terminate();
        }
    }

    /**
     *
     *
     * @param out ...
     */
    public void dump(PrintStream out) {
        for (int i = 0; i < timeline.size(); i++) {
            String name = (String) timeline.elementAt(i);
            Event event = (Event) events.get(name);

            out.println(event);
        }
    }

    class Event {
        String name;
        long start;
        long end;

        Event(String name) {
            this.name = name;
            start = System.currentTimeMillis();
        }

        void terminate() {
            end = System.currentTimeMillis();
        }

        public String toString() {
            long now = System.currentTimeMillis();

            if (end == 0L) {
                return (" + " + (now - start) + " " + name);
            } else {
                return ("   " + (end - start) + " " + name);
            }
        }
    }
}
