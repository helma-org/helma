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
    String uid; // the unique id (login name) for the user, if logged in
    NodeHandle nhandle;
    DbMapping umap;
    long onSince, lastTouched;
    TransientNode cache;
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
     *  This is used to turn an anonymous user into a registered or known one.
     *  The user object remains the same, but she gets some persistent storage.
     */
    public void setNode (INode n) {
	// IServer.getLogger().log ("esn = "+esn);
	if (n == null) {
	    nhandle = null;
	    uid = null;
	} else {
	    uid = n.getElementName ();
	    nhandle = ((helma.objectmodel.db.Node) n).getHandle ();
	}
    }

    public INode getNode () {
	if (nhandle == null) {
	    return cache;
	} else {
	    return nhandle.getNode (app.nmgr.safe);
	}
    }

    public String getSessionID () {
	return sessionID;
    }
    

    public void touch () {
	lastTouched = System.currentTimeMillis ();
    }

    public long touched () {
	return lastTouched;
    }
 
}































