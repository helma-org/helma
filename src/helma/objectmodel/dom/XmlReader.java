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

package helma.objectmodel.dom;

import helma.objectmodel.INode;
import helma.objectmodel.db.WrappedNodeManager;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Stack;

/**
 * 
 */
public final class XmlReader extends DefaultHandler implements XmlConstants {
    static SAXParserFactory factory = SAXParserFactory.newInstance();
    private INode rootNode;
    private INode currentNode;
    private Stack nodeStack;
    private HashMap convertedNodes;
    private String elementType = null;
    private String elementName = null;
    private StringBuffer charBuffer = null;
    boolean parsingHopObject;
    WrappedNodeManager nmgr;

    /**
     * Creates a new XmlReader object.
     */
    public XmlReader(WrappedNodeManager nmgr) {
        this.nmgr = nmgr;
    }

    /**
     * main entry to read an xml-file.
     */
    public INode read(File file, INode helmaNode)
               throws ParserConfigurationException, SAXException, IOException {
        try {
            return read(new FileInputStream(file), helmaNode);
        } catch (FileNotFoundException notfound) {
            System.err.println("couldn't find xml-file: " + file.getAbsolutePath());

            return helmaNode;
        }
    }

    /**
     * read an InputStream with xml-content.
     */
    public INode read(InputStream in, INode helmaNode)
               throws ParserConfigurationException, SAXException, IOException {
        return read(new InputSource(in), helmaNode);
    }

    /**
     * read an character reader with xml-content.
     */
    public INode read(Reader in, INode helmaNode)
               throws ParserConfigurationException, SAXException, IOException {
        return read(new InputSource(in), helmaNode);
    }

    /**
     * read an InputSource with xml-content.
     */
    public INode read(InputSource in, INode helmaNode)
               throws ParserConfigurationException, SAXException, IOException {
        if (helmaNode == null) {
            throw new RuntimeException("Can't create a new Node without a root Node");
        }

        SAXParser parser = factory.newSAXParser();

        rootNode = helmaNode;
        currentNode = null;
        convertedNodes = new HashMap();
        nodeStack = new Stack();
        parsingHopObject = true;

        parser.parse(in, this);

        return rootNode;
    }

    /**
     *
     *
     * @param namespaceURI ...
     * @param localName ...
     * @param qName ...
     * @param atts ...
     *
     * @throws SAXException ...
     */
    public void startElement(String namespaceURI, String localName, String qName,
                             Attributes atts) throws SAXException {
        // System.err.println ("XML-READ: startElement "+namespaceURI+", "+localName+", "+qName+", "+atts.getValue("id"));
        // discard the first element called xmlroot
        if ("xmlroot".equals(qName) && (currentNode == null)) {
            return;
        }

        // if currentNode is null, this must be the hopobject node
        String id = atts.getValue("id");

        if (id != null) {
            // check if there is a current node.
            if (currentNode == null) {
                // If currentNode is null, this is the root node we're parsing.
                currentNode = rootNode;
            } else if ("hop:child".equals(qName)) {
                // it's an anonymous child node
                nodeStack.push(currentNode);
                currentNode = currentNode.createNode(null);
            } else {
                // it's a named node property
                nodeStack.push(currentNode);

                // property name may be encoded as "propertyname" attribute,
                // otherwise it is the element name
                String propName = atts.getValue("propertyname");

                if (propName == null) {
                    propName = qName;
                }

                currentNode = currentNode.createNode(propName);
            }

            // set the prototype on the current node and
            // add it to the map of parsed nodes.
            String prototype = atts.getValue("prototype");

            if (!"".equals(prototype) && !"hopobject".equals(prototype)) {
                currentNode.setPrototype(prototype);
            }

            String key = id + "-" + prototype;

            convertedNodes.put(key, currentNode);

            return;
        }

        // check if we have a currentNode to set properties on,
        // otherwise throw exception.
        if (currentNode == null) {
            throw new SAXException("Invalid XML: No valid root HopObject found");
        }

        // check if we are inside a HopObject - otherwise throw an exception
        if (!parsingHopObject) {
            throw new SAXException("Invalid XML: Found nested non-HobObject elements");
        }

        // if we got so far, the element is not a hopobject. Set flag to prevent
        // the hopobject stack to be popped when the element
        // is closed.
        parsingHopObject = false;

        // Is it a reference to an already parsed node?
        String idref = atts.getValue("idref");

        if (idref != null) {
            // a reference to a node that should have been parsed
            // and lying in our cache of parsed nodes.
            String prototyperef = atts.getValue("prototyperef");
            String key = idref + "-" + prototyperef;
            INode n = (INode) convertedNodes.get(key);

            // if not a reference to a node we already read, try to
            // resolve against the NodeManager.
            if (n == null) {
                n = nmgr.getNode(idref, nmgr.getDbMapping(prototyperef));
            }

            if (n != null) {
                if ("hop:child".equals(qName)) {
                    // add an already parsed node as child to current node
                    currentNode.addNode(n);
                } else {
                    // set an already parsed node as node property to current node
                    // property name may be encoded as "propertyname" attribute,
                    // otherwise it is the element name
                    String propName = atts.getValue("propertyname");

                    if (propName == null) {
                        propName = qName;
                    }
                    
                    if ("hop:parent".equals(qName)) {
                        // FIXME: we ought to set parent here, but we're 
                        // dealing with INodes, which don't have a setParent().
                    } else {
                        currentNode.setNode(propName, n);
                    }
                }
            }
        } else {
            // It's a primitive property. Remember the property name and type
            // so we can properly parse/interpret the character data when we
            // get it later on.
            elementType = atts.getValue("type");

            if (elementType == null) {
                elementType = "string";
            }

            // property name may be encoded as "propertyname" attribute,
            // otherwise it is the element name
            elementName = atts.getValue("propertyname");

            if (elementName == null) {
                elementName = qName;
            }

            if (charBuffer == null) {
                charBuffer = new StringBuffer();
            } else {
                charBuffer.setLength(0);
            }
        }
    }

    /**
     *
     *
     * @param ch ...
     * @param start ...
     * @param length ...
     *
     * @throws SAXException ...
     */
    public void characters(char[] ch, int start, int length)
                    throws SAXException {
        // System.err.println ("CHARACTERS: "+new String (ch, start, length));
        // append chars to char buffer
        if (elementType != null) {
            charBuffer.append(ch, start, length);
        }
    }

    /**
     *
     *
     * @param namespaceURI ...
     * @param localName ...
     * @param qName ...
     *
     * @throws SAXException ...
     */
    public void endElement(String namespaceURI, String localName, String qName)
                    throws SAXException {
        if (elementType != null) {
            String charValue = charBuffer.toString();

            charBuffer.setLength(0);

            if ("boolean".equals(elementType)) {
                if ("true".equals(charValue)) {
                    currentNode.setBoolean(elementName, true);
                } else {
                    currentNode.setBoolean(elementName, false);
                }
            } else if ("date".equals(elementType)) {
                SimpleDateFormat format = new SimpleDateFormat(DATEFORMAT);

                try {
                    Date date = format.parse(charValue);

                    currentNode.setDate(elementName, date);
                } catch (ParseException e) {
                    currentNode.setString(elementName, charValue);
                }
            } else if ("float".equals(elementType)) {
                currentNode.setFloat(elementName, (new Double(charValue)).doubleValue());
            } else if ("integer".equals(elementType)) {
                currentNode.setInteger(elementName, (new Long(charValue)).longValue());
            } else {
                currentNode.setString(elementName, charValue);
            }

            elementName = null;
            elementType = null;
            charValue = null;
        }

        if (parsingHopObject && !nodeStack.isEmpty()) {
            currentNode = (INode) nodeStack.pop();
        } else {
            parsingHopObject = true; // the next element end tag closes a hopobject again
        }
    }
}
