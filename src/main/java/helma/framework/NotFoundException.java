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
 * The basic exception class used to tell when certain things go
 * wrong in evaluation of requests.
 */
public class NotFoundException extends RuntimeException {
    /**
     * Creates a new NotFoundException object.
     *
     * @param message ...
     */
    public NotFoundException(String message) {
        super(message);
    }

    /**
     * Creates a new NotFoundException object with a cause.
     *
     * @param message the message
     * @param cause the cause
     */
    public NotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
