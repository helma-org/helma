// ApplicationManager.java
// Copyright (c) Hannes Wallnöfer 1998-2000
 
package helma.main;

import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Properties;
import java.io.*;
import java.lang.reflect.*;
import java.rmi.*;
import java.rmi.server.*;
import java.net.URLEncoder;
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
    private Properties mountpoints;
    private int port;
    private File hopHome;
    private SystemProperties props;
    private Server server;
    private long lastModified;
    // EmbeddedTomcat tomcat;

    public ApplicationManager (int port, File hopHome, SystemProperties props, Server server) {
	this.port = port;
	this.hopHome = hopHome;
	this.props = props;
	this.server = server;
	applications = new Hashtable ();
	mountpoints = new Properties ();
	lastModified = 0;
    }


    // regularely check applications property file to create and start new applications
    protected void checkForChanges () {
	if (props.lastModified () > lastModified) {
	    try {
	        for (Enumeration e = props.keys(); e.hasMoreElements (); ) {
	            String appName = (String) e.nextElement ();
	            if (appName.indexOf (".") == -1 && applications.get (appName) == null) {
	                start (appName);
	                register (appName);
	            }
	        }
	        // then stop deleted ones
	        for (Enumeration e = applications.keys(); e.hasMoreElements (); ) {
	            String appName = (String) e.nextElement ();
	            // check if application has been removed and should be stopped
	            if (!props.containsKey (appName)) {
	                stop (appName);
	            } else if (server.websrv != null) {
	                // check if application should be remounted at a
	                // different location on embedded web server
	                String oldMountpoint = mountpoints.getProperty (appName);
	                String mountpoint = props.getProperty (appName+".mountpoint");
	                    if (mountpoint == null || "".equals (mountpoint.trim()))
	                    mountpoint = "/"+URLEncoder.encode(appName);
	                if (!mountpoint.equals (oldMountpoint)) {
	                    if ("/".equals (oldMountpoint))
	                        server.websrv.removeDefaultServlet ();
	                       else
	                        server.websrv.removeServlet (oldMountpoint+"/*");
	                    Application app = (Application) applications.get (appName);
	                    app.setBaseURI (mountpoint);
	                    EmbeddedServletClient servlet = new EmbeddedServletClient (appName, mountpoint);
	                    if ("/".equals (mountpoint))
	                        server.websrv.setDefaultServlet (servlet);
	                    else
	                        server.websrv.addServlet (mountpoint+"/*", servlet);
	                    mountpoints.setProperty (appName, mountpoint);
	                }
	            }
	        }
	    } catch (Exception mx) {
	        Server.getLogger().log ("Error checking applications: "+mx);
	    }

	    lastModified = System.currentTimeMillis ();
	}
    }

    void start (String appName) {
	Server.getLogger().log ("Building application "+appName);
	try {
	    Application app = new Application (appName, hopHome, Server.sysProps, Server.dbProps);
	    applications.put (appName, app);
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
	        String mountpoint = mountpoints.getProperty (appName);
	        if (mountpoint == null || "".equals (mountpoint.trim()))
	            mountpoint = "/"+URLEncoder.encode(appName);
	        if ("/".equals (mountpoint))
	            server.websrv.removeDefaultServlet ();
	        else
	            server.websrv.removeServlet (mountpoint+"/*");
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
	        String mountpoint = props.getProperty (appName+".mountpoint");
	        if (mountpoint == null || "".equals (mountpoint.trim()))
	            mountpoint = "/"+URLEncoder.encode(appName);
	        // set application URL prefix
	        app.setBaseURI (mountpoint);
	        // is the application mounted on the server root?
	        boolean isRoot = "/".equals (mountpoint);
	        EmbeddedServletClient servlet = new EmbeddedServletClient (appName, mountpoint);
	        if (isRoot) {
	            server.websrv.setDefaultServlet (servlet);
	        } else {
	            server.websrv.addServlet (mountpoint+"/*", servlet);
	        }
	        mountpoints.setProperty (appName, mountpoint);
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
	        if (appName.indexOf (".") == -1)
	            start (appName);
	    }
	    for (Enumeration e = props.keys(); e.hasMoreElements (); ) {
	        String appName = (String) e.nextElement ();
	        if (appName.indexOf (".") == -1)
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
