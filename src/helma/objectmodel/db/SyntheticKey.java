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
     * make a key for a persistent Object, describing its datasource and id.
     */
    public SyntheticKey(Key key, String name) {
        this.parentKey = key;
        this.name = name.toLowerCase();
    }

    /**
     *
     *
     * @param what ...
     *
     * @return ...
     */
    public boolean equals(Object what) {
        if (what == this) {
            return true;
        }

        if (!(what instanceof SyntheticKey)) {
            return false;
        }

        SyntheticKey k = (SyntheticKey) what;

        return parentKey.equals(k.parentKey) &&
               ((name == k.name) || name.equals(k.name));
    }

    /**
     *
     *
     * @return ...
     */
    public int hashCode() {
        if (hashcode == 0) {
            hashcode = 17 + (37 * name.hashCode()) + (37 * parentKey.hashCode());
        }

        return hashcode;
    }

    /**
     *
     *
     * @return ...
     */
    public Key getParentKey() {
        return parentKey;
    }

    /**
     *
     *
     * @return ...
     */
    public String getID() {
        return name;
    }

    /**
     *
     *
     * @return ...
     */
    public String getStorageName() {
        return null;
    }

    /**
     *
     *
     * @return ...
     */
    public String toString() {
        return parentKey + "/" + name;
    }
}
