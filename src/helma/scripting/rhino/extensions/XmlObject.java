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

package helma.scripting.rhino.extensions;

import helma.scripting.rhino.*;
import helma.framework.core.Application;
import helma.framework.core.RequestEvaluator;
import helma.objectmodel.INode;
import helma.objectmodel.db.Node;
import helma.objectmodel.dom.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;

/**
 *  This class provides methods for converting HopObjects to XML and back.
 *
 *  @see http://helma.org/development/rfc/xmlconversion/
 */
public class XmlObject {
    RhinoCore core;

    /**
     * Creates a new XmlObject object.
     *
     * @param core ...
     */
    public XmlObject(RhinoCore core) {
        this.core = core;
    }

    /**
     *  Writes a HopObject to an XML file
     *
     * @param node the HopObject to encode
     * @param file the file to write to
     *
     * @throws IOException if something went wrong along the way
     */
    public boolean write(INode node, String file)
                  throws IOException {
        // we definitly need a node
        if (node == null) {
            throw new RuntimeException("First argument in Xml.write() is not an hopobject");
        }

        if (file == null) {
            throw new RuntimeException("Second argument file name must not be null");
        }

        File tmpFile = new File(file + ".tmp." + XmlWriter.generateID());
        XmlWriter writer = new XmlWriter(tmpFile, "UTF-8");

        writer.setDatabaseMode(false);

        writer.write(node);

        writer.close();

        File finalFile = new File(file);

        tmpFile.renameTo(finalFile);
        core.getApplication().logEvent("wrote xml to " + finalFile.getAbsolutePath());

        return true;
    }

    /**
     *  Transforms a HopObject to XML and returns the result as string
     *
     * @param node the HopObject to encode
     *
     * @return the XML representing the HopObject
     *
     * @throws IOException if something went wrong
     */
    public String writeToString(INode node) throws IOException {
        // we definitly need a node
        if (node == null) {
            throw new RuntimeException("First argument in Xml.write() is not an hopobject");
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        XmlWriter writer = new XmlWriter(out, "UTF-8");

        //  in case we ever want to limit serialization depth...
        // if (arguments.length > 1 && arguments[1] instanceof ESNumber)
        //     writer.setMaxLevels(arguments[1].toInt32());
        writer.setDatabaseMode(false);

        writer.write(node);

        writer.flush();

        return out.toString("UTF-8");
    }

    /**
     *  Reads an XML document from a file and creates a HopObject out of it
     *
     * @param file the file name
     *
     * @return the HopObject
     *
     * @throws RuntimeException ...
     */
    public Object read(String file) throws RuntimeException {
        return read(file, null);
    }

    /**
     * Reads an XML document from a file and reads it into the HopObject argument
     *
     * @param file the file name
     * @param node the HopObject to use for conversion
     *
     * @return the HopObject
     */
    public Object read(String file, INode node) throws RuntimeException {
        if (file == null) {
            throw new RuntimeException("Missing arguments in Xml.read()");
        }

        if (node == null) {
            // make sure we have a node, even if 2nd arg doesn't exist or is not a node
            node = new Node((String) null, (String) null,
                            core.getApplication().getWrappedNodeManager());
        }

        try {
            XmlReader reader = new XmlReader();
            INode result = reader.read(new File(file), node);

            return core.getNodeWrapper(result);
        } catch (NoClassDefFoundError e) {
            throw new RuntimeException("Can't load XML parser:" + e);
        } catch (Exception f) {
            throw new RuntimeException(f.toString());
        }
    }

    /**
     * Reads an XML document from an XML literal and creates a HopObject out of it
     *
     * @param str the XML string
     *
     * @return the HopObject
     *
     * @throws RuntimeException ...
     */
    public Object readFromString(String str) throws RuntimeException {
        return readFromString(str, null);
    }

    /**
     * Reads an XML document from an XML literal and creates a HopObject out of it
     *
     * @param str the XML string
     * @param node the HopObject to use for conversion
     *
     * @return ...
     *
     * @throws RuntimeException ...
     */
    public Object readFromString(String str, INode node)
                          throws RuntimeException {
        if (str == null) {
            throw new RuntimeException("Missing arguments in Xml.read()");
        }

        if (node == null) {
            // make sure we have a node, even if 2nd arg doesn't exist or is not a node
            node = new Node((String) null, (String) null,
                            core.getApplication().getWrappedNodeManager());
        }

        try {
            XmlReader reader = new XmlReader();
            INode result = reader.read(new StringReader(str), node);

            return core.getNodeWrapper(result);
        } catch (NoClassDefFoundError e) {
            throw new RuntimeException("Can't load XML parser:" + e);
        } catch (Exception f) {
            f.printStackTrace();
            throw new RuntimeException(f.toString());
        }
    }


    /**
     * Retrieves an XML document from a given URL and transforms it into a HopObject
     *
     * @param url the URL containing the XML to be parsed
     *
     * @return a HopObject obtained from parsing the XML
     */
    public Object get(String url) {
        return get(url, null);
    }


    /**
     * Retrieves an XML document from a given URL and transforms it into a HopObject
     *
     * @param url the URL containing the XML to be parsed
     * @param conversionRules a file name pointing to the conversion rules
     *
     * @return a HopObject obtained from parsing the XML
     *
     * @see http://helma.org/development/rfc/xmlconversion/
     */
    public Object get(String url, String conversionRules) {
        if (url == null) {
            throw new RuntimeException("Xml.get() needs a location as an argument");
        }

        try {
            XmlConverter converter;

            if (conversionRules != null) {
                converter = new XmlConverter(conversionRules);
            } else {
                converter = new XmlConverter();
            }

            INode node = new helma.objectmodel.db.Node((String) null,
                                                       (String) null,
                                                       core.getApplication()
                                                           .getWrappedNodeManager());
            INode result = converter.convert(url, node);

            return core.getNodeWrapper(result);
        } catch (NoClassDefFoundError e) {
            throw new RuntimeException("Can't load dom-capable xml parser.");
        }
    }

    /**
     * Transforms a XML literal into a HopObject
     *
     * @param str an XML literal
     *
     * @return a HopObject obtained from parsing the XML
     */
    public Object getFromString(String str) {
        return getFromString(str, null);
    }


    /**
     * Transforms a XML literal into a HopObject according to the rules specified in
     * the file defined by conversionRules
     *
     * @param str an XML literal
     * @param conversionRules a file name pointing to the conversion rules
     *
     * @return a HopObject obtained from parsing the XML
     *
     * @see http://helma.org/development/rfc/xmlconversion/
     */
    public Object getFromString(String str, String conversionRules) {
        if (str == null) {
            throw new RuntimeException("Xml.getFromString() needs an XML string as parameter");
        }

        try {
            XmlConverter converter;

            if (conversionRules != null) {
                converter = new XmlConverter(conversionRules);
            } else {
                converter = new XmlConverter();
            }

            INode node = new Node((String) null, (String) null,
                              core.getApplication().getWrappedNodeManager());
            INode result = converter.convertFromString(str, node);

            return core.getNodeWrapper(result);
        } catch (NoClassDefFoundError e) {
            throw new RuntimeException("Can't load dom-capable xml parser.");
        }
    }


}
