// HtmlParser.java
// Copyright (c) Hannes Wallnöfer 2002

package helma.util;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Enumeration;
import java.io.IOException;
import javax.swing.text.html.parser.*;
import javax.swing.text.SimpleAttributeSet;
import org.xml.sax.SAXException;
import org.apache.html.dom.*;
import org.w3c.dom.html.HTMLDocument;

public class HtmlParser extends Parser {

    HTMLBuilder builder;
    Attributes attributes = new Attributes ();

    public HtmlParser () throws IOException {
	super (DTD.getDTD ("html32"));
	dtd.getElement ("table");
	dtd.getElement ("tr");
	dtd.getElement ("td");
	dtd.getElement ("span");
	dtd.getElement ("div");
	dtd.getElement ("font");
	dtd.getElement ("b");
	dtd.getElement ("i");
	dtd.getElement ("a");
	dtd.getElement ("blockquote");
	dtd.getElement ("em");
	dtd.getElement ("ul");
	dtd.getElement ("ol");
	dtd.getElement ("li");
	dtd.elementHash.remove ("meta");
	dtd.elementHash.remove ("link");
	dtd.elementHash.remove ("base");
	builder = new HTMLBuilder ();
	try {
	    builder.startDocument ();
	} catch (SAXException x) {
	    System.err.println ("Error in constructor");
	}
    }

    /**
     * Handle Start Tag.
     */
    protected void handleStartTag(TagElement tag) {
	// System.err.println ("handleStartTag ("+tag.getHTMLTag()+")");
	attributes.convert (getAttributes());
	flushAttributes();
	try {
	    builder.startElement (tag.getHTMLTag().toString().toUpperCase(), attributes);
	} catch (SAXException x) {
	    System.err.println ("Error in handleStartTag");
	}
    }

    /**
     * Handle Empty Tag.
     */
    protected void handleEmptyTag(TagElement tag) {
	// System.err.println ("handleEmptyTag ("+tag.getHTMLTag()+")");
	attributes.convert (getAttributes());
	flushAttributes();
	try {
	    builder.startElement (tag.getHTMLTag().toString().toUpperCase(), attributes);
	    builder.endElement (tag.getHTMLTag().toString().toUpperCase());
	} catch (SAXException x) {
	    System.err.println ("Error in handleEmptyTag: "+x);
	}
    }

    /**
     * Handle End Tag.
     */
    protected void handleEndTag(TagElement tag) {
	// System.err.println ("handleEndTag ("+tag.getHTMLTag()+")");
	try {
	    builder.endElement (tag.getHTMLTag().toString().toUpperCase());
	} catch (SAXException x) {
	    System.err.println ("Error in handleEndTag: "+x);
	}
    }

    /**
     * Handle Text.
     */
    protected void handleText(char data[]) {
	// System.err.println ("handleText ("+new String (data)+")");
	try {
	    builder.characters (data, 0, data.length);
	} catch (SAXException x) {
	    System.err.println ("Error in handleText");
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
    protected void handleComment(char text[]) {
    }

    public HTMLDocument getDocument () {
	try {
	    builder.endDocument ();
	} catch (SAXException x) {}
	return builder.getHTMLDocument ();
    }


    class Attributes implements org.xml.sax.AttributeList {
	HashMap map = new HashMap();
	ArrayList names = new ArrayList();
	ArrayList values = new ArrayList ();

	public int getLength() {
	    return names.size();
	}

	public String getName (int i) {
	    return (String) names.get (i);
	}

	public String getType (int i) {
	    return "CDATA";
	}

	public String getType (String name) {
	    return "CDATA";
	}

	public String getValue (int i) {
	    return (String) values.get (i);
	}

	public String getValue (String name) {
	    return (String) map.get (name);
	}

	public void convert (SimpleAttributeSet attset) {
	    map.clear ();
	    names.clear ();
	    values.clear ();
	    for (Enumeration e = attset.getAttributeNames(); e.hasMoreElements(); ) {
	        Object name = e.nextElement ();
	        Object value = attset.getAttribute (name).toString();
	        map.put (name.toString(), value);
	        names.add (name.toString());
	        values.add (value);
	    }
	}

    }
}

