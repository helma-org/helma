// Replicator.java
// Copyright (c) Hannes Wallnöfer 2001
 
package helma.objectmodel.db;

import helma.framework.IReplicatedApp;
import java.rmi.*;
import java.util.*;

/**
 * This class replicates the updates of transactions to other applications via RMI
 */
 
public class Replicator implements Runnable {

    Vector urls;
    Vector apps;
    Vector add, delete, currentAdd, currentDelete;
    Thread runner;

    public Replicator () {
	urls = new Vector ();
	apps = new Vector ();
	add = new Vector ();
	delete = new Vector ();
	runner = new Thread (this);
	runner.start ();
    }

    public void addUrl (String url) {
	urls.addElement (url);
    }

    public void addApp (IReplicatedApp app) {
	apps.addElement (app);
    }

    public void run () {
	while (Thread.currentThread () == runner) {
	    if (prepareReplication ()) {
	        for (int i=0; i<urls.size(); i++) {
	            try {
	                IReplicatedApp app = (IReplicatedApp) Naming.lookup ((String) urls.elementAt (i));
	                app.replicateCache (currentAdd, currentDelete);
	            } catch (Exception x) {
	                System.err.println ("ERROR REPLICATING CACHE: "+x);
	            }
	        }
	        for (int i=0; i<apps.size(); i++) {
	            try {
	                IReplicatedApp app = (IReplicatedApp) apps.elementAt (i);
	                app.replicateCache (currentAdd, currentDelete);
	            } catch (Exception x) {
	                System.err.println ("ERROR REPLICATING CACHE: "+x);
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




































































