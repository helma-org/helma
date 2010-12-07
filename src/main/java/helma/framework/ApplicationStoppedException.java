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
 * This is thrown when a request is made to a stopped
 * application
 */
public class ApplicationStoppedException extends RuntimeException {
    private static final long serialVersionUID = 7125229844095452333L;

    /**
     * Creates a new ApplicationStoppedException object.
     */
    public ApplicationStoppedException() {
        super("The application has been stopped");
    }
}
