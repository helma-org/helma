// Server.java
// Copyright (c) Hannes Wallnöfer 1999-2000
 
package helma.image;

import java.awt.*;
import java.util.*;
import java.io.*;
import java.net.URL;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.*;
import java.rmi.server.*;
import java.rmi.registry.*;

/**
 * Implementation of RMI Image Generator. This accepts only connection from localhost.
 */
 
 public class Server extends UnicastRemoteObject implements IRemoteGenerator {

    static int port = 3033;
    ImageGenerator imggen;

    public static void main (String args[]) throws Exception {
	new Server (); 
    }


    public Server () throws Exception {

	imggen = new ImageGenerator ();

	// the following seems not to be necessary after all ...
	// System.setSecurityManager(new RMISecurityManager());

	System.out.println ("Starting server on port "+port);
	LocateRegistry.createRegistry (port);
	try {
	    Naming.bind ("//:"+port+"/server", this);
	} catch (Exception x) {
	    System.out.println ("error binding remote objects: " + x);
	}
	
    }

    public IRemoteImage createPaintableImage (int x, int y) throws RemoteException {
	try {
	    String client = RemoteServer.getClientHost ();
	    if (!InetAddress.getLocalHost ().equals (InetAddress.getByName (client)))
	        throw new RemoteException ("Access Denied");
	} catch (ServerNotActiveException ignore) {
	} catch (UnknownHostException ignore) {}
	return new RemoteImage (imggen.createPaintableImage (x, y));
    }
    
    public IRemoteImage createPaintableImage (byte[] bytes) throws RemoteException {
	try {
	    String client = RemoteServer.getClientHost ();
	    if (!InetAddress.getLocalHost ().equals (InetAddress.getByName (client)))
	        throw new RemoteException ("Access Denied");
	} catch (ServerNotActiveException ignore) {
	} catch (UnknownHostException ignore) {}
	return new RemoteImage (imggen.createPaintableImage (bytes));
    }

    public IRemoteImage createPaintableImage (String url) throws RemoteException {
	try {
	    String client = RemoteServer.getClientHost ();
	    if (!InetAddress.getLocalHost ().equals (InetAddress.getByName (client)))
	        throw new RemoteException ("Access Denied");
	} catch (ServerNotActiveException ignore) {
	} catch (UnknownHostException ignore) {}
	return new RemoteImage (imggen.createPaintableImage (url));
    }

    public IRemoteImage createImage (byte[] bytes) throws RemoteException {
	try {
	    String client = RemoteServer.getClientHost ();
	    if (!InetAddress.getLocalHost ().equals (InetAddress.getByName (client)))
	        throw new RemoteException ("Access Denied");
	} catch (ServerNotActiveException ignore) {
	} catch (UnknownHostException ignore) {}
	return new RemoteImage (imggen.createImage (bytes));
    }



}

