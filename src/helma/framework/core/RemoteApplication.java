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

package helma.framework.core;

import helma.framework.*;
import helma.objectmodel.db.*;
import java.rmi.*;
import java.rmi.server.*;
import java.util.Vector;

/**
 * Proxy class for Aplication that listens to requests via RMI.
 */
public class RemoteApplication extends UnicastRemoteObject implements IRemoteApp,
                                                                      IReplicationListener {
    Application app;

    /**
     * Creates a new RemoteApplication object.
     *
     * @param app ...
     *
     * @throws RemoteException ...
     */
    public RemoteApplication(Application app) throws RemoteException {
        this.app = app;
    }

    /**
     *  ping method to let clients know if the server is reachable
     */
    public void ping() {
        // do nothing
    }

    /**
     *  Execute a request coming in from a web client.
     */
    public ResponseTrans execute(RequestTrans req) {
        return app.execute(req);
    }

    /**
     * Update HopObjects in this application's cache. This is used to replicate
     * application caches in a distributed app environment
     */
    public void replicateCache(Vector add, Vector delete) {
        if (!"true".equalsIgnoreCase(app.getProperty("allowReplication"))) {
            app.logEvent("Rejecting cache replication event: allowReplication property is not set to true");
            throw new RuntimeException("Replication event rejected: setup does not allow replication.");
        }

        app.nmgr.replicateCache(add, delete);
    }
}
