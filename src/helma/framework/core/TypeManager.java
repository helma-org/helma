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
import helma.util.*;
import helma.framework.repository.ZipRepository;
import helma.framework.repository.Resource;
import helma.framework.repository.Repository;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

/**
 * The type manager periodically checks the prototype definitions for its
 * applications and updates the evaluators if anything has changed.
 */
public final class TypeManager {
    final static String[] standardTypes = { "User", "Global", "Root", "HopObject" };
    final static String templateExtension = ".hsp";
    final static String scriptExtension = ".js";
    final static String actionExtension = ".hac";
    final static String skinExtension = ".skin";

    private Application app;
    Repository[] repositories;
    long[] modified;
    // map of prototypes
    private HashMap prototypes;
    // map of zipped script files
    private HashMap zipfiles;
    // set of Java archives
    private HashSet jarfiles;
    private long lastCheck = 0;
    private long appDirMod = 0;

    // a checksum that changes whenever something in the application files changes.
    private long checksum;

    // the hopobject prototype
    // private Prototype hopobjectProto;

    // the global prototype
    // private Prototype globalProto;

    // app specific class loader, includes jar files in the app directory
    private AppClassLoader loader;

    /**
     * Creates a new TypeManager object.
     *
     * @param app ...
     *
     * @throws MalformedURLException ...
     * @throws RuntimeException ...
     */
    public TypeManager(Application app) throws MalformedURLException {
        this.app = app;
        repositories = new Repository[app.repositories.size()];
        app.repositories.toArray(repositories);
        modified = new long[repositories.length];
        prototypes = new HashMap();
        jarfiles = new HashSet();

        URL helmajar = TypeManager.class.getResource("/");

        if (helmajar == null) {
            // Helma classes are in jar file, get helma.jar URL
            URL[] urls = ((URLClassLoader) TypeManager.class.getClassLoader()).getURLs();

            for (int i = 0; i < urls.length; i++) {
                String url = urls[i].toString().toLowerCase();
                if (url.endsWith("helma.jar")) {
                    helmajar = urls[i];
                    break;
                }
            }
        }

        if (helmajar == null) {
            throw new RuntimeException("helma.jar not found in embedding classpath");
        }

        loader = new AppClassLoader(app.getName(), new URL[] { helmajar });
    }

    /**
     * Run through application's prototype directories and create prototypes, but don't
     * compile or evaluate any scripts.
     */
    public void createPrototypes() {
        // create standard prototypes.
        for (int i = 0; i < standardTypes.length; i++) {
            createPrototype(standardTypes[i], null);
        }

        // loop through directories and create prototypes
        checkFiles();
    }

    /**
     * Run through application's prototype directories and check if anything
     * has been updated.
     * If so, update prototypes and scripts.
     */
    public synchronized void checkPrototypes() {
        if ((System.currentTimeMillis() - lastCheck) < 1000L) {
            return;
        }

        try {
            checkFiles();
        } catch (Exception ignore) {}

        lastCheck = System.currentTimeMillis();
    }

    private void checkRepository(Repository repository) {
        Repository[] list = repository.getRepositories();
        for (int i = 0; i < list.length; i++) {
            if (list[i] instanceof ZipRepository && ((ZipRepository) list[i]).isRootRepository() == true) {
                checkRepository((ZipRepository) list[i]);
            } else if (list[i] instanceof Repository && !((Repository) list[i]).isRootRepository()) {
                // its an prototype
                String name = null;
                name = ((Repository) list[i]).getShortName();
                Prototype proto = getPrototype(name);

                // if prototype doesn't exist, create it
                if ((proto == null) && isValidTypeName(name)) {
                    // create new prototype
                    createPrototype(name, (Repository) list[i]);
                } else {
                    proto.addRepository((Repository) list[i]);
                }
            }
        }

        Resource[] resources = repository.getResources();
        for (int i = 0; i < resources.length; i++) {
            // its something else
            String name = resources[i].getName();
            if (name.endsWith(".jar")) {
                if (!jarfiles.contains(name)) {
                    jarfiles.add(name);
                    loader.addURL(((Resource) resources[i]).getUrl());
                }
            }
        }

        return;
    }

    /**
     * Run through application's prototype sources and check if
     * there are any prototypes to be created.
     */
    private void checkFiles() {
        // check if any files have been created/removed since last time we
        // checked...
        for (int i = 0; i < repositories.length; i++) {
            if (repositories[i].lastModified() > modified[i]) {
                modified[i] = repositories[i].lastModified();

                checkRepository(repositories[i]);
            }
        }

        // calculate this app's checksum by adding all checksums from all prototypes
        long newChecksum = 0;

        // loop through prototypes and check if type.properties needs updates
        // it's important that we do this _after_ potentially new prototypes
        // have been created in the previous loop.
        for (Iterator i = prototypes.values().iterator(); i.hasNext();) {
            Prototype proto = (Prototype) i.next();

            // calculate this app's type checksum
            newChecksum += proto.getChecksum();

            // update prototype's type mapping
            DbMapping dbmap = proto.getDbMapping();

            if ((dbmap != null) && dbmap.needsUpdate()) {
                // call dbmap.update(). This also checks the
                // parent prototype for prototypes other than
                // global and HopObject, which is a bit awkward...
                // I mean we're the type manager, so this should
                // be part of our job, right?
                proto.props.update();
                dbmap.update();
            }
        }

        checksum = newChecksum;
    }

    private boolean isValidTypeName(String str) {
        if (str == null) {
            return false;
        }

        char[] c = str.toCharArray();

        for (int i = 0; i < c.length; i++)
            if (!Character.isJavaIdentifierPart(c[i])) {
                return false;
            }

        return true;
    }

    /**
     *  Return a checksum over all files in all prototypes in this application.
     *  The checksum can be used to find out quickly if any file has changed.
     */
    public long getChecksum() {
        return checksum;
    }

    /**
     * Return the class loader used by this application.
     *
     * @return the ClassLoader
     */
    public ClassLoader getClassLoader() {
        return loader;
    }

    /**
     * Return a collection containing the prototypes defined for this type
     * manager.
     *
     * @return a collection containing the prototypes
     */
    public Collection getPrototypes() {
        return Collections.unmodifiableCollection(prototypes.values());
    }

    /**
     *   Get a prototype defined for this application
     */
    public Prototype getPrototype(String typename) {
        if (typename == null) {
            return null;
        }
        return (Prototype) prototypes.get(typename.toLowerCase());
    }

    /**
     * Create and register a new Prototype.
     *
     * @param typename the name of the prototype
     * @param repository the first prototype source
     * @return the newly created prototype
     */
    public Prototype createPrototype(String typename, Repository repository) {
        Prototype proto = new Prototype(typename, repository, app);

        // put the prototype into our map
        prototypes.put(proto.getLowerCaseName(), proto);

        return proto;
    }

    /**
     * Update a prototype to the files in the prototype directory.
     */
    public void updatePrototype(String name) {
        // System.err.println ("UPDATE PROTO: "+app.getName()+"/"+name);
        Prototype proto = getPrototype(name);

        updatePrototype(proto);
    }

    /**
     * Update a prototype to the files in the prototype directory.
     */
    public void updatePrototype(Prototype proto) {

        if ((proto == null) || proto.isUpToDate()) {
            return;
        }

        synchronized (proto) {
            // check again because another thread may have checked the
            // prototype while we were waiting for access to the synchronized section

            HashSet updateSet = null;
            LinkedHashSet createSet = null;

            // our plan is to do as little as possible, so first check if
            // anything the prototype knows about has changed on disk
            for (Iterator i = proto.updatables.values().iterator(); i.hasNext();) {
                Updatable upd = (Updatable) i.next();

                if (upd.needsUpdate()) {
                    if (updateSet == null) {
                        updateSet = new HashSet();
                    }

                    updateSet.add(upd);
                    // System.err.println("needs update: "+upd);
                }
            }

            // next we check if files have been created or removed since last update
            // if (proto.getLastCheck() < dir.lastModified ()) {
            Resource[] resources = proto.getResources();

            for (int i = 0; i < resources.length; i++) {
                String sn = resources[i].getName();
                if (!proto.updatables.containsKey(sn)) {
                    if (sn.endsWith(templateExtension) ||
                        sn.endsWith(scriptExtension) ||
                        sn.endsWith(actionExtension) ||
                        sn.endsWith(skinExtension)) {
                        if (createSet == null) {
                            createSet = new LinkedHashSet();
                        }

                        createSet.add(resources[i]);
                        // System.err.println("new resource: "+resources[i]);
                    }
                }
            }

            // }
            // if nothing needs to be updated, mark prototype as checked and return
            if ((updateSet == null) && (createSet == null)) {
                proto.markChecked();

                return;
            }

            // first go through new files and create new items
            if (createSet != null) {
                Resource[] newResources = new Resource[createSet.size()];
                createSet.toArray(newResources);

                for (int i = 0; i < newResources.length; i++) {
                     String resourceName = newResources[i].getName();
                     if (resourceName.endsWith(templateExtension)) {
                         try {
                             TemplateResource tr = new TemplateResource(newResources[i], proto);
                             proto.addActionResource(tr);
                         } catch (Throwable x) {
                             app.logEvent("Error updating prototype: " + x);
                         }
                     } else if (resourceName.endsWith(scriptExtension)) {
                         try {
                             FunctionResource fr = new FunctionResource(newResources[i], proto);
                             proto.addFunctionResource(fr);
                         } catch (Throwable x) {
                             app.logEvent("Error updating prototype: " + x);
                         }
                     } else if (resourceName.endsWith(actionExtension)) {
                         try {
                             ActionResource ar = new ActionResource(newResources[i], proto);
                             proto.addActionResource(ar);
                         } catch (Throwable x) {
                             app.logEvent("Error updating prototype: " + x);
                         }
                     } else if (resourceName.endsWith(skinExtension)) {
                         try {
                             SkinResource sr = new SkinResource(newResources[i], proto);
                             proto.addSkinResource(sr);
                         } catch (Throwable x) {
                             app.logEvent("Error updating prototype: " + x);
                         }
                     }
                }
            }

            // next go through existing updatables
            if (updateSet != null) {
                for (Iterator i = updateSet.iterator(); i.hasNext();) {
                    Updatable upd = (Updatable) i.next();

                    try {
                        upd.update();
                    } catch (Exception x) {
                        if (upd instanceof DbMapping) {
                            app.logEvent("Error updating db mapping for type " +
                                         proto.getName() + ": " + x);
                        } else {
                            app.logEvent("Error updating " + upd + " of prototye type " +
                                         proto.getName() + ": " + x);
                        }
                    }
                }
            }

            // mark prototype as checked and updated.
            proto.markChecked();
            proto.markUpdated();

        }
         // end of synchronized (proto)
    }
    
    private String getSourceName(File file) {
        StringBuffer b = new StringBuffer(app.getName());
        b.append(":");
        b.append(file.getParentFile().getName());
        b.append("/");
        b.append(file.getName());
        return b.toString();
    }
}
