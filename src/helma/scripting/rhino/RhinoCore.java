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
import helma.framework.core.*;
import helma.objectmodel.*;
import helma.scripting.*;
import helma.util.CacheMap;
import helma.util.SystemMap;
import helma.util.WrappedMap;
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
    DynamicGlobalObject global;

    // caching table for JavaScript object wrappers
    CacheMap wrappercache;

    // table containing JavaScript prototypes
    Hashtable prototypes;

    // timestamp of last type update
    long lastUpdate = 0;

    // the wrap factory
    WrapFactory wrapper;

    // the prototype for HopObject
    ScriptableObject hopObjectProto;

    // the prototype for path objects
    PathWrapper pathProto;

    // Any error that may have been found in global code
    String globalError;

    /**
     *  Create a Rhino evaluator for the given application and request evaluator.
     */
    public RhinoCore(Application app) {
        this.app = app;
        // wrappercache = new CacheMap(500, .75f);
        wrappercache = new CacheMap(500);
        prototypes = new Hashtable();

        Context context = Context.enter();

        context.setCompileFunctionsWithDynamicScope(true);
        context.setApplicationClassLoader(app.getClassLoader());
        wrapper = new WrapMaker();
        wrapper.setJavaPrimitiveWrap(false);
        context.setWrapFactory(wrapper);

        int optLevel = 0;

        try {
            optLevel = Integer.parseInt(app.getProperty("rhino.optlevel"));
        } catch (Exception ignore) {
        }

        // System.err.println("Setting Rhino optlevel to " + optLevel);
        context.setOptimizationLevel(optLevel);

        try {
            // create global object
            global = new DynamicGlobalObject(this, app);
            global.init();
            // call the initStandardsObject in ImporterTopLevel so that
            // importClass() and importPackage() are set up.
            global.initStandardObjects(context, false);

            pathProto = new PathWrapper(this);

            hopObjectProto =  HopObject.init(global);
            FileObject.init(global);
            FtpObject.init(global);
            ImageObject.init(global);
            XmlRpcObject.init(global);
            MailObject.init(global, app.getProperties());

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
            Context.exit();
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
     *   Initialize a prototype info without compiling its script files.
     *
     *  @param prototype the prototype to be created
     */
    private synchronized void initPrototype(Prototype prototype) {

        String name = prototype.getName();
        String lowerCaseName = prototype.getLowerCaseName();

        TypeInfo type = (TypeInfo) prototypes.get(lowerCaseName);

        // check if the prototype info exists already
        ScriptableObject op = (type == null) ? null : type.objectPrototype;

        // if prototype info doesn't exist (i.e. is a standard prototype
        // built by HopExtension), create it.
        if (op == null) {
            if ("global".equals(lowerCaseName)) {
                op = global;
            } else if ("hopobject".equals(lowerCaseName)) {
                op = hopObjectProto;
            } else {
                op = new HopObject(name);
                op.setParentScope(global);
            }
            type = registerPrototype(prototype, op);
        }

        // Register a constructor for all types except global.
        // This will first create a new prototyped HopObject and then calls
        // the actual (scripted) constructor on it.
        if (!"global".equals(lowerCaseName)) {
            try {
                installConstructor(name, op);
            } catch (Exception ignore) {
                System.err.println("Error adding ctor for " + name + ": " + ignore);
                ignore.printStackTrace();
            }
        }
    }

    /**
     *  Set up a prototype, parsing and compiling all its script files.
     *
     *  @param type the info, containing the object proto, last update time and
     *         the set of compiled functions properties
     */
    private synchronized void evaluatePrototype(TypeInfo type) {

        Scriptable op = type.objectPrototype;
        Prototype prototype = type.frameworkPrototype;

        // set the parent prototype in case it hasn't been done before
        // or it has changed...
        setParentPrototype(prototype, type);

        type.error = null;
        if ("global".equals(prototype.getLowerCaseName())) {
            globalError = null;
        }

        // loop through the prototype's code elements and evaluate them
        // first the zipped ones ...
        for (Iterator it = prototype.getZippedCode().values().iterator(); it.hasNext();) {
            Object code = it.next();

            evaluate(type, code);
        }

        // then the unzipped ones (this is to make sure unzipped code overwrites zipped code)
        for (Iterator it = prototype.getCode().values().iterator(); it.hasNext();) {
            Object code = it.next();

            evaluate(type, code);
        }

        // loop through function properties defined for this proto and
        // remove those which are left over from the previous generation
        // and haven't been renewed in this pass.
        Set oldFunctions = type.compiledFunctions;
        Set newFunctions = new HashSet();
        Object[] keys = ((ScriptableObject) op).getAllIds();
        for (int i = 0; i < keys.length; i++) {
            String key = keys[i].toString();
            if (type.predefinedProperties.contains(key)) {
                // don't mess with properties we didn't set
                continue;
            }
            Object prop = op.get(key, op);
            if (oldFunctions.contains(prop) && prop instanceof NativeFunction) {
                // if this is a function compiled from script, it's from the
                // old generation and wasn't renewed -- delete it.
                // System.err.println("DELETING OLD FUNC: "+key);
                try {
                    ((ScriptableObject) op).setAttributes(key, 0);
                    op.delete(key);
                } catch (Exception px) {
                    System.err.println("Error unsetting property "+key+" on "+prototype);
                }
            } else {
                newFunctions.add(prop);
            }
        }

        type.compiledFunctions = newFunctions;
        type.lastUpdate = prototype.getLastUpdate();
    }

    /**
     *  Set the parent prototype on the ObjectPrototype.
     *
     *  @param prototype the prototype spec
     *  @param type the prototype object info
     */
    private void setParentPrototype(Prototype prototype, TypeInfo type) {
        String name = prototype.getName();
        String lowerCaseName = prototype.getLowerCaseName();

        if (!"global".equals(lowerCaseName) && !"hopobject".equals(lowerCaseName)) {

            // get the prototype's prototype if possible and necessary
            TypeInfo parentType = null;
            Prototype parent = prototype.getParentPrototype();

            if (parent != null) {
                // see if parent prototype is already registered. if not, register it
                parentType = getPrototypeInfo(parent.getName());
            }

            if (parentType == null && !app.isJavaPrototype(name)) {
                // FIXME: does this ever occur?
                parentType = getPrototypeInfo("hopobject");
            }

            type.setParentType(parentType);
        }
    }

    /**
     *  This is a version of org.mozilla.javascript.FunctionObject.addAsConstructor()
     *  that does not set the constructor property in the prototype. This is because
     *  we want our own scripted constructor function to be visible, if it is defined.
     *
     * @param name the name of the constructor
     * @param op the object prototype
     */
    private void installConstructor(String name, Scriptable op) {
        FunctionObject fo = new FunctionObject(name, HopObject.hopObjCtor, global);

        ScriptRuntime.setFunctionProtoAndParent(global, fo);
        fo.setImmunePrototypeProperty(op);

        op.setParentScope(fo);

        ScriptableObject.defineProperty(global, name, fo, ScriptableObject.DONTENUM);

        fo.setParentScope(global);
    }

    /**
     *  This method is called before an execution context is entered to let the
     *  engine know it should update its prototype information. The update policy
     *  here is to check for update those prototypes which already have been compiled
     *  before. Others will be updated/compiled on demand.
     */
    public synchronized void updatePrototypes() {
        if ((System.currentTimeMillis() - lastUpdate) < 1000L) {
            return;
        }

        Collection protos = app.getPrototypes();

        // in order to respect inter-prototype dependencies, we try to update
        // the global prototype before all other prototypes, and parent
        // prototypes before their descendants.

        HashSet checked = new HashSet(protos.size()*2);

        TypeInfo type = (TypeInfo) prototypes.get("global");

        updatePrototype(type, checked);

        for (Iterator i = protos.iterator(); i.hasNext();) {
            Prototype proto = (Prototype) i.next();

            if (checked.contains(proto)) {
                continue;
            }

            type = (TypeInfo) prototypes.get(proto.getLowerCaseName());

            if (type == null) {
                // a prototype we don't know anything about yet. Init local update info.
                initPrototype(proto);
            } else if (type.lastUpdate > -1) {
                // only need to update prototype if it has already been initialized.
                // otherwise, this will be done on demand.
                updatePrototype(type, checked);
            }
        }

        lastUpdate = System.currentTimeMillis();
    }

    /**
     * Check one prototype for updates. Used by <code>upatePrototypes()</code>.
     *
     * @param type the type info to check
     * @param checked a set of prototypes that have already been checked
     */
    private void updatePrototype(TypeInfo type, HashSet checked) {
        // first, remember prototype as updated
        checked.add(type.frameworkPrototype);

        if (type.parentType != null &&
                !checked.contains(type.parentType.frameworkPrototype)) {
            updatePrototype(type.getParentType(), checked);
        }

        // let the type manager scan the prototype's directory
        app.typemgr.updatePrototype(type.frameworkPrototype);

        // and re-evaluate if necessary
        if (type.needsUpdate()) {
            evaluatePrototype(type);
        }
    }

    /**
     * A version of getPrototype() that retrieves a prototype and checks
     * if it is valid, i.e. there were no errors when compiling it. If
     * invalid, a ScriptingException is thrown.
     */
    public Scriptable getValidPrototype(String protoName) {
        if (globalError != null) {
            throw new EvaluatorException(globalError);
        }
        TypeInfo type = getPrototypeInfo(protoName);
        if (type != null && type.hasError()) {
            throw new EvaluatorException(type.getError());
        }
        return type == null ? null : type.objectPrototype;
    }

    /**
     *  Get the object prototype for a prototype name and initialize/update it
     *  if necessary. The policy here is to update the prototype only if it
     *  hasn't been updated before, otherwise we assume it already was updated
     *  by updatePrototypes(), which is called for each request.
     */
    public Scriptable getPrototype(String protoName) {
        TypeInfo type = getPrototypeInfo(protoName);
        return type == null ? null : type.objectPrototype;
    }

    /**
     *  Private helper function that retrieves a prototype's TypeInfo
     *  and creates it if not yet created. This is used by getPrototype() and
     *  getValidPrototype().
     */
    private TypeInfo getPrototypeInfo(String protoName) {
        if (protoName == null) {
            return null;
        }

        TypeInfo type = (TypeInfo) prototypes.get(protoName.toLowerCase());

        // if type exists and hasn't been evaluated (used) yet, evaluate it now.
        // otherwise, it has already been evaluated for this request by updatePrototypes(),
        // which is called before a request is handled.
        if ((type != null) && (type.lastUpdate == -1)) {
            app.typemgr.updatePrototype(type.frameworkPrototype);

            if (type.needsUpdate()) {
                evaluatePrototype(type);
            }
        }

        return type;
    }

    /**
     * Register an object prototype for a prototype name.
     */
    private TypeInfo registerPrototype(Prototype proto, ScriptableObject op) {
        TypeInfo type = new TypeInfo(proto, op);
        prototypes.put(proto.getLowerCaseName(), type);
        return type;
    }

    /**
    * Check if an object has a function property (public method if it
    * is a java object) with that name.
    */
    public boolean hasFunction(String protoname, String fname) {
        // System.err.println ("HAS_FUNC: "+fname);
        // throws EvaluatorException if type has a syntax error
        Scriptable op = getValidPrototype(protoname);

        try {
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
        // unwrap if argument is a Wrapper
        if (what instanceof Wrapper) {
            what = ((Wrapper) what).unwrap();
        }
        if (what instanceof NativeObject) {
            NativeObject no = (NativeObject) what;
            Object[] ids = no.getIds();
            Hashtable ht = new Hashtable(ids.length*2);
            for (int i=0; i<ids.length; i++) {
                if (ids[i] instanceof String) {
                    String key = (String) ids[i];
                    Object o = no.get(key, no);
                    if (o != null) {
                        ht.put(key, processXmlRpcResponse(o));
                    }
                }
            }
            what = ht;
        } else if (what instanceof NativeArray) {
            NativeArray na = (NativeArray) what;
            Number n = (Number) na.get("length", na);
            int l = n.intValue();
            Vector retval = new Vector(l);
            for (int i=0; i<l; i++) {
                retval.add(i, processXmlRpcResponse(na.get(i, na)));
            }
            what = retval;
        } else if (what instanceof Map) {
            Map map = (Map) what;
            Hashtable ht = new Hashtable(map.size()*2);
            for (Iterator it=map.entrySet().iterator(); it.hasNext();) {
                Map.Entry entry = (Map.Entry) it.next();
                ht.put(entry.getKey().toString(),
                       processXmlRpcResponse(entry.getValue()));
            }
            what = ht;
        } else if (what instanceof Number) {
            Number n = (Number) what;
            if (what instanceof Float || what instanceof Long) {
                what = new Double(n.doubleValue());
            } else if (!(what instanceof Double)) {
                what = new Integer(n.intValue());
            }
        } else if (what instanceof Scriptable) {
            Scriptable s = (Scriptable) what;
            if ("Date".equals(s.getClassName())) {
                what = new Date((long) ScriptRuntime.toNumber(s));
            }
        }
        return what;
    }


    /**
     * Return the application we're running in
     */
    public Application getApplication() {
        return app;
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
                prototypeName = "HopObject";
                op = getPrototype("HopObject");
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
        if (n == null) {
            return null;
        }

        HopObject esn = (HopObject) wrappercache.get(n);

        if (esn == null) {

            String protoname = n.getPrototype();

            Scriptable op = null;

            op = getValidPrototype(protoname);

            // no prototype found for this node?
            if (op == null) {
                op = getValidPrototype("HopObject");
                protoname = "HopObject";
            }

            esn = new HopObject(protoname, op);
            esn.init(this, n);

            wrappercache.put(n, esn);
        }

        return esn;
    }

    protected String postProcessHref(Object obj, String protoName, String basicHref) {

        // check if the app.properties specify a href-function to post-process the
        // basic href.
        String hrefFunction = app.getProperty("hrefFunction", null);

        if (hrefFunction != null) {

            Object handler = obj;
            String proto = protoName;

            while (handler != null) {
                if (hasFunction(proto, hrefFunction)) {

                    // get the currently active rhino engine and invoke the function
                    Context cx = Context.getCurrentContext();
                    RhinoEngine engine = (RhinoEngine) cx.getThreadLocal("engine");
                    Object result = null;

                    try {
                        result = engine.invoke(handler, hrefFunction,
                                               new Object[] { basicHref }, false);
                    } catch (ScriptingException x) {
                        throw new EvaluatorException("Error in hrefFunction: " + x);
                    }

                    if (result == null) {
                        throw new EvaluatorException("hrefFunction " + hrefFunction +
                                                       " returned null");
                    }

                    basicHref = result.toString();
                    break;
                }
                handler = app.getParentElement(handler);
                proto = app.getPrototypeName(handler);

            }
        }

        // check if the app.properties specify a href-skin to post-process the
        // basic href.
        String hrefSkin = app.getProperty("hrefSkin", null);

        if (hrefSkin != null) {
            // we need to post-process the href with a skin for this application
            // first, look in the object href was called on.
            Skin skin = null;
            Object handler = obj;

            while (handler != null) {
                Prototype proto = app.getPrototype(handler);

                if (proto != null) {
                    skin = proto.getSkin(hrefSkin);
                }

                if (skin != null) {
                    break;
                }

                handler = app.getParentElement(handler);
            }

            if (skin != null) {
                // get the currently active rhino engine and render the skin
                Context cx = Context.getCurrentContext();
                RhinoEngine engine = (RhinoEngine) cx.getThreadLocal("engine");

                engine.getResponse().pushStringBuffer();
                HashMap param = new HashMap();
                param.put("path", basicHref);
                skin.render(engine.getRequestEvaluator(), handler, param);

                basicHref = engine.getResponse().popStringBuffer().trim();
            }
        }

        return basicHref;
    }

    /////////////////////////////////////////////
    // skin related methods
    /////////////////////////////////////////////

    protected static Object[] unwrapSkinpath(Object[] skinpath) {
        if (skinpath != null) {
            for (int i=0; i<skinpath.length; i++) {
                if (skinpath[i] instanceof HopObject) {
                    skinpath[i] = ((HopObject) skinpath[i]).getNode();
                } else if (skinpath[i] instanceof Wrapper) {
                    skinpath[i] = ((Wrapper) skinpath[i]).unwrap();
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
                    if (obj instanceof Wrapper) {
                        param.put(ids[i], ((Wrapper) obj).unwrap());
                    } else if (obj != Undefined.instance) {
                        param.put(ids[i], obj);
                    }
                }
            }
        }

        return param;
    }

    ////////////////////////////////////////////////
    // private evaluation/compilation methods
    ////////////////////////////////////////////////

    private synchronized void evaluate(TypeInfo type, Object code) {
        if (code instanceof FunctionFile) {
            FunctionFile funcfile = (FunctionFile) code;
            File file = funcfile.getFile();

            if (file != null) {
                try {
                    FileReader fr = new FileReader(file);

                    updateEvaluator(type, fr, funcfile.getSourceName(), 1);
                } catch (IOException iox) {
                    app.logEvent("Error updating function file: " + iox);
                }
            } else {
                StringReader reader = new StringReader(funcfile.getContent());

                updateEvaluator(type, reader, funcfile.getSourceName(), 1);
            }
        } else if (code instanceof ActionFile) {
            ActionFile action = (ActionFile) code;
            RhinoActionAdapter fa = new RhinoActionAdapter(action);

            try {
                updateEvaluator(type, new StringReader(fa.function),
                                action.getSourceName(), 0);
                if (fa.functionAsString != null) {
                    // templates have an _as_string variant that needs to be compiled
                    updateEvaluator(type, new StringReader(fa.functionAsString),
                                action.getSourceName(), 0);
                }
            } catch (Exception esx) {
                app.logEvent("Error parsing " + action + ": " + esx);
            }
        }
    }

    private synchronized void updateEvaluator(TypeInfo type, Reader reader,
                                              String sourceName, int firstline) {
        // System.err.println("UPDATE EVALUATOR: "+prototype+" - "+sourceName);
        Scriptable threadScope = global.unregisterScope();

        try {
            // get the current context
            Context cx = Context.getCurrentContext();

            Scriptable op = type.objectPrototype;

            // do the update, evaluating the file
            cx.evaluateReader(op, reader, sourceName, firstline, null);

        } catch (Exception e) {
            app.logEvent("Error parsing file " + sourceName + ": " + e);
            // also write to standard out unless we're logging to it anyway
            if (!"console".equalsIgnoreCase(app.getProperty("logDir"))) {
                System.err.println("Error parsing file " + sourceName + ": " + e);
            }
            // mark prototype as broken
            if (type.error == null) {
                type.error = e.getMessage();
                if (type.error == null || e instanceof EcmaError) {
                    type.error = e.toString();
                }
                if ("global".equals(type.frameworkPrototype.getLowerCaseName())) {
                    globalError = type.error;
                }
                wrappercache.clear();
            }
            // e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignore) {
                    // shouldn't happen
                }
            }
            if (threadScope != null) {
                global.registerScope(threadScope);
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

        // the framework prototype object
        Prototype frameworkPrototype;

        // the JavaScript prototype for this type
        ScriptableObject objectPrototype;

        // timestamp of last update. This is -1 so even an empty prototype directory
        // (with lastUpdate == 0) gets evaluated at least once, which is necessary
        // to get the prototype chain set.
        long lastUpdate = -1;

        // the parent prototype info
        TypeInfo parentType;

        // a set of property values that were defined in last script compliation
        Set compiledFunctions;

        // a set of property keys that were present before first script compilation
        final Set predefinedProperties;

        String error;

        public TypeInfo(Prototype proto, ScriptableObject op) {
            frameworkPrototype = proto;
            objectPrototype = op;
            compiledFunctions = new HashSet(0);
            // remember properties already defined on this object prototype
            predefinedProperties = new HashSet();
            Object[] keys = op.getAllIds();
            for (int i = 0; i < keys.length; i++) {
                predefinedProperties.add(keys[i].toString());
            }
        }

        public boolean needsUpdate() {
            return frameworkPrototype.getLastUpdate() > lastUpdate;
        }

        public void setParentType(TypeInfo type) {
            parentType = type;
            if (type == null) {
                objectPrototype.setPrototype(null);
            } else {
                objectPrototype.setPrototype(type.objectPrototype);
            }
        }

        public TypeInfo getParentType() {
            return parentType;
        }

        public boolean hasError() {
            TypeInfo p = this;
            while (p != null) {
                if (p.error != null)
                    return true;
                p = p.parentType;
            }
            return false;
        }

        public String getError() {
            TypeInfo p = this;
            while (p != null) {
                if (p.error != null)
                    return p.error;
                p = p.parentType;
            }
            return null;
        }

        public String toString() {
            return ("TypeInfo[" + frameworkPrototype + "," + new Date(lastUpdate) + "]");
        }
    }

    /**
     *  Object wrapper class
     */
    class WrapMaker extends WrapFactory {

        public Object wrap(Context cx, Scriptable scope, Object obj, Class staticType) {
            // System.err.println ("Wrapping: "+obj);
            if (obj instanceof INode) {
                return getNodeWrapper((INode) obj);
            }

            if (obj instanceof SystemMap || obj instanceof WrappedMap) {
                return new MapWrapper((Map) obj, RhinoCore.this);
            }

            if (obj != null && app.getPrototypeName(obj) != null) {
                return getElementWrapper(obj);
            }

            return super.wrap(cx, scope, obj, staticType);
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

