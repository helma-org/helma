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
import helma.framework.repository.ZipRepository;
import helma.framework.repository.Resource;
import helma.framework.repository.Repository;
import helma.framework.repository.ResourceTracker;

import java.io.*;
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
    // map of prototypes
    private HashMap prototypes;

    // set of Java archives
    private HashSet jarfiles;

    private long lastCheck = 0;
    private long lastCodeUpdate;
    private long lastRepositoryScan;

    // app specific class loader, includes jar files in the app directory
    private AppClassLoader loader;

    /**
     * Creates a new TypeManager object.
     *
     * @param app ...
     *
     * @throws RuntimeException ...
     */
    public TypeManager(Application app) {
        this.app = app;
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
    public void createPrototypes() throws IOException {
        // create standard prototypes.
        for (int i = 0; i < standardTypes.length; i++) {
            createPrototype(standardTypes[i], null);
        }

        // loop through directories and create prototypes
        checkRepositories();
    }

    /**
     * Run through application's prototype directories and check if anything
     * has been updated.
     * If so, update prototypes and scripts.
     */
    public synchronized void checkPrototypes() throws IOException {
        if ((System.currentTimeMillis() - lastCheck) < 1000L) {
            return;
        }

        checkRepositories();

        lastCheck = System.currentTimeMillis();
    }

    private void checkRepository(Repository repository) throws IOException {
        Repository[] list = repository.getRepositories();
        for (int i = 0; i < list.length; i++) {
            if (list[i].isScriptRoot()) {
                // this is an embedded top-level script repository 
                if (app.addRepository(list[i])) {
                    // repository is new, check it
                    checkRepository(list[i]);
                }
            } else {
                // its an prototype
                String name = null;
                name = list[i].getShortName();
                Prototype proto = getPrototype(name);

                // if prototype doesn't exist, create it
                if (proto == null) {
                    // create new prototype if type name is valid
                    if (isValidTypeName(name)) 
                        createPrototype(name, list[i]);
                } else {
                    proto.addRepository(list[i]);
                }
            }
        }

        Iterator resources = repository.getResources();
        while (resources.hasNext()) {
            // check for jar files to add to class loader
            Resource resource = (Resource) resources.next();
            String name = resource.getName();
            if (name.endsWith(".jar")) {
                if (!jarfiles.contains(name)) {
                    jarfiles.add(name);
                    loader.addURL(resource.getUrl());
                }
            }
        }
    }

    /**
     * Run through application's prototype sources and check if
     * there are any prototypes to be created.
     */
    private void checkRepositories() throws IOException {
        // check if any files have been created/removed since last time we checked...
        Iterator it = app.getRepositories();
        while (it.hasNext()) {
            Repository repository = (Repository) it.next();
            if (repository.lastModified() > lastRepositoryScan) {
                lastRepositoryScan = Math.max(System.currentTimeMillis(),
                                              repository.lastModified());

                checkRepository(repository);
            }
        }

        // loop through prototypes and check if type.properties needs updates
        // it's important that we do this _after_ potentially new prototypes
        // have been created in the previous loop.
        for (Iterator i = prototypes.values().iterator(); i.hasNext();) {
            Prototype proto = (Prototype) i.next();

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
    public long lastCodeUpdate() {
        return lastCodeUpdate;
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
     * Check a prototype for new or updated resources.
     */
    public void updatePrototype(Prototype proto) {

        synchronized (proto) {
            // check again because another thread may have checked the
            // prototype while we were waiting for access to the synchronized section

            boolean updatedResources = false;
            List createdResources = null;

            // our plan is to do as little as possible, so first check if
            // anything the prototype knows about has changed on disk
            for (Iterator i = proto.trackers.values().iterator(); i.hasNext();) {
                ResourceTracker tracker = (ResourceTracker) i.next();

                try {
                    if (tracker.hasChanged()) {
                        updatedResources = true;
                        tracker.markClean();
                    }
                } catch (IOException iox) {
                    iox.printStackTrace();
                }
            }

            // next we check if files have been created or removed since last update
            Resource[] resources = proto.getResources();

            for (int i = 0; i < resources.length; i++) {
                String name = resources[i].getName();
                if (!proto.trackers.containsKey(name)) {
                    if (name.endsWith(templateExtension) ||
                        name.endsWith(scriptExtension) ||
                        name.endsWith(actionExtension) ||
                        name.endsWith(skinExtension)) {
                        if (createdResources == null) {
                            createdResources = new ArrayList();
                        }

                        createdResources.add(resources[i]);
                    }
                }
            }

            // if nothing needs to be updated, mark prototype as checked and return
            if (!updatedResources && createdResources == null) {
                proto.markChecked();

                return;
            }

            // first go through new files and create new items
            if (createdResources != null) {
                Resource[] newResources = new Resource[createdResources.size()];
                createdResources.toArray(newResources);

                for (int i = 0; i < newResources.length; i++) {
                     String resourceName = newResources[i].getName();
                     if (resourceName.endsWith(templateExtension) ||
                         resourceName.endsWith(scriptExtension) ||
                         resourceName.endsWith(actionExtension)) {
                         try {
                             proto.addCodeResource(newResources[i]);
                         } catch (Throwable x) {
                             app.logEvent("Error updating prototype: " + x);
                         }
                     } else if (resourceName.endsWith(skinExtension)) {
                         try {
                             proto.addSkinResource(newResources[i]);
                         } catch (Throwable x) {
                             app.logEvent("Error updating prototype: " + x);
                         }
                     }
                }
            }

            // next go through existing updatables
            if (updatedResources) {
                /*
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
                */
            }

            // mark prototype as checked and updated.
            proto.markChecked();
            proto.markUpdated();
            lastCodeUpdate = proto.lastCodeUpdate();

        } // end of synchronized (proto)
    }

}
