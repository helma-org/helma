// ESNode.java
// Copyright (c) Hannes Wallnöfer 1998-2000


package helma.framework.core;

import helma.objectmodel.*;
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

    // The ID of the wrapped Node. Makes ESNodes comparable without accessing the wrapped node.
    String nodeID;
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
        nodeID = node.getID ();
        dbmap = node.getDbMapping ();
    }
    
    public ESNode (ESObject prototype, Evaluator evaluator, Object obj, RequestEvaluator eval) {
        super (prototype, evaluator);
        // eval.app.logEvent ("in ESNode constructor: "+o.getClass ());
        this.eval = eval;
        if (obj == null)
            node = new Node ();
        else if (obj instanceof ESWrapper) 
            node = (INode) ((ESWrapper) obj).getJavaObject ();
        else if (obj instanceof INode)
            node = (INode) obj;
        else
            node = new Node (obj.toString ());
        // set nodeID to id of wrapped node
        nodeID = node.getID ();
        dbmap = node.getDbMapping ();

        // get transient cache Node
        cache = node.getCacheNode ();
        cacheWrapper = new ESNode (cache, eval);
    }

    /**
     * Check if the node has been invalidated. If so, it has to be re-fetched
     * from the db via the app's node manager.
     */
    private void checkNode () {
	if (node.getState () == INode.INVALID) try {
	    setNode (eval.app.nmgr.getNode (node.getID (), node.getDbMapping ()));
	} catch (Exception nx) {}
    }

    public INode getNode () {
        checkNode ();
        return node;
    }

    public void setNode (INode node) {
        if (node != null) {
            this.node = node;
            nodeID = node.getID ();
            dbmap = node.getDbMapping ();
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
    

    public boolean setContent (ESValue what[]) {
        checkNode ();
        if (what.length > 0) {
            if (what[0] instanceof ESString) {
                node.setContent (what[0].toString ());
                return true;
            }
            if (what[0] instanceof ESWrapper) {
                Object o =  ((ESWrapper) what[0]).toJavaObject ();
                if (o instanceof INode) {
                    try {
                        INode p = (INode) o;
                        node.setContent (p.getContent (), p.getContentType ());
                        return true;
                    } catch (Exception x) {
                        eval.app.logEvent ("error in ESNode.setContent: "+x);
                    }
                }
            }
            if (what[0] instanceof ESNode) {
                INode i = ((ESNode) what[0]).getNode ();
                try {
                    node.setContent (i.getContent (), i.getContentType ());
                    return true;
                } catch (Exception x) {
                    eval.app.logEvent ("error in ESNode.setContent: "+x);
                }
            }
        }
        return false;
    }

    public Object getContent () {
        checkNode ();
        if (node.getContentLength () == 0)
            return null;
        String contentType = node.getContentType ();
        if (contentType != null && contentType.startsWith ("text/")) {
            return node.getText ();
        } else {
            return node.getContent ();
        }
    }
    
    public boolean add (ESValue what[]) {
        checkNode ();
        for (int i=0; i<what.length; i++)
            if (what[i] instanceof ESNode) {
            	   ESNode esn = (ESNode) what[i];
                INode added = node.addNode (esn.getNode ());
                // only rewrap if a transient node was addet to a persistent one.
                if (esn.getNode () instanceof helma.objectmodel.Node && 
                            !(node instanceof helma.objectmodel.Node))
                    esn.rewrap (added);
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
        // only rewrap if a transient node was addet to a persistent one.
        if (esn.getNode () instanceof helma.objectmodel.Node && 
                            !(node instanceof helma.objectmodel.Node))
            esn.rewrap (added);
        return true;
    }
    
    /** 
     * This is necessary to remap ESNodes to their new peers
     *  when they go from transient to persistent state.
     */
    protected void rewrap (INode newnode) {
        // eval.app.logEvent ("rewrapping "+this+" from "+node+" to "+newnode);
        if (newnode == null)
            throw new RuntimeException ("Non-consistent check-in detected in rewrap ()");
        INode oldnode = node;
        if (oldnode == newnode) {
            // eval.app.logEvent ("loop detected or new peers unchanged in rewrap");
            return;
        }
        // set node and nodeID to new node
        node = newnode;
        nodeID = node.getID ();
        dbmap = node.getDbMapping ();

        int l = oldnode.numberOfNodes ();
        for (int i=0; i<l; i++) {
            INode next = oldnode.getSubnodeAt (i);
            ESNode esn = eval.getNodeWrapperFromCache (next);
            // eval.app.logEvent ("rewrapping node: "+next+" -> "+esn);
            if (esn != null) {
                esn.rewrap (newnode.getSubnodeAt (i));
            }
        }
        for (Enumeration e=oldnode.properties (); e.hasMoreElements (); ) {
            IProperty p = oldnode.get ((String) e.nextElement (), false);
            if (p != null && p.getType () == IProperty.NODE) {
            	   INode next = p.getNodeValue ();
                ESNode esn = eval.getNodeWrapperFromCache (next);
                if (esn != null) {
                    esn.rewrap (newnode.getNode (p.getName (), false));
                }
            }
        }
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
	        "contentlength".equalsIgnoreCase (propertyName) || "cache".equalsIgnoreCase (propertyName))
	    throw new EcmaScriptException ("Can't modify read-only property \""+propertyName+"\".");

	if ("subnodeRelation".equalsIgnoreCase (propertyName)) {
	    node.setSubnodeRelation (propertyValue instanceof ESNull ? null : propertyValue.toString ());
	    return;
	}

	if ("contenttype".equalsIgnoreCase (propertyName))
	    node.setContentType (propertyValue.toString ());
	else if (propertyValue instanceof ESNull)
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
	    if (esn.getNode () instanceof helma.objectmodel.Node &&
                            !(node instanceof helma.objectmodel.Node)) {
	        INode newnode = node.getNode (propertyName, false);
	        esn.rewrap (newnode);
	    }
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
	if ("contenttype".equalsIgnoreCase (propertyName))
	    return new ESString (node.getContentType ());
	if ("contentlength".equalsIgnoreCase (propertyName))
	    return new ESNumber (node.getContentLength ());

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
	if ("__id__".equalsIgnoreCase (propertyName))
	    return new ESString (node.getID ());
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
            return (other.nodeID.equals (nodeID) /* && other.dbmap == dbmap*/ );
        }
        return false;
    }	
  
} // class ESNode





















































