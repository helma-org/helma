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

import java.io.*;
import java.util.*;

/**
 *  A property dictionary that is updated from a property file each time the
 *  file is modified. It is also case insensitive.
 */
public final class SystemProperties extends Properties {
    final static long cacheTime = 1500L;
    private SystemProperties defaultProps; // the default/fallback properties.
    private File file; // the underlying properties file from which we read.
    private long lastread; // time we last read/checked the underlying properties file
    private long lastcheck; // time we last read/checked the underlying properties file
    private long lastadd; // time we last read/checked the underlying properties file

    // map of additional properties
    private HashMap additionalProps = null;

    /**
     *  Construct an empty properties object.
     */
    public SystemProperties() {
        this(null, null);
    }

    /**
     *  Construct a properties object and read it from an input stream.
     */
    public SystemProperties(InputStream in) {
        this(null, null);

        try {
            load(in);
        } catch (Exception x) {
            System.err.println("Error reading properties from file " + file + ": " + x);
        } finally {
            try {
                in.close();
            } catch (Exception ignore) {
            }
        }

        lastread = System.currentTimeMillis();
    }

    /**
     *  Construct a properties object from a properties file.
     */
    public SystemProperties(String filename) {
        this(filename, null);
    }

    /**
     *  Contstruct a properties object with the given default properties.
     */
    public SystemProperties(SystemProperties defaultProps) {
        this(null, defaultProps);
    }

    /**
     *  Construct a properties object from a file name with the given default properties
     */
    public SystemProperties(String filename, SystemProperties defaultProps) {
        // System.err.println ("building sysprops with file "+filename+" and node "+node);
        super(defaultProps);
        this.defaultProps = defaultProps;
        file = (filename == null) ? null : new File(filename);
        lastcheck = lastread = lastadd = 0;
    }

    /**
     *  Return the modify-time of the underlying properties file.
     */
    public long lastModified() {
        if ((file == null) || !file.exists()) {
            return lastadd;
        }

        return Math.max(file.lastModified(), lastadd);
    }

    /**
     *  Return a checksum that changes when something in the properties changes.
     */
    public long getChecksum() {
        if (defaultProps == null) {
            return lastModified();
        }

        return lastModified() + defaultProps.lastModified();
    }

    /**
     *  Private method to read file if it has been changed since the last time we did
     */
    private void checkFile() {
        if ((file != null) && file.exists() && (file.lastModified() > lastread)) {
            readFile();
        }

        lastcheck = System.currentTimeMillis();
    }

    /**
     *  Private method to read the underlying properties file. Assumes that the
     *  file exists and is readable.
     */
    private synchronized void readFile() {
        // IServer.getLogger().log ("Reading properties from file "+file);
        FileInputStream bpin = null;

        try {
            bpin = new FileInputStream(file);
            load(bpin);
        } catch (Exception x) {
            System.err.println("Error reading properties from file " + file + ": " + x);
        } finally {
            try {
                bpin.close();
            } catch (Exception ignore) {
            }
        }
    }

    /**
     *
     *
     * @param in ...
     *
     * @throws IOException ...
     */
    public synchronized void load(InputStream in) throws IOException {
        clear();
        super.load(in);

        if (additionalProps != null) {
            for (Iterator i = additionalProps.values().iterator(); i.hasNext();)
                putAll((Properties) i.next());
        }

        lastread = System.currentTimeMillis();
    }

    /**
     * Similar to load(), but adds to the existing properties instead
     * of discarding them.
     */
    public synchronized void addProps(String key, InputStream in)
                               throws IOException {
        Properties p = new SystemProperties();

        p.load(in);

        if (additionalProps == null) {
            additionalProps = new HashMap();
        }

        additionalProps.put(key, p);
        putAll(p);
        lastadd = System.currentTimeMillis();
    }

    /**
     *  Remove an additional properties dictionary.
     */
    public synchronized void removeProps(String key) {
        if (additionalProps != null) {
            // remove added properties for this key. If we had
            // properties associated with the key, mark props as updated.
            Object p = additionalProps.remove(key);

            if (p != null) {
                lastadd = System.currentTimeMillis();
            }
        }
    }

    /*
     * This should not be used directly if properties are read from file,
     *  otherwise changes will be lost whe the file is next modified.
     */
    public Object put(Object key, Object value) {
        // cut off trailing whitespace
        if (value != null) {
            value = value.toString().trim();
        }

        return super.put(key.toString().toLowerCase(), value);
    }

    /**
     *  Overrides method to act on the wrapped properties object.
     */
    public Object get(Object key) {
        if ((System.currentTimeMillis() - lastcheck) > cacheTime) {
            checkFile();
        }

        return super.get(key.toString().toLowerCase());
    }

    /**
     *  Overrides method to act on the wrapped properties object.
     */
    public Object remove(Object key) {
        return super.remove(key.toString().toLowerCase());
    }

    /**
     *  Overrides method to act on the wrapped properties object.
     */
    public boolean contains(Object obj) {
        if ((System.currentTimeMillis() - lastcheck) > cacheTime) {
            checkFile();
        }

        return super.contains(obj);
    }

    /**
     *  Overrides method to act on the wrapped properties object.
     */
    public boolean containsKey(Object key) {
        if ((System.currentTimeMillis() - lastcheck) > cacheTime) {
            checkFile();
        }

        return super.containsKey(key.toString().toLowerCase());
    }

    /**
     *  Overrides method to act on the wrapped properties object.
     */
    public boolean isEmpty() {
        if ((System.currentTimeMillis() - lastcheck) > cacheTime) {
            checkFile();
        }

        return super.isEmpty();
    }

    /**
     *  Overrides method to act on the wrapped properties object.
     */
    public String getProperty(String name) {
        if ((System.currentTimeMillis() - lastcheck) > cacheTime) {
            checkFile();
        }

        return super.getProperty(name.toLowerCase());
    }

    /**
     *  Overrides method to act on the wrapped properties object.
     */
    public String getProperty(String name, String defaultValue) {
        if ((System.currentTimeMillis() - lastcheck) > cacheTime) {
            checkFile();
        }

        return super.getProperty(name.toLowerCase(), defaultValue);
    }

    /**
     *  Overrides method to act on the wrapped properties object.
     */
    public Enumeration keys() {
        if ((System.currentTimeMillis() - lastcheck) > cacheTime) {
            checkFile();
        }

        return super.keys();
    }

    /**
     *  Overrides method to act on the wrapped properties object.
     */
    public Set keySet() {
        if ((System.currentTimeMillis() - lastcheck) > cacheTime) {
            checkFile();
        }

        return super.keySet();
    }

    /**
     *  Overrides method to act on the wrapped properties object.
     */
    public Enumeration elements() {
        if ((System.currentTimeMillis() - lastcheck) > cacheTime) {
            checkFile();
        }

        return super.elements();
    }

    /**
     *  Overrides method to act on the wrapped properties object.
     */
    public int size() {
        if ((System.currentTimeMillis() - lastcheck) > cacheTime) {
            checkFile();
        }

        return super.size();
    }

    /**
     *  Overrides method to act on the wrapped properties object.
     */
    public String toString() {
        return super.toString();
    }
}
