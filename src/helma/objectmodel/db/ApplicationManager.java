// ApplicationManager.java
// Copyright (c) Hannes Wallnöfer 1998-2000
 
package helma.objectmodel.db;

import java.util.*;
import java.io.*;
import java.lang.reflect.*;
import java.rmi.*;
import java.rmi.server.*;
import helma.framework.*;
import helma.framework.core.*;
import helma.objectmodel.*;
import helma.servlet.*;
import Acme.Serve.*;
import javax.servlet.Servlet;


/**
 * This class is responsible for starting and stopping HOP applications. 
 */
 
public class ApplicationManager {

    private Hashtable applications;
    private int port;
    private File appHome, dbHome;
    private SystemProperties props;
    private Server server;
    private long lastModified;

    public ApplicationManager (int port, File appHome, File dbHome, SystemProperties props, Server server) {
	this.port = port;
	this.appHome = appHome;
	this.dbHome = dbHome;
	this.props = props;
	this.server = server;
	applications = new Hashtable ();
	lastModified = 0;
    }


    // regularely check applications property file to create and start new applications
    protected void checkForChanges () {
	if (props.lastModified () > lastModified) {
	    try {
	        for (Enumeration e = props.keys(); e.hasMoreElements (); ) {
	            String appName = (String) e.nextElement ();
	            if (applications.get (appName) == null) {
	                start (appName);
	                register (appName);
	            }
	        }
	        // then stop deleted ones
	        for (Enumeration e = applications.keys(); e.hasMoreElements (); ) {
	            String appName = (String) e.nextElement ();
	            if (!props.containsKey (appName)) {
	                stop (appName);
	            }
	        }
	    } catch (Exception mx) {
	        IServer.getLogger().log ("Error starting applications: "+mx);
	    }

	    lastModified = System.currentTimeMillis ();
	}
    }


    private void start (String appName) {
	IServer.getLogger().log ("Building application "+appName);
	try {
	    Application app = new Application (appName, dbHome, appHome);
	    applications.put (appName, app);
	    app.start ();
	} catch (Exception x) {
	    IServer.getLogger().log ("Error creating application "+appName+": "+x);
	    x.printStackTrace ();
	}
    }

    private void stop (String appName) {
	IServer.getLogger().log ("Stopping application "+appName);
	try {
	    Application app = (Application) applications.get (appName);
	    if (server.websrv == null) {
	        Naming.unbind ("//:"+port+"/"+appName);
	    } else {
	        server.websrv.removeServlet ("/"+appName+"/");
	        server.websrv.removeServlet ("/"+appName+"/*");
	    }
	    app.stop ();
	    IServer.getLogger().log ("Unregistered application "+appName);
	} catch (Exception x) {
	    IServer.getLogger().log ("Couldn't unregister app: "+x);
	}
	applications.remove (appName);
    }

    private void register (String appName) {
	try {
	    IServer.getLogger().log ("Binding application "+appName);
	    Application app = (Application) applications.get (appName);
	    if (server.websrv == null) {
	        Naming.rebind ("//:"+port+"/"+appName, app);
	    } else {
	        AcmeServletClient servlet = new AcmeServletClient (app);
	        server.websrv.addServlet ("/"+appName+"/", servlet);
	        server.websrv.addServlet ("/"+appName+"/*", servlet);
	    }
	} catch (Exception x) {
	    IServer.getLogger().log ("Couldn't register app: "+x);
	}
    }

    public void startAll () {
	try {
	    for (Enumeration e = props.keys(); e.hasMoreElements (); ) {
	        String appName = (String) e.nextElement ();
	        start (appName);
	    }
	    for (Enumeration e = props.keys(); e.hasMoreElements (); ) {
	        String appName = (String) e.nextElement ();
	        register (appName);
	    }
	    if (server.websrv != null) {
	        File staticContent = new File (server.getHopHome(), "static");
	        IServer.getLogger().log("Serving static content from "+staticContent.getAbsolutePath());
	        AcmeFileServlet fsrv = new AcmeFileServlet (staticContent);
	        server.websrv.addServlet ("/static/", fsrv);
	        server.websrv.addServlet ("/static/*", fsrv);
	    }
	    lastModified = System.currentTimeMillis ();
	} catch (Exception mx) {
	    IServer.getLogger().log ("Error starting applications: "+mx);
	    mx.printStackTrace ();
	}
    }

}
