package helma.objectmodel.dom;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Stack;

import javax.xml.parsers.*;

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import helma.objectmodel.INode;

public final class XmlReader extends DefaultHandler implements XmlConstants {

    private INode rootNode, currentNode;
    private Stack nodeStack;
    private HashMap convertedNodes;

    private String elementType = null;
    private String elementName = null;
    private StringBuffer charBuffer = null;

    static SAXParserFactory factory = SAXParserFactory.newInstance ();

    boolean parsingHopObject;

    public XmlReader () {
    }


    /**
     * main entry to read an xml-file.
     */
    public INode read (File file, INode helmaNode)
	throws ParserConfigurationException, SAXException, IOException {
	try {
	    return read (new FileInputStream(file), helmaNode);
	} catch (FileNotFoundException notfound) {
	    System.err.println ("couldn't find xml-file: " + file.getAbsolutePath ());
	    return helmaNode;
	}
    }

    /**
     * read an InputStream with xml-content.
     */
    public INode read (InputStream in, INode helmaNode)
	throws ParserConfigurationException, SAXException, IOException {
	return read (new InputSource (in), helmaNode);
    }

    /**
     * read an character reader with xml-content.
     */
    public INode read (Reader in, INode helmaNode)
	throws ParserConfigurationException, SAXException, IOException {
	return read (new InputSource (in), helmaNode);
    }

    /**
     * read an InputSource with xml-content.
     */
    public INode read (InputSource in, INode helmaNode)
	throws ParserConfigurationException, SAXException, IOException {
	if (helmaNode==null)
	    throw new RuntimeException ("Can't create a new Node without a root Node");

	SAXParser parser = factory.newSAXParser ();

	rootNode = helmaNode;
	currentNode = null;
	convertedNodes = new HashMap ();
	nodeStack = new Stack ();
	parsingHopObject = true;

	parser.parse (in, this);
	return rootNode;
    }


    public void startElement(String namespaceURI, String localName, String qName, Attributes atts)
                        throws SAXException {
	// System.err.println ("XML-READ: startElement "+namespaceURI+", "+localName+", "+qName+", "+atts.getValue("id"));
	// discard the first element called xmlroot
	if ("xmlroot".equals (qName) && currentNode == null)
	    return;
	// if currentNode is null, this must be the hopobject node
	String id = atts.getValue ("id");
	if (id != null) {
	    // check if there is a current node.
	    if (currentNode == null) {
	        // If currentNode is null, this is the root node we're parsing.
	        currentNode = rootNode;
	    } else if ("hop:child".equals (qName)) {
	        // it's an anonymous child node
	        nodeStack.push (currentNode);
	        currentNode = currentNode.createNode (null);
	    } else {
	        // it's a named node property
	        nodeStack.push (currentNode);
	        currentNode = currentNode.createNode (qName);
	    }
	    // set the prototype on the current node and
	    // add it to the map of parsed nodes.
	    String name = atts.getValue ("name");
	    String prototype = atts.getValue ("prototype");
	    if ( !"".equals(prototype) && !"hopobject".equals(prototype) )
	        currentNode.setPrototype (prototype);
	    String key = id + "-" + prototype;
	    convertedNodes.put( key, currentNode );
	    return;
	}
	
	// check if we have a currentNode to set properties on,
	// otherwise throw exception.
	if (currentNode == null)
	    throw new SAXException ("Invalid XML: No valid root HopObject found");
	// check if we are inside a HopObject - otherwise throw an exception
	if (!parsingHopObject)
	    throw new SAXException ("Invalid XML: Found nested non-HobObject elements");

	// if we got so far, the element is not a hopobject. Set flag to prevent
	// the hopobject stack to be popped when the element
	// is closed.
	parsingHopObject = false;

	// Is it a reference to an already parsed node?
	String idref = atts.getValue ("idref");
	if (idref != null) {
	    // a reference to a node that should have been parsed
	    // and lying in our cache of parsed nodes.
	    String prototyperef = atts.getValue ("prototyperef");
	    String key = idref + "-" + prototyperef;
	    INode n = (INode) convertedNodes.get (key);
	    if (n != null) {
	        if ("hop:child".equals (qName)) {
	           // add an already parsed node as child to current node
	           currentNode.addNode (n);
	        } else {
	           // set an already parsed node as node property to current node
	           currentNode.setNode (qName, n);
	        }
	    }
	} else {
	    // It's a primitive property. Remember the property name and type
	    // so we can properly parse/interpret the character data when we
	    // get it later on.
	    elementType = atts.getValue ("type");
	    if (elementType == null)
	        elementType = "string";
	    elementName = qName;
	    if (charBuffer == null)
	        charBuffer = new StringBuffer();
	    else
	        charBuffer.setLength (0);
	}
    }

    public void characters (char[] ch, int start, int length) throws SAXException {
	// System.err.println ("CHARACTERS: "+new String (ch, start, length));
	// append chars to char buffer
	if (elementType != null)
	    charBuffer.append (ch, start, length);
    }

    public void endElement(String namespaceURI, String localName, String qName)
                        throws SAXException {
	if (elementType != null) {
	    String charValue = charBuffer.toString ();
	    charBuffer.setLength (0);
	    if ( "boolean".equals (elementType) ) {
	        if ( "true".equals(charValue) ) {
	            currentNode.setBoolean(elementName, true);
	        } else {
	            currentNode.setBoolean(elementName, false);
	        }
	    } else if ( "date".equals(elementType) ) {
	        SimpleDateFormat format = new SimpleDateFormat ( DATEFORMAT );
	        try {
	            Date date = format.parse(charValue);
	            currentNode.setDate (elementName, date);
	        } catch ( ParseException e ) {
	            currentNode.setString (elementName, charValue);
	        }
	    } else if ( "float".equals(elementType) ) {
	        currentNode.setFloat (elementName, (new Double(charValue)).doubleValue());
	    } else if ( "integer".equals(elementType) ) {
	        currentNode.setInteger (elementName, (new Long(charValue)).longValue());
	    } else {
	        currentNode.setString (elementName, charValue);
	    }
	    elementName = null;
	    elementType = null;
	    charValue = null;
	}
	if (parsingHopObject && !nodeStack.isEmpty ())
	    currentNode = (INode) nodeStack.pop ();
	else
	    parsingHopObject = true;  // the next element end tag closes a hopobject again
    }

}

