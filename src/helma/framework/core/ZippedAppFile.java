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

package helma.framework.core;

import helma.objectmodel.db.DbMapping;
import helma.scripting.*;
import helma.util.SystemProperties;
import helma.util.Updatable;
import java.io.*;
import java.util.*;
import java.util.zip.*;

/**
 * This represents a Zip-File which may contain other Updatables for one or more prototypes.
 */
public class ZippedAppFile implements Updatable {
    Application app;
    File file;
    long lastmod;
    Set updatables;

    /**
     * Creates a new ZippedAppFile object.
     *
     * @param file ...
     * @param app ...
     */
    public ZippedAppFile(File file, Application app) {
        this.app = app;
        this.file = file;

        // System.err.println ("CREATING ZIP FILE "+this);
    }

    /**
     * Tell the type manager whether we need an update. this is the case when
     * the file has been modified or deleted.
     */
    public boolean needsUpdate() {
        return !file.exists() || (lastmod != file.lastModified());
    }

    /**
     *
     */
    public void update() {
        if (!file.exists()) {
            remove();
        } else {
            ZipFile zip = null;

            // collect created protos - we need this to check DbMappings for each created
            // prototype afterwards
            HashSet newPrototypes = new HashSet();

            try {
                lastmod = file.lastModified();

                // System.err.println ("UPDATING ZIP FILE "+this);
                zip = new ZipFile(file);
                updatables = new HashSet();

                for (Enumeration en = zip.entries(); en.hasMoreElements();) {
                    ZipEntry entry = (ZipEntry) en.nextElement();
                    String ename = entry.getName();
                    StringTokenizer st = new StringTokenizer(ename, "/");

                    int tokens = st.countTokens();
                    if (tokens == 1) {
                        String fname = st.nextToken();

                        if ("app.properties".equalsIgnoreCase(fname)) {
                            app.props.addProps(file.getName(), zip.getInputStream(entry));
                        } else if ("db.properties".equalsIgnoreCase(fname)) {
                            app.dbProps.addProps(file.getName(), zip.getInputStream(entry));
                        }

                    } else if (tokens == 2) {
                        String dir = st.nextToken();
                        String fname = st.nextToken();

                        // System.err.println ("ZIPENTRY: "+ dir +" ~ "+fname);
                        Prototype proto = app.typemgr.getPrototype(dir);

                        if (proto == null) {
                            proto = app.typemgr.createPrototype(dir);
                            newPrototypes.add(proto);
                        }

                        if (fname.endsWith(".hac")) {
                            String name = fname.substring(0, fname.lastIndexOf("."));
                            String sourceName = file.getName() + "/" + ename;
                            String content = getZipEntryContent(zip, entry);

                            // System.err.println ("["+content+"]");
                            ActionFile act = new ActionFile(content, name, sourceName,
                                                            proto);

                            proto.addActionFile(act);
                            updatables.add(act);

                        } else if (fname.endsWith(".hsp")) {
                            String name = fname.substring(0, fname.lastIndexOf("."));
                            String sourceName = file.getName() + "/" + ename;
                            String content = getZipEntryContent(zip, entry);

                            // System.err.println ("["+content+"]");
                            Template tmp = new Template(content, name, sourceName, proto);

                            proto.addTemplate(tmp);
                            updatables.add(tmp);

                        } else if (fname.endsWith(".skin")) {
                            String name = fname.substring(0, fname.lastIndexOf("."));
                            String content = getZipEntryContent(zip, entry);

                            // System.err.println ("["+content+"]");
                            SkinFile skin = new SkinFile(content, name, proto);

                            proto.addSkinFile(skin);
                            updatables.add(skin);

                        } else if (fname.endsWith(".js")) {
                            String sourceName = file.getName() + "/" + ename;
                            String content = getZipEntryContent(zip, entry);

                            // System.err.println ("["+content+"]");
                            FunctionFile ff = new FunctionFile(content, sourceName, proto);

                            proto.addFunctionFile(ff);
                            updatables.add(ff);

                        } else if ("type.properties".equalsIgnoreCase(fname)) {
                            DbMapping dbmap = proto.getDbMapping();
                            SystemProperties props = dbmap.getProperties();

                            props.addProps(file.getName(), zip.getInputStream(entry));
                        }

                        // mark prototype as updated
                        proto.markUpdated();
                    }
                }
            } catch (Throwable x) {
                System.err.println("Error updating ZipFile: " + x);
            } finally {
                try {
                    zip.close();
                } catch (Exception ignore) {
                }
            }
        }
    }

    /**
     *
     */
    public void remove() {
        if (updatables != null) {
            for (Iterator it = updatables.iterator(); it.hasNext();)
                ((Updatable) it.next()).remove();
        }

        app.typemgr.removeZipFile(file.getName());

        // System.err.println ("REMOVING ZIP FILE "+this);
    }

    /**
     *
     *
     * @param zip ...
     * @param entry ...
     *
     * @return ...
     *
     * @throws IOException ...
     */
    public String getZipEntryContent(ZipFile zip, ZipEntry entry)
                              throws IOException {
        int size = (int) entry.getSize();
        char[] c = new char[size];
        InputStreamReader reader = new InputStreamReader(zip.getInputStream(entry));

        reader.read(c);

        return new String(c);
    }

    /**
     *
     *
     * @return ...
     */
    public String toString() {
        return file.getName();
    }
}
