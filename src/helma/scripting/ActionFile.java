// ActionFile.java
// Copyright (c) Helma.org 1998-2002

package helma.scripting;

import java.util.Vector;
import java.util.Iterator;
import java.io.*;
import helma.framework.*;
import helma.framework.core.*;
import helma.util.Updatable;


/**
 *  An ActionFile is a file containing function code that is exposed as a URI
 *  of objects of this class/type. It is
 *  usually represented by a file with extension .hac (hop action file)
 *  that contains the raw body of the function.
 */


public class ActionFile implements Updatable {

    String name, sourceName;
    Prototype prototype;
    Application app;
    File file;
    String content;
    long lastmod;


    public ActionFile (File file, String name, Prototype proto) {
	this.prototype = proto;
	this.app = proto.getApplication ();
	this.name = name;
	this.sourceName = file.getParentFile().getName()+"/"+file.getName();
	this.file = file;
	this.content = null;
    }

    public ActionFile (String content, String name, String sourceName, Prototype proto) {
	this.prototype = proto;
	this.app = proto.getApplication ();
	this.name = name;
	this.sourceName = sourceName;
	this.file = null;
	this.content = content;
    }


    /**
     * Tell the type manager whether we need an update. this is the case when
     * the file has been modified or deleted.
     */
    public boolean needsUpdate () {
	return lastmod != file.lastModified ();
    }


    public void update () {
	if (!file.exists ()) {
	    // remove functions declared by this from all object prototypes
	    remove ();
	} else {
	    lastmod = file.lastModified ();
	}
    }

    public void remove () {
	prototype.removeActionFile (this);
    }

    public File getFile () {
	return file;
    }

    public String getName () {
	return name;
    }

    public String getSourceName () {
	return sourceName;
    }

    public Reader getReader () throws FileNotFoundException {
	if (content != null)
	    return new StringReader (content);
	else if (file.length() == 0)
	    return new StringReader(";");
	else
	    return new FileReader (file);
    }

    public String getContent () {
	if (content != null)
	    return content;
	else {
	    try {
	        FileReader reader = new FileReader (file);
	        char cbuf[] = new char[(int) file.length ()];
	        reader.read (cbuf);
	        reader.close ();
	        return new String (cbuf);
	    } catch (Exception filex) {
	        app.logEvent ("Error reading "+this+": "+filex);
	        return null;
	    }
	}
    }

    public String getFunctionName () {
	return name + "_action";
    }

    public Prototype getPrototype () {
	return prototype;
    }

    public Application getApplication () {
	return app;
    }

    public String toString () {
	return "ActionFile["+sourceName+"]";
    }

}


