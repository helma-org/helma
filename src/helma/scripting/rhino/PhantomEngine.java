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
     *
     */
    public Object invoke(Object thisObject, String functionName, Object[] args,
                         int argsWrapMode) throws ScriptingException {
        return super.invoke(thisObject, functionName, args, argsWrapMode);
    }
}
