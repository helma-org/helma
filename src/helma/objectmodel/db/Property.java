// Property.java
// Copyright (c) Hannes Wallnöfer 1997-2000
 
package helma.objectmodel.db;

import helma.util.*;
import java.util.*;
import java.io.*;
import java.text.*;
import helma.objectmodel.*;

/**
 * A property implementation for Nodes stored inside a database. Basically 
 * the same as for transient nodes, with a few hooks added.
 */
public final class Property implements IProperty, Serializable, Cloneable {


    protected String propname;
    protected Node node;

    protected String svalue;
    protected boolean bvalue;
    protected long lvalue;
    protected double dvalue;
    // protected String nvalueID;
    protected NodeHandle nhandle;
    protected Object jvalue;

    protected int type;

    transient boolean dirty;

    static final long serialVersionUID = -1022221688349192379L;

    private void readObject (ObjectInputStream in) throws IOException {
	try {
	    propname = in.readUTF ();
	    node = (Node) in.readObject ();
	    type = in.readInt ();
	    switch (type) {
	    case STRING:
	        svalue = in.readUTF ();
	        break;
	    case BOOLEAN:
	        bvalue = in.readBoolean ();
	        break;
	    case INTEGER:
	    case DATE:
	        lvalue = in.readLong ();
	        break;
	    case FLOAT:
	        dvalue = in.readDouble ();
	        break;
	    case NODE:
	        // try to convert from old format
	        if (node.version > 4)
	            nhandle = (NodeHandle) in.readObject ();
	        else
	            nhandle = new NodeHandle (new DbKey (null, in.readUTF ()));
	        break;
	    case JAVAOBJECT:
	        jvalue = in.readObject ();
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
	    out.writeUTF (svalue);
	    break;
	case BOOLEAN:
	    out.writeBoolean (bvalue);
	    break;
	case INTEGER:
	case DATE:
	    out.writeLong (lvalue);
	    break;
	case FLOAT:
	    out.writeDouble (dvalue);
	    break;
	case NODE:
	    out.writeObject (nhandle);
	    break;
	case JAVAOBJECT:
	    if (jvalue != null && !(jvalue instanceof Serializable))
	        out.writeObject (null);
	    else
	        out.writeObject (jvalue);
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

    public Property (String propname, Node node, Node value) {
	this (propname, node);
	type = NODE;
	nhandle = value == null ? null : value.getHandle ();
	dirty = true;
    }

    public String getName () {
	return propname;
    }

    public Object getValue () {
	switch (type) {
	case STRING:
	    return svalue;
	case BOOLEAN:
	    return new Boolean (bvalue);
	case INTEGER:
	    return new Long (lvalue);
	case FLOAT:
	    return new Double (dvalue);
	case DATE:
	    return new Date (lvalue);
	case NODE:
	    return null;
	case JAVAOBJECT:
	    return jvalue;
	}
	return null;
    }

    public void setStringValue (String value) {
	if (type == NODE)
	    unregisterNode ();
	if (type == JAVAOBJECT)
	    this.jvalue = null;
	type = STRING;
	this.svalue = value;
	dirty = true;
    }


    public void setIntegerValue (long value) {
	if (type == NODE)
	    unregisterNode ();
	if (type == JAVAOBJECT)
	    this.jvalue = null;
	type = INTEGER;
	this.lvalue = value;
	dirty = true;
    }

    public void setFloatValue (double value) {
	if (type == NODE)
	    unregisterNode ();
	if (type == JAVAOBJECT)
	    this.jvalue = null;
	type = FLOAT;
	this.dvalue = value;
	dirty = true;
    }

    public void setDateValue (Date value) {
	if (type == NODE)
	    unregisterNode ();
	if (type == JAVAOBJECT)
	    this.jvalue = null;
	type = DATE;
	this.lvalue = value == null ? 0 : value.getTime();
	dirty = true;
    }

    public void setBooleanValue (boolean value) {
	if (type == NODE)
	    unregisterNode ();
	if (type == JAVAOBJECT)
	    this.jvalue = null;
	type = BOOLEAN;
	this.bvalue = value;
	dirty = true;
    }

    public void setNodeValue (Node value) {
	// value.checkWriteLock ();
	if (type == NODE)
	    unregisterNode ();
	if (type == JAVAOBJECT)
	    this.jvalue = null;
	
	// registerNode (value);	
	type = NODE;
	
	nhandle = value.getHandle ();
	dirty = true;
    }

    public void setJavaObjectValue (Object value) {
	if (type == NODE)
	    unregisterNode ();
	type = JAVAOBJECT;
	this.jvalue = value;
    }


    /**
     * tell a the value node that it is no longer used as a property. 
     * If this was the "main" property for the node, also remove all other references.
     */
    protected void unregisterNode () {
	Node nvalue = null;
	if (nhandle != null)
	    nvalue = nhandle.getNode (node.nmgr);
	
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
	switch (type) {
	case STRING:
	    return svalue;
	case BOOLEAN:
	    return "" + bvalue;
	case DATE:
	    SimpleDateFormat format = new SimpleDateFormat ("dd.MM.yy hh:mm");
	    return format.format (new Date (lvalue));
	case INTEGER:
	    return Long.toString (lvalue);
	case FLOAT:
	    return Double.toString (dvalue);
	case NODE:
	    return nhandle.getID ();
	case JAVAOBJECT:
	    return jvalue == null ? null : jvalue.toString ();
	}
	return "";
    }

    public String toString () {
	return getStringValue ();
    }

    public long getIntegerValue () {
	if (type == INTEGER) 	
	    return lvalue;
	return 0;
    }

    public double getFloatValue () {
	if (type == FLOAT) 	
	    return dvalue;
	return 0.0;
    }


    public Date getDateValue () {
	if (type == DATE) 	
	    return new Date (lvalue);
	return null;
    }

    public boolean getBooleanValue () {
	if (type == BOOLEAN) 
	    return bvalue;
	return false;
    }

    public INode getNodeValue () {
	
	if (nhandle != null) {
	    Node n = nhandle.getNode (node.nmgr);
	    if (n != null) return n;
	}
	return null;
    }

    public Object getJavaObjectValue () {
	if (type == JAVAOBJECT)
	    return jvalue;
	return null;
    }


    public int getType () {
	return type;
    }

}




































































