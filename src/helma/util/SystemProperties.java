// Property.java
// Copyright (c) Hannes Wallnöfer 1997-2000
 
package helma.util;

import java.util.*;
import java.io.*;

/**
 *  A property dictionary that is updated from a property file each time the
 *  file is modified. It is also case insensitive.
 */
 
public final class SystemProperties extends Properties {

    private Properties props;        // wrapped properties
    private Properties newProps;    // used while building up props
    private Properties defaultProps;  // the default/fallback properties.
    private File file;   // the underlying properties file from which we read.
    private long lastread, lastcheck, lastadd;  // time we last read/checked the underlying properties file

    // the timespan for which we omit checking for changed files after we
    // did a check, in milliseconds.
    final static long cacheTime = 1500l;

    private HashMap additionalProps = null;

    /** 
     *  Construct an empty properties object.
     */
    public SystemProperties () {
	this (null, null);
    }

    /**
     *  Construct a properties object and read it from an input stream.
     */
    public SystemProperties (InputStream in) {
	this (null, null);
	try {
	    load (in);
	} catch (Exception x) {
	    System.err.println ("Error reading properties from file "+file+": "+x);
	} finally {
	    try {
	        in.close ();
	    } catch (Exception ignore) {}
	}
	lastread = System.currentTimeMillis ();
    }

    /**
     *  Construct a properties object from a properties file.
     */
    public SystemProperties (String filename) {
	this (filename, null);
    }

    /**
     *  Contstruct a properties object with the given default properties.
     */
    public SystemProperties (Properties defaultProps) {
	this (null, defaultProps);
    }


    /**
     *  Construct a properties object from a file name with the given default properties
     */
    public SystemProperties (String filename, Properties defaultProps) {
	// System.err.println ("building sysprops with file "+filename+" and node "+node);
	this.defaultProps = defaultProps;
	props = defaultProps == null ? new Properties () : new Properties (defaultProps);
	file = filename == null ? null : new File (filename);
	lastcheck = lastread = lastadd = 0;
    }

   /**
    *  Return the modify-time of the underlying properties file.
    */
    public long lastModified () {
	if (file == null || !file.exists ())
	    return lastadd;
	return Math.max (file.lastModified (), lastadd);
    }

    /**
     *  Private method to read file if it has been changed since the last time we did
     */
    private void checkFile () {
	if (file != null && file.exists() && file.lastModified () > lastread)
	    readFile ();
	lastcheck = System.currentTimeMillis ();
    }

    /**
     *  Private method to read the underlying properties file. Assumes that the
     *  file exists and is readable.
     */
    private synchronized void readFile () {
	// IServer.getLogger().log ("Reading properties from file "+file);
	FileInputStream bpin = null;
	try {
	    bpin = new FileInputStream (file);
	    load (bpin);
	} catch (Exception x) {
	    System.err.println ("Error reading properties from file "+file+": "+x);
	} finally {
	    try {
	        bpin.close ();
	    } catch (Exception ignore) {}
	}
    }


    public synchronized void load (InputStream in) throws IOException {
	newProps = defaultProps == null ?
	    new Properties () : new Properties (defaultProps);
	super.load (in);
	lastread = System.currentTimeMillis ();
	props = newProps;
	newProps = null;
	if (additionalProps != null) {
	    for (Iterator i=additionalProps.values().iterator(); i.hasNext(); ) 
	        props.putAll ((Properties) i.next());
	}
    }


    /**
     * Similar to load(), but adds to the existing properties instead
     * of discarding them.
     */
    public synchronized void addProps (String key, InputStream in) throws IOException {
	Properties p = new Properties();
	p.load (in);
	if (additionalProps == null)
	    additionalProps = new HashMap ();
	additionalProps.put (key, p);
	if (props != null)
	    props.putAll (p);
	else
	    props = p;
	lastadd = System.currentTimeMillis ();
    }
    
    public synchronized void removeProps (String key) {
	if (additionalProps != null) {
	    // remove added properties for this key. If we had
	    // properties associated with the key, mark props as updated.
	    Object p = additionalProps.remove (key);
	    if (p != null)
	        lastadd = System.currentTimeMillis ();
	}
    }


    /*
     * This should not be used directly if properties are read from file,
     *  otherwise changes will be lost whe the file is next modified.
     */
    public Object put (Object key, Object value) {
	if (newProps == null)
	    return props.put (key.toString().toLowerCase(), value);
	else
	    return newProps.put (key.toString().toLowerCase(), value);
    }

    /**
     *  Overrides method to act on the wrapped properties object.
     */
    public Object get (Object key) {
	if (System.currentTimeMillis () - lastcheck > cacheTime)
	    checkFile ();
	return props.get (key.toString().toLowerCase());
    }

    /**
     *  Overrides method to act on the wrapped properties object.
     */
    public Object remove (Object key) {
	return props.remove (key.toString().toLowerCase());
    }

    /**
     *  Overrides method to act on the wrapped properties object.
     */
    public void clear () {
	props.clear ();
    }

    /**
     *  Overrides method to act on the wrapped properties object.
     */
    public boolean contains (Object obj) {
	if (System.currentTimeMillis () - lastcheck > cacheTime)
	    checkFile ();
	return props.contains (obj);
    }

    /**
     *  Overrides method to act on the wrapped properties object.
     */
    public boolean containsKey (Object key) {
	if (System.currentTimeMillis () - lastcheck > cacheTime)
	    checkFile ();
	return props.containsKey (key.toString().toLowerCase());
    }

    /**
     *  Overrides method to act on the wrapped properties object.
     */
    public boolean isEmpty () {
	if (System.currentTimeMillis () - lastcheck > cacheTime)
	    checkFile ();
	return props.isEmpty ();
    }

    /**
     *  Overrides method to act on the wrapped properties object.
     */
    public String getProperty (String name) {
	if (System.currentTimeMillis () - lastcheck > cacheTime)
	    checkFile ();
	return props.getProperty (name.toLowerCase());
    }

    /**
     *  Overrides method to act on the wrapped properties object.
     */
    public String getProperty (String name, String defaultValue) {
	if (System.currentTimeMillis () - lastcheck > cacheTime)
	    checkFile ();
	return props.getProperty (name.toLowerCase(), defaultValue);
    }

    /**
     *  Overrides method to act on the wrapped properties object.
     */
    public Enumeration keys () {
	if (System.currentTimeMillis () - lastcheck > cacheTime)
	    checkFile ();
	return props.keys();
    }

    /**
     *  Overrides method to act on the wrapped properties object.
     */
    public Set keySet () {
	if (System.currentTimeMillis () - lastcheck > cacheTime)
	    checkFile ();
	return props.keySet();
    }

    /**
     *  Overrides method to act on the wrapped properties object.
     */
    public Enumeration elements () {
	if (System.currentTimeMillis () - lastcheck > cacheTime)
	    checkFile ();
	return props.elements();
    }

    /**
     *  Overrides method to act on the wrapped properties object.
     */
    public int size() {
	if (System.currentTimeMillis () - lastcheck > cacheTime)
	    checkFile ();
	return props.size();
    }

    /**
     *  Overrides method to act on the wrapped properties object.
     */
    public String toString () {
	return props.toString ();
    }

}

