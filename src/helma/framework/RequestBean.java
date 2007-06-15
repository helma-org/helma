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
     * Proxy to HttpServletRequest.getHeader().
     * @param name the header name
     * @return the header value, or null
     */
    public String getHeader(String name) {
        return req.getHeader(name);        
    }

    /**
     * Proxy to HttpServletRequest.getHeaders(), returns header values as string array.
     * @param name the header name
     * @return the header values as string array
     */
    public String[] getHeaders(String name) {
        return req.getHeaders(name);
    }

    /**
     * Proxy to HttpServletRequest.getIntHeader(), fails silently by returning -1.
     * @param name the header name
     * @return the header parsed as integer or -1
     */
    public int getIntHeader(String name) {
        return req.getIntHeader(name);
    }

    /**
     * Proxy to HttpServletRequest.getDateHeader(), fails silently by returning -1.
     * @param name the header name
     * @return the date in milliseconds, or -1
     */
    public long getDateHeader(String name) {
        return req.getDateHeader(name);
    }

    /**
     * @return A string representation of this request
     */
    public String toString() {
        return "[Request]";
    }

    /**
     * @return the invoked action
     */
    public String getAction() {
        return req.getAction();
    }

    /**
     * @return The req.data map containing request parameters, cookies and
     * assorted HTTP headers
     */
    public Map getData() {
        return req.getRequestData();
    }

    /**
     * @return the req.params map containing combined query and post parameters
     */
    public Map getParams() {
        return req.getParams();
    }

    /**
     * @return the req.queryParams map containing parameters parsed from the query string
     */
    public Map getQueryParams() {
        return req.getQueryParams();
    }

    /**
     * @return the req.postParams map containing params parsed from post data
     */
    public Map getPostParams() {
        return req.getPostParams();
    }

    /**
     * @return the req.cookies map containing request cookies
     */
    public Map getCookies() {
        return req.getCookies();
    }

    /**
     * @return the time this request has been running, in milliseconds
     */
    public long getRuntime() {
        return (System.currentTimeMillis() - req.getStartTime());
    }

    /**
     * @return the password if using HTTP basic authentication
     */
    public String getPassword() {
        return req.getPassword();
    }

    /**
     * @return the request path
     */
    public String getPath() {
        return req.getPath();
    }

    /**
     * @return the username if using HTTP basic authentication
     */
    public String getUsername() {
        return req.getUsername();
    }

}
