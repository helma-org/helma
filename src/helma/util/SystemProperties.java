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
    private Properties defaultProps;
    private File file;
    private long lastread, lastcheck;

    final static long cacheTime = 1500l;


    public SystemProperties () {
	this (null, null);
    }

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

    public SystemProperties (String filename) {
	this (filename, null);
    }

    public SystemProperties (Properties defaultProps) {
	this (null, defaultProps);
    }

    public SystemProperties (String filename, Properties defaultProps) {
 	// System.err.println ("building sysprops with file "+filename+" and node "+node);
	this.defaultProps = defaultProps;
	props = defaultProps == null ?
	        new Properties () : new Properties (defaultProps);

	if (filename != null) {
	    file = new File (filename);
	    checkFile ();
	}
    }

    public boolean wasModified () {
	return file != null && file.exists() && file.lastModified () > lastread;
    }

    public long lastModified () {
	return file == null || !file.exists () ? 0 : file.lastModified ();
    }


    private synchronized void checkFile () {
	if (wasModified ()) {
	    // IServer.getLogger().log ("Reading properties from file "+file);
	    newProps = defaultProps == null ?
	        new Properties () : new Properties (defaultProps);
	    try {
	        FileInputStream bpin = new FileInputStream (file);
	        load (bpin);
	        bpin.close ();
	    } catch (Exception x) {
	        System.err.println ("Error reading properties from file "+file+": "+x);
	    }
	    lastread = System.currentTimeMillis ();
	    props = newProps;
	    newProps = null;
	}
	lastcheck = System.currentTimeMillis ();
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

    public Object get (Object key) {
	return props.get (key);
    }

    public Object remove (Object key) {
	return props.remove (key);
    }

    public void clear () {
	props.clear ();
    }

    public boolean contains (Object obj) {
	return props.contains (obj);
    }

    public boolean containsKey (Object key) {
	return props.containsKey (key);
    }

    public boolean isEmpty () {
	return props.isEmpty ();
    }

    public String getProperty (String name) {
	if (System.currentTimeMillis () - lastcheck > cacheTime)
	    checkFile ();
	return props.getProperty (name.toLowerCase());
    }

    public String getProperty (String name, String defaultValue) {
	if (System.currentTimeMillis () - lastcheck > cacheTime)
	    checkFile ();
	return props.getProperty (name.toLowerCase(), defaultValue);
    }

    public Enumeration keys () {
	if (System.currentTimeMillis () - lastcheck > cacheTime)
	    checkFile ();
	return props.keys();
    }

    public Enumeration elements () {
	if (System.currentTimeMillis () - lastcheck > cacheTime)
	    checkFile ();
	return props.elements();
    }

}





