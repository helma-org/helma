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

    String name;
    String functionName;
    Prototype prototype;
    Application app;
    File file;
    String content;
    long lastmod;


    public ActionFile (File file, String name, Prototype proto) {
	this.prototype = proto;
	this.app = proto.getApplication ();
	this.name = name;
	functionName = getName()+"_action";
	this.file = file;
	this.content = null;
	if (file != null)
	    update ();
    }

    public ActionFile (String content, String name, Prototype proto) {
	this.prototype = proto;
	this.app = proto.getApplication ();
	this.name = name;
	functionName = getName()+"_action";
	this.file = null;
	this.content = content;
    }


    /**
     * Abstract method that must be implemented by subclasses to update evaluators with
     * new content of action file.
     */
    // protected abstract void update (String content) throws Exception;

    /**
     * Abstract method that must be implemented by subclasses to remove
     * action from evaluators.
     */
    // protected abstract void remove ();


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
	    try {
	        FileReader reader = new FileReader (file);
	        char cbuf[] = new char[(int) file.length ()];
	        reader.read (cbuf);
	        reader.close ();
	        content = new String (cbuf);
	        // update (content);
	    } catch (Exception filex) {
	        app.logEvent ("*** Error reading action file "+file+": "+filex);
	    }
	    lastmod = file.lastModified ();
	}
    }

    protected void remove () {
	prototype.removeAction (name);
	if (file != null)
	    prototype.removeUpdatable (file.getName());
    }

    public String getName () {
	return name;
    }

    public String getContent () {
	return content;
    }

    public String getFunctionName () {
	return functionName;
    }

    public Prototype getPrototype () {
	return prototype;
    }

    public Application getApplication () {
	return app;
    }

    public String toString () {
	return "ActionFile["+prototype.getName()+"/"+functionName+"]";
    }

}


