// Application.java
// Copyright (c) Hannes Wallnöfer 1998-2000

package helma.framework.core;

import java.io.*;
import java.lang.reflect.*;
import java.rmi.*;
import java.rmi.server.*;
import helma.framework.*;
import helma.objectmodel.*;
import helma.objectmodel.db.NodeManager;
import helma.objectmodel.db.WrappedNodeManager;
import helma.xmlrpc.*;
import helma.util.*;
import FESI.Data.*;
import FESI.Interpreter.*;
import com.sleepycat.db.DbException;
import java.util.Hashtable;
import java.util.Vector;
import java.util.Enumeration;
import java.util.Stack;
import java.util.EmptyStackException;
import java.util.StringTokenizer;

/**
 * The central class of a HOP application. This class keeps a pool of so-called
 * request evaluators (threads with JavaScript interpreters), waits for 
 * requests from the Web server or XML-RPC port and dispatches them to 
 * the evaluators.
 */
public class Application extends UnicastRemoteObject implements IRemoteApp, Runnable {

    private String name;
    SystemProperties props, dbProps;
    File home, appDir, dbDir;
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
    Hashtable dbSources;

    Thread worker;
    long requestTimeout = 60000; // 60 seconds for request timeout.
    ThreadGroup threadgroup;

    // Map of requesttrans -> active requestevaluators
    Hashtable activeRequests;

    // Two logs for each application: events and accesses
    Logger eventLog, accessLog;
    
    protected String templateExtension, scriptExtension, actionExtension, skinExtension;

    // A transient node that is shared among all evaluators
    protected INode appnode;
    protected volatile long requestCount = 0;
    protected volatile long xmlrpcCount = 0;
    protected volatile long errorCount = 0;

    private DbMapping rootMapping, userRootMapping, userMapping;

    protected CacheMap skincache = new CacheMap (100);

    String charset;

    private CryptFile pwfile;


    public Application () throws RemoteException {
	super ();
    }

    public Application (String name, SystemProperties sysProps, SystemProperties sysDbProps, File home)
	    throws RemoteException, DbException, IllegalArgumentException {
	
	if (name == null || name.trim().length() == 0)
	    throw new IllegalArgumentException ("Invalid application name: "+name);

	this.name = name;
	this.home = home;

	threadgroup = new ThreadGroup ("TX-"+name);

	String appHome = sysProps.getProperty ("appHome");
	if (appHome != null && !"".equals (appHome.trim()))
	    appDir = new File (appHome);
	else
	    appDir = new File (home, "apps");
	appDir = new File (appDir, name);
	if (!appDir.exists())	
	    appDir.mkdirs ();

	String dbHome = sysProps.getProperty ("dbHome");
	if (dbHome != null && !"".equals (dbHome.trim()))
	    dbDir = new File (dbHome);
	else
	    dbDir = new File (home, "db");
	dbDir = new File (dbDir, name);
	if (!dbDir.exists())	
	    dbDir.mkdirs ();

	File propfile = new File (appDir, "app.properties");
	props = new SystemProperties (propfile.getAbsolutePath (), sysProps);

	File dbpropfile = new File (appDir, "db.properties");
	dbProps = new SystemProperties (dbpropfile.getAbsolutePath (), sysDbProps);

	File pwf = new File (home, "passwd");
	CryptFile parentpwfile = new CryptFile (pwf, null);
	pwf = new File (appDir, "passwd");
	pwfile = new CryptFile (pwf, parentpwfile);

	nmgr = new NodeManager (this, dbDir.getAbsolutePath (), props);

	charset = props.getProperty ("charset", "ISO-8859-1");

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
	dbSources = new Hashtable ();

	appnode = new Node ("app");
	xmlrpc = IServer.getXmlRpcServer ();
	xmlrpcAccess = new XmlRpcAccess (this);
    }

    public void start () {

	eval = new RequestEvaluator (this);
	logEvent ("Starting evaluators for "+name);
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
	logEvent ("Started type manager for "+name);

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
	logEvent ("session cleanup and scheduler thread started");
	
	if (xmlrpc != null)
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
	        activeRequests.remove (req);
	        releaseEvaluator (ev);
	        // response needs to be closed/encoded before sending it back
	        try {
	            res.close (charset);
	        } catch (UnsupportedEncodingException uee) {
	            logEvent ("Unsupported response encoding: "+uee.getMessage());
	        }
	    } else {
	        res.waitForClose ();
	    }
	}

	return res;
    }


    /**
     * Update HopObjects in this application's cache. This is used to replicate
     * application caches in a distributed app environment
     */
    public void replicateCache (Vector add, Vector delete) {
	if (!"true".equalsIgnoreCase (props.getProperty ("allowReplication")))
	    return;
	nmgr.replicateCache (add, delete);
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
	    String usernameField = userMapping.getNameField ();
	    if (usernameField == null)
	        usernameField = "name";
	    unode.setString (usernameField, uname);
	    unode.setString ("password", password);
	    unode.setPrototype ("user");
	    unode.setDbMapping (userMapping);
	    users.setNode (uname, unode);
	    return users.getNode (uname, false);	
	} catch (Exception x) {
	    logEvent ("Error registering User: "+x);
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

    /**
     * In contrast to login, this works outside the Hop user object framework. Instead, the user is
     * authenticated against a passwd file in the application directory. This is to have some sort of
     * authentication available *before* the application is up and running, i.e. for application setup tasks.
     */
    public boolean authenticate (String uname, String password) {
	if (uname == null || password == null)
	    return false;
	return pwfile.authenticate (uname, password);
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

    public void logEvent (String msg) {
	if (eventLog == null)
	    eventLog = getLogger (name+"_event");
	eventLog.log (msg);
    }

    public void logAccess (String msg) {
	if (accessLog == null)
	    accessLog = getLogger (name+"_access");
	accessLog.log (msg);
    }

    public Logger getLogger (String logname) {
	Logger log = null;
	String logDir = props.getProperty ("logdir");
	if (logDir == null)
	    logDir = "log";
	// allow log to be redirected to System.out by setting logdir to "console"
	if ("console".equalsIgnoreCase (logDir))
	    return new Logger (System.out);
	try {
	   File helper = new File (logDir);
	    if (home != null && !helper.isAbsolute ())
                      helper = new File (home, logDir);
	    logDir = helper.getAbsolutePath ();
	    log = new Logger (logDir, logname);
	} catch (IOException iox) {
	    System.err.println ("Could not create log "+logname+" for application "+name+": "+iox);
	    // fallback to System.out
	    log = new Logger (System.out);
	}
	return log;
    }

    public void run () {
	long cleanupSleep = 60000;    // thread sleep interval (fixed)
	long scheduleSleep = 60000;  // interval for scheduler invocation
	long lastScheduler = 0;
	long lastCleanup = System.currentTimeMillis ();

	logEvent ("Starting scheduler for "+name);
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

	    long now = System.currentTimeMillis ();

	    // check if we should clean up user sessions
	    if (now - lastCleanup > cleanupSleep) try {
	        lastCleanup = now;
	        logEvent ("Cleaning up "+name+": " + sessions.size () + " sessions active");
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
	        logEvent ("Cleaned up "+name+": " + sessions.size () + " sessions remaining");
	    } catch (Exception cx) {
	        logEvent ("Error cleaning up sessions: "+cx);
	        cx.printStackTrace ();
	    }

	    // check if we should call scheduler
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
	        logEvent ("Called scheduler for "+name+", will sleep for "+scheduleSleep+" millis");
	    }

	    // sleep until we have work to do
 	    try {
	        worker.sleep (Math.min (cleanupSleep, scheduleSleep));
	    } catch (InterruptedException x) {
	        logEvent ("Scheduler for "+name+" interrupted");
	        worker = null;
	        break;
	    }


	}

	logEvent ("Scheduler for "+name+" exiting");
    }

    public void rewireDbMappings () {
	for (Enumeration e=dbMappings.elements(); e.hasMoreElements(); ) {
	    try {
	        DbMapping m = (DbMapping) e.nextElement ();
	        m.rewire ();
	        String typename = m.getTypeName ();
	        // set prototype hierarchy
	        if (!"hopobject".equalsIgnoreCase (typename) && !"global".equalsIgnoreCase (typename)) {
	            Prototype proto = (Prototype) typemgr.prototypes.get (typename);
	            if (proto != null) {
	                String protoname = m.getExtends ();
	                if (protoname == null)
	                    protoname = "hopobject";
	                Prototype protoProto = (Prototype) typemgr.prototypes.get (protoname);
	                if (protoProto == null)
	                    protoProto = (Prototype) typemgr.prototypes.get ("hopobject");
	                if (protoProto != null)
	                    proto.setPrototype (protoProto);
	            }
	        }
	    } catch (Exception x) {
	        logEvent ("Error rewiring DbMappings: "+x);
	    }
	}
    }


    /**
     * Return a DbSource object for a given name. A DbSource is a relational database defined
     * in a db.properties file.
     */
    public DbSource getDbSource (String name) {
	String dbSrcName = name.toLowerCase ();
	DbSource dbs = (DbSource) dbSources.get (dbSrcName);
	if (dbs != null)
	    return dbs;
	if (dbProps.getProperty (dbSrcName+".url") != null && dbProps.getProperty (dbSrcName+".driver") != null) {	
	    try {
	        dbs = new DbSource (name, dbProps);
	        dbSources.put (dbSrcName, dbs);
	    } catch (Exception problem) {
	        logEvent ("Error creating DbSource "+name);
	        logEvent ("Reason: "+problem);
	    }
	}
	return dbs;
    }

    /**
     * Return the name of this application
     */
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
	logEvent ("Thread Stats for "+name+": "+threadgroup.activeCount()+" active");
    	Runtime rt = Runtime.getRuntime ();
	long free = rt.freeMemory ();
	long total = rt.totalMemory ();
	logEvent ("Free memory: "+(free/1024)+" kB");
	logEvent ("Total memory: "+(total/1024)+" kB");
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





