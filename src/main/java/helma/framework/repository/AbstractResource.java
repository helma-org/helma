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

package helma.framework.repository;

/**
 * Abstract resource base class that implents get/setOverloadedResource.
 */
public abstract class AbstractResource implements Resource {

    protected Resource overloaded = null;

    /**
     * Method for registering a Resource this Resource is overloading
     *
     * @param res the overloaded resource
     */
    public void setOverloadedResource(Resource res) {
        overloaded = res;
    }

    /**
     * Get a Resource this Resource is overloading
     *
     * @return the overloaded resource
     */
    public Resource getOverloadedResource() {
        return overloaded;
    }
}
