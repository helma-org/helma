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

package helma.util;

import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.text.*;
import java.util.*;

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

    static final HashSet allTags = new HashSet();

    static {
        allTags.add("a");
        allTags.add("abbr");
        allTags.add("acronym");
        allTags.add("address");
        allTags.add("applet");
        allTags.add("area");
        allTags.add("b");
        allTags.add("base");
        allTags.add("basefont");
        allTags.add("bdo");
        allTags.add("bgsound");
        allTags.add("big");
        allTags.add("blink");
        allTags.add("blockquote");
        allTags.add("bq");
        allTags.add("body");
        allTags.add("br");
        allTags.add("button");
        allTags.add("caption");
        allTags.add("center");
        allTags.add("cite");
        allTags.add("code");
        allTags.add("col");
        allTags.add("colgroup");
        allTags.add("del");
        allTags.add("dfn");
        allTags.add("dir");
        allTags.add("div");
        allTags.add("dl");
        allTags.add("dt");
        allTags.add("dd");
        allTags.add("em");
        allTags.add("embed");
        allTags.add("fieldset");
        allTags.add("font");
        allTags.add("form");
        allTags.add("frame");
        allTags.add("frameset");
        allTags.add("h1");
        allTags.add("h2");
        allTags.add("h3");
        allTags.add("h4");
        allTags.add("h5");
        allTags.add("h6");
        allTags.add("head");
        allTags.add("html");
        allTags.add("hr");
        allTags.add("i");
        allTags.add("iframe");
        allTags.add("img");
        allTags.add("input");
        allTags.add("ins");
        allTags.add("isindex");
        allTags.add("kbd");
        allTags.add("label");
        allTags.add("legend");
        allTags.add("li");
        allTags.add("link");
        allTags.add("listing");
        allTags.add("map");
        allTags.add("marquee");
        allTags.add("menu");
        allTags.add("meta");
        allTags.add("nobr");
        allTags.add("noframes");
        allTags.add("noscript");
        allTags.add("object");
        allTags.add("ol");
        allTags.add("option");
        allTags.add("optgroup");
        allTags.add("p");
        allTags.add("param");
        allTags.add("plaintext");
        allTags.add("pre");
        allTags.add("q");
        allTags.add("s");
        allTags.add("samp");
        allTags.add("script");
        allTags.add("select");
        allTags.add("small");
        allTags.add("span");
        allTags.add("strike");
        allTags.add("strong");
        allTags.add("style");
        allTags.add("sub");
        allTags.add("sup");
        allTags.add("table");
        allTags.add("tbody");
        allTags.add("td");
        allTags.add("textarea");
        allTags.add("tfoot");
        allTags.add("th");
        allTags.add("thead");
        allTags.add("title");
        allTags.add("tr");
        allTags.add("tt");
        allTags.add("u");
        allTags.add("ul");
        allTags.add("var");
        allTags.add("wbr");
        allTags.add("xmp");
    }

    // HTML block tags need to suppress automatic newline to <br>
    // conversion around them to look good. However, they differ 
    // in how many newlines around them should ignored. These sets
    // help to treat each tag right in newline conversion.
    static final HashSet swallowAll = new HashSet();
    static final HashSet swallowTwo = new HashSet();
    static final HashSet swallowOne = new HashSet();

    static {
        // actual block level elements
        swallowOne.add("address");
        swallowTwo.add("blockquote");
        swallowTwo.add("center");
        swallowOne.add("dir");
        swallowOne.add("div");
        swallowTwo.add("dl");
        swallowTwo.add("fieldset");
        swallowTwo.add("form");
        swallowTwo.add("h1");
        swallowTwo.add("h2");
        swallowTwo.add("h3");
        swallowTwo.add("h4");
        swallowTwo.add("h5");
        swallowTwo.add("h6");
        swallowTwo.add("hr");
        swallowTwo.add("isindex");
        swallowAll.add("menu");
        swallowAll.add("noframes");
        swallowAll.add("noscript");
        swallowTwo.add("ol");
        swallowTwo.add("p");
        swallowTwo.add("pre");
        swallowOne.add("table");
        swallowTwo.add("ul");

        /// to be treated as block level elements
        swallowTwo.add("dd");
        swallowTwo.add("dt");
        swallowTwo.add("frameset");
        swallowTwo.add("li");
        swallowAll.add("tbody");
        swallowTwo.add("td");
        swallowAll.add("tfoot");
        swallowOne.add("th");
        swallowAll.add("thead");
        swallowAll.add("tr");
    }

    /**
     *  Do "smart" encodging on a string. This means that valid HTML entities and tags,
     *  Helma macros and HTML comments are passed through unescaped, while
     *  other occurrences of '<', '>' and '&' are encoded to HTML entities.
     */
    public final static String encode(String str) {
        if (str == null) {
            return null;
        }

        int l = str.length();

        if (l == 0) {
            return "";
        }

        // try to make stringbuffer large enough from the start
        StringBuffer ret = new StringBuffer(Math.round(l * 1.4f));

        encode(str, ret, null);

        return ret.toString();
    }

    /**
     *  Do "smart" encodging on a string. This means that valid HTML entities and tags,
     *  Helma macros and HTML comments are passed through unescaped, while
     *  other occurrences of '<', '>' and '&' are encoded to HTML entities.
     */
    public final static void encode(String str, StringBuffer ret) {
        encode(str, ret, null);
    }

    /**
     *  Do "smart" encodging on a string. This means that valid HTML entities and tags,
     *  Helma macros and HTML comments are passed through unescaped, while
     *  other occurrences of '<', '>' and '&' are encoded to HTML entities.
     */
    public final static void encode(String str, StringBuffer ret, Set allowedTags) {
        if (str == null) {
            return;
        }

        int l = str.length();

        Stack openTags = new Stack();

        // are we currently within a < and a > that consitute some kind of tag?
        // we use tag balancing to know whether we are inside a tag (and should
        // pass things through unchanged) or outside (and should encode stuff).
        boolean insideTag = false;

        // are we inside an HTML tag?
        boolean insideHtmlTag = false;

        // if we are inside a <code> tag, we encode everything to make
        // documentation work easier
        boolean insideCodeTag = false;
        boolean insidePreTag = false;

        // are we within a Helma <% macro %> tag? We treat macro tags and
        // comments specially, since we can't rely on tag balancing
        // to know when we leave a macro tag or comment.
        boolean insideMacroTag = false;

        // are we inside an HTML comment?
        boolean insideComment = false;

        // the quotation mark we are in within an HTML or Macro tag, if any
        char htmlQuoteChar = '\u0000';
        char macroQuoteChar = '\u0000';

        // number of newlines to ignore in \n -> <br> conversion
        int swallowLinebreaks = 0;

        // number of newlines met since the last non-whitespace character
        int linebreaks = 0;

        // did we meet a backslash escape?
        boolean escape = false;

        for (int i = 0; i < l; i++) {
            char c = str.charAt(i);

            // step one: check if this is the beginning of an HTML tag, comment or
            // Helma macro.
            if (c == '<') {
                if (i < (l - 2)) {
                    if (!insideMacroTag && ('%' == str.charAt(i + 1))) {
                        // this is the beginning of a Helma macro tag
                        if (!insideCodeTag) {
                            insideMacroTag = insideTag = true;
                            macroQuoteChar = '\u0000';
                        }
                    } else if (('!' == str.charAt(i + 1)) && ('-' == str.charAt(i + 2))) {
                        // the beginning of an HTML comment?
                        if (!insideCodeTag) {
                            insideComment = insideTag = ((i < (l - 3)) &&
                                                        ('-' == str.charAt(i + 3)));
                        }
                    } else if (!insideTag) {
                        // check if this is a HTML tag.
                        boolean insideCloseTag = ('/' == str.charAt(i + 1));
                        int tagStart = insideCloseTag ? (i + 2) : (i + 1);
                        int j = tagStart;

                        while ((j < l) && Character.isLetterOrDigit(str.charAt(j)))
                            j++;

                        if ((j > tagStart) && (j < l)) {
                            String tagName = str.substring(tagStart, j).toLowerCase();

                            if ("code".equals(tagName) && insideCloseTag &&
                                    insideCodeTag) {
                                insideCodeTag = false;
                            }

                            if (((allowedTags == null) || allowedTags.contains(tagName)) &&
                                    allTags.contains(tagName) && !insideCodeTag) {
                                insideHtmlTag = insideTag = true;
                                htmlQuoteChar = '\u0000';

                                // set ignoreNewline on some tags, depending on wheather they're
                                // being opened or closed.
                                // what's going on here? we switch newline encoding on inside some tags, for
                                // others we switch it on when they're closed
                                linebreaks = Math.max(linebreaks - swallowLinebreaks, 0);

                                if (swallowAll.contains(tagName)) {
                                    swallowLinebreaks = 1000;
                                } else if (swallowTwo.contains(tagName)) {
                                    swallowLinebreaks = 2;
                                } else if (swallowOne.contains(tagName)) {
                                    swallowLinebreaks = 1;
                                } else {
                                    swallowLinebreaks = 0;
                                }

                                if (insideCloseTag) {
                                    int t = openTags.search(tagName);

                                    if (t == -1) {
                                        i = j;
                                        insideHtmlTag = insideTag = false;

                                        continue;
                                    } else if (t > 1) {
                                        for (int k = 1; k < t; k++) {
                                            ret.append("</");
                                            ret.append(openTags.pop());
                                            ret.append(">");
                                        }
                                    }

                                    openTags.pop();
                                } else {
                                    openTags.push(tagName);
                                    swallowLinebreaks = Math.max(swallowLinebreaks - 1, 0);
                                }

                                if ("code".equals(tagName) && !insideCloseTag) {
                                    insideCodeTag = true;
                                }

                                if ("pre".equals(tagName)) {
                                    insidePreTag = !insideCloseTag;
                                }
                            }
                        }
                    }
                }
                 // if (i < l-2)
            }

            if ((linebreaks > 0) && !Character.isWhitespace(c)) {
                if (!insidePreTag && (linebreaks > swallowLinebreaks)) {
                    linebreaks -= swallowLinebreaks;

                    for (int k = 0; k < linebreaks; k++)
                        ret.append("<br />\n");
                }

                if (!insideTag) {
                    swallowLinebreaks = 0;
                }

                linebreaks = 0;
            }

            switch (c) {
                case '<':

                    if (insideTag) {
                        ret.append('<');
                    } else {
                        ret.append("&lt;");
                    }

                    break;

                case '&':

                    // check if this is an HTML entity already,
                    // in which case we pass it though unchanged
                    if ((i < (l - 3)) && !insideCodeTag) {
                        // is this a numeric entity?
                        if (str.charAt(i + 1) == '#') {
                            int j = i + 2;

                            while ((j < l) && Character.isDigit(str.charAt(j)))
                                j++;

                            if ((j < l) && (str.charAt(j) == ';')) {
                                ret.append("&");

                                break;
                            }
                        } else {
                            int j = i + 1;

                            while ((j < l) && Character.isLetterOrDigit(str.charAt(j)))
                                j++;

                            if ((j < l) && (str.charAt(j) == ';')) {
                                ret.append("&");

                                break;
                            }
                        }
                    }

                    // we didn't reach a break, so encode the ampersand as HTML entity
                    ret.append("&amp;");

                    break;

                case '\\':
                    ret.append(c);

                    if (insideTag && !insideComment) {
                        escape = !escape;
                    }

                    break;

                case '"':
                case '\'':
                    ret.append(c);

                    if (!insideComment) {
                        // check if the quote is escaped
                        if (insideMacroTag) {
                            if (escape) {
                                escape = false;
                            } else if (macroQuoteChar == c) {
                                macroQuoteChar = '\u0000';
                            } else if (macroQuoteChar == '\u0000') {
                                macroQuoteChar = c;
                            }
                        } else if (insideHtmlTag) {
                            if (escape) {
                                escape = false;
                            } else if (htmlQuoteChar == c) {
                                htmlQuoteChar = '\u0000';
                            } else if (htmlQuoteChar == '\u0000') {
                                htmlQuoteChar = c;
                            }
                        }
                    }

                    break;

                case '\n':
                    ret.append('\n');

                    if (!insideTag) {
                        linebreaks++;
                    }

                    break;

                case '>':

                    // For Helma macro tags and comments, we overrule tag balancing,
                    // i.e. we don't require that '<' and '>' be balanced within
                    // macros and comments. Rather, we check for the matching closing tag.
                    if (insideComment) {
                        ret.append('>');
                        insideComment = !((str.charAt(i - 2) == '-') &&
                                        (str.charAt(i - 1) == '-'));
                    } else if (insideMacroTag) {
                        ret.append('>');
                        insideMacroTag = !((str.charAt(i - 1) == '%') &&
                                         (macroQuoteChar == '\u0000'));
                    } else if (insideHtmlTag) {
                        ret.append('>');

                        // only leave HTML tag if quotation marks are balanced
                        // within that tag.
                        insideHtmlTag = htmlQuoteChar != '\u0000';

                        // Check if this is an empty tag so we don't generate an
                        // additional </close> tag.
                        if (str.charAt(i - 1) == '/') {
                            openTags.pop();
                        }
                    } else {
                        ret.append("&gt;");
                    }

                    // check if we still are inside any kind of tag
                    insideTag = insideComment || insideMacroTag || insideHtmlTag;

                    break;

                default:

                    // ret.append (c);
                    if (c < 128) {
                        ret.append(c);
                    } else if ((c >= 128) && (c < 256)) {
                        ret.append(transform[c - 128]);
                    } else {
                        ret.append("&#");
                        ret.append((int) c);
                        ret.append(";");
                    }

                    escape = false;
            }
        }

        // if tags were opened but not closed, close them.
        int o = openTags.size();

        if (o > 0) {
            for (int k = 0; k < o; k++) {
                ret.append("</");
                ret.append(openTags.pop());
                ret.append(">");
            }
        }

        // add remaining newlines we may have collected
        if ((linebreaks > 0) && (linebreaks > swallowLinebreaks)) {
            linebreaks -= swallowLinebreaks;

            for (int i = 0; i < linebreaks; i++)
                ret.append("<br />\n");
        }
    }

    /**
     *
     */
    public final static String encodeFormValue(String str) {
        if (str == null) {
            return null;
        }

        int l = str.length();

        if (l == 0) {
            return "";
        }

        StringBuffer ret = new StringBuffer(Math.round(l * 1.2f));

        encodeAll(str, ret, false);

        return ret.toString();
    }

    /**
     *
     */
    public final static void encodeFormValue(String str, StringBuffer ret) {
        encodeAll(str, ret, false);
    }

    /**
     *
     */
    public final static String encodeAll(String str) {
        if (str == null) {
            return null;
        }

        int l = str.length();

        if (l == 0) {
            return "";
        }

        StringBuffer ret = new StringBuffer(Math.round(l * 1.2f));

        encodeAll(str, ret, true);

        return ret.toString();
    }

    /**
     *
     */
    public final static void encodeAll(String str, StringBuffer ret) {
        encodeAll(str, ret, true);
    }

    /**
     *
     */
    public final static void encodeAll(String str, StringBuffer ret, boolean encodeNewline) {
        if (str == null) {
            return;
        }

        int l = str.length();

        for (int i = 0; i < l; i++) {
            char c = str.charAt(i);

            switch (c) {
                case '<':
                    ret.append("&lt;");

                    break;

                case '>':
                    ret.append("&gt;");

                    break;

                case '&':
                    ret.append("&amp;");

                    break;

                case '"':
                    ret.append("&quot;");

                    break;

                case '\n':
                    ret.append('\n');

                    if (encodeNewline) {
                        ret.append("<br />");
                    }

                    break;

                default:

                    // ret.append (c);
                    if (c < 128) {
                        ret.append(c);
                    } else if ((c >= 128) && (c < 256)) {
                        ret.append(transform[c - 128]);
                    } else {
                        ret.append("&#");
                        ret.append((int) c);
                        ret.append(";");
                    }
            }
        }
    }

    /**
     *
     *
     * @param str ...
     *
     * @return ...
     */
    public final static String encodeXml(String str) {
        if (str == null) {
            return null;
        }

        int l = str.length();

        if (l == 0) {
            return "";
        }

        StringBuffer ret = new StringBuffer(Math.round(l * 1.2f));

        encodeXml(str, ret);

        return ret.toString();
    }

    /**
     *
     *
     * @param str ...
     * @param ret ...
     */
    public final static void encodeXml(String str, StringBuffer ret) {
        if (str == null) {
            return;
        }

        int l = str.length();

        for (int i = 0; i < l; i++) {
            char c = str.charAt(i);

            switch (c) {
                case '<':
                    ret.append("&lt;");

                    break;

                case '>':
                    ret.append("&gt;");

                    break;

                case '&':
                    ret.append("&amp;");

                    break;

                case '"':
                    ret.append("&quot;");

                    break;

                case '\'':
                    ret.append("&apos;");

                    break;

                default:

                    if (c < 0x20) {
                        // sort out invalid XML characters below 0x20 - all but 0x9, 0xA and 0xD.
                        // The trick is an adaption of java.lang.Character.isSpace().
                        if (((((1L << 0x9) | (1L << 0xA) | (1L << 0xD)) >> c) & 1L) != 0) {
                            ret.append(c);
                        }
                    } else {
                        ret.append(c);
                    }
            }
        }
    }

    // test method
    public static String printCharRange(int from, int to) {
        StringBuffer response = new StringBuffer();

        for (int i = from; i < to; i++) {
            response.append(i);
            response.append("      ");
            response.append((char) i);
            response.append("      ");

            if (i < 128) {
                response.append((char) i);
            } else if ((i >= 128) && (i < 256)) {
                response.append(transform[i - 128]);
            } else {
                response.append("&#");
                response.append(i);
                response.append(";");
            }

            response.append("\r\n");
        }

        return response.toString();
    }

    // for testing...
    public static void main(String[] args) {
        for (int i = 0; i < args.length; i++)
            System.err.println(encode(args[i]));
    }
}
 // end of class
