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
 *  This class describes a parent relation between releational nodes.
 */
public class ParentInfo {
    public final String propname;
    public final String virtualname;
    public final boolean named;
    public final boolean isroot;

    /**
     * Creates a new ParentInfo object.
     *
     * @param desc ...
     */
    public ParentInfo(String desc) {
        int n = desc.indexOf("[named]");

        named = n > -1;

        String d = named ? desc.substring(0, n) : desc;

        int dot = d.indexOf(".");

        if (dot > -1) {
            propname = d.substring(0, dot).trim();
            virtualname = d.substring(dot + 1).trim();
        } else {
            propname = d.trim();
            virtualname = null;
        }

        isroot = "root".equals(propname);

        // System.err.println ("created "+this);
    }

    /**
     *
     *
     * @return ...
     */
    public String toString() {
        return "ParentInfo[" + propname + "," + virtualname + "," + named + "]";
    }
}
