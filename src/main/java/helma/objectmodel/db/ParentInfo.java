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

import helma.util.StringUtils;


/**
 *  This class describes a parent relation between releational nodes.
 */
public class ParentInfo {
    public final String propname;
    public final String virtualname;
    public final String collectionname;
    public final boolean isroot;

    /**
     * Creates a new ParentInfo object.
     *
     * @param desc a single parent info descriptor
     */
    public ParentInfo(String desc) {

        // [named] isn't used anymore, we just want to keep the parsing compatible.
        int n = desc.indexOf("[named]");
        desc = n > -1 ? desc.substring(0, n) : desc;

        String[] parts = StringUtils.split(desc, ".");

        propname = parts.length > 0 ? parts[0].trim() : null;
        virtualname = parts.length > 1 ? parts[1].trim() : null;
        collectionname = parts.length > 2 ? parts[2].trim() : null;

        isroot = "root".equalsIgnoreCase(propname);
    }

    /**
     * @return a string representation of the parent info
     */
    public String toString() {
        StringBuffer b = new StringBuffer("ParentInfo[").append(propname);
        if (virtualname != null)
            b.append(".").append(virtualname);
        if (collectionname != null)
            b.append(".").append(collectionname);
        return b.append("]").toString();
    }
}
