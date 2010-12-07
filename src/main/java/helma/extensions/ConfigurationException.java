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

package helma.extensions;

/**
 * 
 */
public class ConfigurationException extends RuntimeException {
    private static final long serialVersionUID = 6428439427909728917L;

    /**
     * Creates a new ConfigurationException object.
     *
     * @param msg ...
     */
    public ConfigurationException(String msg) {
        super(msg);
    }
}
