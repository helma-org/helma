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

    Application app;
    String sessionID;

    // the unique id (login name) for the user, if logged in
    String uid;
	
	// the user object that holds references to other sessions.
	User user;

    // the handle to this user's persistent db node, if logged in
    NodeHandle userHandle;

    // the transient session node that is exposed to javascript
    // this stays the same across logins and logouts.
    TransientNode sessionNode;

    long onSince, lastTouched;

    // used to remember messages to the user between requests -
    // used for redirects.
    String message;

    public Session (String sessionID, Application app) {
		this.sessionID = sessionID;
		this.app = app;
		this.uid = null;
		this.userHandle = null;
		sessionNode = new TransientNode ("session");
		onSince = System.currentTimeMillis ();
		lastTouched = onSince;
    }
    
	public void login (INode usernode)	{
		if (usernode==null)	{
			userHandle = null;
			uid = null;
		}	else	{
			userHandle = ((Node)usernode).getHandle();
			uid = usernode.getElementName();
		}
	}

	public void logout()	{
		userHandle = null;
		uid = null;
	}

	public boolean isLoggedIn()	{
		if (userHandle!=null && uid!=null)	{
			return true;
		}	else	{
			return false;
		}
	}

	public INode getUserNode()	{
		if (userHandle!=null)	{
			return userHandle.getNode (app.getWrappedNodeManager());
		}	else	{
			return null;
		}
	}

    public INode getNode () {
    	return sessionNode;
    }

	public Application getApp ()	{
		return app;
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

	public String toString ()	{
		if ( uid!=null )
			return "Session " + sessionID + ":" + uid;
		else
			return "Session " + sessionID;
	}

    /**
     * Get the persistent user id of a registered user. This is usually the user name, or
     * null if the user is not logged in.
     */
    public String getUID () {
		return uid;
    }

}

