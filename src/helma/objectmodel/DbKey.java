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

    // the name of the prototype which defines the storage of this object.
    // this is the name of the object's prototype, or one of its ancestors.
    // If null, the object is stored in the embedded db.
    private final String storageName;
    // the id that defines this key's object within the above storage space
    private final String id;
    // the name of the db field this key refers to. A null value means the primary key column is used.
    private final String idfield;

    /**
     * make a key for a persistent Object, describing its datasource and id.
     */
    public DbKey (DbMapping dbmap, String id) {
	this.id = id;
	this.storageName = dbmap == null ? null : dbmap.getStorageTypeName ();
	this.idfield = null;
   }

    /**
     * make a key for a persistent Object, describing its datasource and id using something
     * else than the primary key column.
     */
    public DbKey (DbMapping dbmap, String id, String idfield) {
	this.id = id;
	this.storageName = dbmap == null ? null : dbmap.getStorageTypeName ();
	this.idfield = idfield;
   }


    public boolean equals (Object what) {
	if (what == this)
	    return true;
	try {
	    DbKey k = (DbKey) what;
	    return (storageName == k.storageName || storageName.equals (k.storageName)) &&
	    	(idfield == k.idfield || idfield.equals (k.idfield)) &&
	    	(id == k.id || id.equals (k.id));
	} catch (Exception x) {
	    return false;
	}
    }

    public int hashCode () {
	return storageName == null ? id.hashCode () : storageName.hashCode() + id.hashCode ();
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

    public String getIDField () {
	return idfield;
    }

    public String toString () {
	return storageName == null ? "["+id+"]" : storageName+"["+id+"]";
    }


}























































