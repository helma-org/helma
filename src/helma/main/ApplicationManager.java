// ApplicationManager.java
// Copyright (c) Hannes Wallnöfer 1998-2000
 
package helma.main;

import java.util.Hashtable;
import java.util.Enumeration;
import java.io.*;
import java.lang.reflect.*;
import java.rmi.*;
import java.rmi.server.*;
import helma.framework.*;
import helma.framework.core.*;
import helma.objectmodel.*;
import helma.servlet.*;
import helma.util.SystemProperties;
import Acme.Serve.*;
import javax.servlet.Servlet;


/**
 * This class is responsible for starting and stopping Helma applications.
 */
 
public class ApplicationManager {

    private Hashtable applications;
    private int port;
    private File hopHome;
    private SystemProperties props;
    private Server server;
    private long lastModified;
    EmbeddedTomcat tomcat;

    public ApplicationManager (int port, File hopHome, SystemProperties props, Server server) {
	this.port = port;
	this.hopHome = hopHome;
	this.props = props;
	this.server = server;
	applications = new Hashtable ();
	lastModified = 0;
	/*  tomcat = new EmbeddedTomcat();
	tomcat.setPath("/Users/hannes/Desktop/jakarta-tomcat-4.0.3/test");
	try {
	    tomcat.startTomcat();
	} catch (Exception x) {
	    System.err.println ("Error starting Tomcat: "+x);
	}  */
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
	        Server.getLogger().log ("Error starting applications: "+mx);
	    }

	    lastModified = System.currentTimeMillis ();
	}
    }

    void start (String appName) {
	Server.getLogger().log ("Building application "+appName);
	try {
	    Application app = new Application (appName, hopHome, Server.sysProps, Server.dbProps);
	    applications.put (appName, app);
	    // if we're running with the embedded web server, set app base uri to /appname
	    if (server.websrv != null && !"base".equalsIgnoreCase (appName))
	        app.setBaseURI ("/"+java.net.URLEncoder.encode (appName));
	    // the application is started later in the register method, when it's bound
	    app.init ();
	} catch (Exception x) {
	    Server.getLogger().log ("Error creating application "+appName+": "+x);
	    x.printStackTrace ();
	}
    }

    void stop (String appName) {
	Server.getLogger().log ("Stopping application "+appName);
	try {
	    Application app = (Application) applications.get (appName);
	    if (server.websrv == null) {
	        Naming.unbind ("//:"+port+"/"+appName);
	    } else {
	        // server.websrv.removeServlet ("/"+appName+"/");
	        server.websrv.removeServlet ("/"+appName+"/*");
	    }
	    app.stop ();
	    Server.getLogger().log ("Unregistered application "+appName);
	} catch (Exception x) {
	    Server.getLogger().log ("Couldn't unregister app: "+x);
	}
	applications.remove (appName);
    }

    void register (String appName) {
	try {
	    Server.getLogger().log ("Binding application "+appName);
	    Application app = (Application) applications.get (appName);
	    if (server.websrv == null) {
	        Naming.rebind ("//:"+port+"/"+appName, app);
	    } else {
	        boolean isRoot = "base".equalsIgnoreCase (appName);
	        EmbeddedServletClient servlet = new EmbeddedServletClient (appName, isRoot);
	        if (isRoot)
	            server.websrv.setDefaultServlet (servlet);
	        else {
	            server.websrv.addServlet ("/"+appName+"/*", servlet);
	        }
	        // tomcat.addApplication (appName);
	    }
	    app.start ();
	} catch (Exception x) {
	    Server.getLogger().log ("Couldn't register and start app: "+x);
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
	        Server.getLogger().log("Serving static content from "+staticContent.getAbsolutePath());
	        AcmeFileServlet fsrv = new AcmeFileServlet (staticContent);
	        server.websrv.addServlet ("/static/", fsrv);
	        server.websrv.addServlet ("/static/*", fsrv);
	    }
	    lastModified = System.currentTimeMillis ();
	} catch (Exception mx) {
	    Server.getLogger().log ("Error starting applications: "+mx);
	    mx.printStackTrace ();
	}
    }

    /**
     *  Get an array containing all currently running applications.
     */
    public Object[] getApplications () {
	return applications.values ().toArray ();
    }

    /**
    *  Get an application by name.
    */
    public Application getApplication(String name)	{
	return (Application)applications.get(name);
    }

}
