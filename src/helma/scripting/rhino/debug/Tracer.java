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
 
package helma.scripting.rhino.debug;

import helma.framework.ResponseTrans;
import org.mozilla.javascript.*;
import org.mozilla.javascript.debug.*;

public class Tracer implements Debugger {

    ResponseTrans res;

    /**
     *  Create a tracer that writes to this response object
     */
    public Tracer(ResponseTrans res) {
        this.res = res;
    }

    /**
     *  Implementws handleCompilationDone in interface org.mozilla.javascript.debug.Debugger
     */
    public void handleCompilationDone(Context cx, DebuggableScript script, 
                                      String source) {

        // res.debug("CompilationDone: "+toString(script));
    }

    /**
     *  Implementws getFrame in interface org.mozilla.javascript.debug.Debugger
     */
    public DebugFrame getFrame(Context cx, DebuggableScript script) {

        // res.debug("getFrame: "+toString(script));
        if (script.isFunction()) {

            return new TracerFrame(script);
        }

        return null;
    }

    static String toString(DebuggableScript script) {

        if (script.isFunction()) {

            return script.getSourceName() + ": " + script.getFunctionName();
        } else {

            return script.getSourceName();
        }
    }

    class TracerFrame
        implements DebugFrame {

        DebuggableScript script;

        TracerFrame(DebuggableScript script) {
            this.script = script;
        }

        /**
         * Called when execution is ready to start bytecode interpretation
         * for entered a particular function or script.
         */
        public void onEnter(Context cx, Scriptable activation, 
                            Scriptable thisObj, Object[] args) {

            StringBuffer b = new StringBuffer("Trace: ");
            b.append(Tracer.toString(script));
            b.append("(");

            for (int i = 0; i < args.length; i++) {
                b.append(args[i]);

                if (i < args.length - 1) {
                    b.append(", ");
                }
            }

            b.append(")");
            res.debug(b.toString());
        }

        /**
         *  Called when thrown exception is handled by the function or script.
         */
        public void onExceptionThrown(Context cx, Throwable ex) {
            res.debug("Exception Thrown: " + ex);
        }

        /**
         *  Called when the function or script for this frame is about to return.
         */
        public void onExit(Context cx, boolean byThrow, 
                           Object resultOrException) {

            // res.debug("Exit: "+Tracer.toString(script));
        }

        /**
         *  Called when executed code reaches new line in the source.
         */
        public void onLineChange(Context cx, int lineNumber) {

            // res.debug("LineChange: "+Tracer.toString(script));
        }
	
    } // end of class TracerFrame
} // end of class Tracer

