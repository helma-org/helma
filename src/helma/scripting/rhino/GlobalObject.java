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
import helma.framework.ResponseTrans;
import helma.objectmodel.db.*;
import helma.util.HtmlEncoder;
import helma.util.MimePart;
import helma.util.XmlUtils;
import org.mozilla.javascript.*;
import org.mozilla.javascript.serialize.*;

import java.util.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.*;
import java.io.*;

/**
 *
 */
public class GlobalObject extends ImporterTopLevel implements PropertyRecorder {
    Application app;
    RhinoCore core;

    // fields to implement PropertyRecorder
    private boolean isRecording = false;
    private HashSet changedProperties;

    /**
     * Creates a new GlobalObject object.
     *
     * @param core ...
     * @param app ...
     */
    public GlobalObject(RhinoCore core, Application app) {
        this.core = core;
        this.app = app;
    }

    /**
     * Initializes the global object. This is only done for the shared
     * global objects not the per-thread ones.
     *
     * @throws PropertyException ...
     */
    public void init() throws PropertyException {
        String[] globalFuncs = {
                                   "renderSkin", "renderSkinAsString", "getProperty",
                                   "authenticate", "createSkin", "format", "encode",
                                   "encodeXml", "encodeForm", "stripTags", "formatParagraphs",
                                   "getXmlDocument", "getHtmlDocument", "seal",
                                   "getDBConnection", "getURL", "write", "writeln",
                                   "serialize", "deserialize", "defineLibraryScope"
                               };

        defineFunctionProperties(globalFuncs, GlobalObject.class, 0);
        put("app", this, Context.toObject(new ApplicationBean(app), this));
        put("Xml", this, Context.toObject(new XmlObject(core), this));
        // Define dontEnum() on Object prototype
        String[] objFuncs = { "dontEnum" };
        ScriptableObject objproto = (ScriptableObject) getObjectPrototype(this);
        objproto.defineFunctionProperties(objFuncs, GlobalObject.class, 
                    DONTENUM | READONLY | PERMANENT);
    }
    
    /**
     * Get the global object's class name
     *
     * @return the class name for the global object
     */
    public String getClassName() {
        return "GlobalObject";
    }

    /**
     * Override ScriptableObject.get() to synchronize it.
     *
     * @param name
     * @param start
     * @return the property for the given name
     */
    public synchronized Object get(String name, Scriptable start) {
        return super.get(name, start);
    }

    /**
     * Override ScriptableObject.put() to implement PropertyRecorder interface
     * and to synchronize method.
     *
     * @param name
     * @param start
     * @param value
     */
    public synchronized void put(String name, Scriptable start, Object value) {
        // register property for PropertyRecorder interface
        if (isRecording) {
            changedProperties.add(name);
        }
        super.put(name, start, value);
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
            skin = engine.getSkin("global", skinobj.toString());
        }

        Map param = RhinoCore.getSkinParam(paramobj);

        if (skin != null) {
            skin.render(reval, null, param);
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
            skin = engine.getSkin("global", skinobj.toString());
        }

        Map param = RhinoCore.getSkinParam(paramobj);

        if (skin != null) {
            ResponseTrans res = reval.getResponse();
            res.pushStringBuffer();
            skin.render(reval, null, param);
            return res.popStringBuffer();
        }

        return "";
    }

    /**
     *
     *
     * @param propname ...
     * @param defvalue ...
     *
     * @return ...
     */
    public String getProperty(String propname, Object defvalue) {
        if (defvalue == Undefined.instance) {
            return app.getProperty(propname);
        } else {
            return app.getProperty(propname, toString(defvalue));
        }
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
     *  Create a Skin object from a string
     *
     * @param str the source string to parse
     *
     * @return a parsed skin object
     */
    public Object createSkin(String str) {
        return Context.toObject(new Skin(str, app), this);
    }

    /**
     * Get a Helma DB connection specified in db.properties
     *
     * @param dbsource the db source name
     *
     * @return a DatabaseObject for the specified DbConnection
     */
    public Object getDBConnection(String dbsource) throws Exception {
        if (dbsource == null)
            throw new EvaluatorException("Wrong number of arguments in getDBConnection(dbsource)");
        DbSource dbsrc = app.getDbSource (dbsource);
        if (dbsrc == null)
            throw new EvaluatorException("DbSource "+dbsource+" does not exist");
        DatabaseObject db = new DatabaseObject (dbsrc);
        return Context.toObject(db, this);
    }

    /**
     * Retrieve a Document from the specified URL.
     *
     * @param location the URL to retrieve
     * @param opt either a LastModified date or an ETag string for conditional GETs
     *
     * @return a wrapped MIME object
     */
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
        } catch (Exception xcept) {
            app.logEvent("Error getting URL "+location+": "+xcept);
        }

        return null;
    }

    /**
     *  Try to parse an object to a XML DOM tree. The argument must be
     *  either a URL, a piece of XML, an InputStream or a Reader.
     */
    public Object getXmlDocument(Object src) {
        try {
            Object p = src;
            if (p instanceof Wrapper) {
                p = ((Wrapper) p).unwrap();
            }
            Object doc = XmlUtils.parseXml(p);

            return Context.toObject(doc, this);
        } catch (Exception noluck) {
            app.logEvent("Error creating XML document: " + noluck);
        }

        return null;
    }

    /**
     *  Try to parse an object to a XML DOM tree. The argument must be
     *  either a URL, a piece of XML, an InputStream or a Reader.
     */
    public Object getHtmlDocument(Object src) {
        try {
            Object p = src;
            if (p instanceof Wrapper) {
                p = ((Wrapper) p).unwrap();
            }
            Object doc = helma.util.XmlUtils.parseHtml(p);

            return Context.toObject(doc, this);
        } catch (Exception noluck) {
            app.logEvent("Error creating HTML document: " + noluck);
        }

        return null;
    }

    /**
     * Creates a global object, optionally initializing it with standard
     * JavaScript objects. This can be used for script libraries that want
     * to extend standard JavaScript functionality, but leave the original
     * prototypes alone.
     *
     * @param name the name of the new scope
     */
    public void defineLibraryScope(final String name, boolean initStandardObjects) {
        Object obj = get(name, this);
        if (obj != NOT_FOUND) {
            // put the property again to fool PropertyRecorder
            // into believing it has been renewed
            put(name, this, obj);
            return;
        }
        ScriptableObject scope = new ScriptableObject() {
            public String getClassName() {
                return name;
            }
        };
        if (initStandardObjects) {
            Context cx = Context.getCurrentContext();
            cx.initStandardObjects(scope, false);
        }
        put(name, this, scope);
    }

    /**
     *
     *
     * @param obj ...
     *
     * @return ...
     */
    public String encode(Object obj) {
        return HtmlEncoder.encodeAll(toString(obj));
    }

    /**
     *
     *
     * @param obj ...
     *
     * @return ...
     */
    public String encodeXml(Object obj) {
        return HtmlEncoder.encodeXml(toString(obj));
    }

    /**
     *
     *
     * @param obj ...
     *
     * @return ...
     */
    public String encodeForm(Object obj) {
        return HtmlEncoder.encodeFormValue(toString(obj));
    }

    /**
     *
     *
     * @param obj ...
     *
     * @return ...
     */
    public String format(Object obj) {
        return HtmlEncoder.encode(toString(obj));
    }

    /**
     *
     *
     * @param obj ...
     *
     * @return ...
     */
    public String formatParagraphs(Object obj) {
        String str = toString(obj);

        if (str == null) {
            return null;
        }

        int l = str.length();

        if (l == 0) {
            return "";
        }

        // try to make stringbuffer large enough from the start
        StringBuffer buffer = new StringBuffer(Math.round(l * 1.4f));

        HtmlEncoder.encode(str, buffer, true, null);

        return buffer.toString();
    }

    /**
     *
     *
     * @param str ...
     */
    public void write(String str) {
        System.out.print(str);
    }

    /**
     *
     *
     * @param str ...
     */
    public void writeln(String str) {
        System.out.println(str);
    }

     /**
     * The seal function seals all supplied arguments.
     */
    public static void seal(Context cx, Scriptable thisObj, Object[] args,
                            Function funObj)
    {
        for (int i = 0; i != args.length; ++i) {
            Object arg = args[i];
            if (!(arg instanceof ScriptableObject) || arg == Undefined.instance)
            {
                if (!(arg instanceof Scriptable) || arg == Undefined.instance)
                {
                    throw new EvaluatorException("seal() can only be applied to Objects");
                } else {
                    throw new EvaluatorException("seal() can only be applied to Objects");
                }
            }
        }

        for (int i = 0; i != args.length; ++i) {
            Object arg = args[i];
            ((ScriptableObject)arg).sealObject();
        }
    }

    /**
     * (Try to) strip all HTML/XML style tags from the given string argument
     *
     * @param str a string
     * @return the string with tags removed
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

    /**
     * Serialize a JavaScript object to a file.
     */
    public static void serialize(Context cx, Scriptable thisObj,
                                 Object[] args, Function funObj)
        throws IOException
    {
        if (args.length < 2) {
            throw Context.reportRuntimeError(
                "Expected an object to serialize and a filename to write " +
                "the serialization to");
        }
        Object obj = args[0];
        String filename = Context.toString(args[1]);
        FileOutputStream fos = new FileOutputStream(filename);
        Scriptable scope = ScriptableObject.getTopLevelScope(thisObj).getPrototype();
        // use a ScriptableOutputStream that unwraps Wrappers
        ScriptableOutputStream out = new ScriptableOutputStream(fos, scope) {
            protected Object replaceObject(Object obj) throws IOException {
                if (obj instanceof Wrapper)
                    obj = ((Wrapper) obj).unwrap();
                return super.replaceObject(obj);
            }
        };
        out.writeObject(obj);
        out.close();
    }

    /**
     * Read a previously serialized JavaScript object from a file.
     */
    public static Object deserialize(Context cx, Scriptable thisObj,
                                     Object[] args, Function funObj)
        throws IOException, ClassNotFoundException
    {
        if (args.length < 1) {
            throw Context.reportRuntimeError(
                "Expected a filename to read the serialization from");
        }
        String filename = Context.toString(args[0]);
        FileInputStream fis = new FileInputStream(filename);
        Scriptable scope = ScriptableObject.getTopLevelScope(thisObj).getPrototype();
        ObjectInputStream in = new ScriptableInputStream(fis, scope);
        Object deserialized = in.readObject();
        in.close();
        return Context.toObject(deserialized, scope);
    }

    /**
     * Set DONTENUM attrubutes on the given properties in this object. 
     * This is set on the JavaScript Object prototype.
     */
    public static Object dontEnum (Context cx, Scriptable thisObj, 
                                   Object[] args, Function funObj) {
        if (!(thisObj instanceof ScriptableObject)) {
            throw new EvaluatorException("dontEnum() called on non-ScriptableObject");
        }
        ScriptableObject obj = (ScriptableObject) thisObj;
        for (int i=0; i<args.length; i++) {
            if (!(args[i] instanceof String)) {
                throw new EvaluatorException("dontEnum() called with non-String argument");
            }
            String str = (String) args[i];
            obj.setAttributes(str, obj.getAttributes(str) | DONTENUM);
        }
        return null;
    }

    private static String toString(Object obj) {
        if (obj == null || obj == Undefined.instance) {
            // Note: we might return "" here in order
            // to handle null/undefined as empty string
            return null;
        }
        return Context.toString(obj);
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
}
