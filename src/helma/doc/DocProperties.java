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

import helma.util.ResourceProperties;
import helma.framework.repository.Resource;

import java.util.*;
import java.io.IOException;

/**
 * Documentation around a properties file
 */
public class DocProperties extends DocResourceElement {

    ResourceProperties props;
    String elementName;

    protected DocProperties(Resource res, ResourceProperties props,
                            int index, DocElement parent)
                     throws DocException, IOException {
        super(res.getShortName(), res, PROPERTIES);

        this.parent = parent;
        this.props = props;
        this.comment = resource.getName();
        this.content = resource.getContent();
        this.elementName = name + "_" + index;
    }

    /**
     * Get the underlying properties
     *
     * @return the properties
     */
    public ResourceProperties getProperties() {
        return props;
    }

    public String getElementName() {
        return elementName;
    }

    /**
     * returns the comment string, empty string if no comment is set.
     */
    public String getComment() {
        return resource.getName();
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

    /**
     * from helma.framework.IPathElement. Use the same prototype as functions etc.
     */
    public java.lang.String getPrototype() {
        return "docfunction";
    }
}
