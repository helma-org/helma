// ServletClient.java
// Copyright (c) Hannes Wallnöfer, Raphael Spannocchi 1998-2002


package helma.servlet;

import javax.servlet.*;
import java.io.*;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.*;
import helma.framework.*;

/**
 * This is the standard Helma servlet adapter. This class represents a servlet
 * that is dedicated to one Helma application over RMI.
 */
 
public class ServletClient extends AbstractServletClient {
	
    private IRemoteApp app = null;
    private String appName = null;

    public void init (ServletConfig init) throws ServletException {
	super.init (init);
	appName = init.getInitParameter ("application");
    }

    IRemoteApp getApp (String appID) throws Exception {
	if (app != null)
	    return app;
	if (appName == null)
	    throw new ServletException ("Helma application name not specified for helma.servlet.ServletClient");
	app = (IRemoteApp) Naming.lookup (hopUrl + appName);
	return app;
    }

    void invalidateApp (String appID) {
	app = null;
    }

    String getAppID (String path) {
	return appName;
    }

    String getRequestPath (String path) {
	// get request path
	if (path != null)
	    return trim (path);	
	else
	    return "";
    }

    String trim (String str) {
	char[] val = str.toCharArray ();
	int len = val.length;
	int st = 0;

	while ((st < len) && (val[st] <= ' ' || val[st] == '/'))
	    st++;

	while ((st < len) && (val[len - 1] <= ' ' || val[len - 1] == '/'))
	    len--;

	return ((st > 0) || (len < val.length)) ? new String (val, st, len-st) : str;
    }

    // for testing
      public static void main (String args[]) {
	AbstractServletClient client = new ServletClient ();
	String path = "///appname/some/random/path///";
	System.out.println (client.getAppID (path));
	System.out.println (client.getRequestPath (path));
      }


}


