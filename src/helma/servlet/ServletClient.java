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
	host =  init.getInitParameter ("host");
	if (host == null)
	    host = "localhost";
	String portstr = init.getInitParameter ("port");
	port =  portstr == null ? 5055 : Integer.parseInt (portstr);
	hopUrl = "//" + host + ":" + port + "/";
	if (appName == null)
	    throw new ServletException ("Application name not specified for helma.servlet.ServletClient");
    }

    public void destroy () {
	if (app != null) {
	    app = null;
	}
    }


    ResponseTrans execute (RequestTrans req) throws Exception {
	if (app == null)
	    initApp ();
	try {
	    return app.execute (req);
	} catch (Exception x) {
	    initApp ();
	    return app.execute (req);
	}
    }

    synchronized void initApp () throws Exception {
	app = (IRemoteApp) Naming.lookup (hopUrl + appName);
    }


}


