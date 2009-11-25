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
 * This is the internal key for an object that is not - or not directly - fetched from a db,
 * but derived from another object. This is useful for all kinds of object accessed via a
 * symbolic name from another object, like objects mounted via a property name column,
 * virtual nodes and groupby nodes.
 */
public final class SyntheticKey implements Key, Serializable {

    // the parent key
    private final Key parentKey;

    // the name relative to the parent key
    private final String name;

    // lazily initialized hashcode
    private transient int hashcode = 0;

    static final long serialVersionUID = -693454133259421857L;

    /**
     * Make a symbolic key for an object using its parent key and its property name/id.
     * @param key the parent key
     * @param name the property or collection name
     */
    public SyntheticKey(Key key, String name) {
        this.parentKey = key;
        this.name = name;
    }

    /**
     * Returns true if this key equals obj
     * @param obj another object
     * @return true if obj represents the same key as this
     */
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof SyntheticKey)) {
            return false;
        }

        SyntheticKey k = (SyntheticKey) obj;

        return parentKey.equals(k.parentKey) &&
               ((name == k.name) || name.equalsIgnoreCase(k.name));
    }

    /**
     * Get the hash-code for this key
     * @return the hash-code
     */
    public int hashCode() {
        if (hashcode == 0) {
            hashcode = 17 + (37 * name.toLowerCase().hashCode()) +
                            (37 * parentKey.hashCode());
        }

        return hashcode;
    }

    /**
     * Get the parent key part of this key
     * @return the parent key
     */
    public Key getParentKey() {
        return parentKey;
    }

    /**
     * Get the ID part of this key
     * @return the id part
     */
    public String getID() {
        return name;
    }

    /**
     * Get the storage name for this key. This alwys returns null for symbolic keys.
     * @return null
     */
    public String getStorageName() {
        return null;
    }

    /**
     * Return a string representation for this key
     * @return a string representation for this key
     */
    public String toString() {
        return parentKey + "/" + name;
    }
}
