// FesiScriptingEnvironment.java
// Copyright (c) Hannes Wallnöfer 2002

package helma.scripting.fesi;

import helma.scripting.*;
import helma.framework.core.*;
import java.util.*;
import java.io.File;
import FESI.Data.*;
import FESI.Interpreter.*;
import FESI.Exceptions.*;

/**
 * This is the implementation of ScriptingEnvironment for the FESI EcmaScript interpreter.
 */
public class FesiScriptingEnvironment implements ScriptingEnvironment {

    Application app;
    Properties props;
    HashMap evaluators;

    /**
     * Initialize the environment using the given properties
     */
    public void init (Application app, Properties props) throws ScriptingException {
	this.app = app;
	this.props = props;
	evaluators = new HashMap ();
    }


    /**
     * A prototype has been updated and must be re-evaluated.
     */
    public void updatePrototype (Prototype prototype) {
	System.err.println ("UPDATING PROTOTYPE: "+prototype);
	for (Iterator i = evaluators.values().iterator(); i.hasNext(); ) {
	    FesiEvaluator fesi = (FesiEvaluator) i.next();
	    fesi.evaluatePrototype (prototype);
	}
    }

    /**
     * Invoke a function on some object, using the given arguments and global vars.
     */
    public Object invoke (Object thisObject, String functionName, Object[] args,
			HashMap globals, RequestEvaluator reval)
			throws ScriptingException {
	System.err.println ("INVOKE: "+thisObject+", "+functionName);
	System.err.println (this);
	// check if there is already a FesiEvaluator for this RequestEvaluator.
	// if not, create one.
	FesiEvaluator fesi = getEvaluator (reval);
	return fesi.invoke (thisObject, functionName, args, globals);
    }

    /**
     *  Get a property on an object
     */
    public Object get (Object thisObject, String key, RequestEvaluator reval) {
	System.err.println ("GETPROPERTY "+thisObject+", "+key);
	FesiEvaluator fesi = getEvaluator (reval);
	return fesi.getProperty (thisObject, key);
    }

    /**
     *  Return true if a function by that name is defined for that object.
     */
    public boolean hasFunction (Object thisObject, String functionName, RequestEvaluator reval)
			throws ScriptingException {
	System.err.println ("HASFUNCTION "+thisObject+", "+functionName);
	FesiEvaluator fesi = getEvaluator (reval);
	return fesi.hasFunction (thisObject, functionName);
    }


    Collection getEvaluators () {
	return evaluators.values();
    }

    FesiEvaluator getEvaluator (RequestEvaluator reval) {
	FesiEvaluator fesi = (FesiEvaluator) evaluators.get (reval);
	if (fesi == null) {
	    fesi = new FesiEvaluator (app, reval);
	    evaluators.put (reval, fesi);
	}
	return fesi;
    }
}
