// ESAppNode.java
// Copyright (c) Hannes Wallnöfer 1998-2000

package helma.scripting.fesi;

import helma.framework.core.*;
import helma.objectmodel.*;
import FESI.Exceptions.*;
import FESI.Data.*;
import FESI.Interpreter.Evaluator;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

/**
 * ESApp represents the app node of an application, providing an app-wide transient shared
 * space as well as access to some app related runtime information.
 */

public class ESAppNode extends ESNode {

    private Application app;
    private DatePrototype createtime;

    public ESAppNode (INode node, FesiEvaluator eval) throws EcmaScriptException {
	super (eval.getPrototype("hopobject"), eval.getEvaluator(), node, eval);
	app = eval.getApplication();
	createtime = new DatePrototype (evaluator, node.created());
	FunctionPrototype fp = (FunctionPrototype) evaluator.getFunctionPrototype();
	putHiddenProperty("getThreads", new AppCountThreads ("getThreads", evaluator, fp));
	putHiddenProperty("getMaxThreads", new AppCountEvaluators ("getMaxThreads", evaluator, fp));
	putHiddenProperty("getFreeThreads", new AppCountFreeEvaluators ("getFreeThreads", evaluator, fp));
	putHiddenProperty("getActiveThreads", new AppCountActiveEvaluators ("getActiveThreads", evaluator, fp));
	putHiddenProperty("getMaxActiveThreads", new AppCountMaxActiveEvaluators ("getMaxActiveThreads", evaluator, fp));
	putHiddenProperty("setMaxThreads", new AppSetNumberOfEvaluators ("setMaxThreads", evaluator, fp));
	putHiddenProperty("clearCache", new AppClearCache ("clearCache", evaluator, fp));

	// session-related methods
	putHiddenProperty("countSessions", new AppCountSessions ("countSessions", evaluator, fp));
	putHiddenProperty("getSession",    new AppGetSession    ("getSession", evaluator, fp));
	putHiddenProperty("getSessions",   new AppGetSessions   ("getSessions", evaluator, fp));
	putHiddenProperty("createSession", new AppCreateSession ("createSession", evaluator, fp));

	// user-related methods:
	putHiddenProperty("registerUser",  new AppRegisterUser   ("registerUser", evaluator, fp));
	putHiddenProperty("getUser",       new AppGetUser        ("getUser", evaluator, fp));
	putHiddenProperty("getActiveUsers",new AppGetActiveUsers ("getActiveUsers", evaluator, fp));
	putHiddenProperty("getSessionsForUser", new AppGetSessionsForUser ("getSessionsForUser", evaluator, fp));

    }

    /**
     * Overrides getProperty to return some app-specific properties
     */
    public ESValue getProperty (String propname, int hash) throws EcmaScriptException {
	if ("requestCount".equals (propname)) {
	    return new ESNumber (app.getRequestCount ());
	}
	if ("xmlrpcCount".equals (propname)) {
	    return new ESNumber (app.getXmlrpcCount ());
	}
	if ("errorCount".equals (propname)) {
	    return new ESNumber (app.getErrorCount ());
	}
	if ("upSince".equals (propname)) {
	    return createtime;
	}
	if ("skinfiles".equals (propname)) {
	    ESObject skinz = new ObjectPrototype (null, evaluator);
	    for (Iterator it = app.getPrototypes().iterator(); it.hasNext(); ) {
	        Prototype p = (Prototype) it.next ();
	        ESObject  proto = new ObjectPrototype (null, evaluator);
	        for (Iterator it2 = p.skins.values().iterator(); it2.hasNext(); ) {
	            SkinFile sf = (SkinFile) it2.next ();
	            String name = sf.getName ();
	            Skin skin = sf.getSkin ();
	            proto.putProperty (name, new ESString (skin.getSource ()), name.hashCode ());
	        }
	        skinz.putProperty (p.getName (), proto, p.getName ().hashCode ());
	    }
	    return skinz;
	}
	if ("__app__".equals (propname)) {
	    return new ESWrapper (app, evaluator);
	}
	return super.getProperty (propname, hash);
    }


    class AppCountEvaluators extends BuiltinFunctionObject {
        AppCountEvaluators (String name, Evaluator evaluator, FunctionPrototype fp) {
            super (fp, evaluator, name, 0);
        }
        public ESValue callFunction (ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
           return new ESNumber (app.countEvaluators ());
        }
    }

    class AppCountFreeEvaluators extends BuiltinFunctionObject {
        AppCountFreeEvaluators (String name, Evaluator evaluator, FunctionPrototype fp) {
            super (fp, evaluator, name, 0);
        }
        public ESValue callFunction (ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
           return new ESNumber (app.countFreeEvaluators ());
        }
    }

    class AppCountActiveEvaluators extends BuiltinFunctionObject {
        AppCountActiveEvaluators (String name, Evaluator evaluator, FunctionPrototype fp) {
            super (fp, evaluator, name, 0);
        }
        public ESValue callFunction (ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
           return new ESNumber (app.countActiveEvaluators ());
        }
    }

    class AppCountMaxActiveEvaluators extends BuiltinFunctionObject {
        AppCountMaxActiveEvaluators (String name, Evaluator evaluator, FunctionPrototype fp) {
            super (fp, evaluator, name, 0);
        }
        public ESValue callFunction (ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
           return new ESNumber (app.countMaxActiveEvaluators ());
        }
    }

    class AppCountThreads extends BuiltinFunctionObject {
        AppCountThreads (String name, Evaluator evaluator, FunctionPrototype fp) {
            super (fp, evaluator, name, 0);
        }
        public ESValue callFunction (ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
           return new ESNumber (app.countThreads ());
        }
    }

    class AppSetNumberOfEvaluators extends BuiltinFunctionObject {
        AppSetNumberOfEvaluators (String name, Evaluator evaluator, FunctionPrototype fp) {
            super (fp, evaluator, name, 1);
        }
        public ESValue callFunction (ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
            RequestEvaluator ev = new RequestEvaluator (app);
            if (arguments.length != 1)
                return ESBoolean.makeBoolean (false);
            // add one to the number to compensate for the internal scheduler.
            return ESBoolean.makeBoolean (app.setNumberOfEvaluators (1 + arguments[0].toInt32()));
        }
    }

    class AppClearCache extends BuiltinFunctionObject {
        AppClearCache (String name, Evaluator evaluator, FunctionPrototype fp) {
            super (fp, evaluator, name, 1);
        }
        public ESValue callFunction (ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
            app.clearCache ();
            return ESBoolean.makeBoolean (true);
        }
    }
    
	class AppCountSessions extends BuiltinFunctionObject {
        AppCountSessions (String name, Evaluator evaluator, FunctionPrototype fp) {
            super (fp, evaluator, name, 1);
        }
        public ESValue callFunction (ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
            return new ESNumber ( (double)app.sessions.size() );
        }
    }

	class AppGetSession extends BuiltinFunctionObject {
        AppGetSession (String name, Evaluator evaluator, FunctionPrototype fp) {
            super (fp, evaluator, name, 1);
        }
        public ESValue callFunction (ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
			Session session = null;
			if (arguments.length > 0) {
				String sid = arguments[0].toString ().trim ();
				session = app.getSession (sid);
			}
			if (session == null)
				return ESNull.theNull;
			return new ESSession (session, eval);
        }
    }

	class AppCreateSession extends BuiltinFunctionObject {
        AppCreateSession (String name, Evaluator evaluator, FunctionPrototype fp) {
            super (fp, evaluator, name, 1);
        }
        public ESValue callFunction (ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
			Session session = null;
			if (arguments.length > 0) {
				String sid = arguments[0].toString ().trim ();
				session = app.checkSession (sid);
			}
			if (session == null)
				return ESNull.theNull;
			return new ESSession (session, eval);
        }
    }

	class AppGetSessions extends BuiltinFunctionObject {
        AppGetSessions (String name, Evaluator evaluator, FunctionPrototype fp) {
            super (fp, evaluator, name, 1);
        }
        public ESValue callFunction (ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
			ArrayPrototype theArray = new ArrayPrototype (evaluator.getArrayPrototype(), evaluator);
			theArray.setSize (app.sessions.size ());
			int i=0;
			for (Enumeration e=app.sessions.elements(); e.hasMoreElements(); ) {
				Session s = (Session) e.nextElement ();
				theArray.setElementAt (new ESSession(s,eval), i++);
			}
			return theArray;
		}
    }

	class AppRegisterUser extends BuiltinFunctionObject {
        AppRegisterUser (String name, Evaluator evaluator, FunctionPrototype fp) {
            super (fp, evaluator, name, 1);
        }
        public ESValue callFunction (ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
            if (arguments.length < 2)
                return ESBoolean.makeBoolean(false);
            INode usernode = app.registerUser (arguments[0].toString (), arguments[1].toString ());
			if (usernode==null)
				return ESNull.theNull;
			else
				return eval.getNodeWrapper (usernode);
        }
    }

	class AppGetUser extends BuiltinFunctionObject {
        AppGetUser (String name, Evaluator evaluator, FunctionPrototype fp) {
            super (fp, evaluator, name, 1);
        }
        public ESValue callFunction (ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
            if (arguments.length==0)
                return ESNull.theNull;
            INode usernode = app.getUserNode (arguments[0].toString ());
			if (usernode==null)
				return ESNull.theNull;
			else
				return eval.getNodeWrapper (usernode);
        }
    }

	class AppGetActiveUsers extends BuiltinFunctionObject {
        AppGetActiveUsers (String name, Evaluator evaluator, FunctionPrototype fp) {
            super (fp, evaluator, name, 1);
        }
        public ESValue callFunction (ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
			ArrayPrototype theArray = new ArrayPrototype (evaluator.getArrayPrototype(), evaluator);
			theArray.setSize (app.activeUsers.size ());
			int i=0;
			for (Enumeration e=app.activeUsers.elements(); e.hasMoreElements(); ) {
				User user = (User) e.nextElement ();
				INode usernode = user.getUserNodeHandle().getNode(eval.app.getWrappedNodeManager());
				if (usernode==null)
					theArray.setElementAt (ESNull.theNull, i++);
				else
					theArray.setElementAt (eval.getNodeWrapper(usernode), i++);
			}
			return theArray;
		}
    }

	class AppGetSessionsForUser extends BuiltinFunctionObject {
        AppGetSessionsForUser (String name, Evaluator evaluator, FunctionPrototype fp) {
            super (fp, evaluator, name, 1);
        }
        public ESValue callFunction (ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
            if (arguments.length==0)
                return ESNull.theNull;
			String username = null;
			if ( arguments[0] instanceof ESNode )
				username = ((ESNode)arguments[0]).getNode().getName();
			else
				username = arguments[0].toString();
			ArrayPrototype theArray = new ArrayPrototype (evaluator.getArrayPrototype(), evaluator);
			Hashtable userSessions = app.getSessionsForUsername(username);
			if (userSessions==null)	{
				theArray.setSize(0);
				return theArray;
			}
			theArray.setSize (userSessions.size());
			int i=0;
			for (Enumeration e=userSessions.elements(); e.hasMoreElements(); ) {
				Session s = (Session) e.nextElement ();
				theArray.setElementAt (new ESSession(s,eval), i++);
			}
			return theArray;
		}
    }


    public String toString () {
	return ("AppNode "+node.getElementName ());
    }

}

