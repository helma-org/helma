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

package helma.doc;

import helma.framework.core.Application;
import helma.framework.core.Prototype;
import helma.main.Server;
import helma.util.ResourceProperties;
import java.io.*;
import java.util.*;

/**
 *
 */
public class DocApplication extends DocElement {

    Application app;
    HashSet excluded;

    /**
     * Creates a new DocApplication object.
     *
     */
    public DocApplication(Application app)  {
        super(app.getName(), APPLICATION);
        this.app = app;
        readProps();
    }

    /**
     * reads the app.properties file and parses for helma.excludeDocs
     */
    private void readProps() {
        ResourceProperties appProps = app.getProperties();

        excluded = new HashSet();
        addExclude("cvs");
        addExclude(".docs");

        String excludeProps = appProps.getProperty("helma.excludeDocs");

        if (excludeProps != null) {
            StringTokenizer tok = new StringTokenizer(excludeProps, ",");

            while (tok.hasMoreTokens()) {
                String str = tok.nextToken().trim();

                addExclude(str);
            }
        }
    }

    /**
     *
     *
     * @param str ...
     */
    public void addExclude(String str) {
        excluded.add(str.toLowerCase());
    }

    /**
     *
     *
     * @param name ...
     *
     * @return ...
     */
    public boolean isExcluded(String name) {
        return (excluded.contains(name.toLowerCase()));
    }

    /**
     * reads all prototypes and files of the application
     */
    public void readApplication() throws IOException {
        readProps();
        children.clear();

        Iterator it = app.getPrototypes().iterator();
        // Iterator it  = app.getRepositories().iterator();

        while (it.hasNext()) {

            Prototype proto = (Prototype) it.next();
            proto.checkForUpdates();
            DocPrototype pt = new DocPrototype(proto, this);
            addChild(pt);
            pt.readFiles();

            for (Iterator i = children.values().iterator(); i.hasNext();) {
                ((DocPrototype) i.next()).checkInheritance();
            }
        }
    }

    /**
     *
     *
     * @return ...
     */
    public DocElement[] listFunctions() {
        Vector allFunctions = new Vector();

        for (Iterator i = children.values().iterator(); i.hasNext();) {
            DocElement proto = (DocElement) i.next();

            allFunctions.addAll(proto.children.values());
        }

        Collections.sort(allFunctions, new DocComparator(DocComparator.BY_NAME, this));

        return (DocElement[]) allFunctions.toArray(new DocElement[allFunctions.size()]);
    }

    /**
     * from helma.framework.IPathElement, overridden with "api"
     * to work in manage-application
     */
    public String getElementName() {
        return "api";
    }

    /**
     * from helma.framework.IPathElement, overridden with
     * Server.getServer() to work in manage-application
     */
    public Object getParentElement() {
        Server s = helma.main.Server.getServer();

        return s.getApplication(this.name);
    }
}
