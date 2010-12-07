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

import org.mozilla.javascript.RhinoException;

import java.io.*;

/**
 * The base class for wrapped exceptions thrown by invocation of the scripting engine.
 * If the wrapped exception is a RhinoException, the script stack trace will be
 * prepended to the actual java stack trace in stack dumps. 
 */
public class ScriptingException extends Exception {

    private static final long serialVersionUID = -7191341724784015678L;
    
    String scriptStack = null;

    /**
     * Construct a ScriptingException given an error message and wrapped exception.
     * @param message the message
     * @param cause the original exception
     */
    public ScriptingException(String message, Throwable cause) {
        super(message, cause);
        setScriptStack(cause);
    }

    /**
     * Extract the JavaScript stack trace element from the source exception
     * and copy them over to the target exception.
     * @param cause the original exception
     */
    private void setScriptStack(Throwable cause) {
        if (cause instanceof RhinoException) {
            FilenameFilter filter = new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith(".js") ||
                           name.endsWith(".hac") ||
                           name.endsWith(".hsp");
                }
            };
            scriptStack = ((RhinoException) cause).getScriptStackTrace(filter);
        }
    }

    /**
     * Get the script stack, or null if none is available
     * @return the script stack trace
     */
    public String getScriptStackTrace() {
        return scriptStack;
    }

    /**
     * Get the java stack trace.
     * @return the java stack trace
     */
    public String getJavaStackTrace() {
        StringWriter w = new StringWriter();
        getCause().printStackTrace(new PrintWriter(w));
        return w.toString();
    }

    /*
     * Adaption from Throwable.printStackTrace() to also print Script file stack elements.
     */
    public void printStackTrace(PrintStream s) {
        synchronized (s) {
            if (scriptStack != null) {
                s.println(this);
                s.print(scriptStack);
                s.print("Full trace: ");
            }
            getCause().printStackTrace(s);
        }
    }


    /*
     * Adaption from Throwable.printStackTrace() to also print Script file stack elements.
     */
    public void printStackTrace(PrintWriter s) {
        synchronized (s) {
            if (scriptStack != null) {
                s.println(this);
                s.print(scriptStack);
                s.print("Full trace: ");
            }
            getCause().printStackTrace(s);
        }
    }

}