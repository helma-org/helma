// FunctionFile.java
// Copyright (c) Hannes Wallnöfer 1998-2000
 
package helma.framework.core;

import java.util.*;
import java.io.*;
import helma.framework.*;


/**
 * This represents a File containing JavaScript functions for a given Object. 
 */


public class FunctionFile {

    String name;
    Prototype prototype;
    Application app;
    long lastmod;

    public FunctionFile (File file, String name, Prototype proto) {
	this.prototype = proto;
	this.app = proto.app;
	this.name = name;
	update (file);
    }


    public void update (File f) {

	long fmod = f.lastModified ();
	if (lastmod == fmod)
	    return;

	lastmod = fmod;
	app.typemgr.readFunctionFile (f, prototype.getName ());
    }


}







































