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
 * 
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
     *
     *
     * @param hopObject ...
     * @param file ...
     *
     * @return ...
     *
     * @throws IOException ...
     * @throws RuntimeException ...
     */
    public boolean write(Object hopObject, String file)
                  throws IOException {
        INode node = null;

        if (hopObject instanceof HopObject) {
            node = ((HopObject) hopObject).getNode();
        }

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

        boolean result = writer.write(node);

        writer.close();

        File finalFile = new File(file);

        tmpFile.renameTo(finalFile);
        core.getApplication().logEvent("wrote xml to " + finalFile.getAbsolutePath());

        return true;
    }

    /**
     * Xml.create() is used to get a string containing the xml-content.
     * Useful if Xml-content should be made public through the web.
     */
    public String writeToString(Object hopObject) throws IOException {
        INode node = null;

        if (hopObject instanceof HopObject) {
            node = ((HopObject) hopObject).getNode();
        }

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

        boolean result = writer.write(node);

        writer.flush();

        return out.toString("UTF-8");
    }

    /**
     *
     *
     * @param file ...
     *
     * @return ...
     *
     * @throws RuntimeException ...
     */
    public Object read(String file) throws RuntimeException {
        return read(file, null);
    }

    /**
     *
     *
     * @param file ...
     * @param hopObject ...
     *
     * @return ...
     *
     * @throws RuntimeException ...
     */
    public Object read(String file, Object hopObject) throws RuntimeException {
        if (file == null) {
            throw new RuntimeException("Missing arguments in Xml.read()");
        }

        INode node = null;

        if (hopObject instanceof HopObject) {
            node = ((HopObject) hopObject).getNode();
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
     *
     *
     * @param str ...
     *
     * @return ...
     *
     * @throws RuntimeException ...
     */
    public Object readFromString(String str) throws RuntimeException {
        return readFromString(str, null);
    }

    /**
     *
     *
     * @param str ...
     * @param hopObject ...
     *
     * @return ...
     *
     * @throws RuntimeException ...
     */
    public Object readFromString(String str, Object hopObject)
                          throws RuntimeException {
        if (str == null) {
            throw new RuntimeException("Missing arguments in Xml.read()");
        }

        INode node = null;

        if (hopObject instanceof HopObject) {
            node = ((HopObject) hopObject).getNode();
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
            throw new RuntimeException(f.toString());
        }
    }

}
