// FesiScriptingEnvironment.java
// Copyright (c) Hannes Wallnöfer 2002

package helma.scripting.fesi;

import helma.scripting.*;
import helma.scripting.fesi.extensions.*;
import helma.extensions.HelmaExtension;
import helma.extensions.ConfigurationException;
import helma.framework.*;
import helma.framework.core.*;
import helma.objectmodel.*;
import helma.objectmodel.db.DbMapping;
import helma.objectmodel.db.Relation;
import helma.main.Server;
import helma.util.Updatable;
import helma.util.CacheMap;
import java.util.*;
import java.io.*;
import FESI.Data.*;
import FESI.Interpreter.*;
import FESI.Exceptions.*;

/**
 * This is the implementation of ScriptingEnvironment for the FESI EcmaScript interpreter.
 */
public final class FesiEngine implements ScriptingEngine {

    // the application we're running in
    Application app;

    // The FESI evaluator
    Evaluator evaluator;

    // the global object
    GlobalObject global;

    // caching table for JavaScript object wrappers
    CacheMap wrappercache;

    // table containing JavaScript prototypes
    Hashtable prototypes;

    // the request evaluator instance owning this fesi evaluator
    RequestEvaluator reval;

    // extensions loaded by this evaluator
    static final String[] extensions = new String[] {
	"FESI.Extensions.BasicIO",
	"FESI.Extensions.FileIO",
	"helma.scripting.fesi.extensions.XmlRpcExtension",
	"helma.scripting.fesi.extensions.ImageExtension",
	"helma.scripting.fesi.extensions.FtpExtension",
	"FESI.Extensions.JavaAccess",
	"helma.scripting.fesi.extensions.DomExtension",
	"FESI.Extensions.OptionalRegExp"};

    // remember global variables from last invokation to be able to
    // do lazy cleanup
    Map lastGlobals = null;

    // the global vars set by extensions
    HashMap extensionGlobals;
    

    /**
     *  Zero argument constructor.
     */
    public FesiEngine () {}

    /**
     *  Initialize a FESI evaluator for the given application and request evaluator.
     */
    public void init (Application app, RequestEvaluator reval) {
	this.app = app;
	this.reval = reval;
	wrappercache = new CacheMap (200, .75f);
	prototypes = new Hashtable ();
	try {
	    evaluator = new Evaluator();
	    evaluator.engine = this;
	    global = evaluator.getGlobalObject();
	    for (int i=0; i<extensions.length; i++)
	        evaluator.addExtension (extensions[i]);
	    HopExtension hopx = new HopExtension (app);
	    hopx.initializeExtension (this);
	    MailExtension mailx = (MailExtension) evaluator.addExtension ("helma.scripting.fesi.extensions.MailExtension");
	    mailx.setProperties (app.getProperties ());
	    Database dbx = (Database) evaluator.addExtension ("helma.scripting.fesi.extensions.Database");
	    dbx.setApplication (app);
	    // load extensions defined in server.properties:
	    extensionGlobals = new HashMap ();
	    Vector extVec = Server.getServer ().getExtensions ();
	    for (int i=0; i<extVec.size(); i++ ) {
	        HelmaExtension ext = (HelmaExtension)extVec.get(i);
	        try {
	            HashMap tmpGlobals = ext.initScripting (app,this);
	            if (tmpGlobals!=null)
	                extensionGlobals.putAll(tmpGlobals);
	        } catch (ConfigurationException e) {
	            app.logEvent ("Couldn't initialize extension " + ext.getName () + ": " + e.getMessage ());
	        }
	    }

	    // fake a cache member like the one found in ESNodes
	    global.putHiddenProperty ("cache", new ESNode (new TransientNode ("cache"), this));
	    global.putHiddenProperty ("undefined", ESUndefined.theUndefined);
	    ESBeanWrapper appnode = new ESBeanWrapper (new ApplicationBean (app), this);
	    global.putHiddenProperty ("app", appnode);
	    initialize();
	} catch (Exception e) {
	    System.err.println("Cannot initialize interpreter");
	    System.err.println("Error: " + e);
	    e.printStackTrace ();
	    throw new RuntimeException (e.getMessage ());
	}
    }

    /**
     *  Initialize the evaluator, making sure the minimum type information
     *  necessary to bootstrap the rest is parsed.
     */
    private void initialize () {
	Collection protos = app.getPrototypes();
	for (Iterator i=protos.iterator(); i.hasNext(); ) {
	    Prototype proto = (Prototype) i.next ();
	    initPrototype (proto);
	}
	// always fully initialize global prototype, because
	// we always need it and there's no chance to trigger
	// creation on demand.
	getPrototype ("global");
    }

    /**
     *   Initialize a prototype without fully parsing its script files.
     */
    void initPrototype (Prototype prototype) {
        // System.err.println ("FESI INIT PROTO "+prototype);
	ObjectPrototype op = null;

	// get the prototype's prototype if possible and necessary
	ObjectPrototype opp = null;
	Prototype parent = prototype.getParentPrototype ();
	if (parent != null) {
	    // see if parent prototype is already registered. if not, register it
	    opp = getRawPrototype (parent.getName ());
	    if (opp == null) {
	        initPrototype (parent);
	        opp = getRawPrototype (parent.getName ());
	    }
	}
	String name = prototype.getName ();
	if (!"global".equalsIgnoreCase (name) &&
	    !"hopobject".equalsIgnoreCase (name) &&
	    opp == null)
	{
	    if (app.isJavaPrototype (name))
	        opp = getRawPrototype ("__javaobject__");
	    else
	        opp = getRawPrototype ("hopobject");
	}

	// if prototype doesn't exist (i.e. is a standard prototype built by HopExtension), create it.
	op = getRawPrototype (name);
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

	// Register a constructor for all types except global.
	// This will first create a new prototyped hopobject and then calls
	// the actual (scripted) constructor on it.
	if (!"global".equalsIgnoreCase (name) && !"root".equalsIgnoreCase (name)) {
             try {
	        FunctionPrototype fp = (FunctionPrototype) evaluator.getFunctionPrototype();
	        global.putHiddenProperty (name, new NodeConstructor (name, fp, this));
	    } catch (EcmaScriptException ignore) {}
	}
	// app.typemgr.updatePrototype (prototype.getName());
	// evaluatePrototype (prototype);
    }

    /**
     *   Set up a prototype, parsing and compiling all its script files.
     */
    void evaluatePrototype (Prototype prototype) {
        // System.err.println ("FESI EVALUATE PROTO "+prototype+" FOR "+this);
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
	    // reset prototype to original state
	    resetPrototype (op);
	    // set parent prototype just in case it has been changed
	    op.setPrototype (opp);
	}

	// Register a constructor for all types except global.
	// This will first create a new prototyped hopobject and then calls 
                  // the actual (scripted) constructor on it.
	if (!"global".equalsIgnoreCase (name) && !"root".equalsIgnoreCase (name)) {
	    try {
	        FunctionPrototype fp = (FunctionPrototype) evaluator.getFunctionPrototype();
	        global.putHiddenProperty (name, new NodeConstructor (name, fp, this));
	    } catch (EcmaScriptException ignore) {}
	}
	for (Iterator it = prototype.getZippedCode().values().iterator(); it.hasNext(); ) {
	    Object code = it.next();
	    evaluate (prototype, code);
	}
	for (Iterator it = prototype.getCode().values().iterator(); it.hasNext(); ) {
	    Object code = it.next();
	    evaluate (prototype, code);
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
	        if (esv instanceof ConstructedFunctionObject || esv instanceof FesiActionAdapter.ThrowException)
	            op.deleteProperty (prop, prop.hashCode());
	    } catch (Exception x) {}
	}
    }

    /** 
     *  This method is called before an execution context is entered to let the 
     *  engine know it should update its prototype information.
     */
    public void updatePrototypes () {
	Collection protos = app.getPrototypes();
	for (Iterator i=protos.iterator(); i.hasNext(); ) {
	    Prototype proto = (Prototype) i.next ();
	    TypeInfo info = (TypeInfo) prototypes.get (proto.getName());
	    if (info == null) {
	        // a prototype we don't know anything about yet. Init local update info.
	        initPrototype (proto);
	        info = (TypeInfo) prototypes.get (proto.getName());
	    }
	    // only update prototype if it has already been initialized.
	    // otherwise, this will be done on demand
	    // System.err.println ("CHECKING PROTO: "+info);
	    if (info.lastUpdate > 0) {
	        Prototype p = app.typemgr.getPrototype (info.protoName);
	        if (p != null) {
	            // System.err.println ("UPDATING PROTO: "+p);
	            app.typemgr.updatePrototype(p);
	            if (p.getLastUpdate () > info.lastUpdate) {
	                evaluatePrototype(p);
	                info.lastUpdate = p.getLastUpdate ();
	            }
	        }
	    }
	}
    }

    /**
     *  This method is called when an execution context for a request
     *  evaluation is entered. The globals parameter contains the global values
     *  to be applied during this execution context.
     */
    public void enterContext (HashMap globals) throws ScriptingException {
	// set the thread filed in the FESI evaluator
	evaluator.thread = Thread.currentThread ();
	// set globals on the global object
	globals.putAll(extensionGlobals);
	if (globals != null && globals != lastGlobals) {
	    // loop through global vars and set them
	    for (Iterator i=globals.keySet().iterator(); i.hasNext(); ) {
	        String k = (String) i.next();
	        Object v = globals.get (k);
	        ESValue sv = null;
	        try {
	            // we do a lot of extra work to make access to global variables
	            // comfortable to EcmaScript coders, i.e. we use a lot of custom wrappers
	            // that expose properties and functions in a special way instead of just going
	            // with the standard java object wrappers.
	            if (v instanceof Map) {
	                sv = new ESMapWrapper (this, (Map) v);
	            } else if ("path".equals (k)) {
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
	            } else if ("req".equals (k)) {
	                sv = new ESBeanWrapper (v, this);
	            } else if ("res".equals (k)) {
	                sv = new ESBeanWrapper (v, this);
	            } else if ("session".equals (k)) {
	                sv = new ESBeanWrapper (v, this);
	            } else if ("app".equals (k)) {
	                sv = new ESBeanWrapper (v, this);
	            } else if (v instanceof ESValue) {
	                sv = (ESValue)v;
	            } else {
	                sv = ESLoader.normalizeValue (v, evaluator);
	            }
	            global.putHiddenProperty (k, sv);
	        } catch (Exception x) {
	            app.logEvent ("Error setting global variable "+k+": "+x);
	        }
	    }
	}
	// remember the globals set on this evaluator
	lastGlobals = globals;
    }

    /**
     *   This method is called to let the scripting engine know that the current
     *   execution context has terminated.
     */
    public void exitContext () {
	// unset the thread filed in the FESI evaluator
	evaluator.thread = null;
	// loop through previous globals and unset them, if necessary.
	if (lastGlobals != null) {
	    for (Iterator i=lastGlobals.keySet().iterator(); i.hasNext(); ) {
	        String g = (String) i.next ();
	        try {
	            global.deleteProperty (g, g.hashCode());
	        } catch (Exception x) {
	            System.err.println ("Error resetting global property: "+g);
	        }
	    }
	    lastGlobals = null;
	}
    }


    /**
     * Invoke a function on some object, using the given arguments and global vars.
     */
    public Object invoke (Object thisObject, String functionName, Object[] args, boolean xmlrpc) throws ScriptingException {
	ESObject eso = null;
	if (thisObject == null)
	    eso = global;
	else
	    eso = getElementWrapper (thisObject);
	try {
	    ESValue[] esv = args == null ? new ESValue[0] : new ESValue[args.length];
	    for (int i=0; i<esv.length; i++) {
	        // XML-RPC requires special argument conversion
	        if (xmlrpc)
	            esv[i] = processXmlRpcArgument (args[i], evaluator);
	        // for java.util.Map objects, we use the special "tight" wrapper
	        // that makes the Map look like a native object
	        else if (args[i] instanceof Map)
	            esv[i] = new ESMapWrapper (this, (Map) args[i]);
	        else
	            esv[i] = ESLoader.normalizeValue (args[i], evaluator);
	    }
	    ESValue retval =  eso.doIndirectCall (evaluator, eso, functionName, esv);
	    if (xmlrpc)
	        return processXmlRpcResponse (retval);
	    else if (retval == null)
	        return null;
	    else
	         return retval.toJavaObject ();
	} catch (RedirectException redirect) {
	    throw redirect;
	} catch (TimeoutException timeout) {
	    throw timeout;
	} catch (ConcurrencyException concur) {
	    throw concur;
	} catch (Exception x) {
	    // check if this is a redirect exception, which has been converted by fesi
	    // into an EcmaScript exception, which is why we can't explicitly catch it
	    if (reval.res.getRedirect() != null)
	        throw new RedirectException (reval.res.getRedirect ());
	    // do the same for not-modified responses
	    if (reval.res.getNotModified())
	        throw new RedirectException (null);
	    // has the request timed out? If so, throw TimeoutException
	    if (evaluator.thread != Thread.currentThread())
	        throw new TimeoutException ();
	    if (app.debug ()) {
	        x.printStackTrace ();
	    }
	    // create and throw a ScriptingException wrapping the original exception
	    throw new ScriptingException (x);
	}
    }

    /**
     *  Let the evaluator know that the current evaluation has been
     *  aborted. This is done by setting the thread ref in the evaluator
     * object to null.
     */
    public void abort () {
	// unset the thread filed in the FESI evaluator
	evaluator.thread = null;
    }


    /**
     * Check if an object has a function property (public method if it
     * is a java object) with that name.
     */
    public boolean hasFunction (Object obj, String fname) {
	// System.err.println ("HAS_FUNC: "+fname);
	String protoname = app.getPrototypeName (obj);
	try {
	    ObjectPrototype op = getPrototype (protoname);
	    // if this is an untyped object return false
	    if (op == null)
	        return false;
	    ESValue func = op.getProperty (fname, fname.hashCode());
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
    public Object get (Object obj, String propname) {
	if (obj == null || propname == null)
	    return null;

	String prototypeName = app.getPrototypeName (obj);
	if ("user".equalsIgnoreCase (prototypeName) &&
	    "password".equalsIgnoreCase (propname))
	    throw new RuntimeException ("access to password property not allowed");

                  // if this is a HopObject, check if the property is defined
	// in the type.properties db-mapping.
	if (obj instanceof INode) {
	    DbMapping dbm = app.getDbMapping (prototypeName);
	    if (dbm != null) {
	        Relation rel = dbm.propertyToRelation (propname);
	        if (rel == null || !rel.isPrimitive ())
	            throw new RuntimeException ("\""+propname+"\" is not defined in "+prototypeName);
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
     *  Convert an input argument from Java to the scripting runtime
     *  representation.
     */
    public static ESValue processXmlRpcArgument (Object what, Evaluator evaluator) throws Exception {
	if (what == null)
	   return ESNull.theNull;
	if (what instanceof Vector) {
	    Vector v = (Vector) what;
	    ArrayPrototype retval = new ArrayPrototype (evaluator.getArrayPrototype (), evaluator);
	    int l = v.size ();
	    for (int i=0; i<l; i++)
	        retval.putProperty (i, processXmlRpcArgument (v.elementAt (i), evaluator));
	    return retval;
	}
	if (what instanceof Hashtable) {
	    Hashtable t = (Hashtable) what;
	    ESObject retval = new ObjectPrototype (evaluator.getObjectPrototype (), evaluator);
	    for (Enumeration e=t.keys(); e.hasMoreElements(); ) {
	        String next = (String) e.nextElement ();
	        retval.putProperty (next, processXmlRpcArgument (t.get (next), evaluator), next.hashCode ());
	    }
	    return retval;
	}
	if (what instanceof String)
	   return new ESString (what.toString ());
	if (what instanceof Number)
	   return new ESNumber (new Double (what.toString ()).doubleValue ());
	if (what instanceof Boolean)
	   return ESBoolean.makeBoolean (((Boolean) what).booleanValue ());
	if (what instanceof Date)
	   return new DatePrototype (evaluator, (Date) what);
	return ESLoader.normalizeValue (what, evaluator);
    }


    /**
     * convert a JavaScript Object object to a generic Java object stucture.
     */
    public static Object processXmlRpcResponse (ESValue what) throws EcmaScriptException {
	if (what == null || what instanceof ESNull)
	    return null;
	if (what instanceof ArrayPrototype) {
	    ArrayPrototype a = (ArrayPrototype) what;
	    int l = a.size ();
	    Vector v = new Vector ();
	    for (int i=0; i<l; i++) {
	        Object nj = processXmlRpcResponse (a.getProperty (i));
	        v.addElement (nj);
	    }
	    return v;
	}
	if (what instanceof ObjectPrototype) {
	    ObjectPrototype o = (ObjectPrototype) what;
	    Hashtable t = new Hashtable ();
	    for (Enumeration e=o.getProperties (); e.hasMoreElements (); ) {
	        String next = (String) e.nextElement ();
	        // We don't do deep serialization of HopObjects to avoid
	        // that the whole web site structure is sucked out with one
	        // object. Instead we return some kind of "proxy" objects
	        // that only contain the prototype and id of the HopObject property.
	        Object nj = null;
	        ESValue esv = o.getProperty (next, next.hashCode ());
	        if (esv instanceof ESNode) {
	            INode node = ((ESNode) esv).getNode ();
	            if (node != null) {
	                Hashtable nt = new Hashtable ();
	                nt.put ("id", node.getID());
	                if (node.getPrototype() != null)
	                    nt.put ("prototype", node.getPrototype ());
	                nj = nt;
	            }
	        } else
	            nj = processXmlRpcResponse (esv);
	        if (nj != null)  // can't put null as value in hashtable
	            t.put (next, nj);
	    }
	    return t;
	}
	if (what instanceof ESUndefined || what instanceof ESNull)
	    return null;
	Object jval = what.toJavaObject ();
	if (jval instanceof Byte || jval instanceof Short)
	    jval = new Integer (jval.toString ());
	return jval;
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
     * Get a raw prototype, i.e. in potentially unfinished state
     * without checking if it needs to be updated.
    */
    private ObjectPrototype getRawPrototype (String protoName) {
        if (protoName == null)
            return null;
        TypeInfo info = (TypeInfo) prototypes.get (protoName);
        return info == null? null : info.objectPrototype;
    }

    /**
     *  Get the object prototype for a prototype name and initialize/update it
     *  if necessary.
     */
    public ObjectPrototype getPrototype (String protoName) {
        if (protoName == null)
            return null;
        TypeInfo info = (TypeInfo) prototypes.get (protoName);
        if (info != null && info.lastUpdate == 0) {
            Prototype p = app.typemgr.getPrototype (protoName);
            if (p != null) {
                app.typemgr.updatePrototype(p);
                if (p.getLastUpdate () > info.lastUpdate) {
                    info.lastUpdate = p.getLastUpdate ();
                    evaluatePrototype(p);
                }
	        // set info.lastUpdate to 1 if it is 0 so we know we 
	        // have initialized this prototype already, even if 
	        // it is empty (i.e. doesn't contain any scripts/skins/actoins
	        if (info.lastUpdate == 0)
	            info.lastUpdate = 1;
            }
        }
        return info == null? null : info.objectPrototype;
    }

    /**
     * Register an object prototype for a certain prototype name.
     */
    public void putPrototype (String protoName, ObjectPrototype op) {
        if (protoName != null && op != null)
            prototypes.put (protoName, new TypeInfo (op, protoName));
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
	/* else if (e instanceof Map)
	    return new ESMapWrapper (this, (Map) e); */
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
     *  Get a script wrapper for an instance of helma.objectmodel.INode
     */
    public ESNode getNodeWrapper (INode n) {
        // FIXME: should this return ESNull.theNull?
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

    private synchronized void evaluate (Prototype prototype, Object code) {
	if (code instanceof FunctionFile) {
	    FunctionFile funcfile = (FunctionFile) code;
	    File file = funcfile.getFile ();
	    if (file != null) {
	        try {
	            FileReader fr = new FileReader (file);
	            EvaluationSource es = new FileEvaluationSource (funcfile.getSourceName(), null);
	            updateEvaluator (prototype, fr, es);
	        } catch (IOException iox) {
	            app.logEvent ("Error updating function file: "+iox);
	        }
	    } else {
	        StringReader reader = new StringReader (funcfile.getContent());
	        EvaluationSource es = new FileEvaluationSource (funcfile.getSourceName(), null);
	        updateEvaluator (prototype, reader, es);
	    }
	} else if (code instanceof ActionFile) {
	    ActionFile action = (ActionFile) code;
	    FesiActionAdapter fa = new FesiActionAdapter (action);
	    try {
	        fa.updateEvaluator (this);
	    } catch (EcmaScriptException esx) {
	        app.logEvent ("Error parsing "+action+": "+esx);
	    }
	}
    }


    private  synchronized void updateEvaluator (Prototype prototype, Reader reader, EvaluationSource source) {
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


    class TypeInfo {

        ObjectPrototype objectPrototype;
        long lastUpdate = 0;
        String protoName;

        public TypeInfo (ObjectPrototype op, String name) {
            objectPrototype = op;
            protoName = name;
        }

        public String toString () {
            return ("TypeInfo["+protoName+","+new Date(lastUpdate)+"]");
        }

    }

}
