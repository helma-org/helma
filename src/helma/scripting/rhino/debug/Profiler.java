package helma.scripting.rhino.debug;

import org.mozilla.javascript.debug.Debugger;
import org.mozilla.javascript.debug.DebuggableScript;
import org.mozilla.javascript.debug.DebugFrame;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import java.util.*;

public class Profiler implements Debugger {

    HashMap frames = new HashMap();

    /**
     * Create a profiler that writes to this response object
     */
    public Profiler() {
    }

    /**
     * Implementws handleCompilationDone in interface org.mozilla.javascript.debug.Debugger
     */
    public void handleCompilationDone(Context cx, DebuggableScript script, String source) {}

    /**
     *  Implements getFrame() in interface org.mozilla.javascript.debug.Debugger
     */
    public DebugFrame getFrame(Context cx, DebuggableScript script) {
        if (script.isFunction()) {
            String name = getFunctionName(script);
            ProfilerFrame frame = (ProfilerFrame) frames.get(name);
            if (frame == null) {
                frame = new ProfilerFrame(name);
                frames.put(name, frame);
            }
            return frame;
        }
        return null;
    }

    /**
     * Get a string representation for the given script
     * @param script a function or script
     * @return the file and/or function name of the script
     */
    static String getFunctionName(DebuggableScript script) {
        if (script.isFunction()) {
            if (script.getFunctionName() != null) {
                return script.getSourceName() + ": " + script.getFunctionName();
            } else {
                return script.getSourceName() + ": #" + script.getLineNumbers()[0];
            }
        } else {
            return script.getSourceName();
        }
    }

    public String getResult() {
        ProfilerFrame[] f = (ProfilerFrame[]) frames.values().toArray(new ProfilerFrame[0]);
        Arrays.sort(f, new Comparator() {
            public int compare(Object o1, Object o2) {
                return ((ProfilerFrame)o2).runtime - ((ProfilerFrame)o1).runtime;
            }
        });
        StringBuffer buffer = new StringBuffer("     total  average  calls    path\n");
        buffer.append("==================================================================\n");
        for (int i = 0; i < Math.min(100, f.length); i++) {
            buffer.append(f[i].renderLine(0));
        }
        return buffer.toString();
    }

    class ProfilerFrame implements DebugFrame {

        Stack timer = new Stack();
        int runtime, invocations, lineNumber;
        String name;

        ProfilerFrame(String name) {
            this.name = name;
        }

        /**
         * Called when execution is ready to start bytecode interpretation
         * for entered a particular function or script.
         */
        public void onEnter(Context cx, Scriptable activation,
                            Scriptable thisObj, Object[] args) {

            long time = System.nanoTime();
            timer.push(new Long(time));
        }

        /**
         *  Called when thrown exception is handled by the function or script.
         */
        public void onExceptionThrown(Context cx, Throwable ex) {
            invocations ++;
            Long time = (Long) timer.pop();
            runtime += System.nanoTime() - time.longValue();
        }

        /**
         *  Called when the function or script for this frame is about to return.
         */
        public void onExit(Context cx, boolean byThrow, Object resultOrException) {
            invocations ++;
            Long time = (Long) timer.pop();
            runtime += System.nanoTime() - time.longValue();
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
            this.lineNumber = lineNumber;
        }

        public String renderLine(int prefixLength) {
            long millis = Math.round(this.runtime / 1000000);
            Formatter formatter = new java.util.Formatter();
            Object[] args = new Object[] {
                    Integer.valueOf((int) millis),
                    Integer.valueOf(Math.round(millis / invocations)),
                    Integer.valueOf(invocations),
                    name.substring(prefixLength)
            };
            formatter.format("%1$7d ms %2$5d ms %3$6d %4$s%n", args);
            return formatter.toString();
        }
    }
}
