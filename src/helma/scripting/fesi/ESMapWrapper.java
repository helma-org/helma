// ESMapWrapper.java
// Copyright (c) Hannes Wallnöfer 1998-2000


package helma.scripting.fesi;

import helma.framework.core.*;
import helma.objectmodel.INode;
import FESI.Data.*;
import FESI.Exceptions.*;
import FESI.Interpreter.Evaluator;
import java.util.*;



/**
  * An EcmaScript object that makes stuff in a hashtable accessible as its properties
  */

public class ESMapWrapper extends ESWrapper {

    private Map data;
    private RequestEvaluator reval;

    public ESMapWrapper (RequestEvaluator reval) {
	super (new Object(), reval.evaluator);
	this.reval = reval;
    }

    public ESMapWrapper (RequestEvaluator reval, Map data) {
	super (new Object(), reval.evaluator);
	this.reval = reval;
	this.data = data;
    }

    public void setData (Map data) {
	this.data = data;
    }

    /**
     * Overridden to make the object read-only
     */
    public void putProperty(String propertyName, ESValue propertyValue, int hash) throws EcmaScriptException {
	if (data == null)
	    data = new HashMap ();
	data.put (propertyName, propertyValue);
    }
    
    public boolean deleteProperty(String propertyName, int hash) throws EcmaScriptException {
	data.remove (propertyName);
	return true;
    }
 
     public ESValue getProperty(String propertyName, int hash) throws EcmaScriptException {
	if (data == null)
	    return ESNull.theNull;

	Object val = data.get (propertyName);

	if (val == null)
	    return ESNull.theNull;

	if (val instanceof  String)
	    return new ESString ((String) val);
	else if (val instanceof INode)
	    return reval.getNodeWrapper ((INode) val);
	else if (val instanceof ESValue)
	    return (ESValue) val;
	return ESLoader.normalizeValue(val, evaluator);
    }


    public Enumeration getAllProperties () {
	return getProperties ();
    }

    public Enumeration getProperties () {
	Object[] keys = data == null ? null : data.keySet().toArray ();
	return new Enum (keys);
    }


    class Enum implements Enumeration {
	
	Object[] elements;
	int pos;
	
	Enum (Object[] elements) {
	    this.elements = elements;
	    pos = 0;
	}
	
	public boolean hasMoreElements () {
	    return elements != null && pos < elements.length;
	}
	
	public Object nextElement () {
	    if (elements == null || pos >= elements.length)
	        throw new NoSuchElementException ();
	    return elements[pos++];
	}
    }
}





















































