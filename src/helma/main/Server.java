// Server.java
// Copyright (c) Hannes Wallnöfer 1998-2000
 
package helma.main;

import java.io.*;
import java.rmi.*;
import java.rmi.server.*;
import java.rmi.registry.*;
import java.net.*;
import java.util.*;
import helma.extensions.HelmaExtension;
import helma.objectmodel.db.DbSource;
import helma.framework.*;
import helma.framework.core.*;
import helma.xmlrpc.*;
import helma.util.*;
// import Acme.Serve.Serve;
import org.mortbay.http.*;
import org.mortbay.util.*;


/**
 * Helma server main class.
 */

 public class Server implements IPathElement, Runnable {

    public static final String version = "1.2pre3+ 2002/08/01";
    public long starttime;

    // if true we only accept RMI and XML-RPC connections from 
    // explicitly listed hosts.
    public boolean paranoid;

    private ApplicationManager appManager;

    private Vector extensions;

    private Thread mainThread;

    // server-wide properties
    static SystemProperties appsProps;
    static SystemProperties dbProps;
    static SystemProperties sysProps;

    // server ports
    int rmiPort = 5055;
    int xmlrpcPort = 5056;
    int websrvPort = 0;

    // map of server-wide database sources
    Hashtable dbSources;

    // static server instance
    private static Server server;

    protected static File hopHome = null;

    // our logger
    private static Logger logger;

    // the embedded web server
    // protected Serve websrv;
    protected HttpServer http;

    // the XML-RPC server
    protected WebServer xmlrpc;



    /**
     *  static main entry point.
     */
    public static void main (String args[]) throws IOException {

	// check if we are running on a Java 2 VM - otherwise exit with an error message
	String javaVersion = System.getProperty ("java.version");
	if (javaVersion == null || javaVersion.startsWith ("1.1") || javaVersion.startsWith ("1.0")) {
	    System.err.println ("This version of Helma requires Java 1.2 or greater.");
	    if (javaVersion == null) // don't think this will ever happen, but you never know
	        System.err.println ("Your Java Runtime did not provide a version number. Please update to a more recent version.");
	    else
	        System.err.println ("Your Java Runtime is version "+javaVersion+". Please update to a more recent version.");
	    System.exit (1);
	}

	// create new server instance
	server = new Server (args);
    }

    /**
     * Constructs a new Server instance with an array of command line options.
     */
    public Server (String[] args) {

	starttime = System.currentTimeMillis();
	String homeDir = null;

	boolean usageError = false;

	// file names of various property files
	String propfile = null;
	String dbPropfile = "db.properties";
	String appsPropfile = null;

	// parse arguments
	for (int i=0; i<args.length; i++) {
	    if (args[i].equals ("-h") && i+1<args.length)
	        homeDir = args[++i];
	    else if (args[i].equals ("-f") && i+1<args.length)
	        propfile = args[++i];
	    else if (args[i].equals ("-p") && i+1<args.length) {
	        try {
	            rmiPort = Integer.parseInt (args[++i]);
	        } catch (Exception portx) {
	            usageError = true;
	        }
	    } else if (args[i].equals ("-x") && i+1<args.length) {
	        try {
	            xmlrpcPort = Integer.parseInt (args[++i]);
	        } catch (Exception portx) {
	            usageError = true;
	        }
	    } else if (args[i].equals ("-w") && i+1<args.length) {
	        try {
	            websrvPort = Integer.parseInt (args[++i]);
	        } catch (Exception portx) {
	            usageError = true;
	        }
	    } else
	        usageError = true;
	}

	if (usageError ) {
	    System.out.println ("usage: java helma.objectmodel.db.Server [-h dir] [-f file] [-p port] [-w port] [-x port]");
	    System.out.println ("  -h dir     Specify hop home directory");
	    System.out.println ("  -f file    Specify server.properties file");
	    System.out.println ("  -p port    Specify RMI port number");
	    System.out.println ("  -w port    Specify port number for embedded Web server");
	    System.out.println ("  -x port    Specify XML-RPC port number");
	    getLogger().log ("Usage Error - exiting");
	    System.exit (0);
	}

	// get main property file from home dir or vice versa, depending on what we have.
	// get property file from hopHome
	if (propfile == null) {
	    if (homeDir != null)
	        propfile = new File (homeDir, "server.properties").getAbsolutePath ();
	    else
	        propfile = new File ("server.properties").getAbsolutePath ();
	}
	// create system properties
	sysProps = new SystemProperties (propfile);

	// get hopHome from property file
	if (homeDir == null)
	    homeDir = sysProps.getProperty ("hophome");
	if (homeDir == null)
	    homeDir =  new File (propfile).getParent ();

	// create hopHome File object
	hopHome = new File (homeDir);
	// try to transform hopHome directory to its cononical representation
	try {
	    hopHome = hopHome.getCanonicalFile ();
	} catch (IOException iox) {
	    System.err.println ("Error calling getCanonicalFile() on hopHome: "+iox);
	}

	// from now on it's safe to call getLogger()

	String startMessage = "Starting Helma "+version+
		" on Java "+System.getProperty ("java.version");
	getLogger().log (startMessage);
	// also print a msg to System.out
	System.out.println (startMessage);

	getLogger().log ("propfile = "+propfile);
	getLogger().log ("hopHome = "+hopHome);


	File helper = new File (hopHome, "db.properties");
	dbPropfile = helper.getAbsolutePath ();
	dbProps = new SystemProperties (dbPropfile);
	DbSource.setDefaultProps (dbProps);
	getLogger().log ("dbPropfile = "+dbPropfile);

	appsPropfile = sysProps.getProperty ("appsPropFile");
	if (appsPropfile != null && !"".equals (appsPropfile.trim()))
	    helper = new File (appsPropfile);
	else
	    helper = new File (hopHome, "apps.properties");
	appsPropfile = helper.getAbsolutePath ();
	appsProps = new SystemProperties (appsPropfile);
	getLogger().log ("appsPropfile = "+appsPropfile);

	paranoid = "true".equalsIgnoreCase (sysProps.getProperty ("paranoid"));

	String language = sysProps.getProperty ("language");
	String country = sysProps.getProperty ("country");
	String timezone = sysProps.getProperty ("timezone");

	if (language != null && country != null)
	    Locale.setDefault (new Locale (language, country));
	if (timezone != null)
	    TimeZone.setDefault (TimeZone.getTimeZone (timezone));

	getLogger().log ("Locale = "+Locale.getDefault());
	getLogger().log ("TimeZone = "+TimeZone.getDefault().getDisplayName (Locale.getDefault ()));

	dbSources = new Hashtable ();

	try {
	     // check if servers are already running on the given ports
	    if (websrvPort==0)
	        checkRunning (rmiPort);
	    else
	        checkRunning (websrvPort);
	    checkRunning (xmlrpcPort);
	} catch (Exception running) {
	    System.out.println (running.getMessage ());
	    System.exit (1);
	}

	// nmgr = new NodeManager (this, sysProps);
	// try to load the extensions
	extensions = new Vector ();
	if (sysProps.getProperty ("extensions")!=null) {
	    StringTokenizer tok=new StringTokenizer (sysProps.getProperty ("extensions"),",");
	    while(tok.hasMoreTokens ()) {
	        String extClassName = tok.nextToken ().trim ();
	        try {
	            Class extClass = Class.forName (extClassName);
	            HelmaExtension ext = (HelmaExtension) extClass.newInstance ();
	            ext.init (this);
	            extensions.add (ext);
	            getLogger ().log ("loaded: " + extClassName);
	        } catch (Exception e) {
	            getLogger ().log ("error:  " + extClassName + " (" + e.toString () + ")");
	        }
	    }
	}

	// Start running, finishing setup and then entering a loop to check changes
	// in the apps.properties file.
	mainThread = new Thread (this);
	mainThread.start ();
    }

    /**
     *  The main method of the Server. Basically, we set up Applications and than
     *  periodically check for changes in the apps.properties file, shutting down
     *  apps or starting new ones.
     */
    public void run () {

	try {

	    // start embedded web server if port is specified
	    if (websrvPort > 0) {
	        // websrv = new Acme.Serve.Serve (websrvPort, sysProps);
	       // disable Jetty logging
	       Log.instance().disableLog ();
	       // create new Jetty server and bind it to the web server port
	       http = new HttpServer ();
	       http.addListener (new InetAddrPort (websrvPort));
	       // http.setRequestLogSink (new OutputStreamLogSink ());
	    }

	    String xmlparser = sysProps.getProperty ("xmlparser");
	    if (xmlparser != null)
	        XmlRpc.setDriver (xmlparser);
	    // XmlRpc.setDebug (true);
	    xmlrpc = new WebServer (xmlrpcPort);
	    if (paranoid) {
	        xmlrpc.setParanoid (true);
	        String xallow = sysProps.getProperty ("allowXmlRpc");
	        if (xallow != null) {
	            StringTokenizer st = new StringTokenizer (xallow, " ,;");
	            while (st.hasMoreTokens ())
	                xmlrpc.acceptClient (st.nextToken ());
	        }
	    }
	    getLogger().log ("Starting XML-RPC server on port "+(xmlrpcPort));

	    // the following seems not to be necessary after all ...
	    // System.setSecurityManager(new RMISecurityManager());
	    if (paranoid) {
	        HopSocketFactory factory = new HopSocketFactory ();
	        String rallow = sysProps.getProperty ("allowWeb");
	        if (rallow != null) {
	            StringTokenizer st = new StringTokenizer (rallow, " ,;");
	            while (st.hasMoreTokens ())
	                factory.addAddress (st.nextToken ());
	        }
	        RMISocketFactory.setSocketFactory (factory);
	    }

	    if (http == null) {
	        getLogger().log ("Starting RMI server on port "+rmiPort);
	        LocateRegistry.createRegistry (rmiPort);
	    }


	    // create application manager
	    appManager = new ApplicationManager (rmiPort, hopHome, appsProps, this);


	} catch (Exception gx) {
	    getLogger().log ("Error initializing embedded database: "+gx);
	    gx.printStackTrace ();
	    return;
	}

	// start applications
	appManager.startAll ();

	// start embedded web server
	/* if (websrv != null) {
	    Thread webthread = new Thread (websrv, "WebServer");
	    webthread.start ();
	} */
	if (http != null) try {
	    http.start ();
	} catch (MultiException m) {
	    getLogger().log ("Error starting embedded web server: "+m);
	}

	int count = 0;
	while (Thread.currentThread () == mainThread) {
	    try {
	        mainThread.sleep (3000l);
	    } catch (InterruptedException ie) {}
	    try {
	        appManager.checkForChanges ();
	    } catch (Exception x) {
	        getLogger().log ("Caught in app manager loop: "+x);
	    }
	}

    }

    /**
     *  Get an Iterator over the applications currently running on this Server.
     */
    public Object[] getApplications () {
	return appManager.getApplications ();
    }

    /**
     * Get an Application by name
     */
    public Application getApplication(String name)	{
	return appManager.getApplication(name);
    }

    /**
     *  Get a logger to use for output in this server.
     */
    public static Logger getLogger () {
	if (logger == null) {
	    String logDir = sysProps.getProperty ("logdir", "log");
	    if ("console".equalsIgnoreCase (logDir)) {
	        logger = new Logger (System.out);
	    } else {
	        File helper = new File (logDir);
	        if (hopHome != null && !helper.isAbsolute ())
	            helper = new File (hopHome, logDir);
	        logDir = helper.getAbsolutePath ();
	        logger = Logger.getLogger (logDir, "hop");
	    }
	}
	return logger;
    }

    /**
     *  Get the Home directory of this server.
     */
    public File getHopHome () {
	return hopHome;
    }

    /**
     * Get the main Server instance.
     */
    public static Server getServer()	{
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
    private void checkRunning (int portNumber) throws Exception {
	try {
	    java.net.Socket socket = new java.net.Socket ("localhost", portNumber);
	} catch (Exception x) {
	    return;
	}
	// if we got so far, another server is already running on this port and db
	throw new Exception ("Error: Server already running on this port: " + portNumber);
    }

	public String getProperty( String key )	{
		return (String)sysProps.get(key);
	}

	public SystemProperties getProperties()	{
		return sysProps;
	}
	
	public SystemProperties getDbProperties() {
		return dbProps;
	}

	public File getAppsHome()	{
		String appHome = sysProps.getProperty ("appHome");
		if (appHome != null && !"".equals (appHome.trim()))
		    return new File (appHome);
		else
	    	return new File (hopHome, "apps");
	}

	public Vector getExtensions () {
	    return extensions;
	}

	public void startApplication(String name)	{
		appManager.start (name);
		appManager.register (name);
	}

	public void stopApplication(String name)	{
		appManager.stop (name);
	}

	/**
	  * method from helma.framework.IPathElement
	  */
	public String getElementName()	{
		return "root";
	}

	/**
	  * method from helma.framework.IPathElement,
	  * returning active applications
	  */
	public IPathElement getChildElement(String name)	{
		return appManager.getApplication(name);
	}

	/**
	  * method from helma.framework.IPathElement
	  */
	public IPathElement getParentElement()	{
		return null;
	}

	/**
	  * method from helma.framework.IPathElement
	  */
	public String getPrototype()	{
		return "root";
	}
}
