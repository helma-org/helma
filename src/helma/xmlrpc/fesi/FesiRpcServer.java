/*
 * Copyright 1999 Hannes Wallnoefer
 */
 
package helma.xmlrpc.fesi;

import helma.xmlrpc.*;
import java.util.*;
import java.io.*;
import FESI.Data.*;
import FESI.Interpreter.*;
import FESI.Exceptions.*;

/**
 * An ESObject that makes its properties (sub-objects) callable via XML-RPC. 
 * For example, if Server is an instance of FesiRpcServer, the following would make the 
 * functions defined for someObject available to XML-RPC clients:
 * <pre>
 * Server.someObject = new SomeObject (); 
 * </pre>
 *
 */ 

public class FesiRpcServer extends ObjectPrototype {

    // This is public (for now) to be able to set access restrictions from the outside.
    public WebServer srv;
    Evaluator evaluator;

    /**
     * Create an XML-RPC server with an already existing WebServer.
     */
    public FesiRpcServer (WebServer srv, ESObject op, Evaluator eval) throws IOException, EcmaScriptException {
	super (op, eval);
	this.evaluator = eval;
	this.srv = srv;
    }

    /** 
     * Create an XML-RPC server listening on a specific port. 
     */
    public FesiRpcServer (int port, ESObject op, Evaluator eval) throws IOException, EcmaScriptException {
	super (op, eval);
	this.evaluator = eval;
	srv = new WebServer (port);
    }

    public void putProperty(String propertyName, ESValue propertyValue, int hash) throws EcmaScriptException {
	if (propertyValue instanceof ESObject) 
	    srv.addHandler (propertyName, new FesiInvoker ((ESObject) propertyValue));
	super.putProperty (propertyName, propertyValue, hash);
    }

    public boolean deleteProperty (String propertyName, int hash) throws EcmaScriptException {
	srv.removeHandler (propertyName);
	super.deleteProperty (propertyName, hash);
	return true; 
    }


    class FesiInvoker implements XmlRpcHandler {
    	
    	ESObject target;
    	
    	public FesiInvoker (ESObject target) {
    	    this.target = target;
    	}
    	
	public Object execute (String method, Vector argvec) throws Exception {
	    // convert arguments
	    int l = argvec.size ();
	    
	    ESObject callTarget = target;
	    if (method.indexOf (".") > -1) {
	        StringTokenizer st = new StringTokenizer (method, ".");
	        int cnt = st.countTokens ();
	        for (int i=1; i<cnt; i++) {
	            String next = st.nextToken ();
	            try {
	                callTarget = (ESObject) callTarget.getProperty (next, next.hashCode ());
	            } catch (Exception x) {
	                throw new EcmaScriptException ("The property \""+next+"\" is not defined in the remote object.");
	            }
	        }
	        method = st.nextToken ();
	    }
	    
	    ESValue args[] = new ESValue[l];
	    for (int i=0; i<l; i++) {
    	        args[i] = FesiRpcUtil.convertJ2E (argvec.elementAt (i), evaluator);
	    }
	    Object retval = FesiRpcUtil.convertE2J (callTarget.doIndirectCall (evaluator, callTarget, method, args));
	    return retval;
	}
    }

}
