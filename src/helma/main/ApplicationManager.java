/*
 * Helma License Notice
 *
 * The contents of this file are subject to the Helma License
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://adele.helma.org/download/helma/license.txt
 *
 * Copyright 1998-2003 Helma Software. All Rights Reserved.
 *
 * $RCSfile$
 * $Author$
 * $Revision$
 * $Date$
 */

package helma.main;

import helma.framework.*;
import helma.framework.core.*;
import helma.objectmodel.*;
import helma.servlet.*;
import helma.util.SystemProperties;
import helma.util.StringUtils;
import org.apache.xmlrpc.XmlRpcHandler;
import org.mortbay.http.*;
import org.mortbay.http.handler.*;
import org.mortbay.jetty.servlet.*;
import org.mortbay.util.*;
import java.io.*;
import java.lang.reflect.*;
import java.rmi.*;
import java.rmi.server.*;
import java.util.*;
import javax.servlet.Servlet;

/**
 * This class is responsible for starting and stopping Helma applications.
 */
public class ApplicationManager implements XmlRpcHandler {
    private Hashtable descriptors;
    private Hashtable applications;
    private Hashtable xmlrpcHandlers;
    private int port;
    private File hopHome;
    private SystemProperties props;
    private Server server;
    private long lastModified;

    /**
     * Creates a new ApplicationManager object.
     *
     * @param port The RMI port we're binding to
     * @param hopHome The Helma home directory
     * @param props the properties defining the running apps
     * @param server the server instance
     */
    public ApplicationManager(int port, File hopHome, SystemProperties props,
                              Server server) {
        this.port = port;
        this.hopHome = hopHome;
        this.props = props;
        this.server = server;
        descriptors = new Hashtable();
        applications = new Hashtable();
        xmlrpcHandlers = new Hashtable();
        lastModified = 0;
    }

    /**
     * Called regularely check applications property file
     * to create and start new applications.
     */
    protected void checkForChanges() {
        if (props.lastModified() > lastModified) {
            try {
                for (Enumeration e = props.keys(); e.hasMoreElements();) {
                    String appName = (String) e.nextElement();

                    if ((appName.indexOf(".") == -1) &&
                            (applications.get(appName) == null)) {
                        AppDescriptor appDesc = new AppDescriptor(appName);
                        appDesc.start();
                        appDesc.bind();
                    }
                }

                // then stop deleted ones
                for (Enumeration e = descriptors.elements(); e.hasMoreElements();) {
                    AppDescriptor appDesc = (AppDescriptor) e.nextElement();

                    // check if application has been removed and should be stopped
                    if (!props.containsKey(appDesc.appName)) {
                        appDesc.stop();
                    } else if (server.http != null) {
                        // If application continues to run, remount
                        // as the mounting options may have changed.
                        appDesc.unbind();
                        AppDescriptor ndesc = new AppDescriptor(appDesc.appName);
                        ndesc.app = appDesc.app;
                        ndesc.bind();
                        descriptors.put(ndesc.appName, ndesc);

                    }
                }
            } catch (Exception mx) {
                Server.getLogger().log("Error checking applications: " + mx);
            }

            lastModified = System.currentTimeMillis();
        }
    }


    /**
     *  Start an application by name
     */
    public void start(String appName) {
        AppDescriptor desc = new AppDescriptor(appName);
        desc.start();
    }

    /**
     *  Bind an application by name
     */
    public void register(String appName) {
        AppDescriptor desc = (AppDescriptor) descriptors.get(appName);
        if (desc != null) {
            desc.bind();
        }
    }

    /**
     *  Stop an application by name
     */
    public void stop(String appName) {
        AppDescriptor desc = (AppDescriptor) descriptors.get(appName);
        if (desc != null) {
            desc.stop();
        }
    }


    /**
     * Start all applications listed in the properties
     */
    public void startAll() {
        try {
            for (Enumeration e = props.keys(); e.hasMoreElements();) {
                String appName = (String) e.nextElement();

                if (appName.indexOf(".") == -1) {
                    AppDescriptor desc = new AppDescriptor(appName);
                    desc.start();
                }
            }

            for (Enumeration e = descriptors.elements(); e.hasMoreElements();) {
                AppDescriptor appDesc = (AppDescriptor) e.nextElement();
                appDesc.bind();
            }

            lastModified = System.currentTimeMillis();
        } catch (Exception mx) {
            Server.getLogger().log("Error starting applications: " + mx);
            mx.printStackTrace();
        }
    }

    /**
     *  Stop all running applications.
     */
    public void stopAll() {
        for (Enumeration en = descriptors.elements(); en.hasMoreElements();) {
            AppDescriptor appDesc = (AppDescriptor) en.nextElement();

            appDesc.stop();
        }
    }

    /**
     *  Get an array containing all currently running applications.
     */
    public Object[] getApplications() {
        return applications.values().toArray();
    }

    /**
     *  Get an application by name.
     */
    public Application getApplication(String name) {
        return (Application) applications.get(name);
    }

    /**
     * Implements org.apache.xmlrpc.XmlRpcHandler.execute()
     */
    public Object execute(String method, Vector params)
                   throws Exception {
        int dot = method.indexOf(".");

        if (dot == -1) {
            throw new Exception("Method name \"" + method +
                                "\" does not specify a handler application");
        }

        if ((dot == 0) || (dot == (method.length() - 1))) {
            throw new Exception("\"" + method + "\" is not a valid XML-RPC method name");
        }

        String handler = method.substring(0, dot);
        String method2 = method.substring(dot + 1);
        Application app = (Application) xmlrpcHandlers.get(handler);

        if (app == null) {
            throw new Exception("Handler \"" + handler + "\" not found for " + method);
        }

        return app.executeXmlRpc(method2, params);
    }

    private String getMountpoint(String mountpoint) {
        mountpoint = mountpoint.trim();

        if ("".equals(mountpoint)) {
            return "/";
        } else if (!mountpoint.startsWith("/")) {
            return "/" + mountpoint;
        }

        return mountpoint;
    }

    private String joinMountpoint(String prefix, String suffix) {
        if (prefix.endsWith("/") || suffix.startsWith("/")) {
            return prefix+suffix;
        } else {
            return prefix+"/"+suffix;
        }
    }

    private String getPathPattern(String mountpoint) {
        if (!mountpoint.startsWith("/")) {
            mountpoint = "/"+mountpoint;
        }

        if ("/".equals(mountpoint)) {
            return "/";
        }

        if (mountpoint.endsWith("/")) {
            return mountpoint + "*";
        }

        return mountpoint + "/*";
    }

    /**
     *  Inner class that describes an application and its start settings.
     */
    class AppDescriptor {

        Application app;

        String appName;
        File appDir;
        File dbDir;
        String mountpoint;
        String pathPattern;
        String staticDir;
        String staticMountpoint;
        String[] xmlrpcHandlerNames;
        String cookieDomain;
        String uploadLimit;
        String debug;
        boolean encode;

        /**
         *  Creates an AppDescriptor from the properties.
         */
        AppDescriptor(String name) {
            appName = name;
            mountpoint = getMountpoint(props.getProperty(name+".mountpoint",
                                        appName));
            pathPattern = getPathPattern(mountpoint);
            staticDir = props.getProperty(name+".static");
            staticMountpoint = getPathPattern(props.getProperty(name+".staticMountpoint",
                                        joinMountpoint(mountpoint, "static")));
            xmlrpcHandlerNames = StringUtils.split(props.getProperty(name+".xmlrpcHandler"));
            cookieDomain = props.getProperty(name+".cookieDomain");
            uploadLimit = props.getProperty(name+".uploadLimit");
            debug = props.getProperty(name+".debug");
            encode = "true".equalsIgnoreCase(props.getProperty(name +
                                        ".responseEncoding"));
            String appDirName = props.getProperty(name + ".appdir");
            appDir = (appDirName == null) ? null : new File(appDirName);
            String dbDirName = props.getProperty(name + ".dbdir");
            dbDir = (dbDirName == null) ? null : new File(dbDirName);
        }


        void start() {
            Server.getLogger().log("Building application " + appName);

            try {
                // create the application instance
                app = new Application(appName, server, appDir, dbDir);

                // register ourselves
                descriptors.put(appName, this);
                applications.put(appName, app);

                // the application is started later in the register method, when it's bound
                app.init();
                app.start();
            } catch (Exception x) {
                Server.getLogger().log("Error creating application " + appName + ": " + x);
                x.printStackTrace();
            }
        }

        void stop() {
            Server.getLogger().log("Stopping application " + appName);

            // unbind application
            unbind();

            // stop application
            try {
                app.stop();
                Server.getLogger().log("Stopped application " + appName);
            } catch (Exception x) {
                Server.getLogger().log("Couldn't stop app: " + x);
            }

            descriptors.remove(appName);
            applications.remove(appName);
        }

        void bind() {
            try {
                Server.getLogger().log("Binding application " + appName);

                // bind to RMI server
                if (port > 0) {
                    Naming.rebind("//:" + port + "/" + appName, new RemoteApplication(app));
                }

                // bind to Jetty HTTP server
                if (server.http != null) {
                    // if using embedded webserver (not AJP) set application URL prefix
                    if (!app.hasExplicitBaseURI()) {
                        app.setBaseURI(mountpoint);
                    }

                    ServletHttpContext context = new ServletHttpContext();

                    context.setContextPath(pathPattern);
                    server.http.addContext(context);

                    ServletHolder holder = context.addServlet(appName, "/*",
                                                          "helma.servlet.EmbeddedServletClient");

                    holder.setInitParameter("application", appName);
                    // holder.setInitParameter("mountpoint", mountpoint);

                    if (encode) {
                        context.addHandler(new ContentEncodingHandler());
                    }

                    if (cookieDomain != null) {
                        holder.setInitParameter("cookieDomain", cookieDomain);
                    }

                    if (uploadLimit != null) {
                        holder.setInitParameter("uploadLimit", uploadLimit);
                    }

                    if (debug != null) {
                        holder.setInitParameter("debug", debug);
                    }

                    context.start();

                    if (staticDir != null) {

                        File staticContent = new File(staticDir);
                        if (!staticContent.isAbsolute()) {
                            staticContent = new File(server.getHopHome(), staticDir);
                        }

                        Server.getLogger().log("Serving static from " +
                                       staticContent.getAbsolutePath());
                        Server.getLogger().log("Mounting static at " +
                                       staticMountpoint);

                        HttpContext cx = server.http.addContext(staticMountpoint);

                        cx.setResourceBase(staticContent.getAbsolutePath());

                        ResourceHandler handler = new ResourceHandler();

                        cx.addHandler(handler);
                        cx.start();
                    }
                }

                // register as XML-RPC handler
                xmlrpcHandlers.put(app.getXmlRpcHandlerName(), app);
                // app.start();
            } catch (Exception x) {
                Server.getLogger().log("Couldn't bind app: " + x);
                x.printStackTrace();
            }
        }

        void unbind() {
            Server.getLogger().log("Unbinding application " + appName);

            try {
               // unbind from RMI server
                if (port > 0) {
                    Naming.unbind("//:" + port + "/" + appName);
                }

                // unbind from Jetty HTTP server
                if (server.http != null) {
                    HttpContext context = server.http.getContext(null, pathPattern);

                    if (context != null) {
                        context.stop();
                        context.destroy();
                    }

                    if (staticDir != null) {
                        context = server.http.getContext(null, staticMountpoint);

                        if (context != null) {
                            context.stop();
                            context.destroy();
                        }
                    }
                }

                // unregister as XML-RPC handler
                xmlrpcHandlers.remove(app.getXmlRpcHandlerName());
            } catch (Exception x) {
                Server.getLogger().log("Couldn't unbind app: " + x);
            }

        }

    }
}
