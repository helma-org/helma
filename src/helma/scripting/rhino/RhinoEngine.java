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
    static Map coreMap;

    // the application we're running in
    public Application app;

    // The Rhino context
    Context context;

    // the per-thread global object
    Scriptable global;

    // the request evaluator instance owning this fesi evaluator
    RequestEvaluator reval;
    RhinoCore core;

    // remember global variables from last invokation to be able to
    // do lazy cleanup
    Map lastGlobals = null;

    // the global vars set by extensions
    HashMap extensionGlobals;

    // the thread currently running this engine
    Thread thread;

    // the introspector that provides documentation for this application
    DocApplication doc = null;

    /**
     *  Zero argument constructor.
     */
    public RhinoEngine() {
    }

    /**
     * Init the scripting engine with an application and a request evaluator
     */
    public void init(Application app, RequestEvaluator reval) {
        this.app = app;
        this.reval = reval;
        core = getRhinoCore(app);
        context = Context.enter();
        context.setCompileFunctionsWithDynamicScope(true);
        context.setApplicationClassLoader(app.getClassLoader());

        try {
            global = new GlobalObject(core, app);
            global.setPrototype(core.global);
            global.setParentScope(null);

            // context.putThreadLocal ("reval", reval);
            // context.putThreadLocal ("engine", this);
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
                        app.logEvent("Couldn't initialize extension " + ext.getName() + ": " +
                                 e.getMessage());
                    }
                }
            }

            // context.removeThreadLocal ("reval");
            // context.removeThreadLocal ("engine");
        } catch (Exception e) {
            System.err.println("Cannot initialize interpreter");
            System.err.println("Error: " + e);
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        } finally {
            Context.exit ();
        }
    }

    static synchronized RhinoCore getRhinoCore(Application app) {
        RhinoCore core = null;

        if (coreMap == null) {
            coreMap = new WeakHashMap();
        } else {
            WeakReference ref = (WeakReference) coreMap.get(app);
            if (ref != null) {
                core = (RhinoCore) ref.get();
            }
        }

        if (core == null) {
            core = new RhinoCore(app);
            coreMap.put(app, new WeakReference(core));
        }

        return core;
    }

    /**
     *  This method is called before an execution context is entered to let the
     *  engine know it should update its prototype information.
     */
    public void updatePrototypes() throws IOException {
        context = Context.enter();
        context.setCompileFunctionsWithDynamicScope(true);
        context.setApplicationClassLoader(app.getClassLoader());
        context.setWrapFactory(core.wrapper);

        // if visual debugger is on let it know we're entering a context
        if (core.debugger != null) {
            core.initDebugger(context);
        }

        if ("true".equals(app.getProperty("rhino.trace"))) {
            context.setDebugger(new Tracer(getResponse()), null);
        }

        // Set default optimization level according to whether debugger is on
        int optLevel = core.debugger == null ? 0 : -1;

        try {
            optLevel = Integer.parseInt(app.getProperty("rhino.optlevel"));
        } catch (Exception ignore) {
        }

        context.setOptimizationLevel(optLevel);
        // register the per-thread scope with the dynamic scope
        // core.global.registerScope(global);
        context.putThreadLocal("threadscope", global);
        context.putThreadLocal("reval", reval);
        context.putThreadLocal("engine", this);
        // update prototypes
        core.updatePrototypes();
    }

    /**
     *  This method is called when an execution context for a request
     *  evaluation is entered. The globals parameter contains the global values
     *  to be applied during this execution context.
     */
    public void enterContext(HashMap globals) throws ScriptingException {
        // set the thread filed in the FESI evaluator
        // evaluator.thread = Thread.currentThread ();
        // set globals on the global object
        // context = Context.enter (context);
        globals.putAll(extensionGlobals);
        thread = Thread.currentThread();

        if ((globals != null) && (globals != lastGlobals)) {
            // loop through global vars and set them
            for (Iterator i = globals.keySet().iterator(); i.hasNext();) {
                String k = (String) i.next();
                Object v = globals.get(k);
                Scriptable scriptable = null;

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

        // remember the globals set on this evaluator
        lastGlobals = globals;
    }

    /**
     *   This method is called to let the scripting engine know that the current
     *   execution context has terminated.
     */
    public void exitContext() {
        context.removeThreadLocal("reval");
        context.removeThreadLocal("engine");
        context.removeThreadLocal("threadscope");
        Context.exit();
        // core.global.unregisterScope();
        thread = null;

        // if visual debugger is on let it know we're exiting a context
        if (core.debugger != null) {
            core.debugger.contextExited(context);
            core.debugger.contextReleased(context);
        }

        // loop through previous globals and unset them, if necessary.
        if (lastGlobals != null) {
           for (Iterator i=lastGlobals.keySet().iterator(); i.hasNext(); ) {
               String g = (String) i.next ();
               try {
                   global.delete (g);
               } catch (Exception x) {
                   System.err.println ("Error resetting global property: "+g);
               }
           }
           lastGlobals = null;
           }
    }

    /**
     * Invoke a function on some object, using the given arguments and global vars.
     */
    public Object invoke(Object thisObject, String functionName, Object[] args,
                         int argsWrapMode) throws ScriptingException {
        Scriptable eso = null;

        if (thisObject == null) {
            eso = global;
        } else {
            eso = Context.toObject(thisObject, global);
        }
        try {
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

            Object f = ScriptableObject.getProperty(eso, functionName.replace('.', '_'));

            if ((f == ScriptableObject.NOT_FOUND) || !(f instanceof Function)) {
                return null;
            }

            Object retval = ((Function) f).call(context, global, eso, args);

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
            if (thread != Thread.currentThread())
                throw new TimeoutException ();

            // create and throw a ScriptingException with the right message
            String msg;
            if (x instanceof JavaScriptException) {
                msg = ((JavaScriptException) x).getValue().toString();
            } else if (x instanceof WrappedException) {
                Throwable wrapped = ((WrappedException) x).getWrappedException();
                // if this is a wrapped concurrencyException, rethrow it.
                if (wrapped instanceof ConcurrencyException) {
                    throw (ConcurrencyException) wrapped;
                }
                // also check if this is a wrapped redirect exception
                if (wrapped instanceof RedirectException) {
                    throw (RedirectException) wrapped;
                }
                msg = wrapped.toString();
            } else {
                msg = x.toString();
            }

            if (app.debug()) {
                System.err.println("Error in Script: " + msg);
                x.printStackTrace();
            }

            throw new ScriptingException(msg);
        }
    }

    /**
     *  Let the evaluator know that the current evaluation has been
     *  aborted.
     */
    public void abort() {
        // current request has been aborted.
        Thread t = thread;
        if (t != null && t.isAlive()) {
            t.interrupt();
            try {
                Thread.sleep(5000);
                if (t.isAlive()) {
                    // thread is still running, gotta stop it.
                    t.stop();
                }
            } catch (InterruptedException i) {
            }
        }
    }

    /**
     * Check if an object has a function property (public method if it
     * is a java object) with that name.
     */
    public boolean hasFunction(Object obj, String fname) {
        // Treat HopObjects separately - otherwise we risk to fetch database
        // references/child objects just to check for function properties.
        if (obj instanceof INode) {
            String protoname = ((INode) obj).getPrototype();
            return core.hasFunction(protoname, fname.replace('.', '_'));
        }

        Scriptable op = obj == null ? global : Context.toObject(obj, global);

        Object func = ScriptableObject.getProperty(op, fname.replace('.', '_'));

        if (func != null && func != Undefined.instance && func instanceof Function) {
            return true;
        }

        return false;
    }

    /**
     * Check if an object has a defined property (public field if it
     * is a java object) with that name.
     */
    public Object get(Object obj, String propname) {
        // System.err.println ("GET: "+propname);
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
            // System.err.println ("Error in getProperty: "+esx);
            return null;
        }
    }

    /**
     * Get an introspector to this engine.
     */
    public IPathElement getIntrospector() {
        if (doc == null) {
            try {
                doc = new DocApplication(app);
                doc.readApplication();
            } catch (IOException x) {
                throw new RuntimeException(x.toString());
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
        Context.enter();
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
            Context.exit();
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
        Context.enter();
        try {
            ObjectInputStream sin = new ScriptableInputStream(in, core.global);
            return sin.readObject();
        } finally {
            Context.exit();
        }
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
        return reval.res;
    }

    /**
     *  Return the Request object of the current evaluation context.
     *  Proxy method to RequestEvaluator.
     */
    public RequestTrans getRequest() {
        return reval.req;
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
     *  Get a skin for the given prototype and skin name. This method considers the
     *  skinpath set in the current response object and does per-response skin
     *  caching.
     */
    public Skin getSkin(String protoName, String skinName) throws IOException {
        SkinKey key = new SkinKey(protoName, skinName);

        Skin skin = reval.res.getCachedSkin(key);

        if (skin == null) {
            // retrieve res.skinpath, an array of objects that tell us where to look for skins
            // (strings for directory names and INodes for internal, db-stored skinsets)
            Object[] skinpath = reval.res.getSkinpath();
            RhinoCore.unwrapSkinpath(skinpath);
            skin = app.getSkin(protoName, skinName, skinpath);
            reval.res.cacheSkin(key, skin);
        }
        return skin;
    }

}
