// MultiServletClient.java
// Copyright (c) Hannes Wallnöfer 2001


package helma.servlet;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.Hashtable;
import helma.framework.*;

/**
 * This is the HOP servlet adapter. This class communicates with any
 * Hop application on a given Hop server, extracting the application name
 * from the request path.
 */
 
public class MultiServletClient extends AbstractServletClient {

    private Hashtable apps;

    public void init (ServletConfig init) throws ServletException {
	super.init (init);
	apps = new Hashtable ();
	host =  init.getInitParameter ("host");
	if (host == null)
	    host = "localhost";
	String portstr = init.getInitParameter ("port");
	port =  portstr == null ? 5055 : Integer.parseInt (portstr);
	hopUrl = "//" + host + ":" + port + "/";
    }

    public void destroy () {
	if (apps != null) {
	    apps.clear ();
	    apps = null;
	}
    }

    ResponseTrans execute (RequestTrans req, String reqPath) throws Exception {
	String appId = getAppID (reqPath);
	IRemoteApp app = getApp (appId);
	req.path = getRequestPath (reqPath);
	try {
	    return app.execute (req);
	} catch (Exception x) {
	    invalidateApp (appId);
	    app = getApp (appId);
	    return app.execute (req);
	}
    }

    IRemoteApp getApp (String appId) throws Exception {
	IRemoteApp app = (IRemoteApp) apps.get (appId);
	if (app != null)
	    return app;
	app = (IRemoteApp) Naming.lookup (hopUrl + appId);
	apps.put (appId, app);
	return app;
    }

    void invalidateApp (String appId) {
	apps.remove (appId);
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

	// advance to start of path, eating up any slashes
	while ((st < len) && (val[st] <= ' ' || val[st] == '/'))
	    st++;

	// advance until slash ending the first path element
	while (st < len && val[st] != '/')
	    st++;
	if (st < len && val[st] == '/')
	    st++;

	// eat away spaces and slashes at end of path
	while ((st < len) && (val[len - 1] <= ' ' || val[len - 1] == '/'))
	    len--;

	return ((st > 0) || (len < val.length)) ? new String (val, st, len-st) : path;
    }

    // for testing
      public static void main (String args[]) {
	MultiServletClient client = new MultiServletClient ();
	// String path = "///appname/do/it/for/me///";
	String path = "appname";
	System.out.println (client.getAppID (path));
	System.out.println (client.getRequestPath (path));
      }

}

