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

package helma.framework;


/**
 * RedirectException is thrown internally when a response is redirected to a
 * new URL.
 */
public class RedirectException extends RuntimeException {
    String url;

    /**
     * Creates a new RedirectException object.
     *
     * @param url ...
     */
    public RedirectException(String url) {
        super("Redirection Request to " + url);
        this.url = url;
    }

    /**
     *
     *
     * @return ...
     */
    public String getMessage() {
        return url;
    }

    /**
     *
     *
     * @param s ...
     */
    public void printStackTrace(java.io.PrintStream s) {
        // do nothing
    }

    /**
     *
     *
     * @param w ...
     */
    public void printStackTrace(java.io.PrintWriter w) {
        // do nothing
    }
}
