// FesiScriptingEnvironment.java
// Copyright (c) Hannes Wallnöfer 2002

package helma.scripting.fesi;

import helma.scripting.*;
import helma.scripting.fesi.extensions.*;
import helma.framework.*;
import helma.framework.core.*;
import helma.objectmodel.*;
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

    // the global object
    GlobalObject global;

    // caching table for JavaScript object wrappers
    LruHashtable wrappercache;

    // table containing JavaScript prototypes
    Hashtable prototypes;

    // the request evaluator instance owning this fesi evaluator
    RequestEvaluator reval;


    // extensions loaded by this evaluator
    static String[] extensions = new String[] {
	"FESI.Extensions.BasicIO",
	"FESI.Extensions.FileIO",
	"helma.xmlrpc.fesi.FesiRpcExtension",
	"helma.scripting.fesi.extensions.ImageExtension",
	"helma.scripting.fesi.extensions.FtpExtension",
	"FESI.Extensions.JavaAccess",
	"FESI.Extensions.OptionalRegExp"};

    public FesiEvaluator (Application app, RequestEvaluator reval) {
	this.app = app;
	this.reval = reval;
	wrappercache = new LruHashtable (100, .80f);
	prototypes = new Hashtable ();
	initEvaluator ();
	// initialized = false;
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
	    mailx.setProperties (app.getProperties ());
	    Database dbx = (Database) evaluator.addExtension ("helma.scripting.fesi.extensions.Database");
	    dbx.setApplication (app);

	    // fake a cache member like the one found in ESNodes
	    global.putHiddenProperty ("cache", new ESNode (new TransientNode ("cache"), this));
	    global.putHiddenProperty ("undefined", ESUndefined.theUndefined);
	    ESAppNode appnode = new ESAppNode (app.getAppNode (), this);
	    global.putHiddenProperty ("app", appnode);

	} catch (Exception e) {
	    System.err.println("Cannot initialize interpreter");
	    System.err.println("Error: " + e);
	    e.printStackTrace ();
	    throw new RuntimeException (e.getMessage ());
	}
    }

    /**
     * Invoke a function on some object, using the given arguments and global vars.
     */
    public Object invoke (Object thisObject, String functionName, Object[] args, HashMap globals) throws ScriptingException {
	ESObject eso = null;
	if (thisObject == null)
	    eso = global;
	else
	    eso = getElementWrapper (thisObject);
	try {
	    ESValue[] esv = args == null ? new ESValue[0] : new ESValue[args.length];
	    for (int i=0; i<esv.length; i++)
	        // for java.util.Map objects, we use the special "tight" wrapper
	        // that makes the Map look like a native object
	        if (args[i] instanceof Map)
	            esv[i] = new ESMapWrapper (this, (Map) args[i]);
	        else
	            esv[i] = ESLoader.normalizeValue (args[i], evaluator);
	    ESValue retval =  eso.doIndirectCall (evaluator, eso, functionName, esv);
	    return retval == null ? null : retval.toJavaObject ();
	} catch (Exception x) {
	    String msg = x.getMessage ();
	    if (msg == null || msg.length() < 10)
	        msg = x.toString ();
	    throw new ScriptingException (msg);
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
     *  Get a Script wrapper for an object. In contrast to getElementWrapper, this is called for
     * any Java object, not just the ones in the request path which we know are scripted.
     * So what we do is check if the object belongs to a scripted class. If so, we call getElementWrapper()
     * with the object, otherwise we return a generic unscripted object wrapper.
     */
    public ESValue getObjectWrapper (Object e) {
	if (app.getPrototypeName (e) != null)
	    return getElementWrapper (e);
	else
	    return new ESWrapper (e, evaluator);
    }

    /**
     *  Get a Script wrapper for any given object. If the object implements the IPathElement
     *  interface, the getPrototype method will be used to retrieve the name of the prototype
     * to use. Otherwise, a Java-Class-to-Script-Prototype mapping is consulted.
     */
    public ESObject getElementWrapper (Object e) {

	// check if e is an instance of a helma objectmodel node.
	if (e instanceof INode)
	    return getNodeWrapper ((INode) e);

	// Gotta find out the prototype name to use for this object...
	String prototypeName = app.getPrototypeName (e);

	ObjectPrototype op = getPrototype (prototypeName);

	if (op == null)
	    op = getPrototype ("hopobject");

	return new ESGenericObject (op, evaluator, e);
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


    /**
     *  Register a new Node wrapper with the wrapper cache. This is used by the
     * Node constructor.
     */
    public void putNodeWrapper (INode n, ESNode esn) {
	wrappercache.put (n, esn);
    }

    /**
     *  Get a scripting wrapper object for a user object. Active user objects are represented by
     *  the special ESUser wrapper class.
     */
    public ESNode getNodeWrapper (User u) {
        if (u == null)
            return null;

        ESUser esn = (ESUser) wrappercache.get (u);

        if (esn == null) {
            esn = new ESUser (u.getNode(), this, u);
            wrappercache.put (u, esn);
        } else {
            // the user node may have changed (login/logout) while the ESUser was
            // lingering in the cache.
            esn.updateNodeFromUser ();
        }

        return esn;
    }

    /**
     *  Return the RequestEvaluator owning and driving this FESI evaluator.
     */
    public RequestEvaluator getRequestEvaluator () {
	return reval;
    }

    /**
     *  Return the Response object of the current evaluation context. Proxy method to RequestEvaluator.
     */
    public ResponseTrans getResponse () {
	return reval.res;
    }

    /**
     *  Return the Request object of the current evaluation context. Proxy method to RequestEvaluator.
     */
    public RequestTrans getRequest () {
	return reval.req;
    }


}
