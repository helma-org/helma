// RequestEvaluator.java
// Copyright (c) Hannes Wallnöfer 1998-2000
 
package helma.framework.core;

import helma.objectmodel.*;
import helma.objectmodel.db.*;
import helma.framework.*;
import helma.scripting.*;
import helma.scripting.fesi.*;
import helma.scripting.fesi.extensions.*;
import helma.xmlrpc.fesi.*;
import helma.util.*;
import java.util.*;
import java.io.*;
import java.lang.reflect.*;
import Acme.LruHashtable;
import FESI.Data.*;
import FESI.Interpreter.*;
import FESI.Exceptions.*;

/**
 * This class does the work for incoming requests. It holds a transactor thread 
 * and an EcmaScript evaluator to get the work done. Incoming threads are 
 * blocked until the request has been serviced by the evaluator, or the timeout
 * specified by the application has passed. In the latter case, the evaluator thread
 * is killed and an error message is returned. 
 */

public class RequestEvaluator implements Runnable {


    public Application app;
    protected boolean initialized;

    public RequestTrans req;
    public ResponseTrans res;

    volatile Transactor rtx;

    String method;
    ESObject current;
    User user;
    Vector args;
    ESValue[] esargs;
    ESValue esresult;
    Object result;
    Exception exception;
    protected ArrayPrototype reqPath;

    private ESMapWrapper reqData;
    private ESMapWrapper resData;

    // vars for FESI EcmaScript support
    public Evaluator evaluator;
    public ObjectPrototype esObjectPrototype;
    public ObjectPrototype esNodePrototype;
    public ObjectPrototype esUserPrototype;

    public LruHashtable objectcache;
    Hashtable prototypes;
    // Used to cache skins within one request evaluation
    HashMap skincache;

    GlobalObject global;
    HopExtension hopx;
    MailExtension mailx;
    FesiRpcServer xmlrpc;
    ESAppNode appnode;
    static String[] extensions = new String[] {
	"FESI.Extensions.BasicIO",
	"FESI.Extensions.FileIO",
	"helma.xmlrpc.fesi.FesiRpcExtension",
	"helma.scripting.fesi.extensions.ImageExtension",
	"helma.scripting.fesi.extensions.FtpExtension",
	"FESI.Extensions.JavaAccess",
	"FESI.Extensions.OptionalRegExp"};

    // the type of request to be serviced
    int reqtype;
    static final int NONE = 0;        // no request
    static final int HTTP = 1;           // via HTTP gateway
    static final int XMLRPC = 2;      // via XML-RPC
    static final int INTERNAL = 3;     // generic function call, e.g. by scheduler

    INode[] skinmanagers;

    /**
     *  Build a RenderContext from a RequestTrans. Checks if the path is the user home node ("user")
     *  or a subnode of it. Otherwise, the data root of the site is used. Two arrays are built, one
     *  that contains the data nodes, and anotherone with the corresponding Prototypes or Prototype.Parts.
     */
    public RequestEvaluator (Application app) {
    	this.app = app;
	objectcache = new LruHashtable (100, .80f);
	prototypes = new Hashtable ();
	skincache = new HashMap ();
	initEvaluator ();
	initialized = false;
	// startThread ();
    }


    // init Script Evaluator
    private void initEvaluator () {
	try {
	    evaluator = new Evaluator();
	    evaluator.reval = this;
	    global = evaluator.getGlobalObject();
	    for (int i=0; i<extensions.length; i++)
	        evaluator.addExtension (extensions[i]);
	    hopx = new HopExtension ();
	    hopx.initializeExtension (this);
	    mailx = (MailExtension) evaluator.addExtension ("helma.scripting.fesi.extensions.MailExtension");
	    mailx.setProperties (this.app.props);
	    Database dbx = (Database) evaluator.addExtension ("helma.scripting.fesi.extensions.Database");
	    dbx.setApplication (this.app);

	    // fake a cache member like the one found in ESNodes
	    global.putHiddenProperty ("cache", new ESNode (new TransientNode ("cache"), this));
	    global.putHiddenProperty ("undefined", ESUndefined.theUndefined);
	    appnode = new ESAppNode (app.appnode, this);
	    global.putHiddenProperty ("app", appnode);
	    reqPath = new ArrayPrototype (evaluator.getArrayPrototype(), evaluator);
	    reqData = new ESMapWrapper (this);
	    resData = new ESMapWrapper (this);

	} catch (Exception e) {
	    System.err.println("Cannot initialize interpreter");
	    System.err.println("Error: " + e);
	    e.printStackTrace ();
	    throw new RuntimeException (e.getMessage ());
	}
    }


    public void run () {

        int txcount = 0;
        // first, set a local variable to the current transactor thread so we know
        // when it's time to quit because another thread took over.
        Transactor localrtx = (Transactor) Thread.currentThread ();

        try {
	do {

	    // long startCheck = System.currentTimeMillis ();
	    app.typemgr.checkPrototypes ();
	    // evaluators are only initialized as needed, so we need to check that here
	    if (!initialized)
	        app.typemgr.initRequestEvaluator (this);
	    // System.err.println ("Type check overhead: "+(System.currentTimeMillis ()-startCheck)+" millis");
	
	    // object refs to ressolve request path
	    Object root, currentElement;
	
	    // reset skinManager
	    skinmanagers = null;
	    skincache.clear ();

	    switch (reqtype) {
	    case HTTP:
	        int tries = 0;
	        boolean done = false;
	        String error = null;
	        while (!done) {

	            current = null;
	            currentElement = null;
	            reqPath.setSize (0);
	            // delete path objects-via-prototype
	            for (Enumeration en=reqPath.getAllProperties(); en.hasMoreElements(); ) {
	                String pn = (String) en.nextElement ();
	                if (!"length".equalsIgnoreCase (pn)) try {
	                    reqPath.deleteProperty (pn, pn.hashCode ());
	                } catch (Exception ignore) {}
	            }
	
	            try {

	                String requestPath = app.getName()+"/"+req.path;
	                // set Timer to get some profiling data
	                localrtx.timer.reset ();
	                localrtx.timer.beginEvent (requestPath+" init");
	                localrtx.begin (requestPath);

	                Action action = null;

	                root = app.getDataRoot ();

	                ESUser esu = (ESUser) getNodeWrapper (user);
	                // esu.setUser (user);
	                global.putHiddenProperty ("root", getElementWrapper (root));
	                global.putHiddenProperty("user", esu);
	                global.putHiddenProperty ("req", new ESWrapper (req, evaluator));
	                global.putHiddenProperty ("res", new ESWrapper (res, evaluator));
	                if (error != null)
	                    res.error = error;
	                if (user.message != null) {
	                    // bring over the message from a redirect
	                    res.message = user.message;
	                    user.message = null;
	                }
	                global.putHiddenProperty ("path", reqPath);
	                global.putHiddenProperty ("app", appnode);

	                // set and mount the request and response data object
	                reqData.setData (req.getRequestData());
	                req.data = reqData;
	                resData.setData (res.getResponseData());
	                res.data = resData;

	                try {

	                    if (error != null) {
	                        // there was an error in the previous loop, call error handler
	                        currentElement = root;
	                        current = getElementWrapper (root);
	                        reqPath.putProperty (0, current);
	                        reqPath.putHiddenProperty ("root", current);
	                        Prototype p = app.getPrototype (root);
	                        String errorAction = app.props.getProperty ("error", "error");
	                        action = p.getActionOrTemplate (errorAction);
	                        if (action == null)
	                            throw new RuntimeException (error);
	
	                    } else if (req.path == null || "".equals (req.path.trim ())) {
	                        currentElement = root;
	                        current = getElementWrapper (root);
	                        reqPath.putProperty (0, current);
	                        reqPath.putHiddenProperty ("root", current);
	                        Prototype p = app.getPrototype (root);
	                        action = p.getActionOrTemplate (null);
	                        if (action == null)
	                            throw new FrameworkException ("Action not found");

	                    } else {

	                        // march down request path...
	
	                        StringTokenizer st = new StringTokenizer (req.path, "/");
	                        int ntokens = st.countTokens ();
	                        // limit path to < 50 tokens
	                        if (ntokens > 50)
	                            throw new RuntimeException ("Path too long");
	                        String[] pathItems = new String [ntokens];
	                        for (int i=0; i<ntokens; i++)
	                              pathItems[i] = st.nextToken ();
	
	                        currentElement = root;
	                        current = getElementWrapper (root);
	                        reqPath.putProperty (0, current);
	                        reqPath.putHiddenProperty ("root", current);

	                        for (int i=0; i<ntokens; i++) {
	
	                            if (currentElement == null)
	                                throw new FrameworkException ("Object not found.");
	
	                            // the first token in the path needs to be treated seprerately,
	                            // because "/user" is a shortcut to the current user session, while "/users"
	                            // is the mounting point for all users.
	                            if (i == 0 && "user".equalsIgnoreCase (pathItems[i])) {
	                                currentElement = user.getNode ();
	                                if (currentElement != null) {
	                                    current = getElementWrapper (currentElement);
	                                    reqPath.putProperty (1, current);
	                                    reqPath.putHiddenProperty ("user", current);
	                                }
	
	                            } else if (i == 0 && "users".equalsIgnoreCase (pathItems[i])) {
	                                currentElement = app.getUserRoot ();

	                                if (currentElement != null) {
	                                    current = getElementWrapper (currentElement);
	                                    reqPath.putProperty (1, current);
	                                }
	
	                            } else {
	                                if (i == ntokens-1) {
	                                    Prototype p = app.getPrototype (currentElement);
	                                    if (p != null)
	                                        action = p.getActionOrTemplate (pathItems[i]);
	                                }
	
	                                if (action == null) {

	                                    if (pathItems[i].length () == 0)
	                                        continue;

	                                    currentElement = app.getChildElement (currentElement, pathItems[i]);

	                                    // add object to request path if suitable
	                                    if (currentElement != null) {
	                                        // add to reqPath array
	                                        current = getElementWrapper (currentElement);
	                                        reqPath.putProperty (reqPath.size(), current);
	                                        String pt = app.getPrototypeName (currentElement);
	                                        if (pt != null) {
	                                            // if a prototype exists, add also by prototype name
	                                            reqPath.putHiddenProperty (pt, current);
	                                        }
	                                    }
	                                }
	                            }
	                        }

	                        if (currentElement == null)
	                            throw new FrameworkException ("Object not found.");
	                        else
	                            current = getElementWrapper (currentElement);

	                        if (action == null) {
	                            Prototype p = app.getPrototype (currentElement);
	                            if (p != null)
	                                action = p.getActionOrTemplate (null);
	                        }

	                        if (action == null)
	                            throw new FrameworkException ("Action not found");
	                    }

	                } catch (FrameworkException notfound) {
	                    if (error != null)
	                        // we already have an error and the error template wasn't found,
	                        // display it instead of notfound message
	                        throw new RuntimeException ();
	                    // The path could not be resolved. Check if there is a "not found" action
	                    // specified in the property file.
	                    res.status = 404;
	                    String notFoundAction = app.props.getProperty ("notFound", "notfound");
	                    Prototype p = app.getPrototype (root);
	                    action = p.getActionOrTemplate (notFoundAction);
	                    if (action == null)
	                        throw new FrameworkException (notfound.getMessage ());
	                    current = getElementWrapper (root);
	                }

	                localrtx.timer.endEvent (requestPath+" init");

	                try {
	                    localrtx.timer.beginEvent (requestPath+" execute");

	                    // set the req.action property
	                    req.action = action.getName ();
	                    // do the actual action invocation
	                    current.doIndirectCall (evaluator, current, action.getFunctionName (), new ESValue[0]);

	                    // check if the script set the name of a skin to render in res.skin
	                    if (res.skin != null) {
	                        int dot = res.skin.indexOf (".");
	                        ESValue sobj = null;
	                        String sname = res.skin;
	                        if (dot > -1) {
	                            String soname = res.skin.substring (0, dot);
	                            sobj = reqPath.getProperty (soname, soname.hashCode ());
	                            if (sobj == null || sobj == ESUndefined.theUndefined)
	                                throw new RuntimeException ("Skin "+res.skin+" not found in path.");
	                            sname = res.skin.substring (dot+1);
	                        }
	                        Skin skin = getSkin ((ESObject) sobj, sname);
	                        // get the java object wrapped by the script object, if not global
	                        Object obj = sobj == null ? null : sobj.toJavaObject ();
	                        if (skin != null)
	                            skin.render (this, obj, null);
	                        else
	                            throw new RuntimeException ("Skin "+res.skin+" not found in path.");
	                    }

	                    localrtx.timer.endEvent (requestPath+" execute");
	                } catch (RedirectException redirect) {
	                    // res.redirect = redirect.getMessage ();
	                    // if there is a message set, save it on the user object for the next request
	                    if (res.message != null)
	                        user.message = res.message;
	                    done = true;
	                }

	                // check if we're still the one and only or if the waiting thread has given up on us already
	                commitTransaction ();
	                done = true;

	            } catch (ConcurrencyException x) {

	                res.reset ();
	                if (++tries < 8) {
	                    // try again after waiting some period
	                    abortTransaction (true);
	                    try {
	                        // wait a bit longer with each try
	                        int base = 800 * tries;
	                        Thread.currentThread ().sleep ((long) (base + Math.random ()*base*2));
	                    } catch (Exception ignore) {}
	                    continue;
	                } else {
	                    abortTransaction (false);
	                    if (error == null) {
	                        app.errorCount += 1;
	                        error = "Couldn't complete transaction due to heavy object traffic (tried "+tries+" times)";
	                    } else {
	                        // error in error action. use traditional minimal error message
	                        res.write ("<b>Error in application '"+app.getName()+"':</b> <br><br><pre>"+error+"</pre>");
	                        done = true;
	                    }
	                }

	            } catch (Exception x) {

	                abortTransaction (false);

	                app.logEvent ("### Exception in "+app.getName()+"/"+req.path+": "+x);
	                // Dump the profiling data to System.err
	                if (app.debug) {
	                    ((Transactor) Thread.currentThread ()).timer.dump (System.err);
	                    x.printStackTrace ();
	                }

	                // If the transactor thread has been killed by the invoker thread we don't have to
	                // bother for the error message, just quit.
	                if (localrtx != rtx)
	                    break;

	                res.reset ();
	                if (error == null) {
	                    app.errorCount += 1;
	                    error = x.getMessage ();
	                    if (error == null || error.length() == 0)
	                        error = x.toString ();
	                } else {
	                    // error in error action. use traditional minimal error message
	                    res.write ("<b>Error in application '"+app.getName()+"':</b> <br><br><pre>"+error+"</pre>");
	                    done = true;
	                }

	            }
	        }
	        break;
	    case XMLRPC:
	        try {
	            localrtx.begin (app.getName()+":xmlrpc/"+method);

	            root = app.getDataRoot ();

	            global.putHiddenProperty ("root", getElementWrapper (root));
	            global.deleteProperty("user", "user".hashCode());
	            global.deleteProperty ("req", "req".hashCode());
	            global.putHiddenProperty ("res", ESLoader.normalizeValue(res, evaluator));
	            global.deleteProperty ("path", "path".hashCode());
	            global.putHiddenProperty ("app", appnode);

	            resData.setData (res.getResponseData());
	            res.data = resData;

	            // convert arguments
	            int l = args.size ();
	            current = getElementWrapper (root);
	            if (method.indexOf (".") > -1) {
	                StringTokenizer st = new StringTokenizer (method, ".");
	                int cnt = st.countTokens ();
	                for (int i=1; i<cnt; i++) {
	                    String next = st.nextToken ();
	                    try {
	                        current = (ESObject) current.getProperty (next, next.hashCode ());
	                    } catch (Exception x) {
	                        throw new EcmaScriptException ("The property \""+next+"\" is not defined in the remote object.");
	                    }
	                }
	                if (current == null)
	                    throw new EcmaScriptException ("Method name \""+method+"\" could not be resolved.");
	                method = st.nextToken ();
	            }

	            // check XML-RPC access permissions
	            String proto = ((ESNode) current).getNode().getPrototype ();
	            app.checkXmlRpcAccess (proto, method);

	            ESValue esa[] = new ESValue[l];
	            for (int i=0; i<l; i++) {
    	                esa[i] = FesiRpcUtil.convertJ2E (args.elementAt (i), evaluator);
	            }
	
	            result = FesiRpcUtil.convertE2J (current.doIndirectCall (evaluator, current, method, esa));
	            commitTransaction ();

	        } catch (Exception wrong) {

	            abortTransaction (false);

	            // If the transactor thread has been killed by the invoker thread we don't have to
	            // bother for the error message, just quit.
	            if (localrtx != rtx) {
	                return;
	            }

	            this.exception = wrong;
	        }

	        break;
	    case INTERNAL:
	        esresult = ESNull.theNull;
	        // Just a human readable descriptor of this invocation
	        String funcdesc = app.getName()+":internal/"+method;
	
	        // avoid going into transaction if called function doesn't exist
	        boolean functionexists = true;
	        if (current != null) try {
	            functionexists = global.getProperty (method, method.hashCode()) != ESUndefined.theUndefined;
	        } catch (EcmaScriptException x) {}
	
	        if (!functionexists)
	            // global function doesn't exist, nothing to do here.
	            reqtype = NONE;
	        else try {
	            localrtx.begin (funcdesc);

	            root = app.getDataRoot ();

	            global.putHiddenProperty ("root", getElementWrapper (root));
	            global.deleteProperty("user", "user".hashCode());
	            global.deleteProperty ("req", "req".hashCode());
	            global.putHiddenProperty ("res", ESLoader.normalizeValue(res, evaluator));
	            global.deleteProperty ("path", "path".hashCode());
	            global.putHiddenProperty ("app", appnode);

	            resData.setData (res.getResponseData());
	            res.data = resData;

	            if (current == null) {
	                if (user == null) {
	                    current = global;
	                } else {
	                    ESUser esu = (ESUser) getNodeWrapper (user);
	                    // esu.setUser (user);
	                    current = esu;
	                }
	            }
	            // call internal functions only if they're specified
	            if (current.getProperty (method, method.hashCode()) != ESUndefined.theUndefined)
	                esresult = current.doIndirectCall (evaluator, current, method, new ESValue[0]);
	            commitTransaction ();

	        } catch (Throwable wrong) {

	            abortTransaction (false);

	            // If the transactor thread has been killed by the invoker thread we don't have to
	            // bother for the error message, just quit.
	            if (localrtx != rtx) {
	                return;
	            }

	            String msg = wrong.getMessage ();
	            if (msg == null || msg.length () == 0)
	                msg = wrong.toString ();
	            app.logEvent ("Error executing "+funcdesc+": "+msg);
	            if (app.debug)
	                wrong.printStackTrace ();
	            this.exception = new Exception (msg);
	        }

	        break;

	    }

	    // make sure there is only one thread running per instance of this class
	    // if localrtx != rtx, the current thread has been aborted and there's no need to notify
	    if (localrtx != rtx) {
	        localrtx.closeConnections ();
	        return;
	    }

	    notifyAndWait ();

            }  while (localrtx == rtx);

        } finally {
            localrtx.closeConnections ();
        }
    }

    /**
     * Called by the transactor thread when it has successfully fulfilled a request.
     */
    synchronized void commitTransaction () throws Exception {
	Transactor localrtx = (Transactor) Thread.currentThread ();
	if (localrtx == rtx) {
	    reqtype = NONE;
	    localrtx.commit ();
	} else {
	    throw new TimeoutException ();
	}
    }

    synchronized void abortTransaction (boolean retry) {
	Transactor localrtx = (Transactor) Thread.currentThread ();
	if (!retry && localrtx == rtx)
	    reqtype = NONE;
	try {
	    localrtx.abort ();
	} catch (Exception ignore) {}
    }

    /**
     * Tell waiting thread that we're done, then wait for next request
     */
    synchronized void notifyAndWait () {
	Transactor localrtx = (Transactor) Thread.currentThread ();
	if (reqtype != NONE)
	    return; // is there a new request already?
	notifyAll ();
	try {
	    // wait for request, max 30 min
	    wait (1800000l);
	    //  if no request arrived, release ressources and thread
	    if (reqtype == NONE && rtx == localrtx)
	        rtx = null;
	} catch (InterruptedException ir) {}
    }

    public synchronized ResponseTrans invoke (RequestTrans req, User user)  throws Exception {
	this.reqtype = HTTP;
	this.req = req;
	this.user = user;
	this.res = new ResponseTrans ();

	app.activeRequests.put (req, this);

	checkThread ();
	wait (app.requestTimeout);
 	if (reqtype != NONE) {
	    app.logEvent ("Stopping Thread for Request "+app.getName()+"/"+req.path);
	    stopThread ();
	    res.reset ();
	    res.write ("<b>Error in application '"+app.getName()+"':</b> <br><br><pre>Request timed out.</pre>");
	}

	return res;
    }

    /**
     * This checks if the Evaluator is already executing an equal request. If so, attach to it and
     * wait for it to complete. Otherwise return null, so the application knows it has to run the request.
     */
    public synchronized ResponseTrans attachRequest (RequestTrans req) throws InterruptedException {
	if (this.req == null || res == null || !this.req.equals (req))
	    return null;
	// we already know our response object
	ResponseTrans r = res;
	if (reqtype != NONE)
	    wait (app.requestTimeout);
	return r;
    }


    public synchronized Object invokeXmlRpc (String method, Vector args) throws Exception {
	this.reqtype = XMLRPC;
	this.user = null;
	this.method = method;
	this.args = args;
	this.res = new ResponseTrans ();
	result = null;
	exception = null;

	checkThread ();
	wait (app.requestTimeout);
 	if (reqtype != NONE) {
	    stopThread ();
	}

	if (exception != null)
	    throw (exception);
	return result;
    }

    protected Object invokeDirectFunction (Object obj, String functionName, Object[] args) throws Exception {
	ESObject eso = null;
	if (obj == null)
	    eso = global;
	else
	    eso = getElementWrapper (obj);
	ESValue[] esv = args == null ? new ESValue[0] : new ESValue[args.length];
	for (int i=0; i<esv.length; i++)
	    // for java.util.Map objects, we use the special "tight" wrapper
	    // that makes the Map look like a native object
	    if (args[i] instanceof Map)
	        esv[i] = new ESMapWrapper (this, (Map) args[i]);
	    else
	        esv[i] = ESLoader.normalizeValue (args[i], evaluator);
	ESValue retval =  eso.doIndirectCall (evaluator, eso, functionName, esv);
	return retval == null ? null : retval.toJavaObject ();
    }

    public synchronized Object invokeFunction (Object node, String functionName, Object[] args)
		throws Exception {
	ESObject obj = null;
	if  (node == null)
	    obj = global;
	else
	    obj = getElementWrapper (node);
	return invokeFunction (obj, functionName, args);
    }

    public synchronized Object invokeFunction (ESObject obj, String functionName, Object[] args)
		throws Exception {
	this.reqtype = INTERNAL;
	this.user = null;
	this.current = obj;
	this.method = functionName;
	this.esargs = new ESValue[0];
	this.res = new ResponseTrans ();
	esresult = ESNull.theNull;
	exception = null;

	checkThread ();
	wait (60000l*15); // give internal call more time (15 minutes) to complete

 	if (reqtype != NONE) {
	    stopThread ();
	}

	if (exception != null)
	    throw (exception);
	return esresult == null ? null : esresult.toJavaObject ();
    }

    public synchronized Object invokeFunction (User user, String functionName, Object[] args)
		throws Exception {
	this.reqtype = INTERNAL;
	this.user = user;
	this.current = null;
	this.method = functionName;
	this.esargs = new ESValue[0];
	this.res = new ResponseTrans ();
	esresult = ESNull.theNull;
	exception = null;

	checkThread ();
	wait (app.requestTimeout);

 	if (reqtype != NONE) {
	    stopThread ();
	}

	if (exception != null)
	    throw (exception);
	return esresult == null ? null : esresult.toJavaObject ();
    }


    /**
     *  Stop this request evaluator's current thread. If currently active kill the request, otherwise just
     *  notify.
     */
    public synchronized void stopThread () {
	app.logEvent ("Stopping Thread "+rtx);
	Transactor t = rtx;
	evaluator.thread = null;
	rtx = null;
	if (t != null) {
	    if (reqtype != NONE) {
	        reqtype = NONE;
	        t.kill ();
	        try {
	            t.abort ();
	        } catch (Exception ignore) {}
	    } else {
                     notifyAll ();
	    }
	    t.closeConnections ();
	}
    }

    private synchronized void checkThread () throws InterruptedException {

	if (app.stopped)
	    throw new ApplicationStoppedException ();

	if (rtx == null || !rtx.isAlive()) {
	    // app.logEvent ("Starting Thread");
	    rtx = new Transactor (this, app.threadgroup, app.nmgr);
	    evaluator.thread = rtx;
	    rtx.start ();
	} else {
	    notifyAll ();
	}
    }

    public Skin getSkin (ESObject thisObject, String skinname) {
	Prototype proto = null;
	if (thisObject == null)
	    proto = app.typemgr.getPrototype ("global");
	else {
	    Object elem = thisObject.toJavaObject ();
	    proto = app.getPrototype (elem);
	}
	return getSkin (proto, skinname);
    }
	
	
    public Skin getSkin (Prototype proto, String skinname) {
	if (proto == null)
	    return null;
	// First check if the skin has been already used within the execution of this request
	SkinKey key = new SkinKey (proto.getName(), skinname);
	Skin skin = (Skin) skincache.get (key);
	if (skin != null) {
	    return skin;
	}
	// Skin skin = null;
	if (skinmanagers == null)
	    getSkinManagers ();
	do {
	    for (int i=0; i<skinmanagers.length; i++) {
	        skin = getSkinFromNode (skinmanagers[i], proto.getName (), skinname);
	        if (skin != null) {
	            skincache.put (key, skin);
	            return skin;
	        }
	    }
	    // not found in node managers for this prototype.
	    // the next step is to look if it is defined as skin file for this prototype
	    skin = proto.getSkin (skinname);
	    if (skin != null) {
	        skincache.put (key, skin);
	        return skin;
	    }
	    // still not found. See if there is a parent prototype which might define the skin
	    proto = proto.getParentPrototype ();
	} while (proto != null);
	// looked every where, nothing to be found
	return null;
    }

    	
    private Skin getSkinFromNode (INode node, String prototype, String skinname) {
	if (prototype == null)
	    return null;
	INode n = node.getNode (prototype, false);
	if (n != null) {
	    n = n.getNode (skinname, false);
	    if (n != null) {
	        String skin = n.getString ("skin", false);
	        if (skin != null) {
	            Skin s = (Skin) app.skincache.get (skin);
	            if (s == null) {
	                s = new Skin (skin, app);
	                app.skincache.put (skin, s);
	            }
	            return s;
	        }
	    }
	}
	
	// Inheritance is taken care of in the above getSkin method.
	// the sequence is prototype.skin-from-db, prototype.skin-from-file, parent.from-db, parent.from-file etc.
	
	return null;
    }

    /**
     * Get an array of skin managers for a request path so it is retrieved ony once per request
     */
     private void getSkinManagers () {
 	Vector v = new Vector ();
	for (int i=reqPath.size()-1; i>=0; i--) try {
	    ESNode esn = (ESNode) reqPath.getProperty (i);
	    INode n = esn.getNode ();
	    DbMapping dbm = n.getDbMapping ();
	    if (dbm == null)
	        continue;
	    String[] skinmgr = dbm.getSkinManagers();
	    if (skinmgr == null)
	        continue;
	    for (int j=0; j<skinmgr.length; j++) {
	        INode sm = n.getNode (skinmgr[j], false);
	        if (sm != null)
	            v.addElement (sm);
	    }
	} catch (Exception ignore) { }
	skinmanagers = new INode[v.size()];
	v.copyInto (skinmanagers);
    }	


    /**
     *  Returns a node wrapper only if it already exists in the cache table. This is used
     *  in those places when wrappers have to be updated if they already exist.
     */
    public ESNode getNodeWrapperFromCache (INode n) {
	if (n == null)
	    return null;
	return (ESNode) objectcache.get (n);
    }

    /**
     *  Get a Script wrapper for an object. In contrast to getElementWrapper, this is called for
     * any Java object, not just the ones in the request path which we know are scripted.
     * So what we do is check if the object belongs to a scripted class. If so, we call getElementWrapper()
     * with the object, otherwise we return a generic unscripted object wrapper.
     */
    public ESValue getObjectWrapper (Object e) {
	if (app.getPrototypeName (e) != null)
	    return getElementWrapper (e);
	else
	    return new ESWrapper (e, evaluator);
    }

    /**
     *  Get a Script wrapper for any given object. If the object implements the IPathElement
     *  interface, the getPrototype method will be used to retrieve the name of the prototype
     * to use. Otherwise, a Java-Class-to-Script-Prototype mapping is consulted.
     */
    public ESObject getElementWrapper (Object e) {
	
	// check if e is an instance of a helma objectmodel node.
	if (e instanceof INode)
	    return getNodeWrapper ((INode) e);

	// Gotta find out the prototype name to use for this object...
	String prototypeName = app.getPrototypeName (e);

	ObjectPrototype op = getPrototype (prototypeName);

	if (op == null)
	    op = esObjectPrototype;

	return new ESGenericObject (op, evaluator, e);
    }


    /**
     *  Get a script wrapper for an implemntation of helma.objectmodel.INode
     */
    public ESNode getNodeWrapper (INode n) {

        if (n == null)
            return null;

        ESNode esn = (ESNode) objectcache.get (n);

        if (esn == null || esn.getNode() != n) {
            String protoname = n.getPrototype ();
            ObjectPrototype op = null;

            // set the DbMapping of the node according to its prototype.
            // this *should* be done on the objectmodel level, but isn't currently
            // for embedded nodes since there's not enough type info at the objectmodel level
            // for those nodes.
            if (protoname != null && protoname.length() > 0 && n.getDbMapping () == null) {
                n.setDbMapping (app.getDbMapping (protoname));
            }

            op = getPrototype (protoname);

            // no prototype found for this node?
            if (op == null)
                op = esNodePrototype;


            DbMapping dbm = n.getDbMapping ();
            if (dbm != null && dbm.isInstanceOf ("user"))
                esn = new ESUser (n, this, null);
            else
                esn = new ESNode (op, evaluator, n, this);

            objectcache.put (n, esn);
            // app.logEvent ("Wrapper for "+n+" created");
        }

        return esn;
    }

    /**
     *  Get a scripting wrapper object for a user object. Active user objects are represented by
     *  the special ESUser wrapper class.
     */
    public ESNode getNodeWrapper (User u) {
        if (u == null)
            return null;

        ESUser esn = (ESUser) objectcache.get (u);

        if (esn == null) {
            esn = new ESUser (u.getNode(), this, u);
            objectcache.put (u, esn);
        } else {
            // the user node may have changed (login/logout) while the ESUser was
            // lingering in the cache.
            esn.updateNodeFromUser ();
        }

        return esn;
    }

    /**
     *  Get the object prototype for a prototype name
     */
    public ObjectPrototype getPrototype (String protoName) {
        if (protoName == null)
            return null;
        return (ObjectPrototype) prototypes.get (protoName);
    }

    /**
     * Register an object prototype for a certain prototype name.
     */
    public void putPrototype (String protoName, ObjectPrototype op) {
        if (protoName != null && op != null)
            prototypes.put (protoName, op);
    }

    /**
     * Check if an object has a function property (public method if it
     * is a java object) with that name.
     */
    public boolean hasFunction (Object obj, String fname) {
	ESObject eso = null;
	if (obj == null)
	    eso = global;
	else
	    eso = getElementWrapper (obj);
	try {
	    ESValue func = eso.getProperty (fname, fname.hashCode());
	    if (func != null && func instanceof FunctionPrototype)
	        return true;
	} catch (EcmaScriptException esx) {
	    // System.err.println ("Error in getProperty: "+esx);
	    return false;
	}
	return false;
    }


    /**
     * Check if an object has a defined property (public field if it
     * is a java object) with that name.
     */
    public Object getProperty (Object obj, String propname) {
	if (obj == null || propname == null)
	    return null;

	String prototypeName = app.getPrototypeName (obj);
	if ("user".equalsIgnoreCase (prototypeName) &&
		"password".equalsIgnoreCase (propname))
	    return "[macro access to password property not allowed]";

	// if this is a HopObject, check if the property is defined
	// in the type.properties db-mapping.
	if (obj instanceof INode) {
	    DbMapping dbm = app.getDbMapping (prototypeName);
	    if (dbm != null) {
	        Relation rel = dbm.propertyToRelation (propname);
	        if (rel == null || !rel.isPrimitive ())
	            return "[property \""+propname+"\" is not defined for "+prototypeName+"]";
	    }
	}

	ESObject eso = getElementWrapper (obj);
	try {
	    ESValue prop = eso.getProperty (propname, propname.hashCode());
	    if (prop != null && !(prop instanceof ESNull) &&
	                    !(prop instanceof ESUndefined))
	        return prop.toJavaObject ();
	} catch (EcmaScriptException esx) {
	    System.err.println ("Error in getProperty: "+esx);
	    return null;
	}
	return null;
    }

    /**
     *  Utility class to use for caching skins in a Hashtable.
     *  The key consists out of two strings: prototype name and skin name.
     */
    final class SkinKey {

	final String first, second;

	public SkinKey (String first, String second) {
	    this.first = first;
	    this.second = second;
	}

	public boolean equals (Object other) {
	    try {
	        SkinKey key = (SkinKey) other;
	        return first.equals (key.first) && second.equals (key.second);
	    } catch (Exception x) {
	        return false;
	    }
	}
	
	public int hashCode () {
	    return first.hashCode () + second.hashCode ();
	}
    }
}

