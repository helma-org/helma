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

package helma.util;

import java.io.IOException;
import java.util.*;
import helma.framework.core.*;
import helma.framework.repository.Resource;
import helma.framework.repository.Repository;

/**
 *  A property dictionary that is updated from property resources
 */
public final class SourceProperties extends Properties {

    // Delay between checks
    private final long cacheTime = 1500L;

    // Default properties
    public SourceProperties defaultProperties;

    // Defines wether keys are case-sensitive or not
    private boolean ignoreCase = true;

    // Cached checksum of last check
    private long lastChecksum = -1;

    // Time of last check
    private long lastCheck = 0;

    // Time porperties were last modified
    private long lastModified = 0;

    // Application where to fetch additional resources
    private Application app;

    // Name of possible resources to fetch from the applications's repositories
    private String resourceName;

    // Sorted map of resources
    private TreeMap resources;

    /**
     * Constructs an empty SourceProperties
     * Resources must be added manually afterwards
     */
    public SourceProperties() {
        resources = new TreeMap();
    }

    /**
     * Constructs a SourceProperties retrieving resources from the given
     * application using the given name to fetch resources
     * @param app application to fetch resources from
     * @param resourceName name to use when fetching resources from the application
     */
    public SourceProperties(Application app, String resourceName) {
        this.app = app;
        this.resourceName = resourceName;
        resources = new TreeMap(app.getResourceComparator());
    }

    /**
     * Constructs a SourceProperties retrieving resources from the given
     * application using the given name to fetch resources and falling back
     * to the given default properties
     * @param app application to fetch resources from
     * @param sourceName name to use when fetching resources from the application
     * @param defaultProperties default properties
     */
    public SourceProperties(Application app, String sourceName, SourceProperties defaultProperties) {
        this(app, sourceName);
        this.defaultProperties = defaultProperties;
        forceUpdate();
    }

    /**
     * Updates the properties regardless of an actual need
     */
    private void forceUpdate() {
        lastChecksum = -1;
        update();
    }

    /**
     * Sets the default properties and updates all properties
     * @param defaultproperties default properties
     */
    public void setDefaultProperties(SourceProperties defaultproperties) {
        defaultProperties = defaultProperties;
        update();
    }

    /**
     * Adds a resource to the list of resources and updates all properties if
     * needed
     * @param resource resource to add
     */
    public void addResource(Resource resource) {
        if (resource != null) {
            String name = resource.getName();
            if (!resources.containsKey(name)) {
                resources.put(name, resource);
                forceUpdate();
            }
        }

        return;
    }

    /**
     * Removes a resource from the list of resources and updates all properties
     * if needed
     * @param resource resource to remove
     */
    public void removeResource(Resource resource) {
        String name = resource.getName();
        if (resources.containsKey(name)) {
            resources.remove(name);
            forceUpdate();
        }

        return;
    }

    /**
     * Checks wether the properties need to be updated
     * @return true if the properties need tu be updated
     */
    public boolean needsUpdate() {
        lastCheck = System.currentTimeMillis();
        if (getChecksum() != lastChecksum) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Updates all properties if there is a need to update
     */
    public void update() {
        if (needsUpdate() || (defaultProperties != null && defaultProperties.needsUpdate())) {
            clear();

            // first of all, properties are load from default properties
            if (defaultProperties != null) {
                defaultProperties.update();
                this.putAll(defaultProperties);
            }

            /* next we try to load properties from the application's
             repositories, if we blong to any application */
            if (app != null) {
                Iterator iterator = app.getRepositories();
                while (iterator.hasNext()) {
                    Repository repository = (Repository) iterator.next();
                    Resource resource = repository.getResource(resourceName);
                    if (resource != null) {
                        try {
                            load(resource.getInputStream());
                        }
                        catch (IOException ignore) {ignore.printStackTrace();}
                    }
                }
            }

            // at last we try to load properties from the resource list
            if (resources != null) {
                Iterator iterator = resources.values().iterator();
                while (iterator.hasNext()) {
                    try {
                        load(((Resource) iterator.next()).getInputStream());
                    } catch (IOException ignore) {}
                }
            }

            lastChecksum = getChecksum();
            lastCheck = lastModified = System.currentTimeMillis();
        }

        return;
    }

    /**
     * Checks wether the given object is in the value list
     * @param value value to look for
     * @return true if the value is found in the value list
     */
    public boolean contains(Object value) {
        if ((System.currentTimeMillis() - lastCheck) > cacheTime) {
            update();
        }

        return super.contains(value.toString());
    }

    /**
     * Checks wether the given object is in the key list
     * @param key key to look for
     * @return true if the key is found in the key list
     */
    public boolean containsKey(Object key) {
        if ((System.currentTimeMillis() - lastCheck) > cacheTime) {
            update();
        }

        return super.containsKey(key.toString());
    }

    /**
     * Returns an enumeration of all values
     * @return values enumeration
     */
    public Enumeration elements() {
        if ((System.currentTimeMillis() - lastCheck) > cacheTime) {
            update();
        }

        return super.elements();
    }

    /**
     * Returns a value in this list fetched by the given key
     * @param key key to use for fetching the value
     * @return value belonging to the given key
     */
    public Object get(Object key) {
        if ((System.currentTimeMillis() - lastCheck) > cacheTime) {
            update();
        }

        return (String) super.get(ignoreCase == true ? key.toString().toLowerCase() : key.toString());
    }

    /**
     * Returns the date the resources were last modified
     * @return last modified date
     */
    public long lastModified() {
        return lastModified;
    }

    /**
     * Returns a checksum for all resources
     * @return checksum
     */
    public long getChecksum() {
        long checksum = 0;

        if (app != null) {
            Iterator iterator = app.getRepositories();
            while (iterator.hasNext()) {
                Repository repository = (Repository) iterator.next();
                Resource resource = repository.getResource(resourceName);
                checksum += resource != null ? resource.lastModified() : repository.lastModified();
            }
        }

        if (resources != null) {
            Iterator iterator = resources.values().iterator();
            while (iterator.hasNext()) {
                checksum += ((Resource) iterator.next()).lastModified();
            }
        }

        return checksum;
    }

    /**
     * Returns a value in the list fetched by the given key or a default value
     * if no corresponding key is found
     * @param key key to use for fetching the value
     * @param defaultValue default value to return if key is not found
     * @return spiecific value or default value if not found
     */
    public String getProperty(String key, String defaultValue) {
        if ((System.currentTimeMillis() - lastCheck) > cacheTime) {
            update();
        }

        return super.getProperty(ignoreCase == true ? key.toLowerCase() : key, defaultValue);
    }

    /**
     * Returns a value in this list fetched by the given key
     * @param key key to use for fetching the value
     * @return value belonging to the given key
     */
    public String getProperty(String key) {
        if ((System.currentTimeMillis() - lastCheck) > cacheTime) {
            update();
        }

        return super.getProperty(ignoreCase == true ? key.toLowerCase() : key);
    }

    /**
     * Checks wether the properties list is empty
     * @return true if the properties list is empty
     */
    public boolean isEmpty() {
        if ((System.currentTimeMillis() - lastCheck) > cacheTime) {
            update();
        }

        return super.isEmpty();
    }

    /**
     * Checks wether case-sensitivity is ignored for keys
     * @return true if case-sensitivity is ignored for keys
     */
    public boolean isIgnoreCase() {
        return ignoreCase;
    }

    /**
     * Returns an enumeration of all keys
     * @return keys enumeration
     */
    public Enumeration keys() {
        if ((System.currentTimeMillis() - lastCheck) > cacheTime) {
            update();
        }

        return super.keys();
    }

    /**
     * Returns a set of all keys
     * @return keys set
     */
    public Set keySet() {
        if ((System.currentTimeMillis() - lastCheck) > cacheTime) {
            update();
        }

        return super.keySet();
    }

    /**
     * Puts a new key-value pair into the properties list
     * @param key key
     * @param value value
     * @return the old value, if an old value got replaced
     */
    public Object put(Object key, Object value) {
        if (value != null) {
            value = value.toString().trim();
        }

        return super.put(ignoreCase == true ? key.toString().toLowerCase() : key.toString(), value);
    }

    /**
     * Removes a key-value pair from the properties list
     * @param key key
     * @return the old value
     */
    public Object remove(Object key) {
        return super.remove(ignoreCase == true ? key.toString().toLowerCase() : key.toString());
    }

    /**
     * Changes how keys are handled
     * @param ignore true if to ignore case-sensitivity for keys
     */
    public void setIgnoreCase(boolean ignore) {
        if (!super.isEmpty()) {
            throw new RuntimeException("setIgnoreCase() can only be called on empty Properties");
        }
        ignoreCase = ignore;
        return;
    }

    /**
     * Returns the number of peroperties in the list
     * @return number of properties
     */
    public int size() {
        if ((System.currentTimeMillis() - lastCheck) > cacheTime) {
            update();
        }

        return super.size();
    }

    /**
     * Returns a string-representation of the class
     * @return string
     */
    public String toString() {
        return super.toString();
    }

}
