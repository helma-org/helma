// RequestEvaluator.java
// Copyright (c) Hannes Walln�fer 1998-2000
 
package helma.framework.core;

import java.util.*;
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

    INode root, userroot, currentNode;

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
	startThread ();
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
	    reqData = new ESRequestData (evaluator.getObjectPrototype(), evaluator, this);

	} catch (Exception e) {
	    System.err.println("Cannot initialize interpreter");
	    System.err.println("Error: " + e);
	    e.printStackTrace ();
	    throw new RuntimeException (e.getMessage ());
	}
    }


    private void startThread () {
	rtx = new Transactor (this, app.nmgr);
	evaluator.thread = rtx;
	rtx.start ();
	// yield to make sure the transactor thread reaches waiting status before the first
	// invocation request comes in
	Thread.yield ();
    }

    public void run () {

        int txcount = 0;

        while (evaluator.thread == Thread.currentThread () && evaluator.thread == rtx) {

	synchronized (this) {
	    notifyAll ();
	    try {
	        wait ();
	    } catch (InterruptedException ir) {
	        Thread.currentThread().interrupt();
	    }
	}

	if (Thread.currentThread () != rtx)
	    return;

	// IServer.getLogger().log ("got request "+reqtype);

	switch (reqtype) {
	case HTTP:
	int tries = 0;
	boolean done = false;
	while (!done) {

	    current = null;
	    currentNode = null;
	    reqPath.setSize (0);

	    try {

	        String requestPath = app.getName()+"/"+req.path;
	        // set Timer to get some profiling data
	        rtx.timer.reset ();
	        rtx.timer.beginEvent (requestPath+" init");
	        rtx.begin (requestPath);

	        if (app.debug) {
	            IServer.getLogger().log ("request transactor = "+this);
	            IServer.getLogger().log ("user = "+user);
	        }

	        Action action = null;

	        root = app.getDataRoot ();

	        ESUser esu = (ESUser) getNodeWrapper (user.getNode ());
	        esu.setUser (user);
	        global.putHiddenProperty ("root", getNodeWrapper (root));
	        global.putHiddenProperty("user", esu);
	        global.putHiddenProperty ("req", new ESWrapper (req, evaluator));
	        global.putHiddenProperty ("res", new ESWrapper (res, evaluator));
	        global.putHiddenProperty ("path", reqPath);
	        global.putHiddenProperty ("app", appnode);
	        // set and mount the request data object
	        reqData.setData (req.getReqData());
	        req.data = reqData;

	        if (app.debug) {
	            IServer.getLogger().log ("root = "+root);
	            IServer.getLogger().log ("usernode = "+esu);
	        }

	        try {

	            if (req.path == null || "".equals (req.path.trim ())) {
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
	            // The path could not be resolved. Check if there is a "not found" action
	            // specified in the property file.
	            String notFoundAction = app.props.getProperty ("notFound", "notfound");
	            Prototype p = app.getPrototype (root);
	            action = p.getActionOrTemplate (notFoundAction);
	            if (action == null)
	                throw new FrameworkException (notfound.getMessage ());
	            current = getNodeWrapper (root);
	        }

	        rtx.timer.endEvent (requestPath+" init");

	        try {
	            rtx.timer.beginEvent (requestPath+" execute");
	            current.doIndirectCall (evaluator, current, action.getFunctionName (), new ESValue[0]);
	            rtx.timer.endEvent (requestPath+" execute");
	            done = true;
	        } catch (RedirectException redirect) {
	            res.redirect = redirect.getMessage ();
	            done = true;
	        }

	        rtx.commit ();
	        done = true;
	        // if (app.debug)
	        //     ((Transactor) Thread.currentThread ()).timer.dump (System.err);

	    } catch (ConcurrencyException x) {
	        try { rtx.abort (); } catch (Exception ignore) {}
	        res.reset ();
	        if (++tries < 8) {
	            try {
	                // wait a bit longer with each try
	                int base = 500 * tries;
	                Thread.currentThread ().sleep ((long) (base + Math.random ()*base*2));
	            } catch (Exception ignore) {}
	            continue;
	        }
	        app.errorCount += 1;
	        res.write ("<b>Error in application '"+app.getName()+"':</b> <br><br><pre>Couldn't complete transaction due to heavy object traffic (tried "+tries+" times).</pre>");
	        done = true;

	    } catch (FrameworkException x) {
	        try { rtx.abort (); } catch (Exception ignore) {}
	        app.errorCount += 1;
	        res.reset ();
	        res.write ("<b>Error in application '"+app.getName()+"':</b> <br><br><pre>" + x.getMessage () + "</pre>");
	        if (app.debug)
	            x.printStackTrace ();

	        done = true;
	    } catch (Exception x) {
	        try { rtx.abort (); } catch (Exception ignore) {}
	        app.errorCount += 1;
	        System.err.println ("### Exception in "+app.getName()+"/"+req.path+": current = "+currentNode);
	        System.err.println (x);
	        // Dump the profiling data to System.err
	        ((Transactor) Thread.currentThread ()).timer.dump (System.err);
	        if (app.debug)
	            x.printStackTrace ();

	        // If the transactor thread has been killed by the invoker thread we don't have to
	        // bother for the error message, just quit.
	        if (rtx != Thread.currentThread())
	            return;

	        res.reset ();
	        res.write ("<b>Error in application '"+app.getName()+"':</b> <br><br><pre>" + x.getMessage () + "</pre>");
	        done = true;
	    }
	}
	break;
	case XMLRPC:
	    try {
	        rtx.begin (app.getName()+":xmlrpc/"+method);

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
	        rtx.commit ();
	    } catch (Exception wrong) {
	        try { rtx.abort (); } catch (Exception ignore) {}

	        // If the transactor thread has been killed by the invoker thread we don't have to
	        // bother for the error message, just quit.
	        if (evaluator.thread != Thread.currentThread())
	            return;

	        this.exception = wrong;
	    }

	    break;
	case INTERNAL:
	    esresult = ESNull.theNull;
	    // Just a human readable descriptor of this invocation
	    String funcdesc = app.getName()+":internal/"+method;
	    try {
	        rtx.begin (funcdesc);

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
	        rtx.commit ();
	    } catch (Throwable wrong) {
	        try { rtx.abort (); } catch (Exception ignore) {}

	        // If the transactor thread has been killed by the invoker thread we don't have to
	        // bother for the error message, just quit.
	        if (evaluator.thread != Thread.currentThread())
	            return;

	        String msg = wrong.getMessage ();
	        if (msg == null || msg.length () == 0)
	            msg = wrong.toString ();
	        IServer.getLogger().log ("Error executing "+funcdesc+": "+msg);
	        this.exception = new Exception (msg);
	    }
	    break;

	}
	reqtype = NONE;

	// create a new Thread every 1000 requests. The current one will fade out
	if (txcount++ > 1000) {
	    rtx.cleanup (); // close sql connections held by this thread
	    startThread ();
	}
        }

        // IServer.getLogger().log (this+ " exiting.");
    }

    public synchronized ResponseTrans invoke (RequestTrans req, User user)  throws Exception {
	checkThread ();
	this.reqtype = HTTP;
	this.req = req;
	this.user = user;
	this.res = new ResponseTrans ();

	notifyAll ();
	wait (app.requestTimeout);
 	if (reqtype > 0) {
	    IServer.getLogger().log ("Stopping Thread for Request "+app.getName()+"/"+req.path);
	    evaluator.thread = null;
	    rtx.kill ();
	    res.reset ();
	    res.write ("<b>Error in application '"+app.getName()+"':</b> <br><br><pre>Request timed out.</pre>");
	    rtx = new Transactor (this, app.nmgr);
	    evaluator.thread = rtx;
	    rtx.start ();
	    Thread.yield ();
	}

	return res;
    }


    public synchronized Object invokeXmlRpc (String method, Vector args) throws Exception {
	checkThread ();
	this.reqtype = XMLRPC;
	this.user = null;
	this.method = method;
	this.args = args;
	result = null;
	exception = null;

	notifyAll ();
	wait (app.requestTimeout);
 	if (reqtype > 0) {
	    IServer.getLogger().log ("Stopping Thread");
	    evaluator.thread = null;
	    rtx.kill ();
	    rtx = new Transactor (this, app.nmgr);
	    evaluator.thread = rtx;
	    rtx.start ();
	    Thread.yield ();
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
	checkThread ();
	this.reqtype = INTERNAL;
	this.user = null;
	this.current = obj;
	this.method = functionName;
	this.esargs = args;
             esresult = ESNull.theNull;
	exception = null;

	notifyAll ();
	wait (60000l*15); // give internal call more time (15 minutes) to complete

 	if (reqtype > 0) {
	    IServer.getLogger().log ("Stopping Thread");
	    evaluator.thread = null;
	    rtx.kill ();
	    rtx = new Transactor (this, app.nmgr);
	    evaluator.thread = rtx;
	    rtx.start ();
	    Thread.yield ();
	}

	if (exception != null)
	    throw (exception);
	return esresult;
    }

    public synchronized ESValue invokeFunction (User user, String functionName, ESValue[] args)
		throws Exception {
	checkThread ();
	this.reqtype = INTERNAL;
	this.user = user;
	this.current = null;
	this.method = functionName;
	this.esargs = args;
             esresult = ESNull.theNull;
	exception = null;

	notifyAll ();
	wait (app.requestTimeout);

 	if (reqtype > 0) {
	    IServer.getLogger().log ("Stopping Thread");
	    evaluator.thread = null;
	    rtx.kill ();
	    rtx = new Transactor (this, app.nmgr);
	    evaluator.thread = rtx;
	    rtx.start ();
	    Thread.yield ();
	}

	if (exception != null)
	    throw (exception);
	return esresult;
    }


    /**
     *  Stop this request evaluator. If currently active kill the request, otherwise just
     *  notify.
     */
    public synchronized void stop () {
	evaluator.thread = null;
 	if (reqtype != NONE) {
	    rtx.kill ();
	} else {
                 notifyAll ();
	}
    }

    private void checkThread () {
	if (rtx == null || !rtx.isAlive()) {
	    rtx = new Transactor (this, app.nmgr);
	    evaluator.thread = rtx;
	    rtx.start ();
	    Thread.yield ();
	}
    }
	

    /**
     *  Returns a node wrapper only if it already exists in the cache table. This is used
     *  in those places when wrappers have to be updated if they already exist.
     */
    public ESNode getNodeWrapperFromCache (INode n) {
        return n == null ? null : (ESNode) objectcache.get (n);
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
            // IServer.getLogger().log ("Wrapper for "+n+" created");
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

























































































































































































