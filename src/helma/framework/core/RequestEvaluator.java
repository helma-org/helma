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

import org.apache.xmlrpc.XmlRpcRequestProcessor;
import org.apache.xmlrpc.XmlRpcServerRequest;

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

    public static final Object[] EMPTY_ARGS = new Object[0];

    public final Application app;

    protected ScriptingEngine scriptingEngine;

    // skin depth counter, used to avoid recursive skin rendering
    protected int skinDepth;

    private volatile RequestTrans req;
    private volatile ResponseTrans res;

    // the one and only transactor thread
    private volatile Thread thread;

    private volatile Transactor transactor;

    // the type of request to be serviced,
    // used to coordinate worker and waiter threads
    private volatile int reqtype;

    // the object on which to invoke a function, if specified
    private volatile Object thisObject;

    // the method to be executed
    private volatile Object function;

    // the session object associated with the current request
    private volatile Session session;

    // arguments passed to the function
    private volatile Object[] args;

    // the result of the operation
    private volatile Object result;

    // the exception thrown by the evaluator, if any.
    private volatile Exception exception;

    /**
     *  Create a new RequestEvaluator for this application.
     *  @param app the application
     */
    public RequestEvaluator(Application app) {
        this.app = app;
    }

    protected synchronized void initScriptingEngine() {
        if (scriptingEngine == null) {
            String engineClassName = app.getProperty("scriptingEngine",
                                                     "helma.scripting.rhino.RhinoEngine");
            try {
                app.setCurrentRequestEvaluator(this);
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
                app.logError("Error creating scripting engine", t);

                // null out invalid scriptingEngine
                scriptingEngine = null;
                // rethrow exception
                if (t instanceof RuntimeException) {
                    throw((RuntimeException) t);
                } else {
                    throw new RuntimeException(t.toString(), t);
                }
            } finally {
                app.setCurrentRequestEvaluator(null);
            }
        }
    }

    /**
     *
     */
    public void run() {
        // first, set a local variable to the current transactor thread so we know
        // when it's time to quit because another thread took over.
        Thread localThread = Thread.currentThread();

        // spans whole execution loop - close connections in finally clause
        try {

            // while this thread is serving requests
            while (localThread == thread) {

                // object reference to ressolve request path
                Object currentElement;

                // Get req and res into local variables to avoid memory caching problems
                // in unsynchronized method.
                RequestTrans req = getRequest();
                ResponseTrans res = getResponse();

                // request path object
                RequestPath requestPath = new RequestPath(app);

                int tries = 0;
                boolean done = false;
                Throwable error = null;
                String functionName = function instanceof String ?
                        (String) function : null;

                while (!done && localThread == thread) {
                    // catch errors in path resolution and script execution
                    try {

                        // initialize scripting engine
                        initScriptingEngine();
                        app.setCurrentRequestEvaluator(this);
                        // update scripting prototypes
                        scriptingEngine.enterContext();


                        // avoid going into transaction if called function doesn't exist.
                        // this only works for the (common) case that method is a plain
                        // method name, not an obj.method path
                        if (reqtype == INTERNAL) {
                            // if object is an instance of NodeHandle, get the node object itself.
                            if (thisObject instanceof NodeHandle) {
                                thisObject = ((NodeHandle) thisObject).getNode(app.nmgr.safe);
                                // If no valid node object return immediately
                                if (thisObject == null) {
                                    done = true;
                                    reqtype = NONE;
                                    break;
                                }
                            }
                            // If function doesn't exist, return immediately
                            if (functionName != null && !scriptingEngine.hasFunction(thisObject, functionName, true)) {
                                app.logEvent(missingFunctionMessage(thisObject, functionName));
                                done = true;
                                reqtype = NONE;
                                break;
                            }
                        } else if (function != null && functionName == null) {
                            // only internal requests may pass a function instead of a function name
                            throw new IllegalStateException("No function name in non-internal request ");
                        }

                        // Transaction name is used for logging etc.
                        StringBuffer txname = new StringBuffer(app.getName());
                        txname.append(":").append(req.getMethod().toLowerCase()).append(":");
                        txname.append((error == null) ? req.getPath() : "error");

                        // begin transaction
                        transactor = Transactor.getInstance(app.nmgr);
                        transactor.begin(txname.toString());

                        Object root = app.getDataRoot();
                        initGlobals(root, requestPath);

                        String action = null;

                        if (error != null) {
                            res.setError(error);
                        }

                        switch (reqtype) {
                            case HTTP:

                                // bring over the message from a redirect
                                session.recoverResponseMessages(res);

                                // catch redirect in path resolution or script execution
                                try {
                                    // catch object not found in path resolution
                                    try {
                                        if (error != null) {
                                            // there was an error in the previous loop, call error handler
                                            currentElement = root;
                                            res.setStatus(500);

                                            // do not reset the requestPath so error handler can use the original one
                                            // get error handler action
                                            String errorAction = app.props.getProperty("error",
                                                    "error");

                                            action = getAction(currentElement, errorAction, req);

                                            if (action == null) {
                                                throw new RuntimeException(error);
                                            }
                                        } else if ((req.getPath() == null) ||
                                                "".equals(req.getPath().trim())) {
                                            currentElement = root;
                                            requestPath.add(null, currentElement);

                                            action = getAction(currentElement, null, req);

                                            if (action == null) {
                                                throw new NotFoundException("Action not found");
                                            }
                                        } else {
                                            // march down request path...
                                            StringTokenizer st = new StringTokenizer(req.getPath(),
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
                                                    throw new NotFoundException("Object not found.");
                                                }

                                                if (pathItems[i].length() == 0) {
                                                    continue;
                                                }

                                                // if we're at the last element of the path,
                                                // try to interpret it as action name.
                                                if (i == (ntokens - 1) && !req.getPath().endsWith("/")) {
                                                    action = getAction(currentElement, pathItems[i], req);
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
                                                throw new NotFoundException("Object not found.");
                                            }

                                            if (action == null) {
                                                action = getAction(currentElement, null, req);
                                            }

                                            if (action == null) {
                                                throw new NotFoundException("Action not found");
                                            }
                                        }
                                    } catch (NotFoundException notfound) {
                                        if (error != null) {

                                            // we already have an error and the error template wasn't found,
                                            // display it instead of notfound message
                                            throw new RuntimeException();
                                        }

                                        // The path could not be resolved. Check if there is a "not found" action
                                        // specified in the property file.
                                        res.setStatus(404);

                                        String notFoundAction = app.props.getProperty("notfound",
                                                "notfound");

                                        currentElement = root;
                                        action = getAction(currentElement, notFoundAction, req);

                                        if (action == null) {
                                            throw new NotFoundException(notfound.getMessage());
                                        }
                                    }

                                    // register path objects with their prototype names in
                                    // res.handlers
                                    Map macroHandlers = res.getMacroHandlers();
                                    int l = requestPath.size();
                                    Prototype[] protos = new Prototype[l];

                                    for (int i = 0; i < l; i++) {

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
                                    for (int i = l - 1; i >= 0; i--) {
                                        if (protos[i] != null) {
                                            protos[i].registerParents(macroHandlers, requestPath.get(i));
                                        }
                                    }

                                    /////////////////////////////////////////////////////////////////////////////
                                    // end of path resolution section
                                    /////////////////////////////////////////////////////////////////////////////
                                    // beginning of execution section

                                    // set the req.action property, cutting off the _action suffix
                                    req.setAction(action);

                                    // reset skin recursion detection counter
                                    skinDepth = 0;

                                    // try calling onRequest() function on object before
                                    // calling the actual action
                                    scriptingEngine.invoke(currentElement,
                                            "onRequest",
                                            EMPTY_ARGS,
                                            ScriptingEngine.ARGS_WRAP_DEFAULT,
                                            false);

                                    // reset skin recursion detection counter
                                    skinDepth = 0;

                                    Object actionProcessor = req.getActionHandler() != null ?
                                        req.getActionHandler() : action;

                                    // do the actual action invocation
                                    if (req.isXmlRpc()) {
                                        XmlRpcRequestProcessor xreqproc = new XmlRpcRequestProcessor();
                                        XmlRpcServerRequest xreq = xreqproc.decodeRequest(req.getServletRequest()
                                                .getInputStream());
                                        Vector args = xreq.getParameters();
                                        args.add(0, xreq.getMethodName());
                                        result = scriptingEngine.invoke(currentElement,
                                                actionProcessor,
                                                args.toArray(),
                                                ScriptingEngine.ARGS_WRAP_XMLRPC,
                                                false);
                                        res.writeXmlRpcResponse(result);
                                    } else {
                                        scriptingEngine.invoke(currentElement,
                                                actionProcessor,
                                                EMPTY_ARGS,
                                                ScriptingEngine.ARGS_WRAP_DEFAULT,
                                                false);
                                    }

                                    // try calling onResponse() function on object before
                                    // calling the actual action
                                    scriptingEngine.invoke(currentElement,
                                            "onResponse",
                                            EMPTY_ARGS,
                                            ScriptingEngine.ARGS_WRAP_DEFAULT,
                                            false);

                                } catch (RedirectException redirect) {
                                    // if there is a message set, save it on the user object for the next request
                                    if (res.getRedirect() != null)
                                        session.storeResponseMessages(res);
                                }

                                // check if request is still valid, or if the requesting thread has stopped waiting already
                                if (localThread != thread) {
                                    return;
                                }
                                commitTransaction();
                                done = true;

                                break;

                            case XMLRPC:
                            case EXTERNAL:

                                try {
                                    currentElement = root;

                                    if (functionName.indexOf('.') > -1) {
                                        StringTokenizer st = new StringTokenizer(functionName, ".");
                                        int cnt = st.countTokens();

                                        for (int i = 1; i < cnt; i++) {
                                            String next = st.nextToken();
                                            currentElement = getChildElement(currentElement, next);
                                        }

                                        if (currentElement == null) {
                                            throw new NotFoundException("Method name \"" +
                                                    function + "\" could not be resolved.");
                                        }

                                        functionName = st.nextToken();
                                    }

                                    if (reqtype == XMLRPC) {
                                        // check XML-RPC access permissions
                                        String proto = app.getPrototypeName(currentElement);
                                        app.checkXmlRpcAccess(proto, functionName);
                                    }

                                    // reset skin recursion detection counter
                                    skinDepth = 0;
                                    if (!scriptingEngine.hasFunction(currentElement, functionName, false)) {
                                        throw new NotFoundException(missingFunctionMessage(currentElement, functionName));
                                    }
                                    result = scriptingEngine.invoke(currentElement,
                                            functionName, args,
                                            ScriptingEngine.ARGS_WRAP_XMLRPC,
                                            false);
                                    // check if request is still valid, or if the requesting thread has stopped waiting already
                                    if (localThread != thread) {
                                        return;
                                    }
                                    commitTransaction();
                                } catch (Exception x) {
                                    // check if request is still valid, or if the requesting thread has stopped waiting already
                                    if (localThread != thread) {
                                        return;
                                    }
                                    abortTransaction();
                                    app.logError(txname + ": " + error, x);

                                    // If the transactor thread has been killed by the invoker thread we don't have to
                                    // bother for the error message, just quit.
                                    if (localThread != thread) {
                                        return;
                                    }

                                    this.exception = x;
                                }

                                done = true;
                                break;

                            case INTERNAL:

                                try {
                                    // reset skin recursion detection counter
                                    skinDepth = 0;

                                    result = scriptingEngine.invoke(thisObject,
                                            function,
                                            args,
                                            ScriptingEngine.ARGS_WRAP_DEFAULT,
                                            true);
                                    // check if request is still valid, or if the requesting thread has stopped waiting already
                                    if (localThread != thread) {
                                        return;
                                    }
                                    commitTransaction();
                                } catch (Exception x) {
                                    // check if request is still valid, or if the requesting thread has stopped waiting already
                                    if (localThread != thread) {
                                        return;
                                    }
                                    abortTransaction();
                                    app.logError(txname + ": " + error, x);

                                    // If the transactor thread has been killed by the invoker thread we don't have to
                                    // bother for the error message, just quit.
                                    if (localThread != thread) {
                                        return;
                                    }

                                    this.exception = x;
                                }

                                done = true;
                                break;

                        } // switch (reqtype)
                    } catch (AbortException x) {
                        // res.abort() just aborts the transaction and
                        // leaves the response untouched
                        // check if request is still valid, or if the requesting thread has stopped waiting already
                        if (localThread != thread) {
                            return;
                        }
                        abortTransaction();
                        done = true;
                    } catch (ConcurrencyException x) {
                        res.reset();

                        if (++tries < 8) {
                            // try again after waiting some period
                            // check if request is still valid, or if the requesting thread has stopped waiting already
                            if (localThread != thread) {
                                return;
                            }
                            abortTransaction();

                            try {
                                // wait a bit longer with each try
                                int base = 800 * tries;
                                Thread.sleep((long) (base + (Math.random() * base * 2)));
                            } catch (InterruptedException interrupt) {
                                // we got interrrupted, create minimal error message 
                                res.reportError(interrupt);
                                done = true;
                                // and release resources and thread
                                thread = null;
                                transactor = null;
                            }
                        } else {
                            // check if request is still valid, or if the requesting thread has stopped waiting already
                            if (localThread != thread) {
                                return;
                            }
                            abortTransaction();

                            // error in error action. use traditional minimal error message
                            res.reportError("Application too busy, please try again later");
                            done = true;
                        }
                    } catch (Throwable x) {
                        // check if request is still valid, or if the requesting thread has stopped waiting already
                        if (localThread != thread) {
                            return;
                        }
                        abortTransaction();

                        // If the transactor thread has been killed by the invoker thread we don't have to
                        // bother for the error message, just quit.
                        if (localThread != thread) {
                            return;
                        }

                        res.reset();

                        // check if we tried to process the error already,
                        // or if this is an XML-RPC request
                        if (error == null) {
                            if (!(x instanceof NotFoundException)) {
                                app.errorCount += 1;
                            }

                            // set done to false so that the error will be processed
                            done = false;
                            error = x;

                            Transactor tx = Transactor.getInstance();
                            String txname = tx == null ? "no-txn" : tx.getTransactionName();
                            app.logError(txname + ": " + error, x);

                            if (req.isXmlRpc()) {
                                // if it's an XML-RPC exception immediately generate error response
                                if (!(x instanceof Exception)) {
                                    // we need an exception to pass to XML-RPC responder
                                    x = new Exception(x.toString(), x);
                                }
                                res.writeXmlRpcError((Exception) x);
                                done = true;
                            }
                        } else {
                            // error in error action. use traditional minimal error message
                            res.reportError(error);
                            done = true;
                        }
                    } finally {
                        app.setCurrentRequestEvaluator(null);
                        // exit execution context
                        if (scriptingEngine != null)
                            scriptingEngine.exitContext();
                    }
                }

                notifyAndWait();

            }
        } finally {
            Transactor tx = Transactor.getInstance();
            if (tx != null) tx.closeConnections();
        }
    }

    /**
     * Called by the transactor thread when it has successfully fulfilled a request.
     * @throws Exception transaction couldn't be committed
     */
    synchronized void commitTransaction() throws Exception {
        Thread localThread = Thread.currentThread();

        if (localThread == thread) {
            Transactor tx = Transactor.getInstance();
            if (tx != null)
                tx.commit();
        } else {
            throw new TimeoutException();
        }
    }

    /**
     * Called by the transactor thread when the request didn't terminate successfully.
     */
    synchronized void abortTransaction() {
        Transactor tx = Transactor.getInstance();
        if (tx != null) tx.abort();
    }

    /**
     * Initialize and start the transactor thread.
     */
    private synchronized void startTransactor() {
        if (!app.isRunning()) {
            throw new ApplicationStoppedException();
        }

        if ((thread == null) || !thread.isAlive()) {
            // app.logEvent ("Starting Thread");
            thread = new Thread(app.threadgroup, this);
            thread.setContextClassLoader(app.getClassLoader());
            thread.start();
        } else {
            notifyAll();
        }
    }

    /**
     * Tell waiting thread that we're done, then wait for next request
     */
    synchronized void notifyAndWait() {
        Thread localThread = Thread.currentThread();

        // make sure there is only one thread running per instance of this class
        // if localrtx != rtx, the current thread has been aborted and there's no need to notify
        if (localThread != thread) {
            // A new request came in while we were finishing the last one.
            // Return to run() to get the work done.
            Transactor tx = Transactor.getInstance();
            if (tx != null) {
                tx.closeConnections();
            }
            return;
        }

        reqtype = NONE;
        notifyAll();

        try {
            // wait for request, max 10 min
            wait(1000 * 60 * 10);
        } catch (InterruptedException ix) {
            // we got interrrupted, releases resources and thread
            thread = null;
            transactor = null;
        }

        //  if no request arrived, release ressources and thread
        if ((reqtype == NONE) && (thread == localThread)) {
            // comment this in to release not just the thread, but also the scripting engine.
            // currently we don't do this because of the risk of memory leaks (objects from
            // framework referencing into the scripting engine)
            // scriptingEngine = null;
            thread = null;
            transactor = null;
        }
    }

    /**
     * Stop this request evaluator's current thread. This is called by the
     * waiting thread when it times out and stops waiting, or from an outside
     * thread. If currently active kill the request, otherwise just notify.
     */
    synchronized boolean stopTransactor() {
        Transactor t = transactor;
        thread = null;
        transactor = null;
        boolean stopped = false;
        if (t != null && t.isActive()) {
            // let the scripting engine know that the
            // current transaction is being aborted.
            if (scriptingEngine != null) {
                scriptingEngine.abort();
            }

            app.logEvent("Request timeout for thread " + t);

            reqtype = NONE;

            t.kill();
            t.abort();
            t.closeConnections();
            stopped = true;
        }
        notifyAll();
        return stopped;
    }

    /**
     * Invoke an action function for a HTTP request. The function is dispatched
     * in a new thread and waits for it to finish.
     *
     * @param req the incoming HTTP request
     * @param session the client's session
     * @return the result returned by the invocation
     * @throws Exception any exception thrown by the invocation
     */
    public synchronized ResponseTrans invokeHttp(RequestTrans req, Session session)
                                      throws Exception {
        initObjects(req, session);

        app.activeRequests.put(req, this);

        startTransactor();
        wait(app.requestTimeout);

        if (reqtype != NONE && stopTransactor()) {
            res.reset();
            res.reportError("Request timed out");
        }

        session.commit(this);
        return res;
    }

    /**
     * This checks if the Evaluator is already executing an equal request.
     * If so, attach to it and wait for it to complete. Otherwise return null,
     * so the application knows it has to run the request.
     */
    public synchronized ResponseTrans attachHttpRequest(RequestTrans req)
                                             throws Exception {
        // Get a reference to the res object at the time we enter
        ResponseTrans localRes = res;

        if ((localRes == null) || !req.equals(this.req)) {
            return null;
        }

        if (reqtype != NONE) {
            wait(app.requestTimeout);
        }

        return localRes;
    }

    /*
     * TODO invokeXmlRpc(), invokeExternal() and invokeInternal() are basically the same
     * and should be unified
     */

    /**
     * Invoke a function for an XML-RPC request. The function is dispatched in a new thread
     * and waits for it to finish.
     *
     * @param functionName the name of the function to invoke
     * @param args the arguments
     * @return the result returned by the invocation
     * @throws Exception any exception thrown by the invocation
     */
    public synchronized Object invokeXmlRpc(String functionName, Object[] args)
                                     throws Exception {
        initObjects(functionName, XMLRPC, RequestTrans.XMLRPC);
        this.function = functionName;
        this.args = args;

        startTransactor();
        wait(app.requestTimeout);

        if (reqtype != NONE && stopTransactor()) {
            exception = new RuntimeException("Request timed out");
        }

        // reset res for garbage collection (res.data may hold reference to evaluator)
        res = null;

        if (exception != null) {
            throw (exception);
        }

        return result;
    }



    /**
     * Invoke a function for an external request. The function is dispatched
     * in a new thread and waits for it to finish.
     *
     * @param functionName the name of the function to invoke
     * @param args the arguments
     * @return the result returned by the invocation
     * @throws Exception any exception thrown by the invocation
     */
    public synchronized Object invokeExternal(String functionName, Object[] args)
                                     throws Exception {
        initObjects(functionName, EXTERNAL, RequestTrans.EXTERNAL);
        this.function = functionName;
        this.args = args;

        startTransactor();
        wait();

        if (reqtype != NONE && stopTransactor()) {
            exception = new RuntimeException("Request timed out");
        }

        // reset res for garbage collection (res.data may hold reference to evaluator)
        res = null;

        if (exception != null) {
            throw (exception);
        }

        return result;
    }

    /**
     * Invoke a function internally and directly, using the thread we're running on.
     *
     * @param obj the object to invoke the function on
     * @param function the function or name of the function to invoke
     * @param args the arguments
     * @return the result returned by the invocation
     * @throws Exception any exception thrown by the invocation
     */
    public Object invokeDirectFunction(Object obj, Object function, Object[] args)
                                throws Exception {
        return scriptingEngine.invoke(obj, function, args,
                ScriptingEngine.ARGS_WRAP_DEFAULT, true);
    }

    /**
     * Invoke a function internally. The function is dispatched in a new thread
     * and waits for it to finish.
     *
     * @param object the object to invoke the function on
     * @param function the function or name of the function to invoke
     * @param args the arguments
     * @return the result returned by the invocation
     * @throws Exception any exception thrown by the invocation
     */
    public synchronized Object invokeInternal(Object object, Object function,
                                              Object[] args)
                                       throws Exception {
        // give internal call more time (15 minutes) to complete
        return invokeInternal(object, function, args, 60000L * 15);
    }

    /**
     * Invoke a function internally. The function is dispatched in a new thread
     * and waits for it to finish.
     *
     * @param object the object to invoke the function on
     * @param function the function or name of the function to invoke
     * @param args the arguments
     * @param timeout the time in milliseconds to wait for the function to return, or
     * -1 to wait indefinitely
     * @return the result returned by the invocation
     * @throws Exception any exception thrown by the invocation
     */
    public synchronized Object invokeInternal(Object object, Object function,
                                              Object[] args, long timeout)
                                       throws Exception {
        initObjects(function, INTERNAL, RequestTrans.INTERNAL);
        thisObject = object;
        this.function = function;
        this.args = args;

        startTransactor();
        if (timeout < 0)
            wait();
        else
            wait(timeout);

        if (reqtype != NONE && stopTransactor()) {
            exception = new RuntimeException("Request timed out");
        }

        // reset res for garbage collection (res.data may hold reference to evaluator)
        res = null;

        if (exception != null) {
            throw (exception);
        }

        return result;
    }


    /**
     * Init this evaluator's objects from a RequestTrans for a HTTP request
     *
     * @param req
     * @param session
     */
    private synchronized void initObjects(RequestTrans req, Session session) {
        this.req = req;
        this.reqtype = HTTP;
        this.session = session;
        res = new ResponseTrans(app, req);
        result = null;
        exception = null;
    }

    /**
     * Init this evaluator's objects for an internal, external or XML-RPC type
     * request.
     *
     * @param function the function name or object
     * @param reqtype the request type
     * @param reqtypeName the request type name
     */
    private synchronized void initObjects(Object function, int reqtype, String reqtypeName) {
        this.reqtype = reqtype;
        String functionName = function instanceof String ?
                (String) function : "<function>";
        req = new RequestTrans(reqtypeName, functionName);
        session = new Session(functionName, app);
        res = new ResponseTrans(app, req);
        result = null;
        exception = null;
    }

    /**
     * Initialize the globals in the scripting engine for the current request.
     *
     * @param root
     * @throws ScriptingException
     */
    private synchronized void initGlobals(Object root, Object requestPath)
                throws ScriptingException {
        HashMap globals = new HashMap();

        globals.put("root", root);
        globals.put("session", new SessionBean(session));
        globals.put("req", new RequestBean(req));
        globals.put("res", new ResponseBean(res));
        globals.put("app", new ApplicationBean(app));
        globals.put("path", requestPath);

        // enter execution context
        scriptingEngine.setGlobals(globals);
    }

    /**
     * Get the child element with the given name from the given object.
     *
     * @param obj
     * @param name
     * @return
     * @throws ScriptingException
     */
    private Object getChildElement(Object obj, String name) throws ScriptingException {
        if (scriptingEngine.hasFunction(obj, "getChildElement", false)) {
            return scriptingEngine.invoke(obj, "getChildElement", new Object[] {name},
                                          ScriptingEngine.ARGS_WRAP_DEFAULT, false);
        }

        if (obj instanceof IPathElement) {
            return ((IPathElement) obj).getChildElement(name);
        }

        return null;
    }

    /**
     *  Null out some fields, mostly for the sake of garbage collection.
     */
    synchronized void recycle() {
        res = null;
        req = null;
        session = null;
        function = null;
        args = null;
        result = null;
        exception = null;
    }

    /**
     * Check if an action with a given name is defined for a scripted object. If it is,
     * return the action's function name. Otherwise, return null.
     */
    public String getAction(Object obj, String action, RequestTrans req) {
        if (obj == null)
            return null;

        if (action == null)
            action = "main";

        StringBuffer buffer = new StringBuffer(action).append("_action");
        // record length so we can check without method
        // afterwards for GET, POST, HEAD requests
        int length = buffer.length();

        if (req.checkXmlRpc()) {
            // append _methodname
            buffer.append("_xmlrpc");
            if (scriptingEngine.hasFunction(obj, buffer.toString(), false)) {
                // handle as XML-RPC request
                req.setMethod(RequestTrans.XMLRPC);
                return buffer.toString();
            }
            // cut off method in case it has been appended
            buffer.setLength(length);
        }

        String method = req.getMethod();
        // append HTTP method to action name
        if (method != null) {
            // append _methodname
            buffer.append('_').append(method.toLowerCase());
            if (scriptingEngine.hasFunction(obj, buffer.toString(), false))
                return buffer.toString();

            // cut off method in case it has been appended
            buffer.setLength(length);
        }

        // if no method specified or "ordinary" request try action without method
        if (method == null || "GET".equalsIgnoreCase(method) ||
                              "POST".equalsIgnoreCase(method) ||
                              "HEAD".equalsIgnoreCase(method)) {
            if (scriptingEngine.hasFunction(obj, buffer.toString(), false))
                return buffer.toString();
        }

        return null;
    }

    /**
     * Returns this evaluator's scripting engine
     */
    public ScriptingEngine getScriptingEngine() {
        if (scriptingEngine == null) {
            initScriptingEngine();
        }
        return scriptingEngine;
    }

    /**
     * Get the request object for the current request.
     *
     * @return the request object
     */
    public synchronized RequestTrans getRequest() {
        return req;
    }

    /**
     * Get the response object for the current request.
     *
     * @return the response object
     */
    public synchronized ResponseTrans getResponse() {
        return res;
    }

    /**
     * Get the current transactor thread
     *
     * @return the current transactor thread
     */
    public synchronized Thread getThread() {
        return thread;
    }

    /**
     * Return the current session
     *
     * @return the session for the current request
     */
    public synchronized Session getSession() {
        return session;
    }

    private String missingFunctionMessage(Object obj, String funcName) {
        if (obj == null)
            return "Function " + funcName + " not defined in global scope";
        else
            return "Function " + funcName + " not defined for " + obj;
    }
}
