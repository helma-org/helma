// ScriptingEngine.java
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
public interface ScriptingEngine {

    /**
     *  This method is called when an execution context for a request
     *  evaluation is entered. The globals parameter contains the global values
     *  to be applied during this executino context.
     */
    public void enterContext (HashMap globals) throws ScriptingException;

    /**
     *   This method is called to let the scripting engine know that the current
     *   execution context has terminated.
     */
    public void exitContext ();


    /**
     * Invoke a function on some object, using the given arguments and global vars.
     */
    public Object invoke (Object thisObject, String functionName, Object[] args)
		throws ScriptingException;

    /**
     *  Get a property on an object
     */
    public Object get (Object thisObject, String key);

    /**
     *  Return true if a function by that name is defined for that object.
     */
    public boolean hasFunction (Object thisObject, String functionName);

}





























