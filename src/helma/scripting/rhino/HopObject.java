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

import helma.framework.core.*;
import helma.framework.ResponseTrans;
import helma.framework.repository.Resource;
import helma.objectmodel.*;
import helma.objectmodel.db.*;
import org.mozilla.javascript.*;

import java.lang.reflect.Method;
import java.util.*;
import java.io.UnsupportedEncodingException;
import java.io.IOException;

/**
 *
 */
public class HopObject extends ScriptableObject implements Wrapper, PropertyRecorder {

    String className;
    INode node;
    RhinoCore core;

    // fields to implement PropertyRecorder
    private boolean isRecording = false;
    private HashSet changedProperties;

    /**
     * Creates a new HopObject prototype.
     *
     * @param className the prototype name
     * @param core the RhinoCore
     */
    protected HopObject(String className, RhinoCore core) {
        this.className = className;
        this.core = core;
        setParentScope(core.global);
    }


    /**
     * Creates a new HopObject.
     *
     * @param className the className
     * @param proto the object's prototype
     */
    protected HopObject(String className, RhinoCore core,
                        INode node, Scriptable proto) {
        this(className, core);
        this.node = node;
        setPrototype(proto);
    }

    /**
     * Initialize HopObject prototype for Rhino scope.
     *
     * @param core the RhinoCore
     * @return the HopObject prototype
     * @throws PropertyException
     */
    public static HopObject init(RhinoCore core)
            throws PropertyException {
        int attributes = DONTENUM | PERMANENT;

        // create prototype object
        HopObject proto = new HopObject("HopObject", core);
        proto.setPrototype(getObjectPrototype(core.global));

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
     *
     *
     * @return ...
     */
    public String getClassName() {
        return className;
    }

    /**
     * Return a primitive representation for this object.
     * FIXME: We always return a string representation.
     *
     * @param hint the type hint
     * @return the default value for the object
     */
    public Object getDefaultValue(Class hint) {
        return toString();
    }

    /**
     *  Return the INode wrapped by this HopObject.
     *
     * @return the wrapped INode instance
     */
    public INode getNode() {
        if (node != null) {
            checkNode();
        }
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
    private void checkNode() {
        if (node != null && node.getState() == INode.INVALID) {
            if (node instanceof helma.objectmodel.db.Node) {
                NodeHandle handle = ((helma.objectmodel.db.Node) node).getHandle();
                node = handle.getNode(core.app.getWrappedNodeManager());
                if (node == null) {
                    // we probably have a deleted node. Replace with empty transient node
                    // to avoid throwing an exception.
                    node = new helma.objectmodel.TransientNode();
                    // throw new RuntimeException("Tried to access invalid/removed node " + handle + ".");
                }
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

        INode cache = node.getCacheNode();
        if (cache != null) {
            return Context.toObject(node.getCacheNode(), core.global);
        }

        return null;
    }

    /**
     * Render a skin to the response buffer.
     *
     * @param skinobj The skin object or name
     * @param paramobj An optional parameter object
     *
     * @return ...
     */
    public boolean jsFunction_renderSkin(Object skinobj, Object paramobj)
            throws UnsupportedEncodingException, IOException {
        Context cx = Context.getCurrentContext();
        RequestEvaluator reval = (RequestEvaluator) cx.getThreadLocal("reval");
        RhinoEngine engine = (RhinoEngine) cx.getThreadLocal("engine");
        Skin skin;

        if (skinobj instanceof Wrapper) {
            skinobj = ((Wrapper) skinobj).unwrap();
        }

        if (skinobj instanceof Skin) {
            skin = (Skin) skinobj;
        } else {
            skin = engine.getSkin(className, skinobj.toString());
        }

        Map param = RhinoCore.getSkinParam(paramobj);

        checkNode();

        if (skin != null) {
            skin.render(reval, node, param);
        }

        return true;
    }

    /**
     * Returns a prototype's resource of a given name. Walks up the prototype's
     * inheritance chain if the resource is not found
     *
     * @param resourceName the name of the resource, e.g. "type.properties",
     *                     "messages.properties", "script.js", etc.
     * @return the resource, if found, null otherwise
     */
    public Object jsFunction_getResource(String resourceName) {
        Context cx = Context.getCurrentContext();
        RhinoEngine engine = (RhinoEngine) cx.getThreadLocal("engine");
        Prototype prototype = engine.core.app.getPrototypeByName(className);
        while (prototype != null) {
            Resource[] resources = prototype.getResources();
            for (int i = resources.length - 1; i >= 0; i--) {
                Resource resource = resources[i];
                if (resource.exists() && resource.getShortName().equals(resourceName))
                    return Context.toObject(resource, core.global);
            }
            prototype =  prototype.getParentPrototype();
        }
        return null;
    }


    /**
     * Returns an array containing the prototype's resource with a given name.
     *
     * @param resourceName the name of the resource, e.g. "type.properties",
     *                     "messages.properties", "script.js", etc.
     * @return an array of resources with the given name
     */
    public Object jsFunction_getResources(String resourceName) {
        Context cx = Context.getCurrentContext();
        RhinoEngine engine = (RhinoEngine) cx.getThreadLocal("engine");
        Prototype prototype = engine.core.app.getPrototypeByName(className);
        ArrayList a = new ArrayList();
        while (prototype != null) {
            Resource[] resources = prototype.getResources();
            for (int i = resources.length - 1; i >= 0; i--) {
                Resource resource = resources[i];
                if (resource.exists() && resource.getShortName().equals(resourceName))
                    a.add(Context.toObject(resource, core.global));
            }
            prototype =  prototype.getParentPrototype();
        }
        return Context.getCurrentContext().newArray(core.global, a.toArray());
    }

    /**
     *  Render a skin and return its output as string.
     *
     * @param skinobj The skin object or name
     * @param paramobj An optional parameter object
     *
     * @return ...
     */
    public String jsFunction_renderSkinAsString(Object skinobj, Object paramobj)
            throws UnsupportedEncodingException, IOException {
        Context cx = Context.getCurrentContext();
        RequestEvaluator reval = (RequestEvaluator) cx.getThreadLocal("reval");
        RhinoEngine engine = (RhinoEngine) cx.getThreadLocal("engine");
        Skin skin;

        if (skinobj instanceof Wrapper) {
            skinobj = ((Wrapper) skinobj).unwrap();
        }

        if (skinobj instanceof Skin) {
            skin = (Skin) skinobj;
        } else {
            skin = engine.getSkin(className, skinobj.toString());
        }

        Map param = RhinoCore.getSkinParam(paramobj);

        checkNode();

        if (skin != null) {
            ResponseTrans res = reval.getResponse();
            res.pushStringBuffer();
            skin.render(reval, node, param);
            return res.popStringBuffer();
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
    public Object jsFunction_href(Object action) throws UnsupportedEncodingException,
                                                        IOException {
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

        checkNode();

        if (id instanceof Number) {
            n = node.getSubnodeAt(((Number) id).intValue());
        } else {
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

        if (id instanceof Number) {

            if (!(value instanceof HopObject)) {
                throw new EvaluatorException("Can only set HopObjects as child objects in HopObject.set()");
            }

            checkNode();

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
            Object obj = e.nextElement();
            if (obj != null)
                a.add(Context.toObject(obj, core.global));
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
     * Makes the HopObject and all its reachable descendants persistent.
     *
     * @return the ID of the newly persisted HopObject or null if operation failed
     */
    public Object jsFunction_persist() {

        checkNode();

        if (node instanceof helma.objectmodel.db.Node) {
            ((helma.objectmodel.db.Node) node).persist();
            return node.getID();
        }
        return null;
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

        checkNode();

        if ((node != null) && obj instanceof HopObject) {
            checkNode();

            return node.contains(((HopObject) obj).node);
        }

        return -1;
    }

    /**
     * Set a property in this HopObject
     *
     * @param name property name
     * @param start
     * @param value ...
     */
    public void put(String name, Scriptable start, Object value) {
        if (node == null) {
            // redirect the scripted constructor to __constructor__,
            // constructor is set to the native constructor method.
            if ("constructor".equals(name) && value instanceof NativeFunction) {
                name = "__constructor__";
            }
            // register property for PropertyRecorder interface
            if (isRecording) {
                changedProperties.add(name);
            }
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
                String className = s.getClassName();
                if ("Date".equals(className)) {
                    node.setDate(name, new Date((long) ScriptRuntime.toNumber(s)));
                } else if ("String".equals(className)) {
                    node.setString(name, ScriptRuntime.toString(s));
                } else if ("Number".equals(className)) {
                    node.setFloat(name, ScriptRuntime.toNumber(s));
                } else if ("Boolean".equals(className)) {
                    node.setBoolean(name, ScriptRuntime.toBoolean(s));
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
     * Check if a property is set in this HopObject
     *
     * @param name the property name
     * @param start the object in which the lookup began
     * @return true if the property was found
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
        if (node == null) {
            return super.get(name, start);
        } else {
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

            // Property names starting with an underscore is interpreted
            // as internal properties
            if (name.charAt(0) == '_') {
                Object value = getInternalProperty(name);
                if (value != NOT_FOUND)
                    return value;
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
                        return null;
                    } else {
                        Object[] args = { new Long(d.getTime()) };
                        try {
                            return cx.newObject(core.global, "Date", args);
                        } catch (JavaScriptException nafx) {
                            return null;
                        }
                    }
                }

                if (p.getType() == IProperty.NODE) {
                    INode n = p.getNodeValue();

                    if (n == null) {
                        return null;
                    } else {
                        return Context.toObject(n, core.global);
                    }
                }

                if (p.getType() == IProperty.JAVAOBJECT) {
                    Object obj = p.getJavaObjectValue();

                    if (obj == null) {
                        return null;
                    } else {
                        return Context.toObject(obj, core.global);
                    }
                }
            }

            DbMapping dbmap = node.getDbMapping();
            if (dbmap != null && dbmap.propertyToRelation(name) != null) {
                return null;
            }
        }

        return NOT_FOUND;
    }

    private Object getInternalProperty(String name) {
        if ("__id__".equals(name) || "_id".equals(name)) {
            return node.getID();
        }

        if ("__proto__".equals(name)) {
            return getPrototype(); // prototype object
        }

        if ("__prototype__".equals(name) || "_prototype".equals(name)) {
            return node.getPrototype(); // prototype name
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
            // HopObject prototypes always return an empty array in order not to
            // pollute actual HopObjects properties. Call getAllIds() to get
            // a list of properties from a HopObject prototype.
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
     * Return a string representation of this object
     *
     * @return ...
     */
    public String toString() {
        return (className != null) ? ("[HopObject " + className + "]") : "[HopObject]";
    }

    /**
     * Tell this PropertyRecorder to start recording changes to properties
     */
    public void startRecording() {
        changedProperties = new HashSet();
        isRecording = true;
    }

    /**
     * Tell this PropertyRecorder to stop recording changes to properties
     */
    public void stopRecording() {
        isRecording = false;
    }

    /**
     * Returns a set containing the names of properties changed since
     * the last time startRecording() was called.
     *
     * @return a Set containing the names of changed properties
     */
    public Set getChangeSet() {
        return changedProperties;
    }

    /**
     * Clear the set of changed properties.
     */
    public void clearChangeSet() {
        changedProperties = null;
    }

    /**
     * This method represents the Java-Script-exposed function for updating Subnode-Collections.
     * The following conditions must be met to make a subnodecollection updateable.
     * .) the collection must be specified with collection.updateable=true
     * .) the id's of this collection must be in ascending order, meaning, that new records
     *    do have a higher id than the last record loaded by this collection
     */
    public int jsFunction_update() {
        if (!(node instanceof helma.objectmodel.db.Node))
            throw new RuntimeException ("update only callabel on persistent HopObjects");
        checkNode();
        helma.objectmodel.db.Node n = (helma.objectmodel.db.Node) node;
        return n.updateSubnodes();
    }

    /**
     * Retrieve a view having a different order from this Node's subnodelist.
     * The underlying OrderedSubnodeList will keep those views and updates them
     * if the original collection has been updated.
     * @param expr the order (like sql-order using the properties instead)
     * @return ListViewWrapper holding the information of the ordered view
     */
    public Object jsFunction_getOrderedView(String expr) {
        if (!(node instanceof helma.objectmodel.db.Node)) {
            throw new RuntimeException (
                    "getOrderedView only callable on persistent HopObjects");
        }
        helma.objectmodel.db.Node n = (helma.objectmodel.db.Node) node;
        n.loadNodes();
        SubnodeList subnodes = n.getSubnodeList();
        if (subnodes == null) {
            throw new RuntimeException (
                    "getOrderedView only callable on already existing subnode-collections");
        }
        return new ListViewWrapper (subnodes.getOrderedView(expr),
                    core, core.app.getWrappedNodeManager(), this);
    }
}
