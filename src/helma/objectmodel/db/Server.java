// Server.java
// Copyright (c) Hannes Wallnöfer 1998-2000
 
package helma.objectmodel.db;

import java.util.*;
import java.io.*;
import java.rmi.*;
import java.rmi.server.*;
import java.rmi.registry.*;
import java.net.*;
import helma.objectmodel.*;
import helma.framework.*;
import helma.framework.core.*;
import helma.xmlrpc.*;
import helma.util.*;
import com.sleepycat.db.*;


/**
 * HOP main class.
 */
 
 public class Server extends IServer implements Runnable {


    static boolean useTransactions, paranoid;
    
    private ApplicationManager appManager;

    private  Thread mainThread;

    protected static ThreadGroup txgroup;

    static String dbFilename = "hop.db";
    static String propfile;
    static String dbPropfile = "db.properties";
    static String appsPropfile = "apps.properties";
    static SystemProperties appsProps;
    static String dbDir = null;
    static int port = 5055;
    static int webport = 0;

    Acme.Serve.Serve websrv;

    public static void main (String args[]) throws IOException {

	boolean usageError = false;

	useTransactions = true;
	
	for (int i=0; i<args.length; i++) {
	    if (args[i].equals ("-h") && i+1<args.length)
	        hopHome = args[++i];
	    else if (args[i].equals ("-f") && i+1<args.length)
	        propfile = args[++i];
	    else if (args[i].equals ("-t"))
	        useTransactions = false;
	    else if (args[i].equals ("-p") && i+1<args.length) {
	        try {
	            port = Integer.parseInt (args[++i]);
	        } catch (Exception portx) {
	             usageError = true;
	        }
	    } else if (args[i].equals ("-w") && i+1<args.length) {
	        try {
	            webport = Integer.parseInt (args[++i]);
	        } catch (Exception portx) {
	             usageError = true;
	        }
	    } else
	        usageError = true;
	}

	// get main property file from home dir or vice versa, depending on what we have.
	// get property file from hopHome
	if (propfile == null) {
	    if (hopHome != null)
	        propfile = new File (hopHome, "server.properties").getAbsolutePath ();
	    else
	        propfile = new File ("server.properties").getAbsolutePath ();
	}
	sysProps = new SystemProperties (propfile);
	getLogger().log ("propfile = "+propfile);
	// get hopHome from property file
	if (hopHome == null)
	    hopHome = sysProps.getProperty ("hophome");
	if (hopHome == null)
	    hopHome =  new File (propfile).getParent ();

	getLogger().log ("hopHome = "+hopHome);

	if (usageError ) {
	    System.out.println ("usage: java helma.objectmodel.db.Server [-h dir] [-f file] [-p port] [-w port] [-t]");
	    System.out.println ("  -h dir     Specify hop home directory");
	    System.out.println ("  -f file    Specify server.properties file");
	    System.out.println ("  -p port    Specify TCP port number");
	    System.out.println ("  -w port    Start embedded Web server on that port");
	    System.out.println ("  -t         Disable Berkeley DB Transactions");
	    getLogger().log ("Usage Error - exiting");
	    System.exit (0);
	}

	dbDir = sysProps.getProperty ("dbhome", "db");
	File helper = new File (dbDir);
	if (hopHome != null && !helper.isAbsolute ())
	    helper = new File (hopHome, dbDir);
	dbDir = helper.getAbsolutePath ();
	getLogger().log ("dbHome = "+dbDir);

	dbPropfile = sysProps.getProperty ("dbpropfile", "db.properties");
	helper = new File (dbPropfile);
	if (hopHome != null && !helper.isAbsolute ())
	    helper = new File (hopHome, dbPropfile);
	dbPropfile = helper.getAbsolutePath ();
	getLogger().log ("dbPropfile = "+dbPropfile);

	appsPropfile = sysProps.getProperty ("appspropfile", "apps.properties");
	helper = new File (appsPropfile);
	if (hopHome != null && !helper.isAbsolute ())
	    helper = new File (hopHome, appsPropfile);
	appsPropfile = helper.getAbsolutePath ();
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
	getLogger().log ("TimeZone = "+TimeZone.getDefault());

	dbSources = new Hashtable ();
             txgroup = new ThreadGroup ("Transactor");

	new Server ();

    }

    public Server () {

	try {
	    checkRunning ();  // check if a server is already running with this db
	} catch (Exception running) {
	    System.out.println (running.getMessage ());
	    System.exit (1);
	}

	// nmgr = new NodeManager (this, sysProps);

	mainThread = new Thread (this);
	mainThread.start ();
    }

    public void run () {

	try {

	    // set up dbSources
	    try {
	        dbProps = new SystemProperties (dbPropfile);
	        String sources = dbProps.getProperty ("sources", "");
	        StringTokenizer st = new StringTokenizer (sources, ",; ");
	        String next = null;
	        while (st.hasMoreTokens ()) try {
	            next = st.nextToken ();
	            new DbSource (next);
	        } catch (Exception wrong) {
	            getLogger().log ("Error creating DbSource "+next);
	            getLogger().log ("Reason: "+wrong);
	        }
	    } catch (Exception x) {
	        getLogger().log ("Error loading data source properties: "+x);
	    }

	    // start embedded web server if port is specified
	    if (webport > 0) {
	        websrv = new Acme.Serve.Serve (webport, sysProps);
	    }

                 String xmlparser = sysProps.getProperty ("xmlparser");
	    if (xmlparser != null)
	        XmlRpc.setDriver (xmlparser);
	    // XmlRpc.setDebug (true);
	    xmlrpc = new WebServer (port+1);
	    if (paranoid) {
	        xmlrpc.setParanoid (true);
	        String xallow = sysProps.getProperty ("allowXmlRpc");
	        if (xallow != null) {
	            StringTokenizer st = new StringTokenizer (xallow, " ,;");
	            while (st.hasMoreTokens ())
	                xmlrpc.acceptClient (st.nextToken ());
	        }
	    }
	    getLogger().log ("Starting XML-RPC server on port "+(port+1));

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

	    if (websrv == null) {
	        getLogger().log ("Starting server on port "+port);
	        LocateRegistry.createRegistry (port);
	    }


	    // start application framework
	    String appDir = sysProps.getProperty ("apphome", "apps");
	    File appHome = new File (appDir);
	    if (hopHome != null && !appHome.isAbsolute())
	        appHome = new File (hopHome, appDir);
	    appsProps = new SystemProperties (appsPropfile);
	    File dbHome = new File (dbDir);
	    appManager = new ApplicationManager (port, appHome, dbHome, appsProps, this);


	} catch (Exception gx) {
	    getLogger().log ("Error initializing embedded database: "+gx);
	    gx.printStackTrace ();
	    /* try {
	        transactor.abort ();
	    } catch (Exception ignore) {} */
	    return;
             }

	// start applications
	appManager.startAll ();

	// start embedded web server
	if (websrv != null) {
	    Thread webthread = new Thread (websrv, "WebServer");
	    webthread.start ();
	}

	int count = 0;
	while (Thread.currentThread () == mainThread) {
	    try {
	        mainThread.sleep (3000l);
	    } catch (InterruptedException ie) {
	        return;
	    }
	    appManager.checkForChanges ();
	    // print some thread stats now and then
	    if (count++ > 20) {
	        printThreadStats ();
	        count = 0;
	    }
	}

    }


    private void checkRunning () throws Exception {
	try {
	    java.net.Socket socket = new java.net.Socket ("localhost", port);
	} catch (Exception x) {
	    return;
	}
	// if we got so far, another server is already running on this port and db
	throw new Exception ("Error: Server already running on this port");
    }

    public void printThreadStats () {
	getLogger().log ("Thread Stats: "+txgroup.activeCount()+" active");
    	Runtime rt = Runtime.getRuntime ();
	long free = rt.freeMemory ();
	long total = rt.totalMemory ();
	getLogger().log ("Free memory: "+(free/1024)+" kB");
	getLogger().log ("Total memory: "+(total/1024)+" kB");
    }

}




































































