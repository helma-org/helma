// ESUser.java
// Copyright (c) Hannes Wallnöfer 1998-2000

package helma.framework.core;

import helma.objectmodel.*;
import FESI.Interpreter.*;
import FESI.Exceptions.*;
import FESI.Data.*;

/**
 * The ESUser is a special kind of Node object that represents a user of 
 * a HOP application. The actual user session data are kept in class User.
 * If the user is logged in as a registered member, the wrapped node represents
 * the user object in the database, while for anonymous surfers the node object 
 * is just a transient node. <p>
 * This means that the wrapped node will be swapped when the user logs in or out. 
 * To save session state across logins and logouts, the 
 * cache property of the user object stays the same for the whole time the user
 * spends on this site.
 */

public class ESUser extends ESNode {

    // if the user is online, this is his/her online session object
    public User user;

    public ESUser (INode node, RequestEvaluator eval) {
	super (eval.esUserPrototype, eval.evaluator, node, eval);
	user = (User) eval.app.activeUsers.get (node.getNameOrID ());
	if (user == null)
	    user = (User) eval.app.sessions.get (node.getNameOrID ());
	if (user != null) {
	    cache = user.cache;
	    cacheWrapper = new ESNode (cache, eval);
	}
    }

    /**
     * Overrides getProperty to return the uid (which is not a regular property)
     */
    public ESValue getProperty (String propname, int hash) throws EcmaScriptException {
	if ("uid".equals (propname)) {
	    if (user == null || user.uid == null)
	        return ESNull.theNull;
	    else
	        return new ESString (user.uid);
	}
	if ("sessionID".equals (propname)) {
	    if (user == null || user.getSessionID () == null)
	        return ESNull.theNull;
	    else
	        return new ESString (user.getSessionID ());
	}
	return super.getProperty (propname, hash);
    }


    public void setUser (User user) {
	if (this.user != user) {
	    this.user = user;
	    cache = user.cache;
	}
	cacheWrapper = new ESNode (cache, eval);
    }

    public void setNode (INode node) {
	if (node != null) {
	    this.node = node;
	    nodeID = node.getID ();
	    dbmap = node.getDbMapping ();
	    eval.objectcache.put (node, this);
	    // we don't take over the transient cache from the node,
	    // because we always use the one from the user object.
	}
    }


    public String toString () {
	return ("UserObject "+node.getNameOrID ());
    }

}













































