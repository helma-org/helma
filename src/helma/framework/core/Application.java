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
import helma.util.CacheMap;
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

    private String baseURI;

    public boolean debug;

    TypeManager typemgr;

    RequestEvaluator eval;
    protected Stack freeThreads;
    protected Vector allThreads;

    boolean stopped = false;

    Hashtable sessions;
    Hashtable activeUsers;
    Hashtable dbMappings;

    Thread worker;
    long requestTimeout = 60000; // 60 seconds for request timeout.
    ThreadGroup threadgroup;

    // Map of requesttrans -> active requestevaluators
    Hashtable activeRequests;
    
    protected String templateExtension, scriptExtension, actionExtension, skinExtension;

    // A transient node that is shared among all evaluators
    protected INode appnode;
    protected volatile long requestCount = 0;
    protected volatile long xmlrpcCount = 0;
    protected volatile long errorCount = 0;

    private DbMapping rootMapping, userRootMapping, userMapping;

    protected CacheMap skincache = new CacheMap (100);


    public Application () throws RemoteException {
	super ();
    }

    public Application (String name, File dbHome, File appHome) throws RemoteException, DbException {

	this.name = name;

	threadgroup = new ThreadGroup ("TX-"+name);

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
	skinExtension = ".skin";
	
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
	activeRequests = new Hashtable ();

	typemgr = new TypeManager (this);
	typemgr.check ();
	IServer.getLogger().log ("Started type manager for "+name);

	rootMapping = getDbMapping ("root");
	userMapping = getDbMapping ("user");
	SystemProperties p = new SystemProperties ();
	String usernameField = userMapping.getNameField ();
	if (usernameField == null)
	    usernameField = "name";
	p.put ("_properties", "user."+usernameField);
	userRootMapping = new DbMapping (this, "__userroot__", p);
	rewireDbMappings ();

	worker = new Thread (this, "Worker-"+name);
	worker.setPriority (Thread.NORM_PRIORITY+2);
	worker.start ();
	IServer.getLogger().log ("session cleanup and scheduler thread started");
	
	xmlrpc.addHandler (this.name, new XmlRpcInvoker (this));

	typemgr.start ();
    }


    public void stop () {

	stopped = true;

	// stop all threads, this app is going down
	if (worker != null)
	    worker.interrupt ();
	worker = null;
	typemgr.stop ();

	xmlrpc.removeHandler (this.name);

	if (allThreads != null) {
	    for (Enumeration e=allThreads.elements (); e.hasMoreElements (); ) {
	        RequestEvaluator ev = (RequestEvaluator) e.nextElement ();
	        ev.stopThread ();
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

    protected RequestEvaluator getEvaluator () {
	if (stopped)
	    throw new ApplicationStoppedException ();
	try {
	    return (RequestEvaluator) freeThreads.pop ();
	} catch (EmptyStackException nothreads) {
	    throw new RuntimeException ("Maximum Thread count reached.");
	}
    }

    protected void releaseEvaluator (RequestEvaluator ev) {
	if (ev != null)
	    freeThreads.push (ev);
    }

    protected boolean setNumberOfEvaluators (int n) {
	if (n < 2 || n > 511)
	    return false;
	int current = allThreads.size();
	synchronized (allThreads) {
	    if (n > current) {
	        int toBeCreated = n - current;
	        for (int i=0; i<toBeCreated; i++) {
	            RequestEvaluator ev = new RequestEvaluator (this);
	            freeThreads.push (ev);
	            allThreads.addElement (ev);
	        }
	    } else if (n < current) {
	        int toBeDestroyed = current - n;
	        for (int i=0; i<toBeDestroyed; i++) {
	            try {
	                RequestEvaluator re = (RequestEvaluator) freeThreads.pop ();
	                allThreads.removeElement (re);
	                typemgr.unregisterRequestEvaluator (re);
	                re.stopThread ();
	            } catch (EmptyStackException empty) {
	                return false;
	            }
	        }
	    }
	}
	return true;
    }

    public ResponseTrans execute (RequestTrans req) {

	requestCount += 1;

	User u = getUser (req.session);

	ResponseTrans res = null;
	RequestEvaluator ev = null;
	// are we responsible for releasing the evaluator and closing the result?
	boolean primaryRequest = false;
	try {
	    // first look if a request with same user/path/data is already being executed.
	    // if so, attach the request to its output instead of starting a new evaluation
	    // this helps to cleanly solve "doubleclick" kind of users
	    ev = (RequestEvaluator) activeRequests.get (req);
	    if (ev != null) {
	        res = ev.attachRequest (req);
	    }
	    if (res == null) {
	        primaryRequest = true;
	        // if attachRequest returns null this means we came too late
	        // and the other request was finished in the meantime
	        ev = getEvaluator ();
	        res = ev.invoke (req, u);
	    }
	} catch (ApplicationStoppedException stopped) {
	    // let the servlet know that this application has gone to heaven
	    throw stopped;
	} catch (Exception x) {
	    errorCount += 1;
	    res = new ResponseTrans ();
	    res.write ("Error in application: <b>" + x.getMessage () + "</b>");
	} finally {
	    if (primaryRequest) {
	        releaseEvaluator (ev);
	        res.close ();  // this needs to be done before sending it back
	    } else {
	        res.waitForClose ();
	    }
	}

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
	INode root = nmgr.safe.getNode ("0", rootMapping);
	root.setDbMapping (rootMapping);
	return root;
    }

    public INode getUserRoot () {
	INode users = nmgr.safe.getNode ("1", userRootMapping);
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


    /**
     * Return a prototype for a given node. If the node doesn't specify a prototype,
     * return the generic hopobject prototype.
     */
    public Prototype getPrototype (INode n) {
    	String protoname = n.getPrototype ();
	if (protoname == null)
	    return typemgr.getPrototype ("hopobject");
	return typemgr.getPrototype (protoname);
    }


    /**
     * Return the user currently associated with a given Hop session ID.
     */
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
	    unode.setPrototype ("user");
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
	    if (pw != null && pw.equals (password)) {
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

	    // switch back to the non-persistent user node as cache
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
	INode root = getDataRoot ();
	INode users = getUserRoot ();
	// check base uri from app.properties
	String base = props.getProperty ("baseURI");
	if (base != null)
	    setBaseURI (base);
	String href = n.getHref (root, users, tmpname, baseURI == null ? "/" : baseURI);
	// add cache teaser
	// href = href + "&tease="+((int) (Math.random ()*999));
	return href;
    }
    
    public void setBaseURI (String uri) {
	if (uri == null)
	    this.baseURI = "/";
	else if (!uri.endsWith ("/"))
	    this.baseURI = uri+"/";
	else
	    this.baseURI = uri;
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
	        worker = null;
	        break;
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
	return typename == null ? null : (DbMapping) dbMappings.get (typename);
    }

    public void putDbMapping (String typename, DbMapping dbmap) {
	dbMappings.put (typename, dbmap);
    }

    /**
     * Periodically called to log thread stats for this application
     */
    public void printThreadStats () {
	IServer.getLogger().log ("Thread Stats for "+name+": "+threadgroup.activeCount()+" active");
    	Runtime rt = Runtime.getRuntime ();
	long free = rt.freeMemory ();
	long total = rt.totalMemory ();
	IServer.getLogger().log ("Free memory: "+(free/1024)+" kB");
	IServer.getLogger().log ("Total memory: "+(total/1024)+" kB");
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





