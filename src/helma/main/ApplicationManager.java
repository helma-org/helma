// ApplicationManager.java
// Copyright (c) Hannes Wallnöfer 1998-2000
 
package helma.main;

import java.util.*;
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
import org.mortbay.http.*;
import org.mortbay.http.handler.*;
import org.mortbay.jetty.servlet.*;
import org.mortbay.util.*;
import javax.servlet.Servlet;
import org.apache.xmlrpc.XmlRpcHandler;


/**
 * This class is responsible for starting and stopping Helma applications.
 */

public class ApplicationManager implements XmlRpcHandler {

    private Hashtable applications;
    private Hashtable xmlrpcHandlers;
    private Properties mountpoints;
    private int port;
    private File hopHome;
    private SystemProperties props;
    private Server server;
    private long lastModified;

    public ApplicationManager (int port, File hopHome, SystemProperties props, Server server) {
	this.port = port;
	this.hopHome = hopHome;
	this.props = props;
	this.server = server;
	applications = new Hashtable ();
	xmlrpcHandlers = new Hashtable ();
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
	                    HttpContext oldContext = server.http.getContext (null, oldMountpoint);
	                    if (oldContext != null) {
	                        // oldContext.setContextPath(pattern);
	                        oldContext.stop ();
	                        oldContext.destroy ();
	                    }
	                    Application app = (Application) applications.get (appName);
	                    if (!app.hasExplicitBaseURI())
	                        app.setBaseURI (mountpoint);
	                    ServletHttpContext context = new ServletHttpContext ();
	                    context.setContextPath(pattern);
	                    context.addHandler(new ContentEncodingHandler());
	                    server.http.addContext (context);
	                    ServletHolder holder = context.addServlet (appName, "/*", "helma.servlet.EmbeddedServletClient");
	                    holder.setInitParameter ("application", appName);
	                    holder.setInitParameter ("mountpoint", mountpoint);
	                    String cookieDomain = props.getProperty (appName+".cookieDomain");
	                    if (cookieDomain != null)
	                        holder.setInitParameter ("cookieDomain", cookieDomain);
	                    String uploadLimit = props.getProperty (appName+".uploadLimit");
	                    if (uploadLimit != null)
	                        holder.setInitParameter ("uploadLimit", uploadLimit);
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
	    // check if application and db dirs are set, otherwise go with
	    // the defaults, passing null dirs to the constructor.
	    String appDirName = props.getProperty (appName+".appdir");
	    File appDir = appDirName == null ? null : new File (appDirName);
	    String dbDirName = props.getProperty (appName+".dbdir");
	    File dbDir = dbDirName == null ? null : new File (dbDirName);
	    // create the application instance
	    Application app = new Application (appName, server, appDir, dbDir);
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
	    // unbind from RMI server
	    if (port > 0) {
	        Naming.unbind ("//:"+port+"/"+appName);
	    }
	    // unbind from Jetty HTTP server
	    if (server.http != null) {
	        String mountpoint = mountpoints.getProperty (appName);
	        HttpContext context = server.http.getContext (null, mountpoint);
	        if (context != null) {
	            context.stop ();
	            context.destroy ();
	        }
	    }
	    // unregister as XML-RPC handler
	    xmlrpcHandlers.remove (app.getXmlRpcHandlerName());
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
	    // bind to RMI server
	    if (port > 0) {
	        Naming.rebind ("//:"+port+"/"+appName, new RemoteApplication (app));
	    }
	    // bind to Jetty HTTP server
	    if (server.http != null) {
	        String mountpoint = getMountpoint (appName);
	        // if using embedded webserver (not AJP) set application URL prefix
	        if (!app.hasExplicitBaseURI ())
	            app.setBaseURI (mountpoint);
	        String pattern = getPathPattern (mountpoint);
	        ServletHttpContext context = new ServletHttpContext ();
	        context.setContextPath(pattern);
	        context.addHandler(new ContentEncodingHandler());
	        server.http.addContext (context);
	        ServletHolder holder = context.addServlet (appName, "/*", "helma.servlet.EmbeddedServletClient");
	        holder.setInitParameter ("application", appName);
	        holder.setInitParameter ("mountpoint", mountpoint);
	        String cookieDomain = props.getProperty (appName+".cookieDomain");
	        if (cookieDomain != null)
	            holder.setInitParameter ("cookieDomain", cookieDomain);
	        String uploadLimit = props.getProperty (appName+".uploadLimit");
	        if (uploadLimit != null)
	            holder.setInitParameter ("uploadLimit", uploadLimit);
	        String debug = props.getProperty (appName+".debug");
	        if (debug != null)
	            holder.setInitParameter ("debug", debug);
	        // holder.start ();
	        context.start ();
	        mountpoints.setProperty (appName, pattern);
	    }
	    // register as XML-RPC handler
	    xmlrpcHandlers.put (app.getXmlRpcHandlerName(), app);
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
	        HttpContext context = server.http.addContext ("/static/*");
	        context.setResourceBase (staticContent.getAbsolutePath());
	        ResourceHandler handler = new ResourceHandler();
	        context.addHandler(handler);
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

    /**
     * Implements org.apache.xmlrpc.XmlRpcHandler.execute()
     */
    public Object execute (String method, Vector params) throws Exception {
	int dot = method.indexOf (".");
	if (dot == -1)
	    throw new Exception ("Method name \""+method+"\" does not specify a handler application");
	if (dot == 0 || dot == method.length()-1)
	    throw new Exception ("\""+method+"\" is not a valid XML-RPC method name");
	String handler = method.substring (0, dot);
	String method2 = method.substring (dot+1);
	Application app = (Application) xmlrpcHandlers.get (handler);
	if (app == null)
	    throw new Exception ("Handler \""+handler+"\" not found for "+method);
	return app.executeXmlRpc (method2, params);
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
