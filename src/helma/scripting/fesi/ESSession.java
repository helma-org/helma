// ESSession.java
// Copyright (c) Hannes Wallnöfer 1998-2000

package helma.scripting.fesi;

import java.util.Date;
import helma.framework.core.*;
import helma.objectmodel.*;
import helma.objectmodel.db.Node;
import FESI.Interpreter.*;
import FESI.Exceptions.*;
import FESI.Data.*;

/**
 * The ESSession a wrapper around the session object that represents any user of
 * a Helma application. If the user is logged in as a registered member, the
 * wrapped node holds a property user, that represents the user object in the
 * database.
 */

public class ESSession extends ESNode {

    /** if the user is online, this is his/her online session object */
    public Session session;

    public ESSession (Session session, FesiEvaluator eval) throws EcmaScriptException {
		super (eval.getPrototype("hopobject"), eval.getEvaluator(), session.getNode(), eval);
		this.session = session;
		FunctionPrototype fp = (FunctionPrototype) evaluator.getFunctionPrototype();
        putHiddenProperty("logon", new SessionLogin ("logon", evaluator, fp));
        putHiddenProperty("login", new SessionLogin ("login", evaluator, fp));
        putHiddenProperty("logout", new SessionLogout ("logout", evaluator, fp));
        putHiddenProperty("touch", new SessionTouch ("touch", evaluator, fp));
        putHiddenProperty("lastActive", new SessionLastActive ("lastActive", evaluator, fp));
		putHiddenProperty("onSince", new SessionOnSince ("onSince", evaluator, fp));
    }

    /**
     * Overrides getProperty to return the uid (which is not a regular property)
     */
    public ESValue getProperty (String propname, int hash) throws EcmaScriptException {
		if ("user".equals (propname) )	{
			if (session==null)	{
				return ESNull.theNull;
			}	else	{
				INode usernode = session.getUserNode();
				if (usernode==null)	{
					return ESNull.theNull;
				}	else	{
					return eval.getNodeWrapper (usernode);
				}
			}
		}
		if ("_id".equals (propname) || "cookie".equals (propname) ) {
	    	if (session == null || session.getSessionID () == null)
		        return ESNull.theNull;
	    	else
	        	return new ESString (session.getSessionID ());
		}
		return super.getProperty (propname, hash);
	// FIXME: do we want a cache object?
	//	if ("cache".equals (propname) && session != null) {
	//	    cache = session.getCache ();
	//	    cacheWrapper.node = cache;
	//	    return cacheWrapper;
	//	}
    }

	public void putProperty(String propertyName,ESValue propertyValue, int hash) throws EcmaScriptException {
		// FIXME: could be used as smooth login/logout mechanism?
		if ( propertyName.equals("user") )	{
			throw new EcmaScriptException("session.user is readonly");
		}
		super.putProperty (propertyName,propertyValue,hash);
	}

    public String toString () {
		return ("ESSession " + session.getSessionID() );
    }

    public ESValue getDefaultValue(int hint) throws EcmaScriptException {
        return new ESString (this.toString());
    }

    class SessionLogin extends BuiltinFunctionObject {
        SessionLogin (String name, Evaluator evaluator, FunctionPrototype fp) {
            super (fp, evaluator, name, 1);
        }
        public ESValue callFunction (ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
            if (arguments.length < 2)
                return ESBoolean.makeBoolean(false);
            boolean success = session.getApp().loginSession (arguments[0].toString (), arguments[1].toString (), session);
// FIXME	try {
//                session.doIndirectCall (this.evaluator, u, "onLogin", new ESValue[0]);
//            } catch (Exception nosuch) {}
            return ESBoolean.makeBoolean (success);
        }
    }

    class SessionLogout extends BuiltinFunctionObject {
        SessionLogout (String name, Evaluator evaluator, FunctionPrototype fp) {
            super (fp, evaluator, name, 1);
        }
        public ESValue callFunction (ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
            session.getApp().logoutSession (session);
			return ESBoolean.makeBoolean (true);
// FIXME	try {
//                s.doIndirectCall (this.evaluator, u, "onLogout", new ESValue[0]);
//            } catch (Exception nosuch) {}
//            return ESBoolean.makeBoolean (app.logoutUser (s.session));
        }
    }
    
    class SessionOnSince extends BuiltinFunctionObject {
        SessionOnSince (String name, Evaluator evaluator, FunctionPrototype fp) {
            super (fp, evaluator, name, 1);
        }
        public ESValue callFunction (ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
			DatePrototype date =  new DatePrototype(this.evaluator, new Date (session.onSince ()));
            return date;
        }
    }

    class SessionLastActive extends BuiltinFunctionObject {
        SessionLastActive (String name, Evaluator evaluator, FunctionPrototype fp) {
            super (fp, evaluator, name, 1);
        }
        public ESValue callFunction (ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
            DatePrototype date =  new DatePrototype(this.evaluator, new Date (session.lastTouched ()));
            return date;
        }
    }

    class SessionTouch extends BuiltinFunctionObject {
        SessionTouch (String name, Evaluator evaluator, FunctionPrototype fp) {
            super (fp, evaluator, name, 1);
        }
        public ESValue callFunction (ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
			session.touch ();
            return  ESNull.theNull;
        }
    }


}

