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
// import Acme.Serve.*;
import org.mortbay.http.*;
import org.mortbay.http.handler.*;
import org.mortbay.jetty.servlet.*;
import org.mortbay.util.*;
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
	            } else if (server.http != null) {
	                // check if application should be remounted at a
	                // different location on embedded web server
	                String oldMountpoint = mountpoints.getProperty (appName);
	                String mountpoint = getMountpoint (appName);
	                String pattern = getPathPattern (mountpoint);
	                    if (!pattern.equals (oldMountpoint)) {
	                    Server.getLogger().log("Moving application "+appName+" from "+oldMountpoint+" to "+pattern);
	                    HandlerContext oldContext = server.http.getContext (null, oldMountpoint);
	                    if (oldContext != null) {
	                        oldContext.stop ();
	                        oldContext.destroy ();
	                    }
	                    Application app = (Application) applications.get (appName);
	                    app.setBaseURI (mountpoint);
	                    ServletHandlerContext context = new ServletHandlerContext (server.http, pattern);
	                    server.http.addContext (null, context);
	                    ServletHolder holder = context.addServlet (appName, "/*", "helma.servlet.EmbeddedServletClient");
	                    holder.setInitParameter ("application", appName);
	                    holder.setInitParameter ("mountpoint", mountpoint);
	                    // holder.start ();
	                    context.start ();
	                    mountpoints.setProperty (appName, pattern);
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
	    if (server.http == null) {
	        Naming.unbind ("//:"+port+"/"+appName);
	    } else {
	        String mountpoint = mountpoints.getProperty (appName);
	        HandlerContext context = server.http.getContext (null, mountpoint);
	        if (context != null) {
	            context.stop ();
	            context.destroy ();
	        }
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
	    if (server.http == null) {
	        Naming.rebind ("//:"+port+"/"+appName, app);
	    } else {
	        String mountpoint = getMountpoint (appName);
	        // set application URL prefix
	        app.setBaseURI (mountpoint);
	        String pattern = getPathPattern (mountpoint);
	        ServletHandlerContext context = new ServletHandlerContext (server.http, pattern);
	        server.http.addContext (null, context);
	        ServletHolder holder = context.addServlet (appName, "/*", "helma.servlet.EmbeddedServletClient");
	        holder.setInitParameter ("application", appName);
	        holder.setInitParameter ("mountpoint", mountpoint);
	        // holder.start ();
	        context.start ();
	        mountpoints.setProperty (appName, pattern);
	    }
	    app.start ();
	} catch (Exception x) {
	    Server.getLogger().log ("Couldn't register and start app: "+x);
	    x.printStackTrace ();
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
	    if (server.http != null) {
	        // add handler for static files.
	        File staticContent = new File (server.getHopHome(), "static");
	        Server.getLogger().log("Serving static content from "+staticContent.getAbsolutePath());
	        HandlerContext context = server.http.addContext ("/static/*");
	        context.setResourceBase (staticContent.getAbsolutePath());
	        context.setServingResources (true);
	        context.start ();
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

    private String getMountpoint (String appName) {
	String mountpoint = props.getProperty (appName+".mountpoint");
	if (mountpoint == null)
	    return "/"+URLEncoder.encode(appName);
	mountpoint = mountpoint.trim ();
	if ("".equals (mountpoint))
	    return "/";
	else if (!mountpoint.startsWith ("/"))
	    return "/"+mountpoint;
	return mountpoint;
    }

    private String getPathPattern (String mountpoint) {
	if ("/".equals (mountpoint))
	    return "/";
	if (!mountpoint.endsWith ("/"))
	    return mountpoint+"/*";
	return mountpoint+"*";
    }

}
