// DbKey.java
// Copyright (c) Hannes Wallnöfer 1998-2000
 
package helma.objectmodel;

import java.io.Serializable;


/**
 * This is the internal representation of a database key. It is constructed 
 * out of the database URL, the table name, the user name and the database 
 * key of the node and unique within each Helma application. Currently only
 * single keys are supported.
 */
public final class DbKey implements Key, Serializable {

    private final String storageName;
    private final String id;
    // private transient int hash;


    /**
     * make a key for a persistent Object, describing its datasource and id.
     */
    public DbKey (DbMapping dbmap, String id) {
	this.id = id;
	this.storageName = dbmap == null ? null : dbmap.getStorageTypeName ();
    }


    public boolean equals (Object what) {
	try {
	    DbKey k = (DbKey) what;
	    return (storageName == k.storageName || storageName.equals (k.storageName)) &&
	    	(id == k.id || id.equals (k.id));
	} catch (Exception x) {
	    return false;
	}
    }

    public int hashCode () {
	return storageName == null ? id.hashCode () : storageName.hashCode() + id.hashCode ();
	// return hash;
    }

    public Key getParentKey () {
	return null;
    }

    public String getStorageName () {
	return storageName;
    }

    public String getID () {
	return id;
    }


    public String toString () {
	return storageName == null ? "["+id+"]" : storageName+"["+id+"]";
    }


}























































