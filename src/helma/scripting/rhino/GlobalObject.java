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
import helma.util.HtmlEncoder;
import org.mozilla.javascript.*;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 */
public class GlobalObject extends ScriptableObject {
    Application app;
    RhinoCore core;

    /**
     * Creates a new GlobalObject object.
     *
     * @param core ...
     * @param app ...
     *
     * @throws PropertyException ...
     */
    public GlobalObject(RhinoCore core, Application app, Context cx)
                 throws PropertyException {
        this.core = core;
        this.app = app;

        String[] globalFuncs = {
                                   "renderSkin", "renderSkinAsString", "getProperty",
                                   "authenticate", "createSkin", "format", "encode",
                                   "encodeXml", "encodeForm", "stripTags",
                                   "getDBConnection"
                               };

        defineFunctionProperties(globalFuncs, GlobalObject.class, 0);
        put("app", this, cx.toObject(new ApplicationBean(app), this));
        put("Xml", this, cx.toObject(new XmlObject(core), this));
    }

    /**
     *
     *
     * @return ...
     */
    public String getClassName() {
        return "GlobalObject";
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
            s = core.app.getSkin(null, skin.toString(), null);
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
            s.render(reval, null, p);
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
        Context cx = Context.getCurrentContext();
        RequestEvaluator reval = (RequestEvaluator) cx.getThreadLocal("reval");
        Skin s;

        if (skin instanceof Skin) {
            s = (Skin) skin;
        } else {
            s = core.app.getSkin(null, skin.toString(), null);
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
            s.render(reval, null, p);

            return reval.res.popStringBuffer();
        }

        return "";
    }

    /**
     *
     *
     * @param propname ...
     *
     * @return ...
     */
    public String getProperty(String propname) {
        return app.getProperty(propname);
    }

    /**
     *
     *
     * @param user ...
     * @param pwd ...
     *
     * @return ...
     */
    public boolean authenticate(String user, String pwd) {
        return app.authenticate(user, pwd);
    }

    /**
     *
     *
     * @param str ...
     *
     * @return ...
     */
    public Skin createSkin(String str) {
        return new Skin(str, app);
    }

    public DatabaseObject getDBConnection(String dbsource) throws Exception {
        if (dbsource == null)
            throw new RuntimeException ("Wrong number of arguments in getDBConnection(dbsource)");
        DbSource dbsrc = app.getDbSource (dbsource.toLowerCase ());
        if (dbsrc == null)
            throw new RuntimeException ("DbSource "+dbsource+" does not exist");
        DatabaseObject db = new DatabaseObject (dbsrc, 0);
        return db;
    }


    /**
     *
     *
     * @param str ...
     *
     * @return ...
     */
    public String encode(String str) {
        return HtmlEncoder.encodeAll(str);
    }

    /**
     *
     *
     * @param str ...
     *
     * @return ...
     */
    public String encodeXml(String str) {
        return HtmlEncoder.encodeXml(str);
    }

    /**
     *
     *
     * @param str ...
     *
     * @return ...
     */
    public String encodeForm(String str) {
        return HtmlEncoder.encodeFormValue(str);
    }

    /**
     *
     *
     * @param str ...
     *
     * @return ...
     */
    public String format(String str) {
        return HtmlEncoder.encode(str);
    }

    /**
     *
     *
     * @param str ...
     *
     * @return ...
     */
    public String stripTags(String str) {
        if (str == null) {
            return null;
        }

        char[] c = str.toCharArray();
        boolean inTag = false;
        int i;
        int j = 0;

        for (i = 0; i < c.length; i++) {
            if (c[i] == '<') {
                inTag = true;
            }

            if (!inTag) {
                if (i > j) {
                    c[j] = c[i];
                }

                j++;
            }

            if (c[i] == '>') {
                inTag = false;
            }
        }

        if (i > j) {
            return new String(c, 0, j);
        }

        return str;
    }
}
