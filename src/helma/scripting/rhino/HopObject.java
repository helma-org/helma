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
import helma.framework.*;
import helma.framework.core.*;
import helma.objectmodel.*;
import helma.objectmodel.db.*;
import org.mozilla.javascript.*;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 */
public class HopObject extends ScriptableObject {
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
     *  This method is used as HopObject constructor from JavaScript.
     */
    public static Object jsConstructor(Context cx, Object[] args,
                                              Function ctorObj, boolean inNewExpr)
                         throws JavaScriptException, ScriptingException {
        RhinoEngine engine = (RhinoEngine) cx.getThreadLocal("engine");
        RhinoCore c = engine.core;
        String prototype = ((FunctionObject) ctorObj).getFunctionName();

        // if this is a java object prototype, create a new java object
        // of the given class instead of a HopObject.
        if (c.app.isJavaPrototype(prototype)) {
            String classname = c.app.getJavaClassForPrototype(prototype);
            try {
                Class clazz = Class.forName(classname);
                Constructor[] cnst = clazz.getConstructors();
                // brute force loop through constructors -
                // alas, this isn't very pretty.
                for (int i=0; i<cnst.length; i++) try {
                    FunctionObject fo = new FunctionObject("ctor", cnst[i], engine.global);
                    Object obj = fo.call(cx, engine.global, null, args);
                    return cx.toObject(obj, engine.global);
                } catch (JavaScriptException x) {
                    Object value = x.getValue();
                    if (value instanceof Throwable) {
                        ((Throwable) value).printStackTrace();
                    }
                    throw new JavaScriptException("Error in Java constructor: "+value);
                } catch (Exception ignore) {
                    // constructor arguments didn't match
                }
                throw new ScriptingException("No matching Java Constructor found in "+clazz);
            } catch (Exception x) {
                System.err.println("Error in Java constructor: "+x);
                throw new JavaScriptException(x);
            }
        } else {
            INode n = new helma.objectmodel.db.Node(prototype, prototype,
                                                c.app.getWrappedNodeManager());
            HopObject hobj = new HopObject(prototype);

            hobj.init(c, n);
            Scriptable p = c.getPrototype(prototype);
            if (p != null) {
                hobj.setPrototype(p);
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
     *
     *
     * @return ...
     */
    public Object jsGet_cache() {
        if (node == null) {
            return null;
        }

        return node.getCacheNode();
    }

    /**
     *
     *
     * @param skin ...
     * @param param ...
     *
     * @return ...
     */
    public boolean jsFunction_renderSkin(Object skin, Object param) {
        // System.err.println ("RENDERSKIN CALLED WITH PARAM "+param);
        Context cx = Context.getCurrentContext();
        RequestEvaluator reval = (RequestEvaluator) cx.getThreadLocal("reval");
        Skin s;

        if (skin instanceof Skin) {
            s = (Skin) skin;
        } else {
            // retrieve res.skinpath, an array of objects that tell us where to look for skins
            // (strings for directory names and INodes for internal, db-stored skinsets)
            Object[] skinpath = reval.res.getSkinpath();
            RhinoCore.unwrapSkinpath(skinpath);
            s = core.app.getSkin(node, skin.toString(), skinpath);
        }

        Map p = null;

        if ((param != null) && (param != Undefined.instance)) {
            p = new HashMap();

            if (param instanceof Scriptable) {
                Scriptable sp = (Scriptable) param;
                Object[] ids = sp.getIds();

                for (int i = 0; i < ids.length; i++) {
                    Object obj = sp.get(ids[i].toString(), sp);
                    if (obj instanceof NativeJavaObject) {
                        p.put(ids[i], ((NativeJavaObject) obj).unwrap());
                    } else {
                        p.put(ids[i], obj);
                    }
                }
            }
        }

        if (s != null) {
            s.render(reval, node, p);
        }

        return true;
    }

    /**
     *
     *
     * @param skin ...
     * @param param ...
     *
     * @return ...
     */
    public String jsFunction_renderSkinAsString(Object skin, Object param) {
        // System.err.println ("RENDERSKIN CALLED WITH PARAM "+param);
        Context cx = Context.getCurrentContext();
        RequestEvaluator reval = (RequestEvaluator) cx.getThreadLocal("reval");
        Skin s;

        if (skin instanceof Skin) {
            s = (Skin) skin;
        } else {
            // retrieve res.skinpath, an array of objects that tell us where to look for skins
            // (strings for directory names and INodes for internal, db-stored skinsets)
            Object[] skinpath = reval.res.getSkinpath();
            RhinoCore.unwrapSkinpath(skinpath);
            s = core.app.getSkin(node, skin.toString(), skinpath);
        }

        Map p = null;

        if ((param != null) && (param != Undefined.instance)) {
            p = new HashMap();

            if (param instanceof Scriptable) {
                Scriptable sp = (Scriptable) param;
                Object[] ids = sp.getIds();

                for (int i = 0; i < ids.length; i++) {
                    Object obj = sp.get(ids[i].toString(), sp);
                    if (obj instanceof NativeJavaObject) {
                        p.put(ids[i], ((NativeJavaObject) obj).unwrap());
                    } else {
                        p.put(ids[i], obj);
                    }
                }
            }
        }

        if (s != null) {
            reval.res.pushStringBuffer();
            s.render(reval, node, p);

            return reval.res.popStringBuffer();
        }

        return "";
    }

    /**
     *
     *
     * @param action ...
     *
     * @return ...
     */
    public Object jsFunction_href(Object action) {
        if (node == null) {
            return null;
        }

        String act = null;

        if (action != null) {
            if (action instanceof NativeJavaObject) {
                act = ((NativeJavaObject) action).unwrap().toString();
            } else if (!(action instanceof Undefined)) {
                act = action.toString();
            }
        }

        return core.app.getNodeHref(node, act);
    }

    /**
     *
     *
     * @param id ...
     *
     * @return ...
     */
    public Object jsFunction_get(Object id) {
        if ((node == null) || (id == null)) {
            return null;
        }

        Context cx = Context.getCurrentContext();
        Object n = null;

        if (id instanceof Number) {
            n = node.getSubnodeAt(((Number) id).intValue());
        } else {
            n = node.getChildElement(id.toString());
        }

        if (n == null) {
            return null;
        } else {
            return cx.toObject(n, core.global);
        }
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
    public void jsFunction_prefetchChildren() {
        jsFunction_prefetchChildren(0, 1000);
    }

    /**
     *  Prefetch child objects from (relational) database.
     */
    public void jsFunction_prefetchChildren(int start, int length) {
        if (!(node instanceof helma.objectmodel.db.Node)) {
            return;
        }

        try {
            ((helma.objectmodel.db.Node) node).prefetchChildren(start, length);
        } catch (Exception x) {
        }
    }

    /**
     *  Clear the node's cache node.
     */
    public void jsFunction_clearCache() {
        node.clearCacheNode();
    }

    /**
     *
     *
     * @return ...
     */
    public Object[] jsFunction_list() {
        int l = node.numberOfNodes();
        Enumeration e = node.getSubnodes();
        ArrayList a = new ArrayList();

        while ((e != null) && e.hasMoreElements()) {
            a.add(e.nextElement());
        }

        return a.toArray();
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

        if (child instanceof HopObject) {
            INode added = node.addNode(((HopObject) child).node);

            return true;
        } else if (child instanceof INode) {
            INode added = node.addNode((INode) child);

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

        if (child instanceof HopObject) {
            INode added = node.addNode(((HopObject) child).node, index);

            return true;
        } else if (child instanceof INode) {
            INode added = node.addNode((INode) child, index);

            return true;
        }

        return false;
    }

    /**
     *  Remove node itself or one or more subnodes.
     */
    public boolean jsFunction_remove(Object child) {
        // semantics: if called without arguments, remove self.
        // otherwise, remove given subnodes.
        if (child == null) {
            return node.remove();
        } else if (node != null) {
            try {
                if (child instanceof HopObject) {
                    HopObject hobj = (HopObject) child;

                    if (hobj.node != null) {
                        node.removeNode(hobj.node);

                        return true;
                    }
                }
            } catch (Exception x) {
                System.err.println("XXX: " + x);
                x.printStackTrace();
            }
        }

        return false;
    }

    /**
     *  Invalidate the node itself or a subnode
     */
    public boolean jsFunction_invalidate(String childId) {
        if (node instanceof helma.objectmodel.db.Node) {
            if (childId == null) {
                ((helma.objectmodel.db.Node) node).invalidate();
            } else {
                ((helma.objectmodel.db.Node) node).invalidateNode(childId);
            }
        }

        return true;
    }

    /**
     *  Check if node is contained in subnodes
     */
    public int jsFunction_contains(Object obj) {
        if ((node != null) && (obj != null) && obj instanceof HopObject) {
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
            if (value instanceof NativeJavaObject) {
                value = ((NativeJavaObject) value).unwrap();
            }

            if ((value == null) || (value == Undefined.instance)) {
                node.unset(name);
            } else if (value instanceof Scriptable) {
                Scriptable s = (Scriptable) value;

                if ("Date".equals(s.getClassName())) {
                    node.setDate(name, new Date((long) ScriptRuntime.toNumber(s)));
                } else if ("String".equals(s.getClassName())) {
                    node.setString(name, ScriptRuntime.toString(s));
                } else if (s instanceof MapWrapper) {
                    node.setJavaObject(name, ((MapWrapper) s).unwrap());
                } else if (s instanceof HopObject) {
                    node.setNode(name, ((HopObject) value).node);
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
            } else if (value instanceof HopObject) {
                node.setNode(name, ((HopObject) value).node);
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
        if (super.has(name, start)) {
            return true;
        }

        if ((prototype != null) && prototype.has(name, start)) {
            return true;
        }

        if ((node != null) && (node.get(name) != null)) {
            return true;
        }

        return false;
    }

    /**
     *
     *
     * @param name ...
     */
    public void delete(String name) {
        if ((node != null)) {
            node.unset(name);
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
        // System.err.println ("GET from "+node+": "+name);
        Object retval = super.get(name, start);

        if (retval != ScriptableObject.NOT_FOUND) {
            return retval;
        }

        if (prototype != null) {
            retval = prototype.get(name, start);
        }

        if (retval != ScriptableObject.NOT_FOUND) {
            return retval;
        }

        if (node != null) {
            // Everything starting with an underscore is interpreted as internal property
            if (name.charAt(0) == '_') {
                return getInternalProperty(name);
            }

            IProperty p = node.get(name);

            if (p != null) {
                if (p.getType() == IProperty.STRING) {
                    return p.getStringValue();
                }

                if (p.getType() == IProperty.BOOLEAN) {
                    return p.getBooleanValue() ? Boolean.TRUE : Boolean.FALSE;
                }

                if (p.getType() == IProperty.INTEGER) {
                    return new Long(p.getIntegerValue());
                }

                if (p.getType() == IProperty.FLOAT) {
                    return new Double(p.getFloatValue());
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
                        } catch (PropertyException px) {
                            return NOT_FOUND;
                        } catch (NotAFunctionException nafx) {
                            return NOT_FOUND;
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
                        return cx.toObject(n, core.global);
                    }
                }

                if (p.getType() == IProperty.JAVAOBJECT) {
                    Object obj = p.getJavaObjectValue();

                    if (obj == null) {
                        return NOT_FOUND;
                    } else {
                        return cx.toObject(obj, core.global);
                    }
                }
            }

            // as last resort, try to get property as anonymous subnode
            Object anon = node.getChildElement(name);

            if (anon != null) {
                return anon;
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
            return node;
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

        Enumeration enum = node.properties();
        ArrayList list = new ArrayList();

        while (enum.hasMoreElements())
            list.add(enum.nextElement());

        return list.toArray();
    }

    /**
     *
     *
     * @param name ...
     * @param start ...
     *
     * @return ...
     */
    public boolean has(int idx, Scriptable start) {
        if (node != null) {
            return (0 <= idx && idx < node.numberOfNodes());
        }

        return false;
    }

    /**
     *
     *
     * @param name ...
     * @param start ...
     *
     * @return ...
     */
    public Object get(int idx, Scriptable start) {
        if (node != null) {
            INode n = node.getSubnodeAt(idx);

            if (n == null) {
                return NOT_FOUND;
            } else {
                Context cx = Context.getCurrentContext();
                return cx.toObject(n, core.global);
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
