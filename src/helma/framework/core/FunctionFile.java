// FunctionFile.java
// Copyright (c) Hannes Wallnöfer 1998-2000
 
package helma.framework.core;

import java.util.HashMap;
import java.util.Iterator;
import java.util.HashSet;
import java.util.Enumeration;
import java.io.*;
import helma.framework.*;
import helma.util.Updatable;
import FESI.Data.*;
import FESI.Exceptions.EcmaScriptException;
import FESI.Interpreter.*;


/**
 * This represents a File containing JavaScript functions for a given Object. 
 */


public class FunctionFile implements Updatable {

    String name;
    Prototype prototype;
    Application app;
    File file;
    long lastmod;

    // a set of funcion names defined by this file. We keep this to be able to
    // remove them once the file should get removed
    HashSet declaredProps;
    long declaredPropsTimestamp;

    public FunctionFile (File file, String name, Prototype proto) {
	this.prototype = proto;
	this.app = proto.app;
	this.name = name;
	this.file = file;
	update ();
    }


    /**
     * Tell the type manager whether we need an update. this is the case when
     * the file has been modified or deleted.
     */
    public boolean needsUpdate () {
	return lastmod != file.lastModified () || !file.exists ();
    }


    public void update () {

	if (!file.exists ()) {
	    remove ();

	} else {

	    lastmod = file.lastModified ();
	    // app.typemgr.readFunctionFile (file, prototype.getName ());

	    Iterator evals = app.typemgr.getRegisteredRequestEvaluators ();
	    while (evals.hasNext ()) {
	        try {

	            RequestEvaluator reval = (RequestEvaluator) evals.next ();
	            updateRequestEvaluator (reval);

	        } catch (Exception ignore) {}
	    }
	}

    }

    public  synchronized void updateRequestEvaluator (RequestEvaluator reval) {

        EvaluationSource es = new FileEvaluationSource(file.getPath(), null);
        FileReader fr = null;

        HashMap priorProps = null;
        HashSet newProps = null;

        try {

            ObjectPrototype op = reval.getPrototype (prototype.getName());

            // extract all properties from prototype _before_ evaluation, so we can compare afterwards
            // but only do this is declaredProps is not up to date yet
            if (declaredPropsTimestamp != lastmod) {
                priorProps = new HashMap ();
                // remember properties before evaluation, so we can tell what's new afterwards
                try {
                    for (Enumeration en=op.getAllProperties(); en.hasMoreElements(); ) {
                        String prop = (String) en.nextElement ();
                        priorProps.put (prop, op.getProperty (prop, prop.hashCode()));
                    }
                } catch (Exception ignore) {}
            }

            // do the update, evaluating the file
            fr = new FileReader(file);
            reval.evaluator.evaluate(fr, op, es, false);

            // check what's new
            if (declaredPropsTimestamp != lastmod) try {
                newProps = new HashSet ();
                for (Enumeration en=op.getAllProperties(); en.hasMoreElements(); ) {
                    String prop = (String) en.nextElement ();
                    if (priorProps.get (prop) == null || op.getProperty (prop, prop.hashCode()) != priorProps.get (prop))
                        newProps.add (prop);
                }
            } catch (Exception ignore) {}

        } catch (IOException e) {
            app.logEvent ("Error parsing function file "+app.getName()+":"+prototype.getName()+"/"+file.getName()+": "+e);
        } catch (EcmaScriptException e) {
            app.logEvent ("Error parsing function file "+app.getName()+":"+prototype.getName()+"/"+file.getName()+": "+e);
        } finally {
            if (fr!=null) {
                try {
                    fr.close();
                } catch (IOException ignore) {}
            }

            // now remove the props that were not refreshed, and set declared props to new collection
            if (declaredPropsTimestamp != lastmod) {
                declaredPropsTimestamp = lastmod;
                if (declaredProps != null) {
                    declaredProps.removeAll (newProps);
                    removeProperties (declaredProps);
                }
                declaredProps = newProps;
                // System.err.println ("DECLAREDPROPS = "+declaredProps);
            }

        }
    }


    void remove () {
	prototype.functions.remove (name);
	prototype.updatables.remove (file.getName());

	// if we did not add anything to any evaluator, we're done
	if (declaredProps == null || declaredProps.size() == 0)
	    return;

	removeProperties (declaredProps);
    }

    /**
     * Remove the properties in the HashMap iff they're still the same as declared by this file.
     * This method is called by remove() with the latest props, and by update with the prior props
     * after the file has been reevaluated.
     */
    void removeProperties (HashSet props) {
	// first loop through other function files in this prototype to make a set of properties
	// owned by other files.
	HashSet otherFiles = new HashSet ();
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
	}
    }

    public String toString () {
	return prototype.getName()+"/"+file.getName();
    }


}







































