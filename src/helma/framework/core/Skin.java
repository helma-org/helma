// Skin.java
// Copyright (c) Hannes Wallnöfer 2001
 
package helma.framework.core;

import java.util.*;
import java.io.*;
import helma.framework.*;
import FESI.Data.*;


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

    public String toString () {
	if (parts == null)
	    return "";
	StringBuffer b = new StringBuffer ();
	for (int i=0; i<parts.length; i++)
	    b.append (parts[i]);
	return b.toString ();
    }


    class Macro {

	String handler;
	String name;
	HashMap parameters;

	public Macro (String str) {
	    int dot = str.indexOf (".");
	    if (dot < 0) {
	        handler = null;
	        name = str;
	    } else {
	        handler = str.substring (0, dot);
	        name = str.substring (dot+1);
	    }
	}


	public String toString () {
	    return "[HopMacro: "+handler+","+name+"]";
	}

    }


}





























