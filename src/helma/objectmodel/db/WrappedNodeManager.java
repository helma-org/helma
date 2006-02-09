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

import helma.objectmodel.ObjectNotFoundException;

import java.util.List;
import java.util.Vector;

/**
 * A wrapper around NodeManager that catches most Exceptions, or rethrows them as RuntimeExceptions.
 * The idea behind this is that we don't care a lot about Exception classes, since Hop programming is done
 * in JavaScript which doesn't know about them (except for the exception message).
 */
public final class WrappedNodeManager {
    NodeManager nmgr;

    /**
     * Creates a new WrappedNodeManager object.
     *
     * @param nmgr ...
     */
    public WrappedNodeManager(NodeManager nmgr) {
        this.nmgr = nmgr;
    }

    /**
     * Get a node given its id and DbMapping
     *
     * @param id
     * @param dbmap
     * @return
     */
    public Node getNode(String id, DbMapping dbmap) {
        return getNode(new DbKey(dbmap, id));
    }

    /**
     * Get a node given its key
     *
     * @param key
     * @return
     */
    public Node getNode(Key key) {
        try {
            return nmgr.getNode(key);
        } catch (ObjectNotFoundException x) {
            return null;
        } catch (Exception x) {
            nmgr.app.logEvent("Error retrieving Node for "+key+": " + x);

            if (nmgr.app.debug()) {
                x.printStackTrace();
            }

            throw new RuntimeException("Error retrieving Node: " + x);
        }
    }

    /**
     * Get the node specified by the given id and Relation.
     *
     * @param home
     * @param id
     * @param rel
     * @return
     */
    public Node getNode(Node home, String id, Relation rel) {
        try {
            return nmgr.getNode(home, id, rel);
        } catch (ObjectNotFoundException x) {
            return null;
        } catch (Exception x) {
            nmgr.app.logEvent("Error retrieving Node \"" + id + "\" from " + home + ": " +
                              x);

            if (nmgr.app.debug()) {
                x.printStackTrace();
            }

            throw new RuntimeException("Error retrieving Node: " + x);
        }
    }

    /**
     * Get the list of nodes contained in the collection of the given
     * Node specified by the given Relation.
     *
     * @param home
     * @param rel
     * @return
     */
    public SubnodeList getNodes(Node home, Relation rel) {
        try {
            return nmgr.getNodes(home, rel);
        } catch (Exception x) {
            if (nmgr.app.debug()) {
                x.printStackTrace();
            }

            throw new RuntimeException("Error retrieving Nodes: " + x);
        }
    }

    /**
     * Get a list of IDs of nodes contained in the given Node's
     * collection specified by the given Relation.
     *
     * @param home
     * @param rel
     * @return
     */
    public SubnodeList getNodeIDs(Node home, Relation rel) {
        try {
            return nmgr.getNodeIDs(home, rel);
        } catch (Exception x) {
            if (nmgr.app.debug()) {
                x.printStackTrace();
            }

            throw new RuntimeException("Error retrieving NodeIDs: " + x);
        }
    }

    /**
     * @see helma.objectmodel.db.NodeManager#updateSubnodeList(Node, Relation)
     */
    public int updateSubnodeList (Node home, Relation rel) {
        try {
            return nmgr.updateSubnodeList(home, rel);
        } catch (Exception x) {
            if (nmgr.app.debug()) {
                x.printStackTrace();
            }

            throw new RuntimeException("Error retrieving NodeIDs: ", x);
        }
    }

    /**
     * Count the nodes contained in the given Node's collection
     * specified by the given Relation.
     *
     * @param home
     * @param rel
     * @return
     */
    public int countNodes(Node home, Relation rel) {
        try {
            return nmgr.countNodes(home, rel);
        } catch (Exception x) {
            if (nmgr.app.debug()) {
                x.printStackTrace();
            }

            throw new RuntimeException("Error counting Node: " + x);
        }
    }

    /**
     * Delete a node from the database
     *
     * @param node
     */
    public void deleteNode(Node node) {
        try {
            nmgr.deleteNode(node);
        } catch (Exception x) {
            if (nmgr.app.debug()) {
                x.printStackTrace();
            }

            throw new RuntimeException("Error deleting Node: " + x);
        }
    }

    /**
     * Get a list of property names from the given node.
     * TODO: this retrieves access names of child nodes, not property names
     *
     * @param home
     * @param rel
     * @return
     */
    public Vector getPropertyNames(Node home, Relation rel) {
        try {
            return nmgr.getPropertyNames(home, rel);
        } catch (Exception x) {
            if (nmgr.app.debug()) {
                x.printStackTrace();
            }

            throw new RuntimeException("Error retrieving property names: " + x);
        }
    }

    /**
     * Register a node with the object cache using its primary key.
     *
     * @param node
     */
    public void registerNode(Node node) {
        nmgr.registerNode(node);
    }

    /**
     * Register a node with the object cache using the given key.
     *
     * @param node
     */
    public void registerNode(Node node, Key key) {
        nmgr.registerNode(node, key);
    }

    /**
     * Evict a node from the object cache
     *
     * @param node
     */
    public void evictNode(Node node) {
        nmgr.evictNode(node);
    }

    /**
     * Completely evict the object with the given key from the object cache
     *
     * @param key
     */
    public void evictNodeByKey(Key key) {
        nmgr.evictNodeByKey(key);
    }

    /**
     * Evict the object with the given key from the object cache
     *
     * @param key
     */
    public void evictKey(Key key) {
        nmgr.evictKey(key);
    }

    /**
     * Generate a new id for an object specified by the DbMapping
     *
     * @param map the DbMapping to generate an id for
     * @return a new unique id
     */
    public String generateID(DbMapping map) {
        try {
            return nmgr.generateID(map);
        } catch (Exception x) {
            if (nmgr.app.debug()) {
                x.printStackTrace();
            }
            throw new RuntimeException(x.toString());
        }
    }

    /**
     * Gets the application's root node.
     */
    public Node getRootNode() {
        try {
            return nmgr.getRootNode();
        } catch (Exception x) {
            if (nmgr.app.debug()) {
                x.printStackTrace();
            }
            throw new RuntimeException(x.toString());
        }
    }

    /**
     * Checks if the given node is the application's root node.
     */
    public boolean isRootNode(Node node) {
        return nmgr.isRootNode(node);
    }

    /**
     * Get an array of all objects in the object cache
     */
    public Object[] getCacheEntries() {
        return nmgr.getCacheEntries();
    }

    /**
     * Write an entry to the application's event log
     *
     * @param msg event message
     */
    public void logEvent(String msg) {
        nmgr.app.logEvent(msg);
    }

    /**
     * Get the DbMapping corresponding to a type name
     *
     * @param name a type name
     * @return the corresponding DbMapping
     */
    public DbMapping getDbMapping(String name) {
        return nmgr.app.getDbMapping(name);
    }
}
