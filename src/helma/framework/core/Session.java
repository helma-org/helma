// Session.java

package helma.framework.core;

import java.io.*;
import java.util.*;
import java.net.URLEncoder;
import helma.objectmodel.*;
import helma.objectmodel.db.*;

/**
 * This represents a session currently using the Hop application.
 * This comprends anybody who happens to surf the site.
 * Depending on whether the user is logged in or not, the user object holds a
 * persistent user node.
 */
 
public class Session implements Serializable {

    transient Application app;
    String sessionID;

    // the unique id (login name) for the user, if logged in
    String uid;

    // the handle to this user's persistent db node, if logged in
    NodeHandle userHandle;

    // the transient cache node that is exposed to javascript
    // this stays the same across logins and logouts.
    public TransientNode cacheNode;

    long onSince, lastTouched, lastModified;

    // used to remember messages to the user between requests -
    // used for redirects.
    String message;

    public Session (String sessionID, Application app) {
	this.sessionID = sessionID;
	this.app = app;
	this.uid = null;
	this.userHandle = null;
	cacheNode = new TransientNode ("session");
	onSince = System.currentTimeMillis ();
	lastTouched = lastModified = onSince;
    }

    /**
     * attach the given user node to this session.
     */
    public void login (INode usernode) {
	if (usernode==null) {
	    userHandle = null;
	    uid = null;
	} else {
	    userHandle = ((Node)usernode).getHandle();
	    uid = usernode.getElementName();
	}
	lastModified = System.currentTimeMillis ();
    }

    /**
     * remove this sessions's user node.
     */
    public void logout() {
	userHandle = null;
	uid = null;
	lastModified = System.currentTimeMillis ();
    }

    public boolean isLoggedIn() {
	if (userHandle!=null && uid!=null) {
	    return true;
	} else {
	    return false;
	}
    }

    /**
     * Gets the user Node from this Application's NodeManager.
     */
    public INode getUserNode() {
	if (userHandle!=null)
	    return userHandle.getNode (app.getWrappedNodeManager());
	else
	    return null;
    }

    /**
     * Gets the transient cache node.
     */
    public INode getCacheNode () {
	return cacheNode;
    }

    public Application getApp () {
	return app;
    }

    public void setApp (Application app) {
	this.app = app;
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

    public long lastModified () {
	return lastModified;
    }
    
    public void setLastModified (Date date) {
	if (date != null)
	    lastModified = date.getTime ();
    }

    public long onSince () {
	return onSince;
    }

    public String toString () {
	if ( uid!=null )
	    return "[Session for user " + uid + "]";
	else
	    return "[Anonymous Session]";
    }

    /**
     * Get the persistent user id of a registered user. This is usually the user name, or
     * null if the user is not logged in.
     */
    public String getUID () {
	return uid;
    }

}

