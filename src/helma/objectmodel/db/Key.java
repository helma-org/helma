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

package helma.objectmodel.db;


/**
 * This is the interface for the internal representation of an object key.
 *
 */
public interface Key {

    /**
     * Get the key's parent key
     *
     * @return ...
     */
    public Key getParentKey();

    /**
     * Get the key's ID part
     *
     * @return ...
     */
    public String getID();

    /**
     *  Get the key's storage id
     *
     * @return ...
     */
    public String getStorageName();

}
