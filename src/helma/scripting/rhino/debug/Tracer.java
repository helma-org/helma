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
 * $RCSfile: Tracer.java,v $
 * $Author$
 * $Revision$
 * $Date$
 */
 
package helma.scripting.rhino.debug;

import helma.framework.ResponseTrans;
import helma.util.HtmlEncoder;
import org.mozilla.javascript.*;
import org.mozilla.javascript.debug.*;
import java.util.ArrayList;

public class Tracer implements Debugger {

    ResponseTrans res;
    TracerFrame frame = null;

    /**
     *  Create a tracer that writes to this response object
     * @param res the response object to write to
     */
    public Tracer(ResponseTrans res) {
        this.res = res;
    }

    /**
     *  Implementws handleCompilationDone in interface org.mozilla.javascript.debug.Debugger
     */
    public void handleCompilationDone(Context cx, DebuggableScript script, String source) {}

    /**
     *  Implements getFrame() in interface org.mozilla.javascript.debug.Debugger
     */
    public DebugFrame getFrame(Context cx, DebuggableScript script) {
        if (script.isFunction()) {
            frame = new TracerFrame(script, frame);
            return frame;
        }
        return null;
    }

    /**
     * Get a string representation for the given script
     * @param script a function or script
     * @return the file and/or function name of the script
     */
    static String toString(DebuggableScript script) {
        if (script.isFunction()) {
            return script.getSourceName() + ": " + script.getFunctionName();
        } else {
            return script.getSourceName();
        }
    }

    class TracerFrame implements DebugFrame {

        TracerFrame parent;
        ArrayList children = new ArrayList();
        DebuggableScript script;
        int currentLine, depth = 0;
        long time;

        TracerFrame(DebuggableScript script, TracerFrame parent) {
            this.script = script;
            this.parent = parent;
            if (parent != null) {
                parent.children.add(this);
                depth = parent.depth + 1;
            }
        }

        /**
         * Called when execution is ready to start bytecode interpretation
         * for entered a particular function or script.
         */
        public void onEnter(Context cx, Scriptable activation, 
                            Scriptable thisObj, Object[] args) {

            time = System.currentTimeMillis();
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
        public void onExit(Context cx, boolean byThrow, Object resultOrException) {
            time = System.currentTimeMillis() - time;
            frame = parent;
            if (parent == null)
                render();
        }

        /**
         * Called when the function or script executes a 'debugger' statement.
         *
         * @param cx current Context for this thread
         */
        public void onDebuggerStatement(Context cx) {
            // not implemented
        }

        /**
         *  Called when executed code reaches new line in the source.
         */
        public void onLineChange(Context cx, int lineNumber) {
            currentLine = lineNumber;
        }

        public void render() {
            // Simplify Trace by dropping fast invocations. May be useful when looking
            // looking for bottlenecks, but not when trying to find out wtf is going on
            // if (time <= 1)
            //     return;            
            StringBuffer b = new StringBuffer("Trace: ");
            for (int i = 0; i < depth; i++)
                b.append(".&nbsp;");
            b.append(Tracer.toString(script));
            b.append("(");

            /* for (int i = 0; i < args.length; i++) {
               b.append(HtmlEncoder.encodeAll(ScriptRuntime.toString(args[i])));

               if (i < args.length - 1) {
                   b.append(", ");
               }
           } */

            b.append(")");

            b.append(" <b>");
            b.append(time);
            b.append(" millis</b>");

            res.debug(b);

            for (int i = 0; i < children.size(); i++)
                ((TracerFrame) children.get(i)).render();
        }
    }
}

