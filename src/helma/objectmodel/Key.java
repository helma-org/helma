// Key.java
// Copyright (c) Hannes Wallnöfer 1998-2000
 
package helma.objectmodel;


import Acme.LruHashtable;
import java.io.Serializable;

/**
 * This is the internal representation of a database key. It is constructed 
 * out of the database URL, the table name, the user name and the database 
 * key of the node and unique within each HOP application. Currently only 
 * single keys are supported.
 */
public class Key implements Serializable {

    protected String type;
    protected String id;
    private int hash;
    private static LruHashtable keycache;


    public synchronized static Key makeKey (DbMapping dbmap, String id) {
	String _type = dbmap == null ? "" : dbmap.typename;
	String _id = id.trim ();  // removed .toLowerCase() - hw
	return makeKey (_type, _id);
    }

    private synchronized static Key makeKey (String _type, String _id) {
	if (keycache == null)
	    keycache = new LruHashtable (1000, 0.9f);
	Key k = (Key) keycache.get (_type+"#"+_id);
	if (k == null) {
	    k = new Key (_type, _id);
	    keycache.put (_type+"#"+_id, k);
	}
	return k;
    }

    private Key (String type, String id) {
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
	Key virtkey = makeKey ("", getVirtualID (type, id, sid));
	return virtkey;
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






















































