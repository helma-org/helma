// Skin.java
// Copyright (c) Hannes Wallnöfer 2001
 
package helma.framework.core;

import java.io.*;
import java.util.ArrayList;
import java.util.Hashtable;
import helma.framework.*;
import FESI.Data.*;
import FESI.Exceptions.*;
import helma.objectmodel.INode;
import helma.objectmodel.IServer;
import helma.objectmodel.ConcurrencyException;
import helma.util.HtmlEncoder;
import helma.util.UrlEncoder;

/**
 * This represents a HOP skin, i.e. a template created from JavaScript. It uses the request path array
 * from the RequestEvaluator object to resolve dynamic tokens.
 */

public class Skin {

    Object[] parts;
    Application app;
    String source;
    ESObject sandbox;

    public Skin (String content, Application app) {
	this (content, app, null);
    }

    public Skin (String content, Application app, ESObject sandbox) {
	this.app = app;
	this.sandbox = sandbox;
	parse (content);
    }

    public void parse (String content) {

	this.source = content;
	ArrayList partBuffer = new ArrayList ();
	int l = content.length ();
	char cnt[] = new char[l];
	content.getChars (0, l, cnt, 0);

	int lastIdx = 0;
	for (int i = 0; i < l-1; i++) {
	    if (cnt[i] == '<' && cnt[i+1] == '%') {
	        int j = i+2;
	        while (j < l-1 && (cnt[j] != '%' || cnt[j+1] != '>')) {
	            j++;
	        }
	        if (j > i+2) {
	            if (i - lastIdx > 0)
	                partBuffer.add (new String (cnt, lastIdx, i - lastIdx));
	            String macrotext = new String (cnt, i+2, (j-i)-2);
	            partBuffer.add (new Macro (macrotext));
	            lastIdx = j+2;
	        }
	        i = j+1;
	    }
	}
	if (lastIdx < l)
	    partBuffer.add (new String (cnt, lastIdx, l - lastIdx));

             parts = partBuffer.toArray ();
    }

    public String getSource () {
	return source;
    }

    public void render (RequestEvaluator reval, ESObject thisObject, ESObject paramObject) throws RedirectException {
	
	if (parts == null)
	    return;
	
	IPathElement elem = null;
	
	if (thisObject != null) {
	    try {
	        elem = (IPathElement) thisObject.toJavaObject ();
	    } catch (ClassCastException wrongClass) {
	        throw new RuntimeException ("Can't render a skin on something that is not a path element: "+wrongClass);
	    }
	}
	
	for (int i=0; i<parts.length; i++) {
	    if (parts[i] instanceof Macro)
	        ((Macro) parts[i]).render (reval, thisObject, elem, paramObject);
	    else
	        reval.res.write (parts[i]);
	}
    }

    static final int HANDLER = 0;
    static final int MACRO = 1;
    static final int PARAMNAME = 2;
    static final int PARAMVALUE = 3;

    class Macro {

	String handler;
	String name;
	Hashtable parameters;
	boolean notallowed = false;

	public Macro (String str) {

	    parameters = new Hashtable ();

	    int l = str.length ();
	    char cnt[] = new char[l];
	    str.getChars (0, l, cnt, 0);

	    int state = HANDLER;
	    boolean escape = false;
	    char quotechar = '\u0000';
	    String lastParamName = null;
	    StringBuffer b = new StringBuffer();

	    for (int i=0; i<l; i++) {
	        switch (cnt[i]) {
	            case '.':
	                if (state == HANDLER) {
	                    handler = b.toString ().trim();
	                    b.setLength (0);
	                    state = MACRO;
	                } else
	                    b.append (cnt[i]);
	                break;
	            case '\\':
	                if (escape)
	                    b.append (cnt[i]);
	                escape = !escape;
	                break;;
	            case '"':
	            case '\'':
	                if (!escape && state == PARAMVALUE) {
	                    if (quotechar == cnt[i]) {
	                        parameters.put (lastParamName, b.toString());
	                        lastParamName = null;
	                        b.setLength (0);
	                        state = PARAMNAME;
	                        quotechar = '\u0000';
	                    } else if (quotechar == '\u0000') {
	                        quotechar = cnt[i];
	                        b.setLength (0);
	                    } else
	                        b.append (cnt[i]);
	                } else
	                    b.append (cnt[i]);
	                escape = false;
	                break;
	            case ' ':
	            case '\t':
	            case '\n':
	            case '\r':
	            case '\f':
	                if (state == MACRO || (state == HANDLER && b.length() > 0)) {
	                    name = b.toString().trim();
	                    b.setLength (0);
	                    state = PARAMNAME;
	                } else if (state == PARAMVALUE && quotechar == '\u0000') {
	                    parameters.put (lastParamName, b.toString());
	                    lastParamName = null;
	                    b.setLength (0);
	                    state = PARAMNAME;
	                } else if (state == PARAMVALUE)
	                    b.append (cnt[i]);
	                else
	                    b.setLength (0);
	                break;
	            case '=':
	                if (state == PARAMNAME) {
	                    lastParamName = b.toString().trim();
	                    b.setLength (0);
	                    state = PARAMVALUE;
	                } else
	                    b.append (cnt[i]);
	                break;
	            default:
	                b.append (cnt[i]);
	                escape = false;
	        }
	    }
	    if (b.length() > 0) {
	        if (lastParamName != null && b.length() > 0)
	            parameters.put (lastParamName, b.toString());
	        else if (state <= MACRO)
	            name = b.toString().trim();
	    }
	    if (sandbox != null && name != null) try {
	        ESValue allow = handler == null ?
	                sandbox.getProperty ("global", "global".hashCode ()) :
	                sandbox.getProperty (handler, handler.hashCode ());
	        allow = ((ESObject) allow).getProperty (name, name.hashCode ());
	        if (allow == null || allow == ESUndefined.theUndefined)
	            notallowed = true;
	    } catch (Exception x) {
	        notallowed = true;
	    }
	}


	public void render (RequestEvaluator reval, ESObject thisObject, IPathElement elem, ESObject paramObject) throws RedirectException {

	    if (notallowed) {
	        String h = handler == null ? "global" : handler;
	        reval.res.write ("[Macro "+h+"."+name+" not allowed in sandbox]");
	        return;
	    } else if ("response".equalsIgnoreCase (handler)) {
	        renderFromResponse (reval);
	        return;
	    } else if ("request".equalsIgnoreCase (handler)) {
	        renderFromRequest (reval);
	        return;
	    } else if ("param".equalsIgnoreCase (handler)) {
	        renderFromParam (reval, paramObject);
	        return;
	    }

	    try {

	        ESObject handlerObject = null;

	        ESValue[] arguments = new ESValue[1];
	        ESRequestData par =  new ESRequestData (reval);
	        par.setData (parameters);
	        arguments[0] = par;

	        if (handler != null) {
	            if ("currentuser".equalsIgnoreCase (handler)) {
	                // as a special convention, we use "currentuser" to access macros in the current user object
	                handlerObject = reval.getNodeWrapper (reval.user.getNode ());
	            } else if (elem != null) {
	                // not a global macro - need to find handler object
	                // was called with this object - check it or its parents for matching prototype
	                if (!handler.equalsIgnoreCase ("this") && !handler.equalsIgnoreCase (elem.getPrototype ())) {
	                    // the handler object is not what we want
	                    IPathElement n = elem;
	                    // walk down parent chain to find handler object
	                    while (n != null) {
	                        if (handler.equalsIgnoreCase (n.getPrototype())) {
	                            handlerObject = reval.getElementWrapper (n);
	                            break;
	                        }
	                        n = n.getParentElement ();
	                    }
	                } else {
	                    // we already have the right handler object
	                    handlerObject = thisObject;
	                }
	            }

	            if (handlerObject == null) {
	                // eiter because thisObject == null or the right object wasn't found in the object's parent path
	                // go check request path for an object with matching prototype
	                int l = reval.reqPath.size();
	                for (int i=l-1; i>=0; i--) {
	                    IPathElement pathelem = (IPathElement) reval.reqPath.getProperty (i).toJavaObject ();
	                    if (handler.equalsIgnoreCase (pathelem.getPrototype ())) {
	                         handlerObject = (ESNode) reval.reqPath.getProperty(i);
	                         break;
	                    }
	                }
	            }

	        } else {
	            // this is a global macro with no handler specified
	            handlerObject = reval.global;
	        }

	        if (handlerObject != null) {
	            ESValue v = handlerObject.doIndirectCall (reval.evaluator, handlerObject, name+"_macro", arguments);
	            if (v != ESUndefined.theUndefined && v != ESNull.theNull)
	                reval.res.write (v);
	        } else {
	            String msg = "[HopMacro unhandled: "+handler+"."+name+"]";
	            reval.res.write (" "+msg+" ");
	            app.logEvent (msg);
	        }
	    } catch (RedirectException redir) {
	        throw redir;
	    } catch (ConcurrencyException concur) {
	        throw concur;
	    } catch (Exception x) {
	        String msg = "[HopMacro error: "+x+"]";
	        reval.res.write (" "+msg+" ");
	        app.logEvent (msg);
	    }
	}

	private void renderFromResponse (RequestEvaluator reval) {
	    String encoding = (String) parameters.get ("encoding");
	    if ("title".equals (name) && reval.res.title != null)
	        reval.res.write (encode (reval.res.title, encoding));
	    else if ("head".equals (name) && reval.res.head != null)
	        reval.res.write (encode (reval.res.head, encoding));
	    else if ("body".equals (name) && reval.res.body != null)
	        reval.res.write (encode (reval.res.body, encoding));
	    else if ("message".equals (name) && reval.res.message != null)
	        reval.res.write (encode (reval.res.message, encoding));
	}

	private void renderFromRequest (RequestEvaluator reval) {
	    String encoding = (String) parameters.get ("encoding");
	    Object value = reval.req.get (name);
	    if (value != null)
	        reval.res.write (encode (value.toString (), encoding));
	}

	private void renderFromParam (RequestEvaluator reval, ESObject paramObject) {
	    String encoding = (String) parameters.get ("encoding");
	    if (paramObject == null)
	        reval.res.write ("[HopMacro error: Skin requires a parameter object]");
	    else {
	        try {
	            ESValue value = paramObject.getProperty (name, name.hashCode());
	            if (value != null && value != ESUndefined.theUndefined && value != ESNull.theNull)
	                reval.res.write (encode (value.toString (), encoding));
	        } catch (EcmaScriptException ignore) {}
	    }
	}
	
	public String encode (String text, String encoding) {
	    if (encoding == null || text == null)
	        return text;
	    if ("html".equalsIgnoreCase (encoding))
	        return HtmlEncoder.encodeSoft (text);
	    if ("xml".equalsIgnoreCase (encoding))
	        return HtmlEncoder.encodeXml (text);
	    if ("form".equalsIgnoreCase (encoding))
	        return HtmlEncoder.encodeFormValue (text);
	    if ("url".equalsIgnoreCase (encoding))
	        return UrlEncoder.encode (text);
	    return text;
	}

	public String toString () {
	    return "[HopMacro: "+handler+","+name+"]";
	}
    }


}





























