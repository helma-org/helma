// NodeHandle.java
// Copyright (c) Hannes Wallnöfer 1998-2000
 
package helma.objectmodel.db;

import helma.objectmodel.*;
import java.io.Serializable;

/**
 * This class is a handle or reference to a Node. This is to abstract from different
 * methods of reference: Transient Nodes are referred to directly, while persistent
 * nodes are referred to via key/node manager.
 *
 * A handle is used to refer to a node in a safe way over a longer period.
 * While a direct reference may point to a node that has been evicted from the cache
 * and reinstanciated since being set, NodeHandle will always return an up-to-date
 * instance of its node.
 */
 
public final class NodeHandle implements INodeState, Serializable {

    // direct reference to the node
    private Node node;

    // the node's key
    private Key key;

    // cached DbMapping
    private transient DbMapping dbmap;

    static final long serialVersionUID = 3067763116576910931L;

    /**
     *  Builds a handle for a node
     */
    public NodeHandle (Node node) {
	int state = node.getState ();
	if (state == TRANSIENT) {
	    this.node = node;
	    key = null;
	} else {
	    this.node = null;
	    key = node.getKey ();
	}
    }

    /**
     * Builds a handle given a node's retrieval information. At the time this is called,
     * the node is ususally not yet created. It will be fetched on demand when accessed by
     * application code.
     */
    public NodeHandle (Key key) {
	this.node = null;
	this.key = key;
    }

    /**
     *  Get the node described by this node handle
     */
    public Node getNode (WrappedNodeManager nodemgr) {
	if (node != null) {
	    int state = node.getState ();
	    if (state == TRANSIENT)
	        return node;
	    else {
	        // this node went from TRANSIENT to some other state.
	        // It's time to say goodby to the direct reference, from now on
	        // we'll have to fetch let the node manager fetch it.
	        key = node.getKey ();
	        node = null;
	    }
	}
	return nodemgr.getNode (key);
    }

    /**
     *  Get the key for the node described by this handle. This may only be called on persistent Nodes.
     */
    public Key getKey () {
	if (key == null)
	    throw new RuntimeException ("getKey called on transient Node");
	return key;
    }

    /**
     *  Get the ID for the node described by this handle. This may only be called on persistent Nodes.
     */
    public String getID () {
	if (key == null)
	    throw new RuntimeException ("getID called on transient Node");
	return key.getID ();
    }

    public DbMapping getDbMapping (WrappedNodeManager nmgr) {
	if (dbmap == null) {
	    if (node != null)
	        dbmap = node.getDbMapping ();
	    else
	        dbmap = nmgr.getDbMapping (key.getStorageName ());
	}
	return dbmap;
    }

    private Object getObject () {
	if (node != null) {
	    if (node.getState () != TRANSIENT)
	        return node.getKey ();
	    return node;
	}
	return key;
    }

    public boolean equals (Object other) {
	try {
	    return getObject ().equals (((NodeHandle) other).getObject ());
	} catch (Exception x) {
	    return false;
	}
    }

    public String toString () {
	if (node != null) {
	    if (node.getState () == TRANSIENT)
	        return "NodeHandle[transient:"+node+"]";
	    else
	        key = node.getKey ();
	}
	return "NodeHandle["+key+"]";
    }

}

