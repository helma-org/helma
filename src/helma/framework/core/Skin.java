// Skin.java
// Copyright (c) Hannes Wallnöfer 2001
 
package helma.framework.core;

import java.util.*;
import java.io.*;
import helma.framework.*;
import FESI.Data.*;
import FESI.Exceptions.*;


/**
 * This represents a HOP skin, i.e. a template created from JavaScript. It uses the request path array
 * from the RequestEvaluator object to resolve dynamic tokens.
 */

public class Skin {

    Object[] parts;
    RequestEvaluator reval;

    public Skin (String content, RequestEvaluator reval) {
	this.reval = reval;
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

    public String toString () {
	if (parts == null)
	    return "";
	StringBuffer b = new StringBuffer ();
	for (int i=0; i<parts.length; i++)
	    b.append (parts[i]);
	return b.toString ();
    }

    static final int HANDLER = 0;
    static final int MACRO = 1;
    static final int PARAMNAME = 2;
    static final int PARAMVALUE = 3;

    class Macro {

	String handler;
	String name;
	ESObject parameters;

	public Macro (String str) {

	    parameters = new ObjectPrototype (null, reval.evaluator);

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
	                        try {
	                            parameters.putHiddenProperty (lastParamName, new ESString (b.toString()));
	                        } catch (EcmaScriptException badluck) {}
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
	                    try {
	                        parameters.putHiddenProperty (lastParamName, new ESString (b.toString()));
	                    } catch (EcmaScriptException badluck) {}
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
	    if (lastParamName != null && b.length() > 0) try {
	        parameters.putHiddenProperty (lastParamName, new ESString (b.toString()));
	    } catch (EcmaScriptException noluck) {}
	}


	public String toString () {

	    try {

	        ESValue[] arguments = new ESValue[2];
	        arguments[0] = new ESString (name);
	        arguments[1] = parameters;
	        ESNode handlerNode = null;

	        if (handler != null) {
	            int l = reval.reqPath.size();
	            for (int i=l-1; i>=0; i--) {
	                if (handler.equalsIgnoreCase (((ESNode) reval.reqPath.getProperty(i)).getPrototypeName())) {
	                     handlerNode = (ESNode) reval.reqPath.getProperty(i);
	                     break;
	                }
	            }
                     } else {
	            handlerNode = (ESNode) reval.reqPath.getProperty(0);
	        }

	        if (handlerNode != null) {
	            return handlerNode.doIndirectCall (reval.evaluator, handlerNode, "handleMacro", arguments).toString();
	        } else {
	            return "[HopMacro unhandled: "+handler+"."+name+"]";
	        }
	    } catch (Exception x) {
	        return "[HopMacro error: "+x.getMessage()+"]";
	    }
	}

    }


}





























