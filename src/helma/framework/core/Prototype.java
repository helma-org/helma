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
import helma.util.ResourceProperties;
import helma.util.WrappedMap;
import helma.framework.repository.Resource;
import helma.framework.repository.Repository;
import helma.framework.repository.ResourceTracker;
import helma.framework.repository.FileResource;

import java.io.*;
import java.util.*;

/**
 * The Prototype class represents Script prototypes/type defined in a Helma
 * application. This class manages a prototypes templates, functions and actions
 * as well as optional information about the mapping of this type to a
 * relational database table.
 */
public final class Prototype {
    // the app this prototype belongs to
    Application app;

    // this prototype's name in natural and lower case
    String name;
    String lowerCaseName;

    // this prototype's resources
    Resource[] resources;

    // tells us the checksum of the repositories at the time we last updated them
    long lastChecksum = -1;

    // the time at which any of the prototype's files were found updated the last time
    volatile long lastCodeUpdate = 0;

    TreeSet code;
    TreeSet skins;

    HashMap trackers;

    TreeSet repositories;

    // a map of this prototype's skins as raw strings
    // used for exposing skins to application (script) code (via app.skinfiles).
    SkinMap skinMap;

    DbMapping dbmap;

    private Prototype parent;

    ResourceProperties props;

    /**
     * Creates a new Prototype object.
     *
     * @param name the prototype's name
     * @param repository the first prototype's repository
     * @param app the application this prototype is a part of
     */
    public Prototype(String name, Repository repository, Application app) {
        // app.logEvent ("Constructing Prototype "+app.getName()+"/"+name);
        this.app = app;
        this.name = name;
        repositories = new TreeSet(app.getResourceComparator());
        if (repository != null) {
            repositories.add(repository);
        }
        lowerCaseName = name.toLowerCase();

        // Create and register type properties file
        props = new ResourceProperties(app);
        if (repository != null) {
            props.addResource(repository.getResource("type.properties"));
        }
        dbmap = new DbMapping(app, name, props);
        // we don't need to put the DbMapping into proto.updatables, because
        // dbmappings are checked separately in TypeManager.checkFiles() for
        // each request

        code = new TreeSet(app.getResourceComparator());
        skins = new TreeSet(app.getResourceComparator());

        trackers = new HashMap();

        skinMap = new SkinMap();
    }

    /**
     *  Return the application this prototype is a part of
     */
    public Application getApplication() {
        return app;
    }

    /**
     * Adds an repository to the list of repositories
     * @param repository repository to add
     */
    public void addRepository(Repository repository) {
        if (!repositories.contains(repository)) {
            repositories.add(repository);
            props.addResource(repository.getResource("type.properties"));
        }
    }

    /**
     * Check a prototype for new or updated resources. After this has been
     * called the code and skins collections of this prototype should be
     * up-to-date and the lastCodeUpdate be set if there has been any changes.
     */
    public synchronized void checkForUpdates() {
        boolean updatedResources = false;

        // check if any resource the prototype knows about has changed or gone
        for (Iterator i = trackers.values().iterator(); i.hasNext();) {
            ResourceTracker tracker = (ResourceTracker) i.next();

            try {
                if (tracker.hasChanged()) {
                    updatedResources = true;
                    // let tracker know we've seen the update
                    tracker.markClean();
                    // if resource has gone remove it
                    if (!tracker.getResource().exists()) {
                        i.remove();
                        String name = tracker.getResource().getName();
                        if (name.endsWith(TypeManager.skinExtension)) {
                            skins.remove(tracker.getResource());
                        } else {
                            code.remove(tracker.getResource());
                        }
                    }
                }
            } catch (IOException iox) {
                iox.printStackTrace();
            }
        }

        // next we check if resources have been created or removed
        Resource[] resources = getResources();

        for (int i = 0; i < resources.length; i++) {
            String name = resources[i].getName();
            if (!trackers.containsKey(name)) {
                if (name.endsWith(TypeManager.templateExtension) ||
                        name.endsWith(TypeManager.scriptExtension) ||
                        name.endsWith(TypeManager.actionExtension) ||
                        name.endsWith(TypeManager.skinExtension)) {
                    updatedResources = true;
                    if (name.endsWith(TypeManager.skinExtension)) {
                        skins.add(resources[i]);
                    } else {
                        code.add(resources[i]);
                    }
                    trackers.put(resources[i].getName(), new ResourceTracker(resources[i]));
                }
            }
        }

        if (updatedResources) {
            // mark prototype as dirty and the code as updated
            lastCodeUpdate = System.currentTimeMillis();
            app.typemgr.setLastCodeUpdate(lastCodeUpdate);
        }
    }


    /**
     *  Returns the list of resources in this prototype's repositories. Used
     *  by checkForUpdates() to see whether there is anything new.
     */
    public Resource[] getResources() {
        long checksum = getRepositoryChecksum();
        // reload resources if the repositories checksum has changed
        if (checksum != lastChecksum) {
            ArrayList list = new ArrayList();
            Iterator iterator = repositories.iterator();

            while (iterator.hasNext()) {
                try {
                    list.addAll(((Repository) iterator.next()).getAllResources());
                } catch (IOException iox) {
                    iox.printStackTrace();
                }
            }

            resources = (Resource[]) list.toArray(new Resource[list.size()]);
            lastChecksum = checksum;
        }
        return resources;
    }

    /**
     *  Get a checksum over this prototype's repositories. This tells us
     *  if any resources were added or removed.
     */
    long getRepositoryChecksum() {
        long checksum = 0;
        Iterator iterator = repositories.iterator();

        while (iterator.hasNext()) {
            try {
                checksum += ((Repository) iterator.next()).getChecksum();
            } catch (IOException iox) {
                iox.printStackTrace();
            }
        }

        return checksum;
    }

    /**
     *  Set the parent prototype of this prototype, i.e. the prototype this
     *  prototype inherits from.
     */
    public void setParentPrototype(Prototype parent) {
        // this is not allowed for the hopobject and global prototypes
        if ("hopobject".equals(lowerCaseName) || "global".equals(lowerCaseName)) {
            return;
        }

        this.parent = parent;
    }

    /**
     *  Get the parent prototype from which we inherit, or null
     *  if we are top of the line.
     */
    public Prototype getParentPrototype() {
        return parent;
    }

    /**
     * Check if the given prototype is within this prototype's parent chain.
     */
    public final boolean isInstanceOf(String pname) {
        if (name.equalsIgnoreCase(pname)) {
            return true;
        }

        if (parent != null) {
            return parent.isInstanceOf(pname);
        }

        return false;
    }

    /**
     * Register an object as handler for all our parent prototypes, but only if
     * a handler by that prototype name isn't registered yet. This is used to
     * implement direct over indirect prototype precedence and child over parent
     *  precedence.
     */
    public final void registerParents(Map handlers, Object obj) {

        Prototype p = parent;

        while ((p != null) && !"hopobject".equals(p.getLowerCaseName())) {
            Object old = handlers.put(p.name, obj);
            // if an object was already registered by this name, put it back in again.
            if (old != null) {
                handlers.put(p.name, old);
            }
            // same with lower case name
            old = handlers.put(p.lowerCaseName, obj);
            if (old != null) {
                handlers.put(p.lowerCaseName, old);
            }

            p = p.parent;
        }
    }

    /**
     * Get the DbMapping associated with this prototype
     */
    public DbMapping getDbMapping() {
        return dbmap;
    }

    /**
     *  Get a skin for this prototype. This only works for skins
     *  residing in the prototype directory, not for skins files in
     *  other locations or database stored skins.
     */
    public Skin getSkin(String skinName) throws IOException {
        return skinMap.getSkin(skinName);
    }

    /**
     * Return this prototype's name
     *
     * @return ...
     */
    public String getName() {
        return name;
    }

    /**
     * Return this prototype's name in lower case letters
     *
     * @return ...
     */
    public String getLowerCaseName() {
        return lowerCaseName;
    }

    /**
     *  Get the last time any script has been re-read for this prototype.
     */
    public long lastCodeUpdate() {
        return lastCodeUpdate;
    }

    /**
     *  Signal that some script in this prototype has been
     *  re-read from disk and needs to be re-compiled by
     *  the evaluators.
     */
    public void markUpdated() {
        lastCodeUpdate = System.currentTimeMillis();
    }

    /**
     * Get the prototype's aggregated type.properties
     *
     * @return type.properties
     */
    public ResourceProperties getTypeProperties() {
        return props;
    }

    /**
     *  Return an iterator over this prototype's code resoruces. Synchronized
     *  to not return a collection in a transient state where it is just being
     *  updated by the type manager.
     */
    public synchronized Iterator getCodeResources() {
        return code.iterator();
    }

    /**
     *  Return an iterator over this prototype's skin resoruces. Synchronized
     *  to not return a collection in a transient state where it is just being
     *  updated by the type manager.
     */
    public Iterator getSkinResources() {
        return skins.iterator();
    }

    /**
     *  Return a string representing this prototype.
     */
    public String toString() {
        return "[Prototype " + app.getName() + "/" + name + "]";
    }

    /**
     * Get a map containing this prototype's skins as strings
     *
     * @return
     */
    public Map getScriptableSkinMap() {
        return new ScriptableSkinMap(new SkinMap());
    }

    /**
     * Get a map containing this prototype's skins as strings, overloaded by the
     * skins found in the given skinpath.
     *
     * @return
     */
    public Map getScriptableSkinMap(Object[] skinpath) {
        return new ScriptableSkinMap(new SkinMap(skinpath));
    }

    /**
     * A map of this prototype's skins that acts as a native JavaScript object in
     * rhino and returns the skins as strings. This is used to expose the skins
     * to JavaScript in app.skinfiles[prototypeName][skinName].
     */
    class ScriptableSkinMap extends WrappedMap {

        public ScriptableSkinMap(Map wrapped) {
            super(wrapped);
        }

        public Object get(Object key) {
            Resource res = (Resource) super.get(key);

            if (res == null || !res.exists()) {
                return null;
            }

            try {
                return res.getContent();
            } catch (IOException iox) {
                return null;
            }
        }
    }

    /**
     * A Map that dynamically expands to all skins in this prototype.
     */
    class SkinMap extends HashMap {
        volatile long lastSkinmapLoad = -1;
        Object[] skinpath;

        SkinMap() {
            skinpath = null;
        }

        SkinMap(Object[] path) {
            skinpath = path;
        }

        public boolean containsKey(Object key) {
            checkForUpdates();
            return super.containsKey(key);
        }

        public boolean containsValue(Object value) {
            checkForUpdates();
            return super.containsValue(value);
        }

        public Set entrySet() {
            checkForUpdates();
            return super.entrySet();
        }

        public boolean equals(Object obj) {
            checkForUpdates();
            return super.equals(obj);
        }

        public Skin getSkin(Object key) throws IOException {
            Resource res = (Resource) get(key);

            if (res != null) {
                return Skin.getSkin(res, app);
            } else {
                return null;
            }
        }

        public Object get(Object key) {
            checkForUpdates();
            return super.get(key);
        }

        public int hashCode() {
            checkForUpdates();
            return super.hashCode();
        }

        public boolean isEmpty() {
            checkForUpdates();
            return super.isEmpty();
        }

        public Set keySet() {
            checkForUpdates();
            return super.keySet();
        }

        public Object put(Object key, Object value) {
            // checkForUpdates ();
            return super.put(key, value);
        }

        public void putAll(Map t) {
            // checkForUpdates ();
            super.putAll(t);
        }

        public Object remove(Object key) {
            checkForUpdates();
            return super.remove(key);
        }

        public int size() {
            checkForUpdates();
            return super.size();
        }

        public Collection values() {
            checkForUpdates();
            return super.values();
        }

        private void checkForUpdates() {
            if (lastCodeUpdate > lastSkinmapLoad) {
                if (lastCodeUpdate == 0) {
                    // if prototype resources haven't been checked yet, check them now
                    Prototype.this.checkForUpdates();
                }
                loadSkins();
            }
        }

        private synchronized void loadSkins() {
            if (lastCodeUpdate == lastSkinmapLoad) {
                return;
            }

            super.clear();

            // load Skins
            for (Iterator i = skins.iterator(); i.hasNext();) {
                Resource res = (Resource) i.next();
                super.put(res.getBaseName(), res);
            }

            // if skinpath is not null, overload/add skins from there
            if (skinpath != null) {
                for (int i = skinpath.length - 1; i >= 0; i--) {
                    if ((skinpath[i] != null) && skinpath[i] instanceof String) {
                        loadSkinFiles((String) skinpath[i]);
                    }
                }
            }

            lastSkinmapLoad = lastCodeUpdate;
        }

        private void loadSkinFiles(String skinDir) {
            File dir = new File(skinDir.toString(), Prototype.this.getName());
            // if directory does not exist use lower case property name
            if (!dir.isDirectory()) {
                dir = new File(skinDir.toString(), Prototype.this.getLowerCaseName());
                if (!dir.isDirectory()) {
                    return;
                }
            }

            String[] skinNames = dir.list(app.skinmgr);

            if ((skinNames == null) || (skinNames.length == 0)) {
                return;
            }

            for (int i = 0; i < skinNames.length; i++) {
                String name = skinNames[i].substring(0, skinNames[i].length() - 5);
                File file = new File(dir, skinNames[i]);

                super.put(name, (new FileResource(file)));
            }

        }

        public String toString() {
            return "[SkinMap " + name + "]";
        }
    }
}