// Prototype.java
// Copyright (c) Hannes Wallnöfer 1998-2000
 
package helma.framework.core;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.io.*;
import helma.framework.*;
import helma.scripting.*;
import helma.objectmodel.*;
import helma.objectmodel.db.DbMapping;
import helma.util.Updatable;


/**
 * The Prototype class represents Script prototypes/type defined in a Helma
 * application. This class manages a prototypes templates, functions and actions
 * as well as optional information about the mapping of this type to a
 * relational database table.
 */


public final class Prototype {

    final String name;
    final Application app;

    final HashMap templates;
    final HashMap functions;
    final HashMap actions;
    final HashMap skins;
    final HashMap updatables;
    
    DbMapping dbmap;

    // lastCheck is the time the prototype's files were last checked
    private long lastCheck;
    // lastUpdate is the time at which any of the prototype's files were
    // found updated the last time
    private long lastUpdate;

    private Prototype parent;

    // Tells us whether this prototype is used to script a generic Java object,
    // as opposed to a Helma objectmodel node object.
    boolean isJavaPrototype;

    public Prototype (String name, Application app) {
	// app.logEvent ("Constructing Prototype "+app.getName()+"/"+name);
	this.app = app;
	this.name = name;

	templates = new HashMap ();
	functions = new HashMap ();
	actions = new HashMap ();
	skins = new HashMap ();
	updatables = new HashMap ();

	isJavaPrototype = app.isJavaPrototype (name);
	lastUpdate = lastCheck = 0;
    }

    /**
     *  Return the application this prototype is a part of
     */
    public Application getApplication () {
	return app;
    }


    /**
     *  Set the parent prototype of this prototype, i.e. the prototype this
     *  prototype inherits from.
     */
    public void setParentPrototype (Prototype parent) {
	// this is not allowed for the hopobject and global prototypes
	if ("hopobject".equalsIgnoreCase (name) || "global".equalsIgnoreCase (name))
	    return;
	this.parent = parent;
    }

    /**
     *  Get the parent prototype from which we inherit, or null
     *  if we are top of the line.
     */
    public Prototype getParentPrototype () {
	return parent;
    }

    public void setDbMapping (DbMapping dbmap) {
	this.dbmap = dbmap;
    }
    
    public DbMapping getDbMapping () {
	return dbmap;
    }

    /**
     *  Get a template defined for this prototype. Templates
     *  are files that mix layout and code and were used
     *  before skins came along. Think of them as legacy.
     */
    public Template getTemplate (String tmpname) {
	return (Template) templates.get (tmpname);
    }

    /**
     *  Get a generic function file defined for this prototype.
     */
    public FunctionFile getFunctionFile (String ffname) {
	return (FunctionFile) functions.get (ffname);
    }

    /**
     *  Get an action file defined for this prototype. Action
     *  files are functions with a .hac extension
     *  that are accessible publicly via web interface.
     */
    public ActionFile getActionFile (String afname) {
	return (ActionFile) actions.get (afname);
    }

    /**
     *  Get a Skinfile for this prototype. This only works for skins
     *  residing in the prototype directory, not for skin files in
     *  other locations or database stored skins.
     */
    public SkinFile getSkinFile (String sfname) {
	return (SkinFile) skins.get (sfname);
    }

    /**
     *  Get a skin for this prototype. This only works for skins
     *  residing in the prototype directory, not for skins files in
     *  other locations or database stored skins.
     */
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

    /**
     *  Get the last time any script has been re-read for this prototype.
     */
    public long getLastUpdate () {
	return lastUpdate;
    }

    /**
     *  Signal that some script in this prototype has been
     *  re-read from disk and needs to be re-compiled by
     *  the evaluators.
     */
    public void markUpdated () {
	lastUpdate = System.currentTimeMillis ();
    }

    /**
     *  Get the time at which this prototype's scripts were checked
     *  for changes for the last time.
     */
    public long getLastCheck () {
	return lastCheck;
    }

    /**
     *  Signal that the prototype's scripts have been checked for 
     *  changes.
     */
    public void markChecked () {
	lastCheck = System.currentTimeMillis ();
    }
    
    /**
     *  Return a clone of this prototype's actions container. Synchronized
     *  to not return a map in a transient state where it is just being
     *  updated by the type manager.
     */
    public synchronized Map getActions () {
	return (Map) actions.clone();
    }

    /**
     *  Return a clone of this prototype's functions container. Synchronized
     *  to not return a map in a transient state where it is just being
     *  updated by the type manager.
     */
    public synchronized Map getFunctions () {
	return (Map) functions.clone();
    }

    /**
     *  Return a clone of this prototype's templates container. Synchronized
     *  to not return a map in a transient state where it is just being
     *  updated by the type manager.
     */
    public synchronized Map getTemplates () {
	return (Map) templates.clone();
    }

    /**
     *  Return a clone of this prototype's skins container. Synchronized
     *  to not return a map in a transient state where it is just being
     *  updated by the type manager.
     */
    public synchronized Map getSkins () {
	return (Map) skins.clone();
    }

    public synchronized void removeUpdatable (String fileName) {
	updatables.remove (fileName);
    }

    public synchronized void removeAction (String actionName) {
	actions.remove (actionName);
    }

    public synchronized void removeFunctionFile (String functionFileName) {
	functions.remove (functionFileName);
    }
    
    public synchronized void removeTemplate (String templateName) {
	templates.remove (templateName);
    }


   /**
    *  Return a string representing this prototype.
    */
    public String toString () {
	return "[Prototype "+ app.getName()+"/"+name+"]";
    }

}

