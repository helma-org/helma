// INode.java
// Copyright (c) Hannes Wallnöfer 1998-2000
 
package helma.objectmodel;

import java.util.*;
import java.io.*;
import helma.framework.IPathElement;
import helma.objectmodel.db.DbMapping;

/**
 * Interface that all Nodes implement. Currently, there are two implementations:
 * Transient nodes which only exist in memory, and persistent Nodes, which are
 * stored in a database (either the internal Object DB or an external relational DB).
 */
 
public interface INode extends INodeState, IPathElement {


    /** 
     *  id-related methods
     */

    public String getID ();
    public String getName ();
    public void setDbMapping (DbMapping dbmap);
    public DbMapping getDbMapping ();
    public int getState ();
    public void setState (int s);
    public void setName (String name);
    public long lastModified ();
    public long created ();
    public boolean isAnonymous (); // is this a named node, or an anonymous node in a collection?
    public String getPrototype ();
    public void setPrototype (String prototype);
    public INode getCacheNode ();
    public void clearCacheNode ();
    public String getFullName ();
    public String getFullName (INode root);

    /**
     *  node-related methods
     */

    public INode getParent ();
    public void setSubnodeRelation (String rel);
    public String getSubnodeRelation ();
    public int numberOfNodes ();
    public INode addNode (INode node);
    public INode addNode (INode node, int where);
    public INode createNode (String name);
    public INode createNode (String name, int where);
    public Enumeration getSubnodes ();
    public INode getSubnode (String name);
    public INode getSubnodeAt (int index);
    public int contains (INode node);
    public boolean remove ();
    public void removeNode (INode node);

    /**
     *  property-related methods
     */ 

    public Enumeration properties ();
    public IProperty get (String name, boolean inherit);
    public String getString (String name, boolean inherit); 
    // public String getString (String name, String defaultValue, boolean inherit);
    public boolean getBoolean (String name, boolean inherit);
    public Date getDate (String name, boolean inherit);
    public long getInteger (String name, boolean inherit);
    public double getFloat (String name, boolean inherit);
    public INode getNode (String name, boolean inherit);
    public Object getJavaObject (String name, boolean inherit);

    public void setString (String name, String value);
    public void setBoolean (String name, boolean value);
    public void setDate (String name, Date value);
    public void setInteger (String name, long value);
    public void setFloat (String name, double value);
    public void setNode (String name, INode value);
    public void setJavaObject (String name, Object value);

    public void unset (String name);

}




