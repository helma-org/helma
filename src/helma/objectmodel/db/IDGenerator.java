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
    static final long serialVersionUID = 753408631669789263L;
    private long counter;
    transient volatile boolean dirty;

    /**
     *  Builds a new IDGenerator starting with 0.
     */
    public IDGenerator() {
        this.counter = 0L;
        dirty = false;
    }

    /**
     *  Builds a new IDGenerator starting with value.
     */
    public IDGenerator(long value) {
        this.counter = value;
        dirty = false;
    }

    /**
     * Delivers a unique id and increases counter by 1.
     */
    public synchronized String newID() {
        counter += 1L;
        dirty = true;

        return Long.toString(counter);
    }

    /**
     * Set the counter to a new value
     */
    protected synchronized void setValue(long value) {
        counter = value;
        dirty = true;
    }

    /**
     * Get the current counter  value
     */
    public long getValue() {
        return counter;
    }

    /**
     *
     *
     * @return ...
     */
    public String toString() {
        return "helma.objectmodel.db.IDGenerator[counter=" + counter + ",dirty=" + dirty +
               "]";
    }
}
