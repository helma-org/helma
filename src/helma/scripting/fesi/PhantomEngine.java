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

package helma.scripting.fesi;

import helma.scripting.ScriptingException;

/**
 * 
 */
public final class PhantomEngine extends FesiEngine {
    /**
     *
     */
    public Object invoke(Object thisObject, String functionName, Object[] args,
                         boolean xmlrpc) throws ScriptingException {
        return super.invoke(thisObject, functionName, args, xmlrpc);
    }
}
