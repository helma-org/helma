// User.java
// Copyright (c) Hannes Wallnöfer 1998-2000

package helma.framework.core;

import java.io.*;
import java.util.*;
import java.net.URLEncoder;
import helma.objectmodel.*;
import helma.objectmodel.db.*;

/**
 * This represents a user who is currently using the Hop application. This 
 * object links between several sessions and a single username.
 */
 
public class User	{

    NodeHandle nodehandle;
    String uid;
    Hashtable sessions;

    public User (NodeHandle nodehandle, String uid, Session session)	{
    	this.nodehandle = nodehandle;
    	this.uid = uid;
    	sessions = new Hashtable();
    	sessions.put (session.sessionID, session);
    }

	public NodeHandle getUserNodeHandle()	{
		return nodehandle;
	}

	public void addSession (Session session)	{
    	sessions.put (session.sessionID, session);
	}

	public int removeSession (Session session)	{
		sessions.remove (session.sessionID);
		return sessions.size();
	}

    public Hashtable getSessions()	{
    	return sessions;
    }

	public int countSessions()	{
		return sessions.size();
	}

}

