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

import helma.framework.repository.Resource;

/**
 * abstract class for extracting doc information from files.
 * not used at the moment but left in for further extensions-
 */
public abstract class DocResourceElement extends DocElement {

    protected Resource resource;

    protected DocResourceElement(String name, Resource res, int type) {
        super(name, type);
        this.resource = res;
    }

    /**
     * Get a string describing this element's location
     *
     * @return string representation of the element's resource
     */
    public String toString() {
        return resource.getName();
    }
}
