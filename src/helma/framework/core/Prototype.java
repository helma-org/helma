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
 * The Prototype class represents JavaScript prototypes defined in HOP
 * applications. This manages a prototypes templates, functions and actions 
 * as well as optional information about the mapping of this type to a 
 * relational database table.
 */


public class Prototype {

    String id;
    String name;
    Application app;
    public HashMap templates, functions, actions, skins, updatables;
    long lastUpdate;

    Prototype parent;

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
	    
	Prototype old = this.parent;
	this.parent = parent;

	// if parent has changed, update ES-prototypes in request evaluators
	if (parent != old) {
	    /* Iterator evals = app.typemgr.getRegisteredRequestEvaluators ();
	    while (evals.hasNext ()) {
	        try {
	            RequestEvaluator reval = (RequestEvaluator) evals.next ();
	            ObjectPrototype op = reval.getPrototype (getName());
	            // use hopobject (node) as prototype even if prototype is null -
	            // this is the case if no hopobject directory exists
	            ObjectPrototype opp = parent == null ?
	            	reval.esNodePrototype : reval.getPrototype (parent.getName ());
	            // don't think this is possible, but check anyway
	            if (opp == null)
	                opp = reval.esNodePrototype;
	            op.setPrototype (opp);
	        } catch (Exception ignore) {
	        }
	    } */
	}
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



    public String toString () {
	return "[Prototype "+ app.getName()+"/"+name+"]";
    }

}

