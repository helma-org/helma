// IReplicatedApp.java
// Copyright (c) Hannes Wallnöfer 2001

package helma.framework;

import helma.objectmodel.Key;
import helma.objectmodel.INode;
import java.rmi.*;
import java.util.Vector;

/**
 * RMI interface for an application that is able to replicate it's node cache.
 */

public interface IReplicatedApp extends Remote {

    public void replicateCache (Vector add, Vector delete) throws RemoteException;

}
