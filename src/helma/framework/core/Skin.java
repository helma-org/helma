// Skin.java
// Copyright (c) Hannes Wallnöfer 2001
 
package helma.framework.core;

import java.util.*;
import java.io.*;
import helma.framework.*;
import FESI.Data.*;
import FESI.Exceptions.*;
import helma.objectmodel.INode;
import helma.objectmodel.IServer;


/**
 * This represents a HOP skin, i.e. a template created from JavaScript. It uses the request path array
 * from the RequestEvaluator object to resolve dynamic tokens.
 */

public class Skin {

    Object[] parts;

    public Skin (String content) {
	parse (content);
    }

    public void parse (String content) {

	Vector partBuffer = new Vector ();
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
	                partBuffer.addElement (new String (cnt, lastIdx, i - lastIdx));
	            String macrotext = new String (cnt, i+2, (j-i)-2);
	            partBuffer.addElement (new Macro (macrotext));
	            lastIdx = j+2;
	        }
	        i = j+1;
	    }
	}
	if (lastIdx < l)
	    partBuffer.addElement (new String (cnt, lastIdx, l - lastIdx));

             parts = partBuffer.toArray ();
   }

    public void render (RequestEvaluator reval, ESNode handlerNode) {
	if (parts == null)
	    return;
	for (int i=0; i<parts.length; i++) {
	    if (parts[i] instanceof Macro)
	        ((Macro) parts[i]).render (reval, handlerNode);
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
	}


	public void render (RequestEvaluator reval, ESNode handlerNode) {

	    if ("response".equalsIgnoreCase (handler)) {
	        renderFromResponse (reval);
	        return;
	    }

	    try {

	        ESObject handlerObject = null;

	        ESValue[] arguments = new ESValue[1];
	        ESRequestData par =  new ESRequestData (reval);
	        par.setData (parameters);
	        arguments[0] = par;

	        if (handler != null) {
	            // not a macro handled by global - check handler object
	            if (handlerNode != null) {
	                // was called with this object - check it or its parents for matching prototype
	                if (!handler.equalsIgnoreCase (handlerNode.getPrototypeName ())) {
	                    // the handler object is not what we want
	                    INode n = handlerNode.getNode();
	                    while (n != null) {
	                        if (handler.equalsIgnoreCase (n.getPrototype())) {
	                            handlerObject = reval.getNodeWrapper (n);
	                            break;
	                        }
	                        n = n.getParent ();
	                    }
	                } else {
	                    // we already have the right handler object
	                    handlerObject = handlerNode;
	                }
	            }

	            if (handlerObject == null) {
	                // eiter because handlerNode == null or the right object wasn't found in the targetNode path
	                // go check request path for an object with matching prototype
	                int l = reval.reqPath.size();
	                for (int i=l-1; i>=0; i--) {
	                    if (handler.equalsIgnoreCase (((ESNode) reval.reqPath.getProperty(i)).getPrototypeName())) {
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
	            handlerObject.doIndirectCall (reval.evaluator, handlerObject, name+"_macro", arguments);
	        } else {
	            String msg = "[HopMacro unhandled: "+handler+"."+name+"]";
	            reval.res.write (" "+msg+" ");
	            IServer.getLogger().log (msg);
	        }
	    } catch (Exception x) {
	        String msg = "[HopMacro error: "+x+"]";
	        reval.res.write (" "+msg+" ");
	        IServer.getLogger().log (msg);
	    }
	}

	private void renderFromResponse (RequestEvaluator reval) {
	    if ("title".equals (name))
	        reval.res.write (reval.res.title);
	    else if ("head".equals (name))
	        reval.res.write (reval.res.head);
	    else if ("body".equals (name))
	        reval.res.write (reval.res.body);
	    else if ("message".equals (name))
	        reval.res.write (reval.res.message);
	}

	public String toString () {
	    return "[HopMacro: "+handler+","+name+"]";
	}
    }


}





























