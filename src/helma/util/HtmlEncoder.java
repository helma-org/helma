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

    // transformation table for characters 128 to 255. These actually fall into two 
	// groups, put together for efficiency: "Windows" chacacters 128-159 such as 
	// "smart quotes", which are encoded to valid Unicode entities, and 
	// valid ISO-8859 caracters 160-255, which are encoded to the symbolic HTML
	// entity. Everything >= 256 is encoded to a numeric entity.
	// 
    // for mor on HTML entities see http://www.pemberley.com/janeinfo/latin1.html  and
	// ftp://ftp.unicode.org/Public/MAPPINGS/VENDORS/MICSFT/WINDOWS/CP1252.TXT
	//
    static final String[] transform =  {
        "&euro;",   // 128
        "",           // empty string means character is undefined in unicode
        "&#8218;", 
        "&#402;",
        "&#8222;",
        "&#8230;",
        "&#8224;",
        "&#8225;",
        "&#710;",
        "&#8240;",
        "&#352;",
        "&#8249;",
        "&#338;",
        "",
        "&#381;",
        "",
        "",
        "&#8216;",
        "&#8217;",
        "&#8220;",
        "&#8221;",
        "&#8226;",
        "&#8211;",
        "&#8212;",
        "&#732;",
        "&#8482;",
        "&#353;",
        "&#8250;",
        "&#339;",
        "",
        "&#382;",
        "&#376;",  // 159
        "&nbsp;",    // 160
        "&iexcl;",
        "&cent;",
        "&pound;",
        "&curren;",
        "&yen;",
        "&brvbar;",
        "&sect;",
        "&uml;",
        "&copy;",
        "&ordf;",
        "&laquo;",
        "&not;",
        "&shy;",
        "&reg;",
        "&macr;",
        "&deg;",
        "&plusmn;",
        "&sup2;",
        "&sup3;",
        "&acute;",
        "&micro;",
        "&para;",
        "&middot;",
        "&cedil;",
        "&sup1;",
        "&ordm;",
        "&raquo;",
        "&frac14;",
        "&frac12;",
        "&frac34;",
        "&iquest;",
        "&Agrave;",
        "&Aacute;",
        "&Acirc;",
        "&Atilde;",
        "&Auml;",
        "&Aring;",
        "&AElig;",
        "&Ccedil;",
        "&Egrave;",
        "&Eacute;",
        "&Ecirc;",
        "&Euml;",
        "&Igrave;",
        "&Iacute;",
        "&Icirc;",
        "&Iuml;",
        "&ETH;",
        "&Ntilde;",
        "&Ograve;",
        "&Oacute;",
        "&Ocirc;",
        "&Otilde;",
        "&Ouml;",
        "&times;",
        "&Oslash;",
        "&Ugrave;",
        "&Uacute;",
        "&Ucirc;",
        "&Uuml;",
        "&Yacute;",
        "&THORN;",
        "&szlig;",
        "&agrave;",
        "&aacute;",
        "&acirc;",
        "&atilde;",
        "&auml;",
        "&aring;",
        "&aelig;",
        "&ccedil;",
        "&egrave;",
        "&eacute;",
        "&ecirc;",
        "&euml;",
        "&igrave;",
        "&iacute;",
        "&icirc;",
        "&iuml;",
        "&eth;",
        "&ntilde;",
        "&ograve;",
        "&oacute;",
        "&ocirc;",
        "&otilde;",
        "&ouml;",
        "&divide;",
        "&oslash;",
        "&ugrave;",
        "&uacute;",
        "&ucirc;",
        "&uuml;",
        "&yacute;",
        "&thorn;",
        "&yuml;"    // 255
    };


    /**
     * 
     */ 
    public final static String encode (String str) {
	// try to make stringbuffer large enough from the start
	StringBuffer ret = new StringBuffer (Math.round (str.length()*1.4f));
	encode (str, ret);
	return ret.toString(); 
    }
    
    /**
     *  
     */ 
    public final static void encode (String str, StringBuffer ret) {
	if  (str == null)
	    return;
	
	int l = str.length();
	
	boolean closeTag=false, readTag=false, tagOpen=false;
	// the difference between swallowOneNewline and ignoreNewline is that swallowOneNewline is just effective once (for the next newline)
	boolean ignoreNewline = false;
	boolean swallowOneNewline = false;
	StringBuffer tag = new StringBuffer ();
	
	for (int i=0; i<l; i++) {
	    char c = str.charAt (i);
	    if (readTag) {
	        if (Character.isLetterOrDigit (c))
	            tag.append (c);
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
	                ret.append ("<br />");
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
	             // ret.append (c);
	             if (c < 128)
	                 ret.append (c);
	             else if (c >= 128 && c < 256)
	                 ret.append (transform[c-128]);
	             else { 
	                 ret.append ("&#");
	                 ret.append ((int) c);
	                 ret.append (";");
	             }
	             if (!tagOpen && !Character.isWhitespace (c))
	                 swallowOneNewline = false;
	    }
	}
     }

    /**
     *
     */
    public final static String encodeFormValue (String str) {
	StringBuffer ret = new StringBuffer (Math.round (str.length()*1.2f));
	encodeAll (str, ret, false);
	return ret.toString();
    }


    /**
     *  
     */ 
    public final static String encodeAll (String str) {
	StringBuffer ret = new StringBuffer (Math.round (str.length()*1.2f));
	encodeAll (str, ret, true);
	return ret.toString(); 
    }

    /**
     *  
     */
    public final static String encodeAll (String str, StringBuffer ret) {
	encodeAll (str, ret, true);
	return ret.toString();
    }


    /**
     *  
     */ 
    public final static void encodeAll (String str, StringBuffer ret, boolean encodeNewline) {
	if  (str == null)
	    return;

	int l = str.length();
	for (int i=0; i<l; i++) {
	    char c = str.charAt (i);
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
	                ret.append ("<br />");
	            }
	            break;
	        default:
	             // ret.append (c);
	             if (c < 128)
	                 ret.append (c);
	             else if (c >= 128 && c < 256)
	                 ret.append (transform[c-128]);
	             else {
	                 ret.append ("&#");
	                 ret.append ((int) c);
	                 ret.append (";");
	             }
	    }
	}
     }


    public final static String encodeXml (String str) {
	StringBuffer ret = new StringBuffer (Math.round (str.length()*1.2f));
	encodeXml (str, ret);
	return ret.toString();
    }

    public final static void encodeXml (String str, StringBuffer ret) {
	if  (str == null)
	    return;

	int l = str.length();
	for (int i=0; i<l; i++) {
	    char c = str.charAt (i);
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
	             ret.append (c);
	    }
	}
     }

    // test method
	public static String printCharRange (int from, int to) {
	    StringBuffer response = new StringBuffer();
		for (int i=from;i<to;i++) {
			response.append (i);
			response.append ("      ");
			response.append ((char) i);
			response.append ("      ");
	        if (i < 128)
	            response.append ((char) i);
	        else if (i >= 128 && i < 256)
	            response.append (transform[i-128]);
	        else {
	            response.append ("&#");
	            response.append (i);
	            response.append (";");
	        }
			response.append ("\r\n");
		}
		return response.toString();
	}

} // end of class
