// RpcXtension.java
// Copyright (c) Hannes Wallnöfer, 1999 - All rights reserved

package helma.xmlrpc.fesi;


import helma.xmlrpc.*;
import helma.scripting.fesi.ESNode;
import helma.objectmodel.INode;

import FESI.Exceptions.*;
import FESI.Data.*;
import FESI.Interpreter.*;

import java.util.*;

public class FesiRpcUtil {

    /**
     * convert a generic Java object to a JavaScript Object.
     */
    public static ESValue convertJ2E (Object what, Evaluator evaluator) throws Exception {
	if (what == null)
	   return ESNull.theNull;
	if (what instanceof Vector) {
	    Vector v = (Vector) what;
	    ArrayPrototype retval = new ArrayPrototype (evaluator.getArrayPrototype (), evaluator);
	    int l = v.size ();
	    for (int i=0; i<l; i++)
	        retval.putProperty (i, convertJ2E (v.elementAt (i), evaluator));
	    return retval;
	}
	if (what instanceof Hashtable) {
	    Hashtable t = (Hashtable) what;
	    ESObject retval = new ObjectPrototype (evaluator.getObjectPrototype (), evaluator);
	    for (Enumeration e=t.keys(); e.hasMoreElements(); ) {
	        String next = (String) e.nextElement ();
	        retval.putProperty (next, convertJ2E (t.get (next), evaluator), next.hashCode ());
	    }
	    return retval;
	}
	if (what instanceof String)
	   return new ESString (what.toString ());
	if (what instanceof Number)
	   return new ESNumber (new Double (what.toString ()).doubleValue ());
	if (what instanceof Boolean)
	   return ESBoolean.makeBoolean (((Boolean) what).booleanValue ());
	if (what instanceof Date)
	   return new DatePrototype (evaluator, (Date) what);
	return ESLoader.normalizeValue (what, evaluator);
    }


    /**
     * convert a JavaScript Object object to a generic Java.
     */
    public static Object convertE2J (ESValue what) throws EcmaScriptException {
	if (XmlRpc.debug)
	    System.out.println ("converting e-2-j: "+what.getClass ());
	if (what instanceof ESNull)
	    return null;
	if (what instanceof ArrayPrototype) {
	    ArrayPrototype a = (ArrayPrototype) what;
	    int l = a.size ();
	    Vector v = new Vector ();
	    for (int i=0; i<l; i++) {
	        Object nj = convertE2J (a.getProperty (i));
	        v.addElement (nj);
	    }
	    return v;
	}
	if (what instanceof ObjectPrototype) {
	    ObjectPrototype o = (ObjectPrototype) what;
	    Hashtable t = new Hashtable ();
	    for (Enumeration e=o.getProperties (); e.hasMoreElements (); ) {
	        String next = (String) e.nextElement ();
	        if (XmlRpc.debug) System.out.println ("converting object member "+next);
	        // We don't do deep serialization of HopObjects to avoid
	        // that the whole web site structure is sucked out with one
	        // object. Instead we return some kind of "proxy" objects
	        // that only contain the prototype and id of the HopObject property.
	        Object nj = null;
	        ESValue esv = o.getProperty (next, next.hashCode ());
	        if (esv instanceof ESNode) {
	            INode node = ((ESNode) esv).getNode ();
	            if (node != null) {
	                Hashtable nt = new Hashtable ();
	                nt.put ("id", node.getID());
	                if (node.getPrototype() != null)
	                    nt.put ("prototype", node.getPrototype ());
	                nj = nt;
	            }
	        } else
	            nj = convertE2J (esv);
	        if (nj != null)  // can't put null as value in hashtable
	            t.put (next, nj);
	    }
	    return t;
	}
	if (what instanceof ESUndefined || what instanceof ESNull)
	    return null;
	Object jval = what.toJavaObject ();
	if (jval instanceof Byte || jval instanceof Short)
	    jval = new Integer (jval.toString ());
	return jval;
    }
}
