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

package helma.scripting.rhino;

import helma.scripting.ScriptingException;

/**
 * This class is filtered out by Helma's main class loader
 * although it is present in the main helma.jar file. This forces
 * it to be loaded through the per-application class loader. The
 * goal is to make jar files in the application directory visible to
 * application code.
 *
 * @see helma.main.launcher.FilteredClassLoader
 */
public final class PhantomEngine extends RhinoEngine {

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
     * @param resolve indicates whether functionName may contain an object path
     *                   or just the plain function name
     * @return the return value of the function
     * @throws ScriptingException to indicate something went wrong
     *                   with the invocation
     */
    public Object invoke(Object thisObject, String functionName, Object[] args,
                         int argsWrapMode, boolean resolve) throws ScriptingException {
        return super.invoke(thisObject, functionName, args, argsWrapMode, resolve);
    }
}
