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

import helma.objectmodel.INode;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;

/**
 * 
 */
public class SessionBean implements Serializable {
    // the wrapped session object
    Session session;

    /**
     * Creates a new SessionBean object.
     *
     * @param session ...
     */
    public SessionBean(Session session) {
        this.session = session;
    }

    /**
     *
     *
     * @return ...
     */
    public String toString() {
        return session.toString();
    }

    /**
     *
     *
     * @param username ...
     * @param password ...
     *
     * @return ...
     */
    public boolean login(String username, String password) {
        boolean success = session.getApp().loginSession(username, password, session);

        return success;
    }

    /**
     *
     */
    public void logout() {
        session.getApp().logoutSession(session);
    }

    /**
     *
     */
    public void touch() {
        session.touch();
    }

    /**
     *
     *
     * @return ...
     */
    public Date lastActive() {
        return new Date(session.lastTouched());
    }

    /**
     *
     *
     * @return ...
     */
    public Date onSince() {
        return new Date(session.onSince());
    }

    // property-related methods:
    public INode getdata() {
        return session.getCacheNode();
    }

    /**
     *
     *
     * @return ...
     */
    public INode getuser() {
        return session.getUserNode();
    }

    /**
     *
     *
     * @return ...
     */
    public String get_id() {
        return session.getSessionID();
    }

    /**
     *
     *
     * @return ...
     */
    public String getcookie() {
        return session.getSessionID();
    }

    /**
     *
     *
     * @return ...
     */
    public Date getlastActive() {
        return new Date(session.lastTouched());
    }

    /**
     *
     *
     * @return ...
     */
    public Date getonSince() {
        return new Date(session.onSince());
    }

    /**
     *
     *
     * @return ...
     */
    public Date getLastModified() {
        return new Date(session.lastModified());
    }

    /**
     *
     *
     * @param date ...
     */
    public void setLastModified(Date date) {
        session.setLastModified(date);
    }
}
