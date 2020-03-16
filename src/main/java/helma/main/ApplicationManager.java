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

import helma.framework.core.*;
import helma.framework.repository.Repository;
import helma.framework.repository.FileRepository;
import helma.util.StringUtils;
import org.apache.xmlrpc.XmlRpcHandler;
import org.apache.commons.logging.Log;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import java.io.*;
import java.util.*;
import helma.util.ResourceProperties;
import helma.servlet.EmbeddedServletClient;

/**
 * This class is responsible for starting and stopping Helma applications.
 */
public class ApplicationManager implements XmlRpcHandler {
    private Hashtable descriptors;
    private Hashtable applications;
    private Hashtable xmlrpcHandlers;
    private ResourceProperties props;
    private Server server;
    private long lastModified;
    private ContextHandlerCollection context;
    private JettyServer jetty = null;

    /**
     * Creates a new ApplicationManager object.
     *
     * @param props the properties defining the running apps
     * @param server the server instance
     */
    public ApplicationManager(ResourceProperties props, Server server) {
        this.props = props;
        this.server = server;
        descriptors = new Hashtable();
        applications = new Hashtable();
        xmlrpcHandlers = new Hashtable();
        lastModified = 0;
        jetty = server.jetty;
    }

    /**
     * Called regularely check applications property file
     * to create and start new applications.
     */
    protected void checkForChanges() {
        if (props.lastModified() > lastModified && server.getApplicationsOption() == null) {
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
                    } else if (server.jetty != null) {
                        // If application continues to run, remount
                        // as the mounting options may have changed.
                        AppDescriptor ndesc = new AppDescriptor(appDesc.appName);
                        ndesc.app = appDesc.app;
                        appDesc.unbind();
                        ndesc.bind();
                        descriptors.put(ndesc.appName, ndesc);
                    }
                }
            } catch (Exception mx) {
                getLogger().error("Error checking applications", mx);
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
            String[] apps = server.getApplicationsOption();
            if (apps != null) {
                for (int i = 0; i < apps.length; i++) {
                    AppDescriptor desc = new AppDescriptor(apps[i]);
                    desc.start();
                }
            } else {
                for (Enumeration e = props.keys(); e.hasMoreElements();) {
                    String appName = (String) e.nextElement();

                    if (appName.indexOf(".") == -1) {
                        String appValue = props.getProperty(appName);

                        if (appValue != null && appValue.length() > 0) {
                            appName = appValue;
                        }

                        AppDescriptor desc = new AppDescriptor(appName);
                        desc.start();
                    }
                }
            }

            for (Enumeration e = descriptors.elements(); e.hasMoreElements();) {
                AppDescriptor appDesc = (AppDescriptor) e.nextElement();
                appDesc.bind();
            }

            lastModified = System.currentTimeMillis();
        } catch (Exception mx) {
            getLogger().error("Error starting applications", mx);
            mx.printStackTrace();
        }
    }

    /**
     *  Stop all running applications.
     */
    public void stopAll() {
        for (Enumeration en = descriptors.elements(); en.hasMoreElements();) {
            try {
                AppDescriptor appDesc = (AppDescriptor) en.nextElement();

                appDesc.stop();
            } catch (Exception x) {
                // ignore exception in application shutdown
            }
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
            app = (Application) xmlrpcHandlers.get("*");
            // use the original method name, the handler is resolved within the app.
            method2 = method;
        }

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
            return mountpoint.substring(0, mountpoint.length()-1);
        }

        return mountpoint;
    }

    private File getAbsoluteFile(String path) {
        // make sure our directory has an absolute path,
        // see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4117557
        File file = new File(path);
        if (file.isAbsolute()) {
            return file;
        } else {
            return file.getAbsoluteFile();
        }
    }

    private Log getLogger() {
        return server.getLogger();
    }

    private String findResource(String path) {
        File file = new File(path);
        if (!file.isAbsolute() && !file.exists()) {
            file = new File(server.getHopHome(), path);
        }
        return file.getAbsolutePath();
    }

    /**
     *  Inner class that describes an application and its start settings.
     */
    class AppDescriptor {

        Application app;

        private ContextHandler staticContext = null;
        private ServletContextHandler appContext = null;

        String appName;
        File appDir;
        File dbDir;
        String mountpoint;
        String pathPattern;
        String staticDir;
        String protectedStaticDir;
        String staticMountpoint;
        boolean staticIndex;
        String[] staticHome;
        String xmlrpcHandlerName;
        String cookieDomain;
        String sessionCookieName;
        String protectedSessionCookie;
        String uploadLimit;
        String uploadSoftfail;
        String debug;
        Repository[] repositories;
        String servletClassName;

        /**
         * extend apps.properties, add [appname].ignore
         */
        String ignoreDirs;

        /**
         * Creates an AppDescriptor from the properties.
         * @param name the application name
         */
        AppDescriptor(String name) {
            ResourceProperties conf = props.getSubProperties(name + '.');
            appName = name;
            mountpoint = getMountpoint(conf.getProperty("mountpoint", appName));
            pathPattern = getPathPattern(mountpoint);
            staticDir = conf.getProperty("static");
            staticMountpoint = getPathPattern(conf.getProperty("staticMountpoint",
                                        joinMountpoint(mountpoint, "static")));
            staticIndex = "true".equalsIgnoreCase(conf.getProperty("staticIndex"));
            String home = conf.getProperty("staticHome");
            if (home == null) {
                staticHome = new String[] {"index.html", "index.htm"};
            } else {
                staticHome = StringUtils.split(home, ",");
            }
            protectedStaticDir = conf.getProperty("protectedStatic");

            cookieDomain = conf.getProperty("cookieDomain");
            sessionCookieName = conf.getProperty("sessionCookieName");
            protectedSessionCookie = conf.getProperty("protectedSessionCookie");
            uploadLimit = conf.getProperty("uploadLimit");
            uploadSoftfail = conf.getProperty("uploadSoftfail");
            debug = conf.getProperty("debug");
            String appDirName = conf.getProperty("appdir");
            appDir = (appDirName == null) ? null : getAbsoluteFile(appDirName);
            String dbDirName = conf.getProperty("dbdir");
            dbDir = (dbDirName == null) ? null : getAbsoluteFile(dbDirName);
            servletClassName = conf.getProperty("servletClass");

            // got ignore dirs
            ignoreDirs = conf.getProperty("ignore");

            // read and configure app repositories
            ArrayList repositoryList = new ArrayList();
            Class[] parameters = { String.class };
            for (int i = 0; true; i++) {
                String repositoryArgs = conf.getProperty("repository." + i);

                if (repositoryArgs != null) {
                    // lookup repository implementation
                    String repositoryImpl = conf.getProperty("repository." + i +
                                                              ".implementation");
                    if (repositoryImpl == null) {
                        // implementation not set manually, have to guess it
                        if (repositoryArgs.endsWith(".zip")) {
                            repositoryArgs = findResource(repositoryArgs);
                            repositoryImpl = "helma.framework.repository.ZipRepository";
                        } else if (repositoryArgs.endsWith(".js")) {
                            repositoryArgs = findResource(repositoryArgs);
                            repositoryImpl = "helma.framework.repository.SingleFileRepository";
                        } else {
                            repositoryArgs = findResource(repositoryArgs);
                            repositoryImpl = "helma.framework.repository.FileRepository";
                        }
                    }

                    try {
                        Repository newRepository = (Repository) Class.forName(repositoryImpl)
                                .getConstructor(parameters)
                                .newInstance(new Object[] {repositoryArgs});
                        repositoryList.add(newRepository);
                    } catch (Exception ex) {
                        getLogger().error("Adding repository " + repositoryArgs + " failed. " +
                                           "Will not use that repository. Check your initArgs!", ex);
                    }
                } else {
                    // we always scan repositories 0-9, beyond that only if defined
                    if (i > 9) {
                        break;
                    }
                }
            }

            if (appDir != null) {
                FileRepository appRep = new FileRepository(appDir);
                if (!repositoryList.contains(appRep)) {
                    repositoryList.add(appRep);
                }
            } else if (repositoryList.isEmpty()) {
                repositoryList.add(new FileRepository(
                        new File(server.getAppsHome(), appName)));
            }
            repositories = new Repository[repositoryList.size()];
            repositories = (Repository[]) repositoryList.toArray(repositories);
        }


        void start() {
            getLogger().info("Building application " + appName);

            try {
                // create the application instance
                app = new Application(appName, server, repositories, appDir, dbDir);

                // register ourselves
                descriptors.put(appName, this);
                applications.put(appName, app);

                // the application is started later in the register method, when it's bound
                app.init(ignoreDirs);

                // set application URL prefix if it isn't set in app.properties
                if (!app.hasExplicitBaseURI()) {
                    app.setBaseURI(mountpoint);
                }

                app.start();
            } catch (Exception x) {
                getLogger().error("Error creating application " + appName, x);
                x.printStackTrace();
            }
        }

        void stop() {
            getLogger().info("Stopping application " + appName);

            // unbind application
            unbind();

            // stop application
            try {
                app.stop();
                getLogger().info("Stopped application " + appName);
            } catch (Exception x) {
                getLogger().error("Couldn't stop app", x);
            }

            descriptors.remove(appName);
            applications.remove(appName);
        }

        void bind() {
            try {
                getLogger().info("Binding application " + appName + " :: " + app.hashCode() + " :: " + this.hashCode());

                // set application URL prefix if it isn't set in app.properties
                if (!app.hasExplicitBaseURI()) {
                    app.setBaseURI(mountpoint);
                }

                // bind to Jetty HTTP server
                if (jetty != null) {
                    if (context == null) {
                        context = new ContextHandlerCollection();
                        jetty.getHttpServer().setHandler(context);
                    }

                    // if there is a static direcory specified, mount it
                    if (staticDir != null) {

                        File staticContent = getAbsoluteFile(staticDir);

                        getLogger().info("Serving static from " + staticContent.getPath());
                        getLogger().info("Mounting static at " + staticMountpoint);

                        ResourceHandler rhandler = new ResourceHandler();
                        rhandler.setResourceBase(staticContent.getPath());
                        rhandler.setWelcomeFiles(staticHome);

                        staticContext = context.addContext(staticMountpoint, "");
                        staticContext.setHandler(rhandler);

                        staticContext.start();
                    }

                    appContext = new ServletContextHandler(context, pathPattern, true, true);
                    Class servletClass = servletClassName == null ?
                            EmbeddedServletClient.class : Class.forName(servletClassName);
                    ServletHolder holder = new ServletHolder(servletClass);
                    holder.setInitParameter("application", appName);
                    appContext.addServlet(holder, "/*");

                    if (cookieDomain != null) {
                        holder.setInitParameter("cookieDomain", cookieDomain);
                    }

                    if (sessionCookieName != null) {
                        holder.setInitParameter("sessionCookieName", sessionCookieName);
                    }

                    if (protectedSessionCookie != null) {
                        holder.setInitParameter("protectedSessionCookie", protectedSessionCookie);
                    }

                    if (uploadLimit != null) {
                        holder.setInitParameter("uploadLimit", uploadLimit);
                    }

                    if (uploadSoftfail != null) {
                        holder.setInitParameter("uploadSoftfail", uploadSoftfail);
                    }

                    if (debug != null) {
                        holder.setInitParameter("debug", debug);
                    }
                    
                    if (protectedStaticDir != null) {
                        File protectedContent = getAbsoluteFile(protectedStaticDir);
                        appContext.setResourceBase(protectedContent.getPath());
                        getLogger().info("Serving protected static from " +
                                       protectedContent.getPath());
                    }

                    // Remap the context paths and start
                    context.mapContexts();
                    appContext.start();
                }

                // register as XML-RPC handler
                xmlrpcHandlerName = app.getXmlRpcHandlerName();
                xmlrpcHandlers.put(xmlrpcHandlerName, app);
            } catch (Exception x) {
                getLogger().error("Couldn't bind app", x);
                x.printStackTrace();
            }
        }

        void unbind() {
            getLogger().info("Unbinding application " + appName);

            try {
                // unbind from Jetty HTTP server
                if (jetty != null) {
                    if (appContext != null) {
                        context.removeHandler(appContext);
                        appContext.stop();
                        appContext.destroy();
                        appContext = null;
                    }

                    if (staticContext != null) {
                        context.removeHandler(staticContext);
                        staticContext.stop();
                        staticContext.destroy();
                        staticContext = null;
                    }
                    context.mapContexts();
                }

                // unregister as XML-RPC handler
                if (xmlrpcHandlerName != null) {
                    xmlrpcHandlers.remove(xmlrpcHandlerName);
                }
            } catch (Exception x) {
                getLogger().error("Couldn't unbind app", x);
            }

        }

        public String toString() {
            return "[AppDescriptor "+app+"]";
        }
    }
}
