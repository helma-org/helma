// ZippedFile.java
// Copyright (c) Hannes Wallnöfer 2001
 
package helma.framework.core;

import java.util.HashMap;
import java.util.Iterator;
import java.io.*;
import helma.framework.*;
import helma.util.Updatable;



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
	System.err.println ("CREATING ZIP FILE "+this);
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

	    lastmod = file.lastModified ();
	    System.err.println ("UPDATING ZIP FILE "+this);

	}

    }

    void remove () {
	app.typemgr.zipfiles.remove (file.getName());
	System.err.println ("REMOVING ZIP FILE "+this);
    }


    public String toString () {
	return file.getName();
    }


}







































