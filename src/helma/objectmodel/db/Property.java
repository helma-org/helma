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
    protected String nvalueID;
    private transient DbMapping dbm;
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
	        nvalueID = in.readUTF ();
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
	// don't even start if this is a non-serializable Java object
	if (type == JAVAOBJECT && jvalue != null && !(jvalue instanceof Serializable))
	    return;
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
	    out.writeUTF (nvalueID);
	    break;
	case JAVAOBJECT:
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
	nvalueID = value == null ? null : value.getID ();
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
	// IServer.getLogger().log ("setting string value of property "+propname + " to "+value);
	// mark property as dirty
	dirty = true;
	// if this is not a string property, try to parse a value out of it
	if (type == DATE) {
	    try {
	        SimpleDateFormat dateformat = new SimpleDateFormat ();
	        dateformat.setLenient (true);
	        Date date = dateformat.parse (value);
	        this.lvalue =  date.getTime ();
	        return;
	    } catch (ParseException nodate) {
	        // store as plain string
	    }
	}
	if (type == BOOLEAN) {
	    if ("true".equalsIgnoreCase (value))
	        this.bvalue = true;
	    else if ("false".equalsIgnoreCase (value))
	        this.bvalue = false;
	    return;
	}
	if (type == INTEGER) {
	    this.lvalue = Long.parseLong (value);
	    return;
	}
	if (type == FLOAT) {
	    this.dvalue = new Double (value).doubleValue ();
	    return;
	}
	if (type == JAVAOBJECT)
	    this.jvalue = null;
	this.svalue = value;
	type = STRING;
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
	value.checkWriteLock ();
	if (type == NODE)
	    unregisterNode ();
	if (type == JAVAOBJECT)
	    this.jvalue = null;
	registerNode (value);	
	type = NODE;
	if (node.dbmap != null) {
	    Relation rel = node.dbmap.getPropertyRelation (propname);
	    if (rel != null && rel.other != null) {
	        DbMapping vmap = value.getDbMapping ();
	        // check if actual type matches expected type
	        if (rel.other != vmap && (!rel.virtual || rel.prototype != null)) {
	            throw new RuntimeException ("Can't assign property: expected prototype "+rel.other+", got "+vmap);
	        }
	        // check if this is a forward relation, i.e. if we point to a field in the value object
	        // if so, we may use something else than the object's id to refer to it.
	        if (!rel.virtual && rel.direction == Relation.FORWARD) {
	            if (rel.usesPrimaryKey ()) {
	                this.nvalueID = value.getID ();
	            } else try {
	                this.nvalueID = value.getString (vmap.columnNameToProperty (rel.getRemoteField()).propname, false);
	            } catch (Exception x) {
	                throw new RuntimeException ("Can't set "+propname+" to "+value+": error retrieving target property");
	            }
	            this.dbm = null;
	            dirty = true;
	            return;
	        }
	    }
	}
	this.nvalueID = value == null ? null : value.getID ();
	this.dbm = value == null ? null : value.getDbMapping ();
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
	if (nvalueID != null) {
	    DbMapping nvmap = null;
	    Relation nvrel = null;
	    if (node.dbmap != null) {
	        nvmap = node.dbmap.getPropertyMapping (propname);
	        nvrel = node.dbmap.getPropertyRelation (propname);
	    }
	    Node nvalue = node.nmgr.getNode (nvalueID, nvmap);
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
	        } else {
	            nvalue.unregisterPropLink (this.node);
	        }
	    }
	}
    }


    /**
     *  Tell the value node that it is being used as a property value.
     */
    protected void registerNode (Node n) {
	// only need to call registerPropLink if the value node is not stored in a relational db
	if (n != null && (n.dbmap == null || !n.dbmap.isRelational())) {
	    n.registerPropLink (this.node);
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
	    return nvalueID;
	case JAVAOBJECT:
	    return jvalue.toString ();
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

	if (type == NODE && nvalueID != null) {
	    Relation rel = null;
	    if (dbm == null && node.dbmap != null) {
	        // try to get DbMap for property, if it isn't known yet
	        rel = node.dbmap.getPropertyRelation (propname);
	        // figure out db mapping from relation
	        if (rel != null) {
	            // is the property a virtual node containing objects from relational db?
	            if (rel.virtual && rel.other.isRelational ())
	                return node.nmgr.getNode (node, propname, rel);
	            else if (!rel.virtual && rel.direction == Relation.FORWARD)
	                return node.nmgr.getNode (node, nvalueID, rel);
	            // avoid setting dbm for virtual and groupby relations, except for
	            // [mountpoint] kind of prototyped virtual nodes
	            else if ((!rel.virtual || rel.prototype != null) && rel.groupby == null)
	                dbm = rel.other;
	        }
	    }

	    // we have what we need, now get the node from the node manager
	    Node retval = node.nmgr.getNode (nvalueID, dbm);
	    if (retval != null && retval.parentID == null && !"root".equalsIgnoreCase (retval.getPrototype ())) {
	        retval.setParent (node);
	        retval.setName (propname);
	        retval.anonymous = false;
	    }

	    if (retval != null && retval.getDbMapping () == null && rel != null && rel.virtual && rel.prototype == null) {
	        // a virtual node whose child nodes are not relational -
	        // set up dbmapping that describes subnodes and properties
	        DbMapping _dbm = new DbMapping ();
	        _dbm.setSubnodeMapping (rel.other);
	        _dbm.setPropertyMapping (rel.other);
	        _dbm.setSubnodeRelation (rel.getVirtualSubnodeRelation());
	        _dbm.setPropertyRelation (rel.getVirtualPropertyRelation());
	        retval.setDbMapping (_dbm);
	    }

	    return retval;
	}
	return null;
    }

    public Object getJavaObjectValue () {
	if (type == JAVAOBJECT)
	    return jvalue;
	return null;
    }

    public String getEditor () {
	switch (type) {
	case STRING:
	    return "password".equalsIgnoreCase (propname) ? 
	        "<input type=password name=\""+propname+"\" value='"+ svalue.replace ('\'', '"') +"'>" : 
	        "<input type=text name=\""+propname+"\" value='"+ svalue.replace ('\'', '"') +"'>" ;
	case BOOLEAN:
	    return "<select name=\""+propname+"\"><option selected value="+bvalue+">"+bvalue+"</option><option value="+!bvalue+">"+!bvalue+"</option></select>";
	case INTEGER:
	    return "<input type=text name=\""+propname+"\" value=\""+lvalue+"\">" ;
	case FLOAT:
	    return "<input type=text name=\""+propname+"\" value=\""+dvalue+"\">" ;
	case DATE:
	    SimpleDateFormat format = new SimpleDateFormat ("dd.MM.yy hh:mm");
	    String date =  format.format (new Date (lvalue));
	    return "<input type=text name=\""+propname+"\" value=\""+date+"\">";
	case NODE:
	    DbMapping nvmap = null;
	    if (node.dbmap != null)
	        nvmap = node.dbmap.getPropertyMapping (propname);
	    return "<input type=text size=25 name="+propname+" value='"+ node.nmgr.getNode (nvalueID, nvmap).getName () +"'>";
	}
	return "";
    }

    private String escape (String s) {
	char c[] = new char[s.length()];
	s.getChars (0, c.length, c, 0);
	StringBuffer b = new StringBuffer ();
	int copyfrom = 0;
	for (int i = 0; i < c.length; i++) {
	    switch (c[i]) {
	        case '\\': 
	        case '"':
	            if (i-copyfrom > 0)
	                b.append (c, copyfrom, i-copyfrom);
	            b.append ('\\');
	            b.append (c[i]);
	            copyfrom = i+1;
	    }   
	}
	if (c.length-copyfrom > 0)
	    b.append (c, copyfrom, c.length-copyfrom);
	return b.toString ();
    }

    public int getType () {
	return type;

    }

    public String getTypeString () {
	switch (type) {
	case STRING:
	    return "string";
	case BOOLEAN:
	    return "boolean";
	case DATE:
	    return "date";
	case INTEGER:
	    return "integer";
	case FLOAT:
	    return "float";
	case NODE:
	    return "node";
	}
	return "";
    }


    public Object clone () {
	try {
	    Property c = (Property) super.clone();
	    c.propname = this.propname;
	    c.svalue = this.svalue;
	    c.bvalue = this.bvalue;
	    c.lvalue = this.lvalue;
	    c.dvalue = this.dvalue;
	    c.nvalueID = this.nvalueID;
	    c.type = this.type;
	    return c;
	} catch (CloneNotSupportedException e) { 
	    // this shouldn't happen, since we are Cloneable
	    throw new InternalError ();
	}
    }

}




































































