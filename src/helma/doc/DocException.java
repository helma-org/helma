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
public class DocException extends RuntimeException {
    String str;

    /**
     * Creates a new DocException object.
     *
     * @param str ...
     */
    public DocException(String str) {
        super(str);
        this.str = str;
    }

    /**
     *
     *
     * @return ...
     */
    public String getMessage() {
        return str;
    }
}
