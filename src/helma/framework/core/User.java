// User.java
// Copyright (c) Hannes Wallnöfer 1998-2000

package helma.framework.core;

import java.io.*;
import java.util.*;
import java.net.URLEncoder;
import helma.objectmodel.*;
import helma.objectmodel.db.*;

/**
 * This represents a user who is currently using the Hop application. This does
 * not just comprend registered users, but anybody who happens to surf the site.
 * Depending on whether the user is logged in or not, the user object holds a
 * persistent user node or just a transient cache node
 */
 
public class User implements Serializable {

    Application app;
    String sessionID;

    // the unique id (login name) for the user, if logged in
    String uid;

    // the handle to this user's persistent db node, if logged in
    NodeHandle nhandle;

    // the transient cache node. This stays the same across logins and logouts.
    // If logged out, this also represents the user's main node.
    TransientNode cache;

    DbMapping umap;
    long onSince, lastTouched;

    // used to remember messages to the user between requests -
    // used for redirects.
    String message;

    public User (String sid, Application app) {
	this.uid = null;
	this.nhandle = null;
	this.app = app;
	setNode (null);
	umap = app.getDbMapping ("user");
	cache = new TransientNode ("[session cache]");
	cache.setPrototype ("user");
	cache.setDbMapping (umap);
	sessionID = sid;
	onSince = System.currentTimeMillis ();
	lastTouched = onSince;
    }
    
    
    /**
     *  This is used to turn for login and logout.
     *  Calling this weith a DB Node object will turn an anonymous user into a registered or known one.
     *  The user object remains the same, but he or she gets some persistent storage.
     *  On the other side, calling this method with a parameter value of null is means the user
     *  is logged out and will be represented by its transient cache node.
     */
    public void setNode (INode n) {
	// IServer.getLogger().log ("esn = "+esn);
	if (n == null) {
	    nhandle = null;
	    uid = null;
	} else {
	    uid = n.getElementName ();
	    nhandle = ((Node) n).getHandle ();
	}
	// System.err.println ("User.setNode: "+nhandle);
    }

    public INode getNode () {
	if (nhandle == null) {
	    return cache;
	} else {
	    // in some special cases, a user's node handle may go bad, for instance
	    // if something bad happens during registration. For this reason, we check
	    // if the handle actually works. If not, it is reset to the transient cache, which
	    // means the user is logged out.
	    Node n = nhandle.getNode (app.nmgr.safe);
	    if (n == null) {
	        setNode (null);
	        return cache;
	    }
	    return n;
	}
    }

    public String getSessionID () {
	return sessionID;
    }
    

    public void touch () {
	lastTouched = System.currentTimeMillis ();
    }

    public long lastTouched () {
	return lastTouched;
    }

    public long onSince () {
	return onSince;
    }

    /**
     * Get the persistent user id of a registered user. This is usually the user name, or
     * null if the user is not logged in.
     */
    public String getUID () {
	return uid;
    }

    /**
     * Return the transient cache node for this user.
     */
    public INode getCache () {
	return cache;
    }

    /**
     * Reset the session cache node, clearing all properties.
     * This is done by recreating the cache node object.
     */
    public void clearCache () {
	cache = new TransientNode ("[session cache]");
	cache.setPrototype ("user");
	cache.setDbMapping (umap);
    }
}

