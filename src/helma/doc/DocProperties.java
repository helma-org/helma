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

import helma.framework.IPathElement;
import helma.util.SystemProperties;
import java.io.*;
import java.util.*;

/**
 * 
 */
public class DocProperties extends DocFileElement {
    Properties props = null;

    protected DocProperties(File location, DocElement parent)
                     throws DocException {
        super(location.getName(), location, PROPERTIES);
        this.parent = parent;
        content = Util.readFile(location);
        props = new SystemProperties();

        try {
            props.load(new FileInputStream(location));
        } catch (IOException e) {
            debug("couldn't read file: " + e.toString());
        } catch (Exception e) {
            throw new DocException(e.toString());
        }
    }

    /**
     * creates a new independent DocProperties object
     */
    public static DocProperties newInstance(File location) {
        return newInstance(location, null);
    }

    /**
     * creates a new DocProperties object connected to another DocElement
     */
    public static DocProperties newInstance(File location, DocElement parent) {
        try {
            return new DocProperties(location, parent);
        } catch (DocException doc) {
            return null;
        }
    }

    /**
     *
     *
     * @return ...
     */
    public Properties getProperties() {
        return props;
    }

    /**
     *
     *
     * @return ...
     */
    public Properties getMappings() {
        Properties childProps = new Properties();

        for (Enumeration e = props.keys(); e.hasMoreElements();) {
            String key = (String) e.nextElement();
            String value = props.getProperty(key);

            if (value.startsWith("collection") || value.startsWith("object") ||
                    value.startsWith("mountpoint")) {
                String prototype = value.substring(value.indexOf("(") + 1,
                                                   value.indexOf(")")).trim();

                childProps.setProperty(key, prototype);
            }
        }

        return childProps;
    }
}
