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
import helma.framework.core.*;
import helma.objectmodel.db.DbSource;
import helma.util.*;
import org.apache.xmlrpc.*;
import org.mortbay.http.*;
import org.mortbay.http.ajp.*;
import org.mortbay.util.*;
import java.io.*;
import java.net.*;
import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;
import java.util.*;

/**
 * Helma server main class.
 */
public class Server implements IPathElement, Runnable {
    public static final String version = "1.3-alpha-0 (2003/06/17)";

    // server-wide properties
    static SystemProperties appsProps;
    static SystemProperties dbProps;
    static SystemProperties sysProps;

    // static server instance
    private static Server server;
    protected static File hopHome = null;

    // our logger
    private static Logger logger;
    public final long starttime;

    // if true we only accept RMI and XML-RPC connections from 
    // explicitly listed hosts.
    public boolean paranoid;
    private ApplicationManager appManager;
    private Vector extensions;
    private Thread mainThread;

    // server ports
    int rmiPort = 0;
    int xmlrpcPort = 0;
    int websrvPort = 0;
    int ajp13Port = 0;

    // map of server-wide database sources
    Hashtable dbSources;

    // the embedded web server
    // protected Serve websrv;
    protected HttpServer http;

    // the AJP13 Listener, used for connecting from external webserver to servlet via JK
    protected AJP13Listener ajp13;

    // the XML-RPC server
    protected WebServer xmlrpc;

    /**
     * Constructs a new Server instance with an array of command line options.
     */
    public Server(String[] args) {
        starttime = System.currentTimeMillis();

        String homeDir = null;

        boolean usageError = false;

        // file names of various property files
        String propfile = null;
        String dbPropfile = "db.properties";
        String appsPropfile = null;

        // parse arguments
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-h") && ((i + 1) < args.length)) {
                homeDir = args[++i];
            } else if (args[i].equals("-f") && ((i + 1) < args.length)) {
                propfile = args[++i];
            } else if (args[i].equals("-p") && ((i + 1) < args.length)) {
                try {
                    rmiPort = Integer.parseInt(args[++i]);
                } catch (Exception portx) {
                    usageError = true;
                }
            } else if (args[i].equals("-x") && ((i + 1) < args.length)) {
                try {
                    xmlrpcPort = Integer.parseInt(args[++i]);
                } catch (Exception portx) {
                    usageError = true;
                }
            } else if (args[i].equals("-w") && ((i + 1) < args.length)) {
                try {
                    websrvPort = Integer.parseInt(args[++i]);
                } catch (Exception portx) {
                    usageError = true;
                }
            } else if (args[i].equals("-jk") && ((i + 1) < args.length)) {
                try {
                    ajp13Port = Integer.parseInt(args[++i]);
                } catch (Exception portx) {
                    usageError = true;
                }
            } else if (args[i].equals("-i") && ((i + 1) < args.length)) {
                // eat away the -i parameter which is meant for helma.main.launcher.Main
                i++;
            } else {
                System.err.println("Unknown command line token: " + args[i]);
                usageError = true;
            }
        }

        // get main property file from home dir or vice versa, depending on what we have.
        // get property file from hopHome
        if (propfile == null) {
            if (homeDir != null) {
                propfile = new File(homeDir, "server.properties").getAbsolutePath();
            } else {
                propfile = new File("server.properties").getAbsolutePath();
            }
        }

        // create system properties
        sysProps = new SystemProperties(propfile);

        // check if there's a property setting for those ports not specified via command line
        if ((websrvPort == 0) && (sysProps.getProperty("webPort") != null)) {
            try {
                websrvPort = Integer.parseInt(sysProps.getProperty("webPort"));
            } catch (NumberFormatException fmt) {
                System.err.println("Error parsing web server port property: " + fmt);
            }
        }

        if ((ajp13Port == 0) && (sysProps.getProperty("ajp13Port") != null)) {
            try {
                ajp13Port = Integer.parseInt(sysProps.getProperty("ajp13Port"));
            } catch (NumberFormatException fmt) {
                System.err.println("Error parsing AJP1.3 server port property: " + fmt);
            }
        }

        if ((rmiPort == 0) && (sysProps.getProperty("rmiPort") != null)) {
            try {
                rmiPort = Integer.parseInt(sysProps.getProperty("rmiPort"));
            } catch (NumberFormatException fmt) {
                System.err.println("Error parsing RMI server port property: " + fmt);
            }
        }

        if ((xmlrpcPort == 0) && (sysProps.getProperty("xmlrpcPort") != null)) {
            try {
                xmlrpcPort = Integer.parseInt(sysProps.getProperty("xmlrpcPort"));
            } catch (NumberFormatException fmt) {
                System.err.println("Error parsing XML-RPC server port property: " + fmt);
            }
        }

        // check server ports. If no port is set, issue a warning and exit.
        if (!usageError && ((websrvPort | ajp13Port | rmiPort | xmlrpcPort) == 0)) {
            System.out.println("  Error: No server port specified.");
            usageError = true;
        }

        // if there's a usage error, output message and exit
        if (usageError) {
            System.out.println("usage: java helma.main.Server [-h dir] [-f file] [-p port] [-w port] [-x port]");
            System.out.println("  -h dir     Specify hop home directory");
            System.out.println("  -f file    Specify server.properties file");
            System.out.println("  -p port    Specify RMI port number");
            System.out.println("  -w port    Specify port number for embedded Web server");
            System.out.println("  -x port    Specify XML-RPC port number");
            System.out.println("  -jk port   Specify AJP13 port number");
            System.err.println("Usage Error - exiting");
            System.exit(0);
        }

        // check if any of the specified server ports is in use already
        try {
            if (websrvPort > 0) {
                checkRunning(websrvPort);
            }

            if (rmiPort > 0) {
                checkRunning(rmiPort);
            }

            if (xmlrpcPort > 0) {
                checkRunning(xmlrpcPort);
            }

            if (ajp13Port > 0) {
                checkRunning(ajp13Port);
            }
        } catch (Exception running) {
            System.out.println(running.getMessage());
            System.exit(1);
        }

        // get hopHome from property file
        if (homeDir == null) {
            homeDir = sysProps.getProperty("hophome");
        }

        if (homeDir == null) {
            homeDir = new File(propfile).getParent();
        }

        // create hopHome File object
        hopHome = new File(homeDir);

        // try to transform hopHome directory to its cononical representation
        try {
            hopHome = hopHome.getCanonicalFile();
        } catch (IOException iox) {
            System.err.println("Error calling getCanonicalFile() on hopHome: " + iox);
        }

        // set the current working directory to the helma home dir.
        // note that this is not a real cwd, which is not supported
        // by java. It makes sure relative to absolute path name
        // conversion is done right, so for Helma code, this should
        // work.
        System.setProperty("user.dir", hopHome.getPath());

        // from now on it's safe to call getLogger() because hopHome is set up
        String startMessage = "Starting Helma " + version + " on Java " +
                              System.getProperty("java.version");

        getLogger().log(startMessage);

        // also print a msg to System.out
        System.out.println(startMessage);

        getLogger().log("propfile = " + propfile);
        getLogger().log("hopHome = " + hopHome);

        File helper = new File(hopHome, "db.properties");

        dbPropfile = helper.getAbsolutePath();
        dbProps = new SystemProperties(dbPropfile);
        DbSource.setDefaultProps(dbProps);
        getLogger().log("dbPropfile = " + dbPropfile);

        appsPropfile = sysProps.getProperty("appsPropFile");

        if ((appsPropfile != null) && !"".equals(appsPropfile.trim())) {
            helper = new File(appsPropfile);
        } else {
            helper = new File(hopHome, "apps.properties");
        }

        appsPropfile = helper.getAbsolutePath();
        appsProps = new SystemProperties(appsPropfile);
        getLogger().log("appsPropfile = " + appsPropfile);

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

        getLogger().log("Locale = " + Locale.getDefault());
        getLogger().log("TimeZone = " +
                        TimeZone.getDefault().getDisplayName(Locale.getDefault()));

        dbSources = new Hashtable();

        // try to load the extensions
        extensions = new Vector();

        if (sysProps.getProperty("extensions") != null) {
            StringTokenizer tok = new StringTokenizer(sysProps.getProperty("extensions"),
                                                      ",");

            while (tok.hasMoreTokens()) {
                String extClassName = tok.nextToken().trim();

                try {
                    Class extClass = Class.forName(extClassName);
                    HelmaExtension ext = (HelmaExtension) extClass.newInstance();

                    ext.init(this);
                    extensions.add(ext);
                    getLogger().log("loaded: " + extClassName);
                } catch (Exception e) {
                    getLogger().log("error:  " + extClassName + " (" + e.toString() +
                                    ")");
                }
            }
        }
    }

    /**
     *  static main entry point.
     */
    public static void main(String[] args) throws IOException {
        // check if we are running on a Java 2 VM - otherwise exit with an error message
        String javaVersion = System.getProperty("java.version");

        if ((javaVersion == null) || javaVersion.startsWith("1.1") ||
                javaVersion.startsWith("1.0")) {
            System.err.println("This version of Helma requires Java 1.2 or greater.");

            if (javaVersion == null) { // don't think this will ever happen, but you never know
                System.err.println("Your Java Runtime did not provide a version number. Please update to a more recent version.");
            } else {
                System.err.println("Your Java Runtime is version " + javaVersion +
                                   ". Please update to a more recent version.");
            }

            System.exit(1);
        }

        // create new server instance
        server = new Server(args);

        // start the server main thread
        server.start();
    }

    protected void start() {
        // Start running, finishing setup and then entering a loop to check changes
        // in the apps.properties file.
        mainThread = new Thread(this);
        mainThread.start();
    }

    protected void stop() {
        mainThread = null;
    }

    /**
     *  The main method of the Server. Basically, we set up Applications and than
     *  periodically check for changes in the apps.properties file, shutting down
     *  apps or starting new ones.
     */
    public void run() {
        try {
            if ((websrvPort > 0) || (ajp13Port > 0)) {
                http = new HttpServer();

                // disable Jetty logging  FIXME: seems to be a jetty bug; as soon
                // as the logging is disabled, the more is logged
                Log.instance().disableLog();
                Log.instance().add(new LogSink() {
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
                    });
            }

            // start embedded web server if port is specified
            if (websrvPort > 0) {
                http.addListener(new InetAddrPort(websrvPort));
            }

            // activate the ajp13-listener
            if (ajp13Port > 0) {
                // create AJP13Listener
                ajp13 = new AJP13Listener(new InetAddrPort(ajp13Port));
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
                getLogger().log("Starting AJP13-Listener on port " + (ajp13Port));
            }

            if (xmlrpcPort > 0) {
                String xmlparser = sysProps.getProperty("xmlparser");

                if (xmlparser != null) {
                    XmlRpc.setDriver(xmlparser);
                }

                xmlrpc = new WebServer(xmlrpcPort);

                if (paranoid) {
                    xmlrpc.setParanoid(true);

                    String xallow = sysProps.getProperty("allowXmlRpc");

                    if (xallow != null) {
                        StringTokenizer st = new StringTokenizer(xallow, " ,;");

                        while (st.hasMoreTokens())
                            xmlrpc.acceptClient(st.nextToken());
                    }
                }

                getLogger().log("Starting XML-RPC server on port " + (xmlrpcPort));
            }

            if (rmiPort > 0) {
                if (paranoid) {
                    HelmaSocketFactory factory = new HelmaSocketFactory();
                    String rallow = sysProps.getProperty("allowWeb");

                    if (rallow != null) {
                        StringTokenizer st = new StringTokenizer(rallow, " ,;");

                        while (st.hasMoreTokens())
                            factory.addAddress(st.nextToken());
                    }

                    RMISocketFactory.setSocketFactory(factory);
                }

                getLogger().log("Starting RMI server on port " + rmiPort);
                LocateRegistry.createRegistry(rmiPort);
            }

            // create application manager
            appManager = new ApplicationManager(rmiPort, hopHome, appsProps, this);

            if (xmlrpc != null) {
                xmlrpc.addHandler("$default", appManager);
            }

            // add shutdown hook to close running apps and servers on exit
            Runtime.getRuntime().addShutdownHook(new HelmaShutdownHook(appManager));
        } catch (Exception gx) {
            getLogger().log("Error initializing embedded database: " + gx);
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
                getLogger().log("Setting security manager to " + secManClass);
            }
        } catch (Exception x) {
            getLogger().log("Error setting security manager: " + x);
        }

        // start applications
        appManager.startAll();

        // start embedded web server
        if (http != null) {
            try {
                http.start();
            } catch (MultiException m) {
                getLogger().log("Error starting embedded web server: " + m);
            }
        }

        if (ajp13 != null) {
            try {
                ajp13.start();
            } catch (Exception m) {
                getLogger().log("Error starting AJP13 listener: " + m);
            }
        }

        int count = 0;

        while (Thread.currentThread() == mainThread) {
            try {
                mainThread.sleep(3000L);
            } catch (InterruptedException ie) {
            }

            try {
                appManager.checkForChanges();
            } catch (Exception x) {
                getLogger().log("Caught in app manager loop: " + x);
            }
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
    public static Logger getLogger() {
        if (logger == null) {
            String logDir = sysProps.getProperty("logdir", "log");

            if ("console".equalsIgnoreCase(logDir)) {
                logger = new Logger(System.out);
            } else {
                File helper = new File(logDir);

                if ((hopHome != null) && !helper.isAbsolute()) {
                    helper = new File(hopHome, logDir);
                }

                logDir = helper.getAbsolutePath();
                logger = Logger.getLogger(logDir, "hop");
            }
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
     *  A primitive method to check whether a server is already running on our port.
     */
    private void checkRunning(int portNumber) throws Exception {
        try {
            java.net.Socket socket = new java.net.Socket("localhost", portNumber);
        } catch (Exception x) {
            return;
        }

        // if we got so far, another server is already running on this port and db
        throw new Exception("Error: Server already running on this port: " + portNumber);
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
    public SystemProperties getProperties() {
        return sysProps;
    }

    /**
     *
     *
     * @return ...
     */
    public SystemProperties getDbProperties() {
        return dbProps;
    }

    /**
     *
     *
     * @return ...
     */
    public File getAppsHome() {
        String appHome = sysProps.getProperty("appHome");

        if ((appHome != null) && !"".equals(appHome.trim())) {
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
}
