// SyntheticKey.java
// Copyright (c) Hannes Wallnöfer 1998-2000
 
package helma.objectmodel;

import java.io.Serializable;

/**
 * This is the internal key for an object that is not - or not directly - fetched from a db,
 * but derived from another object. This is useful for all kinds of object accessed via a
 * symbolic name from another object, like objects mounted via a property name column,
  * virtual nodes and groupby nodes.
 */
public final class SyntheticKey implements Key, Serializable {

    private final Key parentKey;
    private final String name;


    /**
     * make a key for a persistent Object, describing its datasource and id.
     */
    public SyntheticKey (Key key, String name) {
	this.parentKey = key;
	this.name = name;
    }


    public boolean equals (Object what) {
	if (what == this)
	    return true;
	try {
	    SyntheticKey k = (SyntheticKey) what;
	    return parentKey.equals (k.parentKey) &&
	    	(name == k.name || name.equals (k.name));
	} catch (Exception x) {
	    return false;
	}
    }

    public int hashCode () {
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























































