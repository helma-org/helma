// Prototype.java
// Copyright (c) Hannes Wallnöfer 1998-2000
 
package helma.framework.core;

import java.util.HashMap;
import java.util.Iterator;
import java.io.*;
import helma.framework.*;
import helma.scripting.*;
import helma.objectmodel.*;
import helma.util.Updatable;


/**
 * The Prototype class represents Script prototypes/type defined in a Helma
 * application. This class manages a prototypes templates, functions and actions 
 * as well as optional information about the mapping of this type to a 
 * relational database table.
 */


public class Prototype {

    String id;
    String name;
    Application app;
    public HashMap templates, functions, actions, skins, updatables;
    long lastUpdate;

    private Prototype parent;

    // Tells us whether this prototype is used to script a generic Java object,
    // as opposed to a Helma objectmodel node object.
    boolean isJavaPrototype;

    public Prototype (String name, Application app) {
	// app.logEvent ("Constructing Prototype "+app.getName()+"/"+name);
	this.app = app;
	this.name = name;
	isJavaPrototype = app.isJavaPrototype (name);
	lastUpdate = 0; // System.currentTimeMillis ();
    }

    /**
     *  Return the application this prototype is a part of
     */
    public Application getApplication () {
	return app;
    }


    /**
     *  Set the parent prototype of this prototype, i.e. the prototype this one inherits from.
     */
    public void setParentPrototype (Prototype parent) {
	// this is not allowed for the hopobject and global prototypes
	if ("hopobject".equalsIgnoreCase (name) || "global".equalsIgnoreCase (name))
	    return;
	this.parent = parent;
    }

    public Prototype getParentPrototype () {
	return parent;
    }

    public Template getTemplate (String tmpname) {
	return (Template) templates.get (tmpname);
    }    
 
    public FunctionFile getFunctionFile (String ffname) {
	return (FunctionFile) functions.get (ffname);
    }

    public ActionFile getActionFile (String afname) {
	return (ActionFile) actions.get (afname);
    }

    public SkinFile getSkinFile (String sfname) {
	return (SkinFile) skins.get (sfname);
    }

    public Skin getSkin (String sfname) {
	SkinFile sf = (SkinFile) skins.get (sfname);
	if (sf != null)
	    return sf.getSkin ();
	else
	    return null;
    }


    public String getName () {
	return name;
    }

    Updatable[] upd = null;
    public Updatable[] getUpdatables () {
	if (upd == null) {
	    upd = new Updatable[updatables.size()];
	    int i = 0;
	    for (Iterator it = updatables.values().iterator(); it.hasNext(); ) {
	        upd[i++] = (Updatable) it.next();
	    }
	}
	return upd;
    }

    public long getLastUpdate () {
	return lastUpdate;
    }
	
    public void markUpdated () {
	lastUpdate = System.currentTimeMillis ();
    }

    public String toString () {
	return "[Prototype "+ app.getName()+"/"+name+"]";
    }

}

