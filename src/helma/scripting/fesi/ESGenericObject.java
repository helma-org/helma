// ESGenericObject.java
// Copyright (c) Hannes Wallnöfer 1998-2000


package helma.scripting.fesi;

import helma.framework.core.*;
import helma.framework.IPathElement;
import FESI.Interpreter.*;
import FESI.Exceptions.*;
import FESI.Data.*;
import java.util.*;


/**
  *
  */

public class ESGenericObject extends ObjectPrototype {

    ESWrapper wrapper;
    IPathElement pathObj;

    public ESGenericObject (ESObject prototype, Evaluator evaluator, IPathElement obj) {
	super (prototype, evaluator);
	pathObj = obj;
	wrapper = new ESWrapper (obj, evaluator);
    }


    public String getESClassName () {
	return "GenericObject";
    }
    
    public String toString () {
	return pathObj.toString ();
    }
    
    public String toDetailString () {
	return wrapper.toDetailString ();
    }


    public void putProperty(String propertyName, ESValue propertyValue, int hash) throws EcmaScriptException {
	wrapper.putProperty (propertyName, propertyValue, hash);
    }

    public boolean hasProperty(String propertyName, int hash) throws EcmaScriptException {
	return super.hasProperty (propertyName, hash) || wrapper.hasProperty (propertyName, hash);
    }

    public boolean deleteProperty(String propertyName, int hash) throws EcmaScriptException {
	return wrapper.deleteProperty (propertyName, hash);
    }
 
     public ESValue getProperty (int i) throws EcmaScriptException {
 	return wrapper.getProperty (i);
     }

    public void putProperty(int index, ESValue propertyValue) throws EcmaScriptException {
	wrapper.putProperty (index, propertyValue);
    }


    public ESValue getProperty(String propertyName, int hash) throws EcmaScriptException {
	ESValue val = super.getProperty (propertyName, hash);
	if (val == null || val == ESUndefined.theUndefined)
	    val = wrapper.getProperty (propertyName, hash);
	return val;
    }

    public ESValue doIndirectCall(Evaluator evaluator, ESObject thisObject, String functionName, ESValue[] arguments)
                                        throws EcmaScriptException, NoSuchMethodException {
	if (super.hasProperty (functionName, functionName.hashCode()))
	    return super.doIndirectCall (evaluator, thisObject, functionName, arguments);
	return wrapper.doIndirectCall (evaluator, thisObject, functionName, arguments);
    }

    public Enumeration getAllProperties () {
	return wrapper.getProperties ();
    }

    public Enumeration getProperties () {
	return wrapper.getProperties ();
    }

    public Object toJavaObject () {
 	return pathObj;
    }

    /**
     * An ESNode equals another object if it is an ESNode that wraps the same INode 
     * or the wrapped INode itself. FIXME: doesen't check dbmapping/type!
     */
    public boolean equals (Object what) {
        if (what == null)
            return false;
        if (what == this)
            return true;
        if (what instanceof ESGenericObject) {
            ESGenericObject other = (ESGenericObject) what;
            return (pathObj.equals (other.pathObj));
        }
        return false;
    }	
  
}





















































