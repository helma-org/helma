// MultiServletClient.java
// Copyright (c) Hannes Wallnöfer 2001


package helma.servlet;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.*;
import helma.framework.IRemoteApp;

/**
 * This is the HOP servlet adapter. This class communicates with any
 * Hop application on a given Hop server, extracting the application name
 * from the request path.
 */
 
public class MultiServletClient extends AbstractServletClient {
	
    private HashMap apps = null;

    public void init (ServletConfig init) {
	apps = new HashMap ();
	super.init (init);
    }

    IRemoteApp getApp (String appID) throws Exception {
	IRemoteApp retval = (IRemoteApp) apps.get (appID);
	if (retval != null) {
	    return retval;
	}
	retval = (IRemoteApp) Naming.lookup (hopUrl + appID);
	apps.put (appID, retval);
	return retval;
    }

    void invalidateApp (String appID) {
	apps.remove (appID);
    }

    String getAppID (String path) {
	if (path == null)
	    throw new RuntimeException ("Invalid request path: "+path);

	char[] val = path.toCharArray ();
	int len = val.length;
	int st = 0;

	// advance to start of path
	while ((st < len) && (val[st] <= ' ' || val[st] == '/'))
	    st++;

	// eat characters of first path element
	int end = st;
	while (end < len && val[end] != '/' && val[end] > 20)
	    end++;

	return new String (val, st, end -st);
    }

    String getRequestPath (String path) {
	if (path == null)
	    return "";

	char[] val = path.toCharArray ();
	int len = val.length;
	int st = 0;

	// advance to start of path
	while ((st < len) && (val[st] <= ' ' || val[st] == '/'))
	    st++;

	// eat characters of first path element
	while (st < len && val[st] != '/')
	    st++;
	if (st < len && val[st] == '/')
	    st++;

	// eat away noise at end of path
	while ((st < len) && (val[len - 1] <= ' ' || val[len - 1] == '/'))
	    len--;

	return ((st > 0) || (len < val.length)) ? new String (val, st, len-st) : path;
    }

    // for testing
      public static void main (String args[]) {
	AbstractServletClient client = new MultiServletClient ();
	// String path = "///appname/do/it/for/me///";
	String path = "appname";
	System.out.println (client.getAppID (path));
	System.out.println (client.getRequestPath (path));
      }

}



















