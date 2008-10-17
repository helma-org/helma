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

import helma.objectmodel.db.*;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;

/**
 * 
 */
public final class XmlDatabaseReader extends DefaultHandler implements XmlConstants {
    static SAXParserFactory factory = SAXParserFactory.newInstance();
    private NodeManager nmgr = null;
    private Node currentNode;
    private String elementType = null;
    private String elementName = null;
    private StringBuffer charBuffer = null;
    Hashtable propMap = null;
    SubnodeList subnodes = null;

    /**
     * Creates a new XmlDatabaseReader object.
     *
     * @param nmgr ...
     */
    public XmlDatabaseReader(NodeManager nmgr) {
        this.nmgr = nmgr;
    }

    /**
     * read an InputSource with xml-content.
     */
    public Node read(File file)
              throws ParserConfigurationException, SAXException, IOException {
        if (nmgr == null) {
            throw new RuntimeException("can't create a new Node without a NodeManager");
        }

        SAXParser parser = factory.newSAXParser();

        currentNode = null;

        parser.parse(file, this);

        return currentNode;
    }

    /**
     *
     *
     * @param namespaceURI ...
     * @param localName ...
     * @param qName ...
     * @param atts ...
     */
    public void startElement(String namespaceURI, String localName, String qName,
                             Attributes atts) {
        // System.err.println ("XML-READ: startElement "+namespaceURI+", "+localName+", "+qName+", "+atts.getValue("id"));
        // discard the first element called xmlroot
        if ("xmlroot".equals(qName) && (currentNode == null)) {
            return;
        }

        // if currentNode is null, this must be the hopobject node
        if ("hopobject".equals(qName) && (currentNode == null)) {
            String id = atts.getValue("id");
            String name = atts.getValue("name");
            String prototype = atts.getValue("prototype");

            if ("".equals(prototype)) {
                prototype = "hopobject";
            }

            try {
                long created = Long.parseLong(atts.getValue("created"));
                long lastmodified = Long.parseLong(atts.getValue("lastModified"));

                currentNode = new Node(name, id, prototype, nmgr.safe, created,
                                       lastmodified);
            } catch (NumberFormatException e) {
                currentNode = new Node(name, id, prototype, nmgr.safe);
            }

            return;
        }

        // find out what kind of element this is by looking at
        // the number and names of attributes.
        String idref = atts.getValue("idref");

        if (idref != null) {
            // a hopobject reference.
            NodeHandle handle = makeNodeHandle(atts);

            if ("hop:child".equals(qName)) {
                if (subnodes == null) {
                    subnodes = currentNode.createSubnodeList();
                }

                subnodes.addSorted(handle);
            } else if ("hop:parent".equals(qName)) {
                currentNode.setParentHandle(handle);
            } else {
                // property name may be encoded as "propertyname" attribute,
                // otherwise it is the element name
                String propName = atts.getValue("propertyname");

                if (propName == null) {
                    propName = qName;
                }

                Property prop = new Property(propName, currentNode);

                prop.setNodeHandle(handle);

                if (propMap == null) {
                    propMap = new Hashtable();
                    currentNode.setPropMap(propMap);
                }

                propMap.put(propName, prop);
            }
        } else {
            // a primitive property
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
            Property prop = new Property(elementName, currentNode);
            String charValue = charBuffer.toString();

            charBuffer.setLength(0);

            if ("boolean".equals(elementType)) {
                if ("true".equals(charValue)) {
                    prop.setBooleanValue(true);
                } else {
                    prop.setBooleanValue(false);
                }
            } else if ("date".equals(elementType)) {
                SimpleDateFormat format = new SimpleDateFormat(DATEFORMAT);

                try {
                    Date date = format.parse(charValue);

                    prop.setDateValue(date);
                } catch (ParseException e) {
                    prop.setStringValue(charValue);
                }
            } else if ("float".equals(elementType)) {
                prop.setFloatValue((new Double(charValue)).doubleValue());
            } else if ("integer".equals(elementType)) {
                prop.setIntegerValue((new Long(charValue)).longValue());
            } else {
                prop.setStringValue(charValue);
            }

            if (propMap == null) {
                propMap = new Hashtable();
                currentNode.setPropMap(propMap);
            }

            propMap.put(elementName, prop);
            elementName = null;
            elementType = null;
            charValue = null;
        }
    }

    // create a node handle from a node reference DOM element
    private NodeHandle makeNodeHandle(Attributes atts) {
        String idref = atts.getValue("idref");
        String protoref = atts.getValue("prototyperef");
        DbMapping dbmap = null;

        if (protoref != null) {
            dbmap = nmgr.getDbMapping(protoref);
        }

        return new NodeHandle(new DbKey(dbmap, idref));
    }
}
