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

import helma.extensions.ConfigurationException;
import helma.extensions.HelmaExtension;
import helma.framework.*;
import helma.framework.core.*;
import helma.main.Server;
import helma.objectmodel.*;
import helma.objectmodel.db.DbMapping;
import helma.objectmodel.db.Relation;
import helma.scripting.*;
import helma.scripting.fesi.extensions.*;
import helma.util.CacheMap;
import helma.util.Updatable;
import org.mozilla.javascript.*;
import java.io.*;
import java.util.*;

/**
 * This is the implementation of ScriptingEnvironment for the Mozilla Rhino EcmaScript interpreter.
 */
public final class RhinoEngine implements ScriptingEngine {
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
            global = context.newObject(core.global);
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
            // Context.exit ();
        }
    }

    static synchronized RhinoCore getRhinoCore(Application app) {
        RhinoCore core = null;

        if (coreMap == null) {
            coreMap = new HashMap();
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
        context = Context.enter(context);
        context.setCompileFunctionsWithDynamicScope(true);
        context.setWrapHandler(core);

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

        if ((globals != null) && (globals != lastGlobals)) {
            // loop through global vars and set them
            for (Iterator i = globals.keySet().iterator(); i.hasNext();) {
                String k = (String) i.next();
                Object v = globals.get(k);
                Scriptable scriptable = null;

                try {
                    // we do a lot of extra work to make access to global variables
                    // comfortable to EcmaScript coders, i.e. we use a lot of custom wrappers
                    // that expose properties and functions in a special way instead of just going
                    // with the standard java object wrappers.
                    if ("path".equals(k)) {
                        Scriptable arr = context.newObject(global, "Array");
                        List path = (List) v;

                        // register path elements with their prototype
                        for (int j = 0; j < path.size(); j++) {
                            Object pathElem = path.get(j);
                            Scriptable wrappedElement = context.toObject(pathElem, global);

                            arr.put(j, arr, wrappedElement);

                            String protoname = app.getPrototypeName(pathElem);

                            if (protoname != null) {
                                arr.put(protoname, arr, wrappedElement);
                            }
                        }

                        v = arr;
                    }

                    /* if (v instanceof Map) {
                       sv = new ESMapWrapper (this, (Map) v);
                       } else if ("path".equals (k)) {
                           ArrayPrototype parr = new ArrayPrototype (evaluator.getArrayPrototype(), evaluator);
                           List path = (List) v;
                           // register path elements with their prototype
                           for (int j=0; j<path.size(); j++) {
                               Object pathElem = path.get (j);
                               ESValue wrappedElement = getElementWrapper (pathElem);
                               parr.putProperty (j, wrappedElement);
                               String protoname = app.getPrototypeName (pathElem);
                               if (protoname != null)
                                   parr.putHiddenProperty (protoname, wrappedElement);
                           }
                           sv = parr;
                       } else if ("req".equals (k)) {
                           sv = new ESBeanWrapper (new RequestBean ((RequestTrans) v), this);
                       } else if ("res".equals (k)) {
                           sv = new ESBeanWrapper (new ResponseBean ((ResponseTrans) v), this);
                       } else if ("session".equals (k)) {
                           sv = new ESBeanWrapper (new SessionBean ((Session)v), this);
                       } else if ("app".equals (k)) {
                           sv = new ESBeanWrapper (new ApplicationBean ((Application)v), this);
                       } else if (v instanceof ESValue) {
                           sv = (ESValue)v;
                       } else {
                           sv = ESLoader.normalizeValue (v, evaluator);
                       } */
                    scriptable = context.toObject(v, global);
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
        context.exit();
        context.removeThreadLocal("reval");
        context.removeThreadLocal("engine");

        // unset the thread filed in the FESI evaluator
        // evaluator.thread = null;
        // loop through previous globals and unset them, if necessary.

        /* if (lastGlobals != null) {
           for (Iterator i=lastGlobals.keySet().iterator(); i.hasNext(); ) {
               String g = (String) i.next ();
               try {
                   global.deleteProperty (g, g.hashCode());
               } catch (Exception x) {
                   System.err.println ("Error resetting global property: "+g);
               }
           }
           lastGlobals = null;
           } */
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
            eso = context.toObject(thisObject, global);
        }
        try {
            for (int i = 0; i < args.length; i++) {
                // XML-RPC requires special argument conversion
                // if (xmlrpc)
                //    esv[i] = processXmlRpcArgument (args[i], evaluator);
                // for java.util.Map objects, we use the special "tight" wrapper
                // that makes the Map look like a native object

                /* else if (args[i] instanceof Map)
                   esv[i] = new ESMapWrapper (this, (Map) args[i]);
                   else
                       esv[i] = ESLoader.normalizeValue (args[i], evaluator); */
                args[i] = context.toObject(args[i], global);
            }

            Object f = ScriptableObject.getProperty(eso, functionName);

            if ((f == ScriptableObject.NOT_FOUND) || !(f instanceof Function)) {
                return null;
            }

            Object retval = ((Function) f).call(context, global, eso, args);

            // if (xmlrpc)
            //     return processXmlRpcResponse (retval);
            if ((retval == null) || (retval == Undefined.instance)) {
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
            // if (evaluator.thread != Thread.currentThread())
            //     throw new TimeoutException ();
            // create and throw a ScriptingException with the right message
            String msg = x.toString();

            if (app.debug()) {
                System.err.println("Error in Script: " + msg);
                x.printStackTrace();
            }

            throw new ScriptingException(msg);
        }
    }

    /**
     *  Let the evaluator know that the current evaluation has been
     *  aborted. This is done by setting the thread ref in the evaluator
     * object to null.
     */
    public void abort() {
        // unset the thread filed in the FESI evaluator
        // evaluator.thread = null;
    }

    /**
     * Check if an object has a function property (public method if it
     * is a java object) with that name.
     */
    public boolean hasFunction(Object obj, String fname) {
        // System.err.println ("HAS_FUNC: "+fname);
        return core.hasFunction(app.getPrototypeName(obj), fname);
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

        Scriptable so = context.toObject(obj, global);

        try {
            Object prop = so.get(propname, so);

            if (prop != Undefined.instance) {
                return prop;
            }
        } catch (Exception esx) {
            // System.err.println ("Error in getProperty: "+esx);
            return null;
        }

        return null;
    }

    /**
     * Get an introspector to this engine. FIXME: not yet implemented for the rhino engine.
     */
    public IPathElement getIntrospector() {
        return null;
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
     *  Return the Response object of the current evaluation context. Proxy method to RequestEvaluator.
     */
    public ResponseTrans getResponse() {
        return reval.res;
    }

    /**
     *  Return the Request object of the current evaluation context. Proxy method to RequestEvaluator.
     */
    public RequestTrans getRequest() {
        return reval.req;
    }
}
