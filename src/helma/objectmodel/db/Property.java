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
        // normalize from java.sql.* Date subclasses
        if (date != null && date.getClass() != Date.class) {
            value = new Date(date.getTime());
        } else {
            value = date;
        }
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
     * @param rel the Relation
     */
    public void convertToNodeReference(Relation rel) {
        if ((value != null) && !(value instanceof NodeHandle)) {
            if (rel.usesPrimaryKey()) {
                value = new NodeHandle(new DbKey(rel.otherType, value.toString()));
            } else {
                value = new NodeHandle(new MultiKey(rel.otherType, rel.getKeyParts(node)));
            }
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
        dirty = true;
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

        if (type == INTEGER || type == FLOAT) {
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
     *
     * The following cases throw a ClassCastException
     * - Properties of a different type
     * - Properties of boolean or node type
     */
    public int compareTo(Object obj) {
        Property p = (Property) obj;
        int ptype = p.getType();
        Object pvalue = p.getValue();

        if (type==NODE || ptype==NODE ||
                type == BOOLEAN || ptype == BOOLEAN) {
            throw new ClassCastException("uncomparable values " + this + "(" + type + ") : " + p + "(" + ptype + ")");
        }
        if (value==null && pvalue == null) {
            return 0;
        } else if (value == null) {
            return 1;
        } if (pvalue == null) {
            return -1;
        }
        if (type != ptype) {
            // float/integer sometimes get mixed up in Rhino
            if ((type == FLOAT && ptype == INTEGER) || (type == INTEGER && ptype == FLOAT))
                return Double.compare(((Number) value).doubleValue(), ((Number) pvalue).doubleValue());
            throw new ClassCastException("uncomparable values " + this + "(" + type + ") : " + p + "(" + ptype + ")");

        }
        if (!(value instanceof Comparable)) {
            throw new ClassCastException("uncomparable value " + value + "(" + value.getClass() + ")");
        }
        // System.err.println("COMPARING: " + value.getClass() + " TO " + pvalue.getClass());
        return ((Comparable) value).compareTo(pvalue);
    }

    /**
     * Return true if object o is equal to this property.
     *
     * @param obj the object to compare to
     * @return true if this equals obj
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof Property))
            return false;
        Property p = (Property) obj;
        return value == null ? p.value == null : value.equals(p.value);
    }
}