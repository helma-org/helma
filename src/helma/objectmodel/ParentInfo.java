// ParentInfo.java
// Copyright (c) Hannes Wallnöfer 1999-2000

package helma.objectmodel;


/**
 *  This class describes a parent relation between releational nodes.
 */

public class ParentInfo {

    public final String propname;
    public final String virtualname;
    public final boolean named;
    public final boolean isroot;


    public ParentInfo (String desc) {
	int n = desc.indexOf ("[named]");
	named = n > -1;
	String d = named ? desc.substring (0, n) : desc;

	int dot = d.indexOf (".");
	if (dot > -1) {
	    propname = d.substring (0, dot).trim();
	    virtualname = d.substring (dot+1).trim();
	} else {
	    propname = d.trim();
	    virtualname = null;
	}
	
	isroot = "root".equals (propname);
	// System.err.println ("created "+this);
    }	

    public String toString () {
	return "ParentInfo["+propname+","+virtualname+","+named+"]";
    }

}
