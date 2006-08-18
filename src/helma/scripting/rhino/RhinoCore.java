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
import helma.scripting.rhino.debug.HelmaDebugger;
import helma.framework.core.*;
import helma.framework.repository.Resource;
import helma.objectmodel.*;
import helma.objectmodel.db.DbMapping;
import helma.scripting.*;
import helma.util.CacheMap;
import helma.util.SystemMap;
import helma.util.WrappedMap;
import helma.util.WeakCacheMap;
import org.mozilla.javascript.*;
import org.mozilla.javascript.tools.debugger.ScopeProvider;

import java.io.*;
import java.text.*;
import java.util.*;
import java.lang.ref.WeakReference;

/**
 * This is the implementation of ScriptingEnvironment for the Mozilla Rhino EcmaScript interpreter.
 */
public final class RhinoCore implements ScopeProvider {
    // the application we're running in
    public final Application app;

    // the global object
    GlobalObject global;

    // caching table for JavaScript object wrappers
    CacheMap wrappercache;

    // table containing JavaScript prototypes
    Hashtable prototypes;

    // timestamp of last type update
    volatile long lastUpdate = 0;

    // the wrap factory
    WrapFactory wrapper;

    // the prototype for HopObject
    ScriptableObject hopObjectProto;

    // the prototype for path objects
    PathWrapper pathProto;

    // Any error that may have been found in global code
    String globalError;

    HelmaDebugger debugger = null;

    // dynamic portion of the type check sleep that grows
    // as the app remains unchanged
    long updateSnooze = 500;

    static {
       ContextFactory.initGlobal(new HelmaContextFactory());
    }


    /**
     *  Create a Rhino evaluator for the given application and request evaluator.
     */
    public RhinoCore(Application app) {
        this.app = app;
        wrappercache = new WeakCacheMap(500);
        prototypes = new Hashtable();

        Context context = Context.enter();

        context.setCompileFunctionsWithDynamicScope(true);
        context.setApplicationClassLoader(app.getClassLoader());
        wrapper = new WrapMaker();
        wrapper.setJavaPrimitiveWrap(false);
        context.setWrapFactory(wrapper);

        // Set up visual debugger if rhino.debug = true
        if ("true".equals(app.getProperty("rhino.debug"))) {
            initDebugger(context);
        }

        // Set default optimization level according to whether debugger is on
        int optLevel = debugger == null ? 0 : -1;

        String opt = app.getProperty("rhino.optlevel");
        if (opt != null) {
            try {
                optLevel = Integer.parseInt(opt);
            } catch (Exception ignore) {
                app.logError("Invalid rhino optlevel: " + opt);
            }
        }
        context.setOptimizationLevel(optLevel);

        try {
            // create global object
            global = new GlobalObject(this, app, false);
            // call the initStandardsObject in ImporterTopLevel so that
            // importClass() and importPackage() are set up.
            global.initStandardObjects(context, false);
            global.init();

            pathProto = new PathWrapper(this);

            hopObjectProto =  HopObject.init(this);
            // use lazy loaded constructors for all extension objects that
            // adhere to the ScriptableObject.defineClass() protocol
            new LazilyLoadedCtor(global, "File",
                    "helma.scripting.rhino.extensions.FileObject", false);
            new LazilyLoadedCtor(global, "Ftp",
                    "helma.scripting.rhino.extensions.FtpObject", false);
            new LazilyLoadedCtor(global, "Image",
                    "helma.scripting.rhino.extensions.ImageObject", false);
            new LazilyLoadedCtor(global, "Remote",
                    "helma.scripting.rhino.extensions.XmlRpcObject", false);
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

    void initDebugger(Context context) {
        try {
        if (debugger == null) {
            debugger = new HelmaDebugger(app.getName());
            debugger.setScopeProvider(this);
            // debugger.setScope(global);
            debugger.attachTo(context.getFactory());
            // debugger.pack();
            // debugger.getDebugFrame().setLocation(60, 60);
        }
        // if (!debugger.isVisible())
            // debugger.setVisible(true);
        } catch (Exception x) {
            x.printStackTrace();
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
        ScriptableObject op = (type == null) ? null : type.objProto;

        // if prototype info doesn't exist (i.e. is a standard prototype
        // built by HopExtension), create it.
        if (op == null) {
            if ("global".equals(lowerCaseName)) {
                op = global;
            } else if ("hopobject".equals(lowerCaseName)) {
                op = hopObjectProto;
            } else {
                op = new HopObject(name, this);
            }
            registerPrototype(prototype, op);
        }

        // Register a constructor for all types except global.
        // This will first create a new prototyped HopObject and then calls
        // the actual (scripted) constructor on it.
        if (!"global".equals(lowerCaseName)) {
            try {
                new HopObjectCtor(name, this, op);
                op.setParentScope(global);
            } catch (Exception x) {
                app.logError("Error adding ctor for " + name,  x);
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

        type.prepareCompilation();
        Prototype prototype = type.frameworkProto;

        // set the parent prototype in case it hasn't been done before
        // or it has changed...
        setParentPrototype(prototype, type);

        type.error = null;
        if ("global".equals(prototype.getLowerCaseName())) {
            globalError = null;
        }

        // loop through the prototype's code elements and evaluate them
        Iterator code = prototype.getCodeResources();
        while (code.hasNext()) {
            evaluate(type, (Resource) code.next());
        }

        type.commitCompilation();
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
     *  This method is called before an execution context is entered to let the
     *  engine know it should update its prototype information. The update policy
     *  here is to check for update those prototypes which already have been compiled
     *  before. Others will be updated/compiled on demand.
     */
    public synchronized void updatePrototypes() throws IOException {
        if ((System.currentTimeMillis() - lastUpdate) < 1000L + updateSnooze) {
            return;
        }

        // init prototypes and/or update prototype checksums
        app.typemgr.checkPrototypes();

        // get a collection of all prototypes (code directories)
        Collection protos = app.getPrototypes();

        // in order to respect inter-prototype dependencies, we try to update
        // the global prototype before all other prototypes, and parent
        // prototypes before their descendants.

        HashSet checked = new HashSet(protos.size() * 2);

        TypeInfo type = (TypeInfo) prototypes.get("global");

        if (type != null) {
            updatePrototype(type, checked);
        }

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
        // max updateSnooze is 4 seconds, reached after 66.6 idle minutes
        long newSnooze = (lastUpdate - app.typemgr.getLastCodeUpdate()) / 1000;
        updateSnooze = Math.min(4000, Math.max(0, newSnooze));
    }

    /**
     * Check one prototype for updates. Used by <code>upatePrototypes()</code>.
     *
     * @param type the type info to check
     * @param checked a set of prototypes that have already been checked
     */
    private void updatePrototype(TypeInfo type, HashSet checked) {
        // first, remember prototype as updated
        checked.add(type.frameworkProto);

        if (type.parentType != null &&
                !checked.contains(type.parentType.frameworkProto)) {
            updatePrototype(type.getParentType(), checked);
        }

        // let the prototype check if its resources have changed
        type.frameworkProto.checkForUpdates();

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
        if (type != null) {
            if (type.hasError()) {
                throw new EvaluatorException(type.getError());
            }
            return type.objProto;
        }
        return null;
    }

    /**
     *  Get the object prototype for a prototype name and initialize/update it
     *  if necessary. The policy here is to update the prototype only if it
     *  hasn't been updated before, otherwise we assume it already was updated
     *  by updatePrototypes(), which is called for each request.
     */
    public Scriptable getPrototype(String protoName) {
        TypeInfo type = getPrototypeInfo(protoName);
        return type == null ? null : type.objProto;
    }

    /**
     * Get an array containing the property ids of all properties that were
     * compiled from scripts for the given prototype.
     *
     * @param protoName the name of the prototype
     * @return an array containing all compiled properties of the given prototype
     */
    public Map getPrototypeProperties(String protoName) {
        TypeInfo type = getPrototypeInfo(protoName);
        SystemMap map = new SystemMap();
        Iterator it =    type.compiledProperties.iterator();
        while(it.hasNext()) {
            Object key = it.next();
            if (key instanceof String)
                map.put(key, type.objProto.get((String) key, type.objProto));
        }
        return map;
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
            type.frameworkProto.checkForUpdates();

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
        // throws EvaluatorException if type has a syntax error
        Scriptable op = getValidPrototype(protoname);

        // if this is an untyped object return false
        if (op == null) {
            return false;
        }

        return ScriptableObject.getProperty(op, fname) instanceof Function;
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
        WeakReference ref = (WeakReference) wrappercache.get(e);
        Scriptable wrapper = ref == null ? null : (Scriptable) ref.get();

        if (wrapper == null) {
            // Gotta find out the prototype name to use for this object...
            String prototypeName = app.getPrototypeName(e);
            Scriptable op = getPrototype(prototypeName);

            if (op == null) {
                // no prototype found, return an unscripted wrapper
                wrapper = new NativeJavaObject(global, e, e.getClass());
            } else {
                wrapper = new JavaObject(global, e, prototypeName, op, this);
            }

            wrappercache.put(e, new WeakReference(wrapper));
        }

        return wrapper;
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

            Scriptable op = getValidPrototype(protoname);

            // no prototype found for this node
            if (op == null) {
                // maybe this object has a prototype name that has been
                // deleted, but the storage layer was able to set a
                // DbMapping matching the relational table the object
                // was fetched from.
                DbMapping dbmap = n.getDbMapping();
                if (dbmap != null && (protoname = dbmap.getTypeName()) != null) {
                    op = getValidPrototype(protoname);
                }

                // if not found, fall back to HopObject prototype
                if (op == null) {
                    protoname = "HopObject";
                    op = getValidPrototype("HopObject");
                }
            }

            esn = new HopObject(protoname, this, n, op);

            wrappercache.put(n, esn);
        }

        return esn;
    }

    protected String postProcessHref(Object obj, String protoName, String basicHref)
            throws UnsupportedEncodingException, IOException {
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
                    Object result;

                    try {
                        result = engine.invoke(handler, hrefFunction,
                                               new Object[] { basicHref },
                                               ScriptingEngine.ARGS_WRAP_DEFAULT,
                                               false);
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
            // get the currently active rhino engine and render the skin
            Context cx = Context.getCurrentContext();
            RhinoEngine engine = (RhinoEngine) cx.getThreadLocal("engine");

            while (handler != null) {
                Prototype proto = app.getPrototype(handler);

                if (proto != null) {
                    skin = engine.getSkin(proto.getName(), hrefSkin);
                }

                if (skin != null) {
                    break;
                }

                handler = app.getParentElement(handler);
            }

            if (skin != null) {
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

    private synchronized void evaluate (TypeInfo type, Resource code) {
        // get the current context
        Context cx = Context.getCurrentContext();
        // unregister the per-thread scope while evaluating
        Object threadScope = cx.getThreadLocal("threadscope");
        cx.removeThreadLocal("threadscope");

        String sourceName = code.getName();
        Reader reader = null;

        try {
            Scriptable op = type.objProto;

            // do the update, evaluating the file
            if (sourceName.endsWith(".js")) {
                reader = new InputStreamReader(code.getInputStream());
                cx.evaluateReader(op, reader, sourceName, 1, null);
            } else if (sourceName.endsWith(".hac")) {
                reader = new StringReader(HacHspConverter.convertHac(code));
                cx.evaluateReader(op, reader, sourceName, 0, null);
            } else if (sourceName.endsWith(".hsp")) {
                reader = new StringReader(HacHspConverter.convertHsp(code));
                cx.evaluateReader(op, reader, sourceName, 0, null);
                reader = new StringReader(HacHspConverter.convertHspAsString(code));
                cx.evaluateReader(op, reader, sourceName, 0, null);
            }

        } catch (Exception e) {
            app.logError("Error parsing file " + sourceName,  e);
            // mark prototype as broken
            if (type.error == null) {
                type.error = e.getMessage();
                if (type.error == null || e instanceof EcmaError) {
                    type.error = e.toString();
                }
                if ("global".equals(type.frameworkProto.getLowerCaseName())) {
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
                cx.putThreadLocal("threadscope", threadScope);
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
        Prototype frameworkProto;

        // the JavaScript prototype for this type
        ScriptableObject objProto;

        // timestamp of last update. This is -1 so even an empty prototype directory
        // (with lastUpdate == 0) gets evaluated at least once, which is necessary
        // to get the prototype chain set.
        long lastUpdate = -1;

        // the parent prototype info
        TypeInfo parentType;

        // a set of property keys that were in script compilation.
        // Used to decide which properties should be removed if not renewed.
        Set compiledProperties;

        // a set of property keys that were present before first script compilation
        final Set predefinedProperties;

        String error;

        public TypeInfo(Prototype proto, ScriptableObject op) {
            frameworkProto = proto;
            objProto = op;
            // remember properties already defined on this object prototype
            compiledProperties = new HashSet();
            predefinedProperties = new HashSet();
            Object[] keys = op.getAllIds();
            for (int i = 0; i < keys.length; i++) {
                predefinedProperties.add(keys[i].toString());
            }
        }

        /**
         * If prototype implements PropertyRecorder tell it to start
         * registering property puts.
         */
        public void prepareCompilation() {
            if (objProto instanceof PropertyRecorder) {
                ((PropertyRecorder) objProto).startRecording();
            }
        }

        /**
         * Compilation has been completed successfully - switch over to code
         * from temporary prototype, removing properties that haven't been
         * renewed.
         */
        public void commitCompilation() {
            // loop through properties defined on the prototype object
            // and remove thos properties which haven't been renewed during
            // this compilation/evaluation pass.
            if (objProto instanceof PropertyRecorder) {

                PropertyRecorder recorder = (PropertyRecorder) objProto;

                recorder.stopRecording();
                Set changedProperties = recorder.getChangeSet();
                recorder.clearChangeSet();

                // ignore all  properties that were defined before we started
                // compilation. We won't manage these properties, even
                // if they were set during compilation.
                changedProperties.removeAll(predefinedProperties);

                // remove all renewed properties from the previously compiled
                // property names so we can remove those properties that were not
                // renewed in this compilation
                compiledProperties.removeAll(changedProperties);

                boolean isGlobal = "global".equals(frameworkProto.getLowerCaseName());

                Iterator it = compiledProperties.iterator();
                while (it.hasNext()) {
                    String key = (String) it.next();
                    if (isGlobal && prototypes.containsKey(key.toLowerCase())) {
                        // avoid removing HopObject constructor
                        predefinedProperties.add(key);
                        continue;
                    }
                    try {
                        objProto.setAttributes(key, 0);
                        objProto.delete(key);
                    } catch (Exception px) {
                        System.err.println("Error unsetting property "+key+" on "+
                                           frameworkProto.getName());
                    }
                }

                // update compiled properties
                compiledProperties = changedProperties;
            }

            // mark this type as updated
            lastUpdate = frameworkProto.lastCodeUpdate();

            // If this prototype defines a postCompile() function, call it
            Context cx = Context.getCurrentContext();
            try {
                Object fObj = ScriptableObject.getProperty(objProto,
                                                           "onCodeUpdate");
                if (fObj instanceof Function) {
                    Object[] args = {frameworkProto.getName()};
                    ((Function) fObj).call(cx, global, objProto, args);
                }
            } catch (Exception x) {
                app.logError("Exception in "+frameworkProto.getName()+
                             ".onCodeUpdate(): " + x, x);
            }
        }

        public boolean needsUpdate() {
            return frameworkProto.lastCodeUpdate() > lastUpdate;
        }

        public void setParentType(TypeInfo type) {
            parentType = type;
            if (type == null) {
                objProto.setPrototype(null);
            } else {
                objProto.setPrototype(type.objProto);
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
            return ("TypeInfo[" + frameworkProto + "," + new Date(lastUpdate) + "]");
        }
    }

    /**
     *  Object wrapper class
     */
    class WrapMaker extends WrapFactory {

        public Object wrap(Context cx, Scriptable scope, Object obj, Class staticType) {
            // Wrap Nodes
            if (obj instanceof INode) {
                return getNodeWrapper((INode) obj);
            }

            // Masquerade SystemMap and WrappedMap as native JavaScript objects
            if (obj instanceof SystemMap || obj instanceof WrappedMap) {
                return new MapWrapper((Map) obj, RhinoCore.this);
            }

            // Convert java.util.Date objects to JavaScript Dates
            if (obj instanceof Date) {
                Object[] args = { new Long(((Date) obj).getTime()) };
                try {
                    return cx.newObject(global, "Date", args);
                 } catch (JavaScriptException nafx) {
                    return obj;
                }
            }

            // Wrap scripted Java objects
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
            SimpleDateFormat df;

            if (args.length > 0 && args[0] != Undefined.instance && args[0] != null) {
                if (args.length > 1 && args[1] instanceof NativeJavaObject) {
                    Object locale = ((NativeJavaObject) args[1]).unwrap();
                    if (locale instanceof Locale) {
                        df = new SimpleDateFormat(args[0].toString(), (Locale) locale);
                    } else {
                        throw new IllegalArgumentException("Second argument to Date.format() not a java.util.Locale: " +
                                                            locale.getClass());
                    }
                } else {
                    df = new SimpleDateFormat(args[0].toString());
                }
            } else {
                df = new SimpleDateFormat();
            }
            return df.format(date);
        }
    }

    class NumberFormat extends BaseFunction {
        public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
            DecimalFormat df;
            if (args.length > 0 && args[0] != Undefined.instance) {
                df = new DecimalFormat(args[0].toString());
            } else {
                df = new DecimalFormat("#,##0.00");
            }
            return df.format(ScriptRuntime.toNumber(thisObj));
        }
    }

}

class HelmaContextFactory extends ContextFactory {
    protected  boolean hasFeature(Context cx, int featureIndex) {
        if (featureIndex == Context.FEATURE_DYNAMIC_SCOPE) {
            return true;
        }
        return super.hasFeature(cx, featureIndex);
    }
}
