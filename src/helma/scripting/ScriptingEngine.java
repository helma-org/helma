// ScriptingEngine.java
// Copyright (c) Hannes Wallnöfer 1998-2001

package helma.scripting;

import helma.framework.IPathElement;
import helma.framework.core.Application;
import helma.framework.core.Prototype;
import helma.framework.core.RequestEvaluator;
import java.util.*;
import java.io.File;

/**
 * This is the interface that must be implemented to make a scripting environment
 * usable by the Helma application server.
 *
 * Implementations of this interface must have a public zero-argument constructor 
 * to be usable by the Helma framework.
 */
public interface ScriptingEngine {

    /** 
     * Init the scripting engine with an application and a request evaluator
     */
    public void init (Application app, RequestEvaluator reval);

    /**
     *  This method is called before an execution context for a request
     *  evaluation is entered to let the Engine know it should update 
     *  its prototype information
     */
    public void updatePrototypes ();

    /**
     *  This method is called when an execution context for a request
     *  evaluation is entered. The globals parameter contains the global values
     *  to be applied during this execution context.
     */
    public void enterContext (HashMap globals) throws ScriptingException;

    /**
     *   This method is called to let the scripting engine know that the current
     *   execution context has terminated.
     */
    public void exitContext ();


    /**
     * Invoke a function on some object, using the given arguments and global vars. 
     * XML-RPC calls require special input and output parameter conversion.
     */
    public Object invoke (Object thisObject, String functionName, Object[] args, boolean xmlrpc)
		throws ScriptingException;

    /**
     *  Let the evaluator know that the current evaluation has been aborted.
     */
    public void abort ();

    /**
     *  Get a property on an object
     */
    public Object get (Object thisObject, String key);

    /**
     *  Return true if a function by that name is defined for that object.
     */
    public boolean hasFunction (Object thisObject, String functionName);

    /**
     *  Get an IPathElement that offers introspection services into the application.
     *  If this method returns null, no introspection is available for this kind of engine.
     *  In order to be compatible with the standard Helma management application, this 
     *  class should be compatible with helma.doc.DocApplication.
     */
    public IPathElement getIntrospector ();

}

