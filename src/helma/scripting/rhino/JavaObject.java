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
import org.mozilla.javascript.*;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.io.UnsupportedEncodingException;
import java.io.IOException;

/**
 *
 */
public class JavaObject extends NativeJavaObject {

    RhinoCore core;
    String protoName;
    NativeJavaObject unscriptedJavaObj;

    static HashMap overload;

    static {
        overload = new HashMap();
        Method[] m = JavaObject.class.getMethods();
        for (int i=0; i<m.length; i++) {
            if ("href".equals(m[i].getName()) ||
                "renderSkin".equals(m[i].getName()) ||
                "renderSkinAsString".equals(m[i].getName()) ||
                "getResource".equals(m[i].getName()) ||
                "getResources".equals(m[i].getName())) {
                overload.put(m[i].getName(), m[i]);
            }
        }
    }

    /**
     *  Creates a new JavaObject wrapper.
     */
    public JavaObject(Scriptable scope, Object obj,
            String protoName, Scriptable prototype, RhinoCore core) {
        this.parent = scope;
        this.javaObject = obj;
        this.protoName = protoName;
        this.core = core;
        staticType = obj.getClass();
        unscriptedJavaObj = new NativeJavaObject(scope, obj, staticType);
        setPrototype(prototype);
        initMembers();
    }

    /**
     *
     *
     * @param skinobj ...
     * @param paramobj ...
     *
     * @return ...
     */
    public boolean renderSkin(Object skinobj, Object paramobj)
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
            skin = engine.getSkin(protoName, skinobj.toString());
        }

        Map param = RhinoCore.getSkinParam(paramobj);

        if (skin != null) {
            skin.render(reval, javaObject, param);
        }

        return true;
    }

    /**
     *
     *
     * @param skinobj ...
     * @param paramobj ...
     *
     * @return ...
     */
    public String renderSkinAsString(Object skinobj, Object paramobj)
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
            skin = engine.getSkin(protoName, skinobj.toString());
        }

        Map param = RhinoCore.getSkinParam(paramobj);

        if (skin != null) {
            ResponseTrans res = reval.getResponse();
            res.pushStringBuffer();
            skin.render(reval, javaObject, param);
            return res.popStringBuffer();
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
    public Object href(Object action) throws UnsupportedEncodingException, 
                                             IOException {
        if (javaObject == null) {
            return null;
        }

        String act = null;

        if (action != null) {
            if (action instanceof Wrapper) {
                act = ((Wrapper) action).unwrap().toString();
            } else if (!(action instanceof Undefined)) {
                act = action.toString();
            }
        }

        String basicHref = core.app.getNodeHref(javaObject, act);

        return core.postProcessHref(javaObject, protoName, basicHref);
    }

    /**
     * Checks whether the given property is defined in this object.
     */
    public boolean has(String name, Scriptable start) {
        // System.err.println ("HAS: "+name);
        if (overload.containsKey(name)) {
            return true;
        }
        return super.has(name, start);
    }

    /** 
     * Get a named property from this object.
     */
    public Object get(String name, Scriptable start) {
        // System.err.println ("GET: "+name);
        Object obj;

        // we really are not supposed to walk down the prototype chain in get(),
        // but we break the rule in order to be able to override java methods,
        // which are looked up by super.get() below
        Scriptable proto = getPrototype();
        while (proto != null) {
            obj = proto.get(name, start);
            if (obj != NOT_FOUND) {
                return obj;
            }
            proto = proto.getPrototype();
        }

        obj = overload.get(name);
        if (obj != null) {
            return new FunctionObject(name, (Method) obj, this);
        }

        if ("_prototype".equals(name) || "__prototype__".equals(name)) {
            return protoName;
        }

        if ("__proto__".equals(name)) {
            return getPrototype();
        }

        if ("__javaObject__".equals(name)) {
            return unscriptedJavaObj;
        }

        return super.get(name, start);
    }

    /**
     * Returns a prototype's resource of a given name. Walks up the prototype's
     * inheritance chain if the resource is not found
     *
     * @param resourceName the name of the resource, e.g. "type.properties",
     *                     "messages.properties", "script.js", etc.
     * @return the resource, if found, null otherwise
     */
    public Object getResource(String resourceName) {
        Context cx = Context.getCurrentContext();
        RhinoEngine engine = (RhinoEngine) cx.getThreadLocal("engine");
        Prototype prototype = engine.core.app.getPrototypeByName(protoName);
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
    public Object getResources(String resourceName) {
        Context cx = Context.getCurrentContext();
        RhinoEngine engine = (RhinoEngine) cx.getThreadLocal("engine");
        Prototype prototype = engine.core.app.getPrototypeByName(protoName);
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

}
