// User.java
// Copyright (c) Hannes Wallnöfer 1998-2000

package helma.framework.core;

import java.io.*;
import java.util.*;
import java.net.URLEncoder;
import helma.objectmodel.*;

/**
 * This represents a user who is currently using the HOP application. This does 
 * not just comprend registered users, but anybody who happens to surf the site. 
 */
 
public class User implements Serializable {

    Application app;
    String sessionID;
    String uid, nid;
    long onSince, lastTouched;
    Node cache;
    DbMapping umap;

    public User (String sid, Application app) {
	this.uid = null;
	this.nid = null;
    	this.app = app;
    	setNode (null);
	cache = new Node (sid);
             cache.setString ("prototype", "user");
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
	    nid = null;
	    uid = null;
	} else {
	    uid = n.getNameOrID ();
	    nid = n.getID ();
	    umap = n.getDbMapping ();
	}
    }

    public INode getNode () {
	if (uid == null) {
	    return cache;
	} else {
	    return app.nmgr.safe.getNode (nid, umap);
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































