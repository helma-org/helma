// Prototype.java
// Copyright (c) Hannes Wallnöfer 1998-2000
 
package helma.framework.core;

import java.util.*;
import java.io.*;
import helma.framework.*;
import helma.objectmodel.*;
import FESI.Exceptions.EcmaScriptException;


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
    Hashtable templates, functions, actions;
    File codeDir;
    long lastUpdate;

    DbMapping dbmap;

    Prototype prototype;


     public Prototype (File codeDir, Application app) {

	IServer.getLogger().log ("Constructing Prototype "+app.getName()+"/"+codeDir.getName ());

	this.codeDir = codeDir;
	this.app = app;
	this.name = codeDir.getName ();
	
	File propfile = new File (codeDir, "type.properties");
	SystemProperties props = new SystemProperties (propfile.getAbsolutePath ());
	dbmap = new DbMapping (app, name, props);

	lastUpdate = System.currentTimeMillis ();

    }


    public Action getActionOrTemplate (String aname) {

	Action retval = null;
	if (aname == null || "".equals (aname))
	    aname = "main";
	retval = (Action) actions.get (aname);
	// check if it's allowed to access templates via URL
	// this should be cached in the future
	if (retval == null && "true".equalsIgnoreCase (app.props.getProperty ("exposetemplates")))
	    retval = (Action) templates.get (aname);
	// if still not found, check if the action is defined for the generic node prototype
	if (retval == null && this != app.typemgr.nodeProto && app.typemgr.nodeProto != null)
	    retval = app.typemgr.nodeProto.getActionOrTemplate (aname);
	return retval;
    }

    public void setPrototype (Prototype prototype) {
	this.prototype = prototype;
    }

    public Prototype getPrototype () {
	return prototype;
    }

    public Template getTemplate (String tmpname) {
	return (Template) templates.get (tmpname);
    }    
 
    public FunctionFile getFunctionFile (String ffname) {
	return (FunctionFile) functions.get (ffname);
    }

    public Action getAction (String afname) {
	return (Action) actions.get (afname);
    }

    public File getCodeDir () {
	return codeDir;
    }
 
    public synchronized boolean checkCodeDir () {

    	boolean retval = false;
	String[] list = codeDir.list ();

	for (int i=0; i<list.length; i++) {
	    if (list[i].endsWith (app.templateExtension) || list[i].endsWith (app.scriptExtension)) {
	        File f = new File (codeDir, list[i]);

	        if (f.lastModified () > lastUpdate) {
	            lastUpdate = System.currentTimeMillis ();
	            try {
	                app.typemgr.updatePrototype (this.name, codeDir,  this);
	                // TypeManager.broadcaster.broadcast ("Finished update for prototype "+name+" @ "+new Date ()+"<br><hr>");
	            } catch (Exception x) {
	                IServer.getLogger().log ("Error building function protos in prototype: "+x);
	                // TypeManager.broadcaster.broadcast ("Error updating prototype "+name+" in application "+app.getName()+":<br>"+x.getMessage ()+"<br><hr>");
	            }
	            retval = true;
	        }
	    }
	}
	return retval;
    }
 

    public String getName () {
	return name;
    }


    public void initRequestEvaluator (RequestEvaluator reval) {
	for (Enumeration en = functions.elements(); en.hasMoreElements(); ) {
	    FunctionFile ff = (FunctionFile) en.nextElement ();
	    ff.updateRequestEvaluator (reval);
	}
	for (Enumeration en = templates.elements(); en.hasMoreElements(); ) {
	    Template tmp = (Template) en.nextElement ();
	    try {
	        tmp.updateRequestEvaluator (reval);
	    } catch (EcmaScriptException ignore) {}
	}
	for (Enumeration en = actions.elements(); en.hasMoreElements(); ) {
	    Action act = (Action) en.nextElement ();
	    try {
	        act.updateRequestEvaluator (reval);
	    } catch (EcmaScriptException ignore) {}
	}
    }
}


















































