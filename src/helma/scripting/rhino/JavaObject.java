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
import helma.objectmodel.*;
import helma.objectmodel.db.*;
import org.mozilla.javascript.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 */
public class JavaObject extends NativeJavaObject {

    RhinoCore core;
    Scriptable prototype;

    /**
     *  Creates a new JavaObject wrapper.
     */
    public JavaObject(Scriptable scope, Object obj, Scriptable prototype, RhinoCore core) {
        this.parent = scope;
        this.javaObject = obj;
        this.prototype = prototype;
        this.core = core;
        staticType = obj.getClass();
        initMembers();
    }


    /**
     *
     *
     * @param skin ...
     * @param param ...
     *
     * @return ...
     */
    public boolean renderSkin(Object skin, Object param) {
        // System.err.println ("RENDERSKIN CALLED WITH PARAM "+param);
        Context cx = Context.getCurrentContext();
        RequestEvaluator reval = (RequestEvaluator) cx.getThreadLocal("reval");
        Skin s;

        if (skin instanceof Skin) {
            s = (Skin) skin;
        } else {
            s = core.app.getSkin(javaObject, skin.toString(), null);
        }

        Map p = null;

        if ((param != null) && (param != Undefined.instance)) {
            p = new HashMap();

            if (param instanceof Scriptable) {
                Scriptable sp = (Scriptable) param;
                Object[] ids = sp.getIds();

                for (int i = 0; i < ids.length; i++)
                    p.put(ids[i], sp.get(ids[i].toString(), sp));
            }
        }

        if (s != null) {
            s.render(reval, javaObject, p);
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
    public String renderSkinAsString(Object skin, Object param) {
        // System.err.println ("RENDERSKINASSTRING CALLED WITH skin "+skin);
        Context cx = Context.getCurrentContext();
        RequestEvaluator reval = (RequestEvaluator) cx.getThreadLocal("reval");
        Skin s;

        if (skin instanceof Skin) {
            s = (Skin) skin;
        } else {
            s = core.app.getSkin(javaObject, skin.toString(), null);
        }

        Map p = null;

        if ((param != null) && (param != Undefined.instance)) {
            p = new HashMap();

            if (param instanceof Scriptable) {
                Scriptable sp = (Scriptable) param;
                Object[] ids = sp.getIds();

                for (int i = 0; i < ids.length; i++)
                    p.put(ids[i], sp.get(ids[i].toString(), sp));
            }
        }

        if (s != null) {
            reval.res.pushStringBuffer();
            s.render(reval, javaObject, p);

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
    public Object href(Object action) {
        if (javaObject == null) {
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

        return core.app.getNodeHref(javaObject, act);
    }

    public boolean has(String name, Scriptable start) {
        // System.err.println ("HAS: "+name);
        if (prototype.has(name, start))
            return true;
        return super.has(name, start);
    }

    public Object get(String name, Scriptable start) {
        // System.err.println ("GET: "+name);
        Object obj = prototype.get(name, start);
        if (obj != null && obj != UniqueTag.NOT_FOUND)
            return obj;

        Method[] m = JavaObject.class.getMethods();
        for (int i=0; i<m.length; i++) {
            if (name.equals(m[i].getName())) {
                obj =  new FunctionObject(name, m[i], this);
                // System.err.println ("GOT: "+obj);
                return obj;
            }
        }
      return super.get(name, start);
    }

}
