/*
 * Helma License Notice
 *
 * The contents of this file are subject to the Helma License
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://adele.helma.org/download/helma/license.txt
 *
 * Copyright 1998-2003 Helma Software. All Rights Reserved.
 *
 * $RCSfile$
 * $Author$
 * $Revision$
 * $Date$
 */

package helma.framework.core;

import helma.objectmodel.*;
import helma.objectmodel.db.*;
import java.io.*;
import java.util.*;

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
    long onSince;
    long lastTouched;
    long lastModified;

    // used to remember messages to the user between requests -
    // used for redirects.
    String message;

    /**
     * Creates a new Session object.
     *
     * @param sessionID ...
     * @param app ...
     */
    public Session(String sessionID, Application app) {
        this.sessionID = sessionID;
        this.app = app;
        this.uid = null;
        this.userHandle = null;
        cacheNode = new TransientNode("session");
        onSince = System.currentTimeMillis();
        lastTouched = lastModified = onSince;
    }

    /**
     * attach the given user node to this session.
     */
    public void login(INode usernode) {
        if (usernode == null) {
            userHandle = null;
            uid = null;
        } else {
            userHandle = ((Node) usernode).getHandle();
            uid = usernode.getElementName();
        }

        lastModified = System.currentTimeMillis();
    }

    /**
     * Try logging in this session given the userName and password.
     *
     * @param userName
     * @param password
     * @return true if session was logged in.
     */
    public boolean login(String userName, String password) {
        return app.loginSession(userName, password, this);
    }

    /**
     * remove this sessions's user node.
     */
    public void logout() {
        userHandle = null;
        uid = null;
        lastModified = System.currentTimeMillis();
    }

    /**
     *
     *
     * @return ...
     */
    public boolean isLoggedIn() {
        if ((userHandle != null) && (uid != null)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Gets the user Node from this Application's NodeManager.
     */
    public INode getUserNode() {
        if (userHandle != null) {
            return userHandle.getNode(app.getWrappedNodeManager());
        } else {
            return null;
        }
    }

    /**
     * Gets the transient cache node.
     */
    public INode getCacheNode() {
        return cacheNode;
    }

    /**
     *
     *
     * @return ...
     */
    public Application getApp() {
        return app;
    }

    /**
     *
     *
     * @param app ...
     */
    public void setApp(Application app) {
        this.app = app;
    }

    /**
     *
     *
     * @return ...
     */
    public String getSessionID() {
        return sessionID;
    }

    /**
     *
     */
    public void touch() {
        lastTouched = System.currentTimeMillis();
    }

    /**
     *
     *
     * @return ...
     */
    public long lastTouched() {
        return lastTouched;
    }

    /**
     *
     *
     * @return ...
     */
    public long lastModified() {
        return lastModified;
    }

    /**
     *
     *
     * @param date ...
     */
    public void setLastModified(Date date) {
        if (date != null) {
            lastModified = date.getTime();
        }
    }

    /**
     *
     *
     * @return ...
     */
    public long onSince() {
        return onSince;
    }

    /**
     *
     *
     * @return ...
     */
    public String toString() {
        if (uid != null) {
            return "[Session for user " + uid + "]";
        } else {
            return "[Anonymous Session]";
        }
    }

    /**
     * Get the persistent user id of a registered user. This is usually the user name, or
     * null if the user is not logged in.
     */
    public String getUID() {
        return uid;
    }
}
