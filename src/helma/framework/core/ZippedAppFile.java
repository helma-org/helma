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
    // Set of updatables provided by this zip file
    Set updatables;
    // Set of prototypes this zip files provides updatables for
    Set prototypes;

    /**
     * Creates a new ZippedAppFile object.
     *
     * @param file ...
     * @param app ...
     */
    public ZippedAppFile(File file, Application app) {
        this.app = app;
        this.file = file;
        updatables = new HashSet();
        prototypes = new HashSet();
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
    public synchronized void update() {
        if (!file.exists()) {
            remove();
        } else {
            ZipFile zip = null;
            Set newUpdatables = new HashSet();
            Set newPrototypes = new HashSet();

            try {
                lastmod = file.lastModified();

                zip = new ZipFile(file);

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
                            proto = app.typemgr.createPrototype(dir, null);
                        }

                        if (fname.endsWith(".hac")) {
                            String name = fname.substring(0, fname.lastIndexOf("."));
                            String srcName = getSourceName(ename);
                            String content = getZipEntryContent(zip, entry);

                            // System.err.println ("["+content+"]");
                            ActionFile act = new ActionFile(content, name, srcName,
                                                            proto);

                            proto.addActionFile(act);
                            newUpdatables.add(act);

                        } else if (fname.endsWith(".hsp")) {
                            String name = fname.substring(0, fname.lastIndexOf("."));
                            String srcName = getSourceName(ename);
                            String content = getZipEntryContent(zip, entry);

                            // System.err.println ("["+content+"]");
                            Template tmp = new Template(content, name, srcName, proto);

                            proto.addTemplate(tmp);
                            newUpdatables.add(tmp);

                        } else if (fname.endsWith(".skin")) {
                            String name = fname.substring(0, fname.lastIndexOf("."));
                            String content = getZipEntryContent(zip, entry);

                            // System.err.println ("["+content+"]");
                            SkinFile skin = new SkinFile(content, name, proto);

                            proto.addSkinFile(skin);
                            newUpdatables.add(skin);

                        } else if (fname.endsWith(".js")) {
                            String srcName = getSourceName(ename);
                            String content = getZipEntryContent(zip, entry);

                            // System.err.println ("["+content+"]");
                            FunctionFile ff = new FunctionFile(content, srcName, proto);

                            proto.addFunctionFile(ff);
                            newUpdatables.add(ff);

                        } else if ("type.properties".equalsIgnoreCase(fname)) {
                            DbMapping dbmap = proto.getDbMapping();
                            SystemProperties props = dbmap.getProperties();

                            props.addProps(file.getName(), zip.getInputStream(entry));
                        }

                        // mark prototype as updated
                        newPrototypes.add(proto);
                    }
                }
            } catch (Throwable x) {
                System.err.println("Error updating ZipFile: " + x);
                if (app.debug) {
                    x.printStackTrace();
                }
            } finally {
                // remove updatables that have gone
                updatables.removeAll(newUpdatables);
                for (Iterator it = updatables.iterator(); it.hasNext();) {
                    ((Updatable) it.next()).remove();
                }
                updatables = newUpdatables;
                
                // mark both old and new prototypes as updated
                prototypes.addAll(newPrototypes);
                for (Iterator it = prototypes.iterator(); it.hasNext();) {
                    ((Prototype) it.next()).markUpdated();
                }
                prototypes = newPrototypes;
                
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
        // remove updatables from prototypes
        for (Iterator it = updatables.iterator(); it.hasNext();) {
            ((Updatable) it.next()).remove();
        }
        // mark affected prototypes as updated
        for (Iterator it = prototypes.iterator(); it.hasNext();) {
            ((Prototype) it.next()).markUpdated();
        }

        // remove self from type manager
        app.typemgr.removeZipFile(file.getName());
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

        int read = 0;
        while (read < size) {
            int r = reader.read(c, read, size-read);
            if (r == -1)
                break;
            read += r;
        }

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
    
        
    private String getSourceName(String entry) {
        StringBuffer b = new StringBuffer(app.getName());
        b.append(":");
        b.append(file.getName());
        b.append("/");
        b.append(entry);
        return b.toString();
    }
}
