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
import helma.util.SystemMap;
import helma.util.SourceProperties;
import helma.framework.repository.ZipResource;
import helma.framework.repository.Resource;
import helma.framework.repository.Repository;

import java.io.*;
import java.util.*;

/**
 * The Prototype class represents Script prototypes/type defined in a Helma
 * application. This class manages a prototypes templates, functions and actions
 * as well as optional information about the mapping of this type to a
 * relational database table.
 */
public final class Prototype {
    String name;
    String lowerCaseName;
    Application app;
    ArrayList resources;
    long lastResourceListing;
    long checksum;
    HashMap code;
    HashMap zippedCode;
    HashMap skins;
    HashMap zippedSkins;
    HashMap updatables;
    HashSet repositories;

    // a map of this prototype's skins as raw strings
    // used for exposing skins to application (script) code (via app.skinfiles).
    SkinMap skinMap;
    DbMapping dbmap;

    // lastCheck is the time the prototype's files were last checked
    private long lastChecksum;

    // lastUpdate is the time at which any of the prototype's files were
    // found updated the last time
    private long lastUpdate;
    private Prototype parent;

    // Tells us whether this prototype is used to script a generic Java object,
    // as opposed to a Helma objectmodel node object.
    boolean isJavaPrototype;

    SourceProperties props;

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
        repositories = new HashSet();
        if (repository != null) {
            repositories.add(repository);
        }
        lowerCaseName = name.toLowerCase();

        // Create and register type properties file
        props = new SourceProperties();
        if (repository != null) {
            props.addResource(repository.getResource("type.properties"));
        }
        dbmap = new DbMapping(app, name, props);
        // we don't need to put the DbMapping into proto.updatables, because
        // dbmappings are checked separately in TypeManager.checkFiles() for
        // each request

        code = new HashMap();
        zippedCode = new HashMap();
        skins = new HashMap();
        zippedSkins = new HashMap();
        updatables = new HashMap();

        skinMap = new SkinMap();

        isJavaPrototype = app.isJavaPrototype(name);
        lastUpdate = lastChecksum = 0;
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
        return;
    }

    /**
     *  Returns the list of resources in this prototype's repositories
     */
    public Resource[] getResources() {
        long resourceListing = getChecksum();
        if (resources == null || resourceListing != lastResourceListing) {
            lastResourceListing = resourceListing;
            resources = new ArrayList();
            Iterator iterator = repositories.iterator();

            while (iterator.hasNext()) {
                resources.addAll(Arrays.asList(((Repository) iterator.next()).getAllResources()));
            }

            if (resources == null) {
                return new Resource[0];
            }
        }

        return (Resource[]) resources.toArray(new Resource[resources.size()]);
    }

    /**
     *  Get a checksum over this prototype's sources
     */
    public long getChecksum() {
        long checksum = 0;
        Iterator iterator = repositories.iterator();

        while (iterator.hasNext()) {
            checksum += ((Repository) iterator.next()).getChecksum();
        }

        return checksum;
    }

    /**
     *
     *
     * @return ...
     */
    public boolean isUpToDate() {
        return (checksum == 0 && lastChecksum == 0) ? false : checksum == lastChecksum;
    }

    /**
     *  Set the parent prototype of this prototype, i.e. the prototype this
     *  prototype inherits from.
     */
    public void setParentPrototype(Prototype parent) {
        // this is not allowed for the hopobject and global prototypes
        if ("HopObject".equals(name) || "global".equals(name)) {
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
        if (name.equals(pname) || lowerCaseName.equals(pname)) {
            return true;
        }

        if ((parent != null) && !"HopObject".equals(parent.getName())) {
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
     *
     *
     * @param dbmap ...
     */
    protected void setDbMapping(DbMapping dbmap) {
        this.dbmap = dbmap;
    }

    /**
     *
     *
     * @return ...
     */
    public DbMapping getDbMapping() {
        return dbmap;
    }

    /**
     *  Get a Skinfile for this prototype. This only works for skins
     *  residing in the prototype directory, not for skin files in
     *  other locations or database stored skins.
     */
    public SkinResource getSkinResource(String sname) {
        SkinResource sr = (SkinResource) skins.get(sname);

        if (sr == null) {
            sr = (SkinResource) zippedSkins.get(sname);
        }

        return sr;
    }

    /**
     *  Get a skin for this prototype. This only works for skins
     *  residing in the prototype directory, not for skins files in
     *  other locations or database stored skins.
     */
    public Skin getSkin(String sname) {
        SkinResource sr = getSkinResource(sname);

        if (sr != null) {
            return sr.getSkin();
        } else {
            return null;
        }
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
    public long getLastUpdate() {
        return lastUpdate;
    }

    /**
     *  Signal that some script in this prototype has been
     *  re-read from disk and needs to be re-compiled by
     *  the evaluators.
     */
    public void markUpdated() {
        lastUpdate = System.currentTimeMillis();
    }

    /**
     *  Signal that the prototype's scripts have been checked for
     *  changes.
     */
    public void markChecked() {
        // lastCheck = System.currentTimeMillis ();
        lastChecksum = checksum;
    }

    /**
     *  Return a clone of this prototype's actions container. Synchronized
     *  to not return a map in a transient state where it is just being
     *  updated by the type manager.
     */
    public synchronized Map getCode() {
        return (Map) code.clone();
    }

    /**
     *  Return a clone of this prototype's functions container. Synchronized
     *  to not return a map in a transient state where it is just being
     *  updated by the type manager.
     */
    public synchronized Map getZippedCode() {
        return (Map) zippedCode.clone();
    }

    public synchronized void addActionResource(ActionResource action) {
        if (action.getResource() instanceof ZipResource && action.getResource().getRepository().getRootRepository().getClass().getName() != "helma.framework.repository.ZipRepository") {
            zippedCode.put(action.getResourceName(), action);
        } else {
            code.put(action.getResourceName(), action);
        }
        updatables.put(action.getName(), action);
        return;
    }

    public synchronized void addFunctionResource(FunctionResource function) {
        if (function.getResource() instanceof ZipResource && function.getResource().getRepository().getRootRepository().getClass().getName() != "helma.framework.repository.ZipRepository") {
            zippedCode.put(function.getResourceName(), function);
        } else {
            code.put(function.getResourceName(), function);
        }
        updatables.put(function.getName(), function);
        return;
    }

    public synchronized void addSkinResource(SkinResource skin) {
        String name = skin.getResource().getShortName();
        if (skin.getResource() instanceof ZipResource && skin.getResource().getRepository().getRootRepository().getClass().getName() != "helma.framework.repository.ZipRepository") {
            if (!zippedSkins.containsKey(name) || app.getResourceComparator().compare(zippedSkins.get(name), skin) == -1) {
                SkinResource previous = (SkinResource) zippedSkins.put(name, skin);
                if (previous != null && previous != skin) {
                    updatables.remove(previous.getName());
                }
                updatables.put(skin.getName(), skin);
            }
        } else {
            if (!skins.containsKey(name) || app.getResourceComparator().compare(skins.get(name), skin) == -1) {
                SkinResource previous = (SkinResource) skins.put(name, skin);
                if (previous != null && previous != skin) {
                    updatables.remove(previous.getName());
                }
                updatables.put(skin.getName(), skin);
            }
        }
        return;
    }

    public synchronized void removeActionResource(ActionResource action) {
        if (action.getResource() instanceof ZipResource && action.getResource().getRepository().getRootRepository().getClass().getName() != "helma.framework.repository.ZipRepository") {
            zippedCode.remove(action.getResourceName());
        } else {
            code.remove(action.getResourceName());
        }
        updatables.remove(action.getName());
    }

    public synchronized void removeFunctionResource(FunctionResource function) {
        if (function.getResource() instanceof ZipResource && function.getResource().getRepository().getRootRepository().getClass().getName() != "helma.framework.repository.ZipRepository") {
            zippedCode.remove(function.getResourceName());
        } else {
            code.remove(function.getResourceName());
        }
        updatables.remove(function.getName());
    }

    public synchronized void removeSkinResource(SkinResource skin) {
        if (skin.getResource() instanceof ZipResource && skin.getResource().getRepository().getRootRepository().getClass().getName() != "helma.framework.repository.ZipRepository") {
            zippedSkins.remove(skin.getResource().getShortName());
        } else {
            skins.remove(skin.getResource().getShortName());
        }
        updatables.remove(skin.getName());
    }

    /**
     *  Return a string representing this prototype.
     */
    public String toString() {
        return "[Prototype " + app.getName() + "/" + name + "]";
    }

    /**
     *
     *
     * @return ...
     */
    public SkinMap getSkinMap() {
        return skinMap;
    }

    // not yet implemented
    public SkinMap getSkinMap(Object[] skinpath) {
        return new SkinMap(skinpath);
    }

    // a map that dynamically expands to all skins in this prototype
    final class SkinMap extends SystemMap {
        long lastSkinmapLoad = 0;
        Object[] skinpath;

        SkinMap() {
            super();
            skinpath = null;
        }

        SkinMap(Object[] path) {
            super();
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

        public Object get(Object key) {
            if (key == null) {
                return null;
            }

            checkForUpdates();

            SkinResource sr = (SkinResource) super.get(key);

            if (sr == null) {
                return null;
            }

            return sr.getSkin().getSource();
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
            if (!isUpToDate()) {
                app.typemgr.updatePrototype(Prototype.this);
            }

            if (lastUpdate > lastSkinmapLoad) {
                load();
            }
        }

        private synchronized void load() {
            if (lastUpdate == lastSkinmapLoad) {
                return;
            }

            super.clear();

            // load Skins from zip files first, then from directories
            for (Iterator i = zippedSkins.entrySet().iterator(); i.hasNext();) {
                Map.Entry e = (Map.Entry) i.next();

                super.put(e.getKey(), e.getValue());
            }

            for (Iterator i = skins.entrySet().iterator(); i.hasNext();) {
                Map.Entry e = (Map.Entry) i.next();

                super.put(e.getKey(), e.getValue());
            }

            // if skinpath is not null, overload/add skins from there
            if (skinpath != null) {
                for (int i = skinpath.length - 1; i >= 0; i--) {
                    if ((skinpath[i] != null) && skinpath[i] instanceof String) {
                        Map m = app.skinmgr.getSkinFiles((String) skinpath[i],
                                                         Prototype.this);

                        if (m != null) {
                            super.putAll(m);
                        }
                    }
                }
            }

            lastSkinmapLoad = lastUpdate;
        }

        public String toString() {
            return "[SkinMap " + name + "]";
        }
    }
}
