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
 * This includes anybody who happens to request a page from this application.
 * Depending on whether the user is logged in or not, the session holds a
 * persistent user node.
 */
public class Session implements Serializable {

    transient protected Application app;
    protected String sessionId;

    // the unique id (login name) for the user, if logged in
    protected String uid;

    // the handle to this user's persistent db node, if logged in
    protected NodeHandle userHandle;

    // the transient cache node that is exposed to javascript
    // this stays the same across logins and logouts.
    protected TransientNode cacheNode;
    protected long onSince;
    protected long lastTouched;
    protected long lastModified;

    // used to remember messages to the user between requests, mainly between redirects.
    protected String message;

    /**
     * Creates a new Session object.
     *
     * @param sessionId ...
     * @param app ...
     */
    public Session(String sessionId, Application app) {
        this.sessionId = sessionId;
        this.app = app;
        this.uid = null;
        this.userHandle = null;
        cacheNode = new TransientNode("session");
        onSince = System.currentTimeMillis();
        lastTouched = lastModified = onSince;
    }

    /**
     * Attach the given user node to this session.
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
     * Remove this sessions's user node.
     */
    public void logout() {
        userHandle = null;
        uid = null;
        lastModified = System.currentTimeMillis();
    }

    /**
     * Returns true if this session is currently associated with a user object.
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
     * Set the user handle for this session.
     */ 
    public void setUserHandle(NodeHandle handle) {
        this.userHandle = handle;
    }

    /**
     * Get the Node handle for the current user, if logged in.
     */
    public NodeHandle getUserHandle() {
        return userHandle;
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
     * Set the cache node for this session.
     */
    public void setCacheNode(TransientNode node) {
        this.cacheNode = node;
    }

    /**
     * Gets the transient cache node.
     */
    public INode getCacheNode() {
        return cacheNode;
    }

    /**
     * Get this session's application
     *
     * @return ...
     */
    public Application getApp() {
        return app;
    }

    /**
     * Set this session's application
     *
     * @param app ...
     */
    public void setApp(Application app) {
        this.app = app;
    }

    /**
     * Return this session's id.
     *
     * @return ...
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Called at the beginning of a request to let the session know it's
     * being used.
     */
    public void touch() {
        lastTouched = System.currentTimeMillis();
    }

    /**
     * Called after a request has been handled.
     *
     * @param reval the request evaluator that handled the request
     */
    public void commit(RequestEvaluator reval) {
        // nothing to do
    }

    /**
     * Returns the time this session was last touched.
     *
     * @return ...
     */
    public long lastTouched() {
        return lastTouched;
    }

    /**
     * Returns the time this session was last modified, meaning the last time
     * its user status changed or its cache node was modified.
     *
     * @return ...
     */
    public long lastModified() {
        return lastModified;
    }

    /**
     * Set the last modified time on this session.
     *
     * @param date ...
     */
    public void setLastModified(Date date) {
        if (date != null) {
            lastModified = date.getTime();
        }
    }

    /**
     * Return the time this session was created.
     *
     * @return ...
     */
    public long onSince() {
        return onSince;
    }

    /**
     * Return a string representation for this session.
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
     * Get the persistent user id of a registered user.
     * This is usually the user name, or null if the user is not logged in.
     */
    public String getUID() {
        return uid;
    }


    /**
     * Return the message that is to be displayed upon the next
     * request within this session.
     *
     * @return the message, or null if none was set.
     */
    public String getMessage() {
        return message;
    }

    /**
     * Set a message to be displayed to this session's user. This
     * can be used to save a message over to the next request when
     * the current request can't be used to display a user visible
     * message.
     *
     * @param msg
     */
    public void setMessage(String msg) {
        message = msg;
    }
}
