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
    // public void init (Application app, Properties props) throws ScriptingException;

    /**
     * A prototype has been updated and must be re-evaluated.
     */
    // public void updatePrototype (Prototype prototype);

    /**
     * Invoke a function on some object, using the given arguments and global vars.
     */
    public Object invoke (Object thisObject, String functionName, Object[] args,
		HashMap globals)
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





























