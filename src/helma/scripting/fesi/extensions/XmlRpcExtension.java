// XmlRpcExtension.java
// Copyright (c) Hannes Wallnöfer, 1999 - All rights reserved

package helma.scripting.fesi.extensions;

import org.apache.xmlrpc.*;

import helma.scripting.fesi.FesiEngine;

import FESI.Interpreter.*;
import FESI.Exceptions.*;
import FESI.Extensions.*;
import FESI.Data.*;

import java.io.*;
import java.util.*;
import java.net.*;


/** 
 * An extension to transparently call and serve XML-RPC from the 
 * <a href=http://home.worldcom.ch/jmlugrin/fesi/>FESI EcmaScript</a> interpreter. 
 * The extension adds constructors for XML-RPC clients and servers to the Global Object. 
 * For more information on how to use this please look at the files <tt>server.es</tt> and 
 * <tt>client.es</tt> in the src/fesi directory of the distribution.
 * 
 * All argument conversion is done automatically. Currently the following argument and return 
 * types are supported:
 * <ul>
 * <li> plain objects (with all properties returned by ESObject.getProperties ())
 * <li> arrays 
 * <li> strings
 * <li> date objects
 * <li> booleans
 * <li> integer and float numbers (long values are not supported!)
 * </ul>
 * 
 */
public class XmlRpcExtension extends Extension {

    Evaluator evaluator;
    ESObject op;

    public void initializeExtension (Evaluator evaluator) throws EcmaScriptException {
        // XmlRpc.setDebug (true);
        this.evaluator = evaluator;
        GlobalObject go = evaluator.getGlobalObject();
        FunctionPrototype fp = (FunctionPrototype) evaluator.getFunctionPrototype();

        op = evaluator.getObjectPrototype();

        go.putHiddenProperty ("Remote", new GlobalObjectRemote ("Remote", evaluator, fp)); // the Remote constructor
    }

    
    class GlobalObjectRemote extends BuiltinFunctionObject {

        GlobalObjectRemote (String name, Evaluator evaluator, FunctionPrototype fp) {
            super(fp, evaluator, name, 1);
        }
        
        public ESValue callFunction(ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
           return doConstruct(thisObject, arguments);
        }
        
        public ESObject doConstruct(ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
           ESObject remote = null;
           String url = null;
           String robj = null;
           if (arguments.length >= 1) 
               url = arguments[0].toString ();
           if (arguments.length >= 2)
               robj = arguments[1].toString ();
           try {
               remote = new ESRemote (op, this.evaluator, url, robj);
           } catch (MalformedURLException x) {
               throw new EcmaScriptException (x.toString ());
           }
           return remote;
        }
    }


    class ESRemote extends ObjectPrototype {

        URL url;
        String remoteObject;
        
        public ESRemote (ESObject prototype, Evaluator evaluator, String urlstring, String robj) throws MalformedURLException {
            super (prototype, evaluator);
            this.url = new URL (urlstring);
            remoteObject = robj;
        }

        public ESRemote (ESObject prototype, Evaluator evaluator, URL url, String robj) {
            super (prototype, evaluator);
            this.url = url;
            remoteObject = robj;
        }
 
        public ESValue doIndirectCall(Evaluator evaluator, ESObject target, String functionName, ESValue arguments[]) 
        throws EcmaScriptException, NoSuchMethodException {
            // System.out.println ("doIndirectCall called with "+remoteObject+"."+functionName);
            XmlRpcClient client = new XmlRpcClient (url);
            // long now = System.currentTimeMillis ();
            Object retval = null;
            int l = arguments.length;
            Vector v = new Vector ();
            for (int i=0; i<l; i++) {
                Object arg = FesiEngine.processXmlRpcResponse (arguments[i]);
                // System.out.println ("converted to J: "+arg.getClass ());
                v.addElement (arg);
            }
            // System.out.println ("spent "+(System.currentTimeMillis ()-now)+" millis in argument conversion");
            ESObject esretval = ObjectObject.createObject (evaluator);
            try {
                String method = remoteObject == null ? functionName : remoteObject+"."+functionName;
                retval = client.execute (method, v);
                esretval.putProperty ("error", ESNull.theNull, "error".hashCode());
                esretval.putProperty ("result", FesiEngine.processXmlRpcArgument (retval, evaluator), "result".hashCode());
            } catch (Exception x) {
                String msg = x.getMessage();
                if (msg == null || msg.length() == 0)
                    msg = x.toString ();
                esretval.putProperty ("error", new ESString(msg), "error".hashCode());
                esretval.putProperty ("result", ESNull.theNull, "result".hashCode());
            }
            return esretval;
        }
        
        public ESValue getProperty (String name, int hash) throws EcmaScriptException {
        	ESValue sprop = super.getProperty (name, hash);
        	if (sprop != ESUndefined.theUndefined && sprop != ESNull.theNull)
        	    return sprop;
        	String newRemoteObject = remoteObject == null ? name : remoteObject+"."+name;
        	return new ESRemote (op, this.evaluator, url, newRemoteObject);
        }
    }

}
