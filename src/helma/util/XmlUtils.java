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

import org.w3c.dom.Document;
import org.w3c.dom.html.HTMLDocument;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLReaderAdapter;
import org.ccil.cowan.tagsoup.Parser;
import org.apache.html.dom.HTMLBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * 
 */
public class XmlUtils {
    private static DocumentBuilderFactory domBuilderFactory = null;

    /**
     *
     *
     * @param obj ...
     *
     * @return ...
     *
     * @throws SAXException ...
     * @throws IOException ...
     * @throws ParserConfigurationException ...
     */
    public static Document parseXml(Object obj)
                             throws SAXException, IOException, 
                                    ParserConfigurationException {
        if (domBuilderFactory == null) {
            domBuilderFactory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        }

        DocumentBuilder parser = domBuilderFactory.newDocumentBuilder();
        Document doc;

        if (obj instanceof String) {
            try {
                // first try to interpret string as URL
                new URL(obj.toString());

                doc = parser.parse(obj.toString());
            } catch (MalformedURLException nourl) {
                // if not a URL, maybe it is the XML itself
                doc = parser.parse(new InputSource(new StringReader(obj.toString())));
            }
        } else if (obj instanceof InputStream) {
            doc = parser.parse(new InputSource((InputStream) obj));
        } else if (obj instanceof Reader) {
            doc = parser.parse(new InputSource((Reader) obj));
        } else {
            throw new RuntimeException("Unrecognized argument to parseXml: " + obj);
        }

        doc.normalize();
        return doc;
    }

    /**
     *
     *
     * @param obj ...
     *
     * @return ...
     *
     * @throws IOException ...
     */
    public static HTMLDocument parseHtml(Object obj)
                              throws IOException, SAXException {
        try {
            Class.forName("org.apache.html.dom.HTMLDocumentImpl");
        } catch (Throwable notfound) {
            throw new RuntimeException("Couldn't load Xerces HTML DOM classes. " +
                "Make sure you have xercesImpl.jar and xml-apis.jar in your classpath.");
        }

        if (obj instanceof String) {
            try {
                // first try to interpret string as URL
                URL url = new URL(obj.toString());
                return getHtmlDocument(new InputStreamReader(url.openStream()));
            } catch (MalformedURLException nourl) {
                // if not a URL, maybe it is the XML itself
                return getHtmlDocument(new StringReader(obj.toString()));
            }
        } else if (obj instanceof InputStream) {
            return getHtmlDocument(new InputStreamReader((InputStream) obj));
        } else if (obj instanceof Reader) {
            return getHtmlDocument((Reader) obj);
        } else {
            throw new RuntimeException("Unrecognized argument to parseHtml: " + obj);
        }
    }

    private static HTMLDocument getHtmlDocument(Reader reader)
            throws IOException, SAXException {
        XMLReaderAdapter parser = new XMLReaderAdapter(new Parser());
        HTMLBuilder builder = new HTMLBuilder();
        parser.setDocumentHandler(builder);
        parser.parse(new InputSource(reader));
        return builder.getHTMLDocument();
    }
}
