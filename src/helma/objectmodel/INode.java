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
import helma.objectmodel.db.DbMapping;
import java.util.*;

/**
 * Interface that all Nodes implement. Currently, there are two implementations:
 * Transient nodes which only exist in memory, and persistent Nodes, which are
 * stored in a database (either the internal Object DB or an external relational DB).
 */
public interface INode extends INodeState, IPathElement {
    /**
     *  id-related methods
     */
    public String getID();

    /**
     *
     *
     * @return ...
     */
    public String getName();

    /**
     *
     *
     * @param dbmap ...
     */
    public void setDbMapping(DbMapping dbmap);

    /**
     *
     *
     * @return ...
     */
    public DbMapping getDbMapping();

    /**
     *
     *
     * @return ...
     */
    public int getState();

    /**
     *
     *
     * @param s ...
     */
    public void setState(int s);

    /**
     *
     *
     * @param name ...
     */
    public void setName(String name);

    /**
     *
     *
     * @return ...
     */
    public long lastModified();

    /**
     *
     *
     * @return ...
     */
    public long created();

    /**
     *
     *
     * @return ...
     */
    public boolean isAnonymous(); // is this a named node, or an anonymous node in a collection?

    /**
     *
     *
     * @return ...
     */
    public String getPrototype();

    /**
     *
     *
     * @param prototype ...
     */
    public void setPrototype(String prototype);

    /**
     *
     *
     * @return ...
     */
    public INode getCacheNode();

    /**
     *
     */
    public void clearCacheNode();

    /**
     *
     *
     * @return ...
     */
    public String getFullName();

    /**
     *
     *
     * @param root ...
     *
     * @return ...
     */
    public String getFullName(INode root);

    /**
     *  node-related methods
     */
    public INode getParent();

    /**
     *
     *
     * @param rel ...
     */
    public void setSubnodeRelation(String rel);

    /**
     *
     *
     * @return ...
     */
    public String getSubnodeRelation();

    /**
     *
     *
     * @return ...
     */
    public int numberOfNodes();

    /**
     *
     *
     * @param node ...
     *
     * @return ...
     */
    public INode addNode(INode node);

    /**
     *
     *
     * @param node ...
     * @param where ...
     *
     * @return ...
     */
    public INode addNode(INode node, int where);

    /**
     *
     *
     * @param name ...
     *
     * @return ...
     */
    public INode createNode(String name);

    /**
     *
     *
     * @param name ...
     * @param where ...
     *
     * @return ...
     */
    public INode createNode(String name, int where);

    /**
     *
     *
     * @return ...
     */
    public Enumeration getSubnodes();

    /**
     *
     *
     * @param name ...
     *
     * @return ...
     */
    public INode getSubnode(String name);

    /**
     *
     *
     * @param index ...
     *
     * @return ...
     */
    public INode getSubnodeAt(int index);

    /**
     *
     *
     * @param node ...
     *
     * @return ...
     */
    public int contains(INode node);

    /**
     *
     *
     * @return ...
     */
    public boolean remove();

    /**
     *
     *
     * @param node ...
     */
    public void removeNode(INode node);

    /**
     *  property-related methods
     */
    public Enumeration properties();

    /**
     *
     *
     * @param name ...
     *
     * @return ...
     */
    public IProperty get(String name);

    /**
     *
     *
     * @param name ...
     *
     * @return ...
     */
    public String getString(String name);

    /**
     *
     *
     * @param name ...
     *
     * @return ...
     */
    public boolean getBoolean(String name);

    /**
     *
     *
     * @param name ...
     *
     * @return ...
     */
    public Date getDate(String name);

    /**
     *
     *
     * @param name ...
     *
     * @return ...
     */
    public long getInteger(String name);

    /**
     *
     *
     * @param name ...
     *
     * @return ...
     */
    public double getFloat(String name);

    /**
     *
     *
     * @param name ...
     *
     * @return ...
     */
    public INode getNode(String name);

    /**
     *
     *
     * @param name ...
     *
     * @return ...
     */
    public Object getJavaObject(String name);

    /**
     *
     *
     * @param name ...
     * @param value ...
     */
    public void setString(String name, String value);

    /**
     *
     *
     * @param name ...
     * @param value ...
     */
    public void setBoolean(String name, boolean value);

    /**
     *
     *
     * @param name ...
     * @param value ...
     */
    public void setDate(String name, Date value);

    /**
     *
     *
     * @param name ...
     * @param value ...
     */
    public void setInteger(String name, long value);

    /**
     *
     *
     * @param name ...
     * @param value ...
     */
    public void setFloat(String name, double value);

    /**
     *
     *
     * @param name ...
     * @param value ...
     */
    public void setNode(String name, INode value);

    /**
     *
     *
     * @param name ...
     * @param value ...
     */
    public void setJavaObject(String name, Object value);

    /**
     *
     *
     * @param name ...
     */
    public void unset(String name);
}
