// StandaloneServletClient.java
// Copyright (c) Hannes Wallnöfer,  2001

package helma.servlet;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.util.*;
import helma.framework.*;
import helma.framework.core.Application;
import helma.objectmodel.*;
import helma.util.*;

/**
 * This is a standalone Hop servlet client, running a Hop application by itself.
 */

public final class StandaloneServletClient extends AbstractServletClient {

    private Application app = null;
    private String appName;
    private String serverProps;

    
    public void init (ServletConfig init) throws ServletException {
	super.init (init);
	appName = init.getInitParameter ("application");
	serverProps = init.getInitParameter ("serverprops");
    }

    IRemoteApp getApp (String appID) {
	if (app == null)
	    createApp ();
	return app;
    }

    /**
     * Create the application. Since we are synchronized only here, we
     * do another check if the app already exists and immediately return if it does.
     */
    synchronized void createApp () {
	if (app != null)
	    return;
	try {
	    File propfile = new File (serverProps);
	    File hopHome = new File (propfile.getParent());
	    SystemProperties sysProps = new SystemProperties (propfile.getAbsolutePath());
	    app = new Application (appName, hopHome, sysProps, null);
	    app.init ();
	    app.start ();
	} catch (Exception x) {
	    System.err.println ("Error starting Application "+appName+": "+x);
	    x.printStackTrace ();
	}
    }


    /**
     * The servlet is being destroyed. Close and release the application if
     * it does exist.
     */
    public void destroy () {
	if (app != null) {
	    try {
	        app.stop ();
	    } catch (Exception x) {
	        System.err.println ("Error shutting down app "+app.getName()+": ");
	        x.printStackTrace ();
	    }
	}
	app = null;
    }

    void invalidateApp (String appID) {
	// app = null;
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



