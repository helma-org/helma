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

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * The base class for exceptions thrown by Helma scripting package
 */
public class ScriptingException extends Exception {

    // original exception that caused this ScriptingException to be thrown.
    Exception wrapped;

    /**
     * Construct a ScriptingException given an error message and wrapped exception.
     */
    public ScriptingException(String msg, Exception wrapped) {
        super(msg);
        this.wrapped = wrapped;
    }

    /**
     * Get the original exception that caused this exception to be thrown.
     *
     * @return the wrapped exception
     */
    public Exception getWrappedException() {
         return wrapped;
    }
}
