// FunctionFile.java
// Copyright (c) Hannes Wallnöfer 1998-2000
 
package helma.framework.core;

import java.util.*;
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
    HashSet functionNames;

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
	    functionNames = null;
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

        HashMap priorProps = new HashMap ();
        ObjectPrototype op = null;

        try {
            op = reval.getPrototype (prototype.getName());

            // remember properties before evaluation, so we can tell what's new afterwards
            if (functionNames == null) try {
                for (Enumeration en=op.getAllProperties(); en.hasMoreElements(); ) {
                    String prop = (String) en.nextElement ();
                    priorProps.put (prop, op.getProperty (prop, prop.hashCode()));
                }
            } catch (Exception ignore) {}

            fr = new FileReader(file);
            reval.evaluator.evaluate(fr, op, es, false);

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

            // check
            if (functionNames == null && op != null) try {
                functionNames = new HashSet ();
                for (Enumeration en=op.getAllProperties(); en.hasMoreElements(); ) {
                    String prop = (String) en.nextElement ();
                    if (priorProps.get (prop) == null || op.getProperty (prop, prop.hashCode()) != priorProps.get (prop))
                        functionNames.add (prop);
                }
            } catch (Exception ignore) {}
        }

    }

    void remove () {
	prototype.functions.remove (name);
	prototype.updatables.remove (file.getName());

	// if we did not add anything to any evaluator, we're done
	if (functionNames == null)
	    return;

	Iterator evals = app.typemgr.getRegisteredRequestEvaluators ();
	while (evals.hasNext ()) {
	    try {
	        RequestEvaluator reval = (RequestEvaluator) evals.next ();
	        ObjectPrototype op = reval.getPrototype (prototype.getName());
	        for (Iterator it=functionNames.iterator();  it.hasNext(); ) {
	            String fname = (String) it.next ();
	            ESValue esv = op.getProperty (fname, fname.hashCode());
	            if (esv instanceof ConstructedFunctionObject) {
	                op.deleteProperty (fname, fname.hashCode());
	            }
	        }
	    } catch (Exception ignore) {}
	}
    }


}







































