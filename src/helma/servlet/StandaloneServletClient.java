// StandaloneServletClient.java
// Copyright (c) Hannes Wallnöfer,  2001

package helma.servlet;

import javax.servlet.*;
import java.io.*;
import java.util.*;
import helma.framework.*;
import helma.framework.core.Application;
import helma.util.*;

/**
 *  Standalone servlet client that runs a Helma application all by itself
 *  in embedded mode without relying on a central instance of helma.main.Server
 *  to start and manage the application.
 *
 *  StandaloneServletClient takes the following init parameters:
 *     <ul>
 *       <li> application - the application name </li>
 *       <li> appdir - the path of the application home directory </li>
 *       <li> dbdir - the path of the embedded XML data store </li>
 *     </ul>
 */

public final class StandaloneServletClient extends AbstractServletClient {

    private Application app = null;
    private String appName;
    private String appDir;
    private String dbDir;


    public void init (ServletConfig init) throws ServletException {
	super.init (init);
	
	appName = init.getInitParameter ("application");
	if (appName == null || appName.trim().length() == 0)
	    throw new ServletException ("application parameter not specified");

	appDir = init.getInitParameter ("appdir");
	if (appDir == null || appDir.trim().length() == 0)
	    throw new ServletException ("appdir parameter not specified");

	dbDir = init.getInitParameter ("dbdir");
	if (dbDir == null || dbDir.trim().length() == 0)
	    throw new ServletException ("dbdir parameter not specified");
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
	    File appHome = new File (appDir);
	    File dbHome = new File (dbDir);
	    app = new Application (appName, appHome, dbHome);
	    app.init ();
	    app.start ();
	} catch (Exception x) {
	    log ("Error starting Application "+appName+": "+x);
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
	        log ("Error shutting down app "+app.getName()+": ");
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
	String path = "///appname/some/random/path///";
	System.out.println (client.getAppID (path));
	System.out.println (client.getRequestPath (path));
      }

}



