// IServer.java
// Copyright (c) Hannes Wallnöfer 1998-2000
 
package helma.objectmodel;

import helma.util.*;
import helma.xmlrpc.WebServer;
import java.util.*;
import java.io.*;

/**
 * Abstract Server class. Defines the methods all servers have to implement.
 */
 
public abstract class IServer {

    // public static final Object sync = new Object ();
    public static SystemProperties sysProps, dbProps;
    public static Hashtable dbSources;

    protected static File hopHome = null;

    private static Logger logger;

    protected static WebServer xmlrpc;


   /* public abstract INode getAppRoot (String appID);

    public abstract INode getAppNode (String appID, Vector path, String name);

    public abstract INode getSubnode (String path); */

    public static void throwNodeEvent (NodeEvent evt) {
	// noop
    }

    public static void addNodeListener (String id, INodeListener listener) {
	// noop
    }

    public static void removeNodeListener (String node, INodeListener listener) {
	// noop
    }

    public static Logger getLogger () {
	if (logger == null) {
	    String logDir = sysProps.getProperty ("logdir");
	    if (logDir == null) {
	        logger = new Logger (System.out);
	    } else {
	        try {
	           File helper = new File (logDir);
	            if (hopHome != null && !helper.isAbsolute ())
                              helper = new File (hopHome, logDir);
	            logDir = helper.getAbsolutePath ();
	            logger = new Logger (logDir, "hop");
	        } catch (IOException iox) {
	            System.err.println ("Could not create Logger for log/hop: "+iox);
	            // fallback to System.out
	            logger = new Logger (System.out);
	        }
	    }
	} 
	return logger;
    }

    public static File getHopHome () {
	return hopHome;
    }

    public static WebServer getXmlRpcServer() {
	return xmlrpc;
    }

}



