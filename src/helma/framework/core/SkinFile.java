// SkinFile.java
// Copyright (c) Hannes Wallnöfer 2001
 
package helma.framework.core;

import java.util.*;
import java.io.*;
import helma.objectmodel.IServer;


/**
 * This represents a File containing a Hop skin
 */


public class SkinFile {

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


    public void update (File f) {

	this.file = f;

	long fmod = file.lastModified ();
	// we only update this if we already have read the skin
	if (skin == null || lastmod == fmod)
	    return;

	read ();
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
	

}







































