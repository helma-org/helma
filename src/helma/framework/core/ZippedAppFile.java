// ZippedFile.java
// Copyright (c) Hannes Wallnöfer 2001
 
package helma.framework.core;

import java.util.*;
import java.util.zip.*;
import java.io.*;
import helma.framework.*;
import helma.util.Updatable;
import helma.objectmodel.SystemProperties;
import helma.objectmodel.DbMapping;

/**
 * This represents a Zip-File which may contain other Updatables for one or more prototypes.
 */


public class ZippedAppFile implements Updatable {

    Application app;
    File file;
    long lastmod;


    public ZippedAppFile (File file, Application app) {
	this.app = app;
	this.file = file;
	// System.err.println ("CREATING ZIP FILE "+this);
    }


    /**
     * Tell the type manager whether we need an update. this is the case when
     * the file has been modified or deleted.
     */
    public boolean needsUpdate () {
	return lastmod != file.lastModified () || !file.exists ();
    }


    public void update () {

	if (!file.exists ()) {
	    remove ();

	} else {

	    ZipFile zip = null;
	    // collect created protos - we need this to check DbMappings for each created
	    // prototype afterwards
	    HashSet newPrototypes = new HashSet ();
	    try {
	        lastmod = file.lastModified ();
	        // System.err.println ("UPDATING ZIP FILE "+this);
	        zip = new ZipFile (file);
	        for (Enumeration en = zip.entries (); en.hasMoreElements (); ) {
	            ZipEntry entry = (ZipEntry) en.nextElement ();
	            String ename = entry.getName ();
	            StringTokenizer st = new StringTokenizer (ename, "/");
	            if (st.countTokens () == 2) {
	                String dir = st.nextToken ();
	                String fname = st.nextToken ();
	                // System.err.println ("ZIPENTRY: "+ dir +" ~ "+fname);
	                Prototype proto = app.typemgr.getPrototype (dir);
	                if (proto == null) {
	                    proto = app.typemgr.createPrototype (dir);
	                    newPrototypes.add (proto);
	                }
	                if (fname.endsWith (".hac")) {
	                    String name = fname.substring (0, fname.lastIndexOf ("."));
	                    String content = getZipEntryContent (zip, entry);
	                    // System.err.println ("["+content+"]");
	                    Action act = new Action (null, name, proto);
	                    act.update (content);
	                    proto.actions.put (name, act);
	                }
	                else if (fname.endsWith (".hsp")) {
	                    String name = fname.substring (0, fname.lastIndexOf ("."));
	                    String content = getZipEntryContent (zip, entry);
	                    // System.err.println ("["+content+"]");
	                    Template tmp = new Template (null, name, proto);
	                    tmp.update (content);
	                    proto.templates.put (name, tmp);
	                }
	                else if (fname.endsWith (".skin")) {
	                    String name = fname.substring (0, fname.lastIndexOf ("."));
	                    String content = getZipEntryContent (zip, entry);
	                    // System.err.println ("["+content+"]");
	                    SkinFile skin = new SkinFile (content, name, proto);
	                    proto.skins.put (name, skin);
	                }
	                else if (fname.endsWith (".js")) {
	                    String name = fname.substring (0, fname.lastIndexOf ("."));
	                    String content = getZipEntryContent (zip, entry);
	                    // System.err.println ("["+content+"]");
	                    FunctionFile ff = new FunctionFile (content, name, proto);
	                    proto.functions.put (name, ff);
	                }
	                else if ("type.properties".equalsIgnoreCase (fname)) {
	                    String name = fname.substring (0, fname.lastIndexOf ("."));
	                    SystemProperties props = new SystemProperties (zip.getInputStream (entry));
	                    // DbMapping does its own registering, just construct it.
	                    new DbMapping (app, proto.getName (), props);
	                }
	            }
	        }
	        for  (Iterator it = newPrototypes.iterator (); it.hasNext (); ) {
	            Prototype proto = (Prototype) it.next ();
	            if (app.getDbMapping (proto.getName ()) == null) {
	                // DbMapping doesn't exist, we still need to create one
	                SystemProperties props = new SystemProperties ();
	                // DbMapping does its own registering, just construct it.
	                new DbMapping (app, proto.getName (), props);
	            }
	        }
	    } catch (Throwable x) {
	        System.err.println ("Error updating ZipFile: "+x);
	    } finally {
	        try {
	           zip.close ();
	        } catch (Exception ignore) {}
	    }
	}

    }

    void remove () {
	app.typemgr.zipfiles.remove (file.getName());
	// System.err.println ("REMOVING ZIP FILE "+this);
    }


    public String getZipEntryContent (ZipFile zip, ZipEntry entry) throws IOException {
	int size = (int) entry.getSize ();
	char[] c = new char[size];
	InputStreamReader reader = new InputStreamReader (zip.getInputStream (entry));
	reader.read (c);
	return new String (c);
    }


    public String toString () {
	return file.getName();
    }


}







































