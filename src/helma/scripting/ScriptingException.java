// ScriptingException.java
// Copyright (c) Hannes Wallnöfer 1998-2001

package helma.scripting;


import java.io.PrintWriter;
import java.io.PrintStream;

/**
 * The base class for exceptions thrown by Helma scripting package
 */
public class ScriptingException extends Exception {

    Exception wrapped;

    /**
     * Construct a ScriptingException given an error message
     */
    public ScriptingException (String msg) {
	super (msg);
	wrapped = null;
    }

    /**
     * Construct a ScriptingException given an error message
     */
    public ScriptingException (Exception w) {
	wrapped = w;
    }

    public String toString () {
	if (wrapped == null)
	    return super.toString ();
	else
	    return wrapped.toString ();
    }

    public String getMessage () {
	if (wrapped == null)
	    return super.getMessage ();
	else
	    return wrapped.getMessage ();
    }

    public void printStackTrace () {
	if (wrapped == null)
	    super.printStackTrace ();
	else
	    wrapped.printStackTrace ();
    }

    public void printStackTrace (PrintStream stream) {
	if (wrapped == null)
	    super.printStackTrace (stream);
	else
	    wrapped.printStackTrace (stream);
    }

    public void printStackTrace (PrintWriter writer) {
	if (wrapped == null)
	    super.printStackTrace (writer);
	else
	    wrapped.printStackTrace (writer);
    }

}
