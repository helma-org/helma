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
import helma.objectmodel.*;
import helma.objectmodel.db.*;
import helma.util.HtmlEncoder;
import helma.util.MimePart;
import org.mozilla.javascript.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.*;
import java.io.*;

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
                                   "getDBConnection", "getURL"
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


    public Object getURL(String location, Object opt) {
            if (location ==  null) {
                return null;
            }

            try {
                URL url = new URL(location);
                URLConnection con = url.openConnection();

                // do we have if-modified-since or etag headers to set?
                if (opt != null && opt != Undefined.instance) {
                    if (opt instanceof Scriptable) {
                        Scriptable scr = (Scriptable) opt;
                        if ("Date".equals(scr.getClassName())) {
                            Date date = new Date((long) ScriptRuntime.toNumber(scr));

                            con.setIfModifiedSince(date.getTime());

                            SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz",
                                                                       Locale.UK);

                            format.setTimeZone(TimeZone.getTimeZone("GMT"));
                            con.setRequestProperty("If-Modified-Since", format.format(date));
                        }else {
                            con.setRequestProperty("If-None-Match", scr.toString());
                        }
                    } else {
                        con.setRequestProperty("If-None-Match", opt.toString());
                    }
                }

                String httpUserAgent = app.getProperty("httpUserAgent");

                if (httpUserAgent != null) {
                    con.setRequestProperty("User-Agent", httpUserAgent);
                }

                con.setAllowUserInteraction(false);

                String filename = url.getFile();
                String contentType = con.getContentType();
                long lastmod = con.getLastModified();
                String etag = con.getHeaderField("ETag");
                int length = con.getContentLength();
                int resCode = 0;

                if (con instanceof HttpURLConnection) {
                    resCode = ((HttpURLConnection) con).getResponseCode();
                }

                ByteArrayOutputStream body = new ByteArrayOutputStream();

                if ((length != 0) && (resCode != 304)) {
                    InputStream in = new BufferedInputStream(con.getInputStream());
                    byte[] b = new byte[1024];
                    int read;

                    while ((read = in.read(b)) > -1)
                        body.write(b, 0, read);

                    in.close();
                }

                MimePart mime = new MimePart(filename, body.toByteArray(), contentType);

                if (lastmod > 0) {
                    mime.lastModified = new Date(lastmod);
                }

                mime.eTag = etag;

                return Context.toObject(mime, this);
            } catch (Exception ignore) {
                System.err.println ("EXCEPT: "+ignore);
            }

            return null;
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
