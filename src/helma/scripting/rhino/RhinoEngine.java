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

package helma.scripting.rhino;

import helma.doc.DocApplication;
import helma.extensions.ConfigurationException;
import helma.extensions.HelmaExtension;
import helma.framework.*;
import helma.framework.repository.Resource;
import helma.framework.core.*;
import helma.main.Server;
import helma.objectmodel.*;
import helma.objectmodel.db.DbMapping;
import helma.objectmodel.db.Relation;
import helma.scripting.*;
import helma.scripting.rhino.debug.Tracer;
import org.mozilla.javascript.*;
import org.mozilla.javascript.serialize.ScriptableOutputStream;
import org.mozilla.javascript.serialize.ScriptableInputStream;

import java.util.*;
import java.io.*;
import java.lang.ref.WeakReference;

/**
 * This is the implementation of ScriptingEnvironment for the Mozilla Rhino EcmaScript interpreter.
 */
public class RhinoEngine implements ScriptingEngine {
    // map for Application to RhinoCore binding
    static final Map coreMap = new WeakHashMap();

    // the application we're running in
    public Application app;

    // The Rhino context
    Context context;

    // the per-thread global object
    GlobalObject global;

    // the request evaluator instance owning this fesi evaluator
    RequestEvaluator reval;

    // the rhino core
    RhinoCore core;

    // the global vars set by extensions
    HashMap extensionGlobals;

    // the thread currently running this engine
    volatile Thread thread;

    // thread local engine registry
    static ThreadLocal engines = new ThreadLocal();

    // the introspector that provides documentation for this application
    DocApplication doc = null;

    /**
     *  Zero argument constructor.
     */
    public RhinoEngine() {
        // nothing to do
    }

    /**
     * Init the scripting engine with an application and a request evaluator
     */
    public synchronized void init(Application app, RequestEvaluator reval) {
        this.app = app;
        this.reval = reval;
        initRhinoCore(app);

        context = core.contextFactory.enter();

        try {
            extensionGlobals = new HashMap();

            if (Server.getServer() != null) {
                Vector extVec = Server.getServer().getExtensions();

                for (int i = 0; i < extVec.size(); i++) {
                    HelmaExtension ext = (HelmaExtension) extVec.get(i);

                    try {
                        HashMap tmpGlobals = ext.initScripting(app, this);

                        if (tmpGlobals != null) {
                            extensionGlobals.putAll(tmpGlobals);
                        }
                    } catch (ConfigurationException e) {
                        app.logError("Couldn't initialize extension " + ext.getName(), e);
                    }
                }
            }

        } catch (Exception e) {
            app.logError("Cannot initialize interpreter", e);
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            core.contextFactory.exit ();
        }
    }

    /**
     * Return the RhinoEngine associated with the current thread, or null.
     * @return the RhinoEngine assocated with the current thread
     */
    public static RhinoEngine getRhinoEngine() {
        return (RhinoEngine) engines.get();
    }

    /**
     * Initialize the RhinoCore instance for this engine and application.
     * @param app the application we belong to
     */
    private synchronized void initRhinoCore(Application app) {
        synchronized (coreMap) {
            WeakReference ref = (WeakReference) coreMap.get(app);
            if (ref != null) {
                core = (RhinoCore) ref.get();
            }

            if (core == null) {
                core = new RhinoCore(app);
                core.initialize();
                coreMap.put(app, new WeakReference(core));
            }
        }
    }

    /**
     *  This method is called before an execution context is entered to let the
     *  engine know it should update its prototype information.
     */
    public synchronized void updatePrototypes() throws IOException {
        // remember the current thread as our thread - we do this here so
        // the thread is already set when the RequestEvaluator calls
        // Application.getDataRoot(), which may result in a function invocation
        // (chicken and egg problem, kind of)
        thread = Thread.currentThread();
        global = new GlobalObject(core, app, true);
        context = core.contextFactory.enter();

        if (core.hasTracer) {
            context.setDebugger(new Tracer(getResponse()), null);
        }

        // register the engine with the current thread
        engines.set(this);
        // update prototypes
        core.updatePrototypes();
    }

    /**
     *  This method is called when an execution context for a request
     *  evaluation is entered. The globals parameter contains the global values
     *  to be applied during this execution context.
     */
    public synchronized void enterContext(Map globals) throws ScriptingException {
        // remember the current thread as our thread
        thread = Thread.currentThread();

        // set globals on the global object
        // add globals from extensions
        globals.putAll(extensionGlobals);
        // loop through global vars and set them
        for (Iterator i = globals.keySet().iterator(); i.hasNext();) {
            String k = (String) i.next();
            Object v = globals.get(k);
            Scriptable scriptable;

            // create a special wrapper for the path object.
            // other objects are wrapped in the default way.
            if (v == null) {
                continue;
            } else if (v instanceof RequestPath) {
                scriptable = new PathWrapper((RequestPath) v, core);
                scriptable.setPrototype(core.pathProto);
            } else {
                scriptable = Context.toObject(v, global);
            }

            global.put(k, global, scriptable);
        }
    }

    /**
     *   This method is called to let the scripting engine know that the current
     *   execution context has terminated.
     */
    public synchronized void exitContext() {
        // unregister the engine threadlocal
        engines.set(null);
        core.contextFactory.exit();
        thread = null;
        global = null;
    }

    /**
     * Invoke a function on some object, using the given arguments and global vars.
     * XML-RPC calls require special input and output parameter conversion.
     *
     * @param thisObject the object to invoke the function on, or null for
     *                   global functions
     * @param functionName the name of the function to be invoked
     * @param args array of argument objects
     * @param argsWrapMode indicated the way to process the arguments. Must be
     *                   one of <code>ARGS_WRAP_NONE</code>,
     *                          <code>ARGS_WRAP_DEFAULT</code>,
     *                          <code>ARGS_WRAP_XMLRPC</code>
     * @param resolve indicates whether functionName may contain an object path
     *                   or just the plain function name
     * @return the return value of the function
     * @throws ScriptingException to indicate something went wrong
     *                   with the invocation
     */
    public Object invoke(Object thisObject, String functionName, Object[] args,
                         int argsWrapMode, boolean resolve) throws ScriptingException {
        try {
            Scriptable obj = thisObject == null ? global : Context.toObject(thisObject, global);
            // if function name should be resolved interpret it as member expression,
            // otherwise replace dots with underscores.
            if (resolve) {
                if (functionName.indexOf('.') > 0) {
                    StringTokenizer st = new StringTokenizer(functionName, ".");
                    for (int i=0; i<st.countTokens()-1; i++) {
                        String propName = st.nextToken();
                        Object propValue = ScriptableObject.getProperty(obj, propName);
                        if (propValue instanceof Scriptable) {
                            obj = (Scriptable) propValue;
                        } else {
                            throw new RuntimeException("Can't resolve function name " +
                                    functionName + " in " + thisObject);
                        }
                    }
                    functionName = st.nextToken();
                }
            } else {
                functionName = functionName.replace('.', '_');
            }
            Object f = ScriptableObject.getProperty(obj, functionName);

            if ((f == ScriptableObject.NOT_FOUND) || !(f instanceof Function)) {
                return null;
            }

            for (int i = 0; i < args.length; i++) {
                switch (argsWrapMode) {
                    case ARGS_WRAP_DEFAULT:
                        // convert java objects to JavaScript
                        if (args[i] != null) {
                            args[i] = Context.javaToJS(args[i], global);
                        }
                        break;
                    case ARGS_WRAP_XMLRPC:
                        // XML-RPC requires special argument conversion
                        args[i] = core.processXmlRpcArgument(args[i]);
                        break;
                }
            }

            // use Context.call() in order to set the context's factory
            Object retval = Context.call(core.contextFactory, (Function)f, global, obj, args);

            if (retval instanceof Wrapper) {
                retval = ((Wrapper) retval).unwrap();
            }

            if ((retval == null) || (retval == Undefined.instance)) {
                return null;
            } else if (argsWrapMode == ARGS_WRAP_XMLRPC) {
                return core.processXmlRpcResponse (retval);
            } else {
                return retval;
            }
        } catch (RedirectException redirect) {
            throw redirect;
        } catch (TimeoutException timeout) {
            throw timeout;
        } catch (ConcurrencyException concur) {
            throw concur;
        } catch (Exception x) {
            // has the request timed out? If so, throw TimeoutException
            if (thread != Thread.currentThread()) {
                throw new TimeoutException();
            }

            if (x instanceof WrappedException) {
                // wrapped java excepiton
                Throwable wrapped = ((WrappedException) x).getWrappedException();
                // rethrow if this is a wrapped concurrency or redirect exception
                if (wrapped instanceof ConcurrencyException) {
                    throw (ConcurrencyException) wrapped;
                } else if (wrapped instanceof RedirectException) {
                    throw (RedirectException) wrapped;
                }
            }
            // create and throw a ScriptingException with the right message
            String msg = x.getMessage();
            throw new ScriptingException(msg, x);
        }
    }

    /**
     *  Let the evaluator know that the current evaluation has been
     *  aborted.
     */
    public  void abort() {
        // current request has been aborted.
        Thread t = thread;
        // set thread to null
        thread = null;
        if (t != null && t.isAlive()) {
            t.interrupt();
            try {
                t.join(1000);
            } catch (InterruptedException ir) {
                // interrupted by other thread
            }
        }
    }

    /**
     * Check if an object has a function property (public method if it
     * is a java object) with that name.
     */
    public boolean hasFunction(Object obj, String fname) {
        // Convert '.' to '_' in function name
        fname = fname.replace('.', '_');
        // Treat HopObjects separately - otherwise we risk to fetch database
        // references/child objects just to check for function properties.
        if (obj instanceof INode) {
            String protoname = ((INode) obj).getPrototype();
            return core.hasFunction(protoname, fname);
        }

        Scriptable op = obj == null ? global : Context.toObject(obj, global);

        Object func = ScriptableObject.getProperty(op, fname);

        return func instanceof Function;
    }

    /**
     * Check if an object has a defined property (public field if it
     * is a java object) with that name.
     */
    public Object get(Object obj, String propname) {
        if ((obj == null) || (propname == null)) {
            return null;
        }

        String prototypeName = app.getPrototypeName(obj);

        if ("user".equalsIgnoreCase(prototypeName) &&
                "password".equalsIgnoreCase(propname)) {
            return "[macro access to password property not allowed]";
        }

        // if this is a HopObject, check if the property is defined
        // in the type.properties db-mapping.
        if (obj instanceof INode) {
            DbMapping dbm = app.getDbMapping(prototypeName);

            if (dbm != null) {
                Relation rel = dbm.propertyToRelation(propname);

                if ((rel == null) || !rel.isPrimitive()) {
                    return "[property \"" + propname + "\" is not defined for " +
                           prototypeName + "]";
                }
            }
        }

        Scriptable so = Context.toObject(obj, global);

        try {
            Object prop = so.get(propname, so);

            if ((prop == null) || (prop == Undefined.instance)
	                       || (prop == ScriptableObject.NOT_FOUND)) {
                return null;
            } else if (prop instanceof Wrapper) {
                return ((Wrapper) prop).unwrap();
            } else {
                // not all Rhino types convert to a string as expected
                // when calling toString() - try to do better by using
                // Rhino's ScriptRuntime.toString(). Note that this
                // assumes that people always use this method to get
                // a string representation of the object - which is
                // currently the case since it's only used in Skin rendering.
                try {
                    return ScriptRuntime.toString(prop);
                } catch (Exception x) {
                    // just return original property object
                }
                return prop;
            }
        } catch (Exception esx) {
            return null;
        }
    }

    /**
     * Get an introspector to this engine.
     */
    public DocApplication getDoc() {
        if (doc == null) {
            try {
                doc = new DocApplication(app);
                doc.readApplication();
            } catch (IOException x) {
                throw new RuntimeException(x.toString(), x);
            }
        }
        return doc;
    }

    /**
     * Provide object serialization for this engine's scripted objects. If no special
     * provisions are required, this method should just wrap the stream with an
     * ObjectOutputStream and write the object.
     *
     * @param obj the object to serialize
     * @param out the stream to write to
     * @throws java.io.IOException
     */
    public void serialize(Object obj, OutputStream out) throws IOException {
        core.contextFactory.enter();
        try {
            // use a special ScriptableOutputStream that unwraps Wrappers
            ScriptableOutputStream sout = new ScriptableOutputStream(out, core.global) {
                protected Object replaceObject(Object obj) throws IOException {
                    if (obj instanceof Wrapper)
                        obj = ((Wrapper) obj).unwrap();
                    return super.replaceObject(obj);
                }
            };
            sout.writeObject(obj);
            sout.flush();
        } finally {
            core.contextFactory.exit();
        }
    }

    /**
     * Provide object deserialization for this engine's scripted objects. If no special
     * provisions are required, this method should just wrap the stream with an
     * ObjectIntputStream and read the object.
     *
     * @param in the stream to read from
     * @return the deserialized object
     * @throws java.io.IOException
     */
    public Object deserialize(InputStream in) throws IOException, ClassNotFoundException {
        core.contextFactory.enter();
        try {
            ObjectInputStream sin = new ScriptableInputStream(in, core.global);
            return sin.readObject();
        } finally {
            core.contextFactory.exit();
        }
    }

    /**
     * Add a code resource to a given prototype by immediately compiling and evaluating it.
     *
     * @param typename the type this resource belongs to
     * @param resource a code resource
     */
    public void injectCodeResource(String typename, Resource resource) {
        core.injectCodeResource(typename, resource);
    }

    /**
     * Return the application we're running in
     */
    public Application getApplication() {
        return app;
    }

    /**
     *  Return the RequestEvaluator owning and driving this FESI evaluator.
     */
    public RequestEvaluator getRequestEvaluator() {
        return reval;
    }

    /**
     *  Return the Response object of the current evaluation context.
     *  Proxy method to RequestEvaluator.
     */
    public ResponseTrans getResponse() {
        return reval.getResponse();
    }

    /**
     *  Return the Request object of the current evaluation context.
     *  Proxy method to RequestEvaluator.
     */
    public RequestTrans getRequest() {
        return reval.getRequest();
    }

    /**
     *  Return the RhinoCore object for the application this engine belongs to.
     *
     * @return this engine's RhinoCore instance
     */
    public RhinoCore getCore() {
        return core;
    }

    /**
     * Try to get a skin from the parameter object.
     */
    public Skin toSkin(Object skinobj, String protoName) throws IOException {
        if (skinobj instanceof Wrapper) {
            skinobj = ((Wrapper) skinobj).unwrap();
        }

        if (skinobj instanceof Skin) {
            return (Skin) skinobj;
        } else {
            return getSkin(protoName, skinobj.toString());
        }
    }

    /**
     *  Get a skin for the given prototype and skin name. This method considers the
     *  skinpath set in the current response object and does per-response skin
     *  caching.
     */
    public Skin getSkin(String protoName, String skinName) throws IOException {
        SkinKey key = new SkinKey(protoName, skinName);
        ResponseTrans res = getResponse();

        Skin skin = res.getCachedSkin(key);

        if (skin == null) {
            // retrieve res.skinpath, an array of objects that tell us where to look for skins
            // (strings for directory names and INodes for internal, db-stored skinsets)
            Object[] skinpath = res.getSkinpath();
            RhinoCore.unwrapSkinpath(skinpath);
            skin = app.getSkin(protoName, skinName, skinpath);
            res.cacheSkin(key, skin);
        }
        return skin;
    }

}
