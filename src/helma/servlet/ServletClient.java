// ServletClient.java
// Copyright (c) Hannes Wallnöfer, Raphael Spannocchi 1998-2000

/* Portierung von helma.asp.AspClient auf Servlets */
/* Author: Raphael Spannocchi Datum: 27.11.1998 */

package helma.servlet;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.*;
import helma.framework.*;
import helma.objectmodel.Node;
import helma.util.*;

/**
 * This is the HOP servlet adapter. This class communicates with just
 * one Hop application.
 */
 
public class ServletClient extends AbstractServletClient {
	
    private IRemoteApp app = null;
    private String appName;

    public void init (ServletConfig init) {

	appName = init.getInitParameter ("application");
	if (appName == null)
	    appName = "base";

	super.init (init);
    }

    IRemoteApp getApp (String appID) throws Exception {
	if (app != null)
	    return app;

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
	String path = "///appname/do/it/for/me///";
	System.out.println (client.getAppID (path));
	System.out.println (client.getRequestPath (path));
      }


}



















