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

import helma.framework.*;
import helma.framework.core.*;
import helma.main.Server;
import helma.objectmodel.*;
import helma.objectmodel.db.DbMapping;
import helma.objectmodel.db.Relation;
import helma.scripting.*;
import helma.util.CacheMap;
import helma.util.Updatable;
import org.mozilla.javascript.*;
import java.io.*;
import java.util.*;

/**
 * This is the implementation of ScriptingEnvironment for the Mozilla Rhino EcmaScript interpreter.
 */
public final class RhinoCore implements WrapHandler {
    // the application we're running in
    public final Application app;

    // the global object
    Scriptable global;

    // caching table for JavaScript object wrappers
    CacheMap wrappercache;

    // table containing JavaScript prototypes
    Hashtable prototypes;
    long lastUpdate = 0;

    /**
     *  Create a Rhino evaluator for the given application and request evaluator.
     */
    public RhinoCore(Application app) {
        this.app = app;
        wrappercache = new CacheMap(500, .75f);
        prototypes = new Hashtable();

        Context context = Context.enter();

        context.setCompileFunctionsWithDynamicScope(true);
        context.setWrapHandler(this);

        int optLevel = 0;

        try {
            optLevel = Integer.parseInt(app.getProperty("rhino.optlevel"));
        } catch (Exception ignore) {
        }

        // System.err.println("Setting Rhino optlevel to " + optLevel);
        context.setOptimizationLevel(optLevel);

        try {
            GlobalObject g = new GlobalObject(this, app, context);

            global = context.initStandardObjects(g);
            ScriptableObject.defineClass(global, HopObject.class);
            ScriptableObject.defineClass(global, FileObject.class);
            putPrototype("hopobject",
                         ScriptableObject.getClassPrototype(global, "HopObject"));
            putPrototype("global", global);
            initialize();
        } catch (Exception e) {
            System.err.println("Cannot initialize interpreter");
            System.err.println("Error: " + e);
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        } finally {
            context.exit();
        }
    }

    /**
     *  Initialize the evaluator, making sure the minimum type information
     *  necessary to bootstrap the rest is parsed.
     */
    private synchronized void initialize() {
        Collection protos = app.getPrototypes();

        for (Iterator i = protos.iterator(); i.hasNext();) {
            Prototype proto = (Prototype) i.next();

            initPrototype(proto);
        }

        // always fully initialize global prototype, because
        // we always need it and there's no chance to trigger
        // creation on demand.
        getPrototype("global");
    }

    /**
     *   Initialize a prototype without fully parsing its script files.
     */
    synchronized void initPrototype(Prototype prototype) {
        // System.err.println ("FESI INIT PROTO "+prototype);
        Scriptable op = null;
        String name = prototype.getName();

        // get the prototype's prototype if possible and necessary
        Scriptable opp = null;
        Prototype parent = prototype.getParentPrototype();

        if (parent != null) {
            // see if parent prototype is already registered. if not, register it
            opp = getRawPrototype(parent.getName());

            if (opp == null) {
                initPrototype(parent);
                opp = getRawPrototype(parent.getName());
            }
        }

        if (!"global".equalsIgnoreCase(name) && !"hopobject".equalsIgnoreCase(name) &&
                (opp == null)) {
            if (app.isJavaPrototype(name)) {
                opp = getRawPrototype("__javaobject__");
            } else {
                opp = getRawPrototype("hopobject");
            }
        }

        // if prototype doesn't exist (i.e. is a standard prototype built by HopExtension), create it.
        op = getRawPrototype(name);

        if (op == null) {
            try {
                Context context = Context.getCurrentContext();

                op = new HopObject(name); // context.newObject (global /*, "HopObject" */);
                op.setPrototype(opp);
                op.setParentScope(global);
                op.put("prototypename", op, name);
            } catch (Exception ignore) {
                System.err.println("Error creating prototype: " + ignore);
                ignore.printStackTrace();
            }

            putPrototype(name, op);
        } else {
            // set parent prototype just in case it has been changed
            op.setPrototype(opp);
        }

        // Register a constructor for all types except global.
        // This will first create a new prototyped hopobject and then calls
        // the actual (scripted) constructor on it.
        if (!"global".equalsIgnoreCase(name) && !"root".equalsIgnoreCase(name)) {
            try {
                FunctionObject fp = new FunctionObject(name, HopObject.hopObjCtor, global);

                fp.addAsConstructor(global, op);
            } catch (Exception ignore) {
                System.err.println("Error adding ctor for " + name + ": " + ignore);
                ignore.printStackTrace();
            }
        }
    }

    /**
     *   Set up a prototype, parsing and compiling all its script files.
     */
    synchronized void evaluatePrototype(Prototype prototype) {
        // System.err.println ("FESI EVALUATE PROTO "+prototype+" FOR "+this);
        Scriptable op = null;

        // get the prototype's prototype if possible and necessary
        Scriptable opp = null;
        Prototype parent = prototype.getParentPrototype();

        if (parent != null) {
            // see if parent prototype is already registered. if not, register it
            opp = getPrototype(parent.getName());

            if (opp == null) {
                evaluatePrototype(parent);
                opp = getPrototype(parent.getName());
            }
        }

        String name = prototype.getName();

        if (!"global".equalsIgnoreCase(name) && !"hopobject".equalsIgnoreCase(name) &&
                (opp == null)) {
            if (app.isJavaPrototype(name)) {
                opp = getPrototype("__javaobject__");
            } else {
                opp = getPrototype("hopobject");
            }
        }

        // if prototype doesn't exist (i.e. is a standard prototype built by HopExtension), create it.
        op = getPrototype(name);

        if (op == null) {
            try {
                Context context = Context.getCurrentContext();

                op = new HopObject(name); // context.newObject (global /*, "HopObject" */);
                op.setPrototype(opp);
                op.setParentScope(global);
                op.put("prototypename", op, name);
            } catch (Exception ignore) {
                System.err.println("Error creating prototype: " + ignore);
                ignore.printStackTrace();
            }

            putPrototype(name, op);
        } else {
            // reset prototype to original state
            resetPrototype(op);

            // set parent prototype just in case it has been changed
            op.setPrototype(opp);
        }

        // Register a constructor for all types except global.
        // This will first create a new prototyped hopobject and then calls
        // the actual (scripted) constructor on it.
        if (!"global".equalsIgnoreCase(name) && !"root".equalsIgnoreCase(name)) {
            try {
                FunctionObject fp = new FunctionObject(name, HopObject.hopObjCtor, global);

                fp.addAsConstructor(global, op);
            } catch (Exception ignore) {
                System.err.println("Error adding ctor for " + name + ": " + ignore);
                ignore.printStackTrace();
            }
        }

        for (Iterator it = prototype.getZippedCode().values().iterator(); it.hasNext();) {
            Object code = it.next();

            evaluate(prototype, code);
        }

        for (Iterator it = prototype.getCode().values().iterator(); it.hasNext();) {
            Object code = it.next();

            evaluate(prototype, code);
        }
    }

    /**
     *  Return an object prototype to its initial state, removing all application specific
     *  functions.
     */
    synchronized void resetPrototype(Scriptable op) {
        Object[] ids = op.getIds();

        for (int i = 0; i < ids.length; i++) {
            /* String prop = en.nextElement ().toString ();
               try {
                   ESValue esv = op.getProperty (prop, prop.hashCode ());
                   if (esv instanceof ConstructedFunctionObject || esv instanceof FesiActionAdapter.ThrowException)
                       op.deleteProperty (prop, prop.hashCode());
               } catch (Exception x) {} */
        }
    }

    /**
     *  This method is called before an execution context is entered to let the
     *  engine know it should update its prototype information.
     */
    public synchronized void updatePrototypes() {
        if ((System.currentTimeMillis() - lastUpdate) < 1000L) {
            return;
        }

        Collection protos = app.getPrototypes();

        for (Iterator i = protos.iterator(); i.hasNext();) {
            Prototype proto = (Prototype) i.next();
            TypeInfo info = (TypeInfo) prototypes.get(proto.getName());

            if (info == null) {
                // a prototype we don't know anything about yet. Init local update info.
                initPrototype(proto);
                info = (TypeInfo) prototypes.get(proto.getName());
            }

            // only update prototype if it has already been initialized.
            // otherwise, this will be done on demand
            // System.err.println ("CHECKING PROTO "+proto+": "+info);
            if (info.lastUpdate > 0) {
                Prototype p = app.typemgr.getPrototype(info.protoName);

                if (p != null) {
                    // System.err.println ("UPDATING PROTO: "+p);
                    app.typemgr.updatePrototype(p);

                    if (p.getLastUpdate() > info.lastUpdate) {
                        evaluatePrototype(p);
                        info.lastUpdate = p.getLastUpdate();
                    }
                }
            }
        }

        lastUpdate = System.currentTimeMillis();
    }

    /**
     * Check if an object has a function property (public method if it
     * is a java object) with that name.
     */
    public boolean hasFunction(String protoname, String fname) {
        // System.err.println ("HAS_FUNC: "+fname);
        try {
            Scriptable op = getPrototype(protoname);

            // if this is an untyped object return false
            if (op == null) {
                return false;
            }

            Object func = ScriptableObject.getProperty(op, fname);

            if ((func != null) && func instanceof Function) {
                return true;
            }
        } catch (Exception esx) {
            // System.err.println ("Error in hasFunction: "+esx);
            return false;
        }

        return false;
    }

    /**
     *  Convert an input argument from Java to the scripting runtime
     *  representation.
     */

    /* public static ESValue processXmlRpcArgument (Object what, Evaluator evaluator) throws Exception {
       if (what == null)
          return ESNull.theNull;
       if (what instanceof Vector) {
           Vector v = (Vector) what;
           ArrayPrototype retval = new ArrayPrototype (evaluator.getArrayPrototype (), evaluator);
           int l = v.size ();
           for (int i=0; i<l; i++)
               retval.putProperty (i, processXmlRpcArgument (v.elementAt (i), evaluator));
           return retval;
       }
       if (what instanceof Hashtable) {
           Hashtable t = (Hashtable) what;
           ESObject retval = new ObjectPrototype (evaluator.getObjectPrototype (), evaluator);
           for (Enumeration e=t.keys(); e.hasMoreElements(); ) {
               String next = (String) e.nextElement ();
               retval.putProperty (next, processXmlRpcArgument (t.get (next), evaluator), next.hashCode ());
           }
           return retval;
       }
       if (what instanceof String)
          return new ESString (what.toString ());
       if (what instanceof Number)
          return new ESNumber (new Double (what.toString ()).doubleValue ());
       if (what instanceof Boolean)
          return ESBoolean.makeBoolean (((Boolean) what).booleanValue ());
       if (what instanceof Date)
          return new DatePrototype (evaluator, (Date) what);
       return ESLoader.normalizeValue (what, evaluator);
       } */

    /**
     * convert a JavaScript Object object to a generic Java object stucture.
     */

    /* public static Object processXmlRpcResponse (ESValue what) throws EcmaScriptException {
       if (what == null || what instanceof ESNull)
           return null;
       if (what instanceof ArrayPrototype) {
           ArrayPrototype a = (ArrayPrototype) what;
           int l = a.size ();
           Vector v = new Vector ();
           for (int i=0; i<l; i++) {
               Object nj = processXmlRpcResponse (a.getProperty (i));
               v.addElement (nj);
           }
           return v;
       }
       if (what instanceof ObjectPrototype) {
           ObjectPrototype o = (ObjectPrototype) what;
           Hashtable t = new Hashtable ();
           for (Enumeration e=o.getProperties (); e.hasMoreElements (); ) {
               String next = (String) e.nextElement ();
               // We don't do deep serialization of HopObjects to avoid
               // that the whole web site structure is sucked out with one
               // object. Instead we return some kind of "proxy" objects
               // that only contain the prototype and id of the HopObject property.
               Object nj = null;
               ESValue esv = o.getProperty (next, next.hashCode ());
               if (esv instanceof ESNode) {
                   INode node = ((ESNode) esv).getNode ();
                   if (node != null) {
                       Hashtable nt = new Hashtable ();
                       nt.put ("id", node.getID());
                       if (node.getPrototype() != null)
                           nt.put ("prototype", node.getPrototype ());
                       nj = nt;
                   }
               } else
                   nj = processXmlRpcResponse (esv);
               if (nj != null)  // can't put null as value in hashtable
                   t.put (next, nj);
           }
           return t;
       }
       if (what instanceof ESUndefined || what instanceof ESNull)
           return null;
       Object jval = what.toJavaObject ();
       if (jval instanceof Byte || jval instanceof Short)
           jval = new Integer (jval.toString ());
       return jval;
       } */

    /**
     * Return the application we're running in
     */
    public Application getApplication() {
        return app;
    }

    /**
     * Get a raw prototype, i.e. in potentially unfinished state
     * without checking if it needs to be updated.
     */
    private Scriptable getRawPrototype(String protoName) {
        if (protoName == null) {
            return null;
        }

        TypeInfo info = (TypeInfo) prototypes.get(protoName);

        return (info == null) ? null : info.objectPrototype;
    }

    /**
     *  Get the object prototype for a prototype name and initialize/update it
     *  if necessary.
     */
    public Scriptable getPrototype(String protoName) {
        if (protoName == null) {
            return null;
        }

        TypeInfo info = (TypeInfo) prototypes.get(protoName);

        if ((info != null) && (info.lastUpdate == 0)) {
            Prototype p = app.typemgr.getPrototype(protoName);

            if (p != null) {
                app.typemgr.updatePrototype(p);

                if (p.getLastUpdate() > info.lastUpdate) {
                    info.lastUpdate = p.getLastUpdate();
                    evaluatePrototype(p);
                }

                // set info.lastUpdate to 1 if it is 0 so we know we 
                // have initialized this prototype already, even if 
                // it is empty (i.e. doesn't contain any scripts/skins/actoins
                if (info.lastUpdate == 0) {
                    info.lastUpdate = 1;
                }
            }
        }

        return (info == null) ? null : info.objectPrototype;
    }

    /**
     * Register an object prototype for a certain prototype name.
     */
    public void putPrototype(String protoName, Scriptable op) {
        if ((protoName != null) && (op != null)) {
            prototypes.put(protoName, new TypeInfo(op, protoName));
        }
    }

    /**
     *
     *
     * @param scope ...
     * @param obj ...
     * @param staticType ...
     *
     * @return ...
     */
    public Object wrap(Scriptable scope, Object obj, Class staticType) {
        if (obj instanceof INode) {
            return getNodeWrapper((INode) obj);
        }

        if (obj instanceof IPathElement) {
            return getElementWrapper(obj);
        }

        if (obj instanceof Map) {
            return new MapWrapper((Map) obj, this);
        }

        return null;
    }

    /**
     *  Get a Script wrapper for an object. In contrast to getElementWrapper, this is called for
     * any Java object, not just the ones in the request path which we know are scripted.
     * So what we do is check if the object belongs to a scripted class. If so, we call getElementWrapper()
     * with the object, otherwise we return a generic unscripted object wrapper.
     */
    /* public Scriptable getObjectWrapper(Object e) {
        if (app.getPrototypeName(e) != null) {
            return getElementWrapper(e);
        }
        / else if (e instanceof INode)
           return new ESNode ((INode) e, this); /
        else {
            return Context.getCurrentContext().toObject(e, global);
        }
    } */

    /**
     *  Get a Script wrapper for any given object. If the object implements the IPathElement
     *  interface, the getPrototype method will be used to retrieve the name of the prototype
     * to use. Otherwise, a Java-Class-to-Script-Prototype mapping is consulted.
     */
    public Scriptable getElementWrapper(Object e) {
        // Gotta find out the prototype name to use for this object...
        String prototypeName = app.getPrototypeName(e);

        Scriptable op = getPrototype(prototypeName);

        if (op == null) {
            op = getPrototype("hopobject");
        }

        return new JavaObject(global, e, op, this);
    }

    /**
     *  Get a script wrapper for an instance of helma.objectmodel.INode
     */
    public Scriptable getNodeWrapper(INode n) {
        // FIXME: should this return ESNull.theNull?
        if (n == null) {
            return null;
        }

        HopObject esn = (HopObject) wrappercache.get(n);

        if (esn == null) {
            try {
                String protoname = n.getPrototype();

                Scriptable op = null;

                // set the DbMapping of the node according to its prototype.
                // this *should* be done on the objectmodel level, but isn't currently
                // for embedded nodes since there's not enough type info at the objectmodel level
                // for those nodes.
                if ((protoname != null) && (protoname.length() > 0) &&
                        (n.getDbMapping() == null)) {
                    n.setDbMapping(app.getDbMapping(protoname));
                }

                op = getPrototype(protoname);

                // no prototype found for this node?
                if (op == null) {
                    op = getPrototype("hopobject");
                }

                esn = new HopObject();
                esn.init(this, n);
                esn.setPrototype(op);

                wrappercache.put(n, esn);

                // app.logEvent ("Wrapper for "+n+" created");
            } catch (Exception x) {
                System.err.println("Error creating node wrapper: " + x);
                throw new RuntimeException(x.toString());
            }
        }

        return esn;
    }

    /**
     *  Register a new Node wrapper with the wrapper cache. This is used by the
     * Node constructor.
     */
    public void putNodeWrapper(INode n, Scriptable esn) {
        wrappercache.put(n, esn);
    }

    private synchronized void evaluate(Prototype prototype, Object code) {
        if (code instanceof FunctionFile) {
            FunctionFile funcfile = (FunctionFile) code;
            File file = funcfile.getFile();

            if (file != null) {
                try {
                    FileReader fr = new FileReader(file);

                    updateEvaluator(prototype, fr, funcfile.getSourceName(), 1);
                } catch (IOException iox) {
                    app.logEvent("Error updating function file: " + iox);
                }
            } else {
                StringReader reader = new StringReader(funcfile.getContent());

                updateEvaluator(prototype, reader, funcfile.getSourceName(), 1);
            }
        } else if (code instanceof ActionFile) {
            ActionFile action = (ActionFile) code;
            RhinoActionAdapter fa = new RhinoActionAdapter(action);

            try {
                updateEvaluator(prototype, new StringReader(fa.function),
                                action.getSourceName(), 0);
            } catch (Exception esx) {
                app.logEvent("Error parsing " + action + ": " + esx);
            }
        }
    }

    private synchronized void updateEvaluator(Prototype prototype, Reader reader,
                                              String sourceName, int firstline) {
        // context = Context.enter(context);
        try {
            Scriptable op = getPrototype(prototype.getName());

            // get the current context
            Context cx = Context.getCurrentContext();

            // do the update, evaluating the file
            cx.evaluateReader(op, reader, sourceName, firstline, null);
        } catch (Throwable e) {
            app.logEvent("Error parsing function file " + sourceName + ": " + e);
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignore) {
                }
            }
        }
    }

    class TypeInfo {
        Scriptable objectPrototype;
        long lastUpdate = 0;
        String protoName;

        public TypeInfo(Scriptable op, String name) {
            objectPrototype = op;
            protoName = name;
        }

        public String toString() {
            return ("TypeInfo[" + protoName + "," + new Date(lastUpdate) + "]");
        }
    }
}
