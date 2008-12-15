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

import helma.framework.repository.Resource;
import helma.framework.core.Application;
import helma.framework.core.RequestEvaluator;

import java.io.OutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

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
     * @param app the application
     * @param reval the request evaluator
     */
    public void init(Application app, RequestEvaluator reval);

    /**
     * Shut down the Scripting engine.
     */
    public void shutdown();

    /**
     *  This method is called when an execution context for a request
     *  evaluation is entered to let the Engine know it should update
     *  its prototype information
     *  @throws IOException an I/O exception occurred
     *  @throws ScriptingException a script related exception occurred
     */
    public void enterContext() throws IOException, ScriptingException;

    /**
     *  This method is called when an execution context for a request
     *  evaluation is entered. The globals parameter contains the global values
     *  to be applied during this execution context.
     *  @param globals map of global variables
     *  @throws ScriptingException a script related exception occurred
     */
    public void setGlobals(Map globals) throws ScriptingException;

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
     * @param function the name of the function to be invoked
     * @param args array of argument objects
     * @param argsWrapMode indicated the way to process the arguments. Must be
     *                   one of <code>ARGS_WRAP_NONE</code>,
     *                          <code>ARGS_WRAP_DEFAULT</code>,
     *                          <code>ARGS_WRAP_XMLRPC</code>
     * @param resolve indicates whether functionName may contain an object path
     *                   or just the plain function name
     * @return the return value of the function
     * @throws ScriptingException to indicate something went wrong
     *                   with the invocation
     */
    public Object invoke(Object thisObject, Object function,
                         Object[] args, int argsWrapMode, boolean resolve)
            throws ScriptingException;


    /**
     *  Let the evaluator know that the current evaluation has been aborted.
     */
    public void abort();

    /**
     * Get a property on an object
     * @param thisObject the object
     * @param propertyName the property name
     * @return true the property value, or null
     */
    public Object getProperty(Object thisObject, String propertyName);

    /**
     * Return true if a function by that name is defined for that object.
     * @param thisObject the object
     * @param functionName the function name
     * @param resolve if member path in function name should be resolved
     * @return true if the function is defined on the object
     */
    public boolean hasFunction(Object thisObject, String functionName, boolean resolve);

    /**
     * Return true if a property by that name is defined for that object.
     * @param thisObject the object
     * @param propertyName the property name
     * @return true if the function is defined on the object
     */
    public boolean hasProperty(Object thisObject, String propertyName);

    /**
     * Determine if the given object is mapped to a type of the scripting engine
     * @param obj an object
     * @return true if the object is mapped to a type
     */
    public boolean isTypedObject(Object obj);

    /**
     * Return a string representation for the given object
     * @param obj an object
     * @return a string representing the object
     */
    public String toString(Object obj);

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

    /**
     * Add a code resource to a given prototype by immediately compiling and evaluating it.
     *
     * @param typename the type this resource belongs to
     * @param resource a code resource
     */
    public void injectCodeResource(String typename, Resource resource);
}
