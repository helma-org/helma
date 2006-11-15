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

package helma.doc;

/**
 * 
 */
public class DocException extends Exception {

    /**
     * Creates a new DocException object.
     *
     * @param msg ...
     */
    public DocException(String msg) {
        super(msg);
    }

    /**
     * Creates a new DocException object.
     *
     * @param msg the exception message
     * @param t the cause
     */
    public DocException(String msg, Throwable t) {
        super(msg, t);
    }

}
