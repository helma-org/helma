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

import org.apache.html.dom.*;
import org.w3c.dom.html.HTMLDocument;
import org.xml.sax.SAXException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.html.parser.*;

/**
 * 
 */
public class HtmlParser extends Parser {
    static final HashSet stopNone = new HashSet();
    static final HashSet stopTable = new HashSet();
    static final HashSet stopList = new HashSet();
    static final HashSet stopDeflist = new HashSet();

    static {
        stopTable.add("TABLE");
        stopList.add("TABLE");
        stopList.add("UL");
        stopList.add("OL");
        stopDeflist.add("TABLE");
        stopDeflist.add("DL");
    }

    HTMLBuilder builder;
    Attributes attributes = new Attributes();
    Stack stack = new Stack();

    /**
     * Creates a new HtmlParser object.
     *
     * @throws IOException ...
     */
    public HtmlParser() throws IOException {
        super(DTD.getDTD("html32"));

        // define elements to be treated as container tags, and undefine those
        // to be treated as empty tags.
        dtd.getElement("table");
        dtd.getElement("tr");
        dtd.getElement("td");
        dtd.getElement("span");
        dtd.getElement("div");
        dtd.getElement("font");
        dtd.getElement("b");
        dtd.getElement("i");
        dtd.getElement("a");
        dtd.getElement("blockquote");
        dtd.getElement("em");
        dtd.getElement("ul");
        dtd.getElement("ol");
        dtd.getElement("li");
        dtd.getElement("dl");
        dtd.getElement("dt");
        dtd.getElement("dd");
        dtd.getElement("h1");
        dtd.getElement("h2");
        dtd.getElement("h3");
        dtd.getElement("h4");
        dtd.getElement("h5");
        dtd.getElement("h6");
        dtd.getElement("form");
        dtd.getElement("option");
        dtd.elementHash.remove("meta");
        dtd.elementHash.remove("link");
        dtd.elementHash.remove("base");
        builder = new HTMLBuilder();

        try {
            builder.startDocument();
        } catch (SAXException x) {
            System.err.println("Error in constructor");
        }
    }

    /**
     * Handle Start Tag.
     */
    protected void handleStartTag(TagElement tag) {
        // System.err.println ("handleStartTag ("+tag.getHTMLTag()+")");
        attributes.convert(getAttributes());
        flushAttributes();

        String tagname = tag.getHTMLTag().toString().toUpperCase();

        // immediately empty A anchor tag
        if ("A".equals(tagname) && (attributes.getValue("href") == null)) {
            try {
                builder.startElement(tagname, attributes);
                builder.endElement(tagname);

                return;
            } catch (SAXException x) {
            }
        }

        if ("TD".equals(tagname)) {
            closeOpenTags("TD", stopTable, 10);
        } else if ("TR".equals(tagname)) {
            closeOpenTags("TR", stopTable, 10);
        } else if ("LI".equals(tagname)) {
            closeOpenTags("LI", stopList, 6);
        } else if ("DT".equals(tagname) || "DD".equals(tagname)) {
            closeOpenTags("DT", stopDeflist, 6);
            closeOpenTags("DL", stopDeflist, 6);
        } else if ("OPTION".equals(tagname)) {
            closeOpenTags("OPTION", stopNone, 1);
        } else if ("P".equals(tagname)) {
            closeOpenTags("P", stopNone, 1);
        }

        stack.push(tagname);

        try {
            builder.startElement(tagname, attributes);
        } catch (SAXException x) {
            System.err.println("Error in handleStartTag");
        }
    }

    /**
     * Handle End Tag.
     */
    protected void handleEndTag(TagElement tag) {
        // System.err.println ("handleEndTag ("+tag.getHTMLTag()+")");
        String tagname = tag.getHTMLTag().toString().toUpperCase();

        try {
            if (tagname.equals(stack.peek())) {
                stack.pop();
            }
        } catch (EmptyStackException es) {
        }

        try {
            builder.endElement(tagname);
        } catch (SAXException x) {
            System.err.println("Error in handleEndTag: " + x);
        }
    }

    /**
     * Handle Empty Tag.
     */
    protected void handleEmptyTag(TagElement tag) {
        // System.err.println ("handleEmptyTag ("+tag.getHTMLTag()+")");
        attributes.convert(getAttributes());
        flushAttributes();

        String tagname = tag.getHTMLTag().toString().toUpperCase();

        try {
            builder.startElement(tagname, attributes);
            builder.endElement(tagname);
        } catch (SAXException x) {
            System.err.println("Error in handleEmptyTag: " + x);
        }
    }

    /**
     * Handle Text.
     */
    protected void handleText(char[] data) {
        // System.err.println ("handleText ("+new String (data)+")");
        try {
            builder.characters(data, 0, data.length);
        } catch (SAXException x) {
            System.err.println("Error in handleText");
        }
    }

    /*
     * Error handling.
     */
    protected void handleError(int ln, String errorMsg) {
        // System.err.println ("handleError ("+ln+": "+errorMsg+")");
    }

    /**
     *  Handle comment.
     */
    protected void handleComment(char[] data) {
        // System.err.println ("handleComment ("+new String (data)+")");

        /* try {
           builder.characters (data, 0, data.length);
           } catch (SAXException x) {
               System.err.println ("Error in handleComment");
           }*/
    }

    /**
     *
     *
     * @return ...
     */
    public HTMLDocument getDocument() {
        try {
            builder.endDocument();
        } catch (SAXException x) {
        }

        return builder.getHTMLDocument();
    }

    private void closeOpenTags(String until, HashSet stoppers, int maxdepth) {
        int l = stack.size();
        int stop = Math.max(0, l - maxdepth);
        int found = -1;

        for (int i = l - 1; i >= stop; i--) {
            Object o = stack.elementAt(i);

            if (stoppers.contains(o)) {
                return;
            }

            if (until.equals(o)) {
                found = i;

                break;
            }
        }

        if (found > -1) {
            for (int i = l - 1; i >= found; i--) {
                try {
                    String t = (String) stack.pop();

                    builder.endElement(t);
                } catch (Exception x) {
                }
            }
        }
    }

    class Attributes implements org.xml.sax.AttributeList {
        HashMap map = new HashMap();
        ArrayList names = new ArrayList();
        ArrayList values = new ArrayList();

        public int getLength() {
            return names.size();
        }

        public String getName(int i) {
            return (String) names.get(i);
        }

        public String getType(int i) {
            return "CDATA";
        }

        public String getType(String name) {
            return "CDATA";
        }

        public String getValue(int i) {
            return (String) values.get(i);
        }

        public String getValue(String name) {
            return (String) map.get(name);
        }

        public void convert(SimpleAttributeSet attset) {
            map.clear();
            names.clear();
            values.clear();

            for (Enumeration e = attset.getAttributeNames(); e.hasMoreElements();) {
                Object name = e.nextElement();
                Object value = attset.getAttribute(name).toString();

                name = name.toString().toLowerCase();
                map.put(name, value);
                names.add(name);
                values.add(value);
            }
        }
    }
}
