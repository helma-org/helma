// Application.java
// Copyright (c) Hannes Wallnöfer 1998-2000

package helma.framework.core;

import java.util.*;
import java.io.*;
import java.lang.reflect.*;
import java.rmi.*;
import java.rmi.server.*;
import helma.framework.*;
import helma.objectmodel.*;
import helma.objectmodel.db.NodeManager;
import helma.objectmodel.db.WrappedNodeManager;
import helma.xmlrpc.*;
import FESI.Data.*;
import FESI.Interpreter.*;
import com.sleepycat.db.DbException;


/**
 * The central class of a HOP application. This class keeps a pool of so-called
 * request evaluators (threads with JavaScript interpreters), waits for 
 * requests from the Web server or XML-RPC port and dispatches them to 
 * the evaluators.
 */
public class Application extends UnicastRemoteObject implements IRemoteApp, Runnable {

    SystemProperties props;
    File appDir, dbDir;
    private String name;
    protected NodeManager nmgr;
    protected static WebServer xmlrpc;
    protected XmlRpcAccess xmlrpcAccess;

    public boolean debug;

    TypeManager typemgr;

    RequestEvaluator eval;
    private Stack freeThreads;
    protected Vector allThreads;

    Hashtable sessions;
    Hashtable activeUsers;
    Hashtable dbMappings;

    Thread worker;
    long requestTimeout = 60000; // 60 seconds for request timeout.
    
    protected String templateExtension, scriptExtension, actionExtension;

    // A transient node that is shared among all evaluators
    protected INode appnode;
    protected volatile long requestCount = 0;
    protected volatile long xmlrpcCount = 0;
    protected volatile long errorCount = 0;

    private DbMapping rootMapping, userRootMapping, userMapping;


    public Application () throws RemoteException {
	super ();
    }

    public Application (String name, File dbHome, File appHome) throws RemoteException, DbException {

	this.name = name;
	appDir = new File (appHome, name);
	if (!appDir.exists())	
	    appDir.mkdirs ();
	dbDir = new File (dbHome, name);
	if (!dbDir.exists())	
	    dbDir.mkdirs ();

	File propfile = new File (appDir, "app.properties");
	props = new SystemProperties (propfile.getAbsolutePath (), IServer.sysProps);

	nmgr = new NodeManager (this, dbDir.getAbsolutePath (), props);

	debug = "true".equalsIgnoreCase (props.getProperty ("debug"));
	try {
	    requestTimeout = Long.parseLong (props.getProperty ("requestTimeout", "60"))*1000l;
	} catch (Exception ignore) {	}
	
	templateExtension = props.getProperty ("templateExtension", ".hsp");
	scriptExtension = props.getProperty ("scriptExtension", ".js");
	actionExtension = props.getProperty ("actionExtension", ".hac");
	
	sessions = new Hashtable ();
	activeUsers = new Hashtable ();
	dbMappings = new Hashtable ();

	appnode = new Node ("app");
	xmlrpc = IServer.getXmlRpcServer ();
	xmlrpcAccess = new XmlRpcAccess (this);
    }

    public void start () {

	eval = new RequestEvaluator (this);
	IServer.getLogger().log ("Starting evaluators for "+name);
	int maxThreads = 12;
	try {
	    maxThreads = Integer.parseInt (props.getProperty ("maxThreads"));
	} catch (Exception ignore) {}
	freeThreads = new Stack ();
	allThreads = new Vector ();
	allThreads.addElement (eval);
	for (int i=0; i<maxThreads; i++) {
	    RequestEvaluator ev = new RequestEvaluator (this);
	    freeThreads.push (ev);
	    allThreads.addElement (ev);
	}

	File urootp = new File (appDir, "userroot.properties");
	SystemProperties p = new SystemProperties (urootp.getAbsolutePath ());
	// if no userroot.properties, set values manually
	if (!urootp.exists ()) {
	    p.put ("_subnodes", "user.id");
	    p.put ("_properties", "user.name");
	}
	new DbMapping (this, "userroot", p);

	typemgr = new TypeManager (this);
	typemgr.check ();
	IServer.getLogger().log ("Started type manager for "+name);

	rootMapping = getDbMapping ("root");
	userRootMapping = getDbMapping ("userroot");
	userMapping = getDbMapping ("user");
	rewireDbMappings ();

	worker = new Thread (this, "Worker-"+name);
	worker.setPriority (Thread.NORM_PRIORITY+2);
	worker.start ();
	IServer.getLogger().log ("session cleanup and scheduler thread started");
	
	xmlrpc.addHandler (this.name, new XmlRpcInvoker (this));

	typemgr.start ();
    }


    public void stop () {
	// stop all threads, this app is going down
	if (worker != null)
	    worker.stop ();
	worker = null;
	typemgr.stop ();

	xmlrpc.removeHandler (this.name);

	if (allThreads != null) {
	    for (Enumeration e=allThreads.elements (); e.hasMoreElements (); ) {
	        RequestEvaluator ev = (RequestEvaluator) e.nextElement ();
	        ev.stop ();
	    }
	}
	allThreads.removeAllElements ();
	freeThreads = null;
	try {
	    nmgr.shutdown ();
	} catch (DbException dbx) {
	    System.err.println ("Error shutting down embedded db: "+dbx);
	}
    }

    public synchronized RequestEvaluator getEvaluator () {
	if (freeThreads == null)
	    throw new ApplicationStoppedException ();
	if (freeThreads.empty ())
	    throw new RuntimeException ("Maximum Thread count reached.");
	RequestEvaluator ev = (RequestEvaluator) freeThreads.pop ();
	return ev;
    }

    public synchronized void releaseEvaluator (RequestEvaluator ev) {
	if (ev != null)
	    freeThreads.push (ev);
    }

    public ResponseTrans execute (RequestTrans req) {

	requestCount += 1;

	User u = getUser (req.session);

	ResponseTrans res = null;
	RequestEvaluator ev = null;
	try {
	    ev = getEvaluator ();
	    res = ev.invoke (req, u);
	} catch (Exception x) {
	    errorCount += 1;
	    res = new ResponseTrans ();
	    res.write ("Error in application: <b>" + x.getMessage () + "</b>");
	} finally {
	    releaseEvaluator (ev);
	}

	res.close ();  // this needs to be done before sending it back
	return res;
    }


    // get raw content from the database, circumventing the scripting framework.
    // currently not used/supported.
    public ResponseTrans get (String path, String sessionID) {
    	ResponseTrans res = null;
	return res;
    }

    public void ping () {
	// do nothing
    }

    public INode getDataRoot () {
	INode root = nmgr.safe.getNode ("0", null);
	root.setDbMapping (rootMapping);
	return root;
    }

    public INode getUserRoot () {
	INode users = nmgr.safe.getNode ("1", null);
	users.setDbMapping (userRootMapping);
	return users;
    }

    public WrappedNodeManager getWrappedNodeManager () {
	return nmgr.safe;
    }

    public INode getUserNode (String uid) {
	if ("prototype".equalsIgnoreCase (uid))
	    return null;
	try {
	    INode users = getUserRoot ();
	    return users.getNode (uid, false);
	} catch (Exception x) {
	    return null;
	}
    }

    public Prototype getPrototype (String str) {
	if (debug) 
	    IServer.getLogger().log ("retrieving prototype for name "+str);
	return typemgr.getPrototype (str);
    }

    public Prototype getPrototype (INode n) {
    	IProperty proto = n.get ("prototype", false);
	if (proto == null)
	    return null;
	return getPrototype (proto.toString ());
    }


    public User getUser (String sessionID) {
    	if (sessionID == null)
	    return null;

    	User u = (User) sessions.get (sessionID);
    	if (u != null) {
    	    u.touch ();
	} else {
	    u = new User (sessionID, this);
	    sessions.put (sessionID, u);
	}
 	return u;
    }


    public INode registerUser (String uname, String password) {
    	// Register a user who already has a user object
    	// (i.e. who has been surfing around)
	if (uname == null)
	    return null;
	uname = uname.toLowerCase ().trim ();
	if ("".equals (uname))
	    return null;

	INode unode = null;
	try {
	    INode users = getUserRoot ();
	    unode = users.getNode (uname, false);
	    if (unode != null) 
	        return null;
	    
	    unode = new Node (uname);
	    unode.setString ("name", uname);
	    unode.setString ("password", password);
	    unode.setString ("prototype", "user");
	    unode.setDbMapping (userMapping);
	    users.setNode (uname, unode);
	    return users.getNode (uname, false);	
	} catch (Exception x) {
	    IServer.getLogger().log ("Error registering User: "+x);
	    return null;
	}
    }

    public boolean loginUser (String uname, String password, ESUser u) {
    	// Check the name/password of a user who already has a user object 
    	// (i.e. who has been surfing around)
	if (uname == null)
	    return false;
	uname = uname.toLowerCase ().trim ();
	if ("".equals (uname))
	    return false;

	try {
	    INode users = getUserRoot ();
	    INode unode = users.getNode (uname, false);
	    String pw = unode.getString ("password", false);
	    if (pw.equals (password)) {
	        // give the user her piece of persistence
	        u.setNode (unode);
	        u.user.setNode (unode);
	        activeUsers.put (unode.getNameOrID (), u.user);
	        return true;
	    }
	        
	} catch (Exception x) {
	    return false;
	}
	return false;
    }

    public boolean logoutUser (ESUser u) {
	if (u.user != null) {
	    String uid = u.user.uid;
                 if (uid != null)
	        activeUsers.remove (uid);
	    u.user.setNode (null);
	    u.setNode (u.user.getNode ());
	}
	return true;
    }

    public String getNodePath (INode n, String tmpname) {
	INode root = getDataRoot ();
	INode users = getUserRoot ();
	String href = n.getUrl (root, users, tmpname);
	return href;
    }

    public String getNodeHref (INode n, String tmpname) {
	boolean linkByQuery = "query".equalsIgnoreCase (props.getProperty ("linkmethod", ""));
	INode root = getDataRoot ();
	INode users = getUserRoot ();
	String connector = linkByQuery ? "?path=" : "/";
	String req = props.getProperty ("baseURI", "") + connector;
	String href = n.getHref (root, users, tmpname, req);
	// add cache teaser
	// href = href + "&tease="+((int) (Math.random ()*999));
	return href;
    }
    

    public void run () {
	long cleanupSleep = 60000;    // thread sleep interval (fixed)
	long scheduleSleep = 60000;  // interval for scheduler invocation
	long lastScheduler = 0;
	IServer.getLogger().log ("Starting scheduler for "+name);
	// as first thing, invoke function onStart in the root object
	try {
	    eval.invokeFunction ((INode) null, "onStart", new ESValue[0]);
	} catch (Exception ignore) {}	

	while (Thread.currentThread () == worker) {
	    // get session timeout
	    int sessionTimeout = 30;
	    try {
	        sessionTimeout = Math.max (0, Integer.parseInt (props.getProperty ("sessionTimeout", "30")));
	    } catch (Exception ignore) {}

 	    try {
	        worker.sleep (cleanupSleep);
	    } catch (InterruptedException x) {
	        IServer.getLogger().log ("Scheduler for "+name+" interrupted");
	        Thread.currentThread().interrupt();
	    }
	    try {
	        IServer.getLogger().log ("Cleaning up "+name+": " + sessions.size () + " sessions active");
	        long now = System.currentTimeMillis ();
	        Hashtable cloned = (Hashtable) sessions.clone ();
	        for (Enumeration e = cloned.elements (); e.hasMoreElements (); ) {
	            User u = (User) e.nextElement ();
	            if (now - u.touched () > sessionTimeout * 60000) {
	                if (u.uid != null) {
	                    try {
	                        eval.invokeFunction (u, "onLogout", new ESValue[0]);
	                    } catch (Exception ignore) {
	                        ignore.printStackTrace ();
	                    }
	                    activeUsers.remove (u.uid);
	                }
	                sessions.remove (u.getSessionID ());
	                u.setNode (null);
	            }
	        }

	        IServer.getLogger().log ("Cleaned up "+name+": " + sessions.size () + " sessions remaining");
	    } catch (Exception cx) {
	        IServer.getLogger().log ("Error cleaning up sessions: "+cx);
	        cx.printStackTrace ();
	    }

	    long now = System.currentTimeMillis ();
	    if (now - lastScheduler > scheduleSleep) {
	        lastScheduler = now;
	        ESValue val = null;
	        try {
	            val = eval.invokeFunction ((INode) null, "scheduler", new ESValue[0]);
	        } catch (Exception ignore) {}	
	        try {
	            int ret = val.toInt32 ();
	            if (ret < 1000)
	                scheduleSleep = 60000l;
	            else
	                scheduleSleep = ret;
	        } catch (Exception ignore) {}
	        IServer.getLogger().log ("Called scheduler for "+name+", will sleep for "+scheduleSleep+" millis");
	    }
	}
	IServer.getLogger().log ("Scheduler for "+name+" exiting");
    }

    public void rewireDbMappings () {
	for (Enumeration e=dbMappings.elements(); e.hasMoreElements(); ) {
	    try {
	        DbMapping m = (DbMapping) e.nextElement ();
	        m.rewire ();
	    } catch (Exception x) {
	        IServer.getLogger().log ("Error rewiring DbMappings: "+x);
	    }
	}
    }

    public String getName () {
	return name;
    }

    public DbMapping getDbMapping (String typename) {
	return (DbMapping) dbMappings.get (typename);
    }

    public void putDbMapping (String typename, DbMapping dbmap) {
	dbMappings.put (typename, dbmap);
    }


    /**
     * Check if a method may be invoked via XML-RPC on a prototype
     */
    protected void checkXmlRpcAccess (String proto, String method) throws Exception {
	xmlrpcAccess.checkAccess (proto, method);
    }

}

//////////////////////////////////////////////////////////////
////  XML-RPC handler class


class XmlRpcInvoker implements XmlRpcHandler {
    	
    Application app;

    public XmlRpcInvoker (Application app) {
    	this.app = app;
    }
    	
    public Object execute (String method, Vector argvec) throws Exception {

	app.xmlrpcCount += 1;

	Object retval = null;
	RequestEvaluator ev = null;
	try {
	    ev = app.getEvaluator ();
	    retval = ev.invokeXmlRpc (method, argvec);
	}  finally {
	    app.releaseEvaluator (ev);
	}
	return retval;
    }
}


//////////////////////////////////////////////////////////////
////  XML-RPC access permission checker


class XmlRpcAccess {
    	
    Application app;
    Hashtable prototypes;
    long lastmod;

    public XmlRpcAccess (Application app) {
    	this.app = app;
	init ();
    }
    	
    public void checkAccess (String proto, String method) throws Exception {
	if (app.props.lastModified () != lastmod)
	    init ();
	Hashtable protoAccess = (Hashtable) prototypes.get (proto.toLowerCase ());
	if (protoAccess == null)
	    throw new Exception ("Method "+method+" is not callable via XML-RPC");
	if (protoAccess.get (method.toLowerCase ()) == null)
	    throw new Exception ("Method "+method+" is not callable via XML-RPC");
    }

    /*
     * create internal representation of  XML-RPC-Permissions. They're encoded in the app property
    * file like this:
    *    xmlrpcAccess = root.sayHello, story.countMessages, user.login
    * i.e. a prototype.method entry for each function callable via XML-RPC.
    */
    private void init () {
	String newAccessprop = app.props.getProperty ("xmlrpcaccess");
	Hashtable newPrototypes = new Hashtable ();
	if (newAccessprop != null) {
	    StringTokenizer st = new StringTokenizer (newAccessprop, ",; ");
	    while (st.hasMoreTokens ()) {
	        String token = st.nextToken ().trim ();
	        int dot = token.indexOf (".");
	        if (dot > -1) {
	            String proto = token.substring (0, dot).toLowerCase ();
	            String method = token.substring (dot+1).toLowerCase ();
	            Hashtable protoAccess = (Hashtable) newPrototypes.get (proto);
	            if (protoAccess == null) {
	                protoAccess = new Hashtable ();
	                newPrototypes.put (proto, protoAccess);
	            }
	            protoAccess.put (method, method);
	        }
	    }
	}
	this.prototypes = newPrototypes;
	this.lastmod = app.props.lastModified ();
    }

}





