// ZippedFile.java
// Copyright (c) Hannes Wallnöfer 2001
 
package helma.framework.core;

import java.util.*;
import java.util.zip.*;
import java.io.*;
import helma.framework.*;
import helma.scripting.*;
import helma.util.Updatable;
import helma.util.SystemProperties;
import helma.objectmodel.db.DbMapping;

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
	return !file.exists () || lastmod != file.lastModified ();
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
	                    ActionFile act = new ActionFile (content, name, proto);
	                    proto.actions.put (name, act);
	                    // mark prototype as updated
	                    proto.markUpdated ();
	                }
	                else if (fname.endsWith (".hsp")) {
	                    String name = fname.substring (0, fname.lastIndexOf ("."));
	                    String content = getZipEntryContent (zip, entry);
	                    // System.err.println ("["+content+"]");
	                    Template tmp = new Template (content, name, proto);
	                    proto.templates.put (name, tmp);
	                    // mark prototype as updated
	                    proto.markUpdated ();
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
	                    // mark prototype as updated
	                    proto.markUpdated ();
	                }
	                else if ("type.properties".equalsIgnoreCase (fname)) {
	                    String name = fname.substring (0, fname.lastIndexOf ("."));
	                    DbMapping dbmap = proto.getDbMapping ();
	                    if (dbmap == null) {
	                        SystemProperties props = new SystemProperties (zip.getInputStream (entry));
	                        dbmap = new DbMapping (app, proto.getName (), props);
	                        proto.setDbMapping (dbmap);
	                    } else {
	                        // FIXME: provide a way to let zip files add to
	                        // type.properties files of existing prototypes.
	                        // SystemProperties props = dbmap.getProperties ();
	                        // props.add (zip.getInputStream (entry));
	                    }
	                    // mark prototype as updated
	                    proto.markUpdated ();
	                }
	            }
	        }
	        for  (Iterator it = newPrototypes.iterator (); it.hasNext (); ) {
	            Prototype proto = (Prototype) it.next ();
	            if (proto.getDbMapping() == null) {
	                // DbMapping doesn't exist, we still need to create one
	                SystemProperties props = new SystemProperties ();
	                proto.setDbMapping (new DbMapping (app, proto.getName (), props));
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



