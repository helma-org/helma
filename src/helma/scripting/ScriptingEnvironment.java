// ScriptingEnvironment.java
// Copyright (c) Hannes Wallnöfer 1998-2001

package helma.scripting;

import helma.framework.core.Application;
import helma.framework.core.Prototype;
import helma.framework.core.RequestEvaluator;
import java.util.*;
import java.io.File;

/**
 * This is the interface that must be implemented to make a scripting environment
 * usable by the Helma application server.
 */
public interface ScriptingEnvironment {

    /**
     * Initialize the environment using the given properties
     */
    public void init (Application app, Properties props) throws ScriptingException;

    /**
     *  Evaluate a source file on a given type/class/prototype
     */
    public void evaluateFile (Prototype prototype, File file);

    /**
     *  Evaluate a source string on a given type/class/prototype
     */
    public void evaluateString (Prototype prototype, String code);

    /**
     * Invoke a function on some object, using the given arguments and global vars.
     */
    public Object invoke (Object thisObject, String functionName, Object[] args,
		HashMap globals, RequestEvaluator reval)
		throws ScriptingException;

    /**
     *  Get a property on an object
     */
    public Object get (Object thisObject, String key);

    /**
     *  Return true if a function by that name is defined for that object.
     */
    public boolean hasFunction (Object thisObject, String functionName)
		throws ScriptingException;

}





























