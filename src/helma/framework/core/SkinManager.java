// SkinManager.java
// Copyright (c) Hannes Wallnöfer 2002
 
package helma.framework.core;

import java.util.Map;
import java.util.Iterator;
import helma.objectmodel.INode;
import java.io.*;


/**
 * Manages skins for a Helma application
 */


public final class SkinManager {

    Application app;

    public SkinManager (Application app) {
	this.app = app;
    }

    public Skin getSkin (Object object, String skinname, Object[] skinpath) {
	Prototype proto = app.getPrototype (object);
	Skin skin = getSkin (proto, skinname, "skin", skinpath);
	return skin;
    }


    protected Skin getSkin (Prototype proto, String skinname, String extension, Object[] skinpath) {
	if (proto == null)
	    return null;
	Skin skin = null;
	// First check if the skin has been already used within the execution of this request
	// check for skinsets set via res.skinpath property
	do {
	    if (skinpath != null) {
	        for (int i=0; i<skinpath.length; i++) {
	            skin = getSkinInternal (skinpath[i], proto.getName (), skinname, extension);
	            if (skin != null) {
	                return skin;
	            }
	        }
	    }
	    // skin for this prototype wasn't found in the skinsets.
	    // the next step is to look if it is defined as skin file in the application directory
	    skin = proto.getSkin (skinname);
	    if (skin != null) {
	        return skin;
	    }
	    // still not found. See if there is a parent prototype which might define the skin.
	    proto = proto.getParentPrototype ();
	} while (proto != null);
	// looked every where, nothing to be found
	return null;
    }


    protected Skin getSkinInternal (Object skinset, String prototype, String skinname, String extension) {
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
	                return new Skin (skin, app);
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
	        return sf.getSkin ();
	    }
	}
	// Inheritance is taken care of in the above getSkin method.
	// the sequence is prototype.skin-from-db, prototype.skin-from-file, parent.from-db, parent.from-file etc.
	return null;
    }

}
