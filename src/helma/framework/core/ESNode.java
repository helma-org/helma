// ESNode.java
// Copyright (c) Hannes Wallnöfer 1998-2000


package helma.framework.core;

import helma.objectmodel.*;
import helma.objectmodel.db.NodeHandle;
import helma.util.*;
import FESI.Interpreter.*;
import FESI.Exceptions.*;
import FESI.Data.*;
import java.io.*;
import java.util.*;


/**
  * An EcmaScript wrapper around a 'Node' object. This is the basic 
  * HOP object type that can be stored in the internal or external databases. 
  * All HOP types inherit from the Node object.
  */

public class ESNode extends ObjectPrototype {

    INode node;
    INode cache;

    // The handle of the wrapped Node. Makes ESNodes comparable without accessing the wrapped node.
    NodeHandle handle;
    DbMapping dbmap;
    ESObject cacheWrapper;
    Throwable lastError = null;
    RequestEvaluator eval;
    
    // used to create cache nodes
    protected ESNode (INode node, RequestEvaluator eval) {
	super (eval.esNodePrototype, eval.evaluator);
	this.eval = eval;
	this.node = node;
	cache = null;
	cacheWrapper = null;
	
	// set node handle to wrapped node
	if (node instanceof helma.objectmodel.db.Node)
	    handle = ((helma.objectmodel.db.Node) node).getHandle ();
	else
	    handle = null;
    }
    
    public ESNode (ESObject prototype, Evaluator evaluator, Object obj, RequestEvaluator eval) {
	super (prototype, evaluator);
	// eval.app.logEvent ("in ESNode constructor: "+o.getClass ());
	this.eval = eval;
	if (obj == null)
	    node = new Node (null);
	else if (obj instanceof ESWrapper)
	    node = (INode) ((ESWrapper) obj).getJavaObject ();
	else if (obj instanceof INode)
	    node = (INode) obj;
	else
	    node = new Node (obj.toString ());
	// set node handle to wrapped node
	if (node instanceof helma.objectmodel.db.Node)
	    handle = ((helma.objectmodel.db.Node) node).getHandle ();
	else
	    handle = null;

	// get transient cache Node
	cache = node.getCacheNode ();
	cacheWrapper = new ESNode (cache, eval);
    }

    /**
     * Check if the node has been invalidated. If so, it has to be re-fetched
     * from the db via the app's node manager.
     */
    protected void checkNode () {
	if (node.getState () == INode.INVALID) try {
	    node = handle.getNode (eval.app.nmgr.safe);
	} catch (Exception nx) {}
    }

   public INode getNode () {
	checkNode ();
	return node;
    }

    public void setNode (INode node) {
	if (node != null) {
	    this.node = node;
	    // set node handle to wrapped node
	    if (node instanceof helma.objectmodel.db.Node)
	        handle = ((helma.objectmodel.db.Node) node).getHandle ();
	    else
	        handle = null;
	    eval.objectcache.put (node, this);
	    // get transient cache Node
	    cache = node.getCacheNode ();
	    cacheWrapper = new ESNode (cache, eval);
	}
    }

    public void setPrototype (String protoName) {
	checkNode ();
	node.setPrototype (protoName);
    }

    public String getPrototypeName () {
	return node.getPrototype ();
    }

    public String getESClassName () {
	return "HopObject";
    }
    
    public String toString () {
	if (node == null)
	    return "<null>";
	return node.toString ();
    }
    
    public String toDetailString () {
	return "ES:[Object: builtin " + this.getClass().getName() + ":" +
			((node == null) ? "null" : node.toString()) + "]";
    }

    protected void setError (Throwable e) {
	lastError = e;
    }
    

    public boolean add (ESValue what[]) {
        checkNode ();
        for (int i=0; i<what.length; i++)
            if (what[i] instanceof ESNode) {
            	   ESNode esn = (ESNode) what[i];
                INode added = node.addNode (esn.getNode ());
            }
        return true;
    }

    public ESValue list () {
           checkNode ();
           int l = node.numberOfNodes ();
           Enumeration e = node.getSubnodes ();
           ESObject ap = evaluator.getArrayPrototype();
           ArrayPrototype theArray = new ArrayPrototype(ap, evaluator);
           if (e != null) {
               theArray.setSize(l);
               for (int i = 0; i<l; i++) {
                   theArray.setElementAt (eval.getNodeWrapper ((INode) e.nextElement ()), i);
               }
           } else {
               theArray.setSize (0);
           }
           return theArray;
    }

    public boolean addAt (ESValue what[]) throws EcmaScriptException {
        checkNode ();
        if (what.length < 2)
            throw new EcmaScriptException ("Wrong number of arguments");
        if (! (what[1] instanceof ESNode))
            throw new EcmaScriptException ("Can ony add Node objects as subnodes");
        ESNode esn = (ESNode) what[1];
        INode added = node.addNode (esn.getNode (), (int) what[0].toInt32 ());
        return true;
    }
    

   /**
    *  Remove one or more subnodes.
    */
    public boolean remove (ESValue args[]) {
        checkNode ();
        for (int i=0; i<args.length; i++) {
            if (args[i] instanceof ESNode) {
                ESNode esn = (ESNode) args[i];
                node.removeNode (esn.getNode ());
            }
        }
        return true;
    }

   /**
    *  Check if node is contained in subnodes
    */
    public int contains (ESValue args[]) {
        checkNode ();
        if (args.length == 1 && args[0] instanceof ESNode) {
            ESNode esn = (ESNode) args[0];
            return node.contains (esn.getNode ());
        }
        return -1;
    }

   /**
    *  This used to be different from add(), it isn't anymore. It's left here for
    *  compatibility.
    */
   public boolean link (ESValue args[]) {
        checkNode ();
        for (int i=0; i<args.length; i++) {
            if (args[i] instanceof ESNode) {
                ESNode esn = (ESNode) args[i];
                node.addNode (esn.getNode ());
            }
        }
        return true;
    }


    public boolean setParent (ESValue[] pval) {
        // do a couple of checks: both nodes need to be persistent in order for  setParent to make sense.
        if (!(node instanceof helma.objectmodel.db.Node))
            return false;
        if (pval == null || pval.length < 1 || pval.length > 2)
            return false;
        if (!(pval[0] instanceof ESNode))
            return false;
        ESNode esn = (ESNode) pval[0];
        INode pn = esn.getNode ();
        if (!(pn instanceof helma.objectmodel.db.Node))
            return false;
        // check if there is an additional string element - if so, it's the property name by which the node is
        // accessed, otherwise it will be accessed as anonymous subnode via its id
        String propname = null;
        if (pval.length == 2 && pval[1] != null && !(pval[1] instanceof ESNull))
            propname = pval[1].toString ();
        helma.objectmodel.db.Node n = (helma.objectmodel.db.Node) node;
        n.setParent ((helma.objectmodel.db.Node) pn, propname);
        return true;
    }


    public void putProperty(String propertyName, ESValue propertyValue, int hash) throws EcmaScriptException {
             checkNode ();
	// eval.app.logEvent ("put property called: "+propertyName+", "+propertyValue.getClass());
	if ("lastmodified".equalsIgnoreCase (propertyName) || "created".equalsIgnoreCase (propertyName) ||
	        "cache".equalsIgnoreCase (propertyName))
	    throw new EcmaScriptException ("Can't modify read-only property \""+propertyName+"\".");

	if ("subnodeRelation".equalsIgnoreCase (propertyName)) {
	    node.setSubnodeRelation (propertyValue instanceof ESNull ? null : propertyValue.toString ());
	    return;
	}

	if (propertyValue instanceof ESNull)
	    node.unset (propertyName);
	else if (propertyValue instanceof ESString)
	    node.setString (propertyName, propertyValue.toString ());
	else if (propertyValue instanceof ESBoolean)
	    node.setBoolean (propertyName, propertyValue.booleanValue ());
	else if (propertyValue instanceof ESNumber)
	    node.setFloat (propertyName, propertyValue.doubleValue ());
	else if (propertyValue instanceof DatePrototype)
	    node.setDate (propertyName, (Date) propertyValue.toJavaObject ());
	else if (propertyValue instanceof ESNode) {
	    // long now = System.currentTimeMillis ();
	    ESNode esn = (ESNode) propertyValue;
	    node.setNode (propertyName, esn.getNode ());
	    // eval.app.logEvent ("*** spent "+(System.currentTimeMillis () - now)+" ms to set property "+propertyName);
	} else {
	    // eval.app.logEvent ("got "+propertyValue.getClass ());
	    // A persistent node can't store anything other than the types above, so throw an exception
                 // throw new EcmaScriptException ("Can't set a JavaScript Object or Array as property of "+node);
	    node.setJavaObject (propertyName, propertyValue.toJavaObject ());
	}
    }
    
    public boolean deleteProperty(String propertyName, int hash) throws EcmaScriptException {
             checkNode ();
    	// eval.app.logEvent ("delete property called: "+propertyName);
    	if (node.get (propertyName, false) != null) {
    	    node.unset (propertyName);
    	    return true;
    	}
	return super.deleteProperty (propertyName, hash);
    }
 
     public ESValue getProperty (int i) throws EcmaScriptException {
             checkNode ();
 	INode n = node.getSubnodeAt (i);
 	if (n == null)
	    return ESNull.theNull;
 	return eval.getNodeWrapper (n);
     }

    public void putProperty(int index, ESValue propertyValue) throws EcmaScriptException {
             checkNode ();
	if (propertyValue instanceof ESNode) {
	    ESNode n = (ESNode) propertyValue;
	    node.addNode (n.getNode (), index);
	} else
	    throw new EcmaScriptException ("Can only add Nodes to Node arrays");
    }


    public ESValue getProperty(String propertyName, int hash) throws EcmaScriptException {
             checkNode ();
	// eval.app.logEvent ("get property called: "+propertyName);
	ESValue retval = super.getProperty (propertyName, hash);
	if (! (retval instanceof ESUndefined))
	    return retval;

             if ("cache".equalsIgnoreCase (propertyName) && cache != null)
	    return cacheWrapper;
	if ("created".equalsIgnoreCase (propertyName))
	    return new DatePrototype (evaluator, node.created ());	
	if ("lastmodified".equalsIgnoreCase (propertyName))
	    return new DatePrototype (evaluator, node.lastModified ());

	if ("subnodeRelation".equalsIgnoreCase (propertyName)) {
	    String rel = node.getSubnodeRelation ();
	    return rel == null ?  (ESValue) ESNull.theNull :  new ESString (rel);
	}

	// this is not very nice, but as a hack we return the id of a node as node.__id__
	if (propertyName.startsWith ("__") && propertyName.endsWith ("__"))
	    return getInternalProperty (propertyName);

             // this _may_ do a relational query if properties are mapped to a relational type.
	IProperty p = node.get (propertyName, false);
	if (p != null) {
	    if (p.getType () == IProperty.STRING) {
	        String str = p.getStringValue ();
	        if (str == null)
	            return ESNull.theNull;
	        else
	            return new ESString (str);
	    }
	    if (p.getType () == IProperty.BOOLEAN)
	        return ESBoolean.makeBoolean (p.getBooleanValue ());
	    if (p.getType () == IProperty.DATE) 
	        return new DatePrototype (evaluator, p.getDateValue ());
	    if (p.getType () == IProperty.INTEGER)
	        return new ESNumber ((double) p.getIntegerValue ());
	    if (p.getType () == IProperty.FLOAT)
	        return new ESNumber (p.getFloatValue ());
	    if (p.getType () == IProperty.NODE) {
	        INode nd = p.getNodeValue ();
	        if (nd == null)
	            return ESNull.theNull;
	        else
	            return eval.getNodeWrapper (nd);
	    }
	    if (p.getType () == IProperty.JAVAOBJECT)
	        return ESLoader.normalizeObject (p.getJavaObjectValue (), evaluator);
	}

	// as last resort, try to get property as anonymous subnode
	INode anon = node.getSubnode (propertyName);
	if (anon != null)
	    return eval.getNodeWrapper (anon);

	return ESNull.theNull;
    }

    private ESValue getInternalProperty (String propertyName) throws EcmaScriptException {
	if ("__id__".equalsIgnoreCase (propertyName)) try {
	    return new ESString (node.getID ());
	} catch (Exception noid) {
	    return new ESString ("transient");
	}
	if ("__prototype__".equalsIgnoreCase (propertyName)) {
	    String p = node.getPrototype ();
	    if (p == null)
	        return ESNull.theNull;
	    else
	        return new ESString (node.getPrototype ());
	}
	// some more internal properties
	if ("__parent__".equals (propertyName)) {
                 INode n = node.getParent ();
	    if (n == null)
	        return ESNull.theNull;
	    else
	        return eval.getNodeWrapper (n);
             }
	if ("__name__".equals (propertyName))
	    return new ESString (node.getName ());
	if ("__fullname__".equals (propertyName))
	    return new ESString (node.getFullName ());
	if ("__hash__".equals (propertyName))
	    return new ESString (""+node.hashCode ());
	if ("__node__".equals (propertyName))
	    return ESLoader.normalizeObject (node, evaluator);
	return ESNull.theNull;
    }

    public Enumeration getAllProperties () {
	return getProperties ();
    }

    public Enumeration getProperties () {
             checkNode ();
	return node.properties ();
    }


    public String error() {
      if (lastError == null) {
          return "";
      } else {
          String exceptionName = lastError.getClass().getName();
          int l = exceptionName.lastIndexOf(".");
          if (l>0) exceptionName = exceptionName.substring(l+1);
          return exceptionName +": " + lastError.getMessage();
      }
    }
   
    public void clearError() {
        lastError = null;
    }

    public Object toJavaObject () {
        return getNode ();
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
        if (what instanceof ESNode) {
            ESNode other = (ESNode) what;
            if (handle != null)
                return handle.equals (other.handle);
            else
                return (node == other.node);
        }
        return false;
    }	
  
} // class ESNode





















































