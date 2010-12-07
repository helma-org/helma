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
 * new URL. Although this is not an Error, it subclasses java.lang.Error
 * because it's not meant to be caught by application code (similar to
 * java.lang.ThreadDeath).
 */
public class RedirectException extends Error {
    private static final long serialVersionUID = 2362170037476457592L;

    String url;

    /**
     * Creates a new RedirectException object.
     *
     * @param url the URL
     */
    public RedirectException(String url) {
        super("Redirection Request to " + url);
        this.url = url;
    }

    /**
     * Return the URL to redirect to.
     * @return the URL
     */
    public String getUrl() {
        return url;
    }

}
