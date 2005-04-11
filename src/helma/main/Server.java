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

import helma.extensions.HelmaExtension;
import helma.framework.*;
import helma.framework.repository.FileResource;
import helma.framework.core.*;
import helma.objectmodel.db.DbSource;
import helma.util.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xmlrpc.*;
import org.mortbay.http.*;
import org.mortbay.http.ajp.*;
import org.mortbay.util.InetAddrPort;
import org.mortbay.util.LogSink;
import org.mortbay.util.MultiException;
import org.mortbay.util.Frame;
import java.io.*;
import java.rmi.registry.*;
import java.rmi.server.*;
import java.util.*;
import helma.util.ResourceProperties;

/**
 * Helma server main class.
 */
public class Server implements IPathElement, Runnable {
    // version string
    public static final String version = "1.4.2 (2005/03/09)";

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

    // if paranoid == true we only accept RMI and XML-RPC connections from
    // explicitly listed hosts.
    public boolean paranoid;
    private ApplicationManager appManager;
    private Vector extensions;
    private Thread mainThread;

    // server ports
    InetAddrPort rmiPort = null;
    InetAddrPort xmlrpcPort = null;
    InetAddrPort websrvPort = null;
    InetAddrPort ajp13Port = null;

    // map of server-wide database sources
    Hashtable dbSources;

    // the embedded web server
    // protected Serve websrv;
    protected HttpServer http;

    // the AJP13 Listener, used for connecting from external webserver to servlet via JK
    protected AJP13Listener ajp13;

    // the XML-RPC server
    protected WebServer xmlrpc;
    
    Thread shutdownhook;


    /**
     * Constructs a new Server instance with an array of command line options.
     */
    public Server(Config config) {
        server = this;
        starttime = System.currentTimeMillis();

        rmiPort    = config.rmiPort;
        xmlrpcPort = config.xmlrpcPort;
        websrvPort = config.websrvPort;
        ajp13Port  = config.ajp13Port;
        hopHome    = config.homeDir;

        // create system properties
        sysProps = new ResourceProperties();
        sysProps.addResource(new FileResource(config.propFile));
    }


    /**
     *  static main entry point.
     */
    public static void main(String[] args) {
        checkJavaVersion();

        Config config = null;
        try {
            config = getConfig(args);
        } catch (Exception cex) {
            printUsageError("error reading configuration: " + cex.getMessage());
            System.exit(1);
        }

        checkRunning(config);

        // create new server instance
        server = new Server(config);

        // parse properties files etc
        server.init();

        // start the server main thread
        server.start();
    }


    /**
      * check if we are running on a Java 2 VM - otherwise exit with an error message
      */
    public static void checkJavaVersion() {
        String javaVersion = System.getProperty("java.version");

        if ((javaVersion == null) || javaVersion.startsWith("1.2")
                                  || javaVersion.startsWith("1.1")
                                  || javaVersion.startsWith("1.0")) {
            System.err.println("This version of Helma requires Java 1.3 or greater.");

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
      * @return Config if successfull
      * @throws Exception on any configuration error
      */
    public static Config getConfig(String[] args) throws Exception {

        Config config = new Config();

        // get possible environment setting for helma home
        if (System.getProperty("helma.home")!=null) {
            config.homeDir = new File(System.getProperty("helma.home"));
        }

        parseArgs(config, args);

        guessConfig(config);

        // create system properties
        ResourceProperties sysProps = new ResourceProperties();
        sysProps.addResource(new FileResource(config.propFile));

        // check if there's a property setting for those ports not specified via command line
        if ((config.websrvPort == null) && (sysProps.getProperty("webPort") != null)) {
            try {
                config.websrvPort = new InetAddrPort(sysProps.getProperty("webPort"));
            } catch (Exception portx) {
                throw new Exception("Error parsing web server port property from server.properties: " + portx);
            }
        }

        if ((config.ajp13Port == null) && (sysProps.getProperty("ajp13Port") != null)) {
            try {
                config.ajp13Port = new InetAddrPort(sysProps.getProperty("ajp13Port"));
            } catch (Exception portx) {
                throw new Exception("Error parsing AJP1.3 server port property from server.properties: " + portx);
            }
        }

        if ((config.rmiPort == null) && (sysProps.getProperty("rmiPort") != null)) {
            try {
                config.rmiPort = new InetAddrPort(sysProps.getProperty("rmiPort"));
            } catch (Exception portx) {
                throw new Exception("Error parsing RMI server port property from server.properties: " + portx);
            }
        }

        if ((config.xmlrpcPort == null) && (sysProps.getProperty("xmlrpcPort") != null)) {
            try {
                config.xmlrpcPort = new InetAddrPort(sysProps.getProperty("xmlrpcPort"));
            } catch (Exception portx) {
                throw new Exception("Error parsing XML-RPC server port property from server.properties: " + portx);
            }
        }
        return config;
    }


    /**
      * parse argument list from command line and store values
      * in given Config object
      * @throws Exception when argument can't be parsed into an InetAddrPort
      * or invalid token is given.
      */
    public static void parseArgs(Config config, String[] args) throws Exception {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-h") && ((i + 1) < args.length)) {
                config.homeDir = new File(args[++i]);
            } else if (args[i].equals("-f") && ((i + 1) < args.length)) {
                config.propFile = new File(args[++i]);
            } else if (args[i].equals("-p") && ((i + 1) < args.length)) {
                try {
                    config.rmiPort = new InetAddrPort(args[++i]);
                } catch (Exception portx) {
                    throw new Exception("Error parsing RMI server port property: " + portx);
                }
            } else if (args[i].equals("-x") && ((i + 1) < args.length)) {
                try {
                    config.xmlrpcPort = new InetAddrPort(args[++i]);
                } catch (Exception portx) {
                    throw new Exception("Error parsing XML-RPC server port property: " + portx);
                }
            } else if (args[i].equals("-w") && ((i + 1) < args.length)) {
                try {
                    config.websrvPort = new InetAddrPort(args[++i]);
                } catch (Exception portx) {
                    throw new Exception("Error parsing web server port property: " + portx);
                }
            } else if (args[i].equals("-jk") && ((i + 1) < args.length)) {
                try {
                    config.ajp13Port = new InetAddrPort(args[++i]);
                } catch (Exception portx) {
                    throw new Exception("Error parsing AJP1.3 server port property: " + portx);
                }
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
    public static void guessConfig(Config config) throws Exception {
        // get property file from hopHome:
        if (config.propFile == null) {
            if (config.homeDir != null) {
                config.propFile = new File(config.homeDir, "server.properties");
            } else {
                config.propFile = new File("server.properties");
            }
        }

        // create system properties
        ResourceProperties sysProps = new ResourceProperties();
        sysProps.addResource(new FileResource(config.propFile));

        // try to get hopHome from property file
        if (config.homeDir == null && sysProps.getProperty("hophome") != null) {
            config.homeDir = new File(sysProps.getProperty("hophome"));
        }

        // use the directory where server.properties is located:
        if (config.homeDir == null && config.propFile != null) {
            config.homeDir = config.propFile.getAbsoluteFile().getParentFile();
        }

        if (!config.hasPropFile()) {
            throw new Exception ("no server.properties found");
        }

        if (!config.hasHomeDir()) {
            throw new Exception ("couldn't determine helma directory");
        }

        // try to transform hopHome directory to its canonical representation
        try {
            config.homeDir = config.homeDir.getCanonicalFile();
        } catch (IOException iox) {
            config.homeDir = config.homeDir.getAbsoluteFile();
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
        System.out.println("  -h dir       Specify hop home directory");
        System.out.println("  -f file      Specify server.properties file");
        System.out.println("  -w [ip:]port      Specify embedded web server address/port");
        System.out.println("  -x [ip:]port      Specify XML-RPC address/port");
        System.out.println("  -jk [ip:]port     Specify AJP13 address/port");
        System.out.println("  -p [ip:]port      Specify RMI address/port");
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
    public static void checkRunning(Config config) {
        // check if any of the specified server ports is in use already
        try {
            if (config.websrvPort != null) {
                checkPort(config.websrvPort);
            }

            if (config.rmiPort != null) {
                checkPort(config.rmiPort);
            }

            if (config.xmlrpcPort != null) {
                checkPort(config.xmlrpcPort);
            }

            if (config.ajp13Port != null) {
                checkPort(config.ajp13Port);
            }
        } catch (Exception running) {
            System.out.println(running.getMessage());
            System.exit(1);
        }

    }


    /**
     *  A primitive method to check whether a server is already running on our port.
     */
    private static void checkPort(InetAddrPort addrPort) throws Exception {
        // checkRunning is disabled until we find a fix for the socket creation
        // timeout problems reported on the list.
        return;

        /*
        InetAddress addr = addrPort.getInetAddress();
        if (addr == null) {
            try {
                addr = InetAddress.getLocalHost();
            } catch (UnknownHostException unknown) {
                System.err.println("Error checking running server: localhost is unknown.");
                return;
            }
        }
        try {
            new Socket(addr, addrPort.getPort());
        } catch (IOException x) {
            // we couldn't connect to the socket because no server
            // is running on it yet. Everything's ok.
            return;
        }

        // if we got so far, another server is already running on this port and db
        throw new Exception("Error: Server already running on this port: " + addrPort);
        */
    }


    /**
      * initialize the server
      */
    public void init() {

        // set the log factory property
        String logFactory = sysProps.getProperty("loggerFactory",
                                                 "helma.util.Logging");

        helmaLogging = "helma.util.Logging".equals(logFactory);
        System.setProperty("org.apache.commons.logging.LogFactory", logFactory);

        // set the current working directory to the helma home dir.
        // note that this is not a real cwd, which is not supported
        // by java. It makes sure relative to absolute path name
        // conversion is done right, so for Helma code, this should
        // work.
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
        dbProps = new ResourceProperties();
        dbProps.addResource(new FileResource(new File(hopHome, "db.properties")));
        DbSource.setDefaultProps(dbProps);

        // read apps.properties file
        String appsPropfile = sysProps.getProperty("appsPropFile");
        File file;
        if ((appsPropfile != null) && !"".equals(appsPropfile.trim())) {
            file = new File(appsPropfile);
            appsProps = new ResourceProperties();
        } else {
            file = new File(hopHome, "apps.properties");
            appsProps = new ResourceProperties();
        }
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
        
        getLogger().info("Shutting down Helma");

        appManager.stopAll();

        if (http != null) {
            try {
                http.stop();
                http.destroy();
            } catch (InterruptedException irx) {
                // http.stop() interrupted by another thread. ignore.
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
            if ((websrvPort != null) || (ajp13Port != null)) {
                http = new HttpServer();

                // disable Jetty logging  FIXME: seems to be a jetty bug; as soon
                // as the logging is disabled, the more is logged
                org.mortbay.util.Log.instance().disableLog();
                org.mortbay.util.Log.instance().add(new HelmaLogSink());
            }

            // start embedded web server if port is specified
            if (websrvPort != null) {
                http.addListener(websrvPort);
            }

            // activate the ajp13-listener
            if (ajp13Port != null) {
                // create AJP13Listener
                ajp13 = new AJP13Listener(ajp13Port);
                ajp13.setHttpServer(http);

                String jkallow = sysProps.getProperty("allowAJP13");

                // by default the AJP13-connection just accepts requests from 127.0.0.1
                if (jkallow == null) {
                    jkallow = "127.0.0.1";
                }

                StringTokenizer st = new StringTokenizer(jkallow, " ,;");
                String[] jkallowarr = new String[st.countTokens()];
                int cnt = 0;

                while (st.hasMoreTokens()) {
                    jkallowarr[cnt] = st.nextToken();
                    cnt++;
                }

                ajp13.setRemoteServers(jkallowarr);
                logger.info("Starting AJP13-Listener on port " + (ajp13Port));
            }

            if (xmlrpcPort != null) {
                String xmlparser = sysProps.getProperty("xmlparser");

                if (xmlparser != null) {
                    XmlRpc.setDriver(xmlparser);
                }

                if (xmlrpcPort.getInetAddress() != null) {
                    xmlrpc = new WebServer(xmlrpcPort.getPort(), xmlrpcPort.getInetAddress());
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

                logger.info("Starting XML-RPC server on port " + (xmlrpcPort));
            }

            if (rmiPort != null) {
                if (paranoid) {
                    HelmaSocketFactory factory = new HelmaSocketFactory();
                    String rallow = sysProps.getProperty("allowWeb");
                    if (rallow == null) {
                        rallow = sysProps.getProperty("allowRMI");
                    }

                    if (rallow != null) {
                        StringTokenizer st = new StringTokenizer(rallow, " ,;");

                        while (st.hasMoreTokens())
                            factory.addAddress(st.nextToken());
                    }

                    RMISocketFactory.setSocketFactory(factory);
                }

                logger.info("Starting RMI server on port " + rmiPort);
                LocateRegistry.createRegistry(rmiPort.getPort());

                // create application manager which binds to the given RMI port
                appManager = new ApplicationManager(appsProps, this, rmiPort.getPort());
            } else {
                // create application manager with RMI port 0
                appManager = new ApplicationManager(appsProps, this, 0);
            }

            if (xmlrpc != null) {
                xmlrpc.addHandler("$default", appManager);
            }

            // add shutdown hook to close running apps and servers on exit
            shutdownhook = new HelmaShutdownHook();
            Runtime.getRuntime().addShutdownHook(shutdownhook);
        } catch (Exception gx) {
            logger.error("Error initializing embedded database: " + gx);
            gx.printStackTrace();

            return;
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
            logger.error("Error setting security manager: " + x);
        }

        // start applications
        appManager.startAll();

        // start embedded web server
        if (http != null) {
            try {
                http.start();
            } catch (MultiException m) {
                logger.error("Error starting embedded web server: " + m);
            }
        }

        if (ajp13 != null) {
            try {
                ajp13.start();
            } catch (Exception m) {
                logger.error("Error starting AJP13 listener: " + m);
            }
        }

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
    public void checkAppManager(int port) {
        if (appManager == null) {
            appManager = new ApplicationManager(appsProps, this, port);
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
     *
     *
     * @return ...
     */
    public ResourceProperties getProperties() {
        return sysProps;
    }

    /**
     *
     *
     * @return ...
     */
    public ResourceProperties getDbProperties() {
        return dbProps;
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

    /**
     * method from helma.framework.IPathElement
     */
    public String getElementName() {
        return "root";
    }

    /**
     * method from helma.framework.IPathElement,
     * returning active applications
     */
    public IPathElement getChildElement(String name) {
        return appManager.getApplication(name);
    }

    /**
     * method from helma.framework.IPathElement
     */
    public IPathElement getParentElement() {
        return null;
    }

    /**
     * method from helma.framework.IPathElement
     */
    public String getPrototype() {
        return "root";
    }

    static class HelmaLogSink implements LogSink {
                        
        public String getOptions() {
            return null;
        }

        public void log(String formattedLog) {
        }

        public void log(String tag, Object msg, Frame frame, long time) {
        }

        public void setOptions(String options) {
        }

        public boolean isStarted() {
            return true;
        }

        public void start() {
        }

        public void stop() {
        }
    }
}


