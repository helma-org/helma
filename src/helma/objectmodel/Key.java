// Key.java
// Copyright (c) Hannes Wallnöfer 1998-2000
 
package helma.objectmodel;


import helma.util.CacheMap;
import java.io.Serializable;

/**
 * This is the internal representation of a database key. It is constructed 
 * out of the database URL, the table name, the user name and the database 
 * key of the node and unique within each HOP application. Currently only 
 * single keys are supported.
 */
public class Key implements Serializable {

    private String type;
    private String id;
    private int hash;


    public Key (DbMapping dbmap, String id) {
	this.type = dbmap == null ? null : dbmap.getStorageTypeName ();
	this.id = id;
	hash = id.hashCode ();
    }

    public Key (String type, String id) {
	this.type = type;
	this.id = id;
	hash = id.hashCode ();
    }

    public boolean equals (Object what) {
	try {
	    Key k = (Key) what;
	    return ((type == k.type || type.equals (k.type)) && (id == k.id || id.equals (k.id)));
	} catch (Exception x) {
	    return false;
	}
    }

    public int hashCode () {
	return hash;
    }


    /**
     *  Get the Key for a virtual node contained by this node, that is, a node that does
     *   not represent a record in the database. The main objective here is to generate
     *   a key that can't be mistaken for a relational db key.
     */
    public Key getVirtualKey (String sid) {
	return new Key ((String) null, makeVirtualID (type, id, sid));
    }

    public String getVirtualID (String sid) {
	return makeVirtualID (type, id, sid);
    }

    public static String makeVirtualID (DbMapping pmap, String pid, String sid) {
	return makeVirtualID (pmap == null ? (String) null : pmap.typename, pid, sid);
    }

    public static String makeVirtualID (String ptype, String pid, String sid) {
	return ptype+"/"+pid + "~" + sid;
    }

    public String getType () {
	return type;
    }

    public String getID () {
	return id;
    }

    public String toString () {
	return type+"["+id+"]";
    }


}






















































