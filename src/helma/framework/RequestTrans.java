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

import helma.objectmodel.*;
import helma.util.Base64;
import java.io.*;
import java.util.*;

/**
 * A Transmitter for a request from the servlet client. Objects of this
 * class are directly exposed to JavaScript as global property req.
 */
public class RequestTrans implements Externalizable {
    static final long serialVersionUID = 5398880083482000580L;

    // the uri path of the request
    public String path;

    // the request's session id
    public String session;

    // the map of form and cookie data
    private Map values;

    // the request method - 0 for GET, 1 for POST
    private byte httpMethod = 0;

    // timestamp of client-cached version, if present in request
    private long ifModifiedSince = -1;

    // set of ETags the client sent with If-None-Match header
    private Set etags;

    // when was execution started on this request?
    public transient long startTime;

    // the name of the action being invoked
    public transient String action;
    private transient String httpUsername;
    private transient String httpPassword;

    /**
     *  Create a new Request transmitter with an empty data map.
     */
    public RequestTrans() {
        httpMethod = 0;
        values = new HashMap();
    }

    /**
     *  Create a new request transmitter with the given data map.
     */
    public RequestTrans(byte method) {
        httpMethod = method;
        values = new HashMap();
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
     *  The hash code is computed from the session id if available. This is used to
     *  detect multiple identic requests.
     */
    public int hashCode() {
        return (session == null) ? super.hashCode() : session.hashCode();
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
     *  Return true if this object represents a HTTP GET Request.
     */
    public boolean isGet() {
        return httpMethod == 0;
    }

    /**
     *  Return true if this object represents a HTTP GET Request.
     */
    public boolean isPost() {
        return httpMethod == 1;
    }

    /**
     * Custom externalization code for quicker serialization.
     */
    public void readExternal(ObjectInput s) throws ClassNotFoundException, IOException {
        path = s.readUTF();
        session = s.readUTF();
        values = (Map) s.readObject();
        httpMethod = s.readByte();
        ifModifiedSince = s.readLong();
        etags = (Set) s.readObject();
    }

    /**
     * Custom externalization code for quicker serialization.
     */
    public void writeExternal(ObjectOutput s) throws IOException {
        s.writeUTF(path);
        s.writeUTF(session);
        s.writeObject(values);
        s.writeByte(httpMethod);
        s.writeLong(ifModifiedSince);
        s.writeObject(etags);
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
