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
import java.sql.Statement;
import java.sql.SQLException;
import java.util.*;

import org.apache.commons.logging.Log;

/**
 * A subclass of thread that keeps track of changed nodes and triggers
 * changes in the database when a transaction is commited.
 */
public class Transactor {

    // The associated node manager
    NodeManager nmgr;

    // List of nodes to be updated
    private Map dirtyNodes;

    // List of visited clean nodes
    private Map cleanNodes;

    // List of nodes whose child index has been modified
    private Set parentNodes;

    // Is a transaction in progress?
    private volatile boolean active;
    private volatile boolean killed;

    // Transaction for the embedded database
    protected ITransaction txn;

    // Transactions for SQL data sources
    private Map sqlConnections;

    // Set of SQL connections that already have been verified
    private Map testedConnections;

    // when did the current transaction start?
    private long tstart;

    // a name to log the transaction. For HTTP transactions this is the rerquest path
    private String tname;

    // the thread we're associated with
    private Thread thread;

    private static final ThreadLocal txtor = new ThreadLocal();

    /**
     * Creates a new Transactor object.
     *
     * @param nmgr the NodeManager used to fetch and persist nodes.
     */
    private Transactor(NodeManager nmgr) {
        this.thread = Thread.currentThread();
        this.nmgr = nmgr;

        dirtyNodes = new LinkedHashMap();
        cleanNodes = new HashMap();
        parentNodes = new HashSet();

        sqlConnections = new HashMap();
        testedConnections = new HashMap();
        active = false;
        killed = false;
    }

    /**
     * Get the transactor for the current thread or null if none exists.
     * @return the transactor associated with the current thread
     */
    public static Transactor getInstance() {
        return (Transactor) txtor.get();
    }

    /**
     * Get the transactor for the current thread or throw a IllegalStateException if none exists.
     * @return the transactor associated with the current thread
     * @throws IllegalStateException if no transactor is associated with the current thread
     */
    public static Transactor getInstanceOrFail() throws IllegalStateException {
        Transactor tx = (Transactor) txtor.get();
        if (tx == null)
            throw new IllegalStateException("Operation requires a Transactor, " +
                "but current thread does not have one.");
        return tx;
    }

    /**
     * Get the transactor for the current thread, creating a new one if none exists.
     * @param nmgr the NodeManager used to create the transactor
     * @return the transactor associated with the current thread
     */
    public static Transactor getInstance(NodeManager nmgr) {
        Transactor t = (Transactor) txtor.get();
        if (t == null) {
            t = new Transactor(nmgr);
            txtor.set(t);
        }
        return t;
    }

    /**
     * Mark a Node as modified/created/deleted during this transaction
     *
     * @param node ...
     */
    public void visitDirtyNode(Node node) {
        if (node != null) {
            Key key = node.getKey();

            if (!dirtyNodes.containsKey(key)) {
                dirtyNodes.put(key, node);
            }
        }
    }

    /**
     * Unmark a Node that has previously been marked as modified during the transaction
     *
     * @param node ...
     */
    public void dropDirtyNode(Node node) {
        if (node != null) {
            Key key = node.getKey();

            dirtyNodes.remove(key);
        }
    }

    /**
     * Get a dirty Node from this transaction.
     * @param key the key
     * @return the dirty node associated with the key, or null
     */
    public Node getDirtyNode(Key key) {
        return (Node) dirtyNodes.get(key);
    }

    /**
     * Keep a reference to an unmodified Node local to this transaction
     *
     * @param node the node to register
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
     * Keep a reference to an unmodified Node local to this transaction
     *
     * @param key the key to register with
     * @param node the node to register
     */
    public void visitCleanNode(Key key, Node node) {
        if (node != null) {
            if (!cleanNodes.containsKey(key)) {
                cleanNodes.put(key, node);
            }
        }
    }

    /**
     * Drop a reference to an unmodified Node previously registered with visitCleanNode().
     * @param key the key
     */
    public void dropCleanNode(Key key) {
        cleanNodes.remove(key);
    }

    /**
     * Get a reference to an unmodified Node local to this transaction
     *
     * @param key ...
     *
     * @return ...
     */
    public Node getCleanNode(Object key) {
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
     * Returns true if a transaction is currently active.
     * @return true if currently a transaction is active
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Check whether the thread associated with this transactor is alive.
     * This is a proxy to Thread.isAlive().
     * @return true if the thread running this transactor is currently alive.
     */
    public boolean isAlive() {
        return thread != null && thread.isAlive();
    }

    /**
     * Register a db connection with this transactor thread.
     * @param src the db source
     * @param con the connection
     */
    public void registerConnection(DbSource src, Connection con) {
        sqlConnections.put(src, con);
        // we assume a freshly created connection is ok.
        testedConnections.put(src, new Long(System.currentTimeMillis()));
    }

    /**
     * Get a db connection that was previously registered with this transactor thread.
     * @param src the db source
     * @return the connection
     */
    public Connection getConnection(DbSource src) {
        Connection con = (Connection) sqlConnections.get(src);
        Long tested = (Long) testedConnections.get(src);
        long now = System.currentTimeMillis();
        if (con != null && (tested == null || now - tested.longValue() > 10000)) {
            // Check if the connection is still alive by executing a simple statement.
            try {
                Statement stmt = con.createStatement();
                stmt.execute("SELECT 1");
                stmt.close();
                testedConnections.put(src, new Long(now));
            } catch (SQLException sx) {
                try {
                    con.close();
                } catch (SQLException ignore) {/* nothing to do */}
                return null;
            }
        }
        return con;
    }

    /**
     * Start a new transaction with the given name.
     *
     * @param name The name of the transaction. This is usually the request
     * path for the underlying HTTP request.
     *
     * @throws Exception ...
     */
    public synchronized void begin(String name) throws Exception {
        if (killed) {
            throw new DatabaseException("Transaction started on killed thread");
        } else if (active) {
            abort();
        }

        dirtyNodes.clear();
        cleanNodes.clear();
        parentNodes.clear();
        testedConnections.clear();
        txn = nmgr.db.beginTransaction();
        active = true;
        tstart = System.currentTimeMillis();
        tname = name;
    }

    /**
     * Commit the current transaction, persisting all changes to DB.
     *
     * @throws Exception ...
     */
    public synchronized void commit() throws Exception {
        if (killed) {
            throw new DatabaseException("commit() called on killed transactor thread");
        } else if (!active) {
            return;
        }
        int inserted = 0;
        int updated = 0;
        int deleted = 0;

        ArrayList insertedNodes = null;
        ArrayList updatedNodes = null;
        ArrayList deletedNodes = null;
        ArrayList modifiedParentNodes = null;
        // if nodemanager has listeners collect dirty nodes
        boolean hasListeners = nmgr.hasNodeChangeListeners();

        if (hasListeners) {
            insertedNodes = new ArrayList();
            updatedNodes = new ArrayList();
            deletedNodes = new ArrayList();
            modifiedParentNodes = new ArrayList();
        }

        if (!dirtyNodes.isEmpty()) {
            Object[] dirty = dirtyNodes.values().toArray();

            // the set to collect DbMappings to be marked as changed
            HashSet dirtyDbMappings = new HashSet();
            Log eventLog = nmgr.app.getEventLog();

            for (int i = 0; i < dirty.length; i++) {
                Node node = (Node) dirty[i];

                // update nodes in db
                int nstate = node.getState();

                if (nstate == Node.NEW) {
                    nmgr.insertNode(nmgr.db, txn, node);
                    dirtyDbMappings.add(node.getDbMapping());
                    node.setState(Node.CLEAN);

                    // register node with nodemanager cache
                    nmgr.registerNode(node);

                    if (hasListeners) {
                        insertedNodes.add(node);
                    }

                    inserted++;
                    if (eventLog.isDebugEnabled()) {
                        eventLog.debug("inserted node: " + node.getPrototype() + "/" +
                                node.getID());
                    }
                } else if (nstate == Node.MODIFIED) {
                    // only mark DbMapping as dirty if updateNode returns true
                    if (nmgr.updateNode(nmgr.db, txn, node)) {
                        dirtyDbMappings.add(node.getDbMapping());
                    }
                    node.setState(Node.CLEAN);

                    // update node with nodemanager cache
                    nmgr.registerNode(node);

                    if (hasListeners) {
                        updatedNodes.add(node);
                    }

                    updated++;
                    if (eventLog.isDebugEnabled()) {
                        eventLog.debug("updated node: " + node.getPrototype() + "/" +
                                node.getID());
                    }
                } else if (nstate == Node.DELETED) {
                    nmgr.deleteNode(nmgr.db, txn, node);
                    dirtyDbMappings.add(node.getDbMapping());

                    // remove node from nodemanager cache
                    nmgr.evictNode(node);

                    if (hasListeners) {
                        deletedNodes.add(node);
                    }

                    deleted++;
                    if (eventLog.isDebugEnabled()) {
                        eventLog.debug("removed node: " + node.getPrototype() + "/" +
                                node.getID());
                    }
                }

                node.clearWriteLock();
            }

            // set last data change times in db-mappings
            // long now = System.currentTimeMillis();
            for (Iterator i = dirtyDbMappings.iterator(); i.hasNext(); ) {
                DbMapping dbm = (DbMapping) i.next();
                if (dbm != null) {
                    dbm.setLastDataChange();
                }
            }
        }

        long now = System.currentTimeMillis();

        if (!parentNodes.isEmpty()) {
            // set last subnode change times in parent nodes
            for (Iterator i = parentNodes.iterator(); i.hasNext(); ) {
                Node node = (Node) i.next();
                node.markSubnodesChanged();
                if (hasListeners) {
                    modifiedParentNodes.add(node);
                }
            }
        }

        if (hasListeners) {
            nmgr.fireNodeChangeEvent(insertedNodes, updatedNodes,
                                     deletedNodes, modifiedParentNodes);
        }

        // clear the node collections
        recycle();

        if (active) {
            active = false;
            nmgr.db.commitTransaction(txn);
            txn = null;
        }

        StringBuffer msg = new StringBuffer(tname).append(" done in ")
                .append(now - tstart).append(" millis");
        if(inserted + updated + deleted > 0) {
            msg.append(" [+")
                    .append(inserted).append(", ~")
                    .append(updated).append(", -")
                    .append(deleted).append("]");
        }
        nmgr.app.logAccess(msg.toString());

        // unset transaction name
        tname = null;
    }

    /**
     * Abort the current transaction, rolling back all changes made.
     */
    public synchronized void abort() {
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
            node.markSubnodesChanged();
        }

        // clear the node collections
        recycle();
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

        // unset transaction name
        tname = null;
    }

    /**
     * Kill this transaction thread. Used as last measure only.
     */
    public synchronized void kill() {

        killed = true;
        thread.interrupt();

        // Interrupt the thread if it has not noticed the flag (e.g. because it is busy
        // reading from a network socket).
        if (thread.isAlive()) {
            thread.interrupt();
            try {
                thread.join(1000);
            } catch (InterruptedException ir) {
                // interrupted by other thread
            }
        }

        if (thread.isAlive() && "true".equals(nmgr.app.getProperty("requestTimeoutStop"))) {
            // still running - check if we ought to stop() it
            try {
                Thread.sleep(2000);
                if (thread.isAlive()) {
                    // thread is still running, pull emergency break
                    nmgr.app.logEvent("Stopping Thread for Transactor " + this);
                    thread.stop();
                }
            } catch (InterruptedException ir) {
                // interrupted by other thread
            }
        }
    }

    /**
     * Closes all open JDBC connections
     */
    public void closeConnections() {
        if (sqlConnections != null) {
            for (Iterator i = sqlConnections.values().iterator(); i.hasNext();) {
                try {
                    Connection con = (Connection) i.next();

                    con.close();
                    nmgr.app.logEvent("Closing DB connection: " + con);
                } catch (Exception ignore) {
                    // exception closing db connection, ignore
                }
            }

            sqlConnections.clear();
        }
    }

    /**
     * Clear collections and throw them away. They may have grown large,
     * so the benefit of keeping them (less GC) needs to be weighted against
     * the potential increas in memory usage.
     */
    private synchronized void recycle() {
        // clear the node collections to ease garbage collection
        dirtyNodes.clear();
        cleanNodes.clear();
        parentNodes.clear();
        testedConnections.clear();
    }

    /**
     * Return the name of the current transaction. This is usually the request
     * path for the underlying HTTP request.
     */
    public String getTransactionName() {
        return tname;
    }

    /**
     * Return a string representation of this Transactor thread
     *
     * @return ...
     */
    public String toString() {
        return "Transactor[" + tname + "]";
    }
}
