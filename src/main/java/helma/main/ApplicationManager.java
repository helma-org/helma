/*
 * Helma License Notice
 *
 * The contents of this file are subject to the Helma License
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://adele.helma.org/download/helma/license.txt
 *
 * Copyright 1998-2003 Helma Software. All Rights Reserved.
 */

package helma.main;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.xmlrpc.XmlRpcHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import helma.framework.core.Application;
import helma.framework.repository.FileRepository;
import helma.framework.repository.Repository;
import helma.servlet.EmbeddedServletClient;
import helma.util.ResourceProperties;
import helma.util.StringUtils;

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
        this.descriptors = new Hashtable();
        this.applications = new Hashtable();
        this.xmlrpcHandlers = new Hashtable();
        this.lastModified = 0;
        this.jetty = server.jetty;
    }

    /**
     * Called regularely check applications property file
     * to create and start new applications.
     */
    protected void checkForChanges() {
        if (this.props.lastModified() > this.lastModified && this.server.getApplicationsOption() == null) {
            try {
                for (Enumeration e = this.props.keys(); e.hasMoreElements();) {
                    String appName = (String) e.nextElement();

                    if ((appName.indexOf(".") == -1) && //$NON-NLS-1$
                            (this.applications.get(appName) == null)) {
                        AppDescriptor appDesc = new AppDescriptor(appName);
                        appDesc.start();
                        appDesc.bind();
                    }
                }

                // then stop deleted ones
                for (Enumeration e = this.descriptors.elements(); e.hasMoreElements();) {
                    AppDescriptor appDesc = (AppDescriptor) e.nextElement();

                    // check if application has been removed and should be stopped
                    if (!this.props.containsKey(appDesc.appName)) {
                        appDesc.stop();
                    } else if (this.server.jetty != null) {
                        // If application continues to run, remount
                        // as the mounting options may have changed.
                        AppDescriptor ndesc = new AppDescriptor(appDesc.appName);
                        ndesc.app = appDesc.app;
                        appDesc.unbind();
                        ndesc.bind();
                        this.descriptors.put(ndesc.appName, ndesc);
                    }
                }
            } catch (Exception mx) {
                getLogger().error("Error checking applications", mx);
            }

            this.lastModified = System.currentTimeMillis();
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
        AppDescriptor desc = (AppDescriptor) this.descriptors.get(appName);
        if (desc != null) {
            desc.bind();
        }
    }

    /**
     *  Stop an application by name
     */
    public void stop(String appName) {
        AppDescriptor desc = (AppDescriptor) this.descriptors.get(appName);
        if (desc != null) {
            desc.stop();
        }
    }


    /**
     * Start all applications listed in the properties
     */
    public void startAll() {
        try {
            String[] apps = this.server.getApplicationsOption();
            if (apps != null) {
                for (int i = 0; i < apps.length; i++) {
                    AppDescriptor desc = new AppDescriptor(apps[i]);
                    desc.start();
                }
            } else {
                for (Enumeration e = this.props.keys(); e.hasMoreElements();) {
                    String appName = (String) e.nextElement();

                    if (appName.indexOf(".") == -1) { //$NON-NLS-1$
                        String appValue = this.props.getProperty(appName);

                        if (appValue != null && appValue.length() > 0) {
                            appName = appValue;
                        }

                        AppDescriptor desc = new AppDescriptor(appName);
                        desc.start();
                    }
                }
            }

            for (Enumeration e = this.descriptors.elements(); e.hasMoreElements();) {
                AppDescriptor appDesc = (AppDescriptor) e.nextElement();
                appDesc.bind();
            }

            this.lastModified = System.currentTimeMillis();
        } catch (Exception mx) {
            getLogger().error("Error starting applications", mx);
            mx.printStackTrace();
        }
    }

    /**
     *  Stop all running applications.
     */
    public void stopAll() {
        for (Enumeration en = this.descriptors.elements(); en.hasMoreElements();) {
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
        return this.applications.values().toArray();
    }

    /**
     *  Get an application by name.
     */
    public Application getApplication(String name) {
        return (Application) this.applications.get(name);
    }

    /**
     * Implements org.apache.xmlrpc.XmlRpcHandler.execute()
     */
    public Object execute(String method, Vector params)
                   throws Exception {
        int dot = method.indexOf("."); //$NON-NLS-1$

        if (dot == -1) {
            throw new Exception("Method name \"" + method +
                                "\" does not specify a handler application");
        }

        if ((dot == 0) || (dot == (method.length() - 1))) {
            throw new Exception("\"" + method + "\" is not a valid XML-RPC method name");
        }

        String handler = method.substring(0, dot);
        String method2 = method.substring(dot + 1);
        Application app = (Application) this.xmlrpcHandlers.get(handler);

        if (app == null) {
            app = (Application) this.xmlrpcHandlers.get("*"); //$NON-NLS-1$
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

        if ("".equals(mountpoint)) { //$NON-NLS-1$
            return "/"; //$NON-NLS-1$
        } else if (!mountpoint.startsWith("/")) { //$NON-NLS-1$
            return "/" + mountpoint; //$NON-NLS-1$
        }

        return mountpoint;
    }

    private String joinMountpoint(String prefix, String suffix) {
        if (prefix.endsWith("/") || suffix.startsWith("/")) {  //$NON-NLS-1$//$NON-NLS-2$
            return prefix+suffix;
        }
        return prefix+"/"+suffix; //$NON-NLS-1$
    }

    private String getPathPattern(String mountpoint) {
        if (!mountpoint.startsWith("/")) { //$NON-NLS-1$
            mountpoint = "/"+mountpoint; //$NON-NLS-1$
        }

        if ("/".equals(mountpoint)) { //$NON-NLS-1$
            return "/"; //$NON-NLS-1$
        }

        if (mountpoint.endsWith("/")) { //$NON-NLS-1$
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
        }
        return file.getAbsoluteFile();
    }

    private Log getLogger() {
        return this.server.getLogger();
    }

    private String findResource(String path) {
        File file = new File(path);
        if (!file.isAbsolute() && !file.exists()) {
            file = new File(this.server.getHopHome(), path);
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
            ResourceProperties conf = ApplicationManager.this.props.getSubProperties(name + '.');
            this.appName = name;
            this.mountpoint = getMountpoint(conf.getProperty("mountpoint", this.appName)); //$NON-NLS-1$
            this.pathPattern = getPathPattern(this.mountpoint);
            this.staticDir = conf.getProperty("static"); //$NON-NLS-1$
            this.staticMountpoint = getPathPattern(conf.getProperty("staticMountpoint", //$NON-NLS-1$
                                        joinMountpoint(this.mountpoint, "static"))); //$NON-NLS-1$
            this.staticIndex = "true".equalsIgnoreCase(conf.getProperty("staticIndex"));  //$NON-NLS-1$//$NON-NLS-2$
            String home = conf.getProperty("staticHome"); //$NON-NLS-1$
            if (home == null) {
                this.staticHome = new String[] {"index.html", "index.htm"}; //$NON-NLS-1$ //$NON-NLS-2$
            } else {
                this.staticHome = StringUtils.split(home, ","); //$NON-NLS-1$
            }
            this.protectedStaticDir = conf.getProperty("protectedStatic"); //$NON-NLS-1$

            this.cookieDomain = conf.getProperty("cookieDomain"); //$NON-NLS-1$
            this.sessionCookieName = conf.getProperty("sessionCookieName"); //$NON-NLS-1$
            this.protectedSessionCookie = conf.getProperty("protectedSessionCookie"); //$NON-NLS-1$
            this.uploadLimit = conf.getProperty("uploadLimit"); //$NON-NLS-1$
            this.uploadSoftfail = conf.getProperty("uploadSoftfail"); //$NON-NLS-1$
            this.debug = conf.getProperty("debug"); //$NON-NLS-1$
            String appDirName = conf.getProperty("appdir"); //$NON-NLS-1$
            this.appDir = (appDirName == null) ? null : getAbsoluteFile(appDirName);
            String dbDirName = conf.getProperty("dbdir"); //$NON-NLS-1$
            this.dbDir = (dbDirName == null) ? null : getAbsoluteFile(dbDirName);
            this.servletClassName = conf.getProperty("servletClass"); //$NON-NLS-1$

            // got ignore dirs
            this.ignoreDirs = conf.getProperty("ignore"); //$NON-NLS-1$

            // read and configure app repositories
            ArrayList repositoryList = new ArrayList();
            Class[] parameters = { String.class };
            for (int i = 0; true; i++) {
                String repositoryArgs = conf.getProperty("repository." + i); //$NON-NLS-1$

                if (repositoryArgs != null) {
                    // lookup repository implementation
                    String repositoryImpl = conf.getProperty("repository." + i + //$NON-NLS-1$
                                                              ".implementation"); //$NON-NLS-1$
                    if (repositoryImpl == null) {
                        // implementation not set manually, have to guess it
                        if (repositoryArgs.endsWith(".zip")) { //$NON-NLS-1$
                            repositoryArgs = findResource(repositoryArgs);
                            repositoryImpl = "helma.framework.repository.ZipRepository"; //$NON-NLS-1$
                        } else if (repositoryArgs.endsWith(".js")) { //$NON-NLS-1$
                            repositoryArgs = findResource(repositoryArgs);
                            repositoryImpl = "helma.framework.repository.SingleFileRepository"; //$NON-NLS-1$
                        } else {
                            repositoryArgs = findResource(repositoryArgs);
                            repositoryImpl = "helma.framework.repository.FileRepository"; //$NON-NLS-1$
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

            if (this.appDir != null) {
                FileRepository appRep = new FileRepository(this.appDir);
                if (!repositoryList.contains(appRep)) {
                    repositoryList.add(appRep);
                }
            } else if (repositoryList.isEmpty()) {
                repositoryList.add(new FileRepository(
                        new File(ApplicationManager.this.server.getAppsHome(), this.appName)));
            }

            this.repositories = (Repository[]) repositoryList.toArray(new Repository[repositoryList.size()]);
        }


        void start() {
            getLogger().info("Building application " + appName);

            try {
                // create the application instance
                this.app = new Application(this.appName, ApplicationManager.this.server, this.repositories, this.appDir, this.dbDir);

                // register ourselves
                ApplicationManager.this.descriptors.put(this.appName, this);
                ApplicationManager.this.applications.put(this.appName, this.app);

                // the application is started later in the register method, when it's bound
                this.app.init(this.ignoreDirs);

                // set application URL prefix if it isn't set in app.properties
                if (!this.app.hasExplicitBaseURI()) {
                    this.app.setBaseURI(this.mountpoint);
                }

                this.app.start();
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

            ApplicationManager.this.descriptors.remove(this.appName);
            ApplicationManager.this.applications.remove(this.appName);
        }

        void bind() {
            try {
                getLogger().info("Binding application " + appName + " :: " + app.hashCode() + " :: " + this.hashCode());

                // set application URL prefix if it isn't set in app.properties
                if (!this.app.hasExplicitBaseURI()) {
                    this.app.setBaseURI(this.mountpoint);
                }

                // bind to Jetty HTTP server
                if (jetty != null) {
                    if (context == null) {
                        context = new ContextHandlerCollection();
                        jetty.getHttpServer().setHandler(context);
                    }

                    // if there is a static direcory specified, mount it
                    if (this.staticDir != null) {

                        File staticContent = getAbsoluteFile(this.staticDir);

                        getLogger().info("Serving static from " + staticContent.getPath());
                        getLogger().info("Mounting static at " + staticMountpoint);

                        ResourceHandler rhandler = new ResourceHandler();
                        rhandler.setResourceBase(staticContent.getPath());
                        rhandler.setWelcomeFiles(staticHome);

                        staticContext = ApplicationManager.this.context.addContext(staticMountpoint, ""); //$NON-NLS-1$
                        staticContext.setHandler(rhandler);

                        staticContext.start();
                    }

                    appContext = new ServletContextHandler(context, pathPattern, true, true);
                    Class servletClass = servletClassName == null ?
                            EmbeddedServletClient.class : Class.forName(servletClassName);
                    ServletHolder holder = new ServletHolder(servletClass);
                    holder.setInitParameter("application", appName);
                    appContext.addServlet(holder, "/*");

                    if (this.cookieDomain != null) {
                        holder.setInitParameter("cookieDomain", this.cookieDomain); //$NON-NLS-1$
                    }

                    if (this.sessionCookieName != null) {
                        holder.setInitParameter("sessionCookieName", this.sessionCookieName); //$NON-NLS-1$
                    }

                    if (this.protectedSessionCookie != null) {
                        holder.setInitParameter("protectedSessionCookie", this.protectedSessionCookie); //$NON-NLS-1$
                    }

                    if (this.uploadLimit != null) {
                        holder.setInitParameter("uploadLimit", this.uploadLimit); //$NON-NLS-1$
                    }

                    if (this.uploadSoftfail != null) {
                        holder.setInitParameter("uploadSoftfail", this.uploadSoftfail); //$NON-NLS-1$
                    }

                    if (this.debug != null) {
                        holder.setInitParameter("debug", this.debug); //$NON-NLS-1$
                    }

                    if (protectedStaticDir != null) {
                        File protectedContent = getAbsoluteFile(protectedStaticDir);
                        appContext.setResourceBase(protectedContent.getPath());
                        getLogger().info("Serving protected static from " +
                                       protectedContent.getPath());
                    }

                    // Remap the context paths and start
                    ApplicationManager.this.context.mapContexts();
                    this.appContext.start();
                }

                // register as XML-RPC handler
                this.xmlrpcHandlerName = this.app.getXmlRpcHandlerName();
                ApplicationManager.this.xmlrpcHandlers.put(this.xmlrpcHandlerName, this.app);
            } catch (Exception x) {
                getLogger().error("Couldn't bind app", x);
                x.printStackTrace();
            }
        }

        void unbind() {
            getLogger().info("Unbinding application " + appName);

            try {
                // unbind from Jetty HTTP server
                if (ApplicationManager.this.jetty != null) {
                    if (this.appContext != null) {
                        ApplicationManager.this.context.removeHandler(this.appContext);
                        this.appContext.stop();
                        this.appContext.destroy();
                        this.appContext = null;
                    }

                    if (this.staticContext != null) {
                        ApplicationManager.this.context.removeHandler(this.staticContext);
                        this.staticContext.stop();
                        this.staticContext.destroy();
                        this.staticContext = null;
                    }
                    ApplicationManager.this.context.mapContexts();
                }

                // unregister as XML-RPC handler
                if (this.xmlrpcHandlerName != null) {
                    ApplicationManager.this.xmlrpcHandlers.remove(this.xmlrpcHandlerName);
                }
            } catch (Exception x) {
                getLogger().error("Couldn't unbind app", x);
            }

        }

        @Override
        public String toString() {
            return "[AppDescriptor "+this.app+"]"; //$NON-NLS-1$ //$NON-NLS-2$
        }
    }
}
