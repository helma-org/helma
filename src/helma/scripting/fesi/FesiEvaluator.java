// FesiScriptingEnvironment.java
// Copyright (c) Hannes Wallnöfer 2002

package helma.scripting.fesi;

import helma.scripting.*;
import helma.scripting.fesi.extensions.*;
import helma.framework.core.*;
import helma.objectmodel.INode;
import helma.objectmodel.db.DbMapping;
import java.util.*;
import java.io.File;
import FESI.Data.*;
import FESI.Interpreter.*;
import FESI.Exceptions.*;
import Acme.LruHashtable;

/**
 * This is the implementation of ScriptingEnvironment for the FESI EcmaScript interpreter.
 */

public class FesiEvaluator {

    // the application we're running in
    Application app;

    // The FESI evaluator
    Evaluator evaluator;

    // the globa object
    GlobalObject global;

    // caching table for JavaScript object wrappers
    LruHashtable wrappercache;

    // table containing JavaScript prototypes
    Hashtable prototypes;


    // extensions loaded by this evaluator
    static String[] extensions = new String[] {
	"FESI.Extensions.BasicIO",
	"FESI.Extensions.FileIO",
	"helma.xmlrpc.fesi.FesiRpcExtension",
	"helma.scripting.fesi.extensions.ImageExtension",
	"helma.scripting.fesi.extensions.FtpExtension",
	"FESI.Extensions.JavaAccess",
	"FESI.Extensions.OptionalRegExp"};

    public FesiEvaluator (Application app) {
	this.app = app;
	wrappercache = new LruHashtable (100, .80f);
	prototypes = new Hashtable ();
	initEvaluator ();
	initialized = false;
    }


    // init Script Evaluator
    private void initEvaluator () {
	try {
	    evaluator = new Evaluator();
	    // evaluator.reval = this;
	    global = evaluator.getGlobalObject();
	    for (int i=0; i<extensions.length; i++)
	        evaluator.addExtension (extensions[i]);
	    HopExtension hopx = new HopExtension (app);
	    hopx.initializeExtension (this);
	    MailExtension mailx = (MailExtension) evaluator.addExtension ("helma.scripting.fesi.extensions.MailExtension");
	    mailx.setProperties (this.app.props);
	    Database dbx = (Database) evaluator.addExtension ("helma.scripting.fesi.extensions.Database");
	    dbx.setApplication (app);

	    // fake a cache member like the one found in ESNodes
	    global.putHiddenProperty ("cache", new ESNode (new TransientNode ("cache"), this));
	    global.putHiddenProperty ("undefined", ESUndefined.theUndefined);
	    ESAppNode appnode = new ESAppNode (app.appnode, this);
	    global.putHiddenProperty ("app", appnode);
	    reqPath = new ArrayPrototype (evaluator.getArrayPrototype(), evaluator);
	    reqData = new ESMapWrapper (this);
	    resData = new ESMapWrapper (this);

	} catch (Exception e) {
	    System.err.println("Cannot initialize interpreter");
	    System.err.println("Error: " + e);
	    e.printStackTrace ();
	    throw new RuntimeException (e.getMessage ());
	}
    }

    /**
     *  Return the FESI Evaluator object wrapped by this object.
     */
    public Evaluator getEvaluator () {
	return evaluator;
    }

    /**
     * Return the application we're running in
     */
    public Application getApplication () {
	return app;
    }

    /**
     *  Get the object prototype for a prototype name
     */
    public ObjectPrototype getPrototype (String protoName) {
        if (protoName == null)
            return null;
        return (ObjectPrototype) prototypes.get (protoName);
    }

    /**
     * Register an object prototype for a certain prototype name.
     */
    public void putPrototype (String protoName, ObjectPrototype op) {
        if (protoName != null && op != null)
            prototypes.put (protoName, op);
    }

    /**
     *  Get a script wrapper for an implemntation of helma.objectmodel.INode
     */
    public ESNode getNodeWrapper (INode n) {

        if (n == null)
            return null;

        ESNode esn = (ESNode) wrappercache.get (n);
        if (esn == null || esn.getNode() != n) {
            String protoname = n.getPrototype ();
            ObjectPrototype op = null;

            // set the DbMapping of the node according to its prototype.
            // this *should* be done on the objectmodel level, but isn't currently
            // for embedded nodes since there's not enough type info at the objectmodel level
            // for those nodes.
            if (protoname != null && protoname.length() > 0 && n.getDbMapping () == null) {
                n.setDbMapping (app.getDbMapping (protoname));
            }

            op = getPrototype (protoname);

            // no prototype found for this node?
            if (op == null)
                op = getPrototype("hopobject");


            DbMapping dbm = n.getDbMapping ();
            if (dbm != null && dbm.isInstanceOf ("user"))
                esn = new ESUser (n, this, null);
            else
                esn = new ESNode (op, evaluator, n, this);

            wrappercache.put (n, esn);
            // app.logEvent ("Wrapper for "+n+" created");
        }

        return esn;
    }


}
