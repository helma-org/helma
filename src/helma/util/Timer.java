// Timer.java
// Copyright (c) Hannes Wallnöfer 1999-2000

package helma.util;

import java.util.*;
import java.io.PrintStream;

/**
 * Utility class for timing a series of events
 */
 
public class Timer {

    private Vector timeline;
    private Hashtable events;

    public Timer () {
	timeline = new Vector ();
	events = new Hashtable ();
    }

    public void reset () {
	timeline.setSize (0);
	events.clear ();
    }

    public void beginEvent (String name) {
	timeline.addElement (name);
	events.put (name, new Event (name));
    }

    public void endEvent (String name) {
	Event event = (Event) events.get (name);
	if (event != null)
	    event.terminate ();
    }

    public void dump (PrintStream out) {
	for (int i=0; i<timeline.size(); i++) {
	    String name = (String) timeline.elementAt (i);
	    Event event = (Event) events.get (name);
	    out.println (event);
	}
    }

    class Event {

	String name;
	long start, end;

	Event (String name) {
	    this.name = name;
	    start = System.currentTimeMillis ();
	}

	void terminate () {
	    end = System.currentTimeMillis ();
	}

	public String toString () {
	    long now = System.currentTimeMillis ();
	    if (end == 0l)
	        return (" + "+(now-start)+" "+name);
	    else
	        return ("   "+(end-start)+" "+name);
	}
    }

}
