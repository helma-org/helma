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

package helma.scripting.rhino.extensions;

import helma.scripting.rhino.*;
import org.mozilla.javascript.*;
import org.apache.xmlrpc.*;
import java.util.*;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

/**
 * An extension to transparently call and serve XML-RPC from Rhino.
 * The extension adds constructors for XML-RPC clients and servers to the Global Object.
 *
 * All argument conversion is done automatically. Currently the following argument and return
 * types are supported:
 * <ul>
 * <li> plain objects (with all properties returned by ESObject.getProperties ())</li>
 * <li> arrays</li>
 * <li> strings</li>
 * <li> date objects</li>
 * <li> booleans</li>
 * <li> integer and float numbers (long values are not supported!)</li>
 * </ul>
 *
 */
public class XmlRpcObject extends BaseFunction {

    String url = null;
    String method = null;

    XmlRpcObject(String url) {
        this.url = url;
        this.method = null;
    }

    XmlRpcObject(String url, String method) {
        this.url = url;
        this.method = method;
    }

        /**
     *  This method is used as HopObject constructor from JavaScript.
     */
    public static Object xmlrpcObjectConstructor(Context cx, Object[] args,
                                              Function ctorObj, boolean inNewExpr) {
        if (args.length == 0 || args.length > 2) {
            throw new IllegalArgumentException("Wrong number of arguments in constructor for XML-RPC client");
        }
        if (args.length == 1) {
            String url = args[0].toString();
            return new XmlRpcObject(url);
        } else {
            String url = args[0].toString();
            String method = args[1].toString();
            return new XmlRpcObject(url, method);
        }

    }

    /**
     * Called by the evaluator after the extension is loaded.
     */
    public static void init(Scriptable scope) {
        Method[] methods = XmlRpcObject.class.getDeclaredMethods();
        Member ctorMember = null;
        for (int i=0; i<methods.length; i++) {
            if ("xmlrpcObjectConstructor".equals(methods[i].getName())) {
                ctorMember = methods[i];
                break;
            }
        }
        FunctionObject ctor = new FunctionObject("Remote", ctorMember, scope);
        ScriptableObject.defineProperty(scope, "Remote", ctor, ScriptableObject.DONTENUM);
        // ctor.addAsConstructor(scope, proto);
    }


    public Object get(String name, Scriptable start) {
        String m = method == null ? name : method+"."+name;
        return new XmlRpcObject(url, m);
    }

    public Object call(Context cx,
                             Scriptable scope,
                             Scriptable thisObj,
                             Object[] args)
                      throws EvaluatorException {

        if (method == null) {
            throw new EvaluatorException("Invalid method name");
        }

        RhinoEngine engine = (RhinoEngine) cx.getThreadLocal("engine");
        RhinoCore c = engine.getCore();
        Scriptable retval = null;

        try {
            retval = Context.getCurrentContext().newObject(c.getScope());
            XmlRpcClient client = new XmlRpcClient(url);

            // long now = System.currentTimeMillis ();
            int l = args.length;
            Vector v = new Vector();

            for (int i = 0; i < l; i++) {
                Object arg = c.processXmlRpcResponse(args[i]);
                v.addElement(arg);
            }

            Object result = client.execute(method, v);

            retval.put("result", retval, c.processXmlRpcArgument(result));

        } catch (Exception x) {
            String msg = x.getMessage();

            if ((msg == null) || (msg.length() == 0)) {
                msg = x.toString();
            }
            retval.put("error", retval, msg);
        }

        return retval;

    }

    public String getClassName() {
        return "Remote";
    }

    public String toString() {
        return "[Remote "+url+"]";
    }

    public Object getDefaultValue(Class hint) {
        if (hint == null || hint == String.class) {
            return toString();
        }
        return super.getDefaultValue(hint);
    }

}
