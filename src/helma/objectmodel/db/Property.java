// Property.java
// Copyright (c) Hannes Wallnöfer 1997-2000
 
package helma.objectmodel.db;

import helma.util.*;
import java.util.*;
import java.io.*;
import java.text.*;
import helma.objectmodel.*;
import java.sql.Timestamp;

/**
 * A property implementation for Nodes stored inside a database. Basically
 * the same as for transient nodes, with a few hooks added.
 */
public final class Property implements IProperty, Serializable, Cloneable {


    private String propname;
    private Node node;

    private Object value;

    private int type;

    transient boolean dirty;

    static final long serialVersionUID = -1022221688349192379L;

    private void readObject (ObjectInputStream in) throws IOException {
	try {
	    propname = in.readUTF ();
	    node = (Node) in.readObject ();
	    type = in.readInt ();
	    switch (type) {
	    case STRING:
	        // try to convert from old format
	        if (node.version < 7)
	            value = in.readUTF ();
	        else
	            value = in.readObject ();
	        break;
	    case BOOLEAN:
	        value = in.readBoolean () ? Boolean.TRUE : Boolean.FALSE;
	        break;
	    case INTEGER:
	        value = new Long (in.readLong ());
	        break;
	    case DATE:
	        value = new Date (in.readLong ());
	        break;
	    case FLOAT:
	        value = new Double (in.readDouble ());
	        break;
	    case NODE:
	        // try to convert from old format
	        if (node.version > 4)
	            value = (NodeHandle) in.readObject ();
	        else
	            value = new NodeHandle (new DbKey (null, in.readUTF ()));
	        break;
	    case JAVAOBJECT:
	        value = in.readObject ();
	        break;
	    }
	} catch (ClassNotFoundException x) {
	    throw new IOException (x.toString ());
	}
    }

    private void writeObject (ObjectOutputStream out) throws IOException {
	out.writeUTF (propname);
	out.writeObject (node);
	out.writeInt (type);
	switch (type) {
	case STRING:
	    out.writeObject (value);
	    break;
	case BOOLEAN:
	    out.writeBoolean (((Boolean) value).booleanValue());
	    break;
	case INTEGER:
	    out.writeLong (((Long) value).longValue());
	    break;
	case DATE:
	    out.writeLong (((Date) value).getTime());
	    break;
	case FLOAT:
	    out.writeDouble (((Double) value).doubleValue());
	    break;
	case NODE:
	    out.writeObject (value);
	    break;
	case JAVAOBJECT:
	    if (value != null && !(value instanceof Serializable))
	        out.writeObject (null);
	    else
	        out.writeObject (value);
	    break;
	}
    }


    public Property (Node node) {
	this.node = node;
	dirty = true;
    }

    public Property (String propname, Node node) {
	this.propname = propname;
	this.node = node;
	dirty = true;
    }

    public Property (String propname, Node node, Node valueNode) {
	this (propname, node);
	type = NODE;
	value = valueNode == null ? null : valueNode.getHandle ();
	dirty = true;
    }

    public String getName () {
	return propname;
    }

    public Object getValue () {
	return value;
    }

    public int getType () {
	return type;
    }

    public void setStringValue (String str) {
	if (type == NODE)
	    unregisterNode ();
	type = STRING;
	value = str;
	dirty = true;
    }


    public void setIntegerValue (long l) {
	if (type == NODE)
	    unregisterNode ();
	type = INTEGER;
	value = new Long(l);
	dirty = true;
    }

    public void setFloatValue (double d) {
	if (type == NODE)
	    unregisterNode ();
	type = FLOAT;
	value = new Double(d);
	dirty = true;
    }

    public void setDateValue (Date date) {
	if (type == NODE)
	    unregisterNode ();
	type = DATE;
	value = date;
	dirty = true;
    }

    public void setBooleanValue (boolean bool) {
	if (type == NODE)
	    unregisterNode ();
	type = BOOLEAN;
	value = bool ? Boolean.TRUE : Boolean.FALSE;
	dirty = true;
    }

    public void setNodeValue (Node node) {
	// value.checkWriteLock ();
	if (type == NODE)
	    unregisterNode ();

	// registerNode (value);
	type = NODE;

	value = node == null ? null : node.getHandle ();
	dirty = true;
    }

    public void setNodeHandle (NodeHandle handle) {
	if (type == NODE)
	    unregisterNode ();
	// registerNode (value);
	type = NODE;
	value = handle;
	dirty = true;
    }

    public NodeHandle getNodeHandle () {
	if (type == NODE)
	    return (NodeHandle) value;
	return null;
    }

    public void convertToNodeReference (DbMapping dbm) {
	if (value != null && !(value instanceof NodeHandle))
	    value = new NodeHandle (new DbKey (dbm, value.toString ()));
	type = NODE;
    }

    public void setJavaObjectValue (Object obj) {
	if (type == NODE)
	    unregisterNode ();
	type = JAVAOBJECT;
	value = obj;
    }


    /**
     * tell a the value node that it is no longer used as a property.
     * If this was the "main" property for the node, also remove all other references.
     */
    protected void unregisterNode () {
	if (value == null || !(value instanceof NodeHandle))
	    return;
	NodeHandle nhandle = (NodeHandle) value;
	Node nvalue = nhandle.getNode (node.nmgr);

	DbMapping nvmap = null;
	Relation nvrel = null;
	if (node.dbmap != null) {
	    nvmap = node.dbmap.getPropertyMapping (propname);
	    nvrel = node.dbmap.getPropertyRelation (propname);
	}

	if (nvalue == null)
	    return;

	nvalue.checkWriteLock ();
	// check if the property node is also a subnode
	// BUG: this doesn't work because properties for subnode/properties are never stored and therefore
	// never reused.
	if (nvrel != null && nvrel.subnodesAreProperties) {
	    node.removeNode (nvalue);
	}
	// only need to call unregisterPropLink if the value node is not stored in a relational db
	// also, getParent is heuristical/implicit for relational nodes, so we don't do deepRemoveNode
	// based on that for relational nodes.
	if (nvmap == null || !nvmap.isRelational()) {
	    if (!nvalue.isAnonymous() && propname.equals (nvalue.getName()) && this.node == nvalue.getParent()) {
	        // this is the "main" property of a named node, so handle this as a cascading delete.
	        nvalue.deepRemoveNode ();
	    }
	}
    }


    public String getStringValue () {
	if (value == null)
	    return null;
	switch (type) {
	case STRING:
	case BOOLEAN:
	case INTEGER:
	case FLOAT:
	case JAVAOBJECT:
	    return value.toString ();
	case DATE:
	    SimpleDateFormat format = new SimpleDateFormat ("dd.MM.yy hh:mm:ss");
	    return format.format ((Date) value);
	case NODE:
	    return ((NodeHandle) value).getID ();
	}
	return "";
    }

    public String toString () {
	return getStringValue ();
    }

    public long getIntegerValue () {
	if (type == INTEGER)
	    return ((Long) value).longValue ();
	if (type == FLOAT)
	    return ((Double) value).longValue ();
	if (type == BOOLEAN)
	    return ((Boolean) value).booleanValue() ? 1 : 0;
	try {
	    return Long.parseLong (getStringValue());
	} catch (Exception x) {
	    return 0;
	}
    }

    public double getFloatValue () {
	if (type == FLOAT)
	    return ((Double) value).doubleValue();
	if (type == INTEGER)
	    return ((Long) value).doubleValue ();
	try {
	    return Double.parseDouble (getStringValue());
	} catch (Exception x) {
	    return 0.0;
	}
    }


    public Date getDateValue () {
	if (type == DATE)
	    return (Date) value;
	return null;
    }

    public Timestamp getTimestampValue () {
	if (type == DATE && value != null)
	    return new Timestamp (((Date) value).getTime());
	return null;
    }

    public boolean getBooleanValue () {
	if (type == BOOLEAN)
	    return ((Boolean) value).booleanValue();
	if (type == INTEGER)
	    return !(0 == getIntegerValue());
	return false;
    }

    public INode getNodeValue () {
	if (type == NODE && value != null) {
	    NodeHandle nhandle = (NodeHandle) value;
	    return nhandle.getNode (node.nmgr);
	}
	return null;
    }

    public Object getJavaObjectValue () {
	if (type == JAVAOBJECT)
	    return value;
	return null;
    }

}


