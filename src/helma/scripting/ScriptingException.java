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
    Exception wrapped;

    /**
     * Construct a ScriptingException given an error message
     */
    public ScriptingException(String msg) {
        super(msg);
        wrapped = null;
    }

    /**
     * Construct a ScriptingException given an error message
     */
    public ScriptingException(Exception w) {
        wrapped = w;
    }

    /**
     *
     *
     * @return ...
     */
    public String toString() {
        if (wrapped == null) {
            return super.toString();
        } else {
            return wrapped.toString();
        }
    }

    /**
     *
     *
     * @return ...
     */
    public String getMessage() {
        if (wrapped == null) {
            return super.getMessage();
        } else {
            return wrapped.getMessage();
        }
    }

    /**
     *
     */
    public void printStackTrace() {
        if (wrapped == null) {
            super.printStackTrace();
        } else {
            wrapped.printStackTrace();
        }
    }

    /**
     *
     *
     * @param stream ...
     */
    public void printStackTrace(PrintStream stream) {
        if (wrapped == null) {
            super.printStackTrace(stream);
        } else {
            wrapped.printStackTrace(stream);
        }
    }

    /**
     *
     *
     * @param writer ...
     */
    public void printStackTrace(PrintWriter writer) {
        if (wrapped == null) {
            super.printStackTrace(writer);
        } else {
            wrapped.printStackTrace(writer);
        }
    }
}
