// RedirectException.java
// Copyright (c) Hannes Wallnöfer 1998-2000

package helma.framework;


/**
 * RedirectException is thrown internally when a response is redirected to a
 * new URL.
 */

public class RedirectException extends RuntimeException {

    String url;

    public RedirectException (String url) {
	super ("Redirection Request to "+url);
	this.url = url;
    }

    public String getMessage () {
	return url;
    }

    public void printStackTrace(java.io.PrintStream s) {
	// do nothing
    }

    public void printStackTrace(java.io.PrintWriter w) {
	// do nothing
    }

}
