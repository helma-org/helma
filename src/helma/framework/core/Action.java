// Action.java
// Copyright (c) Hannes Wallnöfer 1998-2000
 
package helma.framework.core;

import java.util.*;
import java.io.*;
import helma.framework.*;
import helma.objectmodel.IServer;
import FESI.Data.*;


/**
 * An Action is a JavaScript function that is exposed as a URI. It is 
 * usually represented by a file with extension .hac (hop action file)
 * that contains the pure JavaScript body of the function. 
 */


public class Action {

    String name;
    String functionName;
    Prototype prototype;
    Application app;
    long lastmod;

    public Action (File file, String name, Prototype proto) {
	this.prototype = proto;
	this.app = proto.app;
	this.name = name;
	update (file);
    }


    public void update (File f) {

	long fmod = f.lastModified ();
	if (lastmod == fmod)
	    return;

	try {
	    FileReader reader = new FileReader (f);
	    char cbuf[] = new char[(int)f.length ()];
	    reader.read (cbuf);
	    reader.close ();
	    String content = new String (cbuf);
	    update (content);
	} catch (Exception filex) {
	    IServer.getLogger().log ("*** Error reading template file "+f+": "+filex);
	}
	lastmod = fmod;
    }

    

    public void update (String content) throws Exception {
	// IServer.getLogger().log ("Reading text template " + name);

	functionName = name+"_hop_action";

             try {
	    app.typemgr.readFunction (functionName, "arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10", content, prototype.getName ());
             } catch (Exception x) {
                 String message = x.getMessage ();
                 app.typemgr.generateErrorFeedback (functionName, message, prototype.getName ());
             }

   }


    public String getName () {
	return name;
    }

    public String getFunctionName () {
	return functionName;
    }

}
































