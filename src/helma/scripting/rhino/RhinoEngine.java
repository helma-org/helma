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
import helma.util.CacheMap;
import helma.util.Updatable;
import org.mozilla.javascript.*;

import java.io.*;
import java.util.*;

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

    // the global object
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

        try {
            global = new GlobalObject(core, app); // context.newObject(core.global);
            global.setPrototype(core.global);
            global.setParentScope(null);

            // context.putThreadLocal ("reval", reval);
            // context.putThreadLocal ("engine", this);
            extensionGlobals = new HashMap();

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
            core = (RhinoCore) coreMap.get(app);
        }

        if (core == null) {
            core = new RhinoCore(app);
            coreMap.put(app, core);
        }

        return core;
    }

    /**
     *  This method is called before an execution context is entered to let the
     *  engine know it should update its prototype information.
     */
    public void updatePrototypes() {
        context = Context.enter();
        context.setCompileFunctionsWithDynamicScope(true);
        context.setWrapFactory(core.wrapper);

        boolean trace = "true".equals(app.getProperty("rhino.trace"));

        if (trace) {
            context.setDebugger(new Tracer(getResponse()), null);
        }

        int optLevel = 0;

        try {
            optLevel = Integer.parseInt(app.getProperty("rhino.optlevel"));
        } catch (Exception ignore) {
        }

        context.setOptimizationLevel(optLevel);
        core.updatePrototypes();
        context.putThreadLocal("reval", reval);
        context.putThreadLocal("engine", this);
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

                try {
                    // we do some extra work with the path object: first, we create a native
                    // JavaScript array, then we register objects by
                    // their prototype name, which we take from res.handlers.
                    if ("path".equals(k)) {
                        Scriptable arr = context.newObject(global, "Array");
                        List path = (List) v;
                        int length = path.size();
                        Scriptable[] wrapped = new Scriptable[length];

                        // Move through the path list and set the path array.
                        for (int j = 0; j < length; j++) {
                            Object pathElem = path.get(j);
                            Scriptable wrappedElement = Context.toObject(pathElem, global);

                            arr.put(j, arr, wrappedElement);
                            wrapped[j] = wrappedElement;
                        }

                        // register path elements with their prototypes on the path array
                        ResponseTrans res = getResponse();
                        Map handlers = res.getMacroHandlers();

                        if (handlers != null) {
                            for (Iterator h = handlers.entrySet().iterator(); h.hasNext(); ) {
                                Map.Entry entry = (Map.Entry) h.next();
                                int idx = path.indexOf(entry.getValue());
                                arr.put(entry.getKey().toString(), arr, wrapped[idx]);
                            }
                        }

                        v = arr;
                    }

                    scriptable = Context.toObject(v, global);
                    global.put(k, global, scriptable);
                } catch (Exception x) {
                    app.logEvent("Error setting global variable " + k + ": " + x);
                }
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
        Context.exit();
        thread = null;

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
                         boolean xmlrpc) throws ScriptingException {
        Scriptable eso = null;

        if (thisObject == null) {
            eso = global;
        } else {
            eso = Context.toObject(thisObject, global);
        }
        try {
            for (int i = 0; i < args.length; i++) {
                // XML-RPC requires special argument conversion
                if (xmlrpc) {
                    args[i] = core.processXmlRpcArgument (args[i]);
                } else if (args[i] != null) {
                    args[i] = Context.toObject(args[i], global);
                }
            }

            Object f = ScriptableObject.getProperty(eso, functionName.replace('.', '_'));

            if ((f == ScriptableObject.NOT_FOUND) || !(f instanceof Function)) {
                return null;
            }

            Object retval = ((Function) f).call(context, global, eso, args);

            if (xmlrpc) {
                return core.processXmlRpcResponse (retval);
            } else if ((retval == null) || (retval == Undefined.instance)) {
                return null;
            } else if (retval instanceof NativeJavaObject) {
                return ((NativeJavaObject) retval).unwrap();
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
            // check if this is a redirect exception, which has been converted by fesi
            // into an EcmaScript exception, which is why we can't explicitly catch it
            if (reval.res.getRedirect() != null) {
                throw new RedirectException(reval.res.getRedirect());
            }

            // do the same for not-modified responses
            if (reval.res.getNotModified()) {
                throw new RedirectException(null);
            }

            // has the request timed out? If so, throw TimeoutException
            if (thread != Thread.currentThread())
                throw new TimeoutException ();
            // create and throw a ScriptingException with the right message
            String msg;
            if (x instanceof JavaScriptException) {
                msg = ((JavaScriptException) x).getValue().toString();
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
        // System.err.println ("HAS_FUNC: "+fname);
        return core.hasFunction(app.getPrototypeName(obj), fname.replace('.', '_'));
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

            if ((prop == null) || (prop == Undefined.instance)) {
                return null;
            } else if (prop instanceof NativeJavaObject) {
                return ((NativeJavaObject) prop).unwrap();
            } else {
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
            doc = new DocApplication(app.getName(), app.getAppDir().toString());
            doc.readApplication();
        }
        return doc;
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
    public Skin getSkin(String protoName, String skinName) {
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
