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
 *  This is the internal representation of a database key. It is constructed
 *  from the logical table (type) name and the object's primary key
 *  within the table. Currently only single keys are supported.
 */
public final class DbKey implements Key, Serializable {
    // the name of the prototype which defines the storage of this object.
    // this is the name of the object's prototype, or one of its ancestors.
    // If null, the object is stored in the embedded db.
    private final String storageName;

    // the id that defines this key's object within the above storage space
    private final String id;

    // lazily initialized hashcode
    private transient int hashcode = 0;

    /**
     * make a key for a persistent Object, describing its datasource and id.
     */
    public DbKey(DbMapping dbmap, String id) {
        this.id = id;
        this.storageName = (dbmap == null) ? null : dbmap.getStorageTypeName();
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

        if (!(what instanceof DbKey)) {
            return false;
        }

        DbKey k = (DbKey) what;

        // storageName is an interned string (by DbMapping, from where we got it)
        // so we can compare by using == instead of the equals method.
        return (storageName == k.storageName) && ((id == k.id) || id.equals(k.id));
    }

    /**
     *
     *
     * @return ...
     */
    public int hashCode() {
        if (hashcode == 0) {
            hashcode = (storageName == null) ? (17 + (37 * id.hashCode()))
                                             : (17 + (37 * storageName.hashCode()) +
                                             (+37 * id.hashCode()));
        }

        return hashcode;
    }

    /**
     *
     *
     * @return ...
     */
    public Key getParentKey() {
        return null;
    }

    /**
     *
     *
     * @return ...
     */
    public String getStorageName() {
        return storageName;
    }

    /**
     *
     *
     * @return ...
     */
    public String getID() {
        return id;
    }

    /**
     *
     *
     * @return ...
     */
    public String toString() {
        return (storageName == null) ? ("[" + id + "]") : (storageName + "[" + id + "]");
    }
}
