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

import helma.objectmodel.DatabaseException;
import helma.objectmodel.ITransaction;

import java.sql.Connection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

/**
 * A subclass of thread that keeps track of changed nodes and triggers
 * changes in the database when a transaction is commited.
 */
public class Transactor extends Thread {

    // The associated node manager
    NodeManager nmgr;

    // List of nodes to be updated
    private HashMap dirtyNodes;

    // List of visited clean nodes
    private HashMap cleanNodes;

    // List of nodes whose child index has been modified
    private HashSet parentNodes;

    // Is a transaction in progress?
    private volatile boolean active;
    private volatile boolean killed;

    // Transaction for the embedded database
    protected ITransaction txn;

    // Transactions for SQL data sources
    protected HashMap sqlCon;

    // when did the current transaction start?
    private long tstart;

    // a name to log the transaction. For HTTP transactions this is the rerquest path
    private String tname;

    /**
     * Creates a new Transactor object.
     *
     * @param runnable ...
     * @param group ...
     * @param nmgr ...
     */
    public Transactor(Runnable runnable, ThreadGroup group, NodeManager nmgr) {
        super(group, runnable, group.getName());
        this.nmgr = nmgr;

        dirtyNodes = new HashMap();
        cleanNodes = new HashMap();
        parentNodes = new HashSet();

        sqlCon = new HashMap();
        active = false;
        killed = false;
    }

    /**
     *
     *
     * @param node ...
     */
    public void visitNode(Node node) {
        if (node != null) {
            Key key = node.getKey();

            if (!dirtyNodes.containsKey(key)) {
                dirtyNodes.put(key, node);
            }
        }
    }

    /**
     *
     *
     * @param node ...
     */
    public void dropNode(Node node) {
        if (node != null) {
            Key key = node.getKey();

            dirtyNodes.remove(key);
        }
    }

    /**
     *
     *
     * @param node ...
     */
    public void visitCleanNode(Node node) {
        if (node != null) {
            Key key = node.getKey();

            if (!cleanNodes.containsKey(key)) {
                cleanNodes.put(key, node);
            }
        }
    }

    /**
     *
     *
     * @param key ...
     * @param node ...
     */
    public void visitCleanNode(Key key, Node node) {
        if (node != null) {
            if (!cleanNodes.containsKey(key)) {
                cleanNodes.put(key, node);
            }
        }
    }

    /**
     *
     *
     * @param key ...
     *
     * @return ...
     */
    public Node getVisitedNode(Object key) {
        return (key == null) ? null : (Node) cleanNodes.get(key);
    }

    /**
     *
     *
     * @param node ...
     */
    public void visitParentNode(Node node) {
        parentNodes.add(node);
    }


    /**
     *
     *
     * @return ...
     */
    public boolean isActive() {
        return active;
    }

    /**
     *
     *
     * @param src ...
     * @param con ...
     */
    public void registerConnection(DbSource src, Connection con) {
        sqlCon.put(src, con);
    }

    /**
     *
     *
     * @param src ...
     *
     * @return ...
     */
    public Connection getConnection(DbSource src) {
        return (Connection) sqlCon.get(src);
    }

    /**
     *
     *
     * @param name ...
     *
     * @throws Exception ...
     */
    public synchronized void begin(String name) throws Exception {
        if (killed) {
            throw new DatabaseException("Transaction started on killed thread");
        }

        if (active) {
            abort();
        }

        dirtyNodes.clear();
        cleanNodes.clear();
        parentNodes.clear();
        txn = nmgr.db.beginTransaction();
        active = true;
        tstart = System.currentTimeMillis();
        tname = name;
    }

    /**
     *
     *
     * @throws Exception ...
     */
    public synchronized void commit() throws Exception {
        if (killed) {
            abort();

            return;
        }

        int inserted = 0;
        int updated = 0;
        int deleted = 0;

        Object[] dirty = dirtyNodes.values().toArray();

        // the replicator to send update notifications to, if defined
        Replicator replicator = nmgr.getReplicator();
        // the set to collect DbMappings to be marked as changed
        HashSet dirtyDbMappings = new HashSet();

        for (int i = 0; i < dirty.length; i++) {
            Node node = (Node) dirty[i];

            // update nodes in db
            int nstate = node.getState();

            if (nstate == Node.NEW) {
                nmgr.registerNode(node); // register node with nodemanager cache
                nmgr.insertNode(nmgr.db, txn, node);
                dirtyDbMappings.add(node.getDbMapping());
                node.setState(Node.CLEAN);

                if (replicator != null) {
                    replicator.addNewNode(node);
                }

                inserted++;
                nmgr.app.logEvent("inserted: Node " + node.getPrototype() + "/" +
                                  node.getID());
            } else if (nstate == Node.MODIFIED) {
                // only mark DbMapping as dirty if updateNode returns true
                if (nmgr.updateNode(nmgr.db, txn, node)) {
                    dirtyDbMappings.add(node.getDbMapping());
                }
                node.setState(Node.CLEAN);

                if (replicator != null) {
                    replicator.addModifiedNode(node);
                }

                updated++;
                nmgr.app.logEvent("updated: Node " + node.getPrototype() + "/" +
                                  node.getID());
            } else if (nstate == Node.DELETED) {
                nmgr.deleteNode(nmgr.db, txn, node);
                dirtyDbMappings.add(node.getDbMapping());
                nmgr.evictNode(node);

                if (replicator != null) {
                    replicator.addDeletedNode(node);
                }

                deleted++;
            }

            node.clearWriteLock();
        }

        long now = System.currentTimeMillis();

        // set last data change times in db-mappings
        for (Iterator i = dirtyDbMappings.iterator(); i.hasNext(); ) {
            DbMapping dbm = (DbMapping) i.next();
            if (dbm != null) {
                dbm.setLastDataChange(now);
            }
        }

        // set last subnode change times in parent nodes
        for (Iterator i = parentNodes.iterator(); i.hasNext(); ) {
            Node node = (Node) i.next();
            node.setLastSubnodeChange(now);
        }

        // clear the node collections
        dirtyNodes.clear();
        cleanNodes.clear();
        parentNodes.clear();

        // save the id-generator for the embedded db, if necessary
        if (nmgr.idgen.dirty) {
            nmgr.db.saveIDGenerator(txn, nmgr.idgen);
            nmgr.idgen.dirty = false;
        }

        if (active) {
            active = false;
            nmgr.db.commitTransaction(txn);
            txn = null;
        }

        nmgr.app.logAccess(tname + " " + dirty.length + " marked, " + inserted +
                           " inserted, " + updated +
                           " updated, " + deleted + " deleted in " +
                           (now - tstart) + " millis");
    }

    /**
     *
     *
     * @throws Exception ...
     */
    public synchronized void abort() throws Exception {
        Object[] dirty = dirtyNodes.values().toArray();

        // evict dirty nodes from cache
        for (int i = 0; i < dirty.length; i++) {
            Node node = (Node) dirty[i];

            // Declare node as invalid, so it won't be used by other threads
            // that want to write on it and remove it from cache
            nmgr.evictNode(node);
            node.clearWriteLock();
        }

        long now = System.currentTimeMillis();

        // set last subnode change times in parent nodes
        for (Iterator i = parentNodes.iterator(); i.hasNext(); ) {
            Node node = (Node) i.next();
            node.setLastSubnodeChange(now);
        }

        // clear the node collections
        dirtyNodes.clear();
        cleanNodes.clear();
        parentNodes.clear();

        // close any JDBC connections associated with this transactor thread
        closeConnections();

        if (active) {
            active = false;

            if (txn != null) {
                nmgr.db.abortTransaction(txn);
                txn = null;
            }

            nmgr.app.logAccess(tname + " aborted after " +
                               (System.currentTimeMillis() - tstart) + " millis");
        }
    }

    /**
     *
     */
    public synchronized void kill() {
        killed = true;

        // The thread is told to stop by setting the thread flag in the EcmaScript
        // evaluator, so we can hope that it stops without doing anything else.
        try {
            join(500);
        } catch (InterruptedException ir) {
        }

        // Interrupt the thread if it has not noticed the flag (e.g. because it is busy
        // reading from a network socket).
        if (isAlive()) {
            interrupt();

            try {
                join(1000);
            } catch (InterruptedException ir) {
            }
        }
    }

    /**
     *
     */
    public void closeConnections() {
        // nmgr.app.logEvent("Cleaning up Transactor thread");
        if (sqlCon != null) {
            for (Iterator i = sqlCon.values().iterator(); i.hasNext();) {
                try {
                    Connection con = (Connection) i.next();

                    con.close();
                    nmgr.app.logEvent("Closing DB connection: " + con);
                } catch (Exception ignore) {
                }
            }

            sqlCon.clear();
        }
    }

    /**
     *
     *
     * @return ...
     */
    public String toString() {
        return "Transactor[" + tname + "]";
    }
}
