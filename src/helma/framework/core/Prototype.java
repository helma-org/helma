// Prototype.java
// Copyright (c) Hannes Wallnöfer 1998-2000
 
package helma.framework.core;

import java.util.*;
import java.io.*;
import helma.framework.*;
import helma.scripting.*;
import helma.objectmodel.*;
import helma.objectmodel.db.DbMapping;
import helma.util.Updatable;


/**
 * The Prototype class represents Script prototypes/type defined in a Helma
 * application. This class manages a prototypes templates, functions and actions
 * as well as optional information about the mapping of this type to a
 * relational database table.
 */


public final class Prototype {

    String name;
    Application app;
    File directory;

    File [] files;
    long lastDirectoryListing;
    long checksum;

    HashMap code, zippedCode;
    HashMap skins, zippedSkins;
    HashMap updatables;

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

    public Prototype (String name, File dir, Application app) {
	// app.logEvent ("Constructing Prototype "+app.getName()+"/"+name);
	this.app = app;
	this.name = name;
	this.directory = dir;

	code = new HashMap ();
	zippedCode = new HashMap ();
	skins = new HashMap ();
	zippedSkins = new HashMap ();
	updatables = new HashMap ();

	skinMap = new SkinMap ();

	isJavaPrototype = app.isJavaPrototype (name);
	lastUpdate = lastChecksum = 0;
    }

    /**
     *  Return the application this prototype is a part of
     */
    public Application getApplication () {
	return app;
    }

    /**
     *  Return this prototype's directory.
     */
    public File getDirectory () {
	return directory;
    }

    /**
     *  Get the list of files in this prototype's directory
     */
    public File[] getFiles () {
	if (files == null || directory.lastModified() != lastDirectoryListing) {
	    lastDirectoryListing = directory.lastModified ();
	    files = directory.listFiles();
	    if (files == null)
	        files = new File[0];
	}
	return files;
    }

    /**
     *  Get a checksum over the files in this prototype's directory
     */
    public long getChecksum () {
    // long start = System.currentTimeMillis();
	File[] f = getFiles ();
	long c = 0;
	for (int i=0; i<f.length; i++)
	    c += f[i].lastModified();
	checksum = c;
    // System.err.println ("CHECKSUM "+name+": "+(System.currentTimeMillis()-start));
	return checksum;
    }
    
    public boolean isUpToDate () {
	return checksum == lastChecksum;
    }

    /**
     *  Set the parent prototype of this prototype, i.e. the prototype this
     *  prototype inherits from.
     */
    public void setParentPrototype (Prototype parent) {
	// this is not allowed for the hopobject and global prototypes
	if ("hopobject".equalsIgnoreCase (name) || "global".equalsIgnoreCase (name))
	    return;
	this.parent = parent;
    }

    /**
     *  Get the parent prototype from which we inherit, or null
     *  if we are top of the line.
     */
    public Prototype getParentPrototype () {
	return parent;
    }
    
    /**
     * Check if the given prototype is within this prototype's parent chain.
     */
    public final boolean isInstanceOf (String pname) {
	if (name.equals (pname))
	   return true;
	if (parent != null && !"hopobject".equalsIgnoreCase (parent.getName()))
	   return parent.isInstanceOf (pname);
	return false;
    }
    
    /**
     * Register an object as handler for this prototype and all our parent prototypes.
     */
    public final void addToHandlerMap (Map handlers, Object obj) {
	if (parent != null && !"hopobject".equalsIgnoreCase (parent.getName()))
	   parent.addToHandlerMap (handlers, obj);
	handlers.put (name, obj);
    }

    public void setDbMapping (DbMapping dbmap) {
	this.dbmap = dbmap;
    }

    public DbMapping getDbMapping () {
	return dbmap;
    }


    /**
     *  Get a Skinfile for this prototype. This only works for skins
     *  residing in the prototype directory, not for skin files in
     *  other locations or database stored skins.
     */
    public SkinFile getSkinFile (String sfname) {
	SkinFile sf = (SkinFile) skins.get (sfname);
	if (sf == null)
	    sf = (SkinFile) zippedSkins.get (sfname);
	return sf;
    }

    /**
     *  Get a skin for this prototype. This only works for skins
     *  residing in the prototype directory, not for skins files in
     *  other locations or database stored skins.
     */
    public Skin getSkin (String sfname) {
	SkinFile sf = getSkinFile (sfname);
	if (sf != null)
	    return sf.getSkin ();
	else
	    return null;
    }


    public String getName () {
	return name;
    }


    /**
     *  Get the last time any script has been re-read for this prototype.
     */
    public long getLastUpdate () {
	return lastUpdate;
    }

    /**
     *  Signal that some script in this prototype has been
     *  re-read from disk and needs to be re-compiled by
     *  the evaluators.
     */
    public void markUpdated () {
	lastUpdate = System.currentTimeMillis ();
    }

    /**
     *  Get the time at which this prototype's scripts were checked
     *  for changes for the last time.
     */
    /* public long getLastCheck () {
	return lastCheck;
    } */

    /**
     *  Signal that the prototype's scripts have been checked for
     *  changes.
     */
    public void markChecked () {
	// lastCheck = System.currentTimeMillis ();
	lastChecksum = checksum;
    }


    /**
     *  Return a clone of this prototype's actions container. Synchronized
     *  to not return a map in a transient state where it is just being
     *  updated by the type manager.
     */
    public synchronized Map getCode () {
	return (Map) code.clone();
    }

    /**
     *  Return a clone of this prototype's functions container. Synchronized
     *  to not return a map in a transient state where it is just being
     *  updated by the type manager.
     */
    public synchronized Map getZippedCode () {
	return (Map) zippedCode.clone();
    }


    public synchronized void addActionFile (ActionFile action) {
	File f = action.getFile ();
	if (f != null) {
	    code.put (action.getSourceName(), action);
	    updatables.put (f.getName(), action);
	} else {
	    zippedCode.put (action.getSourceName(), action);
	}
    }

    public synchronized void addTemplate (Template template) {
	File f = template.getFile ();
	if (f != null) {
	    code.put (template.getSourceName(), template);
	    updatables.put (f.getName(), template);
	} else {
	    zippedCode.put (template.getSourceName(), template);
	}
    }

    public synchronized void addFunctionFile (FunctionFile funcfile) {
	File f = funcfile.getFile ();
	if (f != null) {
	    code.put (funcfile.getSourceName(), funcfile);
	    updatables.put (f.getName(), funcfile);
	} else {
	    zippedCode.put (funcfile.getSourceName(), funcfile);
	}
    }

    public synchronized void addSkinFile (SkinFile skinfile) {
	File f = skinfile.getFile ();
	if (f != null) {
	    skins.put (skinfile.getName(), skinfile);
	    updatables.put (f.getName(), skinfile);
	} else {
	    zippedSkins.put (skinfile.getName(), skinfile);
	}
    }


    public synchronized void removeActionFile (ActionFile action) {
	File f = action.getFile ();
	if (f != null) {
	    code.remove (action.getSourceName());
	    updatables.remove (f.getName());
	} else {
	    zippedCode.remove (action.getSourceName());
	}
    }

    public synchronized void removeFunctionFile (FunctionFile funcfile) {
	File f = funcfile.getFile ();
	if (f != null) {
	    code.remove (funcfile.getSourceName());
	    updatables.remove (f.getName());
	} else {
	    zippedCode.remove (funcfile.getSourceName());
	}
    }

    public synchronized void removeTemplate (Template template) {
	File f = template.getFile ();
	if (f != null) {
	    code.remove (template.getSourceName());
	    updatables.remove (f.getName());
	} else {
	    zippedCode.remove (template.getSourceName());
	}
    }

    public synchronized void removeSkinFile (SkinFile skinfile) {
	File f = skinfile.getFile ();
	if (f != null) {
	    skins.remove (skinfile.getName());
	    updatables.remove (f.getName());
	} else {
	    zippedSkins.remove (skinfile.getName());
	}
    }


   /**
    *  Return a string representing this prototype.
    */
    public String toString () {
	return "[Prototype "+ app.getName()+"/"+name+"]";
    }


    public SkinMap getSkinMap () {
	return skinMap;
    }

    // not yet implemented
    public SkinMap getSkinMap (Object[] skinpath) {
	return new SkinMap (skinpath);
    }

    // a map that dynamically expands to all skins in this prototype
    final class SkinMap extends HashMap {

	long lastSkinmapLoad = 0;

	Object[] skinpath;

	SkinMap () {
	    super ();
	    skinpath = null;
	}

	SkinMap (Object[] path) {
	    super ();
	    skinpath = path;
	}

	public boolean containsKey (Object key) {
	    checkForUpdates ();
	    return super.containsKey (key);
	}

	public boolean containsValue (Object value) {
	    checkForUpdates ();
	    return super.containsValue (value);
	}

	public Set entrySet () {
	    checkForUpdates ();
	    return super.entrySet ();
	}

	public boolean equals (Object obj) {
	    checkForUpdates ();
	    return super.equals (obj);
	}

	public Object get (Object key) {
	    if (key == null)
	        return null;
	    checkForUpdates ();
	    SkinFile sf = (SkinFile) super.get (key);
	    if (sf == null)
	        return null;
	    return sf.getSkin().getSource ();
	}

	public int hashCode () {
	    checkForUpdates ();
	    return super.hashCode ();
	}

	public boolean isEmpty () {
	    checkForUpdates ();
	    return super.isEmpty ();
	}

	public Set keySet () {
	    checkForUpdates ();
	    return super.keySet ();
	}

	public Object put (Object key, Object value) {
	    // checkForUpdates ();
	    return super.put (key, value);
	}

	public void putAll (Map t) {
	    // checkForUpdates ();
	    super.putAll (t);
	}

	public Object remove (Object key) {
	    checkForUpdates ();
	    return super.remove (key);
	}

	public int size () {
	    checkForUpdates ();
	    return super.size ();
	}

	public Collection values () {
	    checkForUpdates ();
	    return super.values ();
	}


	private void checkForUpdates () {
	    if (/* lastCheck < System.currentTimeMillis()- 2000l*/ !isUpToDate())
	        app.typemgr.updatePrototype (Prototype.this);
	    if (lastUpdate > lastSkinmapLoad)
	        load ();
	}

	private synchronized void load () {
	    if (lastUpdate == lastSkinmapLoad)
	        return;
	    super.clear ();
	    // System.err.println ("LOADING SKIN VALUES: "+Prototype.this);
	    for (Iterator i = skins.entrySet().iterator(); i.hasNext(); ) {
	        Map.Entry e = (Map.Entry) i.next ();
	        super.put (e.getKey(), e.getValue());
	    }
	    // if skinpath is not null, overload/add skins from there
	    if (skinpath != null) {
	        for (int i=skinpath.length-1; i>=0; i--) {
	            if (skinpath[i] != null && skinpath[i] instanceof String) {
	                Map m = app.skinmgr.getSkinFiles ((String) skinpath[i], Prototype.this);
	                if (m != null)
	                    super.putAll (m);
	            }
	        }
	    }
	    lastSkinmapLoad = lastUpdate;
	}
	
	public String toString () {
	    return "[SkinMap "+name+"]";
	}

    }

}

