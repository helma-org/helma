// SyntheticKey.java
// Copyright (c) Hannes Wallnöfer 1998-2000
 
package helma.objectmodel;

import java.io.Serializable;

/**
 * This is the internal key for an object that is not stored in a db, but generated
 * on the fly. Currently there are two kinds of such objects: virtual nodes, which are used
 * as utility containers for objects in the database, and groupby nodes, which are used
 * to group a certain kind of relational objects according to some property.
 */
public final class SyntheticKey implements Key, Serializable {

    private final Key parentKey;
    private final String name;
    // private final int hash;


    /**
     * make a key for a persistent Object, describing its datasource and id.
     */
    public SyntheticKey (Key key, String name) {
	this.parentKey = key;
	this.name = name;
	// hash = name.hashCode () + key.hashCode ();
    }


    public boolean equals (Object what) {
	try {
	    SyntheticKey k = (SyntheticKey) what;
	    return parentKey.equals (k.parentKey) &&
	    	(name == k.name || name.equals (k.name));
	} catch (Exception x) {
	    System.err.println ("SYNTHETIC NOT EQUAL: "+what+" - "+this);
	    return false;
	}
    }

    public int hashCode () {
	// return hash;
	return name.hashCode () + parentKey.hashCode ();
    }


    public Key getParentKey () {
	return parentKey;
    }

    public String getID () {
	return name;
    }

    public String getStorageName () {
	return null;
    }

    public String toString () {
	return parentKey+"/"+name;
    }


}























































