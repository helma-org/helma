// IRemoteApp.java
// Copyright (c) Hannes Wallnöfer 1998-2000

package helma.framework;

import java.rmi.*;
import java.util.Vector;

/**
 * RMI interface for an application. Currently only execute is used and supported.
 */

public interface IRemoteApp extends Remote {

    public ResponseTrans execute (RequestTrans param) throws RemoteException;

    public void ping () throws RemoteException;
 
  }
