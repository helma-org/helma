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

import helma.objectmodel.*;
import helma.util.*;
import java.io.*;
import java.sql.Timestamp;
import java.text.*;
import java.util.*;

/**
 * A property implementation for Nodes stored inside a database. Basically
 * the same as for transient nodes, with a few hooks added.
 */
public final class Property implements IProperty, Serializable, Cloneable {
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

                    // try to convert from old format
                    if (node.version < 7) {
                        value = in.readUTF();
                    } else {
                        value = in.readObject();
                    }

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

                    // try to convert from old format
                    if (node.version > 4) {
                        value = (NodeHandle) in.readObject();
                    } else {
                        value = new NodeHandle(new DbKey(null, in.readUTF()));
                    }

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
        if (type == NODE) {
            unregisterNode();
        }
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
        if (type == NODE) {
            unregisterNode();
        }

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
        if (type == NODE) {
            unregisterNode();
        }

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
        if (type == NODE) {
            unregisterNode();
        }

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
        if (type == NODE) {
            unregisterNode();
        }

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
        if (type == NODE) {
            unregisterNode();
        }

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
        // value.checkWriteLock ();
        if (type == NODE) {
            unregisterNode();
        }

        // registerNode (value);
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
        if (type == NODE) {
            unregisterNode();
        }

        // registerNode (value);
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
        if (type == NODE) {
            unregisterNode();
        }

        type = JAVAOBJECT;
        value = obj;
    }

    /**
     * tell a the value node that it is no longer used as a property.
     * If this was the "main" property for the node, also remove all other references.
     */
    protected void unregisterNode() {
        if ((value == null) || !(value instanceof NodeHandle)) {
            return;
        }

        NodeHandle nhandle = (NodeHandle) value;
        Node nvalue = nhandle.getNode(node.nmgr);

        DbMapping nvmap = null;
        Relation nvrel = null;

        if (node.dbmap != null) {
            nvmap = node.dbmap.getPropertyMapping(propname);
            nvrel = node.dbmap.getPropertyRelation(propname);
        }

        if (nvalue == null) {
            return;
        }

        nvalue.checkWriteLock();

        // check if the property node is also a subnode
        // BUG: this doesn't work because properties for subnode/properties are never stored and therefore
        // never reused.
        if ((nvrel != null) && nvrel.hasAccessName()) {
            node.removeNode(nvalue);
        }

        // only need to call unregisterPropLink if the value node is not stored in a relational db
        // also, getParent is heuristical/implicit for relational nodes, so we don't do deepRemoveNode
        // based on that for relational nodes.
        if ((nvmap == null) || !nvmap.isRelational()) {
            if (!nvalue.isAnonymous() && propname.equals(nvalue.getName()) &&
                    (this.node == nvalue.getParent())) {
                // this is the "main" property of a named node, so handle this as a cascading delete.
                nvalue.deepRemoveNode();
            }
        }
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

                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

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
}
