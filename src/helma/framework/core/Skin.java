// Skin.java
// Copyright (c) Hannes Wallnöfer 2001

package helma.framework.core;

import helma.framework.*;
import helma.scripting.*;
import helma.objectmodel.INode;
import helma.objectmodel.ConcurrencyException;
import helma.util.HtmlEncoder;
import java.net.URLEncoder;
import java.io.*;
import java.util.*;

/**
 * This represents a Helma skin, i.e. a template created from containing Macro tags
 * that will be dynamically evaluated.. It uses the request path array
 * from the RequestEvaluator object to resolve Macro handlers by type name.
 */

public final class Skin {

    Macro[] parts;
    Application app;
    char[] source;
    int sourceLength;
    HashSet sandbox;

    /**
     * Create a skin without any restrictions on which macros are allowed to be called from it
     */
    public Skin (String content, Application app) {
	this (content, app, null);
    }

    /**
     * Create a skin with a sandbox which contains the names of macros allowed to be called
     */
    public Skin (String content, Application app, HashSet sandbox) {
	this.app = app;
	this.sandbox = sandbox;
	source = content.toCharArray ();
	sourceLength = source.length;
	parse ();
    }

    /**
     *  Create a skin without any restrictions on the macros from a char array.
     */
    public Skin (char[] content, int length, Application app) {
	this.app = app;
	this.sandbox = null;
	this.source = content;
	this.sourceLength = length;
	parse ();
    }

    /**
     * Parse a skin object from source text
     */
    private void parse () {

	ArrayList partBuffer = new ArrayList ();

	int start = 0;
	for (int i = 0; i < sourceLength-1; i++) {
	    if (source[i] == '<' && source[i+1] == '%') {
	        // found macro start tag
	        int j = i+2;
	        // search macr end tag
	        while (j < sourceLength-1 && (source[j] != '%' || source[j+1] != '>')) {
	            j++;
	        }
	        if (j > i+2) {
	            partBuffer.add (new Macro (i, j+2));
	            start = j+2;
	        }
	        i = j+1;
	    }
	}

	parts = new Macro[partBuffer.size()];
	partBuffer.toArray (parts);
    }

    /**
     * Get the raw source text this skin was parsed from
     */
    public String getSource () {
	return new String (source, 0, sourceLength);
    }

    /**
     * Render this skin
     */
    public void render (RequestEvaluator reval, Object thisObject, HashMap paramObject) throws RedirectException {

	if (parts == null)
	    reval.res.writeCharArray (source, 0, sourceLength);

	int written = 0;
	for (int i=0; i<parts.length; i++) {
	    if (parts[i].start > written)
	        reval.res.writeCharArray (source, written, parts[i].start-written);
	    parts[i].render (reval, thisObject, paramObject);
	    written = parts[i].end;
	}
	if (written < sourceLength)
	    reval.res.writeCharArray (source, written, sourceLength-written);
    }

    /**
     * Check if a certain macro is present in this skin. The macro name is in handler.name notation
     */
    public boolean containsMacro (String macroname) {
	for (int i=0; i<parts.length; i++) {
	    if (parts[i] instanceof Macro) {
	        Macro m = (Macro) parts[i];
	        if (macroname.equals (m.getFullName ()))
	            return true;
	    }
	}
	return false;
    }

    /**
     *  Adds a macro to the list of allowed macros. The macro is in handler.name notation.
     */
    public void allowMacro (String macroname) {
	if (sandbox == null) {
	    sandbox = new HashSet ();
	}
	sandbox.add (macroname);
    }

    static final int HANDLER = 0;
    static final int MACRO = 1;
    static final int PARAMNAME = 2;
    static final int PARAMVALUE = 3;

    class Macro {

	int start, end;
	String handler;
	String name;
	String fullname;
	HashMap parameters;

	public Macro (int start, int end) {

	    this.start = start;
	    this.end = end;

	    parameters = new HashMap ();

	    int state = HANDLER;
	    boolean escape = false;
	    char quotechar = '\u0000';
	    String lastParamName = null;
	    StringBuffer b = new StringBuffer();

	    for (int i=start+2; i<end-2; i++) {
	        switch (source[i]) {
	            case '.':
	                if (state == HANDLER) {
	                    handler = b.toString ().trim();
	                    b.setLength (0);
	                    state = MACRO;
	                } else
	                    b.append (source[i]);
	                break;
	            case '\\':
	                if (escape)
	                    b.append (source[i]);
	                escape = !escape;
	                break;
	            case '"':
	            case '\'':
	                if (!escape && state == PARAMVALUE) {
	                    if (quotechar == source[i]) {
	                        parameters.put (lastParamName, b.toString());
	                        lastParamName = null;
	                        b.setLength (0);
	                        state = PARAMNAME;
	                        quotechar = '\u0000';
	                    } else if (quotechar == '\u0000') {
	                        quotechar = source[i];
	                        b.setLength (0);
	                    } else
	                        b.append (source[i]);
	                } else
	                    b.append (source[i]);
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
	                    b.append (source[i]);
	                else
	                    b.setLength (0);
	                break;
	            case '=':
	                if (state == PARAMNAME) {
	                    lastParamName = b.toString().trim();
	                    b.setLength (0);
	                    state = PARAMVALUE;
	                } else
	                    b.append (source[i]);
	                break;
	            default:
	                b.append (source[i]);
	                escape = false;
	        }
	    }
	    if (b.length() > 0) {
	        if (lastParamName != null && b.length() > 0)
	            parameters.put (lastParamName, b.toString());
	        else if (state <= MACRO)
	            name = b.toString().trim();
	    }
	}


	/**
	 *  Render the macro given a handler object
	 */
	public void render (RequestEvaluator reval, Object thisObject, HashMap paramObject) throws RedirectException {

	    if (sandbox != null && !sandbox.contains (getFullName ())) {
	        String h = handler == null ? "global" : handler;
	        reval.res.write ("[Macro "+getFullName()+" not allowed in sandbox]");
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

	        Object handlerObject = null;

	        Object[] arguments = new Object[1];
	        // pass a clone of the parameter map so if the script changes it,
	        // we still keep the original version.
	        arguments[0] = parameters.clone ();

	        // flag to tell whether we found our invocation target object
	        boolean objectFound = true;

	        if (handler != null) {
	            if ("currentuser".equalsIgnoreCase (handler)) {
	                // as a special convention, we use "currentuser" to access macros in the current user object
	                handlerObject = reval.user.getNode ();
	            } else if (thisObject != null) {
	                // not a global macro - need to find handler object
	                // was called with this object - check it or its parents for matching prototype
	                if (!handler.equalsIgnoreCase ("this") && !handler.equalsIgnoreCase (app.getPrototypeName (thisObject))) {
	                    // the handler object is not what we want
	                    Object n = thisObject;
	                    // walk down parent chain to find handler object
	                    while (n != null) {
	                        if (handler.equalsIgnoreCase (app.getPrototypeName (n))) {
	                            handlerObject = n;
	                            break;
	                        }
	                        n = app.getParentElement (n);
	                    }
	                } else {
	                    // we already have the right handler object
	                    handlerObject = thisObject;
	                }
	            }

	            if (handlerObject == null) {
	                // eiter because thisObject == null or the right object wasn't found in the object's parent path
	                // go check request path for an object with matching prototype
	                int l = reval.requestPath.size();
	                for (int i=l-1; i>=0; i--) {
	                    Object pathelem = reval.requestPath.get (i);
	                    if (handler.equalsIgnoreCase (app.getPrototypeName (pathelem))) {
	                         handlerObject = pathelem;
	                         break;
	                    }
	                }
	            }

	            // the macro handler object couldn't be found
	            if (handlerObject == null)
	                objectFound = false;

	        } else {
	            // this is a global macro with no handler specified
	            handlerObject = null;
	        }

	        if (objectFound) {
	            // check if a function called name_macro is defined.
	            // if so, the macro evaluates to the function. Otherwise,
	            // a property/field with the name is used, if defined.
	            Object v = null;
	            // remember length of response buffer before calling macro
	            int oldLength = reval.res.getBufferLength ();
	            if (reval.scriptingEngine.hasFunction (handlerObject, name+"_macro")) {
	                // System.err.println ("Getting macro from function");
	                v = reval.scriptingEngine.invoke (handlerObject, name+"_macro", arguments);
	            } else {
	                // System.err.println ("Getting macro from property");
	                v = reval.scriptingEngine.get (handlerObject, name);
	            }
	            // check if macro wrote out to response buffer
	            int newLength = reval.res.getBufferLength ();
	            if (newLength > oldLength) {
	               // insert prefix and append suffix
	               String prefix = (String) parameters.get ("prefix");
	               String suffix = (String) parameters.get ("suffix");
	               reval.res.insert (oldLength, prefix);
	               reval.res.write (suffix);
	            }
	            // if macro returned something append it to response
	            if (v != null) {
	                writeToResponse (v.toString (), reval.res);
	            }
	        } else {
	            String msg = "[HopMacro unhandled: "+getFullName()+"]";
	            reval.res.write (" "+msg+" ");
	            app.logEvent (msg);
	        }
	    } catch (RedirectException redir) {
	        throw redir;
	    } catch (ConcurrencyException concur) {
	        throw concur;
	    } catch (Exception x) {
	        x.printStackTrace();
	        String msg = "[HopMacro error: "+x+"]";
	        reval.res.write (" "+msg+" ");
	        app.logEvent (msg);
	    }
	}

	private void renderFromResponse (RequestEvaluator reval) {
	    Object value = null;
	    // as a transitional solution, try to get the value from the
	    // hardcoded fields in the response object. If not present, try
	    // the response object's data object.
	    if ("title".equals (name))
	        value = reval.res.title;
	    else if ("head".equals (name))
	        value = reval.res.head;
	    else if ("body".equals (name))
	        value = reval.res.body;
	    else if ("message".equals (name))
	        value = reval.res.message;
	    if (value == null)
	        value = reval.res.get (name);
	    if (value != null)
	        writeToResponse (value.toString (), reval.res);
	}

	private void renderFromRequest (RequestEvaluator reval) {
	    Object value = reval.req.get (name);
	    if (value != null)
	        writeToResponse (value.toString (), reval.res);
	}

	private void renderFromParam (RequestEvaluator reval, HashMap paramObject) {
	    if (paramObject == null)
	        reval.res.write ("[HopMacro error: Skin requires a parameter object]");
	    else {
	        Object value = paramObject.get (name);
	        if (value != null)
	            writeToResponse (value.toString (), reval.res);
	    }
	}

	/**
	 * Utility method for writing text out to the response object.
	 */
	void writeToResponse (String text, ResponseTrans res) {
	    if (text == null || text.length() == 0)
	        return;
	    String encoding = (String) parameters.get ("encoding");
	    String prefix = (String) parameters.get ("prefix");
	    String suffix = (String) parameters.get ("suffix");
	    res.write (prefix);
	    res.write (encode (text, encoding));
	    res.write (suffix);
	}

	/**
	 * Utility method for performing different kind of character
	 * encodings on the macro output.
	 */
	String encode (String text, String encoding) {
	    if (encoding == null || text == null)
	        return text;
	    if ("html".equalsIgnoreCase (encoding))
	        return HtmlEncoder.encode (text);
	    if ("xml".equalsIgnoreCase (encoding))
	        return HtmlEncoder.encodeXml (text);
	    if ("form".equalsIgnoreCase (encoding))
	        return HtmlEncoder.encodeFormValue (text);
	    if ("url".equalsIgnoreCase (encoding))
	        return URLEncoder.encode (text);
	    return text;
	}


	public String toString () {
	    return "[HopMacro: "+getFullName()+"]";
	}

	/**
	 * Return the full name of the macro in handler.name notation
	 */
	public String getFullName () {
	    if (fullname == null) {
	        if (handler == null)
	            fullname = name;
	        else
	            fullname = handler+"."+name;
	    }
	    return fullname;
	}

    }

}


