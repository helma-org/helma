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
import helma.objectmodel.TransientNode;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import java.io.IOException;
import java.io.InputStream;
import java.util.WeakHashMap;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * 
 */
public class XmlUtil {
    private static final DocumentBuilderFactory domBuilderFactory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
    private static final WeakHashMap domBuilders = new WeakHashMap();

    private static synchronized DocumentBuilder getDocumentBuilder() {
        DocumentBuilder domBuilder = (DocumentBuilder) domBuilders.get(Thread.currentThread());

        if (domBuilder != null) {
            return domBuilder;
        } else {
            try {
                domBuilder = domBuilderFactory.newDocumentBuilder();
                domBuilders.put(Thread.currentThread(), domBuilder);

                return domBuilder;
            } catch (ParserConfigurationException e) {
                throw new RuntimeException("Cannot build parser: " + e.toString());
            }
        }
    }

    /**
     *
     *
     * @return ...
     */
    public static Document newDocument() {
        DocumentBuilder d = getDocumentBuilder();

        return d.newDocument();
    }

    /**
     *
     *
     * @param in ...
     *
     * @return ...
     *
     * @throws RuntimeException ...
     */
    public static Document parse(InputStream in) throws RuntimeException {
        DocumentBuilder d = getDocumentBuilder();

        try {
            Document doc = d.parse(in);

            doc.normalize();

            return doc;
        } catch (SAXException e) {
            throw new RuntimeException("Bad xml-code: " + e.toString());
        } catch (IOException f) {
            throw new RuntimeException("Could not read Xml: " + f.toString());
        }
    }

    /**
     *
     *
     * @param in ...
     *
     * @return ...
     *
     * @throws RuntimeException ...
     */
    public static Document parse(InputSource in) throws RuntimeException {
        DocumentBuilder d = getDocumentBuilder();

        try {
            Document doc = d.parse(in);

            doc.normalize();

            return doc;
        } catch (SAXException e) {
            throw new RuntimeException("Bad xml-code: " + e.toString());
        } catch (IOException f) {
            throw new RuntimeException("Could not read Xml: " + f.toString());
        }
    }

    /**
     * get first "real" element (ie not the document-rootelement, but the next one
     */
    public static Element getFirstElement(Document document) {
        Element workelement = null;

        if (document.getDocumentElement() != null) {
            org.w3c.dom.Node tmp = document.getDocumentElement().getFirstChild();

            while (tmp != null) {
                tmp = tmp.getNextSibling();

                if (tmp.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                    workelement = (Element) tmp;

                    break;
                }
            }
        }

        return workelement;
    }

    /**
     * return the text content of an element
     */
    public static String getTextContent(org.w3c.dom.Node element) {
        StringBuffer childtext = new StringBuffer();
        NodeList childlist = element.getChildNodes();
        int ct = childlist.getLength();

        for (int j = 0; j < ct; j++) {
            org.w3c.dom.Node childNode = childlist.item(j);

            if ((childNode.getNodeType() == Node.TEXT_NODE) ||
                    (childNode.getNodeType() == Node.CDATA_SECTION_NODE)) {
                childtext.append(childNode.getNodeValue().trim());
            }
        }

        return childtext.toString();
    }
}
