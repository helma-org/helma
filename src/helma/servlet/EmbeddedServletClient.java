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

    // The path where this servlet is mounted
    String mountpoint;

    public EmbeddedServletClient () {
	super ();
    }


    public void init (ServletConfig init) throws ServletException {
	super.init (init);
	appName = init.getInitParameter ("application");
	if (appName == null)
	    throw new ServletException ("Application name not set in init parameters");
	mountpoint = init.getInitParameter ("mountpoint");
	if (mountpoint == null)
	    mountpoint = "/"+appName;
    }

    ResponseTrans execute (RequestTrans req) throws Exception {
	if (app == null)
	    app = Server.getServer().getApplication (appName);
	return app.execute (req);
    }

}



