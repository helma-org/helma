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

    public ESUser (INode node, RequestEvaluator eval, User user) {
	super (eval.esUserPrototype, eval.evaluator, node, eval);
	this.user = user;
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


    /**
     * The node for a user object changes at login and logout, so we don't use our
     * own node, but just reach through to the session user object instead.
     */
    public void setNode (INode node) {
	user.setNode (node);
	if (node != null) {
	    this.node = node;
	} else {
	    // user.getNode will never return null. If the node is set to null (=user logged out)
	    // it will user the original transient cache node again.
	    this.node = user.getNode ();
	}
	// set node handle to wrapped node
	if (node instanceof helma.objectmodel.db.Node)
	    handle = ((helma.objectmodel.db.Node) node).getHandle ();
	else
	    handle = null;
	// we don't take over the transient cache from the node,
	// because we always use the one from the user object.
    }

    public void updateNodeFromUser () {
	node = user.getNode ();
	// set node handle to wrapped node
	if (node instanceof helma.objectmodel.db.Node)
	    handle = ((helma.objectmodel.db.Node) node).getHandle ();
	else
	    handle = null;
	
    }	

    public String toString () {
	return ("UserObject "+node.getName ());
    }

}













































