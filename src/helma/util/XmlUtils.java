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
import org.xml.sax.InputSource;
import org.xml.sax.Parser;
import org.xml.sax.SAXException;
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
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * 
 */
public class XmlUtils {
    private static DocumentBuilderFactory domBuilderFactory = null;
    private static SAXParserFactory saxParserFactory = null;

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
        Document doc = null;

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
     * @throws SAXException ...
     * @throws IOException ...
     * @throws ParserConfigurationException ...
     */
    public static Document parseHtml(Object obj)
                              throws SAXException, IOException, 
                                     ParserConfigurationException {
        try {
            Class.forName("org.apache.html.dom.HTMLBuilder");
        } catch (Throwable notfound) {
            throw new IOException("Couldn't load nekohtml/Xerces HTML parser: " +
                                  notfound);
        }

        Document doc = null;
        HtmlParser parser = new HtmlParser();

        if (obj instanceof String) {
            try {
                // first try to interpret string as URL
                URL url = new URL(obj.toString());

                parser.parse(new InputStreamReader(url.openStream()));
            } catch (MalformedURLException nourl) {
                // if not a URL, maybe it is the XML itself
                parser.parse(new StringReader(obj.toString()));
            }
        } else if (obj instanceof InputStream) {
            parser.parse(new InputStreamReader((InputStream) obj));
        } else if (obj instanceof Reader) {
            parser.parse((Reader) obj);
        }

        doc = parser.getDocument();

        return doc;
    }
}
