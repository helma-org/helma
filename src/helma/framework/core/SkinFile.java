// SkinFile.java
// Copyright (c) Hannes Wallnöfer 2001
 
package helma.framework.core;

import java.util.*;
import java.io.*;
import helma.util.Updatable;


/**
 * This represents a File containing a Hop skin
 */


public class SkinFile implements Updatable {

    String name;
    Prototype prototype;
    Application app;
    File file;
    Skin skin;
    long lastmod;

    public SkinFile (File file, String name, Prototype proto) {
	this.prototype = proto;
	this.app = proto.app;
	this.name = name;
	this.file = file;
	this.skin = null;
    }

    /**
     * Create a skinfile without a file, passing the skin body directly. This is used for
     * Skins contained in zipped applications. The whole update mechanism is bypassed
     *  by immediately setting the skin member.
     */
    public SkinFile (String body, String name, Prototype proto) {
	this.prototype = proto;
	this.app = proto.app;
	this.name = name;
	this.file = null;
	this.skin = new Skin (body, app);
    }

     /**
     * Tell the type manager whether we need an update. this is the case when
     * the file has been modified or deleted.
     */
    public boolean needsUpdate () {
	return (skin != null && lastmod != file.lastModified ()) || !file.exists ();
    }


    public void update () {

	if (!file.exists ()) {
	    // remove skin from  prototype
	    prototype.skins.remove (name);
	    prototype.updatables.remove (file.getName());
	} else {
	    // we only need to update if the skin has already been initialized
	    if (skin != null)
	        read ();
	}
    }

    private void read () {
	try {
	    FileReader reader = new FileReader (file);
	    char c[] = new char[(int) file.length()];
	    reader.read (c);
	    reader.close();
	    skin = new Skin (new String (c), app);
	} catch (IOException x) {
	    app.logEvent ("Error reading Skin "+file+": "+x);
	}
	
	lastmod = file.lastModified ();
    }

    public Skin getSkin () {
	if (skin == null)
	    read ();
	return skin;
    }

    public String getName () {
	return name;
    }
	
    public String toString () {
	return prototype.getName()+"/"+file.getName();
    }

}







































