// FunctionFile.java
// Copyright (c) Hannes Wallnöfer 1998-2000
 
package helma.framework.core;

import java.util.*;
import java.io.*;
import helma.framework.*;
import helma.objectmodel.IServer;
import FESI.Data.ObjectPrototype;
import FESI.Exceptions.EcmaScriptException;
import FESI.Interpreter.*;


/**
 * This represents a File containing JavaScript functions for a given Object. 
 */


public class FunctionFile {

    String name;
    Prototype prototype;
    Application app;
    File file;
    long lastmod;

    public FunctionFile (File file, String name, Prototype proto) {
	this.prototype = proto;
	this.app = proto.app;
	this.name = name;
	this.file = file;
	update (file);
    }


    public void update (File f) {

	this.file = f;

	long fmod = file.lastModified ();
	if (lastmod == fmod)
	    return;

	lastmod = fmod;
	// app.typemgr.readFunctionFile (file, prototype.getName ());
    }

    public void updateRequestEvaluator (RequestEvaluator reval) {

        EvaluationSource es = new FileEvaluationSource(file.getPath(), null);
        FileReader fr = null;

        try {
            fr = new FileReader(file);
            ObjectPrototype op = reval.getPrototype (prototype.getName());
            reval.evaluator.evaluate(fr, op, es, false);
        } catch (IOException e) {
            IServer.getLogger().log ("Error parsing function file "+app.getName()+":"+prototype.getName()+"/"+file.getName()+": "+e);
        } catch (EcmaScriptException e) {
            IServer.getLogger().log ("Error parsing function file "+app.getName()+":"+prototype.getName()+"/"+file.getName()+": "+e);
        } finally {
            if (fr!=null) {
                try {
                    fr.close();
                } catch (IOException ignore) {}
            }
        }

    }


}







































