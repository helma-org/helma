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

package helma.framework.core;

import helma.extensions.ConfigurationException;
import helma.extensions.HelmaExtension;
import helma.framework.*;
import helma.main.Server;
import helma.objectmodel.*;
import helma.objectmodel.db.*;
import helma.scripting.*;
import helma.util.*;
import java.io.*;
import java.lang.reflect.*;
import java.net.MalformedURLException;
import java.rmi.*;
import java.util.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


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
    SystemProperties props;

    // properties and db-properties
    SystemProperties dbProps;

    // Helma server home directory
    File home;

    // application directory
    File appDir;

    // embedded db directory
    File dbDir;

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
     * Collections for evaluator thread pooling
     */
    protected Stack freeThreads;
    protected Vector allThreads;
    boolean stopped = false;
    boolean debug;
    long starttime;
    Hashtable sessions;
    Hashtable dbSources;

    // map of app modules reflected at app.modules
    Map modules;

    // internal worker thread for scheduler, session cleanup etc.
    Thread worker;
    long requestTimeout = 60000; // 60 seconds for request timeout.
    ThreadGroup threadgroup;

    // Map of requesttrans -> active requestevaluators
    Hashtable activeRequests;

    // Two logs for each application
    Log eventLog;
    Log accessLog;

    // Symbolic names for each log
    String eventLogName;
    String accessLogName;

    // A transient node that is shared among all evaluators
    protected INode cachenode;

    // some fields for statistics
    protected volatile long requestCount = 0;
    protected volatile long xmlrpcCount = 0;
    protected volatile long errorCount = 0;

    // the URL-prefix to use for links into this application
    private String baseURI;
    // the name of the root prototype
    private String rootPrototype;

    // Db mappings for some standard prototypes
    private DbMapping rootMapping;
    private DbMapping userRootMapping;
    private DbMapping userMapping;

    // name of response encoding
    String charset;

    // password file to use for authenticate() function
    private CryptFile pwfile;

    // Map of java class names to object prototypes
    SystemProperties classMapping;

    // Map of extensions allowed for public skins
    Properties skinExtensions;

    // time we last read the properties file
    private long lastPropertyRead = 0L;

    // the set of prototype/function pairs which are allowed to be called via XML-RPC
    private HashSet xmlrpcAccess;

    // the name under which this app serves XML-RPC requests. Defaults to the app name
    private String xmlrpcHandlerName;

    // the list of currently active cron jobs
    private Map activeCronJobs = null;
    // the list of custom cron jobs
    Hashtable customCronJobs = null;

    /**
     *  Simple constructor for dead application instances.
     */
    public Application(String name) {
        this.name = name;
    }

    /**
     * Build an application with the given name in the app directory. No Server-wide
     * properties are created or used.
     */
    public Application(String name, File appDir, File dbDir)
                throws RemoteException, IllegalArgumentException {
        this(name, null, appDir, dbDir);
    }

    /**
     * Build an application with the given name and server instance. The
     * app directories will be created if they don't exist already.
     */
    public Application(String name, Server server)
                throws RemoteException, IllegalArgumentException {
        this(name, server, null, null);
    }

    /**
     * Build an application with the given name, server instance, app and
     * db directories.
     */
    public Application(String name, Server server, File customAppDir, File customDbDir)
                throws RemoteException, IllegalArgumentException {
        if ((name == null) || (name.trim().length() == 0)) {
            throw new IllegalArgumentException("Invalid application name: " + name);
        }

        this.name = name;
        appDir = customAppDir;
        dbDir = customDbDir;

        // system-wide properties, default to null
        SystemProperties sysProps;

        // system-wide properties, default to null
        SystemProperties sysDbProps;

        sysProps = sysDbProps = null;
        home = null;

        if (server != null) {
            home = server.getHopHome();

            // if appDir and dbDir weren't explicitely passed, use the
            // standard subdirectories of the Hop home directory
            if (appDir == null) {
                appDir = new File(server.getAppsHome(), name);
            }

            if (dbDir == null) {
                dbDir = new File(server.getDbHome(), name);
            }

            // get system-wide properties
            sysProps = server.getProperties();
            sysDbProps = server.getDbProperties();
        }

        // create the directories if they do not exist already
        if (!appDir.exists()) {
            appDir.mkdirs();
        }

        if (!dbDir.exists()) {
            dbDir.mkdirs();
        }

        // give the Helma Thread group a name so the threads can be recognized
        threadgroup = new ThreadGroup("TX-" + name);

        // create app-level properties
        File propfile = new File(appDir, "app.properties");

        props = new SystemProperties(propfile.getAbsolutePath(), sysProps);

        // get log names
        accessLogName = props.getProperty("accessLog", "helma."+name+".access");
        eventLogName = props.getProperty("eventLog", "helma."+name+".event");

        // create app-level db sources
        File dbpropfile = new File(appDir, "db.properties");

        dbProps = new SystemProperties(dbpropfile.getAbsolutePath(), sysDbProps);

        // the passwd file, to be used with the authenticate() function
        CryptFile parentpwfile = null;

        if (home != null) {
            parentpwfile = new CryptFile(new File(home, "passwd"), null);
        }

        pwfile = new CryptFile(new File(appDir, "passwd"), parentpwfile);

        // the properties that map java class names to prototype names
        File classMappingFile = new File(appDir, "class.properties");

        classMapping = new SystemProperties(classMappingFile.getAbsolutePath());
        classMapping.setIgnoreCase(false);

        // get class name of root object if defined. Otherwise native Helma objectmodel will be used.
        rootObjectClass = classMapping.getProperty("root");

        updateProperties();

        sessions = new Hashtable();
        dbSources = new Hashtable();
        modules = new SystemMap();

        cachenode = new TransientNode("app");
    }

    /**
     * Get the application ready to run, initializing the evaluators and type manager.
     */
    public void init()
              throws DatabaseException, ScriptingException, MalformedURLException {

        // create and init type mananger
        typemgr = new TypeManager(this);
        typemgr.createPrototypes();

        // set the context classloader. Note that this must be done before
        // using the logging framework so that a new LogFactory gets created
        // for this app.
        Thread.currentThread().setContextClassLoader(typemgr.getClassLoader());

        if (Server.getServer() != null) {
            Vector extensions = Server.getServer().getExtensions();

            for (int i = 0; i < extensions.size(); i++) {
                HelmaExtension ext = (HelmaExtension) extensions.get(i);

                try {
                    ext.applicationStarted(this);
                } catch (ConfigurationException e) {
                    logEvent("couldn't init extension " + ext.getName() + ": " +
                             e.toString());
                }
            }
        }

        // read the sessions if wanted
        if ("true".equalsIgnoreCase(getProperty("persistentSessions"))) {
            loadSessionData(null);
        }

        // create and init evaluator/thread lists
        freeThreads = new Stack();
        allThreads = new Vector();

        // preallocate minThreads request evaluators
        int minThreads = 0;

        try {
            minThreads = Integer.parseInt(props.getProperty("minThreads"));
        } catch (Exception ignore) {
            // not parsable as number, keep 0
        }

        logEvent("Starting "+minThreads+" evaluator(s) for " + name);

        for (int i = 0; i < minThreads; i++) {
            RequestEvaluator ev = new RequestEvaluator(this);

            ev.initScriptingEngine();
            freeThreads.push(ev);
            allThreads.addElement(ev);
        }

        activeRequests = new Hashtable();
        activeCronJobs = new WeakHashMap();
        customCronJobs = new Hashtable();

        // create the skin manager
        skinmgr = new SkinManager(this);

        rootMapping = getDbMapping("root");
        userMapping = getDbMapping("user");

        // The whole user/userroot handling is basically old
        // ugly obsolete crap. Don't bother.
        SystemProperties p = new SystemProperties();
        String usernameField = userMapping.getNameField();

        if (usernameField == null) {
            usernameField = "name";
        }

        p.put("_children", "collection(user)");
        p.put("_children.accessname", usernameField);
        userRootMapping = new DbMapping(this, "__userroot__", p);
        userRootMapping.update();

        // create the node manager
        nmgr = new NodeManager(this, dbDir.getAbsolutePath(), props);

        // reset the classloader to the parent/system/server classloader.
        Thread.currentThread().setContextClassLoader(typemgr.getClassLoader().getParent());

    }

    /**
     *  Create and start scheduler and cleanup thread
     */
    public void start() {
        starttime = System.currentTimeMillis();

        // read in standard prototypes to make first request go faster
        // typemgr.updatePrototype("root");
        // typemgr.updatePrototype("global");

        // as first thing, invoke global onStart() function
        RequestEvaluator eval = getEvaluator();
        try {
            eval.invokeInternal(null, "onStart", new Object[0]);
        } catch (Exception ignore) {
            logEvent("Error in " + name + "/onStart(): " + ignore);
        } finally {
            if (!stopped) {
                releaseEvaluator(eval);
            }
        }

        worker = new Thread(this, "Worker-" + name);
        worker.setPriority(Thread.NORM_PRIORITY + 1);
        worker.start();
    }

    /**
     * This is called to shut down a running application.
     */
    public void stop() {
        stopped = true;

        // stop all threads, this app is going down
        if (worker != null) {
            worker.interrupt();
        }

        worker = null;

        // stop evaluators
        if (allThreads != null) {
            for (Enumeration e = allThreads.elements(); e.hasMoreElements();) {
                RequestEvaluator ev = (RequestEvaluator) e.nextElement();

                ev.stopThread();
            }
        }

        // remove evaluators
        allThreads.removeAllElements();
        freeThreads = null;

        // shut down node manager and embedded db
        try {
            nmgr.shutdown();
        } catch (DatabaseException dbx) {
            System.err.println("Error shutting down embedded db: " + dbx);
        }

        // null out type manager
        typemgr = null;

        // tell the extensions that we're stopped.
        if (Server.getServer() != null) {
            Vector extensions = Server.getServer().getExtensions();

            for (int i = 0; i < extensions.size(); i++) {
                HelmaExtension ext = (HelmaExtension) extensions.get(i);

                ext.applicationStopped(this);
            }
        }

        // store the sessions if wanted
        if ("true".equalsIgnoreCase(getProperty("persistentSessions"))) {
            storeSessionData(null);
        }

    }

    /**
     * Returns a free evaluator to handle a request.
     */
    protected RequestEvaluator getEvaluator() {
        if (stopped) {
            throw new ApplicationStoppedException();
        }

        // first try
        try {
            return (RequestEvaluator) freeThreads.pop();
        } catch (EmptyStackException nothreads) {
            int maxThreads = 12;

            try {
                maxThreads = Integer.parseInt(props.getProperty("maxThreads"));
            } catch (Exception ignore) {
                // property not set, use default value
            }

            synchronized (this) {
                // allocate a new evaluator
                if (allThreads.size() < maxThreads) {
                    logEvent("Starting evaluator " + (allThreads.size() + 1) +
                             " for application " + name);

                    RequestEvaluator ev = new RequestEvaluator(this);

                    allThreads.addElement(ev);

                    return (ev);
                }
            }
        }

        // we can't create a new evaluator, so we wait if one becomes available.
        // give it 3 more tries, waiting 3 seconds each time.
        for (int i = 0; i < 4; i++) {
            try {
                Thread.sleep(3000);

                return (RequestEvaluator) freeThreads.pop();
            } catch (EmptyStackException nothreads) {
                // do nothing
            } catch (InterruptedException inter) {
                throw new RuntimeException("Thread interrupted.");
            }
        }

        // no luck, give up.
        throw new RuntimeException("Maximum Thread count reached.");
    }

    /**
     * Returns an evaluator back to the pool when the work is done.
     */
    protected void releaseEvaluator(RequestEvaluator ev) {
        if (ev != null) {
            ev.recycle();
            freeThreads.push(ev);
        }
    }

    /**
     * This can be used to set the maximum number of evaluators which will be allocated.
     * If evaluators are required beyound this number, an error will be thrown.
     */
    public boolean setNumberOfEvaluators(int n) {
        if ((n < 2) || (n > 511)) {
            return false;
        }

        int current = allThreads.size();

        synchronized (allThreads) {
            if (n > current) {
                int toBeCreated = n - current;

                for (int i = 0; i < toBeCreated; i++) {
                    RequestEvaluator ev = new RequestEvaluator(this);

                    freeThreads.push(ev);
                    allThreads.addElement(ev);
                }
            } else if (n < current) {
                int toBeDestroyed = current - n;

                for (int i = 0; i < toBeDestroyed; i++) {
                    try {
                        RequestEvaluator re = (RequestEvaluator) freeThreads.pop();

                        allThreads.removeElement(re);

                        // typemgr.unregisterRequestEvaluator (re);
                        re.stopThread();
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
    public int getActiveThreads() {
        return 0;
    }

    /**
     *  Execute a request coming in from a web client.
     */
    public ResponseTrans execute(RequestTrans req) {
        requestCount += 1;

        // get user for this request's session
        Session session = checkSession(req.session);

        session.touch();

        ResponseTrans res = null;
        RequestEvaluator ev = null;

        // are we responsible for releasing the evaluator and closing the result?
        boolean primaryRequest = false;

        try {
            // first look if a request with same user/path/data is already being executed.
            // if so, attach the request to its output instead of starting a new evaluation
            // this helps to cleanly solve "doubleclick" kind of users
            ev = (RequestEvaluator) activeRequests.get(req);

            if (ev != null) {
                res = ev.attachRequest(req);
            }

            if (res == null) {
                primaryRequest = true;

                // if attachRequest returns null this means we came too late
                // and the other request was finished in the meantime
                // check if the properties file has been updated
                updateProperties();

                // get evaluator and invoke
                ev = getEvaluator();
                res = ev.invokeHttp(req, session);
            }
        } catch (ApplicationStoppedException stopped) {
            // let the servlet know that this application has gone to heaven
            throw stopped;
        } catch (Exception x) {
            errorCount += 1;
            res = new ResponseTrans();
            res.write("Error in application: <b>" + x.getMessage() + "</b>");
        } finally {
            if (primaryRequest) {
                activeRequests.remove(req);
                releaseEvaluator(ev);

                // response needs to be closed/encoded before sending it back
                try {
                    res.close(charset);
                } catch (UnsupportedEncodingException uee) {
                    logEvent("Unsupported response encoding: " + uee.getMessage());
                }
            } else {
                res.waitForClose();
            }
        }

        return res;
    }

    /**
     *  Called to execute a method via XML-RPC, usally by helma.main.ApplicationManager
     *  which acts as default handler/request dispatcher.
     */
    public Object executeXmlRpc(String method, Vector args)
                         throws Exception {
        xmlrpcCount += 1;

        Object retval = null;
        RequestEvaluator ev = null;

        try {
            // check if the properties file has been updated
            updateProperties();

            // get evaluator and invoke
            ev = getEvaluator();
            retval = ev.invokeXmlRpc(method, args.toArray());
        } finally {
            releaseEvaluator(ev);
        }

        return retval;
    }


    public Object executeExternal(String method, Vector args)
                        throws Exception {
        Object retval = null;
        RequestEvaluator ev = null;
        try {
            // check if the properties file has been updated
            updateProperties();
            // get evaluator and invoke
            ev = getEvaluator();
            retval = ev.invokeExternal(method, args.toArray());
        } finally {
            releaseEvaluator(ev);
        }
        return retval;
    }

    /**
     * Reset the application's object cache, causing all objects to be refetched from
     * the database.
     */
    public void clearCache() {
        nmgr.clearCache();
    }

    /**
     * Returns the number of elements in the NodeManager's cache
     */
    public int getCacheUsage() {
        return nmgr.countCacheEntries();
    }

    /**
     *  Set the application's root element to an arbitrary object. After this is called
     *  with a non-null object, the helma node manager will be bypassed. This function
     * can be used to script and publish any Java object structure with Helma.
     */
    public void setDataRoot(Object root) {
        this.rootObject = root;
    }

    /**
     * This method returns the root object of this application's object tree.
     */
    public Object getDataRoot() {
        // check if we have a custom root object class
        if (rootObjectClass != null) {
            // create custom root element.
            if (rootObject == null) {
                try {
                    if (classMapping.containsKey("root.factory.class") &&
                            classMapping.containsKey("root.factory.method")) {
                        String rootFactory = classMapping.getProperty("root.factory.class");
                        Class c = typemgr.getClassLoader().loadClass(rootFactory);
                        Method m = c.getMethod(classMapping.getProperty("root.factory.method"),
                                               null);

                        rootObject = m.invoke(c, null);
                    } else {
                        String rootClass = classMapping.getProperty("root");
                        Class c = typemgr.getClassLoader().loadClass(rootClass);

                        rootObject = c.newInstance();
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Error creating root object: " +
                                               e.toString());
                }
            }

            return rootObject;
        }
        // no custom root object is defined - use standard helma objectmodel
        else {
            String rootId = props.getProperty("rootid", "0");
            rootObject = nmgr.safe.getNode(rootId, rootMapping);

            return rootObject;
        }
    }

    /**
     * Returns the Object which contains registered users of this application.
     */
    public INode getUserRoot() {
        INode users = nmgr.safe.getNode("1", userRootMapping);

        users.setDbMapping(userRootMapping);

        return users;
    }

    /**
     * Returns the node manager for this application. The node manager is
     * the gateway to the helma.objectmodel packages, which perform the mapping
     * of objects to relational database tables or the embedded database.
     */
    public NodeManager getNodeManager() {
        return nmgr;
    }

    /**
     * Returns a wrapper containing the node manager for this application. The node manager is
     * the gateway to the helma.objectmodel packages, which perform the mapping of objects to
     * relational database tables or the embedded database.
     */
    public WrappedNodeManager getWrappedNodeManager() {
        return nmgr.safe;
    }

    /**
     *  Return a transient node that is shared by all evaluators of this application ("app node")
     */
    public INode getCacheNode() {
        return cachenode;
    }

    /**
     * Returns a Node representing a registered user of this application by his or her user name.
     */
    public INode getUserNode(String uid) {
        try {
            INode users = getUserRoot();

            return (INode) users.getChildElement(uid);
        } catch (Exception x) {
            return null;
        }
    }

    /**
     * Return a prototype for a given node. If the node doesn't specify a prototype,
     * return the generic hopobject prototype.
     */
    public Prototype getPrototype(Object obj) {
        String protoname = getPrototypeName(obj);

        if (protoname == null) {
            return typemgr.getPrototype("hopobject");
        }

        Prototype p = typemgr.getPrototype(protoname);

        if (p == null) {
            p = typemgr.getPrototype("hopobject");
        }

        return p;
    }

    /**
     * Return the prototype with the given name, if it exists
     */
    public Prototype getPrototypeByName(String name) {
        return typemgr.getPrototype(name);
    }

    /**
     * Return a collection containing all prototypes defined for this application
     */
    public Collection getPrototypes() {
        return typemgr.getPrototypes();
    }

    /**
     *  Return a skin for a given object. The skin is found by determining the prototype
     *  to use for the object, then looking up the skin for the prototype.
     */
    public Skin getSkin(String protoname, String skinname, Object[] skinpath) {
        Prototype proto = getPrototypeByName(protoname);

        if (proto == null) {
            return null;
        }

        return skinmgr.getSkin(proto, skinname, skinpath);
    }

    /**
     * Return the session currently associated with a given Hop session ID.
     * Create a new session if necessary.
     */
    public Session checkSession(String sessionID) {
        Session session = getSession(sessionID);

        if (session == null) {
            session = new Session(sessionID, this);
            sessions.put(sessionID, session);
        }

        return session;
    }

    /**
     * Remove the session from the sessions-table and logout the user.
     */
    public void destroySession(String sessionID) {
        logoutSession(getSession(sessionID));
        sessions.remove(sessionID);
    }

    /**
     * Remove the session from the sessions-table and logout the user.
     */
    public void destroySession(Session session) {
        logoutSession(session);
        sessions.remove(session.getSessionID());
    }

    /**
     *  Return the whole session map. We return a clone of the table to prevent
     * actual changes from the table itself, which is managed by the application.
     * It is safe and allowed to manipulate the session objects contained in the table, though.
     */
    public Map getSessions() {
        return (Map) sessions.clone();
    }

    /**
     * Return a list of Helma nodes (HopObjects -  the database object representing the user,
     *  not the session object) representing currently logged in users.
     */
    public List getActiveUsers() {
        ArrayList list = new ArrayList();

        // used to keep track of already added users - we only return
        // one object per user, and users may have multiple sessions
        HashSet usernames = new HashSet();

        for (Enumeration e = sessions.elements(); e.hasMoreElements();) {
            Session s = (Session) e.nextElement();

            if (s == null) {
                continue;
            } else if (s.isLoggedIn() && !usernames.contains(s.getUID())) {
                // returns a session if it is logged in and has not been
                // returned before (so for each logged-in user we get one
                // session object, even if this user is logged in several
                // times (used to retrieve the active users list).
                INode node = s.getUserNode();

                // we check again because user may have been logged out between the first check
                if (node != null) {
                    usernames.add(s.getUID());
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
    public List getRegisteredUsers() {
        ArrayList list = new ArrayList();
        INode users = getUserRoot();

        // add all child nodes to the list
        for (Enumeration e = users.getSubnodes(); e.hasMoreElements();) {
            list.add(e.nextElement());
        }

        // if none, try to get them from properties (internal db)
        if (list.size() == 0) {
            for (Enumeration e = users.properties(); e.hasMoreElements();) {
                list.add(users.getNode((String) e.nextElement()));
            }
        }

        return list;
    }

    /**
     * Return an array of <code>SessionBean</code> objects currently associated with a given
     * Helma user.
     */
    public List getSessionsForUsername(String username) {
        ArrayList list = new ArrayList();

        if (username == null) {
            return list;
        }

        for (Enumeration e = sessions.elements(); e.hasMoreElements();) {
            Session s = (Session) e.nextElement();

            if (s == null) {
                continue;
            } else if (username.equals(s.getUID())) {
                // append to list if session is logged in and fits the given username
                list.add(new SessionBean(s));
            }
        }

        return list;
    }

    /**
     * Return the session currently associated with a given Hop session ID.
     */
    public Session getSession(String sessionID) {
        if (sessionID == null) {
            return null;
        }

        return (Session) sessions.get(sessionID);
    }

    /**
     * Register a user with the given user name and password.
     */
    public INode registerUser(String uname, String password) {
        if (uname == null) {
            return null;
        }

        uname = uname.toLowerCase().trim();

        if ("".equals(uname)) {
            return null;
        }

        INode unode = null;

        try {
            INode users = getUserRoot();

            unode = (INode) users.getChildElement(uname);

            if (unode != null) {
                return null;
            }

            unode = users.createNode(uname);
            unode.setPrototype("user");
            unode.setDbMapping(userMapping);

            String usernameField = userMapping.getNameField();
            String usernameProp = null;

            if (usernameField != null) {
                usernameProp = userMapping.columnNameToProperty(usernameField);
            }

            if (usernameProp == null) {
                usernameProp = "name";
            }

            unode.setName(uname);
            unode.setString(usernameProp, uname);
            unode.setString("password", password);

            return unode;
        } catch (Exception x) {
            logEvent("Error registering User: " + x);

            return null;
        }
    }

    /**
     * Log in a user given his or her user name and password.
     */
    public boolean loginSession(String uname, String password, Session session) {
        // Check the name/password of a user and log it in to the current session
        if (uname == null) {
            return false;
        }

        uname = uname.toLowerCase().trim();

        if ("".equals(uname)) {
            return false;
        }

        try {
            INode users = getUserRoot();
            Node unode = (Node) users.getChildElement(uname);
            String pw = unode.getString("password");

            if ((pw != null) && pw.equals(password)) {
                // let the old user-object forget about this session
                logoutSession(session);
                session.login(unode);

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
    public void logoutSession(Session session) {
        session.logout();
    }

    /**
     * In contrast to login, this works outside the Hop user object framework. Instead, the user is
     * authenticated against a passwd file in the application directory. This is to have some sort of
     * authentication available *before* the application is up and running, i.e. for application setup tasks.
     */
    public boolean authenticate(String uname, String password) {
        if ((uname == null) || (password == null)) {
            return false;
        }

        return pwfile.authenticate(uname, password);
    }

    /**
     *  Return the href to the root of this application.
     */
    public String getRootHref() {
        return getNodeHref(getDataRoot(), null);
    }

    /**
     * Return a path to be used in a URL pointing to the given element  and action
     */
    public String getNodeHref(Object elem, String actionName) {
        StringBuffer b = new StringBuffer(baseURI);

        composeHref(elem, b, 0);

        if (actionName != null) {
            b.append(UrlEncoded.encode(actionName));
        }

        return b.toString();
    }

    private final void composeHref(Object elem, StringBuffer b, int pathCount) {
        if ((elem == null) || (pathCount > 20)) {
            return;
        }

        if ((rootPrototype != null) && rootPrototype.equals(getPrototypeName(elem))) {
            return;
        }

        Object parent = getParentElement(elem);

        if (parent == null) {
            return;
        }

        composeHref(getParentElement(elem), b, pathCount++);
        b.append(UrlEncoded.encode(getElementName(elem)));
        b.append("/");
    }

    /**
     *  Returns the baseURI for Hrefs in this application.
     */
    public String getBaseURI() {
        return baseURI;
    }


    /**
     *  This method sets the base URL of this application which will be prepended to
     *  the actual object path.
     */
    public void setBaseURI(String uri) {
        if (uri == null) {
            this.baseURI = "/";
        } else if (!uri.endsWith("/")) {
            this.baseURI = uri + "/";
        } else {
            this.baseURI = uri;
        }
    }

    /**
     *  Return true if the baseURI property is defined in the application
     *  properties, false otherwise.
     */
    public boolean hasExplicitBaseURI() {
        return props.containsKey("baseuri");
    }

    /**
     *  Returns the prototype name that Hrefs in this application should start with.
     */
    public String getRootPrototype() {
        return rootPrototype;
    }

    /**
     * Tell other classes whether they should output logging information for this application.
     */
    public boolean debug() {
        return debug;
    }

    /**
     *
     *
     * @return ...
     */
    public RequestEvaluator getCurrentRequestEvaluator() {
        Thread thread = Thread.currentThread();
        int l = allThreads.size();

        for (int i = 0; i < l; i++) {
            RequestEvaluator r = (RequestEvaluator) allThreads.get(i);

            if ((r != null) && (r.rtx == thread)) {
                return r;
            }
        }

        return null;
    }

    /**
     *  Utility function invoker for the methods below. This *must* be called
     *  by an active RequestEvaluator thread.
     */
    private Object invokeFunction(Object obj, String func, Object[] args) {
        RequestEvaluator reval = getCurrentRequestEvaluator();

        if (args == null) {
            args = new Object[0];
        }

        if (reval != null) {
            try {
                return reval.invokeDirectFunction(obj, func, args);
            } catch (Exception x) {
                if (debug) {
                    System.err.println("Error in Application.invokeFunction (" +
                                        func + "): " + x);
                }
            }
        }

        return null;
    }

    /**
     *  Return the application's classloader
     */
    public ClassLoader getClassLoader() {
        return typemgr.getClassLoader();
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
    public String getElementName(Object obj) {
        if (obj instanceof IPathElement) {
            return ((IPathElement) obj).getElementName();
        }

        Object retval = invokeFunction(obj, "getElementName", new Object[0]);

        if (retval != null) {
            return retval.toString();
        }

        return null;
    }

    /**
     * Retrieve a child element of this object by name.
     */
    public Object getChildElement(Object obj, String name) {
        if (obj instanceof IPathElement) {
            return ((IPathElement) obj).getChildElement(name);
        }

        Object[] arg = new Object[1];

        arg[0] = name;

        return invokeFunction(obj, "getChildElement", arg);
    }

    /**
     * Return the parent element of this object.
     */
    public Object getParentElement(Object obj) {
        if (obj instanceof IPathElement) {
            return ((IPathElement) obj).getParentElement();
        }

        return invokeFunction(obj, "getParentElement", new Object[0]);
    }

    /**
     * Get the name of the prototype to be used for this object. This will
     * determine which scripts, actions and skins can be called on it
     * within the Helma scripting and rendering framework.
     */
    public String getPrototypeName(Object obj) {
        if (obj == null) {
            return "global";
        }

        // check if e implements the IPathElement interface
        if (obj instanceof IPathElement) {
            // e implements the getPrototype() method
            return ((IPathElement) obj).getPrototype();
        } else {
            // use java class name as prototype name
            return classMapping.getProperty(obj.getClass().getName());
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///   The following methods are the IPathElement interface for this application.
    ///   this is useful for scripting and url-building in the base-app.
    //////////////////////////////////////////////////////////////////////////////////////////////////////////
    public String getElementName() {
        return name;
    }

    /**
     *
     *
     * @param name ...
     *
     * @return ...
     */
    public IPathElement getChildElement(String name) {
        // as Prototype and the helma.scripting-classes don't offer enough information
        // we use the classes from helma.doc-pacakge for introspection.
        // the first time an url like /appname/api/ is parsed, the application is read again
        // parsed for comments and exposed as an IPathElement
        if (name.equals("api") && allThreads.size() > 0) {
            return ((RequestEvaluator) allThreads.get(0)).scriptingEngine.getIntrospector();
        }

        return null;
    }

    /**
     *
     *
     * @return ...
     */
    public IPathElement getParentElement() {
        return helma.main.Server.getServer();
    }

    /**
     *
     *
     * @return ...
     */
    public String getPrototype() {
        return "application";
    }

    ////////////////////////////////////////////////////////////////////////

    /**
     * Log an application error
     */
    public void logError(String msg, Throwable error) {
        if (eventLog == null) {
            eventLog = getLogger(eventLogName);
        }

        eventLog.error(msg, error);
    }

    /**
     * Log an application error
     */
    public void logError(String msg) {
        if (eventLog == null) {
            eventLog = getLogger(eventLogName);
        }

        eventLog.error(msg);
    }

    /**
     * Log a generic application event
     */
    public void logEvent(String msg) {
        if (eventLog == null) {
            eventLog = getLogger(eventLogName);
        }

        eventLog.info(msg);
    }

    /**
     * Log an application access
     */
    public void logAccess(String msg) {
        if (accessLog == null) {
            accessLog = getLogger(accessLogName);
        }

        accessLog.info(msg);
    }

    /**
     *  Get a logger object to log events for this application.
     */
    protected Log getLogger(String logname) {
        String logdir = props.getProperty("logdir", "log");

        if ("console".equals(logdir) || "console".equals(logname)) {
            return Logging.getConsoleLog();
        } else {

            // set up helma.logdir system property in case we're using it
            File dir = new File(logdir);
            System.setProperty("helma.logdir", dir.getAbsolutePath());

            return LogFactory.getLog(logname);
        }
    }

    /**
     * The run method performs periodic tasks like executing the scheduler method and
     * kicking out expired user sessions.
     */
    public void run() {
        // interval between session cleanups
        long sessionCleanupInterval = 60000;
        long lastSessionCleanup = System.currentTimeMillis();

        // logEvent ("Starting scheduler for "+name);

        // loop-local cron job data
        List cronJobs = null;
        long lastCronParse = 0;

        while (Thread.currentThread() == worker) {

            long now = System.currentTimeMillis();

            // check if we should clean up user sessions
            if ((now - lastSessionCleanup) > sessionCleanupInterval) {

                lastSessionCleanup = now;

                // get session timeout
                int sessionTimeout = 30;

                try {
                    sessionTimeout = Math.max(0,
                            Integer.parseInt(props.getProperty("sessionTimeout",
                                                               "30")));
                } catch (Exception ignore) {}

                RequestEvaluator thisEvaluator = null;

                try {

                    thisEvaluator = getEvaluator();

                    Hashtable cloned = (Hashtable) sessions.clone();

                    for (Enumeration e = cloned.elements(); e.hasMoreElements();) {
                        Session session = (Session) e.nextElement();

                        if ((now - session.lastTouched()) > (sessionTimeout * 60000)) {
                            NodeHandle userhandle = session.userHandle;

                            if (userhandle != null) {
                                try {
                                    Object[] param = { session.getSessionID() };

                                    thisEvaluator.invokeInternal(userhandle, "onLogout", param);
                                } catch (Exception ignore) {
                                }
                            }

                            destroySession(session);
                        }
                    }
                } catch (Exception cx) {
                    logEvent("Error cleaning up sessions: " + cx);
                    cx.printStackTrace();
                } finally {
                    if (!stopped && thisEvaluator != null) {
                        releaseEvaluator(thisEvaluator);
                    }
                }
            }

            if ((cronJobs == null) || (props.lastModified() > lastCronParse)) {
                updateProperties();
                cronJobs = CronJob.parse(props);
                lastCronParse = props.lastModified();
            }

            Date d = new Date();
            List jobs = new ArrayList(cronJobs);

            jobs.addAll(customCronJobs.values());
            CronJob.sort(jobs);

            for (Iterator i = jobs.iterator(); i.hasNext();) {
                CronJob j = (CronJob) i.next();

                if (j.appliesToDate(d)) {
                    // check if the job is already active ...
                    if (activeCronJobs.containsKey(j.getName())) {
                        logEvent(j + " is still active, skipped in this minute");

                        continue;
                    }

                    RequestEvaluator thisEvaluator;

                    try {
                        thisEvaluator = getEvaluator();
                    } catch (RuntimeException rt) {
                        if (!stopped) {
                            logEvent("couldn't execute " + j +
                                     ", maximum thread count reached");

                            continue;
                        } else {
                            break;
                        }
                    }

                    // if the job has a long timeout or we're already late during this minute
                    // the job is run from an extra thread
                    if ((j.getTimeout() > 20000) ||
                            (CronJob.millisToNextFullMinute() < 30000)) {
                        CronRunner r = new CronRunner(thisEvaluator, j);

                        activeCronJobs.put(j.getName(), r);
                        r.start();
                    } else {
                        try {
                            thisEvaluator.invokeInternal(null, j.getFunction(),
                                                         new Object[0], j.getTimeout());
                        } catch (Exception ex) {
                            logEvent("error running " + j + ": " + ex.toString());
                        } finally {
                            if (!stopped) {
                                releaseEvaluator(thisEvaluator);
                            }
                        }
                    }
                }
            }


            long sleepInterval = CronJob.millisToNextFullMinute();
            try {
                String sleepProp = props.getProperty("schedulerInterval");
                if (sleepProp != null) {
                    sleepInterval = Math.max(1000, Integer.parseInt(sleepProp)*1000);
                }
            } catch (Exception ignore) {}

            // sleep until the next full minute
            try {
                Thread.sleep(sleepInterval);
            } catch (InterruptedException x) {
                worker = null;
                break;
            }
        }

        // when interrupted, shutdown running cron jobs
        synchronized (activeCronJobs) {
            for (Iterator i = activeCronJobs.keySet().iterator(); i.hasNext();) {
                String jobname = (String) i.next();

                ((CronRunner) activeCronJobs.get(jobname)).interrupt();
                activeCronJobs.remove(jobname);
            }
        }

        logEvent("Scheduler for " + name + " exiting");
    }

    /**
     * Check whether a prototype is for scripting a java class, i.e. if there's an entry
     * for it in the class.properties file.
     */
    public boolean isJavaPrototype(String typename) {
        for (Enumeration en = classMapping.elements(); en.hasMoreElements();) {
            String value = (String) en.nextElement();

            if (typename.equals(value)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Return the java class that a given prototype wraps, or null.
     */
    public String getJavaClassForPrototype(String typename) {

        for (Iterator it = classMapping.entrySet().iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry) it.next();

            if (typename.equals(entry.getValue())) {
                return (String) entry.getKey();
            }
        }

        return null;
    }


    /**
     * Return a DbSource object for a given name. A DbSource is a relational database defined
     * in a db.properties file.
     */
    public DbSource getDbSource(String name) {
        String dbSrcName = name.toLowerCase();
        DbSource dbs = (DbSource) dbSources.get(dbSrcName);

        if (dbs != null) {
            return dbs;
        }

        if ((dbProps.getProperty(dbSrcName + ".url") != null) &&
                (dbProps.getProperty(dbSrcName + ".driver") != null)) {
            try {
                dbs = new DbSource(name, dbProps);
                dbSources.put(dbSrcName, dbs);
            } catch (Exception problem) {
                logEvent("Error creating DbSource " + name);
                logEvent("Reason: " + problem);
            }
        }

        return dbs;
    }

    /**
     * Return the name of this application
     */
    public String getName() {
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
    public DbMapping getDbMapping(String typename) {
        Prototype proto = typemgr.getPrototype(typename);

        if (proto == null) {
            return null;
        }

        return proto.getDbMapping();
    }

    private synchronized void updateProperties() {
        // if so property file has been updated, re-read props.
        if (props.lastModified() > lastPropertyRead) {
            // force property update
            props.update();

            // character encoding to be used for responses
            charset = props.getProperty("charset", "ISO-8859-1");

            // debug flag
            debug = "true".equalsIgnoreCase(props.getProperty("debug"));

            String reqTimeout = props.getProperty("requesttimeout", "60");
            try {
                requestTimeout = Long.parseLong(reqTimeout) * 1000L;
            } catch (Exception ignore) {
                // go with default value
                requestTimeout = 60000L;
            }

            // set base URI
            String base = props.getProperty("baseuri");

            if (base != null) {
                setBaseURI(base);
            } else if (baseURI == null) {
                baseURI = "/";
            }

            rootPrototype = props.getProperty("rootprototype");

            // update the XML-RPC access list, containting prototype.method
            // entries of functions that may be called via XML-RPC
            String xmlrpcAccessProp = props.getProperty("xmlrpcaccess");
            HashSet xra = new HashSet();

            if (xmlrpcAccessProp != null) {
                StringTokenizer st = new StringTokenizer(xmlrpcAccessProp, ",; ");

                while (st.hasMoreTokens()) {
                    String token = st.nextToken().trim();

                    xra.add(token.toLowerCase());
                }
            }

            xmlrpcAccess = xra;

            // if node manager exists, update it
            if (nmgr != null) {
                nmgr.updateProperties(props);
            }

            // update extensions
            if (Server.getServer() != null) {
                Vector extensions = Server.getServer().getExtensions();

                for (int i = 0; i < extensions.size(); i++) {
                    HelmaExtension ext = (HelmaExtension) extensions.get(i);

                    try {
                        ext.applicationUpdated(this);
                    } catch (ConfigurationException e) {
                        logEvent("Error updating extension "+ext+": "+e);
                    }
                }
            }

            // set prop read timestamp
            lastPropertyRead = props.lastModified();
        }
    }

    /**
     *  Get a checksum that mirrors the state of this application in the sense
     *  that if anything in the applciation changes, the checksum hopefully will
     *  change, too.
     */
    public long getChecksum() {
        return starttime + typemgr.getChecksum() + props.getChecksum();
    }

    /**
     * Proxy method to get a property from the applications properties.
     */
    public String getProperty(String propname) {
        return props.getProperty(propname);
    }

    /**
     * Proxy method to get a property from the applications properties.
     */
    public String getProperty(String propname, String defvalue) {
        return props.getProperty(propname, defvalue);
    }

    /**
     *
     *
     * @return ...
     */
    public SystemProperties getProperties() {
        return props;
    }

    /**
     * Return the XML-RPC handler name for this app. The contract is to
     * always return the same string, even if it has been changed in the properties file
     * during runtime, so the app gets unregistered correctly.
     */
    public String getXmlRpcHandlerName() {
        if (xmlrpcHandlerName == null) {
            xmlrpcHandlerName = props.getProperty("xmlrpcHandlerName", this.name);
        }

        return xmlrpcHandlerName;
    }

    /**
     * Return a string representation for this app.
     */
    public String toString() {
        return "[Application "+name+"]";
    }

    /**
     *
     */
    public int countThreads() {
        return threadgroup.activeCount();
    }

    /**
     *
     */
    public int countEvaluators() {
        return allThreads.size();
    }

    /**
     *
     */
    public int countFreeEvaluators() {
        return freeThreads.size();
    }

    /**
     *
     */
    public int countActiveEvaluators() {
        return allThreads.size() - freeThreads.size();
    }

    /**
     *
     */
    public int countMaxActiveEvaluators() {
        // return typemgr.countRegisteredRequestEvaluators () -1;
        // not available due to framework refactoring
        return -1;
    }

    /**
     *
     */
    public long getRequestCount() {
        return requestCount;
    }

    /**
     *
     */
    public long getXmlrpcCount() {
        return xmlrpcCount;
    }

    /**
     *
     */
    public long getErrorCount() {
        return errorCount;
    }

    /**
     *
     *
     * @return ...
     */
    public long getStarttime() {
        return starttime;
    }

    /**
     *
     *
     * @return ...
     */
    public String getCharset() {
        return props.getProperty("charset", "ISO-8859-1");
    }

    /**
     * Periodically called to log thread stats for this application
     */
    public void printThreadStats() {
        logEvent("Thread Stats for " + name + ": " + threadgroup.activeCount() +
                 " active");

        Runtime rt = Runtime.getRuntime();
        long free = rt.freeMemory();
        long total = rt.totalMemory();

        logEvent("Free memory: " + (free / 1024) + " kB");
        logEvent("Total memory: " + (total / 1024) + " kB");
    }

    /**
     * Check if a method may be invoked via XML-RPC on a prototype.
     */
    protected void checkXmlRpcAccess(String proto, String method)
                              throws Exception {
        String key = proto + "." + method;

        // XML-RPC access items are case insensitive and stored in lower case
        if (!xmlrpcAccess.contains(key.toLowerCase())) {
            throw new Exception("Method " + key + " is not callable via XML-RPC");
        }
    }

    /**
     *
     *
     * @param f ...
     */
    public void storeSessionData(File f) {
        if (f == null) {
            f = new File(dbDir, "sessions");
        }

        try {
            OutputStream ostream = new BufferedOutputStream(new FileOutputStream(f));
            ObjectOutputStream p = new ObjectOutputStream(ostream);

            synchronized (sessions) {
                p.writeInt(sessions.size());

                for (Enumeration e = sessions.elements(); e.hasMoreElements();) {
                    p.writeObject(e.nextElement());
                }
            }

            p.flush();
            ostream.close();
            logEvent("stored " + sessions.size() + " sessions in file");
        } catch (Exception e) {
            logEvent("error storing session data: " + e.toString());
        }
    }

    /**
     * loads the serialized session table from a given file or from dbdir/sessions
     */
    public void loadSessionData(File f) {
        if (f == null) {
            f = new File(dbDir, "sessions");
        }

        // compute session timeout value
        int sessionTimeout = 30;

        try {
            sessionTimeout = Math.max(0,
                                      Integer.parseInt(props.getProperty("sessionTimeout",
                                                                         "30")));
        } catch (Exception ignore) {
            System.out.println(ignore.toString());
        }

        long now = System.currentTimeMillis();

        try {
            // load the stored data:
            InputStream istream = new BufferedInputStream(new FileInputStream(f));
            ObjectInputStream p = new ObjectInputStream(istream);
            int size = p.readInt();
            int ct = 0;
            Hashtable newSessions = new Hashtable();

            while (ct < size) {
                Session session = (Session) p.readObject();

                if ((now - session.lastTouched()) < (sessionTimeout * 60000)) {
                    session.setApp(this);
                    newSessions.put(session.getSessionID(), session);
                }

                ct++;
            }

            p.close();
            istream.close();
            sessions = newSessions;
            logEvent("loaded " + newSessions.size() + " sessions from file");
        } catch (Exception e) {
            logEvent("error loading session data: " + e.toString());
        }
    }

    class CronRunner extends Thread {
        RequestEvaluator thisEvaluator;
        CronJob job;

        public CronRunner(RequestEvaluator thisEvaluator, CronJob job) {
            this.thisEvaluator = thisEvaluator;
            this.job = job;
        }

        public void run() {
            try {
                thisEvaluator.invokeInternal(null, job.getFunction(),
                                             new Object[0], job.getTimeout());
            } catch (Exception ex) {
                // gets logged in RequestEvaluator
            } finally {
                if (!stopped) {
                    releaseEvaluator(thisEvaluator);
                }
                thisEvaluator = null;
                activeCronJobs.remove(job.getName());
            }
        }
    }
}
