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

package helma.framework;

import helma.framework.core.Application;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * 
 */
public class RequestBean implements Serializable {
    RequestTrans req;

    /**
     * Creates a new RequestBean object.
     *
     * @param req ...
     */
    public RequestBean(RequestTrans req) {
        this.req = req;
    }

    /**
     *
     *
     * @param name ...
     *
     * @return ...
     */
    public Object get(String name) {
        return req.get(name);
    }

    /**
     *
     *
     * @return ...
     */
    public boolean isGet() {
        return req.isGet();
    }

    /**
     *
     *
     * @return ...
     */
    public boolean isPost() {
        return req.isPost();
    }

    /**
     *
     *
     * @return ...
     */
    public String toString() {
        return "[Request]";
    }

    // property related methods:
    public String getaction() {
        return req.action;
    }

    /**
     *
     *
     * @return ...
     */
    public Map getdata() {
        return req.getRequestData();
    }

    /**
     *
     *
     * @return ...
     */
    public long getruntime() {
        return (System.currentTimeMillis() - req.startTime);
    }

    /**
     *
     *
     * @return ...
     */
    public String getpassword() {
        return req.getPassword();
    }

    /**
     *
     *
     * @return ...
     */
    public String getpath() {
        return req.path;
    }

    /**
     *
     *
     * @return ...
     */
    public String getusername() {
        return req.getUsername();
    }

    /* public Date getLastModified () {
       long since = req.getIfModifiedSince ();
       if (since < 0)
           return null;
       else
           return new Date (since);
       }
       public void setLastModified () {
       throw new RuntimeException ("The lastModified property of the Request object is read-only. "+
               "Set lastModified on the Response object if you want to mark the last modification date of a resource.");
       } */
}
