// Application.java
// Copyright (c) Hannes Wallnöfer 1998-2000

package helma.framework.core;

import helma.doc.DocApplication;
import helma.doc.DocException;
import helma.framework.*;
import helma.main.Server;
import helma.scripting.*;
import helma.scripting.fesi.ESUser;
import helma.objectmodel.*;
import helma.objectmodel.db.*;
import helma.xmlrpc.*;
import helma.util.*;
import java.util.*;
import java.io.*;
import java.net.URLEncoder;
import java.lang.reflect.*;
import java.rmi.*;
import java.rmi.server.*;

/**
 * The central class of a Helma application. This class keeps a pool of so-called
 * request evaluators (threads with JavaScript interpreters), waits for
 * requests from the Web server or XML-RPC port and dispatches them to
 * the evaluators.
 */
public final class Application extends UnicastRemoteObject implements IRemoteApp, IPathElement, IReplicatedApp, Runnable {

    private String name;
    SystemProperties props, dbProps;
    File home, appDir, dbDir;
    protected NodeManager nmgr;

    // the root of the website, if a custom root object is defined.
    // otherwise this is managed by the NodeManager and not cached here.
    Object rootObject = null;
    String rootObjectClass;

    /**
    *  The type manager checks if anything in the application's prototype definitions
    * has been updated prior to each evaluation.
    */
    public TypeManager typemgr;

    /**
     * The skin manager for this application
     */
    protected SkinManager skinmgr;

    /**
    *  Each application has one internal request evaluator for calling
    * the scheduler and other internal functions.
    */
    RequestEvaluator eval;

    /**
    * Collections for evaluator thread pooling
    */
    protected Stack freeThreads;
    protected Vector allThreads;

    boolean stopped = false;
    boolean debug;
    public long starttime;

    public Hashtable sessions;
    public Hashtable activeUsers;
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

    protected static WebServer xmlrpc;
    protected XmlRpcAccess xmlrpcAccess;
    private String xmlrpcHandlerName;

    // the URL-prefix to use for links into this application
    private String baseURI;

    private DbMapping rootMapping, userRootMapping, userMapping;

    // boolean checkSubnodes;

    // name of respone encoding
    String charset;

    // password file to use for authenticate() function
    private CryptFile pwfile;

    // Map of java class names to object prototypes
    Properties classMapping;
    // Map of extensions allowed for public skins
    Properties skinExtensions;

    // a cache for parsed skin objects
    public CacheMap skincache = new CacheMap (200, 0.80f);

    // DocApplication used for introspection
    public DocApplication docApp;

    /**
     *  Zero argument constructor needed for RMI
     */
    public Application () throws RemoteException {
	super ();
    }

    /**
     * Build an application with the given name in the home directory. Server-wide properties will
     * be created if the files are present, but they don't have to.
     */
    public Application (String name, File home) throws RemoteException, IllegalArgumentException {
	this (name, home,
		new SystemProperties (new File (home, "server.properties").getAbsolutePath ()),
		new SystemProperties (new File (home, "db.properties").getAbsolutePath ()));
    }

    /**
     * Build an application with the given name, app and db properties and app base directory. The
     * app directories will be created if they don't exist already.
     */
    public Application (String name, File home, SystemProperties sysProps, SystemProperties sysDbProps)
		    throws RemoteException, IllegalArgumentException {

	if (name == null || name.trim().length() == 0)
	    throw new IllegalArgumentException ("Invalid application name: "+name);

	this.name = name;
	this.home = home;

	// give the Helma Thread group a name so the threads can be recognized
	threadgroup = new ThreadGroup ("TX-"+name);

	// check the system props to see if custom app directory is set.
	// otherwise use <home>/apps/<appname>
	String appHome = null;
	if (sysProps != null)
	    appHome = sysProps.getProperty ("appHome");
	if (appHome != null && !"".equals (appHome.trim()))
	    appDir = new File (appHome);
	else
	    appDir = new File (home, "apps");
	appDir = new File (appDir, name);
	if (!appDir.exists())	
	    appDir.mkdirs ();

	// check the system props to see if custom embedded db directory is set.
	// otherwise use <home>/db/<appname>
	String dbHome = null;
	if (sysProps != null)
	    dbHome = sysProps.getProperty ("dbHome");
	if (dbHome != null && !"".equals (dbHome.trim()))
	    dbDir = new File (dbHome);
	else
	    dbDir = new File (home, "db");
	dbDir = new File (dbDir, name);
	if (!dbDir.exists())	
	    dbDir.mkdirs ();

	// create app-level properties
	File propfile = new File (appDir, "app.properties");
	props = new SystemProperties (propfile.getAbsolutePath (), sysProps);

	// create app-level db sources
	File dbpropfile = new File (appDir, "db.properties");
	dbProps = new SystemProperties (dbpropfile.getAbsolutePath (), sysDbProps);

	// the passwd file, to be used with the authenticate() function
	File pwf = new File (home, "passwd");
	CryptFile parentpwfile = new CryptFile (pwf, null);
	pwf = new File (appDir, "passwd");
	pwfile = new CryptFile (pwf, parentpwfile);

	// the properties that map java class names to prototype names
	classMapping = new SystemProperties (new File (appDir, "class.properties").getAbsolutePath ());

	// get class name of root object if defined. Otherwise native Helma objectmodel will be used.
	rootObjectClass = classMapping.getProperty ("root");

	// the properties that map allowed public skin extensions to content types
	skinExtensions = new SystemProperties (new File (appDir, "mime.properties").getAbsolutePath ());

	// character encoding to be used for responses
	charset = props.getProperty ("charset", "ISO-8859-1");

	debug = "true".equalsIgnoreCase (props.getProperty ("debug"));
	// checkSubnodes = !"false".equalsIgnoreCase (props.getProperty ("subnodeChecking"));
	

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

	appnode = new TransientNode ("app");
	xmlrpc = helma.main.Server.getXmlRpcServer ();
	xmlrpcAccess = new XmlRpcAccess (this);
    }

    /**
     * Get the application ready to run, initializing the evaluators and type manager.
     */
    public void init () throws DatabaseException, ScriptingException {
	// scriptingEngine = new helma.scripting.fesi.FesiScriptingEnvironment ();
	// scriptingEngine.init (this, props);

	typemgr = new TypeManager (this);
	typemgr.createPrototypes ();
	// logEvent ("Started type manager for "+name);

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

	skinmgr = new SkinManager (this);

	rootMapping = getDbMapping ("root");
	userMapping = getDbMapping ("user");
	SystemProperties p = new SystemProperties ();
	String usernameField = userMapping.getNameField ();
	if (usernameField == null)
	    usernameField = "name";
	p.put ("_properties", "user."+usernameField);
	userRootMapping = new DbMapping (this, "__userroot__", p);
	rewireDbMappings ();

	nmgr = new NodeManager (this, dbDir.getAbsolutePath (), props);
	
	xmlrpcHandlerName = props.getProperty ("xmlrpcHandlerName", this.name);
	if (xmlrpc != null)
	    xmlrpc.addHandler (xmlrpcHandlerName, new XmlRpcInvoker (this));

    }

    /**
     *  Create and start scheduler and cleanup thread
     */
    public void start () {
	starttime = System.currentTimeMillis();
	worker = new Thread (this, "Worker-"+name);
	worker.setPriority (Thread.NORM_PRIORITY+2);
	worker.start ();
	// logEvent ("session cleanup and scheduler thread started");
    }

    /**
     * This is called to shut down a running application.
     */
    public void stop () {

	stopped = true;

	// stop all threads, this app is going down
	if (worker != null)
	    worker.interrupt ();
	worker = null;

	if (xmlrpc != null && xmlrpcHandlerName != null)
	    xmlrpc.removeHandler (xmlrpcHandlerName);

	// stop evaluators
	if (allThreads != null) {
	    for (Enumeration e=allThreads.elements (); e.hasMoreElements (); ) {
	        RequestEvaluator ev = (RequestEvaluator) e.nextElement ();
	        ev.stopThread ();
	        ev.scriptingEngine = null;
	    }
	}
	
	// remove evaluators
	allThreads.removeAllElements ();
	freeThreads = null;
	
	// shut down node manager and embedded db
	try {
	    nmgr.shutdown ();
	} catch (DatabaseException dbx) {
	    System.err.println ("Error shutting down embedded db: "+dbx);
	}

	// null out type manager
	typemgr = null;
	
	// stop logs if they exist
	if (eventLog != null) {
	    eventLog.close ();
	}
	if (accessLog != null) {
	    accessLog.close ();
	}

    }

    /**
     * Returns a free evaluator to handle a request.
     */
    protected RequestEvaluator getEvaluator () {
	if (stopped)
	    throw new ApplicationStoppedException ();
	try {
	    return (RequestEvaluator) freeThreads.pop ();
	} catch (EmptyStackException nothreads) {
	    throw new RuntimeException ("Maximum Thread count reached.");
	}
    }

    /**
     * Returns an evaluator back to the pool when the work is done.
     */
    protected void releaseEvaluator (RequestEvaluator ev) {
        if (ev != null) {
            ev.recycle ();
            freeThreads.push (ev);
        }
    }

    /**
     * This can be used to set the maximum number of evaluators which will be allocated.
     * If evaluators are required beyound this number, an error will be thrown.
     */
    public boolean setNumberOfEvaluators (int n) {
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
	                // typemgr.unregisterRequestEvaluator (re);
	                re.stopThread ();
	            } catch (EmptyStackException empty) {
	                return false;
	            }
	        }
	    }
	}
	return true;
    }

    /**
     *  Return the number of currently active threads
     */
    public int getActiveThreads () {
	return 0;
    }

    /**
    *  Execute a request coming in from a web client.
    */
    public ResponseTrans execute (RequestTrans req) {

	requestCount += 1;
	
	// get user for this request's session
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
	            // reset data fields for garbage collection (may hold references to evaluator)
	            res.data = null;
	            req.data = null;
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

    /**
     * Reset the application's object cache, causing all objects to be refetched from
     * the database.
     */
    public void clearCache () {
	nmgr.clearCache ();
    }

    /**
     *  Set the application's root element to an arbitrary object. After this is called
     *  with a non-null object, the helma node manager will be bypassed. This function
     * can be used to script and publish any Java object structure with Helma.
     */
    public void setDataRoot (Object root) {
	this.rootObject = root;
    }

    /**
     * This method returns the root object of this application's object tree.
     */
    public Object getDataRoot () {
	// if rootObject is set, immediately return it.
	if (rootObject != null)
	    return rootObject;
	// check if we ought to create a rootObject from its class name
	if (rootObjectClass != null) {
	    // create custom root element.
	    if (rootObject == null) {
	        try {
	            if ( classMapping.containsKey("root.factory.class") && classMapping.containsKey("root.factory.method") ) {
	                Class c = Class.forName( classMapping.getProperty("root.factory.class") );
	                Method m = c.getMethod( classMapping.getProperty("root.factory.method"), null );
	                rootObject = m.invoke(c, null);
	            } else {
	                Class c = Class.forName( classMapping.getProperty("root") );
	                rootObject = c.newInstance();
	            }
	        } catch ( Exception e )	{
	            throw new RuntimeException ( "Error creating root object: " + e.toString() );
	        }
	    }
	    return rootObject;
	}
	// no custom root object is defined - use standard helma objectmodel
	INode root = nmgr.safe.getNode ("0", rootMapping);
	root.setDbMapping (rootMapping);
	return root;
    }

    /**
     * Returns the Object which contains registered users of this application.
     */
    public INode getUserRoot () {
	INode users = nmgr.safe.getNode ("1", userRootMapping);
	users.setDbMapping (userRootMapping);
	return users;
    }

    /**
     * Returns a wrapper containing the node manager for this application. The node manager is
     * the gateway to the helma.objectmodel packages, which perform the mapping of objects to
     * relational database tables or the embedded database.
     */
    public WrappedNodeManager getWrappedNodeManager () {
	return nmgr.safe;
    }

    /**
     *  Return a transient node that is shared by all evaluators of this application ("app node")
     */
    public INode getAppNode () {
	return appnode;
    }


    /**
     * Returns a Node representing a registered user of this application by his or her user name.
     */
    public INode getUserNode (String uid) {
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
    public Prototype getPrototype (Object obj) {
	String protoname = getPrototypeName (obj);
	if (protoname == null)
	    return typemgr.getPrototype ("hopobject");
	Prototype p = typemgr.getPrototype (protoname);
	if (p == null)
	    p = typemgr.getPrototype ("hopobject");
	return p;
    }

    /**
     * Return a collection containing all prototypes defined for this application
     */
    public Collection getPrototypes () {
	return typemgr.prototypes.values ();
    }

    /**
     *  Retrurn a skin for a given object. The skin is found by determining the prototype
     *  to use for the object, then looking up the skin for the prototype.
     */
    public Skin getSkin (Object object, String skinname, Object[] skinpath) {
	return skinmgr.getSkin (object, skinname, skinpath); 
    }

    /**
     * Return the user currently associated with a given Hop session ID. This may be
     * a registered or an anonymous user.
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


    /**
     * Register a user with the given user name and password.
     */
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
	    
	    unode = users.createNode (uname);
	    unode.setPrototype ("user");
	    unode.setDbMapping (userMapping);
	    String usernameField = userMapping.getNameField ();
	    String usernameProp = null;
	    if (usernameField != null)
	        usernameProp = userMapping.columnNameToProperty (usernameField);
	    if (usernameProp == null)
	        usernameProp = "name";
	    unode.setName (uname);
	    unode.setString (usernameProp, uname);
	    unode.setString ("password", password);
	    // users.setNode (uname, unode);
	    // return users.getNode (uname, false);	
	    return unode;
	} catch (Exception x) {
	    logEvent ("Error registering User: "+x);
	    return null;
	}
    }

    /**
     * Log in a user given his or her user name and password.
     */
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
	        // give the user his/her persistant node
	        u.setNode (unode);
	        activeUsers.put (unode.getName (), u.user);
	        return true;
	    }

	} catch (Exception x) {
	    return false;
	}
	return false;
    }

    /**
     * Log out a user from this application.
     */
    public boolean logoutUser (ESUser u) {
	if (u.user != null) {
	    String uid = u.user.uid;
                 if (uid != null)
	        activeUsers.remove (uid);

	    // switch back to the non-persistent user node as cache
	    u.setNode (null);
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

    /**
     *  Return the href to the root of this application.
     */
    public String getRootHref () {
	return getNodeHref (getDataRoot(), null);
    }

    /**
     * Return a path to be used in a URL pointing to the given element  and action
     */
    public String getNodeHref (Object elem, String actionName) {
	// FIXME: will fail for non-node roots
	Object root = getDataRoot ();
	INode users = getUserRoot ();

	// check base uri and optional root prototype from app.properties
	String base = props.getProperty ("baseURI");
	String rootproto = props.getProperty ("rootPrototype");

	if (base != null || baseURI == null)
	    setBaseURI (base);

	// String href = n.getUrl (root, users, tmpname, siteroot);

	String divider = "/";
	StringBuffer b = new StringBuffer ();
	Object p = elem;
	int loopWatch = 0;

	while  (p != null && getParentElement (p) != null && p != root) {

	    if (rootproto != null && rootproto.equals (getPrototypeName (p)))
	        break;
	    b.insert (0, divider);

	    // users always have a canonical URL like /users/username
	    if ("user".equals (getPrototypeName (p))) {
	        b.insert (0, URLEncoder.encode (getElementName (p)));
	        p = users;
	        break;
	    }
	    b.insert (0, URLEncoder.encode (getElementName (p)));
	    p = getParentElement (p);

	    if (loopWatch++ > 20)
	        break;
	}

	if (p == users) {
	    b.insert (0, divider);
	    b.insert (0, "users");
	}

	if (actionName != null)
	    b.append (URLEncoder.encode (actionName));

	return baseURI + b.toString ();
    }


    /**
     *  This method sets the base URL of this application which will be prepended to
     *  the actual object path.
     */
    public void setBaseURI (String uri) {
	if (uri == null)
	    this.baseURI = "/";
	else if (!uri.endsWith ("/"))
	    this.baseURI = uri+"/";
	else
	    this.baseURI = uri;
    }

    /**
     * Tell other classes whether they should output logging information for this application.
     */
     public boolean debug () {
	return debug;
    }

    private Object invokeFunction (Object obj, String func, Object[] args) {
	Thread thread = Thread.currentThread ();
	RequestEvaluator reval = null;
	int l = allThreads.size();
	for (int i=0; i<l; i++) {
	    RequestEvaluator r = (RequestEvaluator) allThreads.get (i);
	    if (r != null && r.rtx == thread)
	        reval = r;
	}
	if (reval != null) try {
	    return reval.invokeDirectFunction (obj, func, args);
	} catch (Exception x) {
	    if (debug)
	        System.err.println ("ERROR invoking function "+func+": "+x);
	}
	return null;
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///   The following methods mimic the IPathElement interface. This allows as
    ///   to script any Java object: If the object implements IPathElement (as does
    ///   the Node class in Helma's internal objectmodel) then the corresponding
    ///   method is called in the object itself. Otherwise, a corresponding script function
    ///   is called on the object.
    //////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     *  Return the name to be used to get this element from its parent
     */
    public String getElementName (Object obj) {
	if (obj instanceof IPathElement)
	    return ((IPathElement) obj).getElementName ();
	Object retval = invokeFunction (obj, "getElementName", null);
	if (retval != null)
	    return retval.toString ();
	return null;
    }

    /**
     * Retrieve a child element of this object by name.
     */
    public Object getChildElement (Object obj, String name) {
	if (obj instanceof IPathElement)
	    return ((IPathElement) obj).getChildElement (name);
	Object[] arg = new Object[1];
	arg[0] = name;
	return invokeFunction (obj, "getChildElement", arg);
    }

    /**
     * Return the parent element of this object.
     */
    public Object getParentElement (Object obj) {
	if (obj instanceof IPathElement)
	    return ((IPathElement) obj).getParentElement ();
	return invokeFunction (obj, "getParentElement", null);
    }


    /**
     * Get the name of the prototype to be used for this object. This will
     * determine which scripts, actions and skins can be called on it
     * within the Helma scripting and rendering framework.
     */
    public String getPrototypeName (Object obj) {
	if (obj == null)
	    return "global";
	// check if e implements the IPathElement interface
	if (obj instanceof IPathElement)
	    // e implements the getPrototype() method
	    return ((IPathElement) obj).getPrototype ();
	else
	    // use java class name as prototype name
	    return classMapping.getProperty (obj.getClass ().getName ());
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///   The following methods are the IPathElement interface for this application.
    ///   this is useful for scripting and url-building in the base-app.
    //////////////////////////////////////////////////////////////////////////////////////////////////////////

    public String getElementName()	{
	return name;
    }

    public IPathElement getChildElement(String name)	{
	// as Prototype and the helma.scripting-classes don't offer enough information
	// we use the classes from helma.doc-pacakge for introspection.
	// the first time an url like /appname/api/ is parsed, the application is read again
	// parsed for comments and exposed as an IPathElement
	if (name.equals("api")) {
	    if ( docApp==null ) {
	        try {
	            docApp = new DocApplication( this.name,appDir.toString() );
	        } catch ( DocException e ) {
	            return null;
	        }
	    }
	    return docApp;
	}
	return null;
    }

    public IPathElement getParentElement() {
	return helma.main.Server.getServer();
    }

    public String getPrototype() {
	return "application";
    }


    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Get the logger object for logging generic events
     */
    public void logEvent (String msg) {
	if (eventLog == null || eventLog.isClosed ())
	    eventLog = getLogger ("event");
	eventLog.log (msg);
    }

    /**
     * Get the logger object for logging access events
     */
    public void logAccess (String msg) {
	if (accessLog == null || accessLog.isClosed ())
	    accessLog = getLogger ("access");
	accessLog.log (msg);
    }

    /**
     *  Get a logger object to log events for this application.
     */
    public Logger getLogger (String logname) {
	Logger log = null;
	String logDir = props.getProperty ("logdir");
	if (logDir == null)
	    logDir = "log";
	// allow log to be redirected to System.out by setting logdir to "console"
	if ("console".equalsIgnoreCase (logDir))
	    return new Logger (System.out);
	File helper = new File (logDir);
	// construct the fully qualified log name
	String fullLogname = name +"_"+logname;
	if (home != null && !helper.isAbsolute ())
	    helper = new File (home, logDir);
	logDir = helper.getAbsolutePath ();
	log = Logger.getLogger (logDir, fullLogname);
	return log;
    }


    /**
     *  Get scripting environment for this application
     */
    /* public ScriptingEnvironment getScriptingEnvironment () {
	return scriptingEngine;
    } */


    /**
     * The run method performs periodic tasks like executing the scheduler method and
     * kicking out expired user sessions.
     */
    public void run () {
	long cleanupSleep = 60000;    // thread sleep interval (fixed)
	long scheduleSleep = 60000;  // interval for scheduler invocation
	long lastScheduler = 0;    // run scheduler immediately
	long lastCleanup = System.currentTimeMillis ();

	// logEvent ("Starting scheduler for "+name);
	// as first thing, invoke function onStart in the root object

	// read in standard prototypes to make first request go faster
	typemgr.updatePrototype ("root");
	typemgr.updatePrototype ("global");

	try {
	    eval.invokeFunction ((INode) null, "onStart", new Object[0]);
	} catch (Exception ignore) {
	    System.err.println ("Error in "+name+"/onStart(): "+ignore);
	}

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
	        // logEvent ("Cleaning up "+name+": " + sessions.size () + " sessions active");
	        Hashtable cloned = (Hashtable) sessions.clone ();
	        for (Enumeration e = cloned.elements (); e.hasMoreElements (); ) {
	            User u = (User) e.nextElement ();
	            if (now - u.lastTouched () > sessionTimeout * 60000) {
	                if (u.uid != null) {
	                    try {
	                        eval.invokeFunction (u, "onLogout", new Object[0]);
	                    } catch (Exception ignore) {}
	                    activeUsers.remove (u.uid);
	                }
	                sessions.remove (u.getSessionID ());
	                u.setNode (null);
	            }
	        }
	        // logEvent ("Cleaned up "+name+": " + sessions.size () + " sessions remaining");
	    } catch (Exception cx) {
	        logEvent ("Error cleaning up sessions: "+cx);
	        cx.printStackTrace ();
	    }

	    // check if we should call scheduler
	    if (now - lastScheduler > scheduleSleep) {
	        lastScheduler = now;
	        Object val = null;
	        try {
	            val = eval.invokeFunction ((INode) null, "scheduler", new Object[0]);
	        } catch (Exception ignore) {}
	        try {
	            int ret = ((Number) val).intValue ();
	            if (ret < 1000)
	                scheduleSleep = 60000l;
	            else
	                scheduleSleep = ret;
	        } catch (Exception ignore) {}
	        // logEvent ("Called scheduler for "+name+", will sleep for "+scheduleSleep+" millis");
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

    /**
     *  This method is called after the type.properties files are read on all prototypes, or after one
     * or more of the type properties have been re-read after an update, to let the DbMappings reestablish
     * the relations among them according to their mappings.
     */
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
	                // only use hopobject prototype if we're scripting HopObjects, not
	                // java objects.
	                boolean isjava = isJavaPrototype (typename);
	                if (protoname == null && !isjava)
	                    protoname = "hopobject";
	                Prototype parentProto = (Prototype) typemgr.prototypes.get (protoname);
	                if (parentProto == null && !isjava)
	                    parentProto = (Prototype) typemgr.prototypes.get ("hopobject");
	                if (parentProto != null)
	                    proto.setParentPrototype (parentProto);
	            }
	        }
	    } catch (Exception x) {
	        logEvent ("Error rewiring DbMappings: "+x);
	    }
	}
    }

    /**
     * Check whether a prototype is for scripting a java class, i.e. if there's an entry
     * for it in the class.properties file.
     */
    public boolean isJavaPrototype (String typename) {
	for (Enumeration en = classMapping.elements(); en.hasMoreElements(); ) {
	    String value = (String) en.nextElement ();
	    if (typename.equals (value))
	        return true;
	}
	return false;
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

    /**
     * Return the directory of this application
     */
    public File getAppDir()	{
	return appDir;
    }

    /**
     * Get the DbMapping associated with a prototype name in this application
     */
    public DbMapping getDbMapping (String typename) {
	return typename == null ? null : (DbMapping) dbMappings.get (typename);
    }

    /**
     * Associate a DbMapping object with a prototype name for this application.
     */
    public void putDbMapping (String typename, DbMapping dbmap) {
	dbMappings.put (typename, dbmap);
    }
    /**
     * Proxy method to get a property from the applications properties.
     */
    public String getProperty (String propname) {
	return props.getProperty (propname);
    }

    /**
     * Proxy method to get a property from the applications properties.
     */
    public String getProperty (String propname, String defvalue) {
	return props.getProperty (propname, defvalue);
    }

	public SystemProperties getProperties()	{
		return props;
	}

    /**
     *
     */
    public int countThreads () {
	return threadgroup.activeCount() -1;
    }

    /**
     *
     */
    public int countEvaluators () {
	return allThreads.size()-1 ;
    }

    /**
     *
     */
    public int countFreeEvaluators () {
	return freeThreads.size ();
    }

    /**
     *
     */
    public int countActiveEvaluators () {
	return allThreads.size() - freeThreads.size () -1;
    }

    /**
     *
     */
    public int countMaxActiveEvaluators () {
	// return typemgr.countRegisteredRequestEvaluators () -1;
	// not available due to framework refactoring
	return -1;
    }

    /**
     *
     */
    public long getRequestCount () {
	return requestCount;
    }

    /**
     *
     */
    public long getXmlrpcCount () {
	return xmlrpcCount;
    }

    /**
     *
     */
    public long getErrorCount () {
	return errorCount;
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


/**
 * XML-RPC handler class for this application.
 */
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
	    retval = ev.invokeXmlRpc (method, argvec.toArray());
	}  finally {
	    app.releaseEvaluator (ev);
	}
	return retval;
    }
}


/**
 * XML-RPC access permission checker
 */
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
    *  file like this:
    *
    *    xmlrpcAccess = root.sayHello, story.countMessages, user.login
    *
    *  i.e. a prototype.method entry for each function callable via XML-RPC.
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





