// IReplicationListener.java
// Copyright (c) Hannes Wallnöfer 2002

package helma.objectmodel.db;

import java.rmi.*;
import java.util.Vector;

/**
 * RMI interface for an application. Currently only execute is used and supported.
 */

public interface IReplicationListener extends Remote {

    /**
     * Update HopObjects in this application's cache. This is used to replicate
     * application caches in a distributed app environment
     */
    public void replicateCache (Vector add, Vector delete) throws RemoteException;

}
