// HopExtension.java
// Copyright (c) Hannes Wallnöfer 1998-2000

package helma.scripting.fesi;

import helma.framework.core.*;
import helma.objectmodel.*;
import helma.util.*;
import helma.framework.IPathElement;
import FESI.Interpreter.*;
import FESI.Exceptions.*;
import FESI.Extensions.*;
import FESI.Data.*;
import java.io.*;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.TimeZone;
import java.util.Date;
import java.text.*;
import org.xml.sax.InputSource;

/** 
 * This is the basic Extension for FESI interpreters used in HOP. It sets up 
 * varios constructors, global functions and properties etc. 
 */
 
public class HopExtension {

    protected Application app;
    protected RequestEvaluator reval;

    public HopExtension () {
        super();
    }


    /**
     * Called by the evaluator after the extension is loaded.
     */
    public void initializeExtension (RequestEvaluator reval) throws EcmaScriptException {

        this.reval = reval;
        this.app = reval.app;
        Evaluator evaluator = reval.evaluator;
        GlobalObject go = evaluator.getGlobalObject();
        FunctionPrototype fp = (FunctionPrototype) evaluator.getFunctionPrototype();

        ESObject op = evaluator.getObjectPrototype();

        ////////// The editor functions for String, Boolean and Number are deprecated!
        ESObject sp = evaluator.getStringPrototype ();
        sp.putHiddenProperty ("editor", new StringEditor ("editor", evaluator, fp));
        ESObject np = evaluator.getNumberPrototype ();
        np.putHiddenProperty ("editor", new NumberEditor ("editor", evaluator, fp));
        np.putHiddenProperty ("format", new NumberPrototypeFormat ("format", evaluator, fp));
        ESObject bp = evaluator.getBooleanPrototype ();
        bp.putHiddenProperty ("editor", new BooleanEditor ("editor", evaluator, fp));
        ESObject dp = evaluator.getDatePrototype ();
        dp.putHiddenProperty ("format", new DatePrototypeFormat ("format", evaluator, fp));

        reval.esNodePrototype = new ObjectPrototype(op, evaluator);  // the Node prototype

        reval.esUserPrototype = new ObjectPrototype (reval.esNodePrototype, evaluator);  // the User prototype

        ESObject node = new NodeConstructor ("Node", fp, reval); // the Node constructor

        // register the default methods of Node objects in the Node prototype
        reval.esNodePrototype.putHiddenProperty ("add", new NodeAdd ("add", evaluator, fp));
        reval.esNodePrototype.putHiddenProperty ("addAt", new NodeAddAt ("addAt", evaluator, fp));
        reval.esNodePrototype.putHiddenProperty ("remove", new NodeRemove ("remove", evaluator, fp));
        reval.esNodePrototype.putHiddenProperty ("link", new NodeLink ("link", evaluator, fp));
        reval.esNodePrototype.putHiddenProperty ("list", new NodeList ("list", evaluator, fp));
        reval.esNodePrototype.putHiddenProperty ("set", new NodeSet ("set", evaluator, fp));
        reval.esNodePrototype.putHiddenProperty ("get", new NodeGet ("get", evaluator, fp));
        reval.esNodePrototype.putHiddenProperty ("count", new NodeCount ("count", evaluator, fp));
        reval.esNodePrototype.putHiddenProperty ("contains", new NodeContains ("contains", evaluator, fp));
        reval.esNodePrototype.putHiddenProperty ("size", new NodeCount ("size", evaluator, fp));
        reval.esNodePrototype.putHiddenProperty ("editor", new NodeEditor ("editor", evaluator, fp));
        reval.esNodePrototype.putHiddenProperty ("chooser", new NodeChooser ("chooser", evaluator, fp));
        reval.esNodePrototype.putHiddenProperty ("multiChooser", new MultiNodeChooser ("multiChooser", evaluator, fp));
        reval.esNodePrototype.putHiddenProperty ("path", new NodeHref ("path", evaluator, fp));
        reval.esNodePrototype.putHiddenProperty ("href", new NodeHref ("href", evaluator, fp));
        reval.esNodePrototype.putHiddenProperty ("setParent", new NodeSetParent ("setParent", evaluator, fp));
        reval.esNodePrototype.putHiddenProperty ("invalidate", new NodeInvalidate ("invalidate", evaluator, fp));
        reval.esNodePrototype.putHiddenProperty("renderSkin", new RenderSkin ("renderSkin", evaluator, fp, false, false));
        reval.esNodePrototype.putHiddenProperty("renderSkinAsString", new RenderSkin ("renderSkinAsString", evaluator, fp, false, true));

        // methods that give access to properties and global user lists
        go.putHiddenProperty("Node", node); // register the constructor for a plain Node object.
        go.putHiddenProperty("HopObject", node); // HopObject is the new name for node.
        go.putHiddenProperty("getProperty", new GlobalGetProperty ("getProperty", evaluator, fp));
        go.putHiddenProperty("token", new GlobalGetProperty ("token", evaluator, fp));
        go.putHiddenProperty("getUser", new GlobalGetUser ("getUser", evaluator, fp));
        go.putHiddenProperty("getUserBySession", new GlobalGetUserBySession ("getUserBySession", evaluator, fp));
        go.putHiddenProperty("getAllUsers", new GlobalGetAllUsers ("getAllUsers", evaluator, fp));
        go.putHiddenProperty("getActiveUsers", new GlobalGetActiveUsers ("getActiveUsers", evaluator, fp));
        go.putHiddenProperty("countActiveUsers", new GlobalCountActiveUsers ("countActiveUsers", evaluator, fp));
        go.putHiddenProperty("isActive", new GlobalIsActive ("isActive", evaluator, fp));
        go.putHiddenProperty("getAge", new GlobalGetAge ("getAge", evaluator, fp));
        go.putHiddenProperty("getURL", new GlobalGetURL ("getURL", evaluator, fp));
        go.putHiddenProperty("encode", new GlobalEncode ("encode", evaluator, fp));
        go.putHiddenProperty("encodeXml", new GlobalEncodeXml ("encodeXml", evaluator, fp));
        go.putHiddenProperty("encodeForm", new GlobalEncodeForm ("encodeForm", evaluator, fp));
        go.putHiddenProperty("format", new GlobalFormat ("format", evaluator, fp));
        go.putHiddenProperty("stripTags", new GlobalStripTags ("stripTags", evaluator, fp));
        go.putHiddenProperty("getXmlDocument", new GlobalGetXmlDocument ("getXmlDocument", evaluator, fp));
        go.putHiddenProperty("getHtmlDocument", new GlobalGetHtmlDocument ("getHtmlDocument", evaluator, fp));
        go.putHiddenProperty("jdomize", new GlobalJDOM ("jdomize", evaluator, fp));
        go.putHiddenProperty("getSkin", new GlobalCreateSkin ("getSkin", evaluator, fp));
        go.putHiddenProperty("createSkin", new GlobalCreateSkin ("createSkin", evaluator, fp));
        go.putHiddenProperty("renderSkin", new RenderSkin ("renderSkin", evaluator, fp, true, false));
        go.putHiddenProperty("renderSkinAsString", new RenderSkin ("renderSkinAsString", evaluator, fp, true, true));
        go.putHiddenProperty("authenticate", new GlobalAuthenticate ("authenticate", evaluator, fp));
        go.deleteProperty("exit", "exit".hashCode());

        // and some methods for session management from JS...
        reval.esUserPrototype.putHiddenProperty("logon", new UserLogin ("logon", evaluator, fp));
        reval.esUserPrototype.putHiddenProperty("login", new UserLogin ("login", evaluator, fp));
        reval.esUserPrototype.putHiddenProperty("register", new UserRegister ("register", evaluator, fp));
        reval.esUserPrototype.putHiddenProperty("logout", new UserLogout ("logout", evaluator, fp));
        reval.esUserPrototype.putHiddenProperty("onSince", new UserOnSince ("onSince", evaluator, fp));
        reval.esUserPrototype.putHiddenProperty("lastActive", new UserLastActive ("lastActive", evaluator, fp));
        reval.esUserPrototype.putHiddenProperty("touch", new UserTouch ("touch", evaluator, fp));

    }

    class NodeAdd extends BuiltinFunctionObject {
        NodeAdd (String name, Evaluator evaluator, FunctionPrototype fp) {
            super (fp, evaluator, name, 1);
        }
        public ESValue callFunction(ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
           ESNode node = (ESNode) thisObject;
           return ESBoolean.makeBoolean (node.add (arguments));
        }
    }

    class NodeAddAt extends BuiltinFunctionObject {
        NodeAddAt (String name, Evaluator evaluator, FunctionPrototype fp) {
            super (fp, evaluator, name, 1);
        }
        public ESValue callFunction (ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
           ESNode node = (ESNode) thisObject;
           return ESBoolean.makeBoolean (node.addAt (arguments));
        }
    }

    class NodeRemove extends BuiltinFunctionObject {
        NodeRemove (String name, Evaluator evaluator, FunctionPrototype fp) {
            super (fp, evaluator, name, 1);
        }
        public ESValue callFunction(ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
           ESNode node = (ESNode) thisObject;
           return ESBoolean.makeBoolean (node.remove (arguments));
        }
    }


    class NodeLink extends BuiltinFunctionObject {
        NodeLink (String name, Evaluator evaluator, FunctionPrototype fp) {
            super (fp, evaluator, name, 1);
        }
        public ESValue callFunction(ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
           ESNode node = (ESNode) thisObject;
           return ESBoolean.makeBoolean (node.link (arguments));
        }
    }
    
    class NodeList extends BuiltinFunctionObject {
        NodeList (String name, Evaluator evaluator, FunctionPrototype fp) {
            super(fp, evaluator, name, 0);
        }
        public ESValue callFunction(ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
           ESNode node = (ESNode) thisObject;
           return node.list ();
        }
    }

    class NodeGet extends BuiltinFunctionObject {
        NodeGet (String name, Evaluator evaluator, FunctionPrototype fp) {
            super (fp, evaluator, name, 1);
        }
        public ESValue callFunction (ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
           ESValue esv = null;
           if (arguments[0].isNumberValue ()) {
               int i = arguments[0].toInt32 ();
               esv = thisObject.getProperty (i);
           } else {
               String name = arguments[0].toString ();
               esv = thisObject.getProperty (name, name.hashCode ());
           }
           return (esv);
        }
    }

    class NodeSet extends BuiltinFunctionObject {
        NodeSet (String name, Evaluator evaluator, FunctionPrototype fp) {
            super (fp, evaluator, name, 1);
        }
        public ESValue callFunction (ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
           ESNode esn = (ESNode) thisObject;
           if (arguments[0].isNumberValue ()) {
               return ESBoolean.makeBoolean (esn.addAt (arguments));
           } else {
           	  String propname = arguments[0].toString ();
               esn.putProperty (propname, arguments[1], propname.hashCode ());
           }
           return ESBoolean.makeBoolean (true);
        }
    }

    class NodeCount extends BuiltinFunctionObject {
        NodeCount (String name, Evaluator evaluator, FunctionPrototype fp) {
            super (fp, evaluator, name, 1);
        }
        public ESValue callFunction (ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
           INode node = ((ESNode) thisObject).getNode ();
           return new ESNumber ((double) node.numberOfNodes ());
        }
    }

    class NodeContains extends BuiltinFunctionObject {
        NodeContains (String name, Evaluator evaluator, FunctionPrototype fp) {
            super (fp, evaluator, name, 1);
        }
        public ESValue callFunction (ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
           ESNode node = (ESNode) thisObject;
           return new ESNumber (node.contains (arguments));
        }
    }


    class NodeInvalidate extends BuiltinFunctionObject {
        NodeInvalidate (String name, Evaluator evaluator, FunctionPrototype fp) {
            super (fp, evaluator, name, 0);
        }
        public ESValue callFunction (ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
            ESNode esn = (ESNode) thisObject;
            INode node = esn.getNode ();
            if (node instanceof helma.objectmodel.db.Node) {
                ((helma.objectmodel.db.Node) node).invalidate ();
                esn.checkNode ();
            }
            return ESBoolean.makeBoolean (true);
        }
    }


    class NodeEditor extends BuiltinFunctionObject {

        NodeEditor (String name, Evaluator evaluator, FunctionPrototype fp) {
            super (fp, evaluator, name, 1);
        }

        public ESValue callFunction (ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
	if (arguments.length < 1)
	    throw new EcmaScriptException ("Missing argument for Node.editor(): Name of property to be edited.");
	String propName = arguments[0].toString ();
	ESValue propValue = thisObject.getProperty (propName, propName.hashCode ());
	if (propValue.isBooleanValue ())
	    return (booleanEditor (thisObject, propName, propValue, arguments));
	if (propValue.isNumberValue ())
	    return (numberEditor (thisObject, propName, propValue, arguments));
	if (propValue instanceof DatePrototype)
	    return (dateEditor (thisObject, propName, propValue, arguments));
	return (stringEditor (thisObject, propName, propValue, arguments));
        }

        public ESValue stringEditor (ESObject thisObject, String propName, ESValue propValue, ESValue[] arguments)
		throws EcmaScriptException {
            String value = null;
            if (propValue == null || ESNull.theNull == propValue || ESUndefined.theUndefined == propValue)
                value = "";
            else
                value = HtmlEncoder.encodeFormValue (propValue.toString ());
            if (arguments.length == 2 && arguments[1].isNumberValue ())
                return new ESString ("<input type=\"text\" name=\""+propName+"\" size=\""+arguments[1].toInt32 ()+"\" value=\""+ value +"\">");
            else if (arguments.length == 3 && arguments[1].isNumberValue () && arguments[2].isNumberValue ())
                return new ESString ("<textarea name=\""+propName+"\" cols=\""+arguments[1].toInt32 ()+"\" rows=\""+arguments[2].toInt32 ()+"\" wrap=\"virtual\">"+value+"</textarea>");
            return new ESString ("<input type=\"text\" name=\""+propName+"\" value=\""+ value +"\">");
        }

        public ESValue dateEditor (ESObject thisObject, String propName, ESValue propValue, ESValue[] arguments)
		throws EcmaScriptException {
            Date date = (Date) propValue.toJavaObject ();
            DateFormat fmt = arguments.length > 1 ? new SimpleDateFormat (arguments[1].toString ()) : new SimpleDateFormat ();
            return new ESString ("<input type=\"text\" name=\""+propName+"\" value=\""+ fmt.format (date) +"\">");
        }

        public ESValue numberEditor (ESObject thisObject, String propName, ESValue propValue, ESValue[] arguments)
		throws EcmaScriptException {
            ESNumber esn = (ESNumber) propValue.toESNumber ();
            double value = esn.doubleValue ();
            if (arguments.length == 3 && arguments[1].isNumberValue () && arguments[2].isNumberValue ())
                return new ESString (getNumberChoice (propName, arguments[1].toInt32 (), arguments[2].toInt32 (), 1, value));
            else if (arguments.length == 4 && arguments[1].isNumberValue () && arguments[2].isNumberValue () && arguments[3].isNumberValue ())
                return new ESString (getNumberChoice (propName, arguments[1].doubleValue (), arguments[2].doubleValue (), arguments[3].doubleValue (), value));
            DecimalFormat fmt = new DecimalFormat ();
            if (arguments.length == 2 && arguments[1].isNumberValue ())
                return new ESString ("<input type=\"text\" name=\""+propName+"\" size=\""+arguments[1].toInt32 ()+"\" value=\""+ fmt.format (value) +"\">");
            else
                return new ESString ("<input type=\"text\" name=\""+propName+"\" size=\"8\" value=\""+ fmt.format (value) +"\">");
        }

        public ESValue booleanEditor (ESObject thisObject, String propName, ESValue propValue, ESValue[] arguments)
		throws EcmaScriptException {
            ESBoolean esb = (ESBoolean) propValue.toESBoolean ();
            String value = esb.toString ();
            if (arguments.length < 2) {
            	   String retval = "<input type=\"checkbox\" name=\""+propName+"\" value=\"on\"";
            	   if (esb.booleanValue ()) retval += " checked";
            	   retval +=">";
            	   return new ESString (retval);
            }
            boolean forValue = arguments[1].booleanValue ();
            StringBuffer retval = new StringBuffer ("<input type=\"radio\" name=\""+propName+"\" value=\"");
            if (forValue) retval.append ("on");
            retval.append ("\"");
            if (forValue == esb.booleanValue()) retval.append (" checked");
            retval.append (">");
            return new ESString (retval.toString ());
        }
    }


    class StringEditor extends BuiltinFunctionObject {
        StringEditor (String name, Evaluator evaluator, FunctionPrototype fp) {
            super (fp, evaluator, name, 1);
        }
        public ESValue callFunction (ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
	throw (new EcmaScriptException ("String.editor() wird nicht mehr unterstuetzt. Statt node.strvar.editor(...) kann node.editor(\"strvar\", ...) verwendet werden."));
        }
    }
    
    class NumberEditor extends BuiltinFunctionObject {
        NumberEditor (String name, Evaluator evaluator, FunctionPrototype fp) {
            super (fp, evaluator, name, 1);
        }
        public ESValue callFunction (ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
	throw (new EcmaScriptException ("Number.editor() wird nicht mehr unterstuetzt. Statt node.nvar.editor(...) kann node.editor(\"nvar\", ...) verwendet werden."));
        }
    }
 
     class BooleanEditor extends BuiltinFunctionObject {
        BooleanEditor (String name, Evaluator evaluator, FunctionPrototype fp) {
            super (fp, evaluator, name, 1);
        }
        public ESValue callFunction (ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
	throw (new EcmaScriptException ("Boolean.editor() wird nicht mehr unterstuetzt. Statt node.boolvar.editor(...) kann node.editor(\"boolvar\", ...) verwendet werden."));
        }
    }

    // previously in FESI.Data.DateObject
    class DatePrototypeFormat extends BuiltinFunctionObject {
        DatePrototypeFormat(String name, Evaluator evaluator, FunctionPrototype fp) {
            super(fp, evaluator, name, 0);
        }
        public ESValue callFunction(ESObject thisObject,
                                ESValue[] arguments)
              throws EcmaScriptException {
           DatePrototype aDate = (DatePrototype) thisObject;

           DateFormat df = arguments.length > 0 ?
                  new SimpleDateFormat (arguments[0].toString ()) :
                  new SimpleDateFormat ();
           df.setTimeZone(TimeZone.getDefault());
           return (aDate.toJavaObject() == null) ?
           new ESString(""):
           new ESString(df.format((Date) aDate.toJavaObject()));
        }
    }

    // previously in FESI.Data.NumberObject
    private Hashtable formatTable = new Hashtable ();
    class NumberPrototypeFormat extends BuiltinFunctionObject {
        NumberPrototypeFormat(String name, Evaluator evaluator, FunctionPrototype fp) {
            super(fp, evaluator, name, 0);
         }
         public ESValue callFunction(ESObject thisObject,
                                ESValue[] arguments)
               throws EcmaScriptException {
           ESObject aNumber = (ESObject) thisObject;
           String fstr = "#,##0.00";
           if (arguments.length >= 1)
               fstr = arguments[0].toString ();
           DecimalFormat df = (DecimalFormat) formatTable.get (fstr);
           if (df == null) {
               df = new DecimalFormat (fstr);
               formatTable.put (fstr, df);
           }
           return new ESString(df.format(aNumber.doubleValue ()));
        }
    }

    class NodeChooser extends BuiltinFunctionObject {
        NodeChooser (String name, Evaluator evaluator, FunctionPrototype fp) {
            super (fp, evaluator, name, 1);
        }
        public ESValue callFunction (ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
            ESNode esn = (ESNode) thisObject;
            if (arguments.length < 1) {
            	   return ESBoolean.makeBoolean(false);
            }
            String nodename = arguments[0].toString ();
            INode target = esn.getNode ().getNode (nodename, false);
            ESNode collection = arguments.length > 1 ? (ESNode) arguments[1] : esn;
            if (arguments.length > 2)
                return new ESString (getNodeChooserDD (nodename, collection.getNode (), target, arguments[2].toString ()));
            else 
                return new ESString (getNodeChooserRB (nodename, collection.getNode (), target));
        }
    }

     class MultiNodeChooser extends BuiltinFunctionObject {
        MultiNodeChooser (String name, Evaluator evaluator, FunctionPrototype fp) {
            super (fp, evaluator, name, 1);
        }
        public ESValue callFunction (ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
            ESNode esn = (ESNode) thisObject;
            if (arguments.length < 1) {
            	   return ESBoolean.makeBoolean(false);
            }
            String nodename = arguments[0].toString ();
            INode thisNode = esn.getNode ();
            INode target = thisNode.getNode (nodename, false);
            if (target == null) {
            	   target = thisNode.createNode (nodename);
            }
            ESNode collection = arguments.length > 1 ? (ESNode) arguments[1] : esn;
            return new ESString (getNodeChooserCB (nodename, collection.getNode (), target));
        }
    }

    class UserLogin extends BuiltinFunctionObject {
        UserLogin (String name, Evaluator evaluator, FunctionPrototype fp) {
            super (fp, evaluator, name, 1);
        }
        public ESValue callFunction (ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
            if (arguments.length < 2) 
                return ESBoolean.makeBoolean(false);
            ESUser u = (ESUser) thisObject;
            if (u.user == null)
                throw new EcmaScriptException ("login() can only be called for user objects that are online at the moment!");
            boolean success = app.loginUser (arguments[0].toString (), arguments[1].toString (), u);
            try {
                u.doIndirectCall (this.evaluator, u, "onLogin", new ESValue[0]);
            } catch (Exception nosuch) {}
            return ESBoolean.makeBoolean (success);
        }
    }

    class UserRegister extends BuiltinFunctionObject {
        UserRegister (String name, Evaluator evaluator, FunctionPrototype fp) {
            super (fp, evaluator, name, 2);
        }
        public ESValue callFunction (ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
            if (arguments.length < 2) 
                return ESBoolean.makeBoolean(false);
            INode unode = app.registerUser (arguments[0].toString (), arguments[1].toString ());
            if (unode == null)
                return ESNull.theNull;
            else
                return  reval.getNodeWrapper (unode);
        }
    }

    class UserLogout extends BuiltinFunctionObject {
        UserLogout (String name, Evaluator evaluator, FunctionPrototype fp) {
            super (fp, evaluator, name, 1);
        }
        public ESValue callFunction (ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
            ESUser u = (ESUser) thisObject;
            if (u.user == null)
                return ESBoolean.makeBoolean (true);
            try {
                u.doIndirectCall (this.evaluator, u, "onLogout", new ESValue[0]);
            } catch (Exception nosuch) {}
            return ESBoolean.makeBoolean (app.logoutUser (u));
        }
    }
    
    class UserOnSince extends BuiltinFunctionObject {
        UserOnSince (String name, Evaluator evaluator, FunctionPrototype fp) {
            super (fp, evaluator, name, 1);
        }
        public ESValue callFunction (ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
            ESUser u = (ESUser) thisObject;
            if (u.user == null)
                throw new EcmaScriptException ("onSince() can only be called for users that are online at the moment!");
            DatePrototype date =  new DatePrototype(this.evaluator, new Date (u.user.onSince ()));
            return  date;
        }
    }

    class UserLastActive extends BuiltinFunctionObject {
        UserLastActive (String name, Evaluator evaluator, FunctionPrototype fp) {
            super (fp, evaluator, name, 1);
        }
        public ESValue callFunction (ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
            ESUser u = (ESUser) thisObject;
            if (u.user == null)
                throw new EcmaScriptException ("lastActive() can only be called for users that are online at the moment!");
            DatePrototype date =  new DatePrototype(this.evaluator, new Date (u.user.lastTouched ()));
            return  date;
        }
    }

    class UserTouch extends BuiltinFunctionObject {
        UserTouch (String name, Evaluator evaluator, FunctionPrototype fp) {
            super (fp, evaluator, name, 1);
        }
        public ESValue callFunction (ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
            ESUser u = (ESUser) thisObject;
            if (u.user != null)
                u.user.touch ();
            return  ESNull.theNull;
        }
    }

    class GlobalGetProperty extends BuiltinFunctionObject {
        GlobalGetProperty (String name, Evaluator evaluator, FunctionPrototype fp) {
            super (fp, evaluator, name, 1);
        }
        public ESValue callFunction (ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
            if (arguments.length == 0)
                return new ESString ("");
            String defval = (arguments.length > 1) ? arguments[1].toString () : "";
            return new ESString (app.getProperty (arguments[0].toString (), defval));
                
        }
    }

    class GlobalAuthenticate extends BuiltinFunctionObject {
        GlobalAuthenticate (String name, Evaluator evaluator, FunctionPrototype fp) {
            super (fp, evaluator, name, 1);
        }
        public ESValue callFunction (ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
            if (arguments.length != 2)
                return ESBoolean.makeBoolean (false);
            return ESBoolean.makeBoolean (app.authenticate (arguments[0].toString (), arguments[1].toString ()));

        }
    }

    /**
     * Get a parsed Skin from an app-managed skin text
     */
    class GlobalCreateSkin extends BuiltinFunctionObject {
        GlobalCreateSkin (String name, Evaluator evaluator, FunctionPrototype fp) {
            super (fp, evaluator, name, 1);
        }
        public ESValue callFunction (ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
            if (arguments.length != 1 || ESNull.theNull.equals (arguments[0]))
                throw new EcmaScriptException ("createSkin must be called with one String argument");
            String str = arguments[0].toString ();
            Skin skin = new Skin (str, app);
            return new ESWrapper (skin, evaluator);
        }
    }

    /**
     * Render a skin
     */
    class RenderSkin extends BuiltinFunctionObject {
        boolean global;
        boolean asString;
        RenderSkin (String name, Evaluator evaluator, FunctionPrototype fp, boolean global, boolean asString) {
            super (fp, evaluator, name, 1);
            this.global = global;
            this.asString = asString;
        }
        public ESValue callFunction (ESObject thisObj, ESValue[] arguments) throws EcmaScriptException {
            if (arguments.length < 1 || arguments.length > 2 || arguments[0] ==null || arguments[0] == ESNull.theNull)
                throw new EcmaScriptException ("renderSkin must be called with one Skin argument and an optional parameter argument");
            try {
                Skin skin = null;
                ESObject thisObject = global ? null : thisObj;
                ESObject paramObject = null;
                if (arguments.length > 1 && arguments[1] instanceof ESObject)
                    paramObject = (ESObject) arguments[1];

                // first, see if the first argument already is a skin object. If not, it's the name of the skin to be called
                if (arguments[0] instanceof ESWrapper) {
                    Object obj = ((ESWrapper) arguments[0]).toJavaObject ();
                    if (obj instanceof Skin)
                        skin = (Skin) obj;
                }

                if (skin == null)
                    skin = reval.getSkin (thisObject, arguments[0].toString ());
                if (asString)
                    reval.res.pushStringBuffer ();
                if (skin != null)
                    skin.render (reval, thisObject, paramObject);
                else
                    reval.res.write ("[Skin not found: "+arguments[0]+"]");
                if (asString)
                    return  new ESString (reval.res.popStringBuffer ());
            } catch (Exception x) {
                x.printStackTrace ();
                throw new EcmaScriptException ("renderSkin: "+x);
            }
            return ESNull.theNull;
        }
    }



    class GlobalGetUser extends BuiltinFunctionObject {
        GlobalGetUser (String name, Evaluator evaluator, FunctionPrototype fp) {
            super (fp, evaluator, name, 1);
        }
        public ESValue callFunction (ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
        	INode user = null;
	if (arguments.length > 0) {
	    String uname = arguments[0].toString ().trim ();
	    user = app.getUserNode (uname);
	}
        	if (user == null)
        	    return ESNull.theNull;
        	return reval.getNodeWrapper (user);
        }
    }

    class GlobalGetUserBySession extends BuiltinFunctionObject {
        GlobalGetUserBySession (String name, Evaluator evaluator, FunctionPrototype fp) {
            super (fp, evaluator, name, 1);
        }
        public ESValue callFunction (ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
        	User user = null;
	if (arguments.length > 0) {
	    String sid = arguments[0].toString ().trim ();
	    user = app.getUser (sid);
	}
        	if (user == null || user.getUID() == null)
        	    return ESNull.theNull;
	user.touch ();
        	return reval.getNodeWrapper (user.getNode ());
        }
    }


    class GlobalGetAllUsers extends BuiltinFunctionObject {
        GlobalGetAllUsers (String name, Evaluator evaluator, FunctionPrototype fp) {
            super (fp, evaluator, name, 1);
        }
        public ESValue callFunction (ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
        	INode users = app.getUserRoot ();
             ESObject ap = this.evaluator.getArrayPrototype();
             ArrayPrototype theArray = new ArrayPrototype(ap, this.evaluator);
             int i=0;
        	for (Enumeration e=users.properties (); e.hasMoreElements (); ) {
        	    String propname = (String) e.nextElement ();
                 theArray.putProperty (i++, new ESString (propname));
             }
             return theArray;
        }
    }

    class GlobalGetActiveUsers extends BuiltinFunctionObject {
        GlobalGetActiveUsers (String name, Evaluator evaluator, FunctionPrototype fp) {
            super (fp, evaluator, name, 1);
        }
        public ESValue callFunction (ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
           Hashtable sessions = (Hashtable) app.sessions.clone ();
           ESObject ap = this.evaluator.getArrayPrototype();
           ArrayPrototype theArray = new ArrayPrototype (ap, this.evaluator);
           theArray.setSize (sessions.size ());
           int i=0;
           // Hashtable visited = new Hashtable ();
           for (Enumeration e=sessions.elements(); e.hasMoreElements(); ) {
               User u = (User) e.nextElement ();
               // Note: we previously sorted out duplicate users - now we simply enumerate all active sessions.
               // if (u.uid == null || !visited.containsKey (u.uid)) {
               theArray.setElementAt (reval.getNodeWrapper (u), i++);
               // if (u.uid != null) visited.put (u.uid, u);
               // }
           }
           return theArray;
        }
    }

    class GlobalCountActiveUsers extends BuiltinFunctionObject {
        GlobalCountActiveUsers (String name, Evaluator evaluator, FunctionPrototype fp) {
            super (fp, evaluator, name, 1);
        }
        public ESValue callFunction (ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
           return new ESNumber (app.sessions.size ());
        }
    }

    class GlobalIsActive extends BuiltinFunctionObject {
        GlobalIsActive (String name, Evaluator evaluator, FunctionPrototype fp) {
            super (fp, evaluator, name, 1);
        }
        public ESValue callFunction (ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
            if (arguments.length < 1) 
                return ESBoolean.makeBoolean (false);
            String username = null;
            boolean active = false;
            if (arguments[0] instanceof ESUser) {
                ESUser esu = (ESUser) arguments[0];
                active = (esu.user != null);
            } else {
                active = app.activeUsers.contains (arguments[0].toString ());
            }
            return ESBoolean.makeBoolean (active);
        }
    }

    class GlobalGetAge extends BuiltinFunctionObject {
        GlobalGetAge (String name, Evaluator evaluator, FunctionPrototype fp) {
            super (fp, evaluator, name, 1);
        }
        public ESValue callFunction (ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
            if (arguments.length != 1 || !(arguments[0] instanceof DatePrototype))
                throw new EcmaScriptException ("Invalid arguments for function getAge(Date)");
                
            Date d = (Date) arguments[0].toJavaObject ();
            try {
                long then = d.getTime () / 60000l;
                long now = System.currentTimeMillis () / 60000l;
                StringBuffer age = new StringBuffer ();
                String divider = "vor ";
                long diff = now - then;
                long days = diff / 1440;
                if (days > 0) {
                    age.append (days > 1 ? divider+days+ " Tagen" : divider+"1 Tag");
                    divider = ", ";
                }
                long hours = (diff % 1440) / 60;
                if (hours > 0) {
                    age.append (hours > 1 ? divider+hours+ " Stunden" : divider+"1 Stunde");
                    divider = ", ";
                }
                long minutes = diff % 60;
                if (minutes > 0)
                    age.append (minutes > 1 ? divider+minutes+ " Minuten" : divider+"1 Minute");
                return new ESString (age.toString ());
	     
            } catch (Exception e) {
                app.logEvent ("Error formatting date: "+e);
                e.printStackTrace ();
                return new ESString (""); 
            }
        }
    }

    class GlobalGetURL extends BuiltinFunctionObject {
        GlobalGetURL (String name, Evaluator evaluator, FunctionPrototype fp) {
            super (fp, evaluator, name, 1);
        }
        public ESValue callFunction (ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
            if (arguments.length < 1)
                return  ESNull.theNull;
            try {
                StringBuffer urltext = new StringBuffer ();
                java.net.URL url = new java.net.URL (arguments[0].toString ());
                BufferedReader in = new BufferedReader (new InputStreamReader (url.openConnection ().getInputStream ()));
                char[] c = new char[1024];
                int read = -1;
                while ((read = in.read (c)) > -1)
                    urltext.append (c, 0, read);
                in.close ();
                return new ESString (urltext.toString ());
            } catch (Exception ignore) {}
            return  ESNull.theNull;
        }
    }

    class GlobalEncode extends BuiltinFunctionObject {
        GlobalEncode (String name, Evaluator evaluator, FunctionPrototype fp) {
            super (fp, evaluator, name, 1);
        }
        public ESValue callFunction (ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
            if (arguments.length < 1)
                return  ESNull.theNull;
            return new ESString (HtmlEncoder.encodeAll (arguments[0].toString ()));
        }
    }

    class GlobalEncodeXml extends BuiltinFunctionObject {
        GlobalEncodeXml (String name, Evaluator evaluator, FunctionPrototype fp) {
            super (fp, evaluator, name, 1);
        }
        public ESValue callFunction (ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
            if (arguments.length < 1)
                return  ESNull.theNull;
            return new ESString (HtmlEncoder.encodeXml (arguments[0].toString ()));
        }
    }

    class GlobalEncodeForm extends BuiltinFunctionObject {
        GlobalEncodeForm (String name, Evaluator evaluator, FunctionPrototype fp) {
            super (fp, evaluator, name, 1);
        }
        public ESValue callFunction (ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
            if (arguments.length < 1)
                return  ESNull.theNull;
            return new ESString (HtmlEncoder.encodeFormValue (arguments[0].toString ()));
        }
    }

    // strip tags from XML/HTML text
    class GlobalStripTags extends BuiltinFunctionObject {
        GlobalStripTags (String name, Evaluator evaluator, FunctionPrototype fp) {
            super (fp, evaluator, name, 1);
        }
        public ESValue callFunction (ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
            if (arguments.length < 1)
                return  ESNull.theNull;
            StringBuffer b = new StringBuffer ();
            char[] c = arguments[0].toString ().toCharArray ();
            boolean inTag = false;
            for (int i=0; i<c.length; i++) {
               if (c[i] == '<') inTag = true;
               if (!inTag) b.append (c[i]);
               if (c[i] == '>') inTag = false;
            }
            return new ESString (b.toString ());
        }
    }

    class GlobalFormat extends BuiltinFunctionObject {
        GlobalFormat (String name, Evaluator evaluator, FunctionPrototype fp) {
            super (fp, evaluator, name, 1);
        }
        public ESValue callFunction (ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
            if (arguments.length < 1)
                return  ESNull.theNull;
            return new ESString (HtmlEncoder.encode (arguments[0].toString ()));
        }
    }

    class GlobalGetXmlDocument extends BuiltinFunctionObject {
        GlobalGetXmlDocument (String name, Evaluator evaluator, FunctionPrototype fp) {
            super (fp, evaluator, name, 1);
        }
        public ESValue callFunction (ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
            try {
                // Class.forName ("org.apache.xerces.parsers.DOMParser");
                // org.apache.xerces.parsers.DOMParser parser = new org.apache.xerces.parsers.DOMParser();
                Class.forName ("org.openxml.parser.XMLParser");
                org.openxml.parser.XMLParser parser = new org.openxml.parser.XMLParser();
                Object p = arguments[0].toJavaObject ();
                if (p instanceof String) try {
                   // first try to interpret string as URL
	      java.net.URL u = new java.net.URL (p.toString ());
                   parser.parse (p.toString());
                } catch (java.net.MalformedURLException nourl) {
                   // if not a URL, maybe it is the XML itself
                   parser.parse (new InputSource (new StringReader (p.toString())));
                }
                else if (p instanceof InputStream)
                   parser.parse (new InputSource ((InputStream) p));
                else if (p instanceof Reader)
                   parser.parse (new InputSource ((Reader) p));
                return ESLoader.normalizeObject (parser.getDocument(), evaluator);
            } catch (Exception noluck) {
                app.logEvent ("Error creating XML document: "+noluck);
            }
            return ESNull.theNull;
        }
    }

    class GlobalGetHtmlDocument extends BuiltinFunctionObject {
        GlobalGetHtmlDocument (String name, Evaluator evaluator, FunctionPrototype fp) {
            super (fp, evaluator, name, 1);
        }
        public ESValue callFunction (ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
            try {
                Class.forName ("org.openxml.parser.HTMLParser");
                org.openxml.parser.HTMLParser parser = new org.openxml.parser.HTMLParser();
                Object p = arguments[0].toJavaObject ();
                if (p instanceof String) try {
                   // first try to interpret string as URL
	      java.net.URL u = new java.net.URL (p.toString ());
                   parser.parse (p.toString());
                } catch (java.net.MalformedURLException nourl) {
                   // if not a URL, maybe it is the HTML itself
                   parser.parse (new InputSource (new StringReader (p.toString())));
                }
                else if (p instanceof InputStream)
                   parser.parse (new InputSource ((InputStream) p));
                else if (p instanceof Reader)
                   parser.parse (new InputSource ((Reader) p));
                return ESLoader.normalizeObject (parser.getDocument(), evaluator);
            } catch (Exception noluck) {
                app.logEvent ("Error creating HTML document: "+noluck);
            }
            return ESNull.theNull;
        }
    }

    class GlobalJDOM extends BuiltinFunctionObject {
        GlobalJDOM (String name, Evaluator evaluator, FunctionPrototype fp) {
            super (fp, evaluator, name, 1);
        }
        public ESValue callFunction (ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
            try {
	   Class.forName ("org.w3c.dom.Document");
                org.w3c.dom.Document doc = (org.w3c.dom.Document) arguments[0].toJavaObject ();
                Class.forName ("org.jdom.input.DOMBuilder");
	   org.jdom.input.DOMBuilder builder = new org.jdom.input.DOMBuilder ();
                return ESLoader.normalizeObject (builder.build (doc), evaluator);
            } catch (Exception noluck) {
                app.logEvent ("Error wrapping JDOM document: "+noluck);
            }
            return ESNull.theNull;
        }
    }


    /* class NodePath extends BuiltinFunctionObject {
        NodePath (String name, Evaluator evaluator, FunctionPrototype fp) {
            super (fp, evaluator, name, 1);
        }
        public ESValue callFunction (ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
            INode n = ((ESNode) thisObject).getNode ();
            String tmpname = arguments[0].toString ();
            return new ESString (app.getNodePath (n, tmpname));
        }
    } */

    class NodeSetParent extends BuiltinFunctionObject {
        NodeSetParent (String name, Evaluator evaluator, FunctionPrototype fp) {
            super (fp, evaluator, name, 2);
        }
        public ESValue callFunction (ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
           ESNode node = (ESNode) thisObject;
           return ESBoolean.makeBoolean (node.setParent (arguments));
        }
    }


    class NodeHref extends BuiltinFunctionObject {
        NodeHref (String name, Evaluator evaluator, FunctionPrototype fp) {
            super (fp, evaluator, name, 1);
        }
        public ESValue callFunction (ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
            IPathElement elem = (IPathElement) thisObject.toJavaObject ();
            String tmpname = arguments.length == 0 ? "" : arguments[0].toString ();
            String basicHref =app.getNodeHref (elem, tmpname);
            String hrefSkin = app.getProperty ("hrefSkin", null);
            // FIXME: we should actually walk down the path from the object we called href() on
            // instead we move down the URL path.
            if (hrefSkin != null) {
                // we need to post-process the href with a skin for this application
                // first, look in the object href was called on.
                IPathElement skinElem = elem;
                Skin skin = null;
                while (skin == null && skinElem != null) {
                    Prototype proto = app.getPrototype (skinElem);
                    if (proto != null)
                        skin = proto.getSkin (hrefSkin);
                    if (skin == null)
                        skinElem = skinElem.getParentElement ();
                }

                if (skin != null) {
                    ESObject eso = reval.getElementWrapper (skinElem);
                    return renderSkin (skin, basicHref, eso);
                }
            }
            return new ESString (basicHref);
        }
        private ESString renderSkin (Skin skin, String path, ESObject obj) throws EcmaScriptException {
            reval.res.pushStringBuffer ();
            ESObject param = new ObjectPrototype (null, reval.evaluator);
            param.putProperty ("path", new ESString (path), "path".hashCode ());
            skin.render (reval, obj, param);
            return new ESString (reval.res.popStringBuffer ().trim ());
        }
    }


    private String getNumberChoice (String name, double from, double to, double step, double value) {
        double l = 0.000001;
        StringBuffer b = new StringBuffer ("<select name=\"");
        // DecimalFormat fmt = new DecimalFormat ();
        b.append (name);
        b.append ("\">");
        // check step for finity
        if (step == 0.0) step = 1.0;
        if (step * (to - from) < 0) step *= -1;
        double d = from <= to ? 1.0 : -1.0;
        for (double i=from; i*d<=to*d; i+=step) {
            if (Math.abs (i%1) < l) {
            	   int j = (int) i; 
                b.append ("<option value=\""+j);
                if (i == value) b.append ("\" selected=\"true");
                b.append ("\">"+j);
            } else {
                b.append ("<option value=\""+i);
                if (i == value) b.append ("\" selected=\"true");
                b.append ("\">"+i);
            }
        }
        b.append ("</select>");
        return b.toString ();
    }
 
    private String getNodeChooserDD (String name, INode collection, INode target, String teaser) {
        StringBuffer buffer = new StringBuffer ("<select name=\"");
        buffer.append (name);
        buffer.append ("\">");
        if (collection.contains (target) == -1) {
            buffer.append ("<option value=>");
            buffer.append (HtmlEncoder.encodeAll (teaser));
        }
        if (collection != null) {
        	int l = collection.numberOfNodes ();
        	for (int i=0; i<l; i++) {
        	    INode next = collection.getSubnodeAt (i);
        	    buffer.append ("<option value=\"");
        	    buffer.append (next.getID ());
        	    if (target == next)
        	        buffer.append ("\" selected=\"true");
        	    buffer.append ("\">");
        	    String cname = next.getString ("name", false);
        	    if (cname == null) cname = next.getName ();
        	    buffer.append (HtmlEncoder.encodeAll (cname));
        	}
        }
        buffer.append ("</select>");
        return buffer.toString ();
    }
    
    private String getNodeChooserRB (String name, INode collection, INode target) {
        StringBuffer buffer = new StringBuffer ();
        if (collection != null) {
        	int l = collection.numberOfNodes ();
        	for (int i=0; i<l; i++) {
        	    INode next = collection.getSubnodeAt (i);
        	    buffer.append ("<input type=radio name=\"");
        	    buffer.append (name);
        	    buffer.append ("\" value=\"");
        	    buffer.append (next.getElementName ()+"\"");
        	    if (target == next)
        	        buffer.append (" checked");
        	    buffer.append (">");
                 String cname = next.getString ("name", false);
        	    if (cname == null) cname = next.getName ();
        	    buffer.append (HtmlEncoder.encodeAll (cname));
        	    buffer.append ("<br>");
        	}
        }
        return buffer.toString ();
    }

    private String getNodeChooserCB (String name, INode collection, INode target) {
        StringBuffer buffer = new StringBuffer ();
         if (collection != null) {
        	int l = collection.numberOfNodes ();
        	for (int i=0; i<l; i++) {
        	    INode next = collection.getSubnodeAt (i);
        	    buffer.append ("<input type=checkbox name=\"");
        	    buffer.append (name);
        	    buffer.append ("\" value=");
        	    buffer.append (next.getElementName ());
        	    if (target.contains (next) > -1)
        	        buffer.append (" checked");
        	    buffer.append (">");
        	    buffer.append (HtmlEncoder.encodeAll (next.getName ()));
        	    buffer.append ("<br>");
        	}
        }
        return buffer.toString ();
    }
  
 }
 
 


























































































