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

    // the object on which to invoke a function, if specified
    Object thisObject;

    // the method to be executed
    String method;

    // the user object associated with the current request
    User user;

    // arguments passed to the function
    Object[] args;

    // the object path of the request we're evaluating
    List requestPath;

    // the result of the
    Object result;

    // the exception thrown by the evaluator, if any.
    Exception exception;

    // the type of request to be serviced
    int reqtype;
    static final int NONE = 0;        // no request
    static final int HTTP = 1;           // via HTTP gateway
    static final int XMLRPC = 2;      // via XML-RPC
    static final int INTERNAL = 3;     // generic function call, e.g. by scheduler


    /**
     *  Build a RenderContext from a RequestTrans. Checks if the path is the user home node ("user")
     *  or a subnode of it. Otherwise, the data root of the site is used. Two arrays are built, one
     *  that contains the data nodes, and anotherone with the corresponding Prototypes or Prototype.Parts.
     */
    public RequestEvaluator (Application app) {
	this.app = app;
	initialized = false;
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
	    // if (!initialized)
	    //     app.typemgr.initRequestEvaluator (this);
	    // System.err.println ("Type check overhead: "+(System.currentTimeMillis ()-startCheck)+" millis");

	    // object refs to ressolve request path
	    Object root, currentElement;

	    requestPath = new ArrayList ();

	    switch (reqtype) {
	    case HTTP:
	        int tries = 0;
	        boolean done = false;
	        String error = null;
	        while (!done) {

	            currentElement = null;

	            try {

	                // used for logging
	                String txname = app.getName()+"/"+req.path;
	                // set Timer to get some profiling data
	                localrtx.timer.reset ();
	                localrtx.timer.beginEvent (requestPath+" init");
	                localrtx.begin (txname);

	                String action = null;

	                root = app.getDataRoot ();

	                HashMap globals = new HashMap ();
	                globals.put ("root", root);
	                globals.put ("user", user);
	                globals.put ("req", req);
	                globals.put ("res", res);
	                globals.put ("path", requestPath);
	                globals.put ("app", app.getAppNode());
	                if (error != null)
	                    res.error = error;
	                if (user.message != null) {
	                    // bring over the message from a redirect
	                    res.message = user.message;
	                    user.message = null;
	                }

	                /* // set and mount the request and response data object
	                reqData.setData (req.getRequestData());
	                req.data = reqData;
	                resData.setData (res.getResponseData());
	                res.data = resData; */

	                try {

	                    if (error != null) {
	                        // there was an error in the previous loop, call error handler
	                        currentElement = root;
	                        requestPath.add (currentElement);
	                        String errorAction = app.props.getProperty ("error", "error");
	                        action = getAction (currentElement, errorAction);
	                        if (action == null)
	                            throw new RuntimeException (error);

	                    } else if (req.path == null || "".equals (req.path.trim ())) {
	                        currentElement = root;
	                        requestPath.add (currentElement);
	                        action = getAction (currentElement, null);
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
	                        requestPath.add (currentElement);

	                        for (int i=0; i<ntokens; i++) {

	                            if (currentElement == null)
	                                throw new FrameworkException ("Object not found.");

	                            // the first token in the path needs to be treated seprerately,
	                            // because "/user" is a shortcut to the current user session, while "/users"
	                            // is the mounting point for all users.
	                            if (i == 0 && "user".equalsIgnoreCase (pathItems[i])) {
	                                currentElement = user.getNode ();
	                                if (currentElement != null) {
	                                    requestPath.add (currentElement);
	                                }

	                            } else if (i == 0 && "users".equalsIgnoreCase (pathItems[i])) {
	                                currentElement = app.getUserRoot ();

	                                if (currentElement != null) {
	                                    requestPath.add (currentElement);
	                                }

	                            } else {

	                                // if we're at the last element of the path,
	                                // try to interpret it as action name.
	                                if (i == ntokens-1) {
	                                    action = getAction (currentElement, pathItems[i]);
	                                }

	                                if (action == null) {

	                                    if (pathItems[i].length () == 0)
	                                        continue;

	                                    currentElement = app.getChildElement (currentElement, pathItems[i]);

	                                    // add object to request path if suitable
	                                    if (currentElement != null) {
	                                        // add to requestPath array
	                                        requestPath.add (currentElement);
	                                        String pt = app.getPrototypeName (currentElement);
	                                    }
	                                }
	                            }
	                        }

	                        if (currentElement == null)
	                            throw new FrameworkException ("Object not found.");

	                        if (action == null)
	                            action = getAction (currentElement, null);

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
	                    currentElement = root;
	                    action = getAction (currentElement, notFoundAction);
	                    if (action == null)
	                        throw new FrameworkException (notfound.getMessage ());
	                }

	                localrtx.timer.endEvent (txname+" init");
	                /////////////////////////////////////////////////////////////////////////////
	                // end of path resolution section

	                /////////////////////////////////////////////////////////////////////////////
	                // beginning of execution section
	                try {
	                    localrtx.timer.beginEvent (txname+" execute");

	                    int actionDot = action.lastIndexOf (".");
	                    boolean isAction =  actionDot == -1;
	                    // set the req.action property, cutting off the _action suffix
	                    if (isAction)
	                        req.action = action.substring (0, action.length()-7);
	                    else
	                        req.action = action;

	                    // try calling onRequest() function on object before
	                    // calling the actual action
	                    try {
	                        app.scriptingEngine.invoke (currentElement, "onRequest", new Object[0], globals, this);
	                    } catch (RedirectException redir) {
	                        throw redir;
	                    } catch (Exception ignore) {
	                        // function is not defined or caused an exception, ignore
	                    }
	                    // do the actual action invocation
	                    if (isAction) {
	                        app.scriptingEngine.invoke (currentElement, action, new Object[0], globals, this);
	                    } else {
	                        /* Skin skin = getSkinInternal (app.appDir, app.getPrototype(currentElement).getName(),
						action.substring (0, actionDot), action.substring (actionDot+1));
	                        if (skin != null)
	                            skin.render (this, currentElement, null);
	                        else
	                            throw new RuntimeException ("Skin "+action+" not found in "+req.path); */
	                    }


	                    // check if the script set the name of a skin to render in res.skin
	                    if (res.skin != null) {
	                        int dot = res.skin.indexOf (".");
	                        Object sobj = null;
	                        String sname = res.skin;
	                        if (dot > -1) {
	                            String soname = res.skin.substring (0, dot);
	                            int l = requestPath.size();
	                            for (int i=l-1; i>=0; i--) {
	                                Object pathelem = requestPath.get (i);
	                                if (soname.equalsIgnoreCase (app.getPrototypeName (pathelem))) {
	                                    sobj = pathelem;
	                                    break;
	                                }
	                            }

	                            if (sobj == null)
	                                throw new RuntimeException ("Skin "+res.skin+" not found in path.");
	                            sname = res.skin.substring (dot+1);
	                        }
	                        Skin skin = app.getSkin (sobj, sname, null);
	                        // get the java object wrapped by the script object, if not global
	                        // Object obj = sobj == null ? null : sobj.toJavaObject ();
	                        if (skin != null)
	                            skin.render (this, sobj, null);
	                        else
	                            throw new RuntimeException ("Skin "+res.skin+" not found in path.");
	                    }

	                    localrtx.timer.endEvent (txname+" execute");
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
	                        // set done to false so that the error will be processed
	                        done = false;
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
	                    // set done to false so that the error will be processed
	                    done = false;
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

	            HashMap globals = new HashMap ();
	            globals.put ("root", root);
	            globals.put ("res", res);
	            globals.put ("app", app.getAppNode());

	            // resData.setData (res.getResponseData());
	            // res.data = resData;

	            // convert arguments
	            /*int l = args.size ();
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
	            commitTransaction (); */

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
	        // Just a human readable descriptor of this invocation
	        String funcdesc = app.getName()+":internal/"+method;

	        // avoid going into transaction if called function doesn't exist
	        boolean functionexists = true;
	        /* if (current == null) try {
	            functionexists = global.getProperty (method, method.hashCode()) != ESUndefined.theUndefined;
	        } catch (EcmaScriptException x) {} */

	        if (!functionexists)
	            // global function doesn't exist, nothing to do here.
	            reqtype = NONE;
	        else try {
	            localrtx.begin (funcdesc);

	            root = app.getDataRoot ();

	            HashMap globals = new HashMap ();
	            globals.put ("root", root);
	            globals.put ("res", res);
	            globals.put ("app", app.getAppNode());

	            // resData.setData (res.getResponseData());
	            // res.data = resData;

	            /*if (current == null) {
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
	                esresult = current.doIndirectCall (evaluator, current, method, new ESValue[0]); */
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

	// clear/reset global vars
	/* for (Enumeration en = global.getProperties(); en.hasMoreElements(); ) {
	    Object obj = en.nextElement ();
	    try {
	        global.deleteProperty(obj.toString(), obj.hashCode());
	    } catch (Exception x) {}
	    // System.err.println (">>> "+obj);
	} */

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


    public synchronized Object invokeXmlRpc (String method, Object[] args) throws Exception {
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
/*	ESObject eso = null;
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
	return retval == null ? null : retval.toJavaObject (); */
	return app.scriptingEngine.invoke (obj, functionName, args, null, this);
    }

    public synchronized Object invokeFunction (Object object, String functionName, Object[] args)
		throws Exception {
	reqtype = INTERNAL;
	user = null;
	thisObject = object;
	method = functionName;
	this.args =args;
	this.res = new ResponseTrans ();
	result = null;
	exception = null;

	checkThread ();
	wait (60000l*15); // give internal call more time (15 minutes) to complete

 	if (reqtype != NONE) {
	    stopThread ();
	}

	if (exception != null)
	    throw (exception);
	return result;
    }

    public synchronized Object invokeFunction (User user, String functionName, Object[] args)
		throws Exception {
	reqtype = INTERNAL;
	this.user = user;
	thisObject = null;
	method = functionName;
	this.args = args;
	res = new ResponseTrans ();
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


    /**
     *  Stop this request evaluator's current thread. If currently active kill the request, otherwise just
     *  notify.
     */
    public synchronized void stopThread () {
	app.logEvent ("Stopping Thread "+rtx);
	Transactor t = rtx;
	// evaluator.thread = null;
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
	    // evaluator.thread = rtx;
	    rtx.start ();
	} else {
	    notifyAll ();
	}
    }



    /**
     * Check if an action with a given name is defined for a scripted object. If it is,
     * return the action's function name. Otherwise, return null.
     */
    public String getAction (Object obj, String action) {
	if (obj == null)
	    return null;
	// check if this is a public skin, i.e. something with an extension
	// like "home.html"
	if (action != null && action.indexOf (".") > -1) {
	    int dot = action.lastIndexOf (".");
	    String extension = action.substring (dot+1);
	    String contentType = app.skinExtensions.getProperty (extension);
	    if (contentType != null) {
	        res.contentType = contentType;
	        return action;
	    } else
	        return null;
	} else {
	    String act = action == null ? "main_action" : action+"_action";
	    try {
	        if (app.scriptingEngine.hasFunction (obj, act, this))
	            return act;
	    } catch (ScriptingException x) {
	        return null;
	    }
	}
	return null;
    }



}

