// IDGenerator.java
// Copyright (c) Hannes Wallnöfer 1997-2000

package helma.objectmodel.db;

import java.io.Serializable;

/**
 * An object that generates IDs (Strings) that are unique across the whole system. 
 * It does this keeping a simple long value which is incremented for each new ID. 
 * This is the key generation for nodes stored in the internal database, but it can 
 * also be used for relational nodes if no other mechanism is available. (Sequences 
 * in Oracle are supported, while auto-IDs are not, since the HOP has to know
 * the keys of new objects.)
 */

public final class IDGenerator implements Serializable {

    private long counter;
    transient volatile boolean dirty;
       
    static final long serialVersionUID = 753408631669789263L;


    /**
     *  Builds a new IDGenerator starting with 0.
     */ 
    public IDGenerator () {
	this.counter = 0l;
	dirty = false;
    }

    /**
     *  Builds a new IDGenerator starting with value.
     */ 
    public IDGenerator (long value) {
	this.counter = value;
	dirty = false;
    }

    /**
     * Delivers a unique id and increases counter by 1. 
     */ 
    public synchronized String newID () {
	counter += 1l;
	dirty = true;
	return Long.toString (counter);
    }

    /**
     * Set the counter to a new value
     */
    protected synchronized void setValue (long value) {
	counter = value;
	dirty = true;
    }

    /**
     * Get the current counter  value
     */
    protected long getValue () {
	return counter;
    }


    public String toString () {
	return "helma.objectmodel.db.IDGenerator[counter="+counter+",dirty="+dirty+"]";
    }

}



