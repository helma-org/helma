// FesiScriptingEnvironment.java
// Copyright (c) Hannes Wallnöfer 2002

package helma.scripting.fesi;

import helma.scripting.*;
import helma.scripting.fesi.extensions.*;
import helma.framework.*;
import helma.framework.core.*;
import helma.objectmodel.*;
import helma.objectmodel.db.DbMapping;
import helma.objectmodel.db.Relation;
import helma.util.Updatable;
import java.util.*;
import java.io.*;
import FESI.Data.*;
import FESI.Interpreter.*;
import FESI.Exceptions.*;
import Acme.LruHashtable;

/**
 * This is the implementation of ScriptingEnvironment for the FESI EcmaScript interpreter.
 */

public final class FesiEvaluator {

    // the application we're running in
    public Application app;

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
	"helma.scripting.fesi.extensions.DomExtension",
	"FESI.Extensions.OptionalRegExp"};

    // remember global variables from last invokation to be able to
    // do lazy cleanup
    Map lastGlobals = null;

    public FesiEvaluator (Application app, RequestEvaluator reval) {
	this.app = app;
	this.reval = reval;
	wrappercache = new LruHashtable (100, .80f);
	prototypes = new Hashtable ();
	try {
	    evaluator = new Evaluator();
	    evaluator.reval = this;
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
	    initialize();
	} catch (Exception e) {
	    System.err.println("Cannot initialize interpreter");
	    System.err.println("Error: " + e);
	    e.printStackTrace ();
	    throw new RuntimeException (e.getMessage ());
	}
    }

    private void initialize () {
	Collection prototypes = app.getPrototypes();
	for (Iterator i=prototypes.iterator(); i.hasNext(); ) {
	    Prototype proto = (Prototype) i.next ();
	    evaluatePrototype (proto);
	}
    }

    void evaluatePrototype (Prototype prototype) {

	ObjectPrototype op = null;

	// get the prototype's prototype if possible and necessary
	ObjectPrototype opp = null;
	Prototype parent = prototype.getParentPrototype ();
	if (parent != null) {
	    // see if parent prototype is already registered. if not, register it
	    opp = getPrototype (parent.getName ());
	    if (opp == null) {
	        evaluatePrototype (parent);
	        opp = getPrototype (parent.getName ());
	    }
	}
	String name = prototype.getName ();
	if (!"global".equalsIgnoreCase (name) && !"hopobject".equalsIgnoreCase (name) && opp == null) {
	    if (app.isJavaPrototype (name))
	        opp = getPrototype ("__javaobject__");
	    else
	        opp = getPrototype ("hopobject");
	}

	// if prototype doesn't exist (i.e. is a standard prototype built by HopExtension), create it.
	op = getPrototype (name);
	if (op == null) {
	    op = new ObjectPrototype (opp, evaluator);
	    try {
	        op.putProperty ("prototypename", new ESString (name), "prototypename".hashCode ());
	    } catch (EcmaScriptException ignore) {}
	    putPrototype (name, op);
	} else {
	    // set parent prototype just in case it has been changed
	    op.setPrototype (opp);
	}

	resetPrototype (op);

	// Register a constructor for all types except global.
	// This will first create a node and then call the actual (scripted) constructor on it.
	if (!"global".equalsIgnoreCase (name)) {
	    try {
	        FunctionPrototype fp = (FunctionPrototype) evaluator.getFunctionPrototype();
	        evaluator.getGlobalObject().putHiddenProperty (name, new NodeConstructor (name, fp, this));
	    } catch (EcmaScriptException ignore) {}
	}
	for (Iterator it = prototype.functions.values().iterator(); it.hasNext(); ) {
	    FunctionFile ff = (FunctionFile) it.next ();
	    if (ff.hasFile ())
	        evaluateFile (prototype, ff.getFile ());
	    else
	        evaluateString (prototype, ff.getContent ());
	}
	for (Iterator it = prototype.templates.values().iterator(); it.hasNext(); ) {
	    Template tmp = (Template) it.next ();
	    try {
	        FesiActionAdapter adp = new FesiActionAdapter (tmp);
	        adp.updateEvaluator (this);
	    } catch (EcmaScriptException ignore) {}
	}
	for (Iterator it = prototype.actions.values().iterator(); it.hasNext(); ) {
	    ActionFile act = (ActionFile) it.next ();
	    try {
	        FesiActionAdapter adp = new FesiActionAdapter (act);
	        adp.updateEvaluator (this);
	    } catch (EcmaScriptException ignore) {}
	}
    }

    /**
     *  Return an object prototype to its initial state, removing all application specific
     *  functions.
     */
    void resetPrototype (ObjectPrototype op) {
	for (Enumeration en = op.getAllProperties(); en.hasMoreElements(); ) {
	    String prop = en.nextElement ().toString ();
	    try {
	        ESValue esv = op.getProperty (prop, prop.hashCode ());
	        // System.err.println (protoname+"."+obj+"   ->   "+esv.getClass());
	        if (esv instanceof ConstructedFunctionObject || esv instanceof FesiActionAdapter.ThrowException)
	            op.deleteProperty (prop, prop.hashCode());
	    } catch (Exception x) {}
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

	GlobalObject global = evaluator.getGlobalObject ();

	// if we are provided with global variables to set for this invocation,
	// remember the global variables before invocation to be able to reset them afterwards.
	Set globalVariables = null;
	try {
	    ESValue[] esv = args == null ? new ESValue[0] : new ESValue[args.length];
	    for (int i=0; i<esv.length; i++) {
	        // for java.util.Map objects, we use the special "tight" wrapper
	        // that makes the Map look like a native object
	        if (args[i] instanceof Map)
	            esv[i] = new ESMapWrapper (this, (Map) args[i]);
	        else
	            esv[i] = ESLoader.normalizeValue (args[i], evaluator);
	    }

	    if (globals != null && globals != lastGlobals) {
	        // remember all global variables before invocation
	        Set tmpGlobal = new HashSet ();
	        for (Enumeration en = global.getAllProperties(); en.hasMoreElements(); ) {
	            tmpGlobal.add (en.nextElement ().toString ());
	        }
	        globalVariables = tmpGlobal;

	        // loop through global vars and set them
	        for (Iterator i=globals.keySet().iterator(); i.hasNext(); ) {
	            String k = (String) i.next();
	            Object v = globals.get (k);
	            ESValue sv = null;
	            // we do a lot of extra work to make access to global variables
	            // comfortable to EcmaScript coders, i.e. we use a lot of custom wrappers
	            // that expose properties and functions in a special way instead of just going
	            // with the standard java object wrappers.
	            if (v instanceof RequestTrans)
	                ((RequestTrans) v).data = new ESMapWrapper (this, ((RequestTrans) v).getRequestData ());
	            else if (v instanceof ResponseTrans)
	                ((ResponseTrans) v).data = new ESMapWrapper (this, ((ResponseTrans) v).getResponseData ());
	            if (v instanceof Map)
	                sv = new ESMapWrapper (this, (Map) v);
	            else if ("path".equals (k)) {
	                ArrayPrototype parr = new ArrayPrototype (evaluator.getArrayPrototype(), evaluator);
	                List path = (List) v;
	                // register path elements with their prototype
	                for (int j=0; j<path.size(); j++) {
	                    Object pathElem = path.get (j);
	                    ESValue wrappedElement = getElementWrapper (pathElem);
	                    parr.putProperty (j, wrappedElement);
	                    String protoname = app.getPrototypeName (pathElem);
	                    if (protoname != null)
	                        parr.putHiddenProperty (protoname, wrappedElement);
	                }
	                sv = parr;
	            } else if ("user".equals (k)) {
	                sv = getNodeWrapper ((User) v);
	            } else if ("app".equals (k)) {
	                sv = new ESAppNode ((INode) v, this);
	            }
	            else
	                sv = ESLoader.normalizeValue (v, evaluator);
	            global.putHiddenProperty (k, sv);
	        }
	        // remember the globals set on this evaluator
	        // lastGlobals = globals;
	    }
	    evaluator.thread = Thread.currentThread ();
	    ESValue retval =  eso.doIndirectCall (evaluator, eso, functionName, esv);
	    return retval == null ? null : retval.toJavaObject ();
	} catch (Exception x) {
	    // check if this is a redirect exception, which has been converted by fesi
	    // into an EcmaScript exception, which is why we can't explicitly catch it
	    if (reval.res.getRedirect() != null)
	        throw new RedirectException (reval.res.getRedirect ());
	    // create and throw a ScriptingException with the right message
	    String msg = x.getMessage ();
	    if (msg == null || msg.length() < 10)
	        msg = x.toString ();
	    if (app.debug ()) {
	        System.err.println ("Error in Script: "+msg);
	        x.printStackTrace ();
	    }
	    throw new ScriptingException (msg);
	} finally {
	    // remove global variables that have been added during invocation.
	    // this are typically undeclared variables, and we don't want them to
	    // endure from one request to the next since this leads to buggy code that
	    // relies on requests being served by the same evaluator, which is typically the
	    // case under development conditions but not in deployment.
	    if (globalVariables != null) {
	        for (Enumeration en = global.getAllProperties(); en.hasMoreElements(); ) {
	            String g = en.nextElement ().toString ();
	            try {
	                if (!globalVariables.contains (g) &&
				!(global.getProperty (g, g.hashCode()) instanceof BuiltinFunctionObject))
	                    global.deleteProperty (g, g.hashCode());
	            } catch (Exception x) {
	                System.err.println ("Error resetting global property: "+g);
	            }
	        }
	    }
	}
    }


    /**
     * Check if an object has a function property (public method if it
     * is a java object) with that name.
     */
    public boolean hasFunction (Object obj, String fname) {
	ESObject eso = null;
	if (obj == null)
	    eso = evaluator.getGlobalObject ();
	else
	    eso = getElementWrapper (obj);
	try {
	    ESValue func = eso.getProperty (fname, fname.hashCode());
	    if (func != null && func instanceof FunctionPrototype)
	        return true;
	} catch (EcmaScriptException esx) {
	    // System.err.println ("Error in hasFunction: "+esx);
	    return false;
	}
	return false;
    }


    /**
     * Check if an object has a defined property (public field if it
     * is a java object) with that name.
     */
    public Object getProperty (Object obj, String propname) {
	if (obj == null || propname == null)
	    return null;

	String prototypeName = app.getPrototypeName (obj);
	if ("user".equalsIgnoreCase (prototypeName) &&
		"password".equalsIgnoreCase (propname))
	    return "[macro access to password property not allowed]";

	// if this is a HopObject, check if the property is defined
	// in the type.properties db-mapping.
	if (obj instanceof INode) {
	    DbMapping dbm = app.getDbMapping (prototypeName);
	    if (dbm != null) {
	        Relation rel = dbm.propertyToRelation (propname);
	        if (rel == null || !rel.isPrimitive ())
	            return "[property \""+propname+"\" is not defined for "+prototypeName+"]";
	    }
	}

	ESObject eso = getElementWrapper (obj);
	try {
	    ESValue prop = eso.getProperty (propname, propname.hashCode());
	    if (prop != null && !(prop instanceof ESNull) &&
	                    !(prop instanceof ESUndefined))
	        return prop.toJavaObject ();
	} catch (EcmaScriptException esx) {
	    // System.err.println ("Error in getProperty: "+esx);
	    return null;
	}
	return null;
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
	/* else if (e instanceof INode)
	    return new ESNode ((INode) e, this); */
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

    public  synchronized void evaluateFile (Prototype prototype, File file) {
	try {
	    FileReader fr = new FileReader (file);
	    EvaluationSource es = new FileEvaluationSource (file.getPath (), null);
	    updateEvaluator (prototype, fr, es);
	} catch (IOException iox) {
	    app.logEvent ("Error updating function file: "+iox);
	}
    }

    public synchronized void evaluateString (Prototype prototype, String code) {
	StringReader reader = new StringReader (code);
	StringEvaluationSource es = new StringEvaluationSource (code, null);
	updateEvaluator (prototype, reader, es);
    }

    public  synchronized void updateEvaluator (Prototype prototype, Reader reader, EvaluationSource source) {

        try {

            ObjectPrototype op = getPrototype (prototype.getName());

            // do the update, evaluating the file
            evaluator.evaluate(reader, op, source, false);

        } catch (Throwable e) {
            app.logEvent ("Error parsing function file "+source+": "+e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignore) {}
            }
        }
    }


}
