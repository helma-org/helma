// Replicator.java
// Copyright (c) Hannes Wallnöfer 2001
 
package helma.objectmodel.db;

import java.rmi.*;
import java.util.*;

/**
 * This class replicates the updates of transactions to other applications via RMI
 */

public class Replicator implements Runnable {

    Vector urls;
    Vector add, delete, currentAdd, currentDelete;
    Thread runner;
    NodeManager nmgr;

    public Replicator (NodeManager nmgr) {
	urls = new Vector ();
	add = new Vector ();
	delete = new Vector ();
	this.nmgr = nmgr;
	runner = new Thread (this);
	runner.start ();
    }

    public void addUrl (String url) {
	urls.addElement (url);
	if (nmgr.logReplication)
	    nmgr.app.logEvent ("Adding replication listener: "+url);
    }

    public void run () {
	while (Thread.currentThread () == runner) {
	    if (prepareReplication ()) {
	        for (int i=0; i<urls.size(); i++) {
	            try {
	                String url = (String) urls.elementAt (i);
	                IReplicationListener listener = (IReplicationListener) Naming.lookup (url);
	                listener.replicateCache (currentAdd, currentDelete);
	                if (nmgr.logReplication)
	                    nmgr.app.logEvent ("Sent cache replication event: "+add.size()+" added, "+delete.size()+" deleted");
	            } catch (Exception x) {
	                nmgr.app.logEvent ("Error sending cache replication event: "+x);
	            }
	        }
	    }

	    try {
	        if (runner != null)
	            runner.sleep (1000l);
	    } catch (InterruptedException ir) {
	        runner = null;
	    }
	}
    }

    public synchronized void addNewNode (Node n) {
	add.addElement (n);
    }

    public synchronized void addModifiedNode (Node n) {
	add.addElement (n);
    }

    public synchronized void addDeletedNode (Node n) {
	delete.addElement (n);
    }

    private synchronized boolean prepareReplication () {
	if (add.size() == 0 && delete.size() == 0)
	    return false;
	currentAdd = add;
	currentDelete = delete;
	add = new Vector ();
	delete = new Vector ();
	return true;
    }

}




































































