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


    static final HashSet allTags = new HashSet ();
    static {
	allTags.add ("a");
	allTags.add ("abbr");
	allTags.add ("address");
	allTags.add ("applet");
	allTags.add ("area");
	allTags.add ("b");
	allTags.add ("base");
	allTags.add ("basefont");
	allTags.add ("bgsound");
	allTags.add ("big");
	allTags.add ("blink");
	allTags.add ("blockquote");
	allTags.add ("bq");
	allTags.add ("body");
	allTags.add ("br");
	allTags.add ("button");
	allTags.add ("caption");
	allTags.add ("center");
	allTags.add ("cite");
	allTags.add ("code");
	allTags.add ("col");
	allTags.add ("colgroup");
	allTags.add ("del");
	allTags.add ("dir");
	allTags.add ("div");
	allTags.add ("dl");
	allTags.add ("dt");
	allTags.add ("dd");
	allTags.add ("em");
	allTags.add ("embed");
	allTags.add ("fieldset");
	allTags.add ("font");
	allTags.add ("form");
	allTags.add ("frame");
	allTags.add ("frameset");
	allTags.add ("h1");
	allTags.add ("h2");
	allTags.add ("h3");
	allTags.add ("h4");
	allTags.add ("h5");
	allTags.add ("h6");
	allTags.add ("head");
	allTags.add ("html");
	allTags.add ("i");
	allTags.add ("iframe");
	allTags.add ("img");
	allTags.add ("input");
	allTags.add ("ins");
	allTags.add ("isindex");
	allTags.add ("kbd");
	allTags.add ("li");
	allTags.add ("link");
	allTags.add ("listing");
	allTags.add ("map");
	allTags.add ("marquee");
	allTags.add ("menu");
	allTags.add ("meta");
	allTags.add ("nobr");
	allTags.add ("noframes");
	allTags.add ("object");
	allTags.add ("ol");
	allTags.add ("option");
	allTags.add ("optgroup");
	allTags.add ("p");
	allTags.add ("param");
	allTags.add ("plaintext");
	allTags.add ("pre");
	allTags.add ("q");
	allTags.add ("samp");
	allTags.add ("script");
	allTags.add ("select");
	allTags.add ("small");
	allTags.add ("span");
	allTags.add ("strike");
	allTags.add ("strong");
	allTags.add ("style");
	allTags.add ("sub");
	allTags.add ("sup");
	allTags.add ("table");
	allTags.add ("tbody");
	allTags.add ("td");
	allTags.add ("textarea");
	allTags.add ("tfoot");
	allTags.add ("th");
	allTags.add ("thead");
	allTags.add ("title");
	allTags.add ("tr");
	allTags.add ("tt");
	allTags.add ("u");
	allTags.add ("ul");
	allTags.add ("var");
	allTags.add ("wbr");
	allTags.add ("xmp");
	allTags.add ("%");
    }

    // tags which signal us to start suppressing \n -> <br> encoding
    // these are "structrural" tags, for example, we don't want to add <br>s 
    // between a <table> and a <tr>.
    static final HashSet suppressLinebreakTags = new HashSet ();
    static {
	suppressLinebreakTags.add ("table");
	suppressLinebreakTags.add ("ul");
	suppressLinebreakTags.add ("ol");
	suppressLinebreakTags.add ("pre");
    }

    // tags which signal us to stop suppressing \n -> <br> encoding
    // these usually signal transition from structural tags to normal
    // HTML text, e.g. <td>
    static final HashSet encodeLinebreakTags = new HashSet ();
    static {
	encodeLinebreakTags.add ("td");
	encodeLinebreakTags.add ("th");
	encodeLinebreakTags.add ("li");
    }

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

	char[] chars = str.toCharArray ();
	int l = chars.length;

	// are we currently within a < and a >?
	boolean insideTag=false;
	// if we are inside a <code> tag, we encode everything to make 
	// documentation work easier
	boolean insideCodeTag = false;
	// the difference between swallowOneNewline and ignoreNewline is that
	// swallowOneNewline is just effective once (for the next newline)
	boolean ignoreNewline = false;
	boolean swallowOneNewline = false;

	for (int i=0; i<l; i++) {
	    char c = chars[i];

	    switch (c) {
	        case '&':
	            // check if this is an HTML entity already, in which case we pass it though unchanged
	            if (i < l-4 && !insideCodeTag) {
	                // is this a numeric entity?
	                if (chars[i+1] == '#' ) {
	                   int j = i+2;
	                   while (j<l && Character.isDigit (chars[j]))
	                       j++;
	                   if (j<l && chars[j] == ';') {
	                       ret.append ("&");
	                       break;
	                   }
	                } else {
	                   int j = i+1;
	                   while (j<l && Character.isLetterOrDigit (chars[j]))
	                       j++;
	                   if (j<l && chars[j] == ';') {
	                       ret.append ("&");
	                       break;
	                   }
	                }
	            }
	            // we didn't reach a break, so encode the ampersand as HTML entity
	            ret.append ("&amp;");
	            break;
	        case '<':
	            if (i < l-2) {
	                boolean insideCloseTag = ('/' == chars[i+1]);
	                int tagStart = insideCloseTag ? i+2 : i+1;
	                int j = tagStart;
	                while (j<l && (Character.isLetterOrDigit (chars[j]) || chars[j] == '%'))
	                    j++;
	                if (j > tagStart && j < l) {
	                    String tagName = new String (chars, tagStart, j-tagStart).toLowerCase ();
	                    if ("code".equals (tagName) && insideCloseTag && insideCodeTag)
	                        insideCodeTag = false;
	                    if (allTags.contains (tagName) && !insideCodeTag) {
	                        insideTag = true;
	                        ret.append ('<');
	                        // set ignoreNewline on some tags, depending on wheather they're
	                        // being opened or closed.
	                        // what's going on here? we switch newline encoding on inside some tags, for
	                        // others we switch it on when they're closed
	                        if (encodeLinebreakTags.contains (tagName)) {
	                            ignoreNewline = insideCloseTag;
	                            swallowOneNewline = true;
	                        } else if (suppressLinebreakTags.contains (tagName)) {
	                            ignoreNewline = !insideCloseTag;
	                            swallowOneNewline = true;
	                        } else if ("p".equalsIgnoreCase (tagName) || 
	                                     "blockquote".equalsIgnoreCase (tagName) ||
	                                     "bq".equalsIgnoreCase (tagName)) {
	                            swallowOneNewline = true;
	                        }
	                        if ("code".equals (tagName) && !insideCloseTag)
	                            insideCodeTag = true;
	                        break;
	                    }
	                }
	            } // if (i < l-2)
	            ret.append ("&lt;");
	            break;
	        case  '\n':
	            ret.append ('\n');
	            if (!insideTag && !ignoreNewline && !swallowOneNewline)
	                ret.append ("<br />");
	            if (!insideTag)
	                swallowOneNewline = false;
	            break;
	        case '>':
	            if (insideTag)
	                ret.append ('>');
	            else
	                ret.append ("&gt;");
	            insideTag = false;
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
	            if (!insideTag && !Character.isWhitespace (c))
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
