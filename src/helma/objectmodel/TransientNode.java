// TransientNode.java
// Copyright (c) Hannes Wallnöfer 1997-2000
 
package helma.objectmodel;


import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Date;
import java.util.StringTokenizer;
import java.io.*;
import helma.util.*;
import helma.framework.IPathElement;
import helma.objectmodel.db.DbMapping;
import helma.objectmodel.db.Relation;

/**
 * A transient implementation of INode. An instance of this class can't be
 * made persistent by reachability from a persistent node. To make a persistent-capable
 * object, class helma.objectmodel.db.Node has to be used.
 */
 
public class TransientNode implements INode, Serializable {


    protected Hashtable propMap, nodeMap;
    protected Vector nodes;
    protected TransientNode parent;
    protected Vector links;   // links to this node
    protected Vector proplinks;  // nodes using this node as property

    transient String prototype;

    protected long created;
    protected long lastmodified;

    protected String id, name;
    // is the main identity a named property or an anonymous node in a collection?
    protected boolean anonymous = false;

    transient DbMapping dbmap;

    private static long idgen = 0;

    public static String generateID () {
	// make transient ids differ from persistent ones
	// and are unique within on runtime session
	return "t"+idgen++;
    }

    public TransientNode () {
	id = generateID ();
	name = id;
	created = lastmodified = System.currentTimeMillis ();
    }

    /**
     *  Make a new TransientNode object with a given name
     */
    public TransientNode (String n) {
	id = generateID ();
	name = n == null || "".equals (n) ? id : n;
	created = lastmodified = System.currentTimeMillis ();
    }


    public void setDbMapping (DbMapping dbmap) {
	this.dbmap = dbmap;
    }

    public DbMapping getDbMapping () {
	return dbmap;
    }


    /** 
     *  navigation-related
     */

    public String getID () {
	return id;
    }
    
    public boolean isAnonymous () {
 	return anonymous;
    }
    

    public String getName () { 
	return name; 
    }
    
    public String getElementName () {
    	return anonymous ? id : name;
    }

    public int getState () {
	return TRANSIENT;
    }

    public void setState (int s) {
	// state always is TRANSIENT on this kind of node
    }

    public String getFullName () {
	return getFullName (null);
    }

    public String getFullName (INode root) {
	String fullname = "";
	String divider = null;
	StringBuffer b = new StringBuffer ();
	TransientNode p = this;
	while  (p != null && p.parent != null && p != root) {
	    if (divider != null)
	        b.insert (0, divider);
	    else
	        divider = "/";
	    b.insert (0, p.getElementName ());
	    p = p.parent;
	}
	return b.toString ();
    }


    public void setName (String name) { 
	// if (name.indexOf('/') > -1)
	//     throw new RuntimeException ("The name of the node must not contain \"/\".");
	if (name == null || name.trim().length() == 0)
	    this.name = id;
	else
	    this.name = name;
    }

    public String getPrototype () {
	// if prototype is null, it's a vanilla HopObject.
	if (prototype == null)
	    return "hopobject";
	return prototype;
    }

    public void setPrototype (String proto) {
	this.prototype = proto;
    }


    public INode getParent () {
	return parent; 
    }


    /**
     *  INode-related
     */

    public void setSubnodeRelation (String rel) {
	throw new RuntimeException ("Can't set subnode relation for non-persistent Node.");
    }

    public String getSubnodeRelation () {
	return null;
    }

    public int numberOfNodes () {
	return nodes == null ? 0 : nodes.size ();
    }

    public INode addNode (INode elem) {
	return addNode (elem, numberOfNodes ());
    }

    public INode addNode (INode elem, int where) {

	if (where < 0 || where > numberOfNodes ()) 
	    where = numberOfNodes ();

	String n = elem.getName();
	if (n.indexOf('/') > -1)
	    throw new RuntimeException ("The name of a node must not contain \"/\" (slash).");
	
	// IServer.getLogger().log ("adding: "+node+" -- "+node.getContentLength ());
	if (nodeMap != null && nodeMap.get (elem.getID ()) != null) {
	    nodes.removeElement (elem);
	    where = Math.min (where, numberOfNodes ());
	    nodes.insertElementAt (elem, where);
	    return elem;
	}

	if (nodeMap == null) nodeMap = new Hashtable ();
	if (nodes == null) nodes = new Vector ();

	nodeMap.put (elem.getID (), elem);
	nodes.insertElementAt (elem, where);

	if (elem instanceof TransientNode) {
	    TransientNode node = (TransientNode) elem;
	    if (node.parent == null) {
	        node.parent = this;
	        node.anonymous = true;
	    }
	}
	
	lastmodified = System.currentTimeMillis ();
	// Server.throwNodeEvent (new NodeEvent (this, NodeEvent.SUBNODE_ADDED, node));
	return elem;
    }

    public INode createNode () {
	return createNode (null, 0); // where is ignored since this is an anonymous node
    }

    public INode createNode (int where) {
	return createNode (null, where);
    }

    public INode createNode (String nm) {
	return createNode (nm, numberOfNodes ()); // where is usually ignored (if nm != null)
    }

    public INode createNode (String nm, int where) {
    	boolean anon = false;
	if (nm == null || "".equals (nm.trim ())) 
	    anon = true;
	INode n = new TransientNode (nm);
	if (anon)
	    addNode (n, where);
	else 
	    setNode (nm, n);
	return n;
    }


    /**
     * register a node that links to this node.
     */
    /* protected void registerLink (TransientNode from) {
	if (links == null) 
	    links = new Vector ();
	if (!links.contains (from)) 
	    links.addElement (from);
    } */

    public IPathElement getParentElement () {
	return getParent ();
    }

    public IPathElement getChildElement (String name) {
	return getNode (name);
    }

    public INode getSubnode (String name) {
	StringTokenizer st = new StringTokenizer (name, "/");
	TransientNode retval = this, runner;
	while (st.hasMoreTokens () && retval != null) {
	    runner = retval;
	    String next = st.nextToken().trim().toLowerCase ();
	    if ("".equals (next))
	        retval = this;
	    else 
	        retval = runner.nodeMap == null ? null : (TransientNode) runner.nodeMap.get (next);
	    if (retval == null)
	        retval = (TransientNode) runner.getNode (next);
	}
	return retval;
    }


    public INode getSubnodeAt (int index) {
	return nodes == null ? null : (INode) nodes.elementAt (index);
    }

    public int contains (INode n) {
	if (n == null || nodes == null)
	    return -1;
	return nodes.indexOf (n);
    }

    public boolean remove () {
    	if (anonymous)
    	    parent.unset (name);
    	else 
	    parent.removeNode (this);
	return true;
    }


    public void removeNode (INode node) {
	// IServer.getLogger().log ("removing: "+ node);
	releaseNode (node);
	TransientNode n = (TransientNode) node;
	if (n.getParent () == this && n.anonymous) {
	    int l = n.links == null ? 0 : n.links.size ();   // notify nodes that link to n that n is going down.
	    for (int i = 0; i < l; i++) {
	        TransientNode link = (TransientNode) n.links.elementAt (i);
	        link.releaseNode (n);
	    }
	    if (n.proplinks != null) {
	        // clean up all nodes that use n as a property
	        for (Enumeration e1 = n.proplinks.elements (); e1.hasMoreElements ();  ) try {
	            Property p = (Property) e1.nextElement ();
	            p.node.propMap.remove (p.propname.toLowerCase ());
	        } catch (Exception ignore) {}
	    }
	    // remove all subnodes, giving them a chance to destroy themselves.
	    Vector v = new Vector ();  // removeElement modifies the Vector we are enumerating, so we are extra careful.
	    for (Enumeration e3 = n.getSubnodes (); e3.hasMoreElements (); ) {
	        v.addElement (e3.nextElement ());
	    }
	    int m = v.size ();
	    for (int i=0; i<m; i++) {
	        n.removeNode ((TransientNode) v.elementAt (i));
	    }
	} else {
	    //
	    n.links.removeElement (this);
	}
    }

    /**
     * "Physically" remove a subnode from the subnodes table. 
     * the logical stuff necessary for keeping data consistent is done elsewhere (in removeNode).
     */
    protected void releaseNode (INode node) {
	if (nodes == null || nodeMap == null)

	    return;
	int runner = nodes.indexOf (node);
	// this is due to difference between .equals() and ==
	while (runner > -1 && nodes.elementAt (runner) != node)
	    runner = nodes.indexOf (node, Math.min (nodes.size()-1, runner+1));
	if (runner > -1)
	    nodes.removeElementAt (runner);
	// nodes.remove (node);
	Object what = nodeMap.remove (node.getName ().toLowerCase ());
	// Server.throwNodeEvent (new NodeEvent (node, NodeEvent.NODE_REMOVED));
	// Server.throwNodeEvent (new NodeEvent (this, NodeEvent.SUBNODE_REMOVED, node));
	lastmodified = System.currentTimeMillis ();
	// IServer.getLogger().log ("released node "+node +" from "+this+"     oldobj = "+what);
    }

    public Enumeration getSubnodes () {
	return nodes == null ? new Vector ().elements ()  : nodes.elements ();
    }


    /**
     *  property-related
     */ 

    public Enumeration properties () {
	return propMap == null ? new EmptyEnumeration () : propMap.keys ();
    }


    private Property getProperty (String propname) {  
	Property prop = propMap == null ? null : (Property) propMap.get (propname);
	// check if we have to create a virtual node
	if (prop == null && dbmap != null) {
	    Relation rel = dbmap.getPropertyRelation (propname);
	    if (rel != null && rel.isVirtual ()) {
	        prop = makeVirtualNode (propname, rel);
	    }
	}
	return prop;
    }

    private Property makeVirtualNode (String propname, Relation rel) {
	INode node = new helma.objectmodel.db.Node (rel.getPropName (), rel.getPrototype (), dbmap.getWrappedNodeManager());
	// node.setState (TRANSIENT);
	// make a db mapping good enough that the virtual node finds its subnodes
	// DbMapping dbm = new DbMapping ();
	// dbm.setSubnodeRelation (rel);
	// dbm.setPropertyRelation (rel);
	node.setDbMapping (rel.getVirtualMapping ());
	setNode (propname, node);
	return (Property) propMap.get (propname);
    }


    public IProperty get (String propname) {
	propname = propname.toLowerCase ();
	return getProperty (propname);
    }

    public String getString (String propname, String defaultValue) {
	String propValue = getString (propname);
	return propValue == null ? defaultValue : propValue;
    }

    public String getString (String propname) {  
	propname = propname.toLowerCase ();
	Property prop = getProperty (propname);
	try {
	    return prop.getStringValue ();
	} catch (Exception ignore) {}
	return null;
    }

    public long getInteger (String propname) {  
	propname = propname.toLowerCase ();
	Property prop = getProperty (propname);
	try {
	    return prop.getIntegerValue ();
	} catch (Exception ignore) {}
	return 0;
    }

    public double getFloat (String propname) {  
	propname = propname.toLowerCase ();
	Property prop = getProperty (propname);
	try {
	    return prop.getFloatValue ();
	} catch (Exception ignore) {}
	return 0.0;
    }

    public Date getDate (String propname) {  
	propname = propname.toLowerCase ();
	Property prop = getProperty (propname);
	try {
	    return prop.getDateValue ();
	} catch (Exception ignore) {}
	return null;
    }


    public boolean getBoolean (String propname) {  
	propname = propname.toLowerCase ();
	Property prop = getProperty (propname);
	try {
	    return prop.getBooleanValue ();
	} catch (Exception ignore) {}
	return false;
    }

    public INode getNode (String propname) {  
	propname = propname.toLowerCase ();
	Property prop = getProperty (propname);
	try {
	    return prop.getNodeValue ();
	} catch (Exception ignore) {}
	return null;
    }

    public Object getJavaObject (String propname) {
	propname = propname.toLowerCase ();
	Property prop = getProperty (propname);
	try {
	    return prop.getJavaObjectValue ();
	} catch (Exception ignore) {}
	return null;
    }

    // create a property if it doesn't exist for this name
    private Property initProperty (String propname) {
	if (propMap == null)
	    propMap = new Hashtable ();
	propname = propname.trim ();
	String p2 = propname.toLowerCase ();
	Property prop = (Property) propMap.get (p2);
	if (prop == null) {
	    prop = new Property (propname, this);
	    propMap.put (p2, prop);
	}
	return prop;
    }

    public void setString (String propname, String value) {
	// IServer.getLogger().log ("setting String prop");
	Property prop = initProperty (propname);
	prop.setStringValue (value);
	// Server.throwNodeEvent (new NodeEvent (this, NodeEvent.PROPERTIES_CHANGED));
	lastmodified = System.currentTimeMillis ();
    }

    public void setInteger (String propname, long value) {
	// IServer.getLogger().log ("setting bool prop");
	Property prop = initProperty (propname);
	prop.setIntegerValue (value);
	// Server.throwNodeEvent (new NodeEvent (this, NodeEvent.PROPERTIES_CHANGED));
	lastmodified = System.currentTimeMillis ();
    }

    public void setFloat (String propname, double value) {
	// IServer.getLogger().log ("setting bool prop");
	Property prop = initProperty (propname);
	prop.setFloatValue (value);
	// Server.throwNodeEvent (new NodeEvent (this, NodeEvent.PROPERTIES_CHANGED));
	lastmodified = System.currentTimeMillis ();
    }

    public void setBoolean (String propname, boolean value) {
	// IServer.getLogger().log ("setting bool prop");
	Property prop = initProperty (propname);
	prop.setBooleanValue (value);
	// Server.throwNodeEvent (new NodeEvent (this, NodeEvent.PROPERTIES_CHANGED));
	lastmodified = System.currentTimeMillis ();
    }


    public void setDate (String propname, Date value) {
	// IServer.getLogger().log ("setting date prop");
	Property prop = initProperty (propname);
	prop.setDateValue (value);
	// Server.throwNodeEvent (new NodeEvent (this, NodeEvent.PROPERTIES_CHANGED));
	lastmodified = System.currentTimeMillis ();
    }

    public void setJavaObject (String propname, Object value) {
	// IServer.getLogger().log ("setting date prop");
	Property prop = initProperty (propname);
	prop.setJavaObjectValue (value);
	// Server.throwNodeEvent (new NodeEvent (this, NodeEvent.PROPERTIES_CHANGED));
	lastmodified = System.currentTimeMillis ();
    }

    public void setNode (String propname, INode value) {
	// IServer.getLogger().log ("setting date prop");
	Property prop = initProperty (propname);
	prop.setNodeValue (value);
	
	// check if the main identity of this node is as a named property
	// or as an anonymous node in a collection
	if (value instanceof TransientNode) {
	    TransientNode n = (TransientNode) value;
	    if (n.parent == null) { 
	        n.name = propname;
	        n.parent = this;
	        n.anonymous = false;
	    }
	}
	
	lastmodified = System.currentTimeMillis ();
    }

    public void unset (String propname) {
	if (propMap == null)
	    return;
	try {
	    Property p = (Property) propMap.remove (propname.toLowerCase ());
	    lastmodified = System.currentTimeMillis ();
	} catch (Exception ignore) {}
    }


    /* public String getUrl (INode root, INode users, String tmpname, String rootproto) {
        throw new RuntimeException ("HREFs on transient (non-db based) Nodes not supported");
    } */


    public long lastModified () {
	return lastmodified;
    }

    public long created () {
	return created;
    }

    public String toString () {
	return "TransientNode " + name;
    }


    INode cacheNode;
    /**
     * Get the cache node for this node. This can
     * be used to store transient cache data per node
     * from Javascript.
     */
    public synchronized INode getCacheNode () {
	if (cacheNode == null)
	    cacheNode = new TransientNode();
	return cacheNode;
    }

    /**
     * Reset the cache node for this node.
     */
    public synchronized void clearCacheNode () {
	cacheNode = null;
    }

}




