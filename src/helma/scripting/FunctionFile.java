// FunctionFile.java
// Copyright (c) Hannes Wallnöfer 1998-2000
 
package helma.scripting;

import java.util.HashMap;
import java.util.Iterator;
import java.util.HashSet;
import java.util.Enumeration;
import java.io.*;
import helma.framework.*;
import helma.framework.core.*;
import helma.util.Updatable;


/**
 * This represents a File containing script functions for a given class/prototype.
 */


public class FunctionFile implements Updatable {

    String name;
    Prototype prototype;
    Application app;
    File file;
    String content;
    long lastmod;

    // a set of funcion names defined by this file. We keep this to be able to
    // remove them once the file should get removed
    HashSet declaredProps;
    long declaredPropsTimestamp;

    public FunctionFile (File file, String name, Prototype proto) {
	this.prototype = proto;
	this.app = proto.getApplication ();
	this.name = name;
	this.file = file;
	update ();
    }

    /**
     *  Create a function file without a file, passing the code directly. This is used for
     *  files contained in zipped applications. The whole update mechanism is bypassed
     *  by immediately parsing the code.
     */
    public FunctionFile (String body, String name, Prototype proto) {
	this.prototype = proto;
	this.app = proto.getApplication ();
	this.name = name;
	this.file = null;
	this.content = body;
	update ();
    }

    /**
     * Tell the type manager whether we need an update. this is the case when
     * the file has been modified or deleted.
     */
    public boolean needsUpdate () {
	return file != null && lastmod != file.lastModified ();
    }


    public void update () {

	if (file != null) {
	    if (!file.exists ()) {
	        remove ();
	    } else {
	        lastmod = file.lastModified ();
	        // app.typemgr.readFunctionFile (file, prototype.getName ());
	        // app.getScriptingEnvironment().evaluateFile (prototype, file);
	    }
	} else {
	    // app.getScriptingEnvironment().evaluateString (prototype, content);
	}
    }

    /* public void evaluate (ScriptingEnvironment env) {
	if (file != null)
	    env.evaluateFile (prototype, file);
	else
	    env.evaluateString (prototype, content);
    }*/
    public boolean hasFile () {
	return file != null;
    }

    public File getFile () {
	return file;
    }

    public String getContent () {
	return content;
    }


    void remove () {
	prototype.removeFunctionFile (name);
	prototype.removeUpdatable (file.getName());

	// if we did not add anything to any evaluator, we're done
	/* if (declaredProps == null || declaredProps.size() == 0)
	    return;

	removeProperties (declaredProps); */
    }

    /**
     * Remove the properties in the HashMap iff they're still the same as declared by this file.
     * This method is called by remove() with the latest props, and by update with the prior props
     * after the file has been reevaluated.
     */
    void removeProperties (HashSet props) {
	// first loop through other function files in this prototype to make a set of properties
	// owned by other files.
/*	HashSet otherFiles = new HashSet ();
	for (Iterator it=prototype.functions.values ().iterator (); it.hasNext (); ) {
	    FunctionFile other = (FunctionFile) it.next ();
	    if (other != this && other.declaredProps != null)
	        otherFiles.addAll (other.declaredProps);
	}

	Iterator evals = app.typemgr.getRegisteredRequestEvaluators ();
	while (evals.hasNext ()) {
	    try {
	        RequestEvaluator reval = (RequestEvaluator) evals.next ();
	        ObjectPrototype op = reval.getPrototype (prototype.getName());
	        for (Iterator it = props.iterator ();  it.hasNext (); ) {
	            String fname = (String) it.next ();
	            // check if this property has been declared by some other function file in the meantime
	            if (otherFiles.contains (fname))
	                continue;
	            op.deleteProperty (fname, fname.hashCode());
	            // System.err.println ("REMOVING PROP: "+fname);
	        }
	    } catch (Exception ignore) {}
	} */
    }

    public String toString () {
	if (file == null)
	    return "[Zipped script file]";
	else
	    return prototype.getName()+"/"+file.getName();
    }


}


