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
     *  Evaluate a source file on a given type/class/prototype
     */
    public void evaluateFile (Prototype prototype, File file) {
	for (Iterator i = evaluators.values().iterator(); i.hasNext(); ) {
	    FesiEvaluator fesi = (FesiEvaluator) i.next();
	    fesi.evaluateFile (prototype, file);
	}
    }

    /**
     *  Evaluate a source string on a given type/class/prototype
     */
    public void evaluateString (Prototype prototype, String code) {
	for (Iterator i = evaluators.values().iterator(); i.hasNext(); ) {
	    FesiEvaluator fesi = (FesiEvaluator) i.next();
	    fesi.evaluateString (prototype, code);
	}
    }

    /**
     * Invoke a function on some object, using the given arguments and global vars.
     */
    public Object invoke (Object thisObject, String functionName, Object[] args,
			HashMap globals, RequestEvaluator reval)
			throws ScriptingException {
	// check if there is already a FesiEvaluator for this RequestEvaluator.
	// if not, create one.
	FesiEvaluator fesi = (FesiEvaluator) evaluators.get (reval);
	if (fesi == null) {
	    fesi = new FesiEvaluator (app, reval);
	    evaluators.put (reval, fesi);
	}
	return fesi.invoke (thisObject, functionName, args, globals);
    }

    /**
     *  Get a property on an object
     */
    public Object get (Object thisObject, String key) {
	return null;
    }

    /**
     *  Return true if a function by that name is defined for that object.
     */
    public boolean hasFunction (Object thisObject, String functionName) throws ScriptingException {
	return false;
    }


    Collection getEvaluators () {
	return evaluators.values();
    }
}
