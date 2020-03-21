/*
 * Helma License Notice
 *
 * The contents of this file are subject to the Helma License
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://adele.helma.org/download/helma/license.txt
 *
 * Copyright 1998-2003 Helma Software. All Rights Reserved.
 *
 * $RCSfile$
 * $Author$
 * $Revision$
 * $Date$
 */

package helma.objectmodel;

import helma.framework.IPathElement;
import helma.framework.core.Application;
import helma.framework.core.RequestEvaluator;
import helma.objectmodel.db.DbMapping;
import helma.objectmodel.db.Relation;
import helma.objectmodel.db.Node;
import helma.util.*;
import java.io.*;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * A transient implementation of INode. An instance of this class can't be
 * made persistent by reachability from a persistent node. To make a persistent-capable
 * object, class helma.objectmodel.db.Node has to be used.
 */
public class TransientNode implements INode, Serializable {
    private static final long serialVersionUID = -4599844796152072979L;

    private static long idgen = 0;
    protected Hashtable propMap;
    protected Hashtable nodeMap;
    protected Vector nodes;
    protected TransientNode parent;
    transient String prototype;
    protected long created;
    protected long lastmodified;
    protected String id;
    protected String name;
    private final Application app;

    // is the main identity a named property or an anonymous node in a collection?
    protected boolean anonymous = false;
    transient DbMapping dbmap;
    INode cacheNode;

    /**
     * Creates a new TransientNode object.
     */
    public TransientNode(Application app) {
        id = generateID();
        name = id;
        created = lastmodified = System.currentTimeMillis();
        this.app=app;
    }
    
    private TransientNode() {
        app=null;
    }

    /**
     *  Make a new TransientNode object with a given name
     */
    public TransientNode(Application app, String n) {
        id = generateID();
        name = (n == null || n.length() == 0) ? id : n;
        // HACK - decrease creation and last-modified timestamp by 1 so we notice 
        // modifications that take place immediately after object creation
        created = lastmodified = System.currentTimeMillis() - 1;
        this.app = app;
    }

    public static String generateID() {
        // make transient ids differ from persistent ones
        // and are unique within on runtime session
        return "t" + idgen++;
    }

    public void setDbMapping(DbMapping dbmap) {
        this.dbmap = dbmap;
    }

    public DbMapping getDbMapping() {
        return dbmap;
    }

    public String getID() {
        return id;
    }

    public boolean isAnonymous() {
        return anonymous;
    }

    public String getName() {
        return name;
    }

    public String getElementName() {
        return anonymous ? id : name;
    }

    public int getState() {
        return TRANSIENT;
    }

    public void setState(int s) {
        // state always is TRANSIENT on this kind of node
    }

    public String getPath() {
        return getFullName(null);
    }

    public String getFullName(INode root) {
        String divider = null;
        StringBuffer b = new StringBuffer();
        TransientNode p = this;

        while ((p != null) && (p.parent != null) && (p != root)) {
            if (divider != null) {
                b.insert(0, divider);
            } else {
                divider = "/";
            }

            b.insert(0, p.getElementName());
            p = p.parent;
        }

        return b.toString();
    }

    public void setName(String name) {
        // if (name.indexOf('/') > -1)
        //     throw new RuntimeException ("The name of the node must not contain \"/\".");
        if ((name == null) || (name.trim().length() == 0)) {
            this.name = id;
        } else {
            this.name = name;
        }
    }

    public String getPrototype() {
        // if prototype is null, it's a vanilla HopObject.
        if (prototype == null) {
            return "HopObject";
        }

        return prototype;
    }

    public void setPrototype(String proto) {
        this.prototype = proto;
    }

    public INode getParent() {
        return parent;
    }

    public void setSubnodeRelation(String rel) {
        throw new UnsupportedOperationException("Can't set subnode relation for non-persistent Node.");
    }

    public String getSubnodeRelation() {
        return null;
    }

    public int numberOfNodes() {
        return (nodes == null) ? 0 : nodes.size();
    }

    public INode addNode(INode elem) {
        return addNode(elem, numberOfNodes());
    }

    public INode addNode(INode elem, int where) {
        if ((where < 0) || (where > numberOfNodes())) {
            where = numberOfNodes();
        }

        String n = elem.getName();

        if (n.indexOf('/') > -1) {
            throw new RuntimeException("The name of a node must not contain \"/\" (slash).");
        }

        if ((nodeMap != null) && (nodeMap.get(elem.getID()) != null)) {
            nodes.removeElement(elem);
            where = Math.min(where, numberOfNodes());
            nodes.insertElementAt(elem, where);

            return elem;
        }

        if (nodeMap == null) {
            nodeMap = new Hashtable();
        }

        if (nodes == null) {
            nodes = new Vector();
        }

        nodeMap.put(elem.getID(), elem);
        nodes.insertElementAt(elem, where);

        if (elem instanceof TransientNode) {
            TransientNode node = (TransientNode) elem;

            if (node.parent == null) {
                node.parent = this;
                node.anonymous = true;
            }
        }

        lastmodified = System.currentTimeMillis();
        return elem;
    }

    public INode createNode() {
        return createNode(null, 0); // where is ignored since this is an anonymous node
    }

    public INode createNode(int where) {
        return createNode(null, where);
    }

    public INode createNode(String nm) {
        return createNode(nm, numberOfNodes()); // where is usually ignored (if nm != null)
    }

    public INode createNode(String nm, int where) {
        boolean anon = false;

        if ((nm == null) || "".equals(nm.trim())) {
            anon = true;
        }

        INode n = new TransientNode(app, nm);

        if (anon) {
            addNode(n, where);
        } else {
            setNode(nm, n);
        }

        return n;
    }


    public IPathElement getParentElement() {
        return getParent();
    }

    public IPathElement getChildElement(String name) {
        return getNode(name);
    }

    public INode getSubnode(String name) {
        StringTokenizer st = new StringTokenizer(name, "/");
        TransientNode retval = this;
        TransientNode runner;

        while (st.hasMoreTokens() && (retval != null)) {
            runner = retval;

            String next = st.nextToken().trim().toLowerCase();

            if ("".equals(next)) {
                retval = this;
            } else {
                retval = (runner.nodeMap == null) ? null
                                                  : (TransientNode) runner.nodeMap.get(next);
            }

            if (retval == null) {
                retval = (TransientNode) runner.getNode(next);
            }
        }

        return retval;
    }

    public INode getSubnodeAt(int index) {
        return (nodes == null) ? null : (INode) nodes.elementAt(index);
    }

    public int contains(INode n) {
        if ((n == null) || (nodes == null)) {
            return -1;
        }

        return nodes.indexOf(n);
    }

    public boolean remove() {
        if (anonymous) {
            parent.unset(name);
        } else {
            parent.removeNode(this);
        }

        return true;
    }

    public void removeNode(INode node) {
        // IServer.getLogger().log ("removing: "+ node);
        releaseNode(node);

        TransientNode n = (TransientNode) node;

        if ((n.getParent() == this) && n.anonymous) {

            // remove all subnodes, giving them a chance to destroy themselves.
            Vector v = new Vector(); // removeElement modifies the Vector we are enumerating, so we are extra careful.

            for (Enumeration e3 = n.getSubnodes(); e3.hasMoreElements();) {
                v.addElement(e3.nextElement());
            }

            int m = v.size();

            for (int i = 0; i < m; i++) {
                n.removeNode((TransientNode) v.elementAt(i));
            }
        }
    }

    /**
     * "Physically" remove a subnode from the subnodes table.
     * the logical stuff necessary for keeping data consistent is done elsewhere (in removeNode).
     */
    protected void releaseNode(INode node) {
        if ((nodes == null) || (nodeMap == null)) {

            return;
        }

        int runner = nodes.indexOf(node);

        // this is due to difference between .equals() and ==
        while ((runner > -1) && (nodes.elementAt(runner) != node))
            runner = nodes.indexOf(node, Math.min(nodes.size() - 1, runner + 1));

        if (runner > -1) {
            nodes.removeElementAt(runner);
        }

        nodeMap.remove(node.getName().toLowerCase());
        lastmodified = System.currentTimeMillis();
    }

    /**
     *
     *
     * @return ...
     */
    public Enumeration getSubnodes() {
        return (nodes == null) ? new Vector().elements() : nodes.elements();
    }

    /**
     *  property-related
     */
    public Enumeration properties() {
        return (propMap == null) ? new EmptyEnumeration() : propMap.keys();
    }

    private TransientProperty getProperty(String propname) {
        TransientProperty prop = (propMap == null) ? null 
                : (TransientProperty) propMap.get(correctPropertyName(propname));

        // check if we have to create a virtual node
        if ((prop == null) && (dbmap != null)) {
            Relation rel = dbmap.getPropertyRelation(propname);

            if ((rel != null) && rel.isVirtual()) {
                prop = makeVirtualNode(propname, rel);
            }
        }

        return prop;
    }

    private TransientProperty makeVirtualNode(String propname, Relation rel) {
        INode node = new Node(rel.getPropName(), rel.getPrototype(),
                                                   dbmap.getWrappedNodeManager());

        node.setDbMapping(rel.getVirtualMapping());
        setNode(propname, node);

        return (TransientProperty) propMap.get(correctPropertyName(propname));
    }

    public IProperty get(String propname) {
        return getProperty(propname);
    }

    public String getString(String propname, String defaultValue) {
        String propValue = getString(propname);

        return (propValue == null) ? defaultValue : propValue;
    }

    public String getString(String propname) {
        TransientProperty prop = getProperty(propname);

        try {
            return prop.getStringValue();
        } catch (Exception ignore) {
        }

        return null;
    }

    public long getInteger(String propname) {
        TransientProperty prop = getProperty(propname);

        try {
            return prop.getIntegerValue();
        } catch (Exception ignore) {
        }

        return 0;
    }

    public double getFloat(String propname) {
        TransientProperty prop = getProperty(propname);

        try {
            return prop.getFloatValue();
        } catch (Exception ignore) {
        }

        return 0.0;
    }

    public Date getDate(String propname) {
        TransientProperty prop = getProperty(propname);

        try {
            return prop.getDateValue();
        } catch (Exception ignore) {
        }

        return null;
    }

    public boolean getBoolean(String propname) {
        TransientProperty prop = getProperty(propname);

        try {
            return prop.getBooleanValue();
        } catch (Exception ignore) {
        }

        return false;
    }

    public INode getNode(String propname) {
        TransientProperty prop = getProperty(propname);

        try {
            return prop.getNodeValue();
        } catch (Exception ignore) {
        }

        return null;
    }

    public Object getJavaObject(String propname) {
        TransientProperty prop = getProperty(propname);

        try {
            return prop.getJavaObjectValue();
        } catch (Exception ignore) {
        }

        return null;
    }

    // create a property if it doesn't exist for this name
    private TransientProperty initProperty(String propname) {
        if (propMap == null) {
            propMap = new Hashtable();
        }

        propname = propname.trim();
        String cpn = correctPropertyName(propname);
        TransientProperty prop = (TransientProperty) propMap.get(cpn);

        if (prop == null) {
            prop = new TransientProperty(propname, this);
            propMap.put(cpn, prop);
        }

        return prop;
    }

    public void setString(String propname, String value) {
        TransientProperty prop = initProperty(propname);
        prop.setStringValue(value);
        lastmodified = System.currentTimeMillis();
    }

    public void setInteger(String propname, long value) {
        TransientProperty prop = initProperty(propname);
        prop.setIntegerValue(value);
        lastmodified = System.currentTimeMillis();
    }

    public void setFloat(String propname, double value) {
        TransientProperty prop = initProperty(propname);
        prop.setFloatValue(value);
        lastmodified = System.currentTimeMillis();
    }

    public void setBoolean(String propname, boolean value) {
        TransientProperty prop = initProperty(propname);
        prop.setBooleanValue(value);
        lastmodified = System.currentTimeMillis();
    }

    public void setDate(String propname, Date value) {
        TransientProperty prop = initProperty(propname);
        prop.setDateValue(value);
        lastmodified = System.currentTimeMillis();
    }

    public void setJavaObject(String propname, Object value) {
        TransientProperty prop = initProperty(propname);
        prop.setJavaObjectValue(value);
        lastmodified = System.currentTimeMillis();
    }

    public void setNode(String propname, INode value) {
        TransientProperty prop = initProperty(propname);
        prop.setNodeValue(value);

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

        lastmodified = System.currentTimeMillis();
    }

    public void unset(String propname) {
        if (propMap != null && propname != null) {
            propMap.remove(correctPropertyName(propname));
            lastmodified = System.currentTimeMillis();
        }
    }

    public long lastModified() {
        return lastmodified;
    }

    public long created() {
        return created;
    }

    public String toString() {
        return "TransientNode " + name;
    }

    /**
     * Get the cache node for this node. This can
     * be used to store transient cache data per node
     * from Javascript.
     */
    public synchronized INode getCacheNode() {
        if (cacheNode == null) {
            cacheNode = new TransientNode(app);
        }

        return cacheNode;
    }

    /**
     * Reset the cache node for this node.
     */
    public synchronized void clearCacheNode() {
        cacheNode = null;
    }
    
    private String correctPropertyName(String propname) {
        return app.correctPropertyName(propname);
    }
}
