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

package helma.objectmodel.db;

import helma.objectmodel.INodeState;

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
    static final long serialVersionUID = 3067763116576910931L;

    // direct reference to the node
    private Node node;

    // the node's key
    private Key key;

    /**
     *  Builds a handle for a node
     */
    public NodeHandle(Node node) {
        int state = node.getState();

        if (state == TRANSIENT) {
            this.node = node;
            key = null;
        } else {
            this.node = null;
            key = node.getKey();
        }
    }

    /**
     * Builds a handle given a node's retrieval information. At the time this is called,
     * the node is ususally not yet created. It will be fetched on demand when accessed by
     * application code.
     */
    public NodeHandle(Key key) {
        this.node = null;
        this.key = key;
    }

    /**
     *  Get the node described by this node handle
     */
    public Node getNode(WrappedNodeManager nodemgr) {
        if (node != null) {
            return node;
        }

        return nodemgr.getNode(key);
    }

    /**
     *  Get the key for the node described by this handle.
     *  This may only be called on persistent Nodes.
     */
    public Key getKey() {
        if (key == null) {
            throw new RuntimeException("getKey called on transient Node");
        }

        return key;
    }

    /**
     *  Get the ID for the node described by this handle.
     *  This may only be called on persistent Nodes.
     */
    public String getID() {
        if (key == null) {
            return node.getID();
        }

        return key.getID();
    }

    private Object getObject() {
        if (node != null) {
            return node;
        } else {
            return key;
        }
    }

    /**
     *
     *
     * @param other ...
     *
     * @return ...
     */
    public boolean equals(Object other) {
        try {
            return getObject().equals(((NodeHandle) other).getObject());
        } catch (Exception x) {
            return false;
        }
    }

    /**
     * This is to notify the handle that the underlying node is becoming
     * persistent and we have to refer to it via the key from now on.
     */
    protected void becomePersistent() {
        if (node != null) {
            key = node.getKey();
            node = null;
        }
    }

    /**
     *
     *
     * @return ...
     */
    public String toString() {
        if (node != null) {
            return "NodeHandle[transient:" + node + "]";
        } else {
            return "NodeHandle[" + key + "]";
        }
    }
}
