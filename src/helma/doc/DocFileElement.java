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

import java.io.File;

/**
 * abstract class for extracting doc information from files.
 * not used at the moment but left in for further extensions-
 */
public abstract class DocFileElement extends DocElement {
    protected DocFileElement(String name, File location, int type) {
        super(name, location, type);
    }





}
