// RequestEvaluator.java
// Copyright (c) Hannes Wallnöfer 1998-2000
 
package helma.framework.core;

import java.util.Vector;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.io.*;
import helma.objectmodel.*;
import helma.objectmodel.db.Transactor;
import helma.framework.*;
import helma.framework.extensions.*;
import helma.xmlrpc.fesi.*;
import helma.util.*;
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


    Application app;
    protected boolean initialized;

    RequestTrans req;
    ResponseTrans res;

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
    private ESRequestData reqData;

    // vars for FESI EcmaScript support
    protected Evaluator evaluator;
    protected ObjectPrototype esNodePrototype;
    protected ObjectPrototype esUserPrototype;
    protected LruHashtable objectcache;
    protected Hashtable prototypes;

    GlobalObject global;
    HopExtension hopx;
    MailExtension mailx;
    FesiRpcServer xmlrpc;
    ESAppNode appnode;
    static String[] extensions = new String[] {
	"FESI.Extensions.BasicIO",
	"FESI.Extensions.FileIO",
	"helma.xmlrpc.fesi.FesiRpcExtension",
	"helma.framework.extensions.ImageExtension",
	"helma.framework.extensions.FtpExtension",
	"helma.framework.extensions.Database",
	"FESI.Extensions.JavaAccess",
	"FESI.Extensions.OptionalRegExp"};

    // the type of request to be serviced
    int reqtype;
    static final int NONE = 0;        // no request
    static final int HTTP = 1;           // via HTTP gateway
    static final int XMLRPC = 2;      // via XML-RPC
    static final int INTERNAL = 3;     // generic function call, e.g. by scheduler

    INode root, userroot, currentNode, skinManager;

    /**
     *  Build a RenderContext from a RequestTrans. Checks if the path is the user home node ("user")
     *  or a subnode of it. Otherwise, the data root of the site is used. Two arrays are built, one
     *  that contains the data nodes, and anotherone with the corresponding Prototypes or Prototype.Parts.
     */
    public RequestEvaluator (Application app) {
    	this.app = app;
	this.objectcache = new LruHashtable (100, .80f);
	this.prototypes = new Hashtable ();
	initEvaluator ();
	initialized = false;
	// startThread ();
    }


    // init Script Evaluator
    private void initEvaluator () {
	try {
	    evaluator = new Evaluator();
	    global = evaluator.getGlobalObject();
	    for (int i=0; i<extensions.length; i++)
	        evaluator.addExtension (extensions[i]);
	    hopx = new HopExtension ();
	    hopx.initializeExtension (this);
	    mailx = (MailExtension) evaluator.addExtension ("helma.framework.extensions.MailExtension");
	    mailx.setProperties (this.app.props);

	    // fake a cache member like the one found in ESNodes
	    global.putHiddenProperty ("cache", new ESNode (new Node ("cache"), this));
	    global.putHiddenProperty ("undefined", ESUndefined.theUndefined);
	    appnode = new ESAppNode (app.appnode, this);
	    global.putHiddenProperty ("app", appnode);
	    reqPath = new ArrayPrototype (evaluator.getArrayPrototype(), evaluator);
	    reqData = new ESRequestData (this);

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

        // evaluators are only initialized as needed, so we need to check that here
        if (!initialized)
	app.typemgr.initRequestEvaluator (this);

        try {
	do {

	    // app.logEvent ("got request "+reqtype);
	    // reset skinManager
	    skinManager = null;

	    switch (reqtype) {
	    case HTTP:
	        int tries = 0;
	        boolean done = false;
	        String error = null;
	        while (!done) {

	            current = null;
	            currentNode = null;
	            reqPath.setSize (0);

	            try {

	                String requestPath = app.getName()+"/"+req.path;
	                // set Timer to get some profiling data
	                localrtx.timer.reset ();
	                localrtx.timer.beginEvent (requestPath+" init");
	                localrtx.begin (requestPath);

	                Action action = null;

	                root = app.getDataRoot ();

	                ESUser esu = (ESUser) getNodeWrapper (user.getNode ());
	                esu.setUser (user);
	                global.putHiddenProperty ("root", getNodeWrapper (root));
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
	                // set and mount the request data object
	                reqData.setData (req.getReqData());
	                req.data = reqData;

	                try {

	                    if (error != null) {
	                        // there was an error in the previous loop, call error handler
	                        currentNode = root;
	                        current = getNodeWrapper (root);
	                        reqPath.putProperty (0, current);
	                        Prototype p = app.getPrototype (root);
	                        String errorAction = app.props.getProperty ("error", "error");
	                        action = p.getActionOrTemplate (errorAction);
	
	                    } else if (req.path == null || "".equals (req.path.trim ())) {
	                        currentNode = root;
	                        current = getNodeWrapper (root);
	                        reqPath.putProperty (0, current);
	                        Prototype p = app.getPrototype (root);
	                        action = p.getActionOrTemplate (null);

	                    } else {

	                        // march down request path...
	                        // is the next path element a subnode or a property of the last one?
	                        // currently only used for users node
	                        boolean isProperty = false;
	                        StringTokenizer st = new StringTokenizer (req.path, "/");
	                        int ntokens = st.countTokens ();
	                        String next = null;
	                        currentNode = root;
	                        reqPath.putProperty (0, getNodeWrapper (currentNode));

	                        // the first token in the path needs to be treated seprerately,
	                        // because "/user" is a shortcut to the current user session, while "/users"
	                        // is the mounting point for all users.
	                        if (ntokens > 1) {
	                            next = st.nextToken ();
	                            if ("user".equalsIgnoreCase (next)) {
	                                currentNode = user.getNode ();
	                                ntokens -=1;
	                                if (currentNode != null)
	                                    reqPath.putProperty (1, getNodeWrapper (currentNode));
	                            } else if ("users".equalsIgnoreCase (next)) {
	                                currentNode = app.getUserRoot ();
	                                ntokens -=1;
	                                isProperty = true;
	                            } else {
	                                currentNode = currentNode.getSubnode (next);
	                                ntokens -=1;
	                                if (currentNode != null)
	                                    reqPath.putProperty (1, getNodeWrapper (currentNode));
	                            }
	                        }
	
	                        for (int i=1; i<ntokens; i++) {
	                            if (currentNode == null)
	                                throw new FrameworkException ("Object not found.");
	                            next = st.nextToken ();
	                            if (isProperty)  // get next element as property
	                                currentNode = currentNode.getNode (next, false);
	                            else  // get next element as subnode
	                                currentNode = currentNode.getSubnode (next);
	                            isProperty = false;
	                            if (currentNode != null && currentNode.getState() != INode.VIRTUAL) // add to reqPath array
	                                reqPath.putProperty (reqPath.size(), getNodeWrapper (currentNode));
	                            // limit path to < 50 tokens
	                            if (i > 50) throw new RuntimeException ("Path too deep");
	                        }

	                        if (currentNode == null)
	                            throw new FrameworkException ("Object not found.");

	                        Prototype p = app.getPrototype (currentNode);
                                     next = st.nextToken ();
	                        if (p != null)
	                            action = p.getActionOrTemplate (next);

	                        if (p == null || action == null) {
	                            currentNode = currentNode.getSubnode (next);
	                            if (currentNode == null)
	                                throw new FrameworkException ("Object or Action '"+next+"' not found.");
	                            p = app.getPrototype (currentNode);
	                            // add to reqPath array
	                            if (currentNode != null && currentNode.getState() != INode.VIRTUAL)
	                                reqPath.putProperty (reqPath.size(), getNodeWrapper (currentNode));
	                            if (p != null)
	                                action = p.getActionOrTemplate ("main");
	                        }

	                        current = getNodeWrapper (currentNode);

	                    }

	                    if (action == null)
	                        throw new FrameworkException ("Action not found");

	                } catch (FrameworkException notfound) {
	                    if (error != null)
	                        // we already have an error and the error template wasn't found,
	                        // display it instead of notfound message
	                        throw new RuntimeException ();
	                    // The path could not be resolved. Check if there is a "not found" action
	                    // specified in the property file.
	                    String notFoundAction = app.props.getProperty ("notFound", "notfound");
	                    Prototype p = app.getPrototype (root);
	                    action = p.getActionOrTemplate (notFoundAction);
	                    if (action == null)
	                        throw new FrameworkException (notfound.getMessage ());
	                    current = getNodeWrapper (root);
	                }

	                localrtx.timer.endEvent (requestPath+" init");

	                try {
	                    localrtx.timer.beginEvent (requestPath+" execute");
	                    current.doIndirectCall (evaluator, current, action.getFunctionName (), new ESValue[0]);
	                    if (res.skin != null) {
	                        Skin skin = getSkin (null, res.skin);
	                        if (skin != null)
	                            skin.render (this, null, null);
	                    }
	                    localrtx.timer.endEvent (requestPath+" execute");
	                } catch (RedirectException redirect) {
	                    res.redirect = redirect.getMessage ();
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
	                if (++tries < 5) {
	                    // try again after waiting some period
	                    abortTransaction (true);
	                    try {
	                        // wait a bit longer with each try
	                        int base = 500 * tries;
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

	            global.putHiddenProperty ("root", getNodeWrapper (root));
	            global.deleteProperty("user", "user".hashCode());
	            global.deleteProperty ("req", "req".hashCode());
	            global.putHiddenProperty ("res", ESLoader.normalizeValue(new ResponseTrans (), evaluator));
	            global.deleteProperty ("path", "path".hashCode());
	            global.putHiddenProperty ("app", appnode);

	            // convert arguments
	            int l = args.size ();
	            current = getNodeWrapper (root);
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
	        try {
	            localrtx.begin (funcdesc);

	            root = app.getDataRoot ();

	            global.putHiddenProperty ("root", getNodeWrapper (root));
	            global.deleteProperty("user", "user".hashCode());
	            global.deleteProperty ("req", "req".hashCode());
	            global.putHiddenProperty ("res", ESLoader.normalizeValue(new ResponseTrans (), evaluator));
	            global.deleteProperty ("path", "path".hashCode());
	            global.putHiddenProperty ("app", appnode);

                         if (current == null) {
	                if (user == null) {
	                    current = global;
	                } else {
	                    ESUser esu = (ESUser) getNodeWrapper (user.getNode ());
	                    esu.setUser (user);
	                    current = esu;
	                }
	            }
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

    public synchronized ESValue invokeFunction (INode node, String functionName, ESValue[] args)
		throws Exception {
	ESObject obj = null;
	if  (node == null)
	    obj = global;
	else
	    obj = getNodeWrapper (node);
	return invokeFunction (obj, functionName, args);
    }

    public synchronized ESValue invokeFunction (ESObject obj, String functionName, ESValue[] args)
		throws Exception {
	this.reqtype = INTERNAL;
	this.user = null;
	this.current = obj;
	this.method = functionName;
	this.esargs = args;
             esresult = ESNull.theNull;
	exception = null;

	checkThread ();
	wait (60000l*15); // give internal call more time (15 minutes) to complete

 	if (reqtype != NONE) {
	    stopThread ();
	}

	if (exception != null)
	    throw (exception);
	return esresult;
    }

    public synchronized ESValue invokeFunction (User user, String functionName, ESValue[] args)
		throws Exception {
	this.reqtype = INTERNAL;
	this.user = user;
	this.current = null;
	this.method = functionName;
	this.esargs = args;
             esresult = ESNull.theNull;
	exception = null;

	checkThread ();
	wait (app.requestTimeout);

 	if (reqtype != NONE) {
	    stopThread ();
	}

	if (exception != null)
	    throw (exception);
	return esresult;
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
	INode n = null;
	Prototype proto = null;
	Skin skin = null;
	// FIXME: we can't do that, because if no db skinmanager exists we'll query over and over again
	if (skinManager == null) {
	    skinManager = currentNode.getNode ("skinmanager", true);
	    // System.err.println ("SKINMGR: "+skinManager);
	    // mark as null
	    if (skinManager == null)
	        skinManager = new Node ("dummy");
	}
	if (thisObject != null && thisObject instanceof ESNode) {
	    n = ((ESNode) thisObject).getNode ();
	    // System.err.println ("GOT SKINMGR "+skinNode+" FROM "+n);
	    if (skinManager != null && !"dummy".equals (skinManager.getName())) {
	        skin = getSkinFromNode (skinManager, n.getPrototype (), skinname);
	        if (skin != null)
	            return skin;
	    }
	    proto = app.getPrototype (n);
	} else {
	    // the requested skin is global - start from currentNode (=end of request path) for app skin retrieval
	    if (skinManager != null) {
	        skin = getSkinFromNode (skinManager, "global", skinname);
	        if (skin != null)
	            return skin;
	    }
	    proto = app.typemgr.getPrototype ("global");
	}
	if (proto != null)
	    skin = proto.getSkin (skinname);
	// if we have a thisObject and didn't find the skin, try in hopobject
	if (skin == null && n != null) {
	    proto = app.typemgr.getPrototype ("hopobject");
	    if (proto != null)
	        skin = proto.getSkin (skinname);
	}
	return skin;
    }
	
    private Skin getSkinFromNode (INode node, String prototype, String skinname) {
	INode n = node.getNode (prototype, false);
	if (n != null) {
	    n = n.getNode (skinname, false);
	    if (n != null) {
	        String skin = n.getString ("skin", false);
	        if (skin != null)
	            return new Skin (skin, app);
	    }
	}
	// if this is not for the global prototype, also check hopobject
	if (!"global".equalsIgnoreCase (prototype) && !"hopobject".equalsIgnoreCase (prototype))
	    return getSkinFromNode (node, "hopobject", skinname);
	return null;
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

    public ESNode getNodeWrapper (INode n) {

        if (n == null)
            return null;

        ESNode esn = (ESNode) objectcache.get (n);

        if (esn == null || esn.getNode() != n) {
            ObjectPrototype op = null;
            String protoname = n.getPrototype ();

            // set the DbMapping of the node according to its prototype.
            // this *should* be done on the objectmodel level, but isn't currently
            // for embedded nodes since there's not enough type info at the objectmodel level
            // for those nodes.
            if (protoname != null && protoname.length() > 0 && n.getDbMapping () == null) {
                n.setDbMapping (app.getDbMapping (protoname));
            }

            try {
                op = (ObjectPrototype) prototypes.get (protoname);
            } catch (Exception ignore) {}

            if (op == null)
                op = esNodePrototype; // no prototype found for this node.

            if ("user".equalsIgnoreCase (protoname))
                esn = new ESUser (n, this);
            else
                esn = new ESNode (op, evaluator, n, this);

            objectcache.put (n, esn);
            // app.logEvent ("Wrapper for "+n+" created");
        }

        return esn;
    }

    public ObjectPrototype getPrototype (String protoName) {
        return (ObjectPrototype) prototypes.get (protoName);
    }

    public void putPrototype (String protoName, ObjectPrototype op) {
        prototypes.put (protoName, op);
    }


}

























































































































































































