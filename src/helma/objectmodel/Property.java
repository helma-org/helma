// Property.java
// Copyright (c) Hannes Wallnöfer 1997-2000

package helma.objectmodel;

import helma.util.*;
import java.util.Vector;
import java.util.Hashtable;
import java.util.Date;
import java.util.Enumeration;
import java.io.*;
import java.text.*;

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


    public Property (TransientNode node) {
	this.node = node;
    }

    public Property (String propname, TransientNode node) {
	this.propname = propname;
	this.node = node;
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
	    return nvalue;
	case JAVAOBJECT:
	    return jvalue;
	}
	return null;
    }

    public void setStringValue (String value) throws ParseException {
	if (type == NODE)
	    this.nvalue = null;
	if (type == JAVAOBJECT)
	    this.jvalue = null;
	type = STRING;
	this.svalue = value;
    }

    public void setIntegerValue (long value) {
	if (type == NODE)
	    this.nvalue = null;
	if (type == JAVAOBJECT)
	    this.jvalue = null;
	type = INTEGER;
	this.lvalue = value;
    }

    public void setFloatValue (double value) {
	if (type == NODE)
	    this.nvalue = null;
	if (type == JAVAOBJECT)
	    this.jvalue = null;
	type = FLOAT;
	this.dvalue = value;
    }

    public void setDateValue (Date value) {
	if (type == NODE)
	    this.nvalue = null;
	if (type == JAVAOBJECT)
	    this.jvalue = null;
	type = DATE;
	this.lvalue = value.getTime();
    }

    public void setBooleanValue (boolean value) {
	if (type == NODE)
	    this.nvalue = null;
	if (type == JAVAOBJECT)
	    this.jvalue = null;
	type = BOOLEAN;
	this.bvalue = value;
    }

    public void setNodeValue (INode value) {
	if (type == JAVAOBJECT)
	    this.jvalue = null;
	type = NODE;
	this.nvalue = value;
    }

    public void setJavaObjectValue (Object value) {
	if (type == NODE)
	    this.nvalue = null;
	type = JAVAOBJECT;
	this.jvalue = value;
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
	    return nvalue.getName ();
	case JAVAOBJECT:
	    return jvalue == null ? null :  jvalue.toString ();
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
	if (type == NODE) 
	    return nvalue;
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
