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

import helma.framework.*;
import helma.objectmodel.*;
import helma.objectmodel.db.*;
import helma.scripting.*;
import java.lang.reflect.*;
import java.util.*;

/**
 * This class does the work for incoming requests. It holds a transactor thread
 * and an EcmaScript evaluator to get the work done. Incoming threads are
 * blocked until the request has been serviced by the evaluator, or the timeout
 * specified by the application has passed. In the latter case, the evaluator thread
 * is killed and an error message is returned.
 */
public final class RequestEvaluator implements Runnable {
    static final int NONE = 0; // no request
    static final int HTTP = 1; // via HTTP gateway
    static final int XMLRPC = 2; // via XML-RPC
    static final int INTERNAL = 3; // generic function call, e.g. by scheduler
    static final int EXTERNAL = 4; // function from script etc
    public final Application app;
    protected ScriptingEngine scriptingEngine;
    public RequestTrans req;
    public ResponseTrans res;
    volatile Transactor rtx;

    // the object on which to invoke a function, if specified
    Object thisObject;

    // the method to be executed
    String method;

    // the session object associated with the current request
    Session session;

    // arguments passed to the function
    Object[] args;

    // the object path of the request we're evaluating
    RequestPath requestPath;

    // the result of the operation
    Object result;

    // the exception thrown by the evaluator, if any.
    Exception exception;

    // the type of request to be serviced
    int reqtype;
    protected int skinDepth;

    /**
     *  Create a new RequestEvaluator for this application.
     */
    public RequestEvaluator(Application app) {
        this.app = app;
    }

    protected void initScriptingEngine() {
        if (scriptingEngine == null) {
            String engineClassName = app.getProperty("scriptingEngine",
                                                     "helma.scripting.rhino.RhinoEngine");

            try {
                Class clazz = app.getClassLoader().loadClass(engineClassName);

                scriptingEngine = (ScriptingEngine) clazz.newInstance();
                scriptingEngine.init(app, this);
            } catch (Exception x) {
                Throwable t = x;

                if (x instanceof InvocationTargetException) {
                    t = ((InvocationTargetException) x).getTargetException();
                }

                app.logEvent("******************************************");
                app.logEvent("*** Error creating scripting engine: ");
                app.logEvent("*** " + t.toString());
                app.logEvent("******************************************");
            }
        }
    }

    /**
     *
     */
    public void run() {
        // first, set a local variable to the current transactor thread so we know
        // when it's time to quit because another thread took over.
        Transactor localrtx = (Transactor) Thread.currentThread();

        try {
            do {
                // initialize scripting engine, in case it hasn't been initialized yet
                initScriptingEngine();
                // update scripting prototypes
                scriptingEngine.updatePrototypes();

                // System.err.println ("Type check overhead: "+(System.currentTimeMillis ()-startCheck)+" millis");
                // object refs to ressolve request path
                Object root;

                // System.err.println ("Type check overhead: "+(System.currentTimeMillis ()-startCheck)+" millis");
                // object refs to ressolve request path
                Object currentElement;

                requestPath = new RequestPath(app);

                switch (reqtype) {
                    case HTTP:

                        int tries = 0;
                        boolean done = false;
                        String error = null;

                        while (!done) {
                            currentElement = null;

                            try {
                                // used for logging
                                StringBuffer txname = new StringBuffer(app.getName());

                                txname.append("/");
                                txname.append((error == null) ? req.path : "error");

                                // begin transaction
                                localrtx.begin(txname.toString());

                                String action = null;

                                root = app.getDataRoot();

                                HashMap globals = new HashMap();

                                globals.put("root", root);
                                globals.put("session", new SessionBean(session));
                                globals.put("req", new RequestBean(req));
                                globals.put("res", new ResponseBean(res));
                                globals.put("app", new ApplicationBean(app));
                                globals.put("path", requestPath);
                                req.startTime = System.currentTimeMillis();

                                // enter execution context
                                scriptingEngine.enterContext(globals);

                                if (error != null) {
                                    res.error = error;
                                }

                                if (session.message != null) {
                                    // bring over the message from a redirect
                                    res.message = session.message;
                                    session.message = null;
                                }

                                try {
                                    if (error != null) {
                                        // there was an error in the previous loop, call error handler
                                        currentElement = root;

                                        // do not reset the requestPath so error handler can use the original one
                                        // get error handler action
                                        String errorAction = app.props.getProperty("error",
                                                                                   "error");

                                        action = getAction(currentElement, errorAction);

                                        if (action == null) {
                                            throw new RuntimeException(error);
                                        }
                                    } else if ((req.path == null) ||
                                                   "".equals(req.path.trim())) {
                                        currentElement = root;
                                        requestPath.add(null, currentElement);

                                        action = getAction(currentElement, null);

                                        if (action == null) {
                                            throw new FrameworkException("Action not found");
                                        }
                                    } else {
                                        // march down request path...
                                        StringTokenizer st = new StringTokenizer(req.path,
                                                                                 "/");
                                        int ntokens = st.countTokens();

                                        // limit path to < 50 tokens
                                        if (ntokens > 50) {
                                            throw new RuntimeException("Path too long");
                                        }

                                        String[] pathItems = new String[ntokens];

                                        for (int i = 0; i < ntokens; i++)
                                            pathItems[i] = st.nextToken();

                                        currentElement = root;
                                        requestPath.add(null, currentElement);


                                        for (int i = 0; i < ntokens; i++) {
                                            if (currentElement == null) {
                                                throw new FrameworkException("Object not found.");
                                            }

                                            if (pathItems[i].length() == 0) {
                                                continue;
                                            }

                                            // if we're at the last element of the path,
                                            // try to interpret it as action name.
                                            if (i == (ntokens - 1)) {
                                                action = getAction(currentElement,
                                                                   pathItems[i]);
                                            }

                                            if (action == null) {
                                                currentElement = getChildElement(currentElement,
                                                                                 pathItems[i]);

                                                // add object to request path if suitable
                                                if (currentElement != null) {
                                                    // add to requestPath array
                                                    requestPath.add(pathItems[i], currentElement);
                                                }
                                            }
                                        }

                                        if (currentElement == null) {
                                            throw new FrameworkException("Object not found.");
                                        }

                                        if (action == null) {
                                            action = getAction(currentElement, null);
                                        }

                                        if (action == null) {
                                            throw new FrameworkException("Action not found");
                                        }
                                    }
                                } catch (FrameworkException notfound) {
                                    if (error != null) {

                                        // we already have an error and the error template wasn't found,
                                        // display it instead of notfound message
                                        throw new RuntimeException();
                                    }

                                    // The path could not be resolved. Check if there is a "not found" action
                                    // specified in the property file.
                                    res.status = 404;

                                    String notFoundAction = app.props.getProperty("notfound",
                                                                                  "notfound");

                                    currentElement = root;
                                    action = getAction(currentElement, notFoundAction);

                                    if (action == null) {
                                        throw new FrameworkException(notfound.getMessage());
                                    }
                                }

                                // register path objects with their prototype names in
                                // res.handlers
                                Map macroHandlers = res.getMacroHandlers();
                                int l = requestPath.size();
                                Prototype[] protos = new Prototype[l];

                                for (int i=0; i<l; i++) {

                                    Object obj = requestPath.get(i);

                                    protos[i] = app.getPrototype(obj);

                                    // immediately register objects with their direct prototype name
                                    if (protos[i] != null) {
                                        macroHandlers.put(protos[i].getName(), obj);
                                        macroHandlers.put(protos[i].getLowerCaseName(), obj);
                                    }
                                }

                                // in a second pass, we register path objects with their indirect
                                // (i.e. parent prototype) names, starting at the end and only
                                // if the name isn't occupied yet.
                                for (int i=l-1; i>=0; i--) {
                                    if (protos[i] != null) {
                                        protos[i].registerParents(macroHandlers, requestPath.get(i));
                                    }
                                }

                                /////////////////////////////////////////////////////////////////////////////
                                // end of path resolution section
                                /////////////////////////////////////////////////////////////////////////////
                                // beginning of execution section
                                try {
                                    // set the req.action property, cutting off the _action suffix
                                    req.action = action.substring(0, action.length() - 7);

                                    // set the application checksum in response to make ETag
                                    // generation sensitive to changes in the app
                                    res.setApplicationChecksum(app.getChecksum());

                                    // reset skin recursion detection counter
                                    skinDepth = 0;

                                    // try calling onRequest() function on object before
                                    // calling the actual action
                                    try {
                                        if (scriptingEngine.hasFunction(currentElement,
                                                                            "onRequest")) {
                                            scriptingEngine.invoke(currentElement,
                                                                   "onRequest",
                                                                   new Object[0], false);
                                        }
                                    } catch (RedirectException redir) {
                                        throw redir;
                                    }

                                    // reset skin recursion detection counter
                                    skinDepth = 0;

                                    // do the actual action invocation
                                    scriptingEngine.invoke(currentElement, action,
                                                           new Object[0], false);
                                } catch (RedirectException redirect) {
                                    // res.redirect = redirect.getMessage ();
                                    // if there is a message set, save it on the user object for the next request
                                    if (res.message != null) {
                                        session.message = res.message;
                                    }

                                    done = true;
                                }

                                // check if we're still the one and only or if the waiting thread has given up on us already
                                commitTransaction();
                                done = true;
                            } catch (ConcurrencyException x) {
                                res.reset();

                                if (++tries < 8) {
                                    // try again after waiting some period
                                    abortTransaction(true);

                                    try {
                                        // wait a bit longer with each try
                                        int base = 800 * tries;

                                        Thread.sleep((long) (base + (Math.random() * base * 2)));
                                    } catch (Exception ignore) {
                                    }

                                    continue;
                                } else {
                                    abortTransaction(false);

                                    if (error == null) {
                                        app.errorCount += 1;

                                        // set done to false so that the error will be processed
                                        done = false;
                                        error = "Couldn't complete transaction due to heavy object traffic (tried " +
                                                tries + " times)";
                                    } else {
                                        // error in error action. use traditional minimal error message
                                        res.write("<b>Error in application '" +
                                                  app.getName() + "':</b> <br><br><pre>" +
                                                  error + "</pre>");
                                        done = true;
                                    }
                                }
                            } catch (Exception x) {
                                abortTransaction(false);

                                // If the transactor thread has been killed by the invoker thread we don't have to
                                // bother for the error message, just quit.
                                if (localrtx != rtx) {
                                    break;
                                }

                                res.reset();

                                // check if we tried to process the error already
                                if (error == null) {
                                    app.errorCount += 1;
                                    app.logEvent("Exception in " +
                                                 Thread.currentThread() + ": " + x);

                                    // Dump the profiling data to System.err
                                    if (app.debug && !(x instanceof ScriptingException)) {
                                        x.printStackTrace ();
                                    }

                                    // set done to false so that the error will be processed
                                    done = false;
                                    error = x.getMessage();

                                    if ((error == null) || (error.length() == 0)) {
                                        error = x.toString();
                                    }

                                    if (error == null) {
                                        error = "Unspecified error";
                                    }
                                } else {
                                    // error in error action. use traditional minimal error message
                                    res.write("<b>Error in application '" +
                                              app.getName() + "':</b> <br><br><pre>" +
                                              error + "</pre>");
                                    done = true;
                                }
                            }
                        }

                        break;

                    case XMLRPC:
                    case EXTERNAL:

                        try {
                            localrtx.begin(app.getName() + ":ext-rpc:" + method);

                            root = app.getDataRoot();

                            HashMap globals = new HashMap();

                            globals.put("root", root);
                            globals.put("res", new ResponseBean(res));
                            globals.put("app", new ApplicationBean(app));

                            scriptingEngine.enterContext(globals);

                            currentElement = root;

                            if (method.indexOf('.') > -1) {
                                StringTokenizer st = new StringTokenizer(method, ".");
                                int cnt = st.countTokens();

                                for (int i = 1; i < cnt; i++) {
                                    String next = st.nextToken();

                                    currentElement = getChildElement(currentElement,
                                                                     next);
                                }

                                if (currentElement == null) {
                                    throw new FrameworkException("Method name \"" +
                                                                 method +
                                                                 "\" could not be resolved.");
                                }

                                method = st.nextToken();
                            }

                            if (reqtype == XMLRPC) {
                                // check XML-RPC access permissions
                                String proto = app.getPrototypeName(currentElement);

                                app.checkXmlRpcAccess(proto, method);
                            }

                            // reset skin recursion detection counter
                            skinDepth = 0;

                            result = scriptingEngine.invoke(currentElement, method, args,
                                                            true);
                            commitTransaction();
                        } catch (Exception x) {
                            abortTransaction(false);

                            app.logEvent("Exception in " + Thread.currentThread() + ": " +
                                         x);

                            // If the transactor thread has been killed by the invoker thread we don't have to
                            // bother for the error message, just quit.
                            if (localrtx != rtx) {
                                return;
                            }

                            this.exception = x;
                        }

                        break;


                    case INTERNAL:

                        // Just a human readable descriptor of this invocation
                        String funcdesc = app.getName() + ":internal:" + method;

                        // if thisObject is an instance of NodeHandle, get the node object itself.
                        if ((thisObject != null) && thisObject instanceof NodeHandle) {
                            thisObject = ((NodeHandle) thisObject).getNode(app.nmgr.safe);

                            // see if a valid node was returned
                            if (thisObject == null) {
                                reqtype = NONE;

                                break;
                            }
                        }

                        // avoid going into transaction if called function doesn't exist.
                        boolean functionexists = true;

                        // this only works for the (common) case that method is a plain
                        // method name, not an obj.method path
                        if (method.indexOf('.') < 0) {
                            functionexists = scriptingEngine.hasFunction(thisObject, method);
                        }

                        if (!functionexists) {
                            // function doesn't exist, nothing to do here.
                            reqtype = NONE;
                        } else {
                            try {
                                localrtx.begin(funcdesc);

                                root = app.getDataRoot();

                                HashMap globals = new HashMap();

                                globals.put("root", root);
                                globals.put("res", new ResponseBean(res));
                                globals.put("app", new ApplicationBean(app));

                                scriptingEngine.enterContext(globals);

                                // reset skin recursion detection counter
                                skinDepth = 0;

                                result = scriptingEngine.invoke(thisObject, method, args,
                                                                false);
                                commitTransaction();
                            } catch (Exception x) {
                                abortTransaction(false);

                                app.logEvent("Exception in " + Thread.currentThread() +
                                             ": " + x);

                                // If the transactor thread has been killed by the invoker thread we don't have to
                                // bother for the error message, just quit.
                                if (localrtx != rtx) {
                                    return;
                                }

                                this.exception = x;
                            }
                        }

                        break;
                }

                // exit execution context
                scriptingEngine.exitContext();

                // make sure there is only one thread running per instance of this class
                // if localrtx != rtx, the current thread has been aborted and there's no need to notify
                if (localrtx != rtx) {
                    localrtx.closeConnections();

                    return;
                }

                notifyAndWait();
            } while (localrtx == rtx);
        } finally {
            localrtx.closeConnections();
        }
    }

    /**
     * Called by the transactor thread when it has successfully fulfilled a request.
     */
    synchronized void commitTransaction() throws Exception {
        Transactor localrtx = (Transactor) Thread.currentThread();

        if (localrtx == rtx) {
            reqtype = NONE;
            localrtx.commit();
        } else {
            throw new TimeoutException();
        }
    }

    synchronized void abortTransaction(boolean retry) {
        Transactor localrtx = (Transactor) Thread.currentThread();

        if (!retry && (localrtx == rtx)) {
            reqtype = NONE;
        }

        try {
            localrtx.abort();
        } catch (Exception ignore) {
        }
    }

    /**
     * Tell waiting thread that we're done, then wait for next request
     */
    synchronized void notifyAndWait() {
        Transactor localrtx = (Transactor) Thread.currentThread();

        if (reqtype != NONE) {
            return; // is there a new request already?
        }

        notifyAll();

        try {
            // wait for request, max 10 min
            wait(1000 * 60 * 10);

            //  if no request arrived, release ressources and thread
            if ((reqtype == NONE) && (rtx == localrtx)) {
                // comment this in to release not just the thread, but also the scripting engine.
                // currently we don't do this because of the risk of memory leaks (objects from
                // framework referencing into the scripting engine)
                // scriptingEngine = null;
                rtx = null;
            }
        } catch (InterruptedException ir) {
        }
    }

    /**
     *
     *
     * @param req ...
     * @param session ...
     *
     * @return ...
     *
     * @throws Exception ...
     */
    public synchronized ResponseTrans invokeHttp(RequestTrans req, Session session)
                                      throws Exception {
        this.reqtype = HTTP;
        this.req = req;
        this.session = session;
        this.res = new ResponseTrans(req);

        app.activeRequests.put(req, this);

        checkThread();
        wait(app.requestTimeout);

        if (reqtype != NONE) {
            app.logEvent("Stopping Thread for Request " + app.getName() + "/" + req.path);
            stopThread();
            res.reset();
            res.write("<b>Error in application '" + app.getName() +
                      "':</b> <br><br><pre>Request timed out.</pre>");
        }

        return res;
    }

    /**
     * This checks if the Evaluator is already executing an equal request. If so, attach to it and
     * wait for it to complete. Otherwise return null, so the application knows it has to run the request.
     */
    public synchronized ResponseTrans attachRequest(RequestTrans req)
                                             throws InterruptedException {
        if ((this.req == null) || (res == null) || !this.req.equals(req)) {
            return null;
        }

        // we already know our response object
        ResponseTrans r = res;

        if (reqtype != NONE) {
            wait(app.requestTimeout);
        }

        return r;
    }

    /**
     *
     *
     * @param method ...
     * @param args ...
     *
     * @return ...
     *
     * @throws Exception ...
     */
    public synchronized Object invokeXmlRpc(String method, Object[] args)
                                     throws Exception {
        this.reqtype = XMLRPC;
        this.session = null;
        this.method = method;
        this.args = args;
        this.res = new ResponseTrans();
        result = null;
        exception = null;

        checkThread();
        wait(app.requestTimeout);

        if (reqtype != NONE) {
            stopThread();
        }

        // reset res for garbage collection (res.data may hold reference to evaluator)
        res = null;

        if (exception != null) {
            throw (exception);
        }

        return result;
    }



    /**
     *
     *
     * @param method ...
     * @param args ...
     *
     * @return ...
     *
     * @throws Exception ...
     */
    public synchronized Object invokeExternal(String method, Object[] args)
                                     throws Exception {
        this.reqtype = EXTERNAL;
        this.session = null;
        this.method = method;
        this.args = args;
        this.res = new ResponseTrans();
        result = null;
        exception = null;

        checkThread();
        wait();

        if (reqtype != NONE) {
            stopThread();
        }

        // reset res for garbage collection (res.data may hold reference to evaluator)
        res = null;

        if (exception != null) {
            throw (exception);
        }

        return result;
    }

    /**
     *
     *
     * @param obj ...
     * @param functionName ...
     * @param args ...
     *
     * @return ...
     *
     * @throws Exception ...
     */
    public Object invokeDirectFunction(Object obj, String functionName, Object[] args)
                                throws Exception {
        return scriptingEngine.invoke(obj, functionName, args, false);
    }

    /**
     *
     *
     * @param object ...
     * @param functionName ...
     * @param args ...
     *
     * @return ...
     *
     * @throws Exception ...
     */
    public synchronized Object invokeInternal(Object object, String functionName,
                                              Object[] args)
                                       throws Exception {
        // give internal call more time (15 minutes) to complete
        return invokeInternal(object, functionName, args, 60000L * 15);
    }

    /**
     *
     *
     * @param object ...
     * @param functionName ...
     * @param args ...
     * @param timeout ...
     *
     * @return ...
     *
     * @throws Exception ...
     */
    public synchronized Object invokeInternal(Object object, String functionName,
                                              Object[] args, long timeout)
                                       throws Exception {
        reqtype = INTERNAL;
        session = null;
        thisObject = object;
        method = functionName;
        this.args = args;
        this.res = new ResponseTrans();
        result = null;
        exception = null;

        checkThread();
        wait(timeout);

        if (reqtype != NONE) {
            stopThread();
        }

        // reset res for garbage collection (res.data may hold reference to evaluator)
        res = null;

        if (exception != null) {
            throw (exception);
        }

        return result;
    }


    private Object getChildElement(Object obj, String name) throws ScriptingException {
        if (scriptingEngine.hasFunction(obj, "getChildElement")) {
            return scriptingEngine.invoke(obj, "getChildElement", new Object[] {name}, false);
        }

        if (obj instanceof IPathElement) {
            return ((IPathElement) obj).getChildElement(name);
        }
        
        return null;
    }

    /**
     *  Stop this request evaluator's current thread. If currently active kill the request, otherwise just
     *  notify.
     */
    public synchronized void stopThread() {
        Transactor t = rtx;

        // let the scripting engine know that the
        // current transaction is being aborted.
        if (scriptingEngine != null) {
            scriptingEngine.abort();
        }

        rtx = null;

        if (t != null) {
            if (reqtype != NONE) {
                app.logEvent("Killing Thread " + t);
                reqtype = NONE;
                t.kill();

                try {
                    t.abort();
                } catch (Exception ignore) {
                }
            } else {
                notifyAll();
            }

            t.closeConnections();
        }
    }

    private synchronized void checkThread() {
        if (!app.isRunning()) {
            throw new ApplicationStoppedException();
        }

        if ((rtx == null) || !rtx.isAlive()) {
            // app.logEvent ("Starting Thread");
            rtx = new Transactor(this, app.threadgroup, app.nmgr);
            rtx.setContextClassLoader(app.getClassLoader());
            rtx.start();
        } else {
            notifyAll();
        }
    }

    /**
     *  Null out some fields, mostly for the sake of garbage collection.
     */
    public void recycle() {
        res = null;
        req = null;
        session = null;
        args = null;
        requestPath = null;
        result = null;
        exception = null;
    }

    /**
     * Check if an action with a given name is defined for a scripted object. If it is,
     * return the action's function name. Otherwise, return null.
     */
    public String getAction(Object obj, String action) {
        if (obj == null) {
            return null;
        }

        String act = (action == null) ? "main_action" : (action + "_action");

        if (scriptingEngine.hasFunction(obj, act)) {
            return act;
        }

        return null;
    }

}
