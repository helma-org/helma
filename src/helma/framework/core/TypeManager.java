// TypeManager.java
// Copyright (c) Hannes Wallnöfer 1998-2000
 
package helma.framework.core;

import helma.objectmodel.*;
import helma.objectmodel.db.DbMapping;
import helma.scripting.*;
import helma.util.*;
import java.util.*;
import java.io.*;


/**
 * The type manager periodically checks the prototype definitions for its 
 * applications and updates the evaluators if anything has changed.
 */

public final class TypeManager {

    Application app;
    File appDir;
    HashMap prototypes;
    HashMap zipfiles;
    long lastCheck = 0;
    long appDirMod = 0;

    // the hopobject prototype
    Prototype hopobjectProto;
    // the global prototype
    Prototype globalProto;
    
    final static String[] standardTypes = {"user", "global", "root", "hopobject"};

    final static String templateExtension = ".hsp";
    final static String scriptExtension = ".js";
    final static String actionExtension = ".hac";
    final static String skinExtension = ".skin";

    public TypeManager (Application app) {
        this.app = app;
        appDir = app.appDir;
        // make sure the directories for the standard prototypes exist, and lament otherwise
        if (appDir.list().length == 0) {
            for (int i=0; i<standardTypes.length; i++) {
                File f = new File (appDir, standardTypes[i]);
                if (!f.exists() && !f.mkdir ())
                    app.logEvent ("Warning: directory "+f.getAbsolutePath ()+" could not be created.");
                else if (!f.isDirectory ())
                    app.logEvent ("Warning: "+f.getAbsolutePath ()+" is not a directory.");
            }
        }
        prototypes = new HashMap ();
        zipfiles = new HashMap ();
    }


    /**
     * Run through application's prototype directories and create prototypes, but don't
     * compile or evaluate any scripts.
     */
    public void createPrototypes () {
        // create standard prototypes.
        createPrototype ("root");
        createPrototype ("user");
        // get references to hopobject and global protos,
        // since we need it regularly when setting parent prototypes.
        hopobjectProto = createPrototype ("hopobject");
        globalProto = createPrototype ("global");
        // loop through directories and create prototypes
        checkFiles ();
    }


    /**
     * Run through application's prototype directories and check if anything has been updated.
     * If so, update prototypes and scripts.
     */
    public synchronized void checkPrototypes () {
	if (System.currentTimeMillis () - lastCheck  < 1000l)
	    return;
	try {
	    checkFiles ();
	} catch (Exception ignore) {}
	lastCheck = System.currentTimeMillis ();
    }

    /**
     * Run through application's prototype directories and check if
     * there are any prototypes to be created.
     */
    public void checkFiles () {
	// check if any files have been created/removed since last time we
	// checked...
	if (appDir.lastModified() > appDirMod) {
	    appDirMod = appDir.lastModified ();
	    String[] list = appDir.list ();
	    if (list == null)
	        throw new RuntimeException ("Can't read app directory "+appDir+" - check permissions");
	    for (int i=0; i<list.length; i++) {
	        if (list[i].endsWith (".zip")) {
	            ZippedAppFile zipped = (ZippedAppFile) zipfiles.get (list[i]);
	            if (zipped == null) {
	                File f = new File (appDir, list[i]);
	                if (!f.isDirectory ()) {
	                    zipped = new ZippedAppFile (f, app);
	                    zipfiles.put (list[i], zipped);
	                }
	            }
	            continue;
	        }
	        if (list[i].indexOf ('.') > -1)
	            continue;
	        Prototype proto = getPrototype (list[i]);
	        // if prototype doesn't exist, create it
	        if (proto == null  && isValidTypeName (list[i])) {
	            File f = new File (appDir, list[i]);
	            if (f.isDirectory ()) {
	                // create new prototype
	                createPrototype (list[i], f);
	            }
	        }
	    }
	}

	// loop through zip files to check for updates
	for (Iterator it=zipfiles.values ().iterator (); it.hasNext (); ) {
	    ZippedAppFile zipped = (ZippedAppFile) it.next ();
	    if (zipped.needsUpdate ()) {
	        zipped.update ();
	    }
	}

	// loop through prototypes and check if type.properties needs updates
	// it's important that we do this _after_ potentially new prototypes
	// have been created in the previous loop.
	for (Iterator i=prototypes.values().iterator(); i.hasNext(); ) {
	    Prototype proto = (Prototype) i.next ();
	    // update prototype's type mapping
	    DbMapping dbmap = proto.getDbMapping ();
	    if (dbmap != null && dbmap.needsUpdate ()) {
	        dbmap.update ();
	        if (proto != hopobjectProto && proto != globalProto) {
	            // set parent prototype, in case it has changed.
	            String parentName = dbmap.getExtends ();
	            if (parentName != null)
	                proto.setParentPrototype (getPrototype (parentName));
	            else if (!app.isJavaPrototype (proto.getName()))
	                proto.setParentPrototype (hopobjectProto);
	        }
	    }
	}

    }

    protected void removeZipFile (String zipname) {
	zipfiles.remove (zipname);
	for (Iterator i=prototypes.values().iterator(); i.hasNext(); ) {
	    Prototype proto = (Prototype) i.next ();
	    // update prototype's type mapping
	    DbMapping dbmap = proto.getDbMapping ();
	    SystemProperties props = dbmap.getProperties();
	    props.removeProps (zipname);
	}
    }


    private boolean isValidTypeName (String str) {
	if (str == null)
	    return false;
	char[] c = str.toCharArray ();
	for (int i=0; i<c.length; i++)
	    if (!Character.isJavaIdentifierPart (c[i]))
	        return false;
	return true;
    }

    /**
    *   Get a prototype defined for this application
    */
    public Prototype getPrototype (String typename) {
	return (Prototype) prototypes.get (typename);
    }

    /**
     * Get a prototype, creating it if it doesn't already exist. Note
     * that it doesn't create a DbMapping - this is left to the 
     * caller (e.g. ZippedAppFile).
     */
    public Prototype createPrototype (String typename) {
	return createPrototype (typename, new File (appDir, typename));
    }


    /**
     *  Create a prototype from a directory containing scripts and other stuff
     */
    public Prototype createPrototype (String typename, File dir) {
	Prototype proto = new Prototype (typename, app);
	// Create and register type properties file
	File propfile = new File (dir, "type.properties");
	SystemProperties props = new SystemProperties (propfile.getAbsolutePath ());
	DbMapping dbmap = new DbMapping (app, typename, props);
	// we don't need to put the DbMapping into proto.updatables, because
	// dbmappings are checked separately in checkFiles for each request
	// proto.updatables.put ("type.properties", dbmap);
	proto.setDbMapping (dbmap);
	// put the prototype into our map
	prototypes.put (typename, proto);
	return proto;
    }


    /**
    * Update a prototype to the files in the prototype directory.
    */
    public void updatePrototype (String name) {
        // System.err.println ("UPDATE PROTO: "+app.getName()+"/"+name);
        Prototype proto = getPrototype (name);
        updatePrototype (proto);
    }

    /**
    * Update a prototype to the files in the prototype directory.
    */
    public void updatePrototype (Prototype proto) {

        if (proto == null)
            return;
        // if prototype has been checked in the last 1.5 seconds, return
        // if (System.currentTimeMillis() - proto.getLastCheck() < 2500)
        //     return;

        synchronized (proto) {
        // check again because another thread may have checked the
        // prototype while we were waiting for access to the synchronized section
        if (System.currentTimeMillis() - proto.getLastCheck() < 1000)
            return;

        File dir = new File (appDir, proto.getName());
        boolean needsUpdate = false;
        HashSet updatables = null;

        // our plan is to do as little as possible, so first check if
        // anything the prototype knows about has changed on disk
        for (Iterator i = proto.updatables.values().iterator(); i.hasNext(); ) {
            Updatable upd = (Updatable) i.next();
            if (upd.needsUpdate ()) {
                if (updatables == null)
                    updatables = new HashSet ();
                needsUpdate = true;
                updatables.add (upd);
            }
        }

        // next we check if files have been created or removed since last update
        if (proto.getLastCheck() < dir.lastModified ()) {
            String[] list = dir.list();
            for (int i=0; i<list.length; i++) {
                String fn = list[i];
                if (!proto.updatables.containsKey (fn)) {
                    if (fn.endsWith (templateExtension) || fn.endsWith (scriptExtension) ||
			fn.endsWith (actionExtension) || fn.endsWith (skinExtension) ||
			"type.properties".equalsIgnoreCase (fn)) {
                        needsUpdate = true;
                        // updatables.add ("[new:"+proto.getName()+"/"+fn+"]");
                    }
                }
            }
        }

        // if nothing needs to be updated, mark prototype as checked and return
        if (!needsUpdate) {
            proto.markChecked ();
            return;
        }

        // app.logEvent ("TypeManager: Updating prototypes for "+app.getName()+": "+updatables);

        // first go through new files and create new items
        String[] list = dir.list ();
        for (int i=0; i<list.length; i++) {
            String fn = list[i];
            int dot = fn.indexOf (".");

            if (dot < 0)
                continue;

            if (proto.updatables.containsKey (fn) || !(fn.endsWith (templateExtension) || fn.endsWith (scriptExtension) ||
            fn.endsWith (actionExtension) || fn.endsWith (skinExtension) || "type.properties".equalsIgnoreCase (fn))) {
                continue;
            }

            String tmpname = list[i].substring(0, dot);
            File tmpfile = new File (dir, list[i]);

            if (list[i].endsWith (templateExtension)) {
                try {
                    Template t = new Template (tmpfile, tmpname, proto);
                    proto.updatables.put (list[i], t);
                    proto.templates.put (tmpname, t);
                } catch (Throwable x) {
                    app.logEvent ("Error updating prototype: "+x);
                }

            } else if (list[i].endsWith (scriptExtension)) {
                try {
                    FunctionFile ff = new FunctionFile (tmpfile, tmpname, proto);
                    proto.updatables.put (list[i], ff);
                    proto.functions.put (tmpname, ff);
                } catch (Throwable x) {
                    app.logEvent ("Error updating prototype: "+x);
                }

            }  else if (list[i].endsWith (actionExtension)) {
                try {
                    ActionFile af = new ActionFile (tmpfile, tmpname, proto);
                    proto.updatables.put (list[i], af);
                    proto.actions.put (tmpname, af);
                } catch (Throwable x) {
                    app.logEvent ("Error updating prototype: "+x);
                }

            }  else if (list[i].endsWith (skinExtension)) {
                SkinFile sf = new SkinFile (tmpfile, tmpname, proto);
                proto.updatables.put (list[i], sf);
                proto.skins.put (tmpname, sf);
            }
        }

        // next go through existing updatables
        if (updatables != null) {
            for (Iterator i = updatables.iterator(); i.hasNext(); ) {
                Updatable upd = (Updatable) i.next();

                try {
                    upd.update ();
                } catch (Exception x) {
                     if (upd instanceof DbMapping)
                        app.logEvent ("Error updating db mapping for type "+proto.getName()+": "+x);
                     else
                        app.logEvent ("Error updating "+upd+" of prototye type "+proto.getName()+": "+x);
                }
            }
        }

        // mark prototype as checked and updated.
        proto.markChecked ();
        proto.markUpdated();

        } // end of synchronized (proto)

    }

}

