// Transactor.java
// Copyright (c) Hannes Wallnöfer 1998-2000

package helma.objectmodel.db;

import java.io.*;
import java.util.*;
import java.sql.*;
import helma.objectmodel.*;
import helma.util.Timer;
import helma.framework.TimeoutException;
import com.sleepycat.db.*;

/**
 * A subclass of thread that keeps track of changed nodes and triggers 
 * changes in the database when a transaction is commited.
 */

public class Transactor extends Thread {

    NodeManager nmgr;

    // List of nodes to be updated
    private HashMap nodes;
    // List of visited clean nodes
    private HashMap cleannodes;
    // Is a transaction in progress?
    private volatile boolean active;
    private volatile boolean killed;

    // Transaction for the embedded database
    protected DbTxn txn;
    // Transactions for SQL data sources
    protected HashMap sqlCon;

    public Timer timer;
    // when did the current transaction start?
    private long tstart;
    // a name to log the transaction. For HTTP transactions this is the rerquest path
    private String tname;


    public Transactor (Runnable runnable, NodeManager nmgr) {
	super (Server.txgroup, runnable, "Transactor");
	this.nmgr = nmgr;
	nodes = new HashMap ();
	cleannodes = new HashMap ();
	sqlCon = new HashMap ();
	active = false;
	killed = false;
	timer = new Timer();
    }

    public void visitNode (Node node) {
	if (node != null) {
	    Key key = node.getKey ();
	    if (!nodes.containsKey (key)) {
	        nodes.put (key, node);
	    }
	}
    }

    public void dropNode (Node node) {
	if (node != null) {
	    Key key = node.getKey ();
	    nodes.remove (key);
	}
    }

    public void visitCleanNode (Node node) {
	if (node != null) {
	    Key key = node.getKey ();
	    if (!cleannodes.containsKey (key)) {
	        cleannodes.put (key, node);
	    }
	}
    }

    public void visitCleanNode (Key key, Node node) {
	if (node != null) {
	    if (!cleannodes.containsKey (key)) {
	        cleannodes.put (key, node);
	    }
	}
    }


    public Node getVisitedNode (Object key) {
	return key == null ? null : (Node) cleannodes.get (key);
    }

    public boolean isActive () {
	return active;
    }

    public void registerConnection (DbSource src, Connection con) {
	sqlCon.put (src, con);
    }

    public Connection getConnection (DbSource src) {
	return (Connection) sqlCon.get (src);
    }

    public synchronized void begin (String tnm) throws Exception {
	if (active) {
	    abort ();
	}
	nodes.clear ();
	cleannodes.clear ();
	txn = nmgr.db.beginTransaction ();
	active = true;
	tstart = System.currentTimeMillis ();
	tname = tnm;
    }

    public synchronized void commit () throws Exception {

	if (killed) {
	    abort ();
	    return;
	}

	int ins = 0, upd = 0, dlt = 0;
	int l = nodes.size ();

	for (Iterator i=nodes.values().iterator(); i.hasNext (); ) {
	    Node node = (Node) i.next ();

	    // update nodes in db
	    int nstate = node.getState ();
	    if (nstate == Node.NEW) {
	        nmgr.registerNode (node); // register node with nodemanager cache
	        nmgr.insertNode (nmgr.db, txn, node);
	        node.setState (Node.CLEAN);
	        ins++;
	        // IServer.getLogger().log ("inserted: "+node.getFullName ());
	    } else if (nstate == Node.MODIFIED) {
	        nmgr.updateNode (nmgr.db, txn, node);
	        node.setState (Node.CLEAN);
	        upd++;
	        IServer.getLogger().log ("updated: "+node.getFullName ());
	    } else if (nstate == Node.DELETED) {
	        // IServer.getLogger().log ("deleted: "+node.getFullName ()+" ("+node.getName ()+")");
	        nmgr.deleteNode (nmgr.db, txn, node);
	        nmgr.evictNode (node);
	        dlt++;
	    } else {
	        // IServer.getLogger().log ("noop: "+node.getFullName ());
	    }
	    node.clearWriteLock ();
	}

	nodes.clear ();
	cleannodes.clear ();
	// sqlCon.clear ();

	if (nmgr.idgen.dirty) {
	    nmgr.db.save (txn, "idgen", nmgr.idgen);
	    nmgr.idgen.dirty = false;
	}

	if (active) {
	    active = false;
	    nmgr.db.commitTransaction (txn);
	    txn = null;
	}

	IServer.getLogger().log (tname+" "+l+" marked, "+ins+" inserted, "+upd+" updated, "+dlt+" deleted in "+(System.currentTimeMillis()-tstart)+" millis");
    }

    public synchronized void abort () throws Exception {

	int l = nodes.size ();
	for (Iterator i=nodes.values().iterator(); i.hasNext(); ) {
	    Node node = (Node) i.next ();
	    // Declare node as invalid, so it won't be used by other threads that want to
	    // write on it and remove it from cache
	    nmgr.evictNode (node);
	    node.clearWriteLock ();
	}
	nodes.clear ();
	cleannodes.clear ();
	for (Iterator i=sqlCon.values().iterator(); i.hasNext(); ) {
	    try {
	        Connection con = (Connection) i.next ();
	        con.close ();
	    } catch (Exception ignore) {}
	}
	sqlCon.clear ();

	if (active) {
	    active = false;
	    nmgr.db.abortTransaction (txn);
	    txn = null;
	}
	IServer.getLogger().log (tname+" aborted after "+(System.currentTimeMillis()-tstart)+" millis");
    }

    public synchronized void kill () {
	killed = true;
	// The thread is told to stop by setting the thread flag in the EcmaScript
	// evaluator, so we can hope that it stops without doing anything else.
	try {
	    join (1000);
	} catch (InterruptedException ir) {
	    Thread.currentThread().interrupt();
	}

	// Interrupt the thread if it has not noticed the flag (e.g. because it is busy
	// reading from a network socket).
	if (isAlive()) {
	    interrupt ();
	    try {
	        join (3000);
	    } catch (InterruptedException ignore) {
	        Thread.currentThread().interrupt();
	    }
	    if (isAlive())
	        // Sorry to be so rude...
	        stop (new TimeoutException());
	}
    }

    public void cleanup () {
	if (sqlCon != null) {
	    for (Iterator i=sqlCon.values().iterator(); i.hasNext(); ) {
	        try {
	            Connection con = (Connection) i.next();
	            con.close ();
	        } catch (Exception ignore) {}
	    }
	    sqlCon.clear ();
	    sqlCon = null;
	}
    }

}





































































































