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

package helma.objectmodel;

import helma.util.*;
import java.io.*;
import java.text.*;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * A property implementation for Nodes stored inside a database.
 */
public final class Property implements IProperty, Serializable {
    protected String propname;
    protected TransientNode node;
    public String svalue;
    public boolean bvalue;
    public long lvalue;
    public double dvalue;
    public INode nvalue;
    public Object jvalue;
    public int type;

    /**
     * Creates a new Property object.
     *
     * @param node ...
     */
    public Property(TransientNode node) {
        this.node = node;
    }

    /**
     * Creates a new Property object.
     *
     * @param propname ...
     * @param node ...
     */
    public Property(String propname, TransientNode node) {
        this.propname = propname;
        this.node = node;
    }

    /**
     *
     *
     * @return ...
     */
    public String getName() {
        return propname;
    }

    /**
     *
     *
     * @return ...
     */
    public Object getValue() {
        switch (type) {
            case STRING:
                return svalue;

            case BOOLEAN:
                return new Boolean(bvalue);

            case INTEGER:
                return new Long(lvalue);

            case FLOAT:
                return new Double(dvalue);

            case DATE:
                return new Date(lvalue);

            case NODE:
                return nvalue;

            case JAVAOBJECT:
                return jvalue;
        }

        return null;
    }

    /**
     *
     *
     * @param value ...
     */
    public void setStringValue(String value) {
        if (type == NODE) {
            this.nvalue = null;
        }

        if (type == JAVAOBJECT) {
            this.jvalue = null;
        }

        type = STRING;
        this.svalue = value;
    }

    /**
     *
     *
     * @param value ...
     */
    public void setIntegerValue(long value) {
        if (type == NODE) {
            this.nvalue = null;
        }

        if (type == JAVAOBJECT) {
            this.jvalue = null;
        }

        type = INTEGER;
        this.lvalue = value;
    }

    /**
     *
     *
     * @param value ...
     */
    public void setFloatValue(double value) {
        if (type == NODE) {
            this.nvalue = null;
        }

        if (type == JAVAOBJECT) {
            this.jvalue = null;
        }

        type = FLOAT;
        this.dvalue = value;
    }

    /**
     *
     *
     * @param value ...
     */
    public void setDateValue(Date value) {
        if (type == NODE) {
            this.nvalue = null;
        }

        if (type == JAVAOBJECT) {
            this.jvalue = null;
        }

        type = DATE;
        this.lvalue = value.getTime();
    }

    /**
     *
     *
     * @param value ...
     */
    public void setBooleanValue(boolean value) {
        if (type == NODE) {
            this.nvalue = null;
        }

        if (type == JAVAOBJECT) {
            this.jvalue = null;
        }

        type = BOOLEAN;
        this.bvalue = value;
    }

    /**
     *
     *
     * @param value ...
     */
    public void setNodeValue(INode value) {
        if (type == JAVAOBJECT) {
            this.jvalue = null;
        }

        type = NODE;
        this.nvalue = value;
    }

    /**
     *
     *
     * @param value ...
     */
    public void setJavaObjectValue(Object value) {
        if (type == NODE) {
            this.nvalue = null;
        }

        type = JAVAOBJECT;
        this.jvalue = value;
    }

    /**
     *
     *
     * @return ...
     */
    public String getStringValue() {
        switch (type) {
            case STRING:
                return svalue;

            case BOOLEAN:
                return "" + bvalue;

            case DATE:

                SimpleDateFormat format = new SimpleDateFormat("dd.MM.yy HH:mm");

                return format.format(new Date(lvalue));

            case INTEGER:
                return Long.toString(lvalue);

            case FLOAT:
                return Double.toString(dvalue);

            case NODE:
                return nvalue.getName();

            case JAVAOBJECT:
                return (jvalue == null) ? null : jvalue.toString();
        }

        return "";
    }

    /**
     *
     *
     * @return ...
     */
    public String toString() {
        return getStringValue();
    }

    /**
     *
     *
     * @return ...
     */
    public long getIntegerValue() {
        if (type == INTEGER) {
            return lvalue;
        }

        return 0;
    }

    /**
     *
     *
     * @return ...
     */
    public double getFloatValue() {
        if (type == FLOAT) {
            return dvalue;
        }

        return 0.0;
    }

    /**
     *
     *
     * @return ...
     */
    public Date getDateValue() {
        if (type == DATE) {
            return new Date(lvalue);
        }

        return null;
    }

    /**
     *
     *
     * @return ...
     */
    public boolean getBooleanValue() {
        if (type == BOOLEAN) {
            return bvalue;
        }

        return false;
    }

    /**
     *
     *
     * @return ...
     */
    public INode getNodeValue() {
        if (type == NODE) {
            return nvalue;
        }

        return null;
    }

    /**
     *
     *
     * @return ...
     */
    public Object getJavaObjectValue() {
        if (type == JAVAOBJECT) {
            return jvalue;
        }

        return null;
    }

    /**
     *
     *
     * @return ...
     */
    public int getType() {
        return type;
    }
}
