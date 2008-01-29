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
import helma.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Cookie;
import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

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
    private final Map values = new DataComboMap();

    private ParamComboMap params;
    private ParameterMap queryParams, postParams, cookies;
    
    // the HTTP request method
    private String method;

    // timestamp of client-cached version, if present in request
    private long ifModifiedSince = -1;

    // set of ETags the client sent with If-None-Match header
    private final Set etags = new HashSet();

    // when was execution started on this request?
    private final long startTime;

    // the name of the action being invoked
    private String action;
    private String httpUsername;
    private String httpPassword;

    static private final Pattern paramPattern = Pattern.compile("\\[(.+?)\\]");

    /**
     *  Create a new Request transmitter with an empty data map.
     */
    public RequestTrans(String method, String path) {
        this.method = method;
        this.path = path;
        this.request = null;
        this.response = null;
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
        startTime = System.currentTimeMillis();

        // do standard HTTP variables
        String header = request.getHeader("Host");
        if (header != null) {
            values.put("http_host", header.toLowerCase());
        }

        header = request.getHeader("Referer");
        if (header != null) {
            values.put("http_referer", header);
        }

        try {
            long ifModifiedSince = request.getDateHeader("If-Modified-Since");
            if (ifModifiedSince > -1) {
               setIfModifiedSince(ifModifiedSince);
            }
        } catch (IllegalArgumentException ignore) {
            // not a date header
        }

        header = request.getHeader("If-None-Match");
        if (header != null) {
            setETags(header);
        }

        header = request.getRemoteAddr();
        if (header != null) {
            values.put("http_remotehost", header);
        }

        header = request.getHeader("User-Agent");
        if (header != null) {
            values.put("http_browser", header);
        }

        header = request.getHeader("Accept-Language");
        if (header != null) {
            values.put("http_language", header);
        }

        header = request.getHeader("authorization");
        if (header != null) {
            values.put("authorization", header);
        }
    }

    /**
     * Return true if we should try to handle this as XML-RPC request.
     *
     * @return true if this might be an XML-RPC request.
     */
    public synchronized boolean checkXmlRpc() {
        return "POST".equals(method) && "text/xml".equals(request.getContentType());
    }

    /**
     * Return true if this request is in fact handled as XML-RPC request.
     * This implies that {@link #checkXmlRpc()} returns true and a matching
     * XML-RPC action was found.
     *
     * @return true if this request is handled as XML-RPC request.
     */
    public synchronized boolean isXmlRpc() {
        return XMLRPC.equals(method);
    }

    /**
     * Set a cookie
     * @param name the cookie name
     * @param cookie the cookie
     */
    public void setCookie(String name, Cookie cookie) {
        if (cookies == null) {
            cookies = new ParameterMap();
        }
        cookies.put(name, cookie);
    }

    /**
     * @return a map containing the cookies sent with this request
     */
    public Map getCookies() {
        if (cookies == null) {
            cookies = new ParameterMap();
        }
        return cookies;
    }

    /**
     * @return the combined query and post parameters for this request
     */
    public Map getParams() {
        if (params == null) {
            params = new ParamComboMap();
        }
        return params;
    }

    /**
     * @return get the query parameters for this request
     */
    public Map getQueryParams() {
        if (queryParams == null) {
            queryParams = new ParameterMap();
        }
        return queryParams;
    }

    /**
     * @return get the post parameters for this request
     */
    public Map getPostParams() {
        if (postParams == null) {
            postParams = new ParameterMap();
        }
        return postParams;
    }

    /**
     * set the request parameters
     */
    public void setParameters(Map parameters, boolean isPost) {
        if (isPost) {
            postParams = new ParameterMap(parameters);
        } else {
            queryParams = new ParameterMap(parameters);
        }
    }

    /**
     * Add a post parameter to the request
     * @param name the parameter name
     * @param value the parameter value
     */
    public void addPostParam(String name, Object value) {
        if (postParams == null) {
            postParams = new ParameterMap();
        }
        Object previous = postParams.getRaw(name);
        if (previous instanceof Object[]) {
            Object[] array = (Object[]) previous;
            Object[] values = new Object[array.length + 1];
            System.arraycopy(array, 0, values, 0, array.length);
            values[array.length] = value;
            postParams.put(name, values);
        } else if (previous == null) {
            postParams.put(name, new Object[] {value});
        }
    }

    /**
     * Set a parameter value in this request transmitter. This
     * parses foo[bar][baz] as nested objects/maps.
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
     * Proxy to HttpServletRequest.getHeader().
     * @param name the header name
     * @return the header value, or null
     */
    public String getHeader(String name) {
        return request == null ? null : request.getHeader(name);
    }

    /**
     * Proxy to HttpServletRequest.getHeaders(), returns header values as string array.
     * @param name the header name
     * @return the header values as string array
     */
    public String[] getHeaders(String name) {
        return request == null ?
                null : StringUtils.collect(request.getHeaders(name));
    }

    /**
     * Proxy to HttpServletRequest.getIntHeader(), fails silently by returning -1.
     * @param name the header name
     * @return the header parsed as integer or -1
     */
    public int getIntHeader(String name) {
        try {
            return request == null ? -1 : getIntHeader(name);
        } catch (NumberFormatException nfe) {
            return -1;
        }
    }

    /**
     * Proxy to HttpServletRequest.getDateHeader(), fails silently by returning -1.
     * @param name the header name
     * @return the date in milliseconds, or -1
     */
    public long getDateHeader(String name) {
        try {
            return request == null ? -1 : getDateHeader(name);
        } catch (NumberFormatException nfe) {
            return -1;
        }
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
        if (session == null || path == null) {
            return super.hashCode();
        } else {
            return 17 + (37 * session.hashCode()) +
                        (37 * path.hashCode());
        }
    }

    /**
     * A request is considered equal to another one if it has the same method,
     * path, session, request data, and conditional get data. This is used to
     * evaluate multiple simultanous identical requests only once.
     */
    public boolean equals(Object what) {
        if (what instanceof RequestTrans) {
            if (session == null || path == null) {
                return super.equals(what);
            } else {
                RequestTrans other = (RequestTrans) what;
                return (session.equals(other.session)
                        && path.equalsIgnoreCase(other.path)
                        && values.equals(other.values)
                        && ifModifiedSince == other.ifModifiedSince
                        && etags.equals(other.etags));
            }
        }
        return false;
    }

    /**
     * Return the method of the request. This may either be a HTTP method or
     * one of the Helma pseudo methods defined in this class.
     */
    public synchronized String getMethod() {
        return method;
    }

    /**
     * Set the method of this request.
     *
     * @param method the method.
     */
    public synchronized void setMethod(String method) {
        this.method = method;
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
        int suffix = action.lastIndexOf("_action");
        this.action = suffix > -1 ? action.substring(0, suffix) : action;
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

    class ParameterMap extends SystemMap {

        public ParameterMap() {
            super();
        }

        public ParameterMap(Map map) {
            super((int) (map.size() / 0.75f) + 1);
            for (Iterator i = map.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry e = (Map.Entry) i.next();
                put(e.getKey(), e.getValue());
            }
        }

        public Object put(Object key, Object value) {
            if (key instanceof String) {
                String name = (String) key;
                int bracket = name.indexOf('[');
                if (bracket > -1 && name.endsWith("]")) {
                    Matcher matcher = paramPattern.matcher(name);
                    String partName = name.substring(0, bracket);
                    return putInternal(partName, matcher, value);
                }
            }
            Object previous = super.get(key);
            if (previous != null && (previous instanceof Map || value instanceof Map))
                throw new RuntimeException("Conflicting HTTP Parameters for '" + key + "'");
            return super.put(key, value);
        }

        private Object putInternal(String name, Matcher matcher, Object value) {
            Object previous = super.get(name);
            if (matcher.find()) {
                ParameterMap map = null;
                if (previous instanceof ParameterMap) {
                    map = (ParameterMap) previous;
                } else if (previous == null) {
                    map = new ParameterMap();
                    super.put(name, map);
                } else {
                    throw new RuntimeException("Conflicting HTTP Parameters for '" + name + "'");
                }
                String partName = matcher.group(1);
                return map.putInternal(partName, matcher, value);
            }
            if (previous != null && (previous instanceof Map || value instanceof Map))
                throw new RuntimeException("Conflicting HTTP Parameters for '" + name + "'");
            return super.put(name, value);
        }

        public Object get(Object key) {
            if (key instanceof String) {
                Object value = super.get(key);
                String name = (String) key;
                if (name.endsWith("_array") && value == null) {
                    value = super.get(name.substring(0, name.length() - 6));
                    return value instanceof Object[] ? value : null;
                } else if (name.endsWith("_cookie") && value == null) {
                    value = super.get(name.substring(0, name.length() - 7));
                    return value instanceof Cookie ? value : null;
                } else if (value instanceof Object[]) {
                    Object[] values = ((Object[]) value);
                    return values.length > 0 ? values[0] : null;
                } else if (value instanceof Cookie) {
                    Cookie cookie = (Cookie) value;
                    return cookie.getValue();
                }
            }
            return super.get(key);
        }

        protected Object getRaw(Object key) {
            return super.get(key);
        }
    }

    class DataComboMap extends SystemMap {

        public Object get(Object key) {
            Object value = super.get(key);
            if (value != null)
                return value;
            if (postParams != null && (value = postParams.get(key)) != null)
                return value;
            if (queryParams != null && (value = queryParams.get(key)) != null)
                return value;
            if (cookies != null && (value = cookies.get(key)) != null)
                return value;
            return null;
        }

        public boolean containsKey(Object key) {
            return get(key) != null;
        }

        public Set entrySet() {
            Set entries = new HashSet(super.entrySet());
            if (postParams != null) entries.addAll(postParams.entrySet());
            if (queryParams != null) entries.addAll(queryParams.entrySet());
            if (cookies != null) entries.addAll(cookies.entrySet());
            return entries;
        }

        public Set keySet() {
            Set keys = new HashSet(super.keySet());
            if (postParams != null) keys.addAll(postParams.keySet());
            if (queryParams != null) keys.addAll(queryParams.keySet());
            if (cookies != null) keys.addAll(cookies.keySet());
            return keys;
        }
    }

    class ParamComboMap extends SystemMap {
        public Object get(Object key) {
            Object value;
            if (postParams != null && (value = postParams.get(key)) != null)
                return value;
            if (queryParams != null && (value = queryParams.get(key)) != null)
                return value;
            return null;
        }

        public boolean containsKey(Object key) {
            return get(key) != null;
        }

        public Set entrySet() {
            Set entries = new HashSet();
            if (postParams != null) entries.addAll(postParams.entrySet());
            if (queryParams != null) entries.addAll(queryParams.entrySet());
            return entries;
        }

        public Set keySet() {
            Set keys = new HashSet();
            if (postParams != null) keys.addAll(postParams.keySet());
            if (queryParams != null) keys.addAll(queryParams.keySet());
            return keys;
        }
    }
}
