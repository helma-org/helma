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

import helma.util.Base64;
import helma.util.SystemMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;

/**
 * A Transmitter for a request from the servlet client. Objects of this
 * class are directly exposed to JavaScript as global property req.
 */
public class RequestTrans implements Serializable {

    static final long serialVersionUID = 5398880083482000580L;

    // HTTP methods
    public final static String GET = "GET";
    public final static String POST = "POST";
    public final static String DELETE = "DELETE";
    public final static String HEAD = "HEAD";
    public final static String OPTIONS = "OPTIONS";
    public final static String PUT = "PUT";
    public final static String TRACE = "TRACE";
    // Helma pseudo-methods
    public final static String XMLRPC = "XMLRPC";
    public final static String EXTERNAL = "EXTERNAL";
    public final static String INTERNAL = "INTERNAL";

    // the servlet request and response, may be null
    final HttpServletRequest request;
    final HttpServletResponse response;

    // the uri path of the request
    private final String path;

    // the request's session id
    private String session;

    // the map of form and cookie data
    private final Map values;
    
    // the HTTP request method
    private final String method;

    // timestamp of client-cached version, if present in request
    private long ifModifiedSince = -1;

    // set of ETags the client sent with If-None-Match header
    private Set etags;

    // when was execution started on this request?
    private final long startTime;

    // true if this might be an XML-RPC request
    private boolean isXmlRpc;

    // the name of the action being invoked
    private String action;
    private String httpUsername;
    private String httpPassword;

    /**
     *  Create a new Request transmitter with an empty data map.
     */
    public RequestTrans(String method, String path) {
        this.method = method;
        this.path = path;
        this.request = null;
        this.response = null;
        values = new SystemMap();
        startTime = System.currentTimeMillis();
    }

    /**
     *  Create a new request transmitter with the given data map.
     */
    public RequestTrans(HttpServletRequest request,
                        HttpServletResponse response, String path) {
        this.method = request.getMethod();
        this.request = request;
        this.response = response;
        this.path = path;
        values = new SystemMap();
        startTime = System.currentTimeMillis();
        if ("POST".equals(method) && "text/xml".equals(request.getContentType())) {
            isXmlRpc = true;
        }
    }

    /**
     * Return true if we should try to handle this as XML-RPC request.
     *
     * @return true if this might be an XML-RPC request.
     */
    public synchronized boolean isXmlRpc() {
        return isXmlRpc;
    }

    /**
     * Set the isXmlRpc flag
     *
     * @param xmlrpc true if this is infact an XML-RPC request
     */
    public synchronized void setXmlRpc(boolean xmlrpc) {
        isXmlRpc = xmlrpc;
    }

    /**
     *  Set a parameter value in this request transmitter.
     */
    public void set(String name, Object value) {
        values.put(name, value);
    }

    /**
     *  Get a value from the requests map by key.
     */
    public Object get(String name) {
        try {
            return values.get(name);
        } catch (Exception x) {
            return null;
        }
    }

    /**
     *  Get the data map for this request transmitter.
     */
    public Map getRequestData() {
        return values;
    }

    /**
     * Returns the Servlet request represented by this RequestTrans instance.
     * Returns null for internal and XML-RPC requests.
     */
    public HttpServletRequest getServletRequest() {
        return request;
    }

    /**
     * Returns the Servlet response for this request.
     * Returns null for internal and XML-RPC requests.
     */
    public HttpServletResponse getServletResponse() {
        return response;
    }

    /**
     *  The hash code is computed from the session id if available. This is used to
     *  detect multiple identic requests.
     */
    public int hashCode() {
        if (session == null || path == null)
            return super.hashCode();
        return 17 + (37 * session.hashCode()) +
                    (37 * path.hashCode());
    }

    /**
     * A request is considered equal to another one if it has the same user, path,
     * and request data. This is used to evaluate multiple simultanous requests only once
     */
    public boolean equals(Object what) {
        try {
            RequestTrans other = (RequestTrans) what;

            return (session.equals(other.session) && path.equalsIgnoreCase(other.path) &&
                   values.equals(other.getRequestData()));
        } catch (Exception x) {
            return false;
        }
    }

    /**
     * Return the method of the request. This may either be a HTTP method or
     * one of the Helma pseudo methods defined in this class.
     */
    public String getMethod() {
        return method;
    }

    /**
     *  Return true if this object represents a HTTP GET Request.
     */
    public boolean isGet() {
        return GET.equalsIgnoreCase(method);
    }

    /**
     *  Return true if this object represents a HTTP GET Request.
     */
    public boolean isPost() {
        return POST.equalsIgnoreCase(method);
    }

    /**
     * Get the request's session id
     */
    public String getSession() {
        return session;
    }

    /**
     * Set the request's session id
     */
    public void setSession(String session) {
        this.session = session;
    }

    /**
     * Get the request's path
     */
    public String getPath() {
        return path;
    }

    /**
     * Get the request's action.
     */
    public String getAction() {
        return action;
    }

    /**
     * Set the request's action.
     */
    public void setAction(String action) {
        this.action = action;
    }

    /**
     * Get the time the request was created.
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     *
     *
     * @param since ...
     */
    public void setIfModifiedSince(long since) {
        ifModifiedSince = since;
    }

    /**
     *
     *
     * @return ...
     */
    public long getIfModifiedSince() {
        return ifModifiedSince;
    }

    /**
     *
     *
     * @param etagHeader ...
     */
    public void setETags(String etagHeader) {
        etags = new HashSet();

        if (etagHeader.indexOf(",") > -1) {
            StringTokenizer st = new StringTokenizer(etagHeader, ", \r\n");

            while (st.hasMoreTokens())
                etags.add(st.nextToken());
        } else {
            etags.add(etagHeader);
        }
    }

    /**
     *
     *
     * @return ...
     */
    public Set getETags() {
        return etags;
    }

    /**
     *
     *
     * @param etag ...
     *
     * @return ...
     */
    public boolean hasETag(String etag) {
        if ((etags == null) || (etag == null)) {
            return false;
        }

        return etags.contains(etag);
    }

    /**
     *
     *
     * @return ...
     */
    public String getUsername() {
        if (httpUsername != null) {
            return httpUsername;
        }

        String auth = (String) get("authorization");

        if ((auth == null) || "".equals(auth)) {
            return null;
        }

        decodeHttpAuth(auth);

        return httpUsername;
    }

    /**
     *
     *
     * @return ...
     */
    public String getPassword() {
        if (httpPassword != null) {
            return httpPassword;
        }

        String auth = (String) get("authorization");

        if ((auth == null) || "".equals(auth)) {
            return null;
        }

        decodeHttpAuth(auth);

        return httpPassword;
    }

    private void decodeHttpAuth(String auth) {
        if (auth == null) {
            return;
        }

        StringTokenizer tok;

        if (auth.startsWith("Basic ")) {
            tok = new StringTokenizer(new String(Base64.decode((auth.substring(6)).toCharArray())),
                                      ":");
        } else {
            tok = new StringTokenizer(new String(Base64.decode(auth.toCharArray())), ":");
        }

        try {
            httpUsername = tok.nextToken();
        } catch (NoSuchElementException e) {
            httpUsername = null;
        }

        try {
            httpPassword = tok.nextToken();
        } catch (NoSuchElementException e) {
            httpPassword = null;
        }
    }
}
