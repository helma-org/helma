// SkinFile.java
// Copyright (c) Hannes Wallnöfer 2001
 
package helma.framework.core;

import java.util.*;
import java.io.*;
import helma.util.Updatable;


/**
 * This represents a File containing a Hop skin
 */


public final class SkinFile implements Updatable {

    String name;
    Prototype prototype;
    Application app;
    File file;
    Skin skin;
    long lastmod = 0;

    public SkinFile (File file, String name, Prototype proto) {
	this.prototype = proto;
	this.file = file;
	this.name = name;
	this.app = proto.app;
	skin = null;
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
	skin = new Skin (body, app);
    }

    /**
     * Create a skinfile that doesn't belong to a prototype, or at
     * least it doesn't know about its prototype and isn't managed by the prototype.
     */
    public SkinFile (File file, String name, Application app) {
	this.app = app;
	this.file = file;
	this.name = name;
	this.prototype = null;
	skin = null;
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
	    remove ();
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
	    int length = reader.read (c);
	    reader.close();
	    skin = new Skin (c, length, app);
	} catch (IOException x) {
	    app.logEvent ("Error reading Skin "+file+": "+x);
	}
	lastmod = file.lastModified ();
    }

    public void remove () {
	if (prototype != null) {
	    prototype.removeSkinFile (this);
	}
    }


    public File getFile () {
	return file;
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


