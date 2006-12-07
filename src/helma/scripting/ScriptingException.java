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

import java.util.List;
import java.util.ArrayList;
import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * The base class for exceptions thrown by Helma scripting package
 */
public class ScriptingException extends Exception {

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
        List list = new ArrayList();
        StackTraceElement[] stack = cause.getStackTrace();
        for (int i = 0; i < stack.length; i++) {
            StackTraceElement e = stack[i];
            String name = e.getFileName();
            if (e.getLineNumber() > -1 &&
                    (name.endsWith(".js") || name.endsWith(".hac"))) {
                list.add(e);
            }
        }
        setStackTrace((StackTraceElement[]) list.toArray(new StackTraceElement[list.size()]));
    }


    /*
     * Adaption from Throwable.printStackTrace() to only print Script file stack elements.
     */
    public void printStackTrace(PrintStream s) {
        synchronized (s) {
            s.println(this);
            StackTraceElement[] trace = getStackTrace();
            for (int i=0; i < trace.length; i++)
                s.println("\tat " + trace[i].getFileName() + ":" +
                                    trace[i].getLineNumber());
            Throwable ourCause = getCause();
            if (ourCause != null)
                printStackTraceAsCause(ourCause, s, trace);
        }
    }

    /*
     * Adaption from Throwable.printTraceAsCause() to be callable from this class.
     */
    private static void printStackTraceAsCause(Throwable t, PrintStream s,
                                        StackTraceElement[] causedTrace)
    {
        // assert Thread.holdsLock(s);

        // Compute number of frames in common between this and caused
        StackTraceElement[] trace = t.getStackTrace();
        int m = trace.length-1, n = causedTrace.length-1;
        while (m >= 0 && n >=0 && trace[m].equals(causedTrace[n])) {
            m--; n--;
        }
        int framesInCommon = trace.length - 1 - m;

        s.println("Caused by: " + t);
        for (int i=0; i <= m; i++)
            s.println("\tat " + trace[i]);
        if (framesInCommon != 0)
            s.println("\t... " + framesInCommon + " more");

        // Recurse if t has a cause
        Throwable theCause = t.getCause();
        if (theCause != null)
            printStackTraceAsCause(theCause, s, trace);
    }

    /*
     * Adaption from Throwable.printStackTrace() to only print Script file stack elements.
     */
    public void printStackTrace(PrintWriter s) {
        synchronized (s) {
            s.println(this);
            StackTraceElement[] trace = getStackTrace();
            for (int i=0; i < trace.length; i++)
                s.println("\tat " + trace[i].getFileName() + ":" +
                                    trace[i].getLineNumber());
            Throwable ourCause = getCause();
            if (ourCause != null)
                printStackTraceAsCause(ourCause, s, trace);
        }
    }

    /*
     * Adaption from Throwable.printTraceAsCause() to be callable from this class.
     */
    private static void printStackTraceAsCause(Throwable t, PrintWriter s,
                                        StackTraceElement[] causedTrace)
    {
        // assert Thread.holdsLock(s);

        // Compute number of frames in common between this and caused
        StackTraceElement[] trace = t.getStackTrace();
        int m = trace.length-1, n = causedTrace.length-1;
        while (m >= 0 && n >=0 && trace[m].equals(causedTrace[n])) {
            m--; n--;
        }
        int framesInCommon = trace.length - 1 - m;

        s.println("Caused by: " + t);
        for (int i=0; i <= m; i++)
            s.println("\tat " + trace[i]);
        if (framesInCommon != 0)
            s.println("\t... " + framesInCommon + " more");

        // Recurse if t has a cause
        Throwable theCause = t.getCause();
        if (theCause != null)
            printStackTraceAsCause(theCause, s, trace);
    }

}
