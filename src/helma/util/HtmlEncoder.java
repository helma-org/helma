// HtmlEncoder.java
// Copyright (c) Hannes Wallnöfer 1997-2000

package helma.util;

import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.image.*;
import java.text.*;


/**
 * This is a utility class to encode special characters and do formatting 
 * for HTML output.
 */

public final class HtmlEncoder {


    static final Hashtable convertor = new Hashtable (128);

    // conversion table
    static {
     convertor.put(new Integer(160), "&nbsp;");
     convertor.put(new Integer(161), "&iexcl;");
     convertor.put(new Integer(162), "&cent;");
     convertor.put(new Integer(163), "&pound;");
     convertor.put(new Integer(164), "&curren;");
     convertor.put(new Integer(165), "&yen;");
     convertor.put(new Integer(166), "&brvbar;");
     convertor.put(new Integer(167), "&sect;");
     convertor.put(new Integer(168), "&uml;");
     convertor.put(new Integer(169), "&copy;");
     convertor.put(new Integer(170), "&ordf;");
     convertor.put(new Integer(171), "&laquo;");
     convertor.put(new Integer(172), "&not;");
     convertor.put(new Integer(173), "&shy;");
     convertor.put(new Integer(174), "&reg;");
     convertor.put(new Integer(175), "&macr;");
     convertor.put(new Integer(176), "&deg;");
     convertor.put(new Integer(177), "&plusmn;");
     convertor.put(new Integer(178), "&sup2;");
     convertor.put(new Integer(179), "&sup3;");
     convertor.put(new Integer(180), "&acute;");
     convertor.put(new Integer(181), "&micro;");
     convertor.put(new Integer(182), "&para;");
     convertor.put(new Integer(183), "&middot;");
     convertor.put(new Integer(184), "&cedil;");
     convertor.put(new Integer(185), "&sup1;");
     convertor.put(new Integer(186), "&ordm;");
     convertor.put(new Integer(187), "&raquo;");
     convertor.put(new Integer(188), "&frac14;");
     convertor.put(new Integer(189), "&frac12;");
     convertor.put(new Integer(190), "&frac34;");
     convertor.put(new Integer(191), "&iquest;");
     convertor.put(new Integer(192), "&Agrave;");
     convertor.put(new Integer(193), "&Aacute;");
     convertor.put(new Integer(194), "&Acirc;");
     convertor.put(new Integer(195), "&Atilde;");
     convertor.put(new Integer(196), "&Auml;");
     convertor.put(new Integer(197), "&Aring;");
     convertor.put(new Integer(198), "&AElig;");
     convertor.put(new Integer(199), "&Ccedil;");
     convertor.put(new Integer(200), "&Egrave;");
     convertor.put(new Integer(201), "&Eacute;");
     convertor.put(new Integer(202), "&Ecirc;");
     convertor.put(new Integer(203), "&Euml;");
     convertor.put(new Integer(204), "&Igrave;");
     convertor.put(new Integer(205), "&Iacute;");
     convertor.put(new Integer(206), "&Icirc;");
     convertor.put(new Integer(207), "&Iuml;");
     convertor.put(new Integer(208), "&ETH;");
     convertor.put(new Integer(209), "&Ntilde;");
     convertor.put(new Integer(210), "&Ograve;");
     convertor.put(new Integer(211), "&Oacute;");
     convertor.put(new Integer(212), "&Ocirc;");
     convertor.put(new Integer(213), "&Otilde;");
     convertor.put(new Integer(214), "&Ouml;");
     convertor.put(new Integer(215), "&times;");
     convertor.put(new Integer(216), "&Oslash;");
     convertor.put(new Integer(217), "&Ugrave;");
     convertor.put(new Integer(218), "&Uacute;");
     convertor.put(new Integer(219), "&Ucirc;");
     convertor.put(new Integer(220), "&Uuml;");
     convertor.put(new Integer(221), "&Yacute;");
     convertor.put(new Integer(222), "&THORN;");
     convertor.put(new Integer(223), "&szlig;");
     convertor.put(new Integer(224), "&agrave;");
     convertor.put(new Integer(225), "&aacute;");
     convertor.put(new Integer(226), "&acirc;");
     convertor.put(new Integer(227), "&atilde;");
     convertor.put(new Integer(228), "&auml;");
     convertor.put(new Integer(229), "&aring;");
     convertor.put(new Integer(230), "&aelig;");
     convertor.put(new Integer(231), "&ccedil;");
     convertor.put(new Integer(232), "&egrave;");
     convertor.put(new Integer(233), "&eacute;");
     convertor.put(new Integer(234), "&ecirc;");
     convertor.put(new Integer(235), "&euml;");
     convertor.put(new Integer(236), "&igrave;");
     convertor.put(new Integer(237), "&iacute;");
     convertor.put(new Integer(238), "&icirc;");
     convertor.put(new Integer(239), "&iuml;");
     convertor.put(new Integer(240), "&eth;");
     convertor.put(new Integer(241), "&ntilde;");
     convertor.put(new Integer(242), "&ograve;");
     convertor.put(new Integer(243), "&oacute;");
     convertor.put(new Integer(244), "&ocirc;");
     convertor.put(new Integer(245), "&otilde;");
     convertor.put(new Integer(246), "&ouml;");
     convertor.put(new Integer(247), "&divide;");
     convertor.put(new Integer(248), "&oslash;");
     convertor.put(new Integer(249), "&ugrave;");
     convertor.put(new Integer(250), "&uacute;");
     convertor.put(new Integer(251), "&ucirc;");
     convertor.put(new Integer(252), "&uuml;");
     convertor.put(new Integer(253), "&yacute;");
     convertor.put(new Integer(254), "&thorn;");
     convertor.put(new Integer(255), "&yuml;");
    }

    /**
     * 
     */ 
    public final static String encode (String what) {
	// try to make stringbuffer large enough from the start
	StringBuffer ret = new StringBuffer (Math.round (what.length()*1.4f));
	encode (what, ret);
	return ret.toString(); 
    }
    
    /**
     *  
     */ 
    public final static void encode (String what, StringBuffer ret) {
	if  (what == null || what.length() == 0) {
	    return;
	}

	StringReader in = new StringReader (what);
	int c;
	boolean closeTag=false, readTag=false, tagOpen=false;
	// the difference between swallowOneNewline and ignoreNewline is that swallowOneNewline is just effective once (for the next newline)
             boolean ignoreNewline = false;
	boolean swallowOneNewline = false;
	StringBuffer tag = new StringBuffer ();
	try {
	    while ((c = in.read()) != -1) {
	        if (readTag) {
	             if (Character.isLetterOrDigit ((char) c))
	                 tag.append ((char) c);
	             else if ('/' == c)
	                 closeTag = true;
	             else {
	                 String t = tag.toString ();
	                 // set ignoreNewline on some tags, depending on wheather they're
	                 // being opened or closed.
	                 // what's going on here? we switch newline encoding on inside some tags, for
	                 // others we switch it on when they're closed
	                 if ("td".equalsIgnoreCase (t) || "th".equalsIgnoreCase (t) || "li".equalsIgnoreCase (t)) {
	                     ignoreNewline = closeTag;
	                     swallowOneNewline = true;
	                 } else if ("table".equalsIgnoreCase (t) || "ul".equalsIgnoreCase (t) || "ol".equalsIgnoreCase (t) || "pre".equalsIgnoreCase (t)) {
	                     ignoreNewline = !closeTag;
	                     swallowOneNewline = true;
	                 } else if ("p".equalsIgnoreCase (t)) {
	                     swallowOneNewline = true;
	                 }

	                 readTag = false;
	                 closeTag = false;
	                 tag.setLength (0);
	             }
	        } // if (readTag)

	        switch (c) {
	        // case '&':
                     //    ret.append ("&amp;");
	        //    break;
	        case  '\n':
	            ret.append ('\n');
                         if (!ignoreNewline && !swallowOneNewline)
	                ret.append ("<br>");
	            if (!tagOpen)
	                swallowOneNewline = false;
	            break;
	        case '<':
	            closeTag = false;
	            readTag = true;
	            tagOpen = true;
	            ret.append ('<');
	            break;
	        case '>':
	            tagOpen = false;
	            ret.append ('>');
	            break;
	        default:
	             if (c < 160)
	                 ret.append ((char) c);
	             else if (c >= 160 && c <= 255)
	                 ret.append (convertor.get(new Integer(c)));
	             else {
	                 ret.append ("&#");
	                 ret.append (c);
	                 ret.append (";");
	             }
	             if (!tagOpen && !Character.isWhitespace ((char)c))
	                 swallowOneNewline = false;
	        }
	    }
	} catch (IOException e) {}
     }

    /**
     *
     */
    public final static String encodeFormValue (String what) {
	StringBuffer ret = new StringBuffer (Math.round (what.length()*1.4f));
	encodeAll (what, ret, false);
	return ret.toString();
    }


    /**
     *  
     */ 
    public final static String encodeAll (String what) {
	StringBuffer ret = new StringBuffer (Math.round (what.length()*1.4f));
	encodeAll (what, ret, true);
	return ret.toString(); 
    }

    /**
     *  
     */
    public final static String encodeAll (String what, StringBuffer ret) {
	encodeAll (what, ret, true);
	return ret.toString();
    }


    /**
     *  
     */ 
    public final static void encodeAll (String what, StringBuffer ret, boolean encodeNewline) {
	if  (what == null || what.length() == 0) {
	    return;
	}

	StringReader in = new StringReader (what);
	int c;
	try {
	    while ((c = in.read()) != -1) {
	      switch (c) {
	         case '<' :
	            ret.append ("&lt;");
	            break;
	         case '>':
	            ret.append ("&gt;");
	            break;
	         case '&':
	            ret.append ("&amp;");
	            break;
	         case '"':
	            ret.append ("&quot;");
	            break;
	         case  '\n':
	            ret.append ('\n');
	            if (encodeNewline) {
	                ret.append ("<br>");
	                break;
	            }
	         default: 
	             if (c < 160)
	                 ret.append ((char) c);
	             else if (c >= 160 && c <= 255)
	                 ret.append (convertor.get(new Integer(c)));
	             else {
	                 ret.append ("&#");
	                 ret.append (c);
	                 ret.append (";");
	             }
	        }
	    }
	} catch (IOException e) {}
     }

    public final static String encodeSoft (String what) {
	StringBuffer ret = new StringBuffer (Math.round (what.length()*1.4f));
	encodeSoft (what, ret);
	return ret.toString(); 
    }
    
    public final static void encodeSoft (String what, StringBuffer ret) {
	if  (what == null || what.length() == 0) {
	    return;
	}

	StringReader in = new StringReader (what);
	int c;
	try {
	    while ((c = in.read()) != -1) {
	      switch (c) {
 	         case 128: // Euro-Symbol. This is for missing Unicode support in TowerJ.
                         ret.append ("&#8364;");
	            break; 
	         default: 
	             if (c < 160)
	                 ret.append ((char) c);
	             else if (c >= 160 && c <= 255)
	                 ret.append (convertor.get(new Integer(c)));
	             else {
	                 ret.append ("&#");
	                 ret.append (c);
	                 ret.append (";");
	             }
	        }
	    }
	} catch (IOException e) {}
     }


    public final static String encodeXml (String what) {
	StringBuffer ret = new StringBuffer (Math.round (what.length()*1.4f));
	encodeXml (what, ret);
	return ret.toString();
    }

    public final static void encodeXml (String what, StringBuffer ret) {
	if  (what == null || what.length() == 0) {
	    return;
	}

	StringReader in = new StringReader (what);
	int c;
	try {
	    while ((c = in.read()) != -1) {
	      switch (c) {
	         case '<' :
	            ret.append ("&lt;");
	            break;
	         case '>':
	            ret.append ("&gt;");
	            break;
	         case '&':
	            ret.append ("&amp;");
	            break;
	         default:
	             ret.append ((char) c);
	        }
	    }
	} catch (IOException e) {}
     }



}
