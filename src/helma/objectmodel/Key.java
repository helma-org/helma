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
public final class Key implements Serializable {

    protected String type;
    protected String id;
    private int hash;


    public Key (DbMapping dbmap, String id) {
	this.type = dbmap == null ? "" : dbmap.typename;
	this.id = id;
	hash = this.id.hashCode ();
    }

    public Key (String type, String id) {
	this.type = type;
	this.id = id;
	hash = this.id.hashCode ();
    }

    public boolean equals (Object what) {
	if (what == this)
	    return true;
	if (what == null || !(what instanceof Key))
	    return false;
	Key other = (Key) what;
             if (type == null)
	    return (id.equals (other.id) && other.type == null);
             else
                 return (id.equals (other.id) && type.equals (other.type));
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
	return new Key ("", getVirtualID (type, id, sid));
    }

    public static String getVirtualID (DbMapping pmap, String pid, String sid) {
	String ptype = pmap == null ? "" : pmap.typename;
	return ptype+"/"+pid + "*h~v*" + sid;
    }

    public static String getVirtualID (String ptype, String pid, String sid) {
	return ptype+"/"+pid + "*h~v*" + sid;
    }

    public String toString () {
	return type+"["+id+"]";
    }

}






















































