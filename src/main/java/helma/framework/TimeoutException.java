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
 * TimeoutException is thrown by the request evaluator when a request could
 * not be serviced within the timeout period specified for an application.
 */
public class TimeoutException extends RuntimeException {
    private static final long serialVersionUID = 3853135482278393735L;

    /**
     * Creates a new TimeoutException object.
     */
    public TimeoutException() {
        super("Request timed out");
    }
}
