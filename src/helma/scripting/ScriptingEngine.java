/*
 * Helma License Notice
 *
 * The contents of this file are subject to the Helma License
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://adele.helma.org/download/helma/license.txt
 *
 * Copyright 1998-2003 Helma Software. All Rights Reserved.
 *
 * $RCSfile$
 * $Author$
 * $Revision$
 * $Date$
 */

package helma.scripting;

import helma.framework.IPathElement;
import helma.framework.core.Application;
import helma.framework.core.RequestEvaluator;
import java.io.OutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

/**
 * This is the interface that must be implemented to make a scripting environment
 * usable by the Helma application server.
 *
 * Implementations of this interface must have a public zero-argument constructor
 * to be usable by the Helma framework.
 */
public interface ScriptingEngine {

    /**
     * Argument wrapping mode that indicates arguments are wrapped already
     * and should be passed along unchanged.
     */
    public final int ARGS_WRAP_NONE = 0;

    /**
     * Argument wrapping mode that indicates arguments may be arbitrary
     * Java objects that may need to be wrapped.
     */
    public final int ARGS_WRAP_DEFAULT = 1;

    /**
     * Argument wrapping mode that indicates this is an XML-RPC call and
     * arguments should be processed accordingly.
     */
    public final int ARGS_WRAP_XMLRPC = 2;

    /**
     * Init the scripting engine with an application and a request evaluator
     */
    public void init(Application app, RequestEvaluator reval);

    /**
     *  This method is called before an execution context for a request
     *  evaluation is entered to let the Engine know it should update
     *  its prototype information
     */
    public void updatePrototypes() throws IOException, ScriptingException;

    /**
     *  This method is called when an execution context for a request
     *  evaluation is entered. The globals parameter contains the global values
     *  to be applied during this execution context.
     */
    public void enterContext(HashMap globals) throws ScriptingException;

    /**
     *   This method is called to let the scripting engine know that the current
     *   execution context has terminated.
     */
    public void exitContext();

    /**
     * Invoke a function on some object, using the given arguments and global vars.
     * XML-RPC calls require special input and output parameter conversion.
     *
     * @param thisObject the object to invoke the function on, or null for
     *                   global functions
     * @param functionName the name of the function to be invoked
     * @param args array of argument objects
     * @param argsWrapMode indicated the way to process the arguments. Must be
     *                   one of <code>ARGS_WRAP_NONE</code>,
     *                          <code>ARGS_WRAP_DEFAULT</code>,
     *                          <code>ARGS_WRAP_XMLRPC</code>
     * @return the return value of the function
     * @throws ScriptingException to indicate something went wrong
     *                   with the invocation
     */
    public Object invoke(Object thisObject, String functionName,
                         Object[] args, int argsWrapMode)
            throws ScriptingException;


    /**
     *  Let the evaluator know that the current evaluation has been aborted.
     */
    public void abort();

    /**
     *  Get a property on an object
     */
    public Object get(Object thisObject, String key);

    /**
     *  Return true if a function by that name is defined for that object.
     */
    public boolean hasFunction(Object thisObject, String functionName);

    /**
     *  Get an IPathElement that offers introspection services into the application.
     *  If this method returns null, no introspection is available for this kind of engine.
     *  In order to be compatible with the standard Helma management application, this
     *  class should be compatible with helma.doc.DocApplication.
     */
    public IPathElement getIntrospector();

    /**
     * Provide object serialization for this engine's scripted objects. If no special
     * provisions are required, this method should just wrap the stream with an
     * ObjectOutputStream and write the object.
     *
     * @param obj the object to serialize
     * @param out the stream to write to
     * @throws IOException
     */
    public void serialize(Object obj, OutputStream out) throws IOException;

    /**
     * Provide object deserialization for this engine's scripted objects. If no special
     * provisions are required, this method should just wrap the stream with an
     * ObjectIntputStream and read the object.
     *
     * @param in the stream to read from
     * @return the deserialized object
     * @throws IOException
     */
    public Object deserialize(InputStream in) throws IOException, ClassNotFoundException;
}
