// EmbeddedServletClient.java
// Copyright (c) Hannes Wallnöfer,  2002

package helma.servlet;

import javax.servlet.*;
import java.io.*;
import java.util.*;
import helma.framework.*;
import helma.framework.core.Application;
import helma.main.*;
import helma.util.*;

/**
 *  Servlet client that runs a Helma application for the embedded
 *  web server
 */

public final class EmbeddedServletClient extends AbstractServletClient {

    private Application app = null;
    private String appName;

    // tells us whether the application is mounted as root or by its name
    // depending on this we know whether we have to transform the request path
    boolean root;

    public EmbeddedServletClient () {
	super ();
	}

    public EmbeddedServletClient (String appName, boolean isRoot) {
	this.appName = appName;
	this.root = isRoot;
    }

    public void init (ServletConfig init) throws ServletException {
	super.init (init);
	String app = init.getInitParameter ("application");
	if (app != null)
	    appName = app;
    }

    IRemoteApp getApp (String appID) {
	if (app == null)
	    app = Server.getServer().getApplication (appName);
	return app;
    }


    void invalidateApp (String appID) {
	// do nothing
    }

    String getAppID (String path) {
	return appName;
    }

    String getRequestPath (String path) {
	if (path == null)
	    return "";
	if (root)
	    return trim (path);
	int appInPath = path.indexOf (appName);
	if (appInPath > 0)
	    return trim (path.substring (appInPath+appName.length()));
	else
	    return trim (path);
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

}



