// IRemoteGenerator.java
// Copyright (c) Hannes Wallnöfer 1999-2000
  
package helma.image;

import java.util.*;
import java.rmi.*;
import java.io.*;

/**
 * RMI interface for accessing remote image generators.
 */
 
public interface IRemoteGenerator extends Remote  {

    public IRemoteImage createPaintableImage (int w, int h) throws RemoteException;

    public IRemoteImage createPaintableImage (byte src[]) throws RemoteException;

    public IRemoteImage createPaintableImage (String urlstring) throws RemoteException;

    public IRemoteImage createImage (byte src[]) throws RemoteException;

}
