// ActivatedImageWrapper.java
// Copyright (c) Hannes Wallnöfer 1999-2000
  
package helma.image;

import java.util.*;
import java.rmi.*;
import java.io.*;

/** 
 * RMI interface for accessing images on remote image servers.
 */
 
public interface IRemoteImage extends Remote  {

    public void setFont (String name, int style, int size) throws RemoteException;
    public void setColor (int color) throws RemoteException;
    public void setColor (int r, int g, int b) throws RemoteException;
    public void reduceColors (int colors) throws RemoteException;

    public void drawString (String str, int x, int y) throws RemoteException;
    public void drawRect (int x, int y, int w, int h) throws RemoteException;
    public void drawLine (int x1, int y1, int x2, int y2) throws RemoteException;
    public void fillRect (int x, int y, int w, int h) throws RemoteException;

    public int getWidth () throws RemoteException;
    public int getHeight () throws RemoteException;
    public void crop (int x, int y, int w, int h) throws RemoteException;
    public void resize (int w, int h) throws RemoteException;
    
    public void saveAs (String filename) throws RemoteException;
    public void readFrom (String filename) throws RemoteException;
    
    public byte[] getBytes (String type) throws RemoteException;
    public void setBytes (byte[] bytes, String type) throws RemoteException;

}