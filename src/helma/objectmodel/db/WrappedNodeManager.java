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

import helma.objectmodel.*;
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
     *
     *
     * @param id ...
     * @param dbmap ...
     *
     * @return ...
     */
    public Node getNode(String id, DbMapping dbmap) {
        return getNode(new DbKey(dbmap, id));
    }

    /**
     *
     *
     * @param key ...
     *
     * @return ...
     */
    public Node getNode(Key key) {
        try {
            return nmgr.getNode(key);
        } catch (ObjectNotFoundException x) {
            return null;
        } catch (Exception x) {
            nmgr.app.logEvent("Error retrieving Node via DbMapping: " + x);

            if (nmgr.app.debug()) {
                x.printStackTrace();
            }

            throw new RuntimeException("Error retrieving Node: " + x);
        }
    }

    /**
     *
     *
     * @param home ...
     * @param id ...
     * @param rel ...
     *
     * @return ...
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
     *
     *
     * @param home ...
     * @param rel ...
     *
     * @return ...
     */
    public List getNodes(Node home, Relation rel) {
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
     *
     *
     * @param home ...
     * @param rel ...
     *
     * @return ...
     */
    public List getNodeIDs(Node home, Relation rel) {
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
     *
     *
     * @param home ...
     * @param rel ...
     *
     * @return ...
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
     *
     *
     * @param node ...
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
     *
     *
     * @param home ...
     * @param rel ...
     *
     * @return ...
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
     *
     *
     * @param node ...
     */
    public void registerNode(Node node) {
        nmgr.registerNode(node);
    }

    /**
     *
     *
     * @param node ...
     */
    public void evictNode(Node node) {
        nmgr.evictNode(node);
    }

    /**
     *
     *
     * @param key ...
     */
    public void evictNodeByKey(Key key) {
        nmgr.evictNodeByKey(key);
    }

    /**
     *
     *
     * @param key ...
     */
    public void evictKey(Key key) {
        nmgr.evictKey(key);
    }

    /**
     *
     *
     * @return ...
     */
    public String generateID() {
        return nmgr.idgen.newID();
    }

    /**
     *
     *
     * @param map ...
     *
     * @return ...
     */
    public String generateID(DbMapping map) {
        try {
            // check if we use internal id generator
            if ((map == null) || !map.isRelational() ||
                    "[hop]".equalsIgnoreCase(map.getIDgen())) {
                return nmgr.idgen.newID();
            }
            // or if we query max key value
            else if ((map.getIDgen() == null) ||
                         "[max]".equalsIgnoreCase(map.getIDgen())) {
                return nmgr.generateMaxID(map);
            } else {
                return nmgr.generateID(map);
            }

            // otherwise, we use an oracle sequence
        } catch (Exception x) {
            if (nmgr.app.debug()) {
                x.printStackTrace();
            }

            throw new RuntimeException(x.toString());
        }
    }

    /**
     *
     *
     * @return ...
     */
    public Object[] getCacheEntries() {
        return nmgr.getCacheEntries();
    }

    /**
     *
     *
     * @param msg ...
     */
    public void logEvent(String msg) {
        nmgr.app.logEvent(msg);
    }

    /**
     *
     *
     * @param name ...
     *
     * @return ...
     */
    public DbMapping getDbMapping(String name) {
        return nmgr.app.getDbMapping(name);
    }
}
