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

import helma.objectmodel.db.Transactor;

import javax.servlet.http.HttpServletResponse;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * 
 */
public class ResponseBean implements Serializable {
    ResponseTrans res;

    /**
     * Creates a new ResponseBean object.
     *
     * @param res ...
     */
    public ResponseBean(ResponseTrans res) {
        this.res = res;
    }

    /**
     *
     *
     * @param what ...
     */
    public void encode(Object what) {
        res.encode(what);
    }

    /**
     *
     *
     * @param what ...
     */
    public void encodeXml(Object what) {
        res.encodeXml(what);
    }

    /**
     *
     *
     * @param what ...
     */
    public void format(Object what) {
        res.format(what);
    }

    /**
     *
     *
     * @param url ...
     *
     * @throws RedirectException ...
     */
    public void redirect(String url) throws RedirectException {
        res.redirect(url);
    }

    /**
     *
     *
     * @param url ...
     *
     * @throws RedirectException ...
     */
    public void forward(String url) throws RedirectException {
        res.forward(url);
    }

    /**
     *
     */
    public void reset() {
        res.reset();
    }

    /**
     * Returns the ServletResponse instance for this Response.
     * Returns null for internal and XML-RPC requests.
     */
    public HttpServletResponse getServletResponse() {
        return res.getServletResponse();
    }

    /**
     *
     *
     * @param key ...
     * @param value ...
     */
    public void setCookie(String key, String value) {
        res.setCookie(key, value, -1, null, null);
    }

    /**
     *
     *
     * @param key ...
     * @param value ...
     * @param days ...
     */
    public void setCookie(String key, String value, int days) {
        res.setCookie(key, value, days, null, null);
    }

    /**
     *
     *
     * @param key ...
     * @param value ...
     * @param days ...
     * @param path ...
     */
    public void setCookie(String key, String value, int days, String path) {
        res.setCookie(key, value, days, path, null);
    }

    /**
     *
     *
     * @param key ...
     * @param value ...
     * @param days ...
     * @param path ...
     * @param domain ...
     */
    public void setCookie(String key, String value, int days, String path, String domain) {
        res.setCookie(key, value, days, path, domain);
    }

    /**
     *
     *
     * @param what ...
     */
    public void write(String what) {
        res.write(what);
    }

    /**
     *
     *
     * @param what ...
     */
    public void writeln(String what) {
        res.writeln(what);
    }

    /**
     *
     *
     * @param what ...
     */
    public void writeBinary(byte[] what) {
        res.writeBinary(what);
    }

    /**
     *
     *
     * @param message ...
     */
    public void debug(String message) {
        res.debug(message);
    }

    /**
     *
     *
     * @return ...
     */
    public String toString() {
        return "[Response]";
    }

    // property-related methods:
    public boolean getCache() {
        return res.cache;
    }

    /**
     *
     *
     * @param cache ...
     */
    public void setCache(boolean cache) {
        res.cache = cache;
    }

    /**
     *
     *
     * @return ...
     */
    public String getCharset() {
        return res.charset;
    }

    /**
     *
     *
     * @param charset ...
     */
    public void setCharset(String charset) {
        res.charset = charset;
    }

    /**
     *
     *
     * @return ...
     */
    public String getContentType() {
        return res.contentType;
    }

    /**
     *
     *
     * @param contentType ...
     */
    public void setContentType(String contentType) {
        res.contentType = contentType;
    }

    /**
     *
     *
     * @return ...
     */
    public Map getData() {
        return res.getResponseData();
    }

    /**
     *
     *
     * @return ...
     */
    public Map getHandlers() {
        return res.getMacroHandlers();
    }
    
    /**
     *
     *
     * @return ...
     */
    public Map getMeta() {
        return res.getMetaData();
    }

    /**
     *
     *
     * @return ...
     */
    public String getError() {
        return res.error;
    }

    /**
     *
     *
     * @return ...
     */
    public String getMessage() {
        return res.message;
    }

    /**
     *
     *
     * @param message ...
     */
    public void setMessage(String message) {
        res.message = message;
    }

    /**
     *
     *
     * @return ...
     */
    public String getRealm() {
        return res.realm;
    }

    /**
     *
     *
     * @param realm ...
     */
    public void setRealm(String realm) {
        res.realm = realm;
    }

    /**
     *
     *
     * @param arr ...
     */
    public void setSkinpath(Object[] arr) {
        res.setSkinpath(arr);
    }

    /**
     *
     *
     * @return ...
     */
    public Object[] getSkinpath() {
        return res.getSkinpath();
    }

    /**
     *
     *
     * @return ...
     */
    public int getStatus() {
        return res.status;
    }

    /**
     *
     *
     * @param status ...
     */
    public void setStatus(int status) {
        res.status = status;
    }

    /**
     *
     *
     * @return ...
     */
    public Date getLastModified() {
        long modified = res.getLastModified();

        if (modified > -1) {
            return new Date(modified);
        } else {
            return null;
        }
    }

    /**
     *
     *
     * @param date ...
     */
    public void setLastModified(Date date) {
        if (date == null) {
            res.setLastModified(-1);
        } else {
            res.setLastModified(date.getTime());
        }
    }

    /**
     *
     *
     * @return ...
     */
    public String getETag() {
        return res.getETag();
    }

    /**
     *
     *
     * @param etag ...
     */
    public void setETag(String etag) {
        res.setETag(etag);
    }

    /**
     *
     *
     * @param what ...
     */
    public void dependsOn(String what) {
        res.dependsOn(what);
    }

    /**
     *
     */
    public void digest() {
        res.digestDependencies();
    }

    /**
     * Push a string buffer on the response object. All further
     * writes will be redirected to this buffer.
     */
    public void push() {
        res.pushStringBuffer();
    }

    /**
     * Pop a string buffer from the response object containing
     * all the writes since the last pushBuffer
     *
     * @return ...
     */
    public String pop() {
        return res.popStringBuffer();
    }

    /**
     * Old version for push() kept for compatibility
     */
    public void pushStringBuffer() {
        res.pushStringBuffer();
    }

    /**
     * Old version for pop() kept for compatibility
     *
     * @return ...
     */
    public String popStringBuffer() {
        return res.popStringBuffer();
    }

    /**
     * Commit changes made during the course of the current transaction
     * and start a new one
     *
     * @throws Exception
     */
    public void commit() throws Exception {
        if (Thread.currentThread() instanceof Transactor) {
            Transactor tx = (Transactor) Thread.currentThread();
            String tname = tx.getTransactionName();
            tx.commit();
            tx.begin(tname);
        }
    }

    /**
     * Abort the current transaction by throwing an Error
     *  
     * @throws AbortException
     */
    public void abort() throws AbortException {
        throw new AbortException();
    }

}
