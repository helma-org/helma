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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Map;

/**
 *  This is the internal representation of a database key with multiple
 *  columns. It is constructed from the logical table (type) name and the
 *  column name/column value pairs that identify the key's object
 *
 *  NOTE: This class doesn't fully support the Key interface - getID always
 *  returns null since there is no unique key (at least we don't know about it).
 */
public final class MultiKey implements Key, Serializable {
    // the name of the prototype which defines the storage of this object.
    // this is the name of the object's prototype, or one of its ancestors.
    // If null, the object is stored in the embedded db.
    private String storageName;

    // the id that defines this key's object within the above storage space
    private Map parts;

    // lazily initialized hashcode
    private transient int hashcode = 0;

    static final long serialVersionUID = -9173409137561990089L;


    /**
     * Make a key for a persistent Object, describing its datasource and key parts.
     */
    public MultiKey(DbMapping dbmap, Map parts) {
        this.parts = parts;
        this.storageName = getStorageNameFromParts(dbmap, parts);
    }

    /**
     * Get the actual dbmapping prototype name out of the parts map if possible.
     * This is necessary to implement references to unspecified prototype targets.
     * @param dbmap the nominal/static dbmapping
     * @param parts the parts map
     * @return the actual dbmapping name
     */
    private String getStorageNameFromParts(DbMapping dbmap, Map parts) {
        if (dbmap == null)
            return null;
        String protoName = (String) parts.get("$prototype");
        if (protoName != null) {
            DbMapping dynamap = dbmap.app.getDbMapping(protoName);
            if (dynamap != null) {
                return (dynamap.getStorageTypeName());
            }
        }
        return dbmap.getStorageTypeName();
    }

    /**
     *
     *
     * @param what the other key to be compared with this one
     *
     * @return true if both keys are identical
     */
    public boolean equals(Object what) {
        if (what == this) {
            return true;
        }

        if (!(what instanceof MultiKey)) {
            return false;
        }

        MultiKey k = (MultiKey) what;

        // storageName is an interned string (by DbMapping, from where we got it)
        // so we can compare by using == instead of the equals method.
        return (storageName == k.storageName) &&
                ((parts == k.parts) || parts.equals(k.parts));
    }

    /**
     *
     *
     * @return this key's hash code
     */
    public int hashCode() {
        if (hashcode == 0) {
            hashcode = (storageName == null) ? (17 + (37 * parts.hashCode()))
                                             : (17 + (37 * storageName.hashCode()) +
                                             (+37 * parts.hashCode()));
        }

        return hashcode;
    }

    /**
     *
     *
     * @return the key of this key's object's parent object
     */
    public Key getParentKey() {
        return null;
    }

    /**
     *
     *
     * @return the unique storage name for this key's object
     */
    public String getStorageName() {
        return storageName;
    }

    /**
     *
     *
     * @return this key's object's id
     */
    public String getID() {
        return null;
    }

    /**
     *
     *
     * @return a string representation for this key
     */
    public String toString() {
        return (storageName == null) ? ("[" + parts + "]") : (storageName + "[" + parts + "]");
    }

    // We implement write/readObject to set storageName
    // to the interned version of the string.

    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.writeObject(storageName);
        stream.writeObject(parts);
    }

    private void readObject(ObjectInputStream stream)
                                        throws IOException, ClassNotFoundException {
        storageName = (String) stream.readObject();
        parts = (Map) stream.readObject();
        // if storageName is not null, set it to the interned version
        if (storageName != null) {
            storageName = storageName.intern();
        }
    }

}
