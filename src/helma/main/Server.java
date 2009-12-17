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
 * $RCSfile: Server.java,v $
 * $Author$
 * $Revision$
 * $Date$
 */

package helma.main;

import helma.extensions.HelmaExtension;
import helma.framework.repository.FileResource;
import helma.framework.core.*;
import helma.objectmodel.db.DbSource;
import helma.util.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xmlrpc.*;

import java.io.*;
import java.rmi.registry.*;
import java.rmi.server.*;
import java.util.*;
import java.net.*;

import helma.util.ResourceProperties;

/**
 * Helma server main class.
 */
public class Server implements Runnable {
    // version string
    public static final String version = "1.7.0 (__builddate__)";

    // static server instance
    private static Server server;

    // Server home directory
    protected File hopHome;

    // server-wide properties
    ResourceProperties appsProps;
    ResourceProperties dbProps;
    ResourceProperties sysProps;

    // our logger
    private Log logger;
    // are we using helma.util.Logging?
    private boolean helmaLogging;

    // server start time
    public final long starttime;

    // if paranoid == true we only accept XML-RPC connections from
    // explicitly listed hosts.
    public boolean paranoid;
    private ApplicationManager appManager;
    private Vector extensions;
    private Thread mainThread;

    // configuration
    ServerConfig config;

    // map of server-wide database sources
    Hashtable dbSources;

    // the embedded web server
    // protected Serve websrv;
    protected JettyServer jetty;

    // the XML-RPC server
    protected WebServer xmlrpc;
    
    Thread shutdownhook;


    /**
     * Constructs a new Server instance with an array of command line options.
     * TODO make this a singleton
     * @param config the configuration
     */
    public Server(ServerConfig config) {
        server = this;
        starttime = System.currentTimeMillis();

        this.config = config;
        hopHome    = config.getHomeDir();
        if (hopHome == null) {
            throw new RuntimeException("helma.home property not set");
        }

        // create system properties
        sysProps = new ResourceProperties();
        if (config.hasPropFile()) {
            sysProps.addResource(new FileResource(config.getPropFile()));
        }
    }


    /**
     * Static main entry point.
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        loadServer(args);
        // parse properties files etc
        server.init();
        // start the server main thread
        server.start();
    }

    /**
     * Entry point used by launcher.jar to load a server instance
     * @param args the command line arguments
     * @return the server instance
     */
    public static Server loadServer(String[] args) {
        checkJavaVersion();

        ServerConfig config = null;
        try {
            config = getConfig(args);
        } catch (Exception cex) {
            printUsageError("error reading configuration: " + cex.getMessage());
            System.exit(1);
        }

        checkRunning(config);

        // create new server instance
        server = new Server(config);
        return server;
    }


    /**
      * check if we are running on a Java 2 VM - otherwise exit with an error message
      */
    public static void checkJavaVersion() {
        String javaVersion = System.getProperty("java.version");

        if ((javaVersion == null) || javaVersion.startsWith("1.3")
                                  || javaVersion.startsWith("1.2")
                                  || javaVersion.startsWith("1.1")
                                  || javaVersion.startsWith("1.0")) {
            System.err.println("This version of Helma requires Java 1.4 or greater.");

            if (javaVersion == null) { // don't think this will ever happen, but you never know
                System.err.println("Your Java Runtime did not provide a version number. Please update to a more recent version.");
            } else {
                System.err.println("Your Java Runtime is version " + javaVersion +
                                   ". Please update to a more recent version.");
            }

            System.exit(1);
        }
    }


    /**
      * parse the command line arguments, read a given server.properties file
      * and check the values given for server ports
      * @return ServerConfig if successfull
      * @throws Exception on any configuration error
      */
    public static ServerConfig getConfig(String[] args) throws Exception {

        ServerConfig config = new ServerConfig();

        // get possible environment setting for helma home
        if (System.getProperty("helma.home")!=null) {
            config.setHomeDir(new File(System.getProperty("helma.home")));
        }

        parseArgs(config, args);

        guessConfig(config);

        // create system properties
        ResourceProperties sysProps = new ResourceProperties();
        sysProps.addResource(new FileResource(config.getPropFile()));

        // check if there's a property setting for those ports not specified via command line
        if (!config.hasWebsrvPort() && sysProps.getProperty("webPort") != null) {
            try {
                config.setWebsrvPort(getInetSocketAddress(sysProps.getProperty("webPort")));
            } catch (Exception portx) {
                throw new Exception("Error parsing web server port property from server.properties: " + portx);
            }
        }

        if (!config.hasAjp13Port() && sysProps.getProperty("ajp13Port") != null) {
            try {
                config.setAjp13Port(getInetSocketAddress(sysProps.getProperty("ajp13Port")));
            } catch (Exception portx) {
                throw new Exception("Error parsing AJP1.3 server port property from server.properties: " + portx);
            }
        }

        if (!config.hasXmlrpcPort() && sysProps.getProperty("xmlrpcPort") != null) {
            try {
                config.setXmlrpcPort(getInetSocketAddress(sysProps.getProperty("xmlrpcPort")));
            } catch (Exception portx) {
                throw new Exception("Error parsing XML-RPC server port property from server.properties: " + portx);
            }
        }
        return config;
    }


    /**
      * parse argument list from command line and store values
      * in given ServerConfig object
      * @throws Exception when argument can't be parsed into an InetAddrPort
      * or invalid token is given.
      */
    public static void parseArgs(ServerConfig config, String[] args) throws Exception {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-h") && ((i + 1) < args.length)) {
                config.setHomeDir(new File(args[++i]));
            } else if (args[i].equals("-f") && ((i + 1) < args.length)) {
                config.setPropFile(new File(args[++i]));
            } else if (args[i].equals("-a") && ((i + 1) < args.length)) {
                config.setApps(StringUtils.split(args[++i]));
            } else if (args[i].equals("-x") && ((i + 1) < args.length)) {
                try {
                    config.setXmlrpcPort(getInetSocketAddress(args[++i]));
                } catch (Exception portx) {
                    throw new Exception("Error parsing XML-RPC server port property: " + portx);
                }
            } else if (args[i].equals("-w") && ((i + 1) < args.length)) {
                try {
                    config.setWebsrvPort(getInetSocketAddress(args[++i]));
                } catch (Exception portx) {
                    throw new Exception("Error parsing web server port property: " + portx);
                }
            } else if (args[i].equals("-jk") && ((i + 1) < args.length)) {
                try {
                    config.setAjp13Port(getInetSocketAddress(args[++i]));
                } catch (Exception portx) {
                    throw new Exception("Error parsing AJP1.3 server port property: " + portx);
                }
            } else if (args[i].equals("-c") && ((i + 1) < args.length)) {
                config.setConfigFile(new File(args[++i]));
            } else if (args[i].equals("-i") && ((i + 1) < args.length)) {
                // eat away the -i parameter which is meant for helma.main.launcher.Main
                i++;
            } else {
                throw new Exception("Unknown command line token: " + args[i]);
            }
        }
    }


    /**
      * get main property file from home dir or vice versa,
      * depending on what we have
      */
    public static void guessConfig(ServerConfig config) throws Exception {
        // get property file from hopHome:
        if (!config.hasPropFile()) {
            if (config.hasHomeDir()) {
                config.setPropFile(new File(config.getHomeDir(), "server.properties"));
            } else {
                config.setPropFile(new File("server.properties"));
            }
        }

        // create system properties
        ResourceProperties sysProps = new ResourceProperties();
        sysProps.addResource(new FileResource(config.getPropFile()));

        // try to get hopHome from property file
        if (!config.hasHomeDir() && sysProps.getProperty("hophome") != null) {
            config.setHomeDir(new File(sysProps.getProperty("hophome")));
        }

        // use the directory where server.properties is located:
        if (!config.hasHomeDir() && config.hasPropFile()) {
            config.setHomeDir(config.getPropFile().getAbsoluteFile().getParentFile());
        }

        if (!config.hasPropFile()) {
            throw new Exception ("no server.properties found");
        }

        if (!config.hasHomeDir()) {
            throw new Exception ("couldn't determine helma directory");
        }
    }


    /**
      * print the usage hints and prefix them with a message.
      */
    public static void printUsageError(String msg) {
        System.out.println(msg);
        printUsageError();
    }


    /**
      * print the usage hints
      */
    public static void printUsageError() {
        System.out.println("");
        System.out.println("Usage: java helma.main.Server [options]");
        System.out.println("Possible options:");
        System.out.println("  -a app[,...]      Specify applications to start");
        System.out.println("  -h dir            Specify hop home directory");
        System.out.println("  -f file           Specify server.properties file");
        System.out.println("  -c jetty.xml      Specify Jetty XML configuration file");
        System.out.println("  -w [ip:]port      Specify embedded web server address/port");
        System.out.println("  -x [ip:]port      Specify XML-RPC address/port");
        System.out.println("  -jk [ip:]port     Specify AJP13 address/port");
        System.out.println("");
        System.out.println("Supported formats for server ports:");
        System.out.println("   <port-number>");
        System.out.println("   <ip-address>:<port-number>");
        System.out.println("   <hostname>:<port-number>");
        System.out.println("");
        System.err.println("Usage Error - exiting");
        System.out.println("");
    }



    /**
     *  Check wheter a server is already running on any of the given ports
     *  - otherwise exit with an error message
     */
    public static void checkRunning(ServerConfig config) {
        // check if any of the specified server ports is in use already
        try {
            if (config.hasWebsrvPort()) {
                checkPort(config.getWebsrvPort());
            }

            if (config.hasXmlrpcPort()) {
                checkPort(config.getXmlrpcPort());
            }

            if (config.hasAjp13Port()) {
                checkPort(config.getAjp13Port());
            }
        } catch (Exception running) {
            System.out.println(running.getMessage());
            System.exit(1);
        }

    }


    /**
     *  Check whether a server port is available by trying to open a server socket
     */
    private static void checkPort(InetSocketAddress endpoint) throws IOException {
        try {
            ServerSocket sock = new ServerSocket();
            sock.bind(endpoint);
            sock.close();
        } catch (IOException x) {
            throw new IOException("Error binding to " + endpoint + ": " + x.getMessage());
        }
    }


    /**
      * initialize the server
      */
    public void init() throws IOException {

        // set the log factory property
        String logFactory = sysProps.getProperty("loggerFactory",
                                                 "helma.util.Logging");

        helmaLogging = "helma.util.Logging".equals(logFactory);
        System.setProperty("org.apache.commons.logging.LogFactory", logFactory);

        // set the current working directory to the helma home dir.
        // note that this is not a real cwd, which is not supported
        // by java. It makes sure relative to absolute path name
        // conversion is done right, so for Helma code, this should work.
        System.setProperty("user.dir", hopHome.getPath());

        // from now on it's safe to call getLogger() because hopHome is set up
        getLogger();

        String startMessage = "Starting Helma " + version + " on Java " +
                              System.getProperty("java.version");

        logger.info(startMessage);

        // also print a msg to System.out
        System.out.println(startMessage);

        logger.info("Setting Helma Home to " + hopHome);


        // read db.properties file in helma home directory
        String dbPropfile = sysProps.getProperty("dbPropFile");
        File file;
        if ((dbPropfile != null) && !"".equals(dbPropfile.trim())) {
            file = new File(dbPropfile);
        } else {
            file = new File(hopHome, "db.properties");
        }

        dbProps = new ResourceProperties();
        dbProps.setIgnoreCase(false);
        dbProps.addResource(new FileResource(file));
        DbSource.setDefaultProps(dbProps);

        // read apps.properties file
        String appsPropfile = sysProps.getProperty("appsPropFile");
        if ((appsPropfile != null) && !"".equals(appsPropfile.trim())) {
            file = new File(appsPropfile);
        } else {
            file = new File(hopHome, "apps.properties");
        }
        appsProps = new ResourceProperties();
        appsProps.setIgnoreCase(true);
        appsProps.addResource(new FileResource(file));

        paranoid = "true".equalsIgnoreCase(sysProps.getProperty("paranoid"));

        String language = sysProps.getProperty("language");
        String country = sysProps.getProperty("country");
        String timezone = sysProps.getProperty("timezone");

        if ((language != null) && (country != null)) {
            Locale.setDefault(new Locale(language, country));
        }

        if (timezone != null) {
            TimeZone.setDefault(TimeZone.getTimeZone(timezone));
        }

        // logger.debug("Locale = " + Locale.getDefault());
        // logger.debug("TimeZone = " +
        //                 TimeZone.getDefault().getDisplayName(Locale.getDefault()));

        dbSources = new Hashtable();

        // try to load the extensions
        extensions = new Vector();
        if (sysProps.getProperty("extensions") != null) {
            initExtensions();
        }
        jetty = JettyServer.init(this, config);
    }


    /**
      * initialize extensions
      */
    private void initExtensions() {
        StringTokenizer tok = new StringTokenizer(sysProps.getProperty("extensions"), ",");
        while (tok.hasMoreTokens()) {
            String extClassName = tok.nextToken().trim();

            try {
                Class extClass = Class.forName(extClassName);
                HelmaExtension ext = (HelmaExtension) extClass.newInstance();
                ext.init(this);
                extensions.add(ext);
                logger.info("Loaded: " + extClassName);
            } catch (Throwable e) {
                logger.error("Error loading extension " + extClassName + ": " + e.toString());
            }
        }
    }



    public void start() {
        // Start running, finishing setup and then entering a loop to check changes
        // in the apps.properties file.
        mainThread = new Thread(this);
        mainThread.start();
    }

    public void stop() {
        mainThread = null;
        appManager.stopAll();
    }

    public void shutdown() {
        getLogger().info("Shutting down Helma");

        if (jetty != null) {
            try {
                jetty.stop();
                jetty.destroy();
            } catch (Exception x) {
                // exception in jettx stop. ignore.
            }
        }

        if (xmlrpc != null) {
            try {
                xmlrpc.shutdown();
            } catch (Exception x) {
                // exception in xmlrpc server shutdown, ignore.
            }
        }
        
        if (helmaLogging) {
            Logging.shutdown();
        }
        
        server = null;
        
        try {
            Runtime.getRuntime().removeShutdownHook(shutdownhook);
            // HACK: running the shutdownhook seems to be necessary in order
            // to prevent it from blocking garbage collection of helma 
            // classes/classloaders. Since we already set server to null it 
            // won't do anything anyhow.
            shutdownhook.start();
            shutdownhook = null;
        } catch (Exception x) {
            // invalid shutdown hook or already shutting down. ignore.
        }
    }

    /**
     *  The main method of the Server. Basically, we set up Applications and than
     *  periodically check for changes in the apps.properties file, shutting down
     *  apps or starting new ones.
     */
    public void run() {
        try {
            if (config.hasXmlrpcPort()) {
                InetSocketAddress xmlrpcPort = config.getXmlrpcPort();
                String xmlparser = sysProps.getProperty("xmlparser");

                if (xmlparser != null) {
                    XmlRpc.setDriver(xmlparser);
                }

                if (xmlrpcPort.getAddress() != null) {
                    xmlrpc = new WebServer(xmlrpcPort.getPort(), xmlrpcPort.getAddress());
                } else {
                    xmlrpc = new WebServer(xmlrpcPort.getPort());
                }

                if (paranoid) {
                    xmlrpc.setParanoid(true);

                    String xallow = sysProps.getProperty("allowXmlRpc");

                    if (xallow != null) {
                        StringTokenizer st = new StringTokenizer(xallow, " ,;");

                        while (st.hasMoreTokens())
                            xmlrpc.acceptClient(st.nextToken());
                    }
                }
                xmlrpc.start();
                logger.info("Starting XML-RPC server on port " + (xmlrpcPort));
            }

            appManager = new ApplicationManager(appsProps, this);

            if (xmlrpc != null) {
                xmlrpc.addHandler("$default", appManager);
            }

            // add shutdown hook to close running apps and servers on exit
            shutdownhook = new HelmaShutdownHook();
            Runtime.getRuntime().addShutdownHook(shutdownhook);
        } catch (Exception x) {
            throw new RuntimeException("Error setting up Server", x);
        }

        // set the security manager.
        // the default implementation is helma.main.HelmaSecurityManager.
        try {
            String secManClass = sysProps.getProperty("securityManager");

            if (secManClass != null) {
                SecurityManager secMan = (SecurityManager) Class.forName(secManClass)
                                                                .newInstance();

                System.setSecurityManager(secMan);
                logger.info("Setting security manager to " + secManClass);
            }
        } catch (Exception x) {
            logger.error("Error setting security manager", x);
        }

        // start embedded web server
        if (jetty != null) {
            try {
                jetty.start();
            } catch (Exception m) {
                throw new RuntimeException("Error starting embedded web server", m);
            }
        }

        // start applications
        appManager.startAll();

        while (Thread.currentThread() == mainThread) {
            try {
                Thread.sleep(3000L);
            } catch (InterruptedException ie) {
            }

            try {
                appManager.checkForChanges();
            } catch (Exception x) {
                logger.warn("Caught in app manager loop: " + x);
            }
        }
    }

    /**
     * Make sure this server has an ApplicationManager (e.g. used when
     * accessed from CommandlineRunner)
     */
    public void checkAppManager() {
        if (appManager == null) {
            appManager = new ApplicationManager(appsProps, this);
        }
    }

    /**
     *  Get an Iterator over the applications currently running on this Server.
     */
    public Object[] getApplications() {
        return appManager.getApplications();
    }

    /**
     * Get an Application by name
     */
    public Application getApplication(String name) {
        return appManager.getApplication(name);
    }

    /**
     *  Get a logger to use for output in this server.
     */
    public Log getLogger() {
        if (logger == null) {
            if (helmaLogging) {
                // set up system properties for helma.util.Logging
                String logDir = sysProps.getProperty("logdir", "log");

                if (!"console".equals(logDir)) {
                    // try to get the absolute logdir path

                    // set up helma.logdir system property
                    File dir = new File(logDir);
                    if (!dir.isAbsolute()) {
                        dir = new File(hopHome, logDir);
                    }

                    logDir = dir.getAbsolutePath();
                }
                System.setProperty("helma.logdir", logDir);
            }
            logger = LogFactory.getLog("helma.server");
        }

        return logger;
    }

    /**
     *  Get the Home directory of this server.
     */
    public File getHopHome() {
        return hopHome;
    }

    /**
     * Get the explicit list of apps if started with -a option
     * @return
     */
    public String[] getApplicationsOption() {
        return config.getApps();
    }

    /**
     * Get the main Server instance.
     */
    public static Server getServer() {
        return server;
    }

    /**
     *  Get the Server's  XML-RPC web server.
     */
    public static WebServer getXmlRpcServer() {
        return server.xmlrpc;
    }

    /**
     *
     *
     * @param key ...
     *
     * @return ...
     */
    public String getProperty(String key) {
        return (String) sysProps.get(key);
    }

    /**
     * Return the server.properties for this server
     * @return the server.properties
     */
    public ResourceProperties getProperties() {
        return sysProps;
    }

    /**
     * Return the server-wide db.properties
     * @return the server-wide db.properties
     */
    public ResourceProperties getDbProperties() {
        return dbProps;
    }

    /**
     * Return the apps.properties entries for a given application
     * @param appName the app name
     * @return the apps.properties subproperties for the given app
     */
    public ResourceProperties getAppsProperties(String appName) {
        if (appName == null) {
            return appsProps;
        } else {
            return appsProps.getSubProperties(appName + ".");
        }
    }

    /**
     *
     *
     * @return ...
     */
    public File getAppsHome() {
        String appHome = sysProps.getProperty("appHome", "");

        if (appHome.trim().length() != 0) {
            return new File(appHome);
        } else {
            return new File(hopHome, "apps");
        }
    }

    /**
     *
     *
     * @return ...
     */
    public File getDbHome() {
        String dbHome = sysProps.getProperty("dbHome", "");

        if (dbHome.trim().length() != 0) {
            return new File(dbHome);
        } else {
            return new File(hopHome, "db");
        }
    }

    /**
     *
     *
     * @return ...
     */
    public Vector getExtensions() {
        return extensions;
    }

    /**
     *
     *
     * @param name ...
     */
    public void startApplication(String name) {
        appManager.start(name);
        appManager.register(name);
    }

    /**
     *
     *
     * @param name ...
     */
    public void stopApplication(String name) {
        appManager.stop(name);
    }

    private static InetSocketAddress getInetSocketAddress(String inetAddrPort)
            throws UnknownHostException {
        InetAddress addr = null;
        int c = inetAddrPort.indexOf(':');
        if (c >= 0) {
            String a = inetAddrPort.substring(0, c);
            if (a.indexOf('/') > 0)
                a = a.substring(a.indexOf('/') + 1);
            inetAddrPort = inetAddrPort.substring(c + 1);

            if (a.length() > 0 && !"0.0.0.0".equals(a)) {
                addr = InetAddress.getByName(a);
            }
        }
        int port = Integer.parseInt(inetAddrPort);
        return new InetSocketAddress(addr, port);
    }
}


