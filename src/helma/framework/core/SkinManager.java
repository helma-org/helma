// SkinManager.java
// Copyright (c) Hannes Wallnöfer 2002
 
package helma.framework.core;

import java.util.HashMap;
import java.util.Iterator;
import helma.objectmodel.INode;
import java.io.*;


/**
 * Manages skins for a Helma application
 */


public class SkinManager {

    Application app;


    public SkinManager (Application app) {
	this.app = app;
    }

    public Skin getSkin (Object object, String skinname, Object[] skinpath) {
	Prototype proto = app.getPrototype (object);
	return getSkin (proto, skinname, "skin", skinpath);
    }


    public Skin getSkin (Prototype proto, String skinname, String extension, Object[] skinpath) {
	if (proto == null)
	    return null;
	Skin skin = null;
	// First check if the skin has been already used within the execution of this request
	/* SkinKey key = new SkinKey (proto.getName(), skinname, extension);
	Skin skin = (Skin) skincache.get (key);
	if (skin != null) {
	    return skin;
	} */
	// check for skinsets set via res.skinpath property
	do {
	    for (int i=0; i<skinpath.length; i++) {
	        skin = getSkinInternal (skinpath[i], proto.getName (), skinname, extension);
	        if (skin != null) {
	            // skincache.put (key, skin);
	            return skin;
	        }
	    }
	    // skin for this prototype wasn't found in the skinsets.
	    // the next step is to look if it is defined as skin file in the application directory
	    skin = proto.getSkin (skinname);
	    if (skin != null) {
	        // skincache.put (key, skin);
	        return skin;
	    }
	    // still not found. See if there is a parent prototype which might define the skin.
	    proto = proto.getParentPrototype ();
	} while (proto != null);
	// looked every where, nothing to be found
	return null;
    }


    private Skin getSkinInternal (Object skinset, String prototype, String skinname, String extension) {
	if (prototype == null || skinset == null)
	    return null;
	// check if the skinset object is a HopObject (db based skin)
	// or a String (file based skin)
	if (skinset instanceof INode) {
	    INode n = ((INode) skinset).getNode (prototype, false);
	    if (n != null) {
	        n = n.getNode (skinname, false);
	        if (n != null) {
	            String skin = n.getString (extension, false);
	            if (skin != null) {
	                Skin s = (Skin) app.skincache.get (skin);
	                if (s == null) {
	                    s = new Skin (skin, app);
	                    app.skincache.put (skin, s);
	                }
	                return s;
	            }
	        }
	    }
	} else {
	    // Skinset is interpreted as directory name from which to
	    // retrieve the skin
	    File f = new File (skinset.toString (), prototype);
	    f = new File (f, skinname+"."+extension);
	    if (f.exists() && f.canRead()) {
	        SkinFile sf = new SkinFile (f, skinname, app);
	        Skin s = sf.getSkin ();
	        return s;
	    }
	}
	// Inheritance is taken care of in the above getSkin method.
	// the sequence is prototype.skin-from-db, prototype.skin-from-file, parent.from-db, parent.from-file etc.
	return null;
    }


    /**
     *  Utility class to use for caching skins in a Hashtable.
     *  The key consists out of two strings: prototype name and skin name.
     */
    final class SkinKey {

	final String first, second, third;

	public SkinKey (String first, String second, String third) {
	    this.first = first;
	    this.second = second;
	    this.third = third;
	}

	public boolean equals (Object other) {
	    try {
	        SkinKey key = (SkinKey) other;
	        return first.equals (key.first) && second.equals (key.second) && third.equals (key.third);
	    } catch (Exception x) {
	        return false;
	    }
	}

	public int hashCode () {
	    return first.hashCode () + second.hashCode () + third.hashCode ();
	}
    }

}
