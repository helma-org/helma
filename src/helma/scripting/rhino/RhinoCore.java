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

import helma.scripting.rhino.extensions.*;
import helma.framework.*;
import helma.framework.core.*;
import helma.main.Server;
import helma.objectmodel.*;
import helma.objectmodel.db.DbMapping;
import helma.objectmodel.db.Relation;
import helma.scripting.*;
import helma.util.CacheMap;
import helma.util.SystemMap;
import helma.util.SystemProperties;
import helma.util.Updatable;
import org.mozilla.javascript.*;
import java.io.*;
import java.text.*;
import java.util.*;

/**
 * This is the implementation of ScriptingEnvironment for the Mozilla Rhino EcmaScript interpreter.
 */
public final class RhinoCore {
    // the application we're running in
    public final Application app;

    // the global object
    Scriptable global;

    // caching table for JavaScript object wrappers
    WeakHashMap wrappercache;

    // table containing JavaScript prototypes
    Hashtable prototypes;
    long lastUpdate = 0;

    // the wrap factory
    Wrapper wrapper;

    /**
     *  Create a Rhino evaluator for the given application and request evaluator.
     */
    public RhinoCore(Application app) {
        this.app = app;
        // wrappercache = new CacheMap(500, .75f);
        wrappercache = new WeakHashMap();
        prototypes = new Hashtable();

        Context context = Context.enter();

        context.setCompileFunctionsWithDynamicScope(true);
        wrapper = new Wrapper();
        context.setWrapFactory(wrapper);

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
            ScriptableObject.defineClass(global, FtpObject.class);
            ImageObject.init(global);
            XmlRpcObject.init(global);
            MailObject.init(global, app.getProperties());
            putPrototype("hopobject",
                         ScriptableObject.getClassPrototype(global, "HopObject"));
            putPrototype("global", global);

            // add some convenience functions to string, date and number prototypes
            Scriptable stringProto = ScriptableObject.getClassPrototype(global, "String");
            stringProto.put("trim", stringProto, new StringTrim());

            Scriptable dateProto = ScriptableObject.getClassPrototype(global, "Date");
            dateProto.put("format", dateProto, new DateFormat());

            Scriptable numberProto = ScriptableObject.getClassPrototype(global, "Number");
            numberProto.put("format", numberProto, new NumberFormat());

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
                opp = null;
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
        if (!"global".equalsIgnoreCase(name) &&
                !"root".equalsIgnoreCase(name) &&
                !"hopobject".equalsIgnoreCase(name)) {
            try {
                installConstructor(name, op);
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
                opp = null;
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
     *  This is a version of org.mozilla.javascript.FunctionObject.addAsConstructor()
     *  that does not set the constructor property in the prototype. This is because
     *  we want our own scripted constructor function to prevail, if it is defined.
     */
    private void installConstructor(String name, Scriptable prototype) {
        FunctionObject fo = new FunctionObject(name, HopObject.hopObjCtor, global);

        ScriptRuntime.setFunctionProtoAndParent(global, fo);
        fo.setImmunePrototypeProperty(prototype);

        prototype.setParentScope(fo);

        ScriptableObject.defineProperty(global, name, fo, ScriptableObject.DONTENUM);

        fo.setParentScope(global);
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

    public Object processXmlRpcArgument (Object what) throws Exception {
        if (what == null)
            return null;
        if (what instanceof Vector) {
            Vector v = (Vector) what;
            Object[] a = v.toArray();
            for (int i=0; i<a.length; i++) {
                a[i] = processXmlRpcArgument(a[i]);
            }
            return Context.getCurrentContext().newArray(global, a);
        }
        if (what instanceof Hashtable) {
            Hashtable t = (Hashtable) what;
            for (Enumeration e=t.keys(); e.hasMoreElements(); ) {
                Object key = e.nextElement();
                t.put(key, processXmlRpcArgument(t.get(key)));
            }
            return Context.toObject(new SystemMap(t), global);
        }
        if (what instanceof String)
            return what;
        if (what instanceof Number)
            return what;
        if (what instanceof Boolean)
            return what;
        if (what instanceof Date) {
            Date d = (Date) what;
            Object[] args = { new Long(d.getTime()) };
            return Context.getCurrentContext().newObject(global, "Date", args);
        }
        return Context.toObject(what, global);
    }

    /**
     * convert a JavaScript Object object to a generic Java object stucture.
     */

    public Object processXmlRpcResponse (Object what) throws Exception {
        if (what instanceof NativeJavaObject) {
            what = ((NativeJavaObject) what).unwrap();
        }
        if (what instanceof NativeObject) {
            NativeObject no = (NativeObject) what;
            Object[] ids = no.getIds();
            Hashtable ht = new Hashtable(ids.length);
            for (int i=0; i<ids.length; i++) {
                if (ids[i] instanceof String) {
                    String key = (String) ids[i];
                    ht.put(key, processXmlRpcResponse(no.get(key, no)));
                }
            }
            what = ht;
        }
        if (what instanceof NativeArray) {
            NativeArray na = (NativeArray) what;
            Number n = (Number) na.get("length", na);
            int l = n.intValue();
            Vector retval = new Vector(l);
            for (int i=0; i<l; i++) {
                retval.add(i, processXmlRpcResponse(na.get(i, na)));
            }
            what = retval;
        }
        if (what instanceof Number) {
            Number n = (Number) what;
            if (what instanceof Float || what instanceof Long) {
                what = new Double(n.doubleValue());
            } else if (!(what instanceof Double)) {
                what = new Integer(n.intValue());
            }
        }
        if (what instanceof Scriptable) {
            Scriptable s = (Scriptable) what;
            if ("Date".equals(s.getClassName())) {
                what = new Date((long) ScriptRuntime.toNumber(s));
            }
        }
        return what;
       /* if (what == null || what instanceof ESNull)
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
       return jval; */
    }


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
    private void putPrototype(String protoName, Scriptable op) {
        if ((protoName != null) && (op != null)) {
            prototypes.put(protoName, new TypeInfo(op, protoName));
        }
    }


    /**
     *  Get a Script wrapper for any given object. If the object implements the IPathElement
     *  interface, the getPrototype method will be used to retrieve the name of the prototype
     * to use. Otherwise, a Java-Class-to-Script-Prototype mapping is consulted.
     */
    public Scriptable getElementWrapper(Object e) {

        Scriptable w = (Scriptable) wrappercache.get(e);

        if (w == null) {
            // Gotta find out the prototype name to use for this object...
            String prototypeName = app.getPrototypeName(e);

            Scriptable op = getPrototype(prototypeName);

            if (op == null) {
                prototypeName = "hopobject";
                op = getPrototype("hopobject");
            }

            w = new JavaObject(global, e, prototypeName, op, this);

            wrappercache.put(e, w);
        }

        return w;
    }

    /**
     *  Get a script wrapper for an instance of helma.objectmodel.INode
     */
    public Scriptable getNodeWrapper(INode n) {
        // FIXME: is this needed? should this return ESNull.theNull?
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
                    protoname = "hopobject";
                }

                esn = new HopObject(protoname);
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
    /* public void putNodeWrapper(INode n, Scriptable esn) {
        wrappercache.put(n, esn);
    } */

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

    protected static Object[] unwrapSkinpath(Object[] skinpath) {
        if (skinpath != null) {
            for (int i=0; i<skinpath.length; i++) {
                if (skinpath[i] instanceof HopObject) {
                    skinpath[i] = ((HopObject) skinpath[i]).getNode();
                } else if (skinpath[i] instanceof NativeJavaObject) {
                    skinpath[i] = ((NativeJavaObject) skinpath[i]).unwrap();
                }
            }
        }
        return skinpath;
    }

    protected static Map getSkinParam(Object paramobj) {
        Map param = null;

        if ((paramobj != null) && (paramobj != Undefined.instance)) {
            param = new HashMap();

            if (paramobj instanceof Scriptable) {
                Scriptable sp = (Scriptable) paramobj;
                Object[] ids = sp.getIds();

                for (int i = 0; i < ids.length; i++) {
                    Object obj = sp.get(ids[i].toString(), sp);
                    if (obj instanceof NativeJavaObject) {
                        param.put(ids[i], ((NativeJavaObject) obj).unwrap());
                    } else {
                        param.put(ids[i], obj);
                    }
                }
            }
        }

        return param;
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
            // e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignore) {
                }
            }
        }
    }

    /**
     *  Return the global scope of this RhinoCore.
     */
    public Scriptable getScope() {
        return global;
    }

    /**
     *  TypeInfo helper class
     */
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

    /**
     *  Object wrapper class
     */
    class Wrapper extends WrapFactory {

        public Object wrap(Context cx, Scriptable scope, Object obj, Class staticType)  {
            // System.err.println ("Wrapping: "+obj);
            if (obj instanceof INode) {
                return getNodeWrapper((INode) obj);
            }

            if (obj != null && app.getPrototypeName(obj) != null) {
                return getElementWrapper(obj);
            }

            if (obj instanceof SystemMap) {
                return new MapWrapper((Map) obj, RhinoCore.this);
            }

            if (obj instanceof String) {
                return obj;
            }

            return super.wrap(cx, scope, obj, staticType);
        }

        public Scriptable wrapAsJavaObject(Context cx, Scriptable scope, Object javaObject) {
            if (javaObject != null && app.getPrototypeName(javaObject) != null) {
                return getElementWrapper(javaObject);
            }

            return super.wrapAsJavaObject(cx, scope, javaObject);
        }

        public Scriptable wrapNewObject(Context cx, Scriptable scope, Object obj) {
            // System.err.println ("N-Wrapping: "+obj);
            if (obj instanceof INode) {
                return getNodeWrapper((INode) obj);
            }

            if (obj != null && app.getPrototypeName(obj) != null) {
                return getElementWrapper(obj);
            }

            return super.wrapNewObject(cx, scope, obj);
        }
    }

    class StringTrim extends BaseFunction {
        public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
            String str = thisObj.toString();
            return str.trim();
        }
    }

    class DateFormat extends BaseFunction {
        public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
            Date date = new Date((long) ScriptRuntime.toNumber(thisObj));
            SimpleDateFormat df = null;
            if (args.length > 0 && args[0] != Undefined.instance) {
                df = new SimpleDateFormat(args[0].toString());
            } else {
                df = new SimpleDateFormat();
            }
            return df.format(date);
        }
    }

    class NumberFormat extends BaseFunction {
        public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
            DecimalFormat df = null;
            if (args.length > 0 && args[0] != Undefined.instance) {
                df = new DecimalFormat(args[0].toString());
            } else {
                df = new DecimalFormat("#,##0.00");
            }
            return df.format(ScriptRuntime.toNumber(thisObj)).toString();
        }
    }

}

