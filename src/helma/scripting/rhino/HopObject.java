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

import helma.scripting.ScriptingException;
import helma.framework.core.*;
import helma.objectmodel.*;
import helma.objectmodel.db.*;
import org.mozilla.javascript.*;

import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Map;

/**
 * 
 */
public class HopObject extends ScriptableObject implements Wrapper {
    static Method hopObjCtor;

    static {
        Method[] methods = HopObject.class.getMethods();

        for (int i = 0; i < methods.length; i++) {
            if ("jsConstructor".equals(methods[i].getName())) {
                hopObjCtor = methods[i];

                break;
            }
        }
    }

    String className;
    INode node;
    RhinoCore core;

    /**
     * Creates a new HopObject object.
     */
    public HopObject() {
        className = "HopObject";
    }

    /**
     * Creates a new HopObject prototype.
     *
     * @param cname ...
     */
    protected HopObject(String cname) {
        className = cname;
    }


    /**
     * Creates a new HopObject prototype.
     *
     * @param cname ...
     */
    protected HopObject(String cname, Scriptable proto) {
        className = cname;
        setPrototype(proto);
    }

    public static HopObject init(Scriptable scope)
            throws PropertyException {
        int attributes = READONLY | DONTENUM | PERMANENT;

        // create prototype object
        HopObject proto = new HopObject();
        proto.setPrototype(getObjectPrototype(scope));

        // install JavaScript methods and properties
        Method[] methods = HopObject.class.getDeclaredMethods();
        for (int i=0; i<methods.length; i++) {
            String methodName = methods[i].getName();

            if (methodName.startsWith("jsFunction_")) {
                methodName = methodName.substring(11);
                FunctionObject func = new FunctionObject(methodName,
                                                         methods[i], proto);
                proto.defineProperty(methodName, func, attributes);

            } else if (methodName.startsWith("jsGet_")) {
                methodName = methodName.substring(6);
                proto.defineProperty(methodName, null, methods[i],
                                         null, attributes);
            }
        }
        return proto;
    }


    /**
     *  This method is used as HopObject constructor from JavaScript.
     */
    public static Object jsConstructor(Context cx, Object[] args,
                                              Function ctorObj, boolean inNewExpr)
                         throws EvaluatorException, ScriptingException {
        RhinoEngine engine = (RhinoEngine) cx.getThreadLocal("engine");
        RhinoCore core = engine.core;
        String protoname = ((FunctionObject) ctorObj).getFunctionName();

        // if this is a java object prototype, create a new java object
        // of the given class instead of a HopObject.
        if (core.app.isJavaPrototype(protoname)) {
            String classname = core.app.getJavaClassForPrototype(protoname);
            try {
                Class clazz = Class.forName(classname);
                // try to get the constructor matching our arguments
                Class[] argsTypes = new Class[args.length];
                for (int i=0; i<argsTypes.length; i++) {
                    argsTypes[i] = args[i] == null ? null : args[i].getClass();
                }
                Constructor cnst = clazz.getConstructor(argsTypes);
                // crate a new instance using the constructor
                Object obj = cnst.newInstance(args);
                return Context.toObject(obj, engine.global);
            } catch (Exception x) {
                System.err.println("Error in Java constructor: "+x);
                throw new EvaluatorException(x.toString());
            }
        } else {
            INode node = new helma.objectmodel.db.Node(protoname, protoname,
                                                    core.app.getWrappedNodeManager());
            Scriptable proto = core.getPrototype(protoname);
            HopObject hobj = new HopObject(protoname, proto);

            hobj.init(core, node);
            if (proto != null) {
                engine.invoke(hobj, "constructor", args, false);
            }

            return hobj;
        }
    }

    /**
     *
     *
     * @return ...
     */
    public String getClassName() {
        return className;
    }

    /**
     *
     *
     * @param c ...
     * @param n ...
     */
    public void init(RhinoCore c, INode n) {
        core = c;
        node = n;
    }

    /**
     *  Return the INode wrapped by this HopObject.
     *
     * @return the wrapped INode instance
     */
    public INode getNode() {
        return node;
    }

    /**
     * Returns the wrapped Node. Implements unwrap() in interface Wrapper.
     *
     */
    public Object unwrap() {
        if (node != null) {
            checkNode();
            return node;
        } else {
            return this;
        }
    }

    /**
     * Check if the node has been invalidated. If so, it has to be re-fetched
     * from the db via the app's node manager.
     */
    private final void checkNode() {
        if (node != null && node.getState() == INode.INVALID) {
            if (node instanceof helma.objectmodel.db.Node) {
                NodeHandle handle = ((helma.objectmodel.db.Node) node).getHandle();
                node = handle.getNode(core.app.getWrappedNodeManager());
            }
        }
    }


    /**
     *
     *
     * @return ...
     */
    public Object jsGet_cache() {
        if (node == null) {
            return null;
        }

        checkNode();

        return node.getCacheNode();
    }

    /**
     * Render a skin to the response buffer.
     *
     * @param skinobj The skin object or name
     * @param paramobj An optional parameter object
     *
     * @return ...
     */
    public boolean jsFunction_renderSkin(Object skinobj, Object paramobj) {
        Context cx = Context.getCurrentContext();
        RequestEvaluator reval = (RequestEvaluator) cx.getThreadLocal("reval");
        RhinoEngine engine = (RhinoEngine) cx.getThreadLocal("engine");
        Skin skin;

        if (skinobj instanceof Skin) {
            skin = (Skin) skinobj;
        } else {
            skin = engine.getSkin(className, skinobj.toString());
        }

        Map param = RhinoCore.getSkinParam(paramobj);

        if (skin != null) {
            skin.render(reval, node, param);
        }

        return true;
    }

    /**
     *  Render a skin and return its output as string.
     *
     * @param skinobj The skin object or name
     * @param paramobj An optional parameter object
     *
     * @return ...
     */
    public String jsFunction_renderSkinAsString(Object skinobj, Object paramobj) {
        Context cx = Context.getCurrentContext();
        RequestEvaluator reval = (RequestEvaluator) cx.getThreadLocal("reval");
        RhinoEngine engine = (RhinoEngine) cx.getThreadLocal("engine");
        Skin skin;

        if (skinobj instanceof Skin) {
            skin = (Skin) skinobj;
        } else {
            skin = engine.getSkin(className, skinobj.toString());
        }

        Map param = RhinoCore.getSkinParam(paramobj);

        if (skin != null) {
            reval.res.pushStringBuffer();
            skin.render(reval, node, param);

            return reval.res.popStringBuffer();
        }

        return "";
    }

    /**
     * Get the href (URL path) of this object within the application.
     *
     * @param action the action name, or null/undefined for the "main" action.
     *
     * @return ...
     */
    public Object jsFunction_href(Object action) {
        if (node == null) {
            return null;
        }

        String act = null;

        checkNode();

        if (action != null) {
            if (action instanceof Wrapper) {
                act = ((Wrapper) action).unwrap().toString();
            } else if (!(action instanceof Undefined)) {
                act = action.toString();
            }
        }

        String basicHref = core.app.getNodeHref(node, act);

        return core.postProcessHref(node, className, basicHref);
    }

    /**
     * Get a childObject by name/id or index
     *
     * @param id The name/id or index, depending if the argument is a String or Number.
     *
     * @return ...
     */
    public Object jsFunction_get(Object id) {
        if ((node == null) || (id == null)) {
            return null;
        }

        Object n = null;

        if (id instanceof Number) {
            n = node.getSubnodeAt(((Number) id).intValue());
        } else if (id != null) {
            n = node.getChildElement(id.toString());
        }

        if (n != null) {
            return Context.toObject(n, core.global);
        }

        return null;
    }

    /**
     * Get a child object by ID
     *
     * @param id the child id.
     *
     * @return ...
     */
    public Object jsFunction_getById(Object id) {
        if ((node == null) || (id == null) || id == Undefined.instance) {
            return null;
        }

        checkNode();

        String idString = (id instanceof Double) ?
                          Long.toString(((Double) id).longValue()) :
                          id.toString();
        Object n = node.getSubnode(idString);

        if (n == null) {
            return null;
        } else {
            return Context.toObject(n, core.global);
        }
    }

    /**
     * Set a property on this HopObject
     *
     * @param id The name/id or index, depending if the argument is a String or Number.
     *
     * @return ...
     */
    public boolean jsFunction_set(Object id, Object value) {
        if (id == Undefined.instance || value == Undefined.instance) {
            throw new EvaluatorException("HopObject.set() called with wrong number of arguments");
        }
        if ((node == null)) {
            return false;
        }

        checkNode();

        if (id instanceof Number) {

            if (!(value instanceof HopObject)) {
                throw new EvaluatorException("Can only set HopObjects as child objects in HopObject.set()");
            }

            int idx = (((Number) id).intValue());
            INode n = ((HopObject) value).getNode();

            node.addNode(n, idx);

        } else if (id != null) {
            put(id.toString(), this, value);
        }

        return true;
    }

    /**
     *
     *
     * @return ...
     */
    public int jsFunction_count() {
        if (node == null) {
            return 0;
        }

        checkNode();

        return node.numberOfNodes();
    }

    /**
     *
     *
     * @return ...
     */
    public int jsFunction_size() {
        return jsFunction_count();
    }

    /**
     *  Prefetch child objects from (relational) database.
     */
    public void jsFunction_prefetchChildren(Object startArg, Object lengthArg) {
        // check if we were called with no arguments
        if (startArg == Undefined.instance && lengthArg == Undefined.instance) {
            prefetchChildren(0, 1000);
        } else {
            int start = (int) ScriptRuntime.toNumber(startArg);
            int length = (int) ScriptRuntime.toNumber(lengthArg);
            prefetchChildren(start, length);
        }
    }

    private void prefetchChildren(int start, int length) {
        if (!(node instanceof helma.objectmodel.db.Node)) {
            return;
        }

        checkNode();

        try {
            ((helma.objectmodel.db.Node) node).prefetchChildren(start, length);
        } catch (Exception ignore) {
            System.err.println("Error in HopObject.prefetchChildren(): "+ignore);
        }
    }

    /**
     *  Clear the node's cache node.
     */
    public void jsFunction_clearCache() {
        checkNode();

        node.clearCacheNode();
    }

    /**
     * Return the full list of child objects in a JavaScript Array.
     * This is called by jsFunction_list() if called with no arguments.
     *
     * @return A JavaScript Array containing all child objects
     */
    private Scriptable list() {
        checkNode();

        // prefetchChildren(0, 1000);
        Enumeration e = node.getSubnodes();
        ArrayList a = new ArrayList();

        while ((e != null) && e.hasMoreElements()) {
            a.add(Context.toObject(e.nextElement(), core.global));
        }

        return Context.getCurrentContext().newArray(core.global, a.toArray());
    }

    /**
     *  Return a JS array of child objects with the given start and length.
     *
     * @return A JavaScript Array containing the specified child objects
     */
    public Scriptable jsFunction_list(Object startArg, Object lengthArg) {
        if (startArg == Undefined.instance && lengthArg == Undefined.instance) {
            return list();
        }

        int start = (int) ScriptRuntime.toNumber(startArg);
        int length = (int) ScriptRuntime.toNumber(lengthArg);

        if (start < 0 || length < 0) {
            throw new EvaluatorException("Arguments must not be negative in HopObject.list(start, length)");
        }

        checkNode();

        prefetchChildren(start, length);
        ArrayList a = new ArrayList();

        for (int i=start; i<start+length; i++) {
            INode n = node.getSubnodeAt(i);
            if (n != null) {
                a.add(Context.toObject(n, core.global));
            }
        }

        return Context.getCurrentContext().newArray(core.global, a.toArray());
    }

    /**
     *
     *
     * @param child ...
     *
     * @return ...
     */
    public boolean jsFunction_add(Object child) {
        if ((node == null) || (child == null)) {
            return false;
        }

        checkNode();

        if (child instanceof HopObject) {
            node.addNode(((HopObject) child).node);

            return true;
        } else if (child instanceof INode) {
            node.addNode((INode) child);

            return true;
        }

        return false;
    }

    /**
     *
     *
     * @param index ...
     * @param child ...
     *
     * @return ...
     */
    public boolean jsFunction_addAt(int index, Object child) {
        if (child == null) {
            return false;
        }

        checkNode();

        if (child instanceof HopObject) {
            node.addNode(((HopObject) child).node, index);

            return true;
        } else if (child instanceof INode) {
            node.addNode((INode) child, index);

            return true;
        }

        return false;
    }

    /**
     *  Remove this object from the database.
     */
    public boolean jsFunction_remove(Object arg) {
        // shield off usage of old deprecated version taking an argument
        if (arg != Undefined.instance) {
            System.err.println(" *************  WARNING  *************************");
            System.err.println(" The version of HopObject.remove(child) you were ");
            System.err.println(" trying to use has been deprecated. Please use ");
            System.err.println("      hopobj.removeChild(child)");
            System.err.println(" to remove a child object from a collection without");
            System.err.println(" deleting it, or ");
            System.err.println("      hopobj.remove()");
            System.err.println(" without argument to delete the object itself.");
            System.err.println(" *************************************************");
            throw new RuntimeException("Caught deprecated usage of HopObject.remove(child)");
        }

        checkNode();

        return node.remove();
    }

    /**
     *  Remove a child node from this node's collection without deleting
     *  it from the database.
     */
    public boolean jsFunction_removeChild(Object child) {

        checkNode();

        if (child instanceof HopObject) {
            HopObject hobj = (HopObject) child;

            if (hobj.node != null) {
                node.removeNode(hobj.node);
                return true;
            }
        }

        return false;
    }

    /**
     *  Invalidate the node itself or a subnode
     */
    public boolean jsFunction_invalidate(Object childId) {
        if (childId != null && node instanceof helma.objectmodel.db.Node) {
            if (childId == Undefined.instance) {

                if (node.getState() == INode.INVALID) {
                    return true;
                }

                ((helma.objectmodel.db.Node) node).invalidate();
            } else {

                checkNode();

                ((helma.objectmodel.db.Node) node).invalidateNode(childId.toString());
            }
        }

        return true;
    }

    /**
     *  Check if node is contained in subnodes
     */
    public int jsFunction_contains(Object obj) {
        if ((node != null) && obj instanceof HopObject) {
            checkNode();

            return node.contains(((HopObject) obj).node);
        }

        return -1;
    }

    /**
     *
     *
     * @param name ...
     * @param start ...
     * @param value ...
     */
    public void put(String name, Scriptable start, Object value) {
        if (node == null) {
            super.put(name, start, value);
        } else {

            checkNode();

            if ("subnodeRelation".equals(name)) {
                node.setSubnodeRelation(value == null ? null : value.toString());
            }

            if (value instanceof Wrapper) {
                value = ((Wrapper) value).unwrap();
            }

            if ((value == null) || (value == Undefined.instance)) {
                node.unset(name);
            } else if (value instanceof Scriptable) {
                Scriptable s = (Scriptable) value;

                if ("Date".equals(s.getClassName())) {
                    node.setDate(name, new Date((long) ScriptRuntime.toNumber(s)));
                } else if ("String".equals(s.getClassName())) {
                    node.setString(name, ScriptRuntime.toString(s));
                } else if ("Number".equals(s.getClassName())) {
                    node.setFloat(name, ScriptRuntime.toNumber(s));
                } else if ("Boolean".equals(s.getClassName())) {
                    node.setBoolean(name, ScriptRuntime.toBoolean(s));
                } else if (s instanceof MapWrapper) {
                    node.setJavaObject(name, ((MapWrapper) s).unwrap());
                } else {
                    node.setJavaObject(name, s);
                }
            } else if (value instanceof String) {
                node.setString(name, value.toString());
            } else if (value instanceof Boolean) {
                node.setBoolean(name, ((Boolean) value).booleanValue());
            } else if (value instanceof Number) {
                node.setFloat(name, ((Number) value).doubleValue());
            } else if (value instanceof Date) {
                node.setDate(name, (Date) value);
            } else if (value instanceof INode) {
                node.setNode(name, (INode) value);
            } else {
                node.setJavaObject(name, value);
            }
        }
    }

    /**
     *
     *
     * @param name ...
     * @param start ...
     *
     * @return ...
     */
    public boolean has(String name, Scriptable start) {
        if (node != null) {
            checkNode();
            return  (node.get(name) != null);
        } else {
            return super.has(name, start);
        }
    }

    /**
     *
     *
     * @param name ...
     */
    public void delete(String name) {
        if ((node != null)) {
            checkNode();
            node.unset(name);
        } else {
            super.delete(name);
        }
    }

    /**
     *
     *
     * @param name ...
     * @param start ...
     *
     * @return ...
     */
    public Object get(String name, Scriptable start) {
        // System.err.println("GET from "+this+": "+name+" ->"+super.get(name, start));
        Object retval = null;

        if (node == null) {
            return super.get(name, start);
        } else {
            // Note: we do not normally consult the prototype in get(),
            // but we do so here because calling get on the Node may trigger
            // db select statements (resulting in errors) and getting a
            // property on the parent prototype is much cheaper and safer.
            // NOTE: because of this, prototype inheritance is reversed for HopObjects!
            Scriptable prototype = getPrototype();

            while (prototype != null) {
                retval = prototype.get(name, this);
                if (retval != NOT_FOUND) {
                    return retval;
                }
                prototype = prototype.getPrototype();
            }
            return getFromNode(name);
        }
    }

    /**
     *  Retrieve a property only from the node itself, not the underlying prototype object.
     *  This is called directly when we call get(x) on a JS HopObject, since we don't want
     *  to return the prototype functions in that case.
     */
    private Object getFromNode(String name) {
        if (node != null && name != null && name.length() > 0) {

            checkNode();

            // Everything starting with an underscore is interpreted as internal property
            if (name.charAt(0) == '_') {
                return getInternalProperty(name);
            }

            if ("subnodeRelation".equals(name)) {
                return node.getSubnodeRelation();
            }

            IProperty p = node.get(name);

            if (p != null) {
                switch (p.getType()) {
                    case IProperty.STRING:
                    case IProperty.INTEGER:
                    case IProperty.FLOAT:
                    case IProperty.BOOLEAN:
                        return p.getValue();
                }

                Context cx = Context.getCurrentContext();

                if (p.getType() == IProperty.DATE) {
                    Date d = p.getDateValue();

                    if (d == null) {
                        return NOT_FOUND;
                    } else {
                        Object[] args = { new Long(d.getTime()) };
                        try {
                            return cx.newObject(core.global, "Date", args);
                        /* } catch (PropertyException px) {
                            return NOT_FOUND; */
                        /* } catch (NotAFunctionException nafx) {
                            return NOT_FOUND; */
                        } catch (JavaScriptException nafx) {
                            return NOT_FOUND;
                        }
                    }
                }

                if (p.getType() == IProperty.NODE) {
                    INode n = p.getNodeValue();

                    if (n == null) {
                        return NOT_FOUND;
                    } else {
                        return Context.toObject(n, core.global);
                    }
                }

                if (p.getType() == IProperty.JAVAOBJECT) {
                    Object obj = p.getJavaObjectValue();

                    if (obj == null) {
                        return NOT_FOUND;
                    } else {
                        return Context.toObject(obj, core.global);
                    }
                }
            }
        }

        return NOT_FOUND;
    }

    private Object getInternalProperty(String name) {
        if ("__id__".equals(name) || "_id".equals(name)) {
            return node.getID();
        }

        if ("__prototype__".equals(name) || "_prototype".equals(name)) {
            return node.getPrototype();
        }

        if ("__parent__".equals(name) || "_parent".equals(name)) {
            return core.getNodeWrapper(node.getParent());
        }

        // some more internal properties
        if ("__name__".equals(name)) {
            return node.getName();
        }

        if ("__fullname__".equals(name)) {
            return node.getFullName();
        }

        if ("__hash__".equals(name)) {
            return Integer.toString(node.hashCode());
        }

        if ("__node__".equals(name)) {
            return new NativeJavaObject(core.global, node, null);
        }

        if ("__created__".equalsIgnoreCase(name)) {
            return new Date(node.created());
        }

        if ("__lastmodified__".equalsIgnoreCase(name)) {
            return new Date(node.lastModified());
        }

        return NOT_FOUND;
    }

    /**
     *
     *
     * @return ...
     */
    public Object[] getIds() {
        if (node == null) {
            return new Object[0];
        }

        checkNode();

        Enumeration en = node.properties();
        ArrayList list = new ArrayList();

        while (en.hasMoreElements())
            list.add(en.nextElement());

        return list.toArray();
    }

    /**
     *
     *
     * @param idx ...
     * @param start ...
     *
     * @return ...
     */
    public boolean has(int idx, Scriptable start) {
        if (node != null) {
            checkNode();

            return (0 <= idx && idx < node.numberOfNodes());
        }

        return false;
    }

    /**
     *
     *
     * @param idx ...
     * @param start ...
     *
     * @return ...
     */
    public Object get(int idx, Scriptable start) {
        if (node != null) {
            checkNode();

            INode n = node.getSubnodeAt(idx);

            if (n != null) {
                return Context.toObject(n, core.global);
            }
        }

        return NOT_FOUND;
    }


    /**
     *
     *
     * @param hint ...
     *
     * @return ...
     */
    public Object getDefaultValue(Class hint) {
        return (className != null) ? ("[HopObject " + className + "]") : "[HopObject]";
    }

    /**
     *
     *
     * @return ...
     */
    public String toString() {
        return (className != null) ? ("[HopObject " + className + "]") : "[HopObject]";
    }
}
