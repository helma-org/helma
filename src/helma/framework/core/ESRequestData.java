// ESRequestData.java
// Copyright (c) Hannes Wallnöfer 1998-2000


package helma.framework.core;

import FESI.Data.*;
import FESI.Exceptions.*;
import FESI.Interpreter.Evaluator;
import java.util.*;
import helma.objectmodel.INode;


/**
  * An EcmaScript object to access the form data sent with a HTTP request
  */

public class ESRequestData extends ESWrapper {

    private Hashtable data;
    private RequestEvaluator reval;

    public ESRequestData (ESObject prototype, Evaluator evaluator, RequestEvaluator reval) {
	super (prototype, evaluator);
	this.reval = reval;
    }
    
    public void setData (Hashtable data) {
	this.data = data;
    }

    /**
     * Overridden to make the object read-only
     */
    public void putProperty(String propertyName, ESValue propertyValue, int hash) throws EcmaScriptException {
	throw new EcmaScriptException ("Can't set property, req.data is read-only");
    }
    
    public boolean deleteProperty(String propertyName, int hash) throws EcmaScriptException {
	throw new EcmaScriptException ("Can't delete property, req.data is read-only");
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

	return ESLoader.normalizeValue(val, evaluator);
    }


    public Enumeration getAllProperties () {
	return getProperties ();
    }

    public Enumeration getProperties () {
	if (data == null)
	    return new Hashtable().keys();
	return data.keys();
    }



}





















































