// FunctionFile.java
// Copyright (c) Hannes Wallnöfer 1998-2000
 
package helma.scripting;

import java.util.HashMap;
import java.util.Iterator;
import java.util.HashSet;
import java.util.Enumeration;
import java.io.*;
import helma.framework.*;
import helma.framework.core.*;
import helma.util.Updatable;


/**
 * This represents a File containing script functions for a given class/prototype.
 */


public class FunctionFile implements Updatable {

    Prototype prototype;
    Application app;
    File file;
    String sourceName;
    String content;
    long lastmod;


    public FunctionFile (File file, Prototype proto) {
	this.prototype = proto;
	this.app = proto.getApplication ();
	this.sourceName = file.getParentFile().getName()+"/"+file.getName();
	this.file = file;
	update ();
    }

    /**
     *  Create a function file without a file, passing the code directly. This is used for
     *  files contained in zipped applications. The whole update mechanism is bypassed
     *  by immediately parsing the code.
     */
    public FunctionFile (String body, String sourceName, Prototype proto) {
	this.prototype = proto;
	this.app = proto.getApplication ();
	this.sourceName = sourceName;
	this.file = null;
	this.content = body;
    }

    /**
     * Tell the type manager whether we need an update. this is the case when
     * the file has been modified or deleted.
     */
    public boolean needsUpdate () {
	return file != null && lastmod != file.lastModified ();
    }


    public void update () {
	if (file != null) {
	    if (!file.exists ()) {
	        remove ();
	    } else {
	        lastmod = file.lastModified ();
	    }
	}
    }

    public File getFile () {
	return file;
    }

    public String getContent () {
	return content;
    }

    public String getSourceName () {
	return sourceName;
    }

    public void remove () {
	prototype.removeFunctionFile (this);
    }


    public String toString () {
	return sourceName;
    }


}


