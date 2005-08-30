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

import helma.framework.IPathElement;
import helma.framework.core.Application;
import helma.framework.repository.FileRepository;
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
     * @param app
     *
     * @throws DocException ...
     */
    public DocApplication(Application app) throws DocException {
        super(app.getName(), app.getAppDir(), APPLICATION);
        this.app = app;
        readProps();
    }

    /**
     * Creates a new DocApplication object.
     *
     * @param name ...
     * @param appDir ...
     *
     * @throws DocException ...
     */
    public DocApplication(String name, String appDir) throws DocException {
        super(name, new File(appDir), APPLICATION);
        readProps();
    }

    /**
     *
     *
     * @param args ...
     */
   /* public static void main(String[] args) {
        System.out.println("this is helma.doc");
        DocApplication app;
        app = new DocApplication (args[0], args[1]);
        app.readApplication ();

        //		DocPrototype el = DocPrototype.newInstance (new File(args[0]));
        //		el.readFiles ();
        //		DocFunction func = DocFunction.newTemplate (new File(args[0]));

//        DocFunction func = DocFunction.newAction (new File(args[0]));

//        DocFunction[] func = DocFunction.newFunctions(new File(args[0]));
//        for (int i=0; i<func.length; i++) {
//            System.out.println("=============================================");
//            System.out.println("function " + func[i].name);
//            System.out.println("comment = " + func[i].comment + "<<<");
//            String[] arr = func[i].listParameters();
//            for (int j=0; j<arr.length; j++) {
//                System.out.println (arr[j]);
//            }
//            System.out.println ("\ncontent:\n" + func[i].content + "<<<");
//            System.out.println ("\n");
//        }

//		  DocSkin skin = DocSkin.newInstance (new File (args[0]));

//        System.out.println (func.getContent ());
//        System.out.println ("\n\n\ncomment = " + func.getComment ());
    } */

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
     * @param str ...
     *
     * @return ...
     */
    public boolean isExcluded(String str) {
        return (excluded.contains(str.toLowerCase()));
    }

    /**
     * reads all prototypes and files of the application
     */
    public void readApplication() {
        readProps();
        children.clear();

        Iterator it  = app.getRepositories().iterator();

        while (it.hasNext()) {
            Object next = it.next();

            if (!(next instanceof FileRepository)) {
                continue;
            }

            File dir = ((FileRepository) next).getDirectory();

            String[] arr = dir.list();

            for (int i = 0; i < arr.length; i++) {
                if (isExcluded(arr[i])) {
                    continue;
                }

                File f = new File(dir.getAbsolutePath(), arr[i]);

                if (!f.isDirectory()) {
                    continue;
                }

                try {
                    System.err.println("*** NEW PROTOTYPE DOC: " + f);
                    DocPrototype pt = DocPrototype.newInstance(f, this);

                    addChild(pt);
                    pt.readFiles();
                } catch (DocException e) {
                    debug("Couldn't read prototype " + arr[i] + ": " + e.getMessage());
                }

                System.out.println(f);
            }

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
    public IPathElement getParentElement() {
        Server s = helma.main.Server.getServer();

        return s.getChildElement(this.name);
    }
}
