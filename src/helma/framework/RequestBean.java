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

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
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
     * Return the method of the request. This may either be a HTTP method or
     * one of the Helma pseudo methods defined in RequestTrans.
     */
    public String getMethod() {
        return req.getMethod();
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
     * Returns the Servlet request represented by this RequestTrans instance.
     * Returns null for internal and XML-RPC requests.
     */
    public HttpServletRequest getServletRequest() {
        return req.getServletRequest();
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
    public String getAction() {
        return req.getAction();
    }

    /**
     *
     *
     * @return ...
     */
    public Map getData() {
        return req.getRequestData();
    }

    /**
     *
     *
     * @return ...
     */
    public long getRuntime() {
        return (System.currentTimeMillis() - req.getStartTime());
    }

    /**
     *
     *
     * @return ...
     */
    public String getPassword() {
        return req.getPassword();
    }

    /**
     *
     *
     * @return ...
     */
    public String getPath() {
        return req.path;
    }

    /**
     *
     *
     * @return ...
     */
    public String getUsername() {
        return req.getUsername();
    }

}
