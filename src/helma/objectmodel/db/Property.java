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

import helma.objectmodel.INode;
import helma.objectmodel.IProperty;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A property implementation for Nodes stored inside a database. Basically
 * the same as for transient nodes, with a few hooks added.
 */
public final class Property implements IProperty, Serializable, Cloneable, Comparable {
    static final long serialVersionUID = -1022221688349192379L;
    private String propname;
    private Node node;
    private Object value;
    private int type;
    transient boolean dirty;

    /**
     * Creates a new Property object.
     *
     * @param node ...
     */
    public Property(Node node) {
        this.node = node;
        dirty = true;
    }

    /**
     * Creates a new Property object.
     *
     * @param propname ...
     * @param node ...
     */
    public Property(String propname, Node node) {
        this.propname = propname;
        this.node = node;
        dirty = true;
    }

    /**
     * Creates a new Property object.
     *
     * @param propname ...
     * @param node ...
     * @param valueNode ...
     */
    public Property(String propname, Node node, Node valueNode) {
        this(propname, node);
        type = NODE;
        value = (valueNode == null) ? null : valueNode.getHandle();
        dirty = true;
    }

    private void readObject(ObjectInputStream in) throws IOException {
        try {
            propname = in.readUTF();
            node = (Node) in.readObject();
            type = in.readInt();

            switch (type) {
                case STRING:
                    value = in.readObject();

                    break;

                case BOOLEAN:
                    value = in.readBoolean() ? Boolean.TRUE : Boolean.FALSE;

                    break;

                case INTEGER:
                    value = new Long(in.readLong());

                    break;

                case DATE:
                    value = new Date(in.readLong());

                    break;

                case FLOAT:
                    value = new Double(in.readDouble());

                    break;

                case NODE:
                    value = in.readObject();

                    break;

                case JAVAOBJECT:
                    value = in.readObject();

                    break;
            }
        } catch (ClassNotFoundException x) {
            throw new IOException(x.toString());
        }
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeUTF(propname);
        out.writeObject(node);
        out.writeInt(type);

        switch (type) {
            case STRING:
                out.writeObject(value);

                break;

            case BOOLEAN:
                out.writeBoolean(((Boolean) value).booleanValue());

                break;

            case INTEGER:
                out.writeLong(((Long) value).longValue());

                break;

            case DATE:
                out.writeLong(((Date) value).getTime());

                break;

            case FLOAT:
                out.writeDouble(((Double) value).doubleValue());

                break;

            case NODE:
                out.writeObject(value);

                break;

            case JAVAOBJECT:

                if ((value != null) && !(value instanceof Serializable)) {
                    out.writeObject(null);
                } else {
                    out.writeObject(value);
                }

                break;
        }
    }

    /**
     *  Get the name of the property
     *
     * @return this property's name
     */
    public String getName() {
        return propname;
    }

    /**
     *  Set the name of the property
     */
    protected void setName(String name) {
        this.propname = name;
    }

    /**
     *
     *
     * @return the property's value in its native class
     */
    public Object getValue() {
        return value;
    }

    /**
     *
     *
     * @return the property's type as defined in helma.objectmodel.IProperty.java
     */
    public int getType() {
        return type;
    }

    /**
     * Directly set the value of this property.
     */
    protected void setValue(Object value, int type) {
        this.value = value;
        this.type = type;
        dirty = true;
    }

    /**
     *
     *
     * @param str ...
     */
    public void setStringValue(String str) {
        type = STRING;
        value = str;
        dirty = true;
    }

    /**
     *
     *
     * @param l ...
     */
    public void setIntegerValue(long l) {
        type = INTEGER;
        value = new Long(l);
        dirty = true;
    }

    /**
     *
     *
     * @param d ...
     */
    public void setFloatValue(double d) {
        type = FLOAT;
        value = new Double(d);
        dirty = true;
    }

    /**
     *
     *
     * @param date ...
     */
    public void setDateValue(Date date) {
        type = DATE;
        value = date;
        dirty = true;
    }

    /**
     *
     *
     * @param bool ...
     */
    public void setBooleanValue(boolean bool) {
        type = BOOLEAN;
        value = bool ? Boolean.TRUE : Boolean.FALSE;
        dirty = true;
    }

    /**
     *
     *
     * @param node ...
     */
    public void setNodeValue(Node node) {
        type = NODE;
        value = (node == null) ? null : node.getHandle();
        dirty = true;
    }

    /**
     *
     *
     * @param handle ...
     */
    public void setNodeHandle(NodeHandle handle) {
        type = NODE;
        value = handle;
        dirty = true;
    }

    /**
     *
     *
     * @return ...
     */
    public NodeHandle getNodeHandle() {
        if (type == NODE) {
            return (NodeHandle) value;
        }

        return null;
    }

    /**
     *
     *
     * @param dbm ...
     */
    public void convertToNodeReference(DbMapping dbm) {
        if ((value != null) && !(value instanceof NodeHandle)) {
            value = new NodeHandle(new DbKey(dbm, value.toString()));
        }

        type = NODE;
    }

    /**
     *
     *
     * @param obj ...
     */
    public void setJavaObjectValue(Object obj) {
        type = JAVAOBJECT;
        value = obj;
    }


    /**
     *
     *
     * @return ...
     */
    public String getStringValue() {
        if (value == null) {
            return null;
        }

        switch (type) {
            case STRING:
            case BOOLEAN:
            case INTEGER:
            case FLOAT:
            case JAVAOBJECT:
                return value.toString();

            case DATE:

                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                return format.format((Date) value);

            case NODE:
                return ((NodeHandle) value).getID();
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
            return ((Long) value).longValue();
        }

        if (type == FLOAT) {
            return ((Double) value).longValue();
        }

        if (type == BOOLEAN) {
            return ((Boolean) value).booleanValue() ? 1 : 0;
        }

        try {
            return Long.parseLong(getStringValue());
        } catch (Exception x) {
            return 0;
        }
    }

    /**
     *
     *
     * @return ...
     */
    public double getFloatValue() {
        if (type == FLOAT) {
            return ((Double) value).doubleValue();
        }

        if (type == INTEGER) {
            return ((Long) value).doubleValue();
        }

        try {
            return Double.parseDouble(getStringValue());
        } catch (Exception x) {
            return 0.0;
        }
    }

    /**
     *
     *
     * @return ...
     */
    public Date getDateValue() {
        if (type == DATE) {
            return (Date) value;
        }

        return null;
    }

    /**
     *
     *
     * @return ...
     */
    public Timestamp getTimestampValue() {
        if ((type == DATE) && (value != null)) {
            return new Timestamp(((Date) value).getTime());
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
            return ((Boolean) value).booleanValue();
        }

        if (type == INTEGER) {
            return !(0 == getIntegerValue());
        }

        return false;
    }

    /**
     *
     *
     * @return ...
     */
    public INode getNodeValue() {
        if ((type == NODE) && (value != null)) {
            NodeHandle nhandle = (NodeHandle) value;

            return nhandle.getNode(node.nmgr);
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
            return value;
        }

        return null;
    }

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     * FIXME: throw a ClassCastException instead?
     * The following cases throw a RuntimeException
     * - Properties of a different type
     * - Properties of boolean type
     */
    public int compareTo(Object obj) {
        Property p = (Property) obj;
        int pt = p.getType();
        
        if (type==NODE || pt==NODE)
            throw new RuntimeException ("unable to compare nodes " + this + " : " + p);
        if (value==null && p.getStringValue() == null)
            return 0;
        if (value==null)
            return -1;
        if (p.getStringValue()==null)
            return 1;
        if (pt != type)
            throw new RuntimeException ("uncomparable values " + this + "(" + type + ") : " + p + "(" + pt + ")");
        switch (pt) {
            case Property.STRING:
                return (this.getStringValue().compareTo(p.getStringValue()));
            case Property.INTEGER:
                long l = this.getIntegerValue() - p.getIntegerValue();
                // don't know what happens if the result of the subtraction
                // has a higher value than the value which may be stored inside
                // of an integer.
                if (l < 0)
                    return -1;
                if (l > 0)
                    return 1;
                return 0;
            case Property.DATE:
                return this.getDateValue().compareTo(p.getDateValue());
            case Property.FLOAT:
                double d = this.getFloatValue() - p.getFloatValue();
                if (d < 0)
                    return -1;
                if (d > 0)
                    return 1;
                return 0;
            case Property.BOOLEAN:
                throw new RuntimeException ("unable to compare boolean " + this + " : " + p);
            case Property.JAVAOBJECT:
        }
        throw new RuntimeException ("uncomparable values " + this + " : " + p);
    }

    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (o == null)
            return false;
        if (!(o instanceof Property))
            return false;
        Property p = (Property) o;
        switch (p.getType()) {
            case Property.STRING:
                return (this.getStringValue().equals(p.getStringValue()));
            case Property.INTEGER:
                return this.getIntegerValue() == p.getIntegerValue();
            case Property.DATE:
                return this.getDateValue().equals(p.getDateValue());
            case Property.FLOAT:
                return this.getFloatValue() == p.getFloatValue();
            case Property.BOOLEAN:
                return this.getBooleanValue() == p.getBooleanValue();
            case Property.NODE:
                return this.node.equals(p.node);
            case Property.JAVAOBJECT:
                return this.value.equals(p.value);
        }
        return false;
    }
}
