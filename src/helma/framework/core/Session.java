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
import helma.framework.ResponseTrans;
import helma.framework.UploadStatus;

import java.io.*;
import java.util.*;

/**
 * This represents a session currently using the Hop application.
 * This includes anybody who happens to request a page from this application.
 * Depending on whether the user is logged in or not, the session holds a
 * persistent user node.
 */
public class Session implements Serializable {

    static final long serialVersionUID = -6149094040363012913L;
    
    transient protected Application app;
    protected String sessionId;

    // the unique id (login name) for the user, if logged in
    protected String uid;

    // the handle to this user's persistent db node, if logged in
    protected NodeHandle userHandle;

    // the transient cache node that is exposed to javascript
    // this stays the same across logins and logouts.
    protected INode cacheNode;

    // timestamps for creation, last request, last modification
    protected long onSince;
    protected long lastTouched;
    protected long lastModified;
    protected long cacheLastModified;

    // used to remember messages to the user between requests, mainly between redirects.
    protected String message;
    protected StringBuffer debugBuffer;

    protected HashMap uploads = null;

    protected transient boolean modifiedInRequest = false;
    protected transient boolean registered = false;

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
        cacheLastModified = cacheNode.lastModified();
        // HACK - decrease timestamp by 1 to notice modifications
        // taking place immediately after object creation
        onSince = System.currentTimeMillis() - 1;
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
        modifiedInRequest = true;
    }

    /**
     * Try logging in this session given the userName and password.
     *
     * @param userName the user name
     * @param password the password
     * @return true if session was logged in.
     */
    public boolean login(String userName, String password) {
        if (app.loginSession(userName, password, this)) {
            lastModified = System.currentTimeMillis();
            modifiedInRequest = true;
        }
        return false;
    }

    /**
     * Remove this sessions's user node.
     */
    public void logout() {
        if (userHandle != null) {
            try {
                // Invoke User.onLogout() iff this is a transactor request with a request
                // evaluator already associated (i.e., if this is called from an app/script).
                // Otherwise, we assume being called from the scheduler thread, which takes
                // care of calling User.onLogout().
                RequestEvaluator reval = app.getCurrentRequestEvaluator();
                if (reval != null) {
                    Node userNode = userHandle.getNode(app.nmgr.safe);
                    if (userNode != null)
                        reval.invokeDirectFunction(userNode, "onLogout", new Object[] {sessionId});
                }
            } catch (Exception x) {
                // errors should already be logged by request evaluator, but you never know
                app.logError("Error in onLogout", x);
            } finally {
                // do log out
                userHandle = null;
                uid = null;
                lastModified = System.currentTimeMillis();
                modifiedInRequest = true;

            }
        }
    }

    /**
     * Returns true if this session is currently associated with a user object.
     *
     * @return ...
     */
    public boolean isLoggedIn() {
        return userHandle != null;
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
    public void setCacheNode(INode node) {
        if (node == null) {
            throw new NullPointerException("cache node is null");
        }
        this.cacheNode = node;
        this.cacheLastModified = cacheNode.lastModified();
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
    public void commit(RequestEvaluator reval, SessionManager smgr) {
        if (modifiedInRequest || cacheLastModified != cacheNode.lastModified()) {
            if (!registered) {
                smgr.registerSession(this);
                registered = true;
            }
            modifiedInRequest = false;
            cacheLastModified = cacheNode.lastModified();
        }
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
     * @param l the timestamp
     */
    public void setLastModified(long l) {
        lastModified = l;
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
     * Set the persistent user id of a registered user.
     * @param uid the user name, or null if the user is not logged in.
     */
    public void setUID(String uid) {
        this.uid = uid;
    }

    /**
     * Set the user and debug messages over from a previous response.
     * This is used for redirects, where messages can't be displayed immediately.
     * @param res the response to set the messages on
     */
    public synchronized void recoverResponseMessages(ResponseTrans res) {
        if (message != null || debugBuffer != null) {
            res.setMessage(message);
            res.setDebugBuffer(debugBuffer);
            message = null;
            debugBuffer = null;
            modifiedInRequest = true;
        }
    }

    /**
     * Remember the response's user and debug messages for a later response.
     * This is used for redirects, where messages can't be displayed immediately.
     * @param res the response to retrieve the messages from
     */
    public synchronized void storeResponseMessages(ResponseTrans res) {
        message = res.getMessage();
        debugBuffer = res.getDebugBuffer();
        if (message != null || debugBuffer != null) {
            modifiedInRequest = true;
        }
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
     * @param msg the message
     */
    public void setMessage(String msg) {
        message = msg;
    }

    /**
     * Return the debug buffer that is to be displayed upon the next
     * request within this session.
     *
     * @return the debug buffer, or null if none was set.
     */
    public StringBuffer getDebugBuffer() {
        return debugBuffer;
    }

    /**
     * Set the debug buffer to be displayed to this session's user. This
     * can be used to save the debug buffer over to the next request when
     * the current request can't be used to display a user visible
     * message.
     *
     * @param buffer the buffer
     */
    public void setDebugBuffer(StringBuffer buffer) {
        debugBuffer = buffer;
    }

    protected UploadStatus createUpload(String uploadId) {
        if (uploads == null) {
            uploads = new HashMap();
        }
        UploadStatus status = new UploadStatus();
        uploads.put(uploadId, status);
        return status;
    }

    protected UploadStatus getUpload(String uploadId) {
        if (uploads == null) {
            return null;
        } else {
            return (UploadStatus) uploads.get(uploadId);
        }
    }

    protected void pruneUploads() {
        if (uploads == null || uploads.isEmpty())
            return;
        for (Iterator it = uploads.values().iterator(); it.hasNext();) {
            UploadStatus status = (UploadStatus) it.next();
            if (status.isDisposable()) {
                it.remove();
            }
        }
    }
}
