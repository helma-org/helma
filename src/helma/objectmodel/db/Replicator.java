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

import java.rmi.Naming;
import java.util.Vector;

/**
 * This class replicates the updates of transactions to other applications via RMI
 */
public class Replicator implements Runnable {
    Vector urls;
    Vector add;
    Vector delete;
    Vector currentAdd;
    Vector currentDelete;
    Thread runner;
    NodeManager nmgr;

    /**
     * Creates a new Replicator object.
     *
     * @param nmgr ...
     */
    public Replicator(NodeManager nmgr) {
        urls = new Vector();
        add = new Vector();
        delete = new Vector();
        this.nmgr = nmgr;
        runner = new Thread(this);
        runner.start();
    }

    /**
     *
     *
     * @param url ...
     */
    public void addUrl(String url) {
        urls.addElement(url);

        if (nmgr.logReplication) {
            nmgr.app.logEvent("Adding replication listener: " + url);
        }
    }

    /**
     *
     */
    public void run() {
        while (Thread.currentThread() == runner) {
            if (prepareReplication()) {
                for (int i = 0; i < urls.size(); i++) {
                    try {
                        String url = (String) urls.elementAt(i);
                        IReplicationListener listener = (IReplicationListener) Naming.lookup(url);

                        if (listener == null) {
                            throw new NullPointerException("Replication listener not bound for URL "+url);
                        }

                        listener.replicateCache(currentAdd, currentDelete);

                        if (nmgr.logReplication) {
                            nmgr.app.logEvent("Sent cache replication event: " +
                                              currentAdd.size() + " added, " + currentDelete.size() +
                                              " deleted");
                        }
                    } catch (Exception x) {
                        nmgr.app.logEvent("Error sending cache replication event: " + x);
                        if (nmgr.app.debug()) {
                            x.printStackTrace();
                        }
                    }
                }
            }

            try {
                if (runner != null) {
                    Thread.sleep(1000L);
                }
            } catch (InterruptedException ir) {
                runner = null;
            }
        }
    }

    /**
     *
     *
     * @param n ...
     */
    public synchronized void addNewNode(Node n) {
        add.addElement(n);
    }

    /**
     *
     *
     * @param n ...
     */
    public synchronized void addModifiedNode(Node n) {
        add.addElement(n);
    }

    /**
     *
     *
     * @param n ...
     */
    public synchronized void addDeletedNode(Node n) {
        delete.addElement(n);
    }

    private synchronized boolean prepareReplication() {
        if ((add.size() == 0) && (delete.size() == 0)) {
            return false;
        }

        currentAdd = add;
        currentDelete = delete;
        add = new Vector();
        delete = new Vector();

        return true;
    }
}
