// Application.java
// Copyright (c) Hannes Wallnöfer 1998-2000

package helma.framework.core;

import helma.extensions.HelmaExtension;
import helma.extensions.ConfigurationException;
import helma.framework.*;
import helma.main.Server;
import helma.scripting.*;
import helma.objectmodel.*;
import helma.objectmodel.db.*;
import helma.util.*;
import org.apache.xmlrpc.*;
import java.util.*;
import java.io.*;
import java.net.URLEncoder;
import java.net.MalformedURLException;
import java.lang.reflect.*;
import java.rmi.*;
import java.rmi.server.*;

/**
 * The central class of a Helma application. This class keeps a pool of so-called
 * request evaluators (threads with JavaScript interpreters), waits for
 * requests from the Web server or XML-RPC port and dispatches them to
 * the evaluators.
 */
public final class Application implements IPathElement, Runnable {

    // the name of this application
    private String name;
    
    // properties and db-properties
    SystemProperties props, dbProps;
    // home, app and data directories
    File home, appDir, dbDir;
    // this application's node manager
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
    long starttime;

    Hashtable sessions;
    Hashtable dbSources;

    // internal worker thread for scheduler, session cleanup etc.
    Thread worker;
    long requestTimeout = 60000; // 60 seconds for request timeout.
    ThreadGroup threadgroup;

    // Map of requesttrans -> active requestevaluators
    Hashtable activeRequests;

    // Two logs for each application: events and accesses
    Logger eventLog, accessLog;

    // A transient node that is shared among all evaluators
    protected INode cachenode;
    
    // some fields for statistics
    protected volatile long requestCount = 0;
    protected volatile long xmlrpcCount = 0;
    protected volatile long errorCount = 0;


    // the URL-prefix to use for links into this application
    private String baseURI;

    private DbMapping rootMapping, userRootMapping, userMapping;

    // name of response encoding
    String charset;

    // password file to use for authenticate() function
    private CryptFile pwfile;

    // Map of java class names to object prototypes
    Properties classMapping;
    // Map of extensions allowed for public skins
    Properties skinExtensions;

    // time we last read the properties file
    private long lastPropertyRead = 0l;
    
    // the set of prototype/function pairs which are allowed to be called via XML-RPC
    private HashSet xmlrpcAccess;

    // the name under which this app serves XML-RPC requests. Defaults to the app name
    private String xmlrpcHandlerName;


    /**
     * Build an application with the given name in the app directory. No Server-wide
     * properties are created or used.
     */
    public Application (String name, File appDir, File dbDir)
		throws RemoteException, IllegalArgumentException {
	this (name, null, appDir, dbDir);
    }

    /**
     * Build an application with the given name and server instance. The
     * app directories will be created if they don't exist already.
     */
    public Application (String name, Server server)
		throws RemoteException, IllegalArgumentException {

	this (name, server, null, null);
    }

    /**
     * Build an application with the given name, server instance, app and
     * db directories.
     */
    public Application (String name, Server server, File customAppDir, File customDbDir)
		throws RemoteException, IllegalArgumentException {

	if (name == null || name.trim().length() == 0)
	    throw new IllegalArgumentException ("Invalid application name: "+name);

	this.name = name;
	appDir = customAppDir;
	dbDir = customDbDir;

	// system-wide properties, default to null
	SystemProperties sysProps, sysDbProps;
	sysProps = sysDbProps = null;
	home = null;

	if (server != null) {
	     home = server.getHopHome ();
	    // if appDir and dbDir weren't explicitely passed, use the
	    // standard subdirectories of the Hop home directory
	    if (appDir == null) {
	        appDir = new File (home, "apps");
	        appDir = new File (appDir, name);
	    }
	    if (dbDir == null) {
	        dbDir = new File (home, "db");
	        dbDir = new File (dbDir, name);
	    }
	    // get system-wide properties
	    sysProps = server.getProperties ();
	    sysDbProps = server.getDbProperties ();
	}
	// create the directories if they do not exist already
	if (!appDir.exists())
	    appDir.mkdirs ();
	if (!dbDir.exists())
	    dbDir.mkdirs ();

	// give the Helma Thread group a name so the threads can be recognized
	threadgroup = new ThreadGroup ("TX-"+name);

	// create app-level properties
	File propfile = new File (appDir, "app.properties");
	props = new SystemProperties (propfile.getAbsolutePath (), sysProps);

	// create app-level db sources
	File dbpropfile = new File (appDir, "db.properties");
	dbProps = new SystemProperties (dbpropfile.getAbsolutePath (), sysDbProps);

	// the passwd file, to be used with the authenticate() function
	CryptFile parentpwfile = null;
	if (home != null)
	    parentpwfile = new CryptFile (new File (home, "passwd"), null);
	pwfile = new CryptFile (new File (appDir, "passwd"), parentpwfile);

	// the properties that map java class names to prototype names
	File classMappingFile = new File (appDir, "class.properties");
	classMapping = new SystemProperties (classMappingFile.getAbsolutePath ());

	// get class name of root object if defined. Otherwise native Helma objectmodel will be used.
	rootObjectClass = classMapping.getProperty ("root");

	updateProperties ();

	sessions = new Hashtable ();
	dbSources = new Hashtable ();

	cachenode = new TransientNode ("app");
    }

    /**
     * Get the application ready to run, initializing the evaluators and type manager.
     */
    public void init () throws DatabaseException, ScriptingException, MalformedURLException {
	// scriptingEngine = new helma.scripting.fesi.FesiScriptingEnvironment ();
	// scriptingEngine.init (this, props);

	Vector extensions = Server.getServer ().getExtensions ();
	for (int i=0; i<extensions.size(); i++) {
	    HelmaExtension ext = (HelmaExtension)extensions.get(i);
	    try {
	        ext.applicationStarted (this);
	    } catch (ConfigurationException e) {
	        logEvent ("couldn't init extension " + ext.getName () + ": " + e.toString ());
	    }
	}

	// read the sessions if wanted
	if ("true".equalsIgnoreCase (getProperty("persistentSessions"))) {
	    loadSessionData (null);
	}

	typemgr = new TypeManager (this);
	typemgr.createPrototypes ();
	// logEvent ("Started type manager for "+name);

	// eval = new RequestEvaluator (this);
	logEvent ("Starting evaluators for "+name);
	freeThreads = new Stack ();
	allThreads = new Vector ();
	// allThreads.addElement (eval);
	
	// preallocate minThreads request evaluators
	int minThreads = 0;
	try {
	    minThreads = Integer.parseInt (props.getProperty ("minThreads"));
	} catch (Exception ignore) {}
	for (int i=0; i<minThreads; i++) {
	    RequestEvaluator ev = new RequestEvaluator (this);
	    ev.initScriptingEngine ();
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
	p.put ("_children", "collection(user)");
	p.put ("_children.accessname", usernameField);
	userRootMapping = new DbMapping (this, "__userroot__", p);
	userRootMapping.update ();

	nmgr = new NodeManager (this, dbDir.getAbsolutePath (), props);
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

	// stop evaluators
	if (allThreads != null) {
	    for (Enumeration e=allThreads.elements (); e.hasMoreElements (); ) {
	        RequestEvaluator ev = (RequestEvaluator) e.nextElement ();
	        ev.stopThread ();
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

	// tell the extensions that we're stopped.
	Vector extensions = Server.getServer ().getExtensions ();
	for (int i=0; i<extensions.size(); i++) {
	    HelmaExtension ext = (HelmaExtension)extensions.get(i);
	    ext.applicationStopped (this);
	}

	// store the sessions if wanted
	if ("true".equalsIgnoreCase (getProperty("persistentSessions"))) {
	    storeSessionData (null);
	}

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
	// first try
	try {
	    return (RequestEvaluator) freeThreads.pop ();
	} catch (EmptyStackException nothreads) {
	    int maxThreads = 12;
	    try {
	        maxThreads = Integer.parseInt (props.getProperty ("maxThreads"));
	    } catch (Exception ignore) {
	        // property not set, use default value
	    }
	    synchronized (this) {
	        // allocate a new evaluator
	        if (allThreads.size() < maxThreads) {
	            logEvent ("Starting evaluator "+(allThreads.size()+1) +" for application "+name);
	            RequestEvaluator ev = new RequestEvaluator (this);
	            allThreads.addElement (ev);
	            return (ev);
	        }
	    }
	}
	// we can't create a new evaluator, so we wait if one becomes available.
	// give it 3 more tries, waiting 3 seconds each time.
	for (int i=0; i<4; i++) {
	    try {
	        Thread.currentThread().sleep(3000);
	        return (RequestEvaluator) freeThreads.pop ();
	    } catch (EmptyStackException nothreads) {
	        // do nothing
	    } catch (InterruptedException inter) {
	        throw new RuntimeException ("Thread interrupted.");
	    }
	}
	// no luck, give up.
	throw new RuntimeException ("Maximum Thread count reached.");
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
	Session session = checkSession (req.session);
	session.touch();

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

	        // check if the properties file has been updated
	        updateProperties ();
	        // get evaluator and invoke
	        ev = getEvaluator ();
	        res = ev.invoke (req, session);
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
     *  Called to execute a method via XML-RPC, usally by helma.main.ApplicationManager
     *  which acts as default handler/request dispatcher.
     */
    public Object executeXmlRpc (String method, Vector args) throws Exception {
	xmlrpcCount += 1;
	Object retval = null;
	RequestEvaluator ev = null;
	try {
	    // check if the properties file has been updated
	    updateProperties ();
	    // get evaluator and invoke
	    ev = getEvaluator ();
	    retval = ev.invokeXmlRpc (method, args.toArray());
	}  finally {
	    releaseEvaluator (ev);
	}
	return retval;
    }

    /**
     * Reset the application's object cache, causing all objects to be refetched from
     * the database.
     */
    public void clearCache () {
	nmgr.clearCache ();
    }

    /**
     * Returns the number of elements in the NodeManager's cache
     */
    public int getCacheUsage () {
	return nmgr.countCacheEntries ();
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
	// check if we have a custom root object class
	if (rootObjectClass != null) {
	    // create custom root element.
	    if (rootObject == null) {
	        try {
	            if ( classMapping.containsKey("root.factory.class") && classMapping.containsKey("root.factory.method") ) {
	                Class c = typemgr.loader.loadClass( classMapping.getProperty("root.factory.class") );
	                Method m = c.getMethod( classMapping.getProperty("root.factory.method"), null );
	                rootObject = m.invoke(c, null);
	            } else {
	                Class c = typemgr.loader.loadClass( classMapping.getProperty("root") );
	                rootObject = c.newInstance();
	            }
	        } catch ( Exception e )	{
	            throw new RuntimeException ( "Error creating root object: " + e.toString() );
	        }
	    }
	    return rootObject;
	}
	// no custom root object is defined - use standard helma objectmodel
	else {
	    // INode root = nmgr.safe.getNode ("0", rootMapping);
	    // root.setDbMapping (rootMapping);
	    // rootObject = root;
	    rootObject = nmgr.safe.getNode ("0", rootMapping);
	    return rootObject;
	}
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
    public INode getCacheNode () {
	return cachenode;
    }


    /**
     * Returns a Node representing a registered user of this application by his or her user name.
     */
    public INode getUserNode (String uid) {
	try {
	    INode users = getUserRoot ();
	    return users.getNode (uid);
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
     *  Return a skin for a given object. The skin is found by determining the prototype
     *  to use for the object, then looking up the skin for the prototype.
     */
    public Skin getSkin (Object object, String skinname, Object[] skinpath) {
	Prototype proto = getPrototype (object);
	if (proto == null)
	    return null;
	return skinmgr.getSkin (proto, skinname, skinpath);
    }

    /**
     * Return the session currently associated with a given Hop session ID.
     * Create a new session if necessary.
     */
    public Session checkSession (String sessionID) {
	Session session = getSession(sessionID);
	if ( session==null )	{
	    session = new Session (sessionID, this);
	    sessions.put (sessionID, session);
	}
	return session;
    }

    /**
     * Remove the session from the sessions-table and logout the user.
     */
    public void destroySession (String sessionID) {
	logoutSession (getSession (sessionID));
	sessions.remove (sessionID);
    }

    /**
     * Remove the session from the sessions-table and logout the user.
     */
    public void destroySession (Session session) {
	logoutSession (session);
	sessions.remove (session.getSessionID ());
    }

    /**
     *  Return the whole session map. We return a clone of the table to prevent
     * actual changes from the table itself, which is managed by the application.
     * It is safe and allowed to manipulate the session objects contained in the table, though.
     */
    public Map getSessions () {
	return (Map) sessions.clone ();
    }


    /**
     * Return a list of Helma nodes (HopObjects -  the database object representing the user,
     *  not the session object) representing currently logged in users.
     */
    public List getActiveUsers () {
	ArrayList list = new ArrayList();
	// used to keep track of already added users - we only return
	// one object per user, and users may have multiple sessions
	HashSet usernames = new HashSet ();
	for (Enumeration e=sessions.elements(); e.hasMoreElements(); ) {
	    Session s = (Session) e.nextElement ();
	    if(s==null) {
	        continue;
	    } else if (s.isLoggedIn() && !usernames.contains (s.getUID ()) ) {
	        // returns a session if it is logged in and has not been
	        // returned before (so for each logged-in user we get one
	        // session object, even if this user is logged in several
	        // times (used to retrieve the active users list).
	        INode node = s.getUserNode ();
	        // we check again because user may have been logged out between the first check
	        if (node != null) {
	            usernames.add (s.getUID ());
	            list.add(node);
	        }
	    }
	}
	return list;
    }


    /**
     * Return a list of Helma nodes (HopObjects -  the database object representing the user,
     *  not the session object) representing registered users of this application.
     */
	public List getRegisteredUsers () {
	ArrayList list = new ArrayList ();
	INode users = getUserRoot ();
	// first try to get them from subnodes (db)
	for (Enumeration e=users.getSubnodes(); e.hasMoreElements(); ) {
	    list.add ((INode)e.nextElement ());
	}
	// if none, try to get them from properties (internal db)
	if (list.size()==0) {
	    for (Enumeration e=users.properties(); e.hasMoreElements(); ) {
	        list.add (users.getNode ((String)e.nextElement ()));
	    }
	}
	return list;
	}


    /**
     * Return an array of <code>SessionBean</code> objects currently associated with a given
     * Helma user.
     */
    public List getSessionsForUsername (String username)	{
	ArrayList list = new ArrayList();
	if (username == null)
	    return list;
	for (Enumeration e=sessions.elements(); e.hasMoreElements(); ) {
	    Session s = (Session) e.nextElement ();
	    if(s==null) {
	        continue;
	    } else if (username.equals (s.getUID ())) {
	        // append to list if session is logged in and fits the given username
	        list.add (new SessionBean (s));
	    }
	}
	return list;
    }


    /**
     * Return the session currently associated with a given Hop session ID.
     */
    public Session getSession (String sessionID) {
	if (sessionID == null)
	    return null;
	return (Session) sessions.get (sessionID);
	}

    /**
     * Register a user with the given user name and password.
     */
    public INode registerUser (String uname, String password) {
	if (uname == null)
	    return null;
	uname = uname.toLowerCase ().trim ();
	if ("".equals (uname))
	    return null;
	INode unode = null;
	try {
	    INode users = getUserRoot ();
	    unode = users.getNode (uname);
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
	    return unode;
	} catch (Exception x) {
	    logEvent ("Error registering User: "+x);
	    return null;
	}
    }

    /**
     * Log in a user given his or her user name and password.
     */
    public boolean loginSession (String uname, String password, Session session) {
	// Check the name/password of a user and log it in to the current session
	if (uname == null)
	    return false;
	uname = uname.toLowerCase ().trim ();
	if ("".equals (uname))
	    return false;
	try {
	    INode users = getUserRoot ();
	    Node unode = (Node) users.getNode (uname);
	    String pw = unode.getString ("password");
	    if (pw != null && pw.equals (password)) {
	        // let the old user-object forget about this session
	        logoutSession(session);
	        session.login (unode);
	        return true;
	    }
	} catch (Exception x) {
	    return false;
	}
	return false;
    }

    /**
     * Log out a session from this application.
     */
    public void logoutSession (Session session) {
	session.logout();
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
	// Object root = getDataRoot ();
	// check optional root prototype from app.properties
	String rootProto = props.getProperty ("rootPrototype");

	StringBuffer b = new StringBuffer (baseURI);

	composeHref (elem, b, rootProto, 0);

	if (actionName != null)
	    b.append (UrlEncoded.encode (actionName));

	return b.toString ();
    }

    private final void composeHref (Object elem, StringBuffer b, String rootProto, int pathCount) {
	if (elem == null || pathCount > 20)
	    return;
	if (rootProto != null && rootProto.equals (getPrototypeName (elem)))
	    return;
	Object parent = getParentElement (elem);
	if (parent == null)
	    return;
	composeHref (getParentElement (elem), b, rootProto, pathCount++);
	b.append (UrlEncoded.encode (getElementName (elem)));
	b.append ("/");
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
     *  Return true if the baseURI property is defined in the application 
     *  properties, false otherwise.
     */
    public boolean hasExplicitBaseURI () {
	return props.containsKey ("baseuri");
    }

    /**
     * Tell other classes whether they should output logging information for this application.
     */
     public boolean debug () {
	return debug;
    }

    public RequestEvaluator getCurrentRequestEvaluator () {
	Thread thread = Thread.currentThread ();
	int l = allThreads.size();
	for (int i=0; i<l; i++) {
	    RequestEvaluator r = (RequestEvaluator) allThreads.get (i);
	    if (r != null && r.rtx == thread)
	        return r;
	}
	return null;
    }

    /**
     *  Utility function invoker for the methods below. This *must* be called
     *  by an active RequestEvaluator thread.
     */
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
	        System.err.println ("Error in Application.invokeFunction ("+func+"): "+x);
	}
	return null;
    }
    

    /**
     *  Return the application's classloader
     */
    public ClassLoader getClassLoader () {
	return typemgr.loader;
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///   The following methods mimic the IPathElement interface. This allows us
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

    public String getElementName() {
	return name;
    }

    public IPathElement getChildElement(String name) {
	// as Prototype and the helma.scripting-classes don't offer enough information
	// we use the classes from helma.doc-pacakge for introspection.
	// the first time an url like /appname/api/ is parsed, the application is read again
	// parsed for comments and exposed as an IPathElement
	if (name.equals("api")) {
	    return eval.scriptingEngine.getIntrospector ();
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
	String logDir = props.getProperty ("logdir", "log");
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

	eval = new RequestEvaluator (this);
	allThreads.addElement (eval);

	// read in standard prototypes to make first request go faster
	typemgr.updatePrototype ("root");
	typemgr.updatePrototype ("global");

	try {
	    eval.invokeFunction ((INode) null, "onStart", new Object[0]);
	} catch (Exception ignore) {
	    logEvent ("Error in "+name+"/onStart(): "+ignore);
	}

	while (Thread.currentThread () == worker) {
	    // get session timeout
	    int sessionTimeout = 30;
	    try {
	        sessionTimeout = Math.max (0, Integer.parseInt (props.getProperty ("sessionTimeout", "30")));
	    } catch (Exception ignore) {
	        System.out.println(ignore.toString());
	    }

	    long now = System.currentTimeMillis ();

	    // check if we should clean up user sessions
	    if (now - lastCleanup > cleanupSleep) try {
	        lastCleanup = now;
	        Hashtable cloned = (Hashtable) sessions.clone ();
	        for (Enumeration e = cloned.elements (); e.hasMoreElements (); ) {
	            Session session = (Session) e.nextElement ();
	            if (now - session.lastTouched () > sessionTimeout * 60000) {
	                NodeHandle userhandle = session.userHandle;
	                if (userhandle != null) {
	                    try {
	                        Object[] param = { session.getSessionID() };
	                        eval.invokeFunction (userhandle, "onLogout", param);
	                    } catch (Exception ignore) {}
	                }
	                destroySession(session);
	            }
	        }
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
    public File getAppDir() {
	return appDir;
    }

    /**
     * Return the directory of the Helma server
     */
    public File getServerDir() {
	return home;
    }


    /**
     * Get the DbMapping associated with a prototype name in this application
     */
    public DbMapping getDbMapping (String typename) {
	Prototype proto = typemgr.getPrototype (typename);
	if (proto == null)
	    return null;
	return proto.getDbMapping ();
    }


    private synchronized void updateProperties () {
	// if so property file has been updated, re-read props.
	if (props.lastModified () > lastPropertyRead) {
	    // character encoding to be used for responses
	    charset = props.getProperty ("charset", "ISO-8859-1");
	    // debug flag
	    debug = "true".equalsIgnoreCase (props.getProperty ("debug"));
	     try {
	        requestTimeout = Long.parseLong (props.getProperty ("requestTimeout", "60"))*1000l;
	    } catch (Exception ignore) {
	        // go with default value
	        requestTimeout = 60000l;
	    }
	    // set base URI
	    String base = props.getProperty ("baseuri");
	    if (base != null)
	        setBaseURI (base);
	    else if (baseURI == null)
	        baseURI = "/";
	    // update the XML-RPC access list, containting prototype.method
	    // entries of functions that may be called via XML-RPC
	    String xmlrpcAccessProp = props.getProperty ("xmlrpcaccess");
	    HashSet xra = new HashSet ();
	    if (xmlrpcAccessProp != null) {
	        StringTokenizer st = new StringTokenizer (xmlrpcAccessProp, ",; ");
	        while (st.hasMoreTokens ()) {
	            String token = st.nextToken ().trim ();
	            xra.add (token.toLowerCase());
	        }
	    }
	    xmlrpcAccess = xra;
	    // if node manager exists, update it
	    if (nmgr != null)
	        nmgr.updateProperties (props);
	    // update extensions
	    Vector extensions = Server.getServer ().getExtensions ();
	    for (int i=0; i<extensions.size(); i++) {
	        HelmaExtension ext = (HelmaExtension)extensions.get(i);
	        try {
	            ext.applicationUpdated (this);
	        } catch (ConfigurationException e) { }
	    }
	    // set prop read timestamp
	    lastPropertyRead = props.lastModified ();
	}
    }


    /**
     *  Get a checksum that mirrors the state of this application in the sense 
     *  that if anything in the applciation changes, the checksum hopefully will 
     *  change, too.
     */
    public long getChecksum () {
	return starttime + typemgr.getChecksum() + props.getChecksum();
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
     * Return the XML-RPC handler name for this app. The contract is to 
     * always return the same string, even if it has been changed in the properties file
     * during runtime, so the app gets unregistered correctly.
     */
    public String getXmlRpcHandlerName () {
	if (xmlrpcHandlerName == null)
	    xmlrpcHandlerName = props.getProperty ("xmlrpcHandlerName", this.name);
	return xmlrpcHandlerName;
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

    public long getStarttime () {
	return starttime;
    }

    public String getCharset () {
    return props.getProperty ("charset", "ISO-8859-1");
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
     * Check if a method may be invoked via XML-RPC on a prototype.
     */
    protected void checkXmlRpcAccess (String proto, String method) throws Exception {
	String key = proto+"."+method;
	// XML-RPC access items are case insensitive and stored in lower case
	if (!xmlrpcAccess.contains (key.toLowerCase()))
	    throw new Exception ("Method "+key+" is not callable via XML-RPC");
    }

	public void storeSessionData (File f) {
	if (f==null)
	    f = new File(dbDir, "sessions");
	try {
	    OutputStream ostream = new BufferedOutputStream (new FileOutputStream(f));
	    ObjectOutputStream p = new ObjectOutputStream(ostream);
	    synchronized (sessions) {
	        p.writeInt (sessions.size ());
	        for (Enumeration e=sessions.elements (); e.hasMoreElements ();) {
	            p.writeObject ((Session) e.nextElement ());
	        }
	    }
	    p.flush();
	    ostream.close();
	    logEvent ("stored " + sessions.size () + " sessions in file");
	} catch (Exception e) {
	    logEvent ("error storing session data: " + e.toString ());
	}
	}


	/**
	  * loads the serialized session table from a given file or from dbdir/sessions
	  */
	public void loadSessionData (File f) {
	if (f==null)
	    f = new File(dbDir, "sessions");
	// compute session timeout value
	int sessionTimeout = 30;
	try {
	    sessionTimeout = Math.max (0, Integer.parseInt (props.getProperty ("sessionTimeout", "30")));
	} catch (Exception ignore) {
	    System.out.println(ignore.toString());
	}
	long now = System.currentTimeMillis ();
	try {
	    // load the stored data:
	    InputStream istream = new BufferedInputStream (new FileInputStream(f));
	    ObjectInputStream p = new ObjectInputStream(istream);
	    int size = p.readInt ();
	    int ct = 0;
	    Hashtable newSessions = new Hashtable ();
	    while (ct < size) {
	        Session session = (Session) p.readObject ();
	        if (now - session.lastTouched () < sessionTimeout * 60000) {
	            session.setApp (this);
	            newSessions.put (session.getSessionID (), session);
	        }
	        ct++;
	    }
	    p.close ();
	    istream.close ();
	    sessions = newSessions;
	    logEvent ("loaded " + newSessions.size () + " sessions from file");
	} catch (Exception e) {
	    logEvent ("error loading session data: " + e.toString ());
	}
	}

}


