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

    private Macro[] parts;
    private Application app;
    private char[] source;
    private int sourceLength;
    private HashSet sandbox;


    /**
     * Create a skin without any restrictions on which macros are allowed to be called from it
     */
    public Skin (String content, Application app) {
	this.app = app;
	sandbox = null;
	source = content.toCharArray ();
	sourceLength = source.length;
	parse ();
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
	source = content;
	sourceLength = length;
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
    public void render (RequestEvaluator reval, Object thisObject, Map paramObject) throws RedirectException {

	// check for endless skin recursion
	if (++reval.skinDepth > 50)
	    throw new RuntimeException ("Recursive skin invocation suspected");

	if (parts == null) {
	    reval.res.writeCharArray (source, 0, sourceLength);
	    reval.skinDepth--;
	    return;
	}

	try {
	    int written = 0;
	    Map handlerCache = null;
	    if (parts.length > 3) {
	        handlerCache = new HashMap();
	    }
	    for (int i=0; i<parts.length; i++) {
	        if (parts[i].start > written)
	            reval.res.writeCharArray (source, written, parts[i].start-written);
	        parts[i].render (reval, thisObject, paramObject, handlerCache);
	        written = parts[i].end;
	    }
	    if (written < sourceLength)
	        reval.res.writeCharArray (source, written, sourceLength-written);
	} finally {
	    reval.skinDepth--;
	}
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

	final int start, end;
	String handler;
	String name;
	String prefix;
	String suffix;
	String encoding;
	String defaultValue;
	Map parameters = null;

	public Macro (int start, int end) {

	    this.start = start;
	    this.end = end;
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
	                        String paramValue = b.toString();
	                        if (!setSpecialParameter (lastParamName, paramValue))
	                            addGenericParameter (lastParamName, paramValue);
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
	                    String paramValue = b.toString();
	                    if (!setSpecialParameter (lastParamName, paramValue))
	                        addGenericParameter (lastParamName, paramValue);
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
	        if (lastParamName != null && b.length() > 0) {
	            String paramValue = b.toString();
	            if (!setSpecialParameter (lastParamName, paramValue))
	                addGenericParameter (lastParamName, paramValue);
	        } else if (state <= MACRO)
	            name = b.toString().trim();
	    }
	}

	
	private boolean setSpecialParameter (String name, String value) {
	    if ("prefix".equals (name)) {
	        prefix = value;
	        return true;
	    } else if ("suffix".equals (name)) {
	        suffix = value;
	        return true;
	    } else if ("encoding".equals (name)) {
	        encoding = value;
	        return true;
	    } else if ("default".equals (name)) {
	        defaultValue = value;
	        return true;
	    }
	    return false;
	}
	
	private void addGenericParameter (String name, String value) {
	    if (parameters == null)
	        parameters = new HashMap ();
	    parameters.put (name, value);
	}

	/**
	 *  Render the macro given a handler object
	 */
	public void render (RequestEvaluator reval, Object thisObject, Map paramObject, Map handlerCache) throws RedirectException {

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

	        // flag to tell whether we found our invocation target object
	        boolean objectFound = true;

	        if (handler != null) {
	            // try to get handler from handlerCache first
	            if (handlerCache != null)
	                handlerObject = handlerCache.get (handler);

	            if (handlerObject == null) {

	                // if handler object wasn't found in cache retrieve it
	                if (handlerObject == null && thisObject != null) {
	                    // not a global macro - need to find handler object
	                    // was called with this object - check it or its parents for matching prototype
	                    if (!handler.equals ("this") && !handler.equals (app.getPrototypeName (thisObject))) {
	                        // the handler object is not what we want
	                        Object n = app.getParentElement (thisObject);
	                        // walk down parent chain to find handler object
	                        while (n != null) {
	                            if (handler.equals (app.getPrototypeName (n))) {
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
	                        if (handler.equals (app.getPrototypeName (pathelem))) {
	                            handlerObject = pathelem;
	                            break;
	                        }
	                    }
	                }

	                // the macro handler object couldn't be found
	                if (handlerObject == null)
	                    objectFound = false;
	                // else put the found handler object into the cache so we don't have to look again
	                else if (handlerCache != null)
	                    handlerCache.put (handler, handlerObject);
	            }

	        } else {
	            // this is a global macro with no handler specified
	            handlerObject = null;
	        }

	        if (objectFound) {
	            // check if a function called name_macro is defined.
	            // if so, the macro evaluates to the function. Otherwise,
	            // a property/field with the name is used, if defined.

	            String funcName = name+"_macro";
	            if (reval.scriptingEngine.hasFunction (handlerObject, funcName)) {
	                // remember length of response buffer before calling macro
	                int bufLength = reval.res.getBufferLength ();
	                // remember length of buffer with prefix written out
	                int preLength = 0;
	                if (prefix != null) {
	                    reval.res.write (prefix);
	                    preLength = prefix.length();
	                }

	                // System.err.println ("Getting macro from function");
	                // pass a clone of the parameter map so if the script changes it,
	                // Map param = ;
	                // if (parameters == null)
	                //     parameters = new HashMap ();
	                Object[] arguments = { parameters == null ?
	                    new HashMap () :
	                    new HashMap (parameters) };

	                Object v = reval.scriptingEngine.invoke (handlerObject, funcName, arguments, false);
	                // check if macro wrote out to response buffer
	                if (reval.res.getBufferLength () == bufLength + preLength) {
	                    // function didn't write out anything itself
	                    if (preLength > 0)
	                        reval.res.setBufferLength (bufLength);
	                    writeToResponse (v, reval.res, true);
	                } else {
	                    if (suffix != null)
	                        reval.res.write (suffix);
	                    writeToResponse (v, reval.res, false);
	                }
	            } else {
	                // System.err.println ("Getting macro from property");
	                Object v = reval.scriptingEngine.get (handlerObject, name);
	                writeToResponse (v, reval.res, true);
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
	    } catch (TimeoutException timeout) {
	        throw timeout;
	    } catch (Exception x) {
	        x.printStackTrace();
	        String msg = "[HopMacro error in "+getFullName()+": "+x+"]";
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
	    writeToResponse (value, reval.res, true);
	}

	private void renderFromRequest (RequestEvaluator reval) {
	    Object value = reval.req.get (name);
	    writeToResponse (value, reval.res, true);
	}

	private void renderFromParam (RequestEvaluator reval, Map paramObject) {
	    if (paramObject == null)
	        reval.res.write ("[HopMacro error: Skin requires a parameter object]");
	    else {
	        Object value = paramObject.get (name);
	        writeToResponse (value, reval.res, true);
	    }
	}

	/**
	 * Utility method for writing text out to the response object.
	 */
	void writeToResponse (Object value, ResponseTrans res, boolean useDefault) {
	    String text;
	    if (value == null) {
	        if (useDefault)
	            text = defaultValue;
	        else
	            return;
	    } else {
	        text = value.toString ();
	    }
	    if (text == null || text.length() == 0)
	        return;
	    if (encoding != null)
	        text = encode (text, encoding);
	    res.write (prefix);
	    res.write (text);
	    res.write (suffix);
	}

	/**
	 * Utility method for performing different kind of character
	 * encodings on the macro output.
	 */
	String encode (String text, String encoding) {
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
	    if (handler == null)
	        return name;
	    else
	        return handler+"."+name;
	}

    }

}


