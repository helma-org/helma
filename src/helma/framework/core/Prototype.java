// Prototype.java
// Copyright (c) Hannes Wallnöfer 1998-2000
 
package helma.framework.core;

import java.util.HashMap;
import java.util.Iterator;
import java.io.*;
import helma.framework.*;
import helma.objectmodel.*;
import FESI.Data.*;
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
    HashMap templates, functions, actions, skins, updatables;
    File codeDir;
    long lastUpdate;

    DbMapping dbmap;

    Prototype prototype;


     public Prototype (File codeDir, Application app) {

	app.logEvent ("Constructing Prototype "+app.getName()+"/"+codeDir.getName ());

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
	if (retval == null && prototype != null)
	    retval = prototype.getActionOrTemplate (aname);
	return retval;
    }

    public void setPrototype (Prototype prototype) {
	// this is not allowed for the hopobject and global prototypes
	if ("hopobject".equalsIgnoreCase (name) || "global".equalsIgnoreCase (name))
	    return;
	    
	Prototype old = this.prototype;
	this.prototype = prototype;

	// if prototype has changed, update ES-prototypes in request evaluators
	if (prototype != old) {
	    Iterator evals = app.typemgr.getRegisteredRequestEvaluators ();
	    while (evals.hasNext ()) {
	        try {
	            RequestEvaluator reval = (RequestEvaluator) evals.next ();
	            ObjectPrototype op = reval.getPrototype (getName());
	            // use hopobject (node) as prototype even if prototype is null - 
	            // this is the case if no hopobject directory exists
	            ObjectPrototype opp = prototype == null ? 
	            	reval.esNodePrototype : reval.getPrototype (prototype.getName ());
	            // don't think this is possible, but check anyway
	            if (opp == null)
	                opp = reval.esNodePrototype;
	            op.setPrototype (opp);
	        } catch (Exception ignore) {}
	    }
	}
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
	                app.logEvent ("Error building function protos in prototype: "+x);
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
            ObjectPrototype op = null;

            // get the prototype's prototype if possible and necessary
            ObjectPrototype opp = null;
            if (prototype != null) 
                opp = reval.getPrototype (prototype.getName ());
            if (!"global".equalsIgnoreCase (name) && 
            		!"hopobject".equalsIgnoreCase (name) && opp == null)
                opp = reval.esNodePrototype;

            if ("user".equalsIgnoreCase (name)) {
                op = reval.esUserPrototype;
                op.setPrototype (opp);
            } else if ("global".equalsIgnoreCase (name))
                op = reval.global;
            else if ("hopobject".equalsIgnoreCase (name))
                op = reval.esNodePrototype;
            else {
                op = new ObjectPrototype (opp, reval.evaluator);
                try {
                    op.putProperty ("prototypename", new ESString (name), "prototypename".hashCode ());
                } catch (EcmaScriptException ignore) {}
            }
            reval.putPrototype (name, op);

            // Register a constructor for all types except global.
            // This will first create a node and then call the actual (scripted) constructor on it.
            if (!"global".equalsIgnoreCase (name)) {
                try {
                    FunctionPrototype fp = (FunctionPrototype) reval.evaluator.getFunctionPrototype();
                    reval.global.putHiddenProperty (name, new NodeConstructor (name, fp, reval));
                } catch (EcmaScriptException ignore) {}
            }

	for (Iterator it = functions.values().iterator(); it.hasNext(); ) {
	    FunctionFile ff = (FunctionFile) it.next ();
	    ff.updateRequestEvaluator (reval);
	}
	for (Iterator it = templates.values().iterator(); it.hasNext(); ) {
	    Template tmp = (Template) it.next ();
	    try {
	        tmp.updateRequestEvaluator (reval);
	    } catch (EcmaScriptException ignore) {}
	}
	for (Iterator it = actions.values().iterator(); it.hasNext(); ) {
	    Action act = (Action) it.next ();
	    try {
	        act.updateRequestEvaluator (reval);
	    } catch (EcmaScriptException ignore) {}
	}

    }


    public String toString () {
	return "[Prototype "+ app.getName()+"/"+name+"]";
    }

}


















































