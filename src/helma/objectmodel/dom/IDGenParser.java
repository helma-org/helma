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

import helma.objectmodel.ObjectNotFoundException;
import helma.objectmodel.db.IDGenerator;
import org.w3c.dom.*;
import java.io.*;
import java.util.Date;

/**
 * 
 */
public class IDGenParser {
    /**
     *
     *
     * @param file ...
     *
     * @return ...
     *
     * @throws ObjectNotFoundException ...
     */
    public static IDGenerator getIDGenerator(File file)
                                      throws ObjectNotFoundException {
        if (!file.exists()) {
            throw new ObjectNotFoundException("IDGenerator not found in idgen.xml");
        }

        try {
            Document document = XmlUtil.parse(new FileInputStream(file));
            org.w3c.dom.Element tmp = (Element) document.getDocumentElement()
                                                        .getElementsByTagName("counter")
                                                        .item(0);

            return new IDGenerator(Long.parseLong(XmlUtil.getTextContent(tmp)));
        } catch (Exception e) {
            throw new ObjectNotFoundException(e.toString());
        }
    }

    /**
     *
     *
     * @param idgen ...
     * @param file ...
     *
     * @return ...
     *
     * @throws Exception ...
     */
    public static IDGenerator saveIDGenerator(IDGenerator idgen, File file)
                                       throws IOException {
        OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(file));

        out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        out.write("<!-- printed by helma object publisher     -->\n");
        out.write("<!-- created " + (new Date()).toString() + " -->\n");
        out.write("<xmlroot>\n");
        out.write("  <counter>" + idgen.getValue() + "</counter>\n");
        out.write("</xmlroot>\n");
        out.close();

        return idgen;
    }
}
