// NodeHandle.java
// Copyright (c) Hannes Wallnöfer 1998-2000
 
package helma.objectmodel.db;

import helma.objectmodel.*;


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
 
public class NodeHandle implements INodeState {

    // direct reference to the node
    private Node node;

    // the node's key
    private Key key;

    // the node manager used to fetch the node from cache or persistent storage
    private WrappedNodeManager nodemgr;

    /**
     *  Builds a handle for a node
     */
    public NodeHandle (Node node) {
	int state = node.getState ();
	if (state == TRANSIENT) {
	    this.node = node;
	    key = null;
	    nodemgr = null;
	} else {
	    this.node = null;
	    key = node.getKey ();
	    nodemgr = node.nmgr;
	}
    }

    /**
     * Builds a handle given a node's retrieval information. At the time this is called,
     * the node is ususally not yet created. It will be fetched on demand when accessed by
     * application code.
     */
    public NodeHandle (Key key, WrappedNodeManager nodemgr) {
	this.node = null;
	this.key = key;
	this.nodemgr = nodemgr;
    }

    /**
     *  Get the node described by this node handle
     */
    public Node getNode () {
	if (node != null) {
	    int state = node.getState ();
	    if (state == TRANSIENT)
	        return node;
	    else {
	        // this node went from TRANSIENT to some other state.
	        // It's time to say goodby to the direct reference, from now on
	        // we'll have to fetch let the node manager fetch it.
	        key = node.getKey ();
	        nodemgr = node.nmgr;
	        node = null;
	    }
	}
	return nodemgr.getNode (key);
    }

    /**
     *  Get the key for the node described by this handle
     */
    public Key getKey () {
	return key;
    }

}
