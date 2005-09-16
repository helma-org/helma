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
     * @param res the wrapped ResponseTrans
     */
    public ResponseBean(ResponseTrans res) {
        this.res = res;
    }

    /**
     * Write an object to the response buffer by converting it to a string
     * and then HTML-encoding it.
     *
     * @param obj the object to write to the response buffer 
     */
    public void encode(Object obj) {
        res.encode(obj);
    }

    /**
     * Write an object to the response buffer by converting it to a string
     * and then XML-encoding it.
     *
     * @param obj the object to write to the response buffer 
     */
    public void encodeXml(Object obj) {
        res.encodeXml(obj);
    }

    /**
     * Write an object to the response buffer by converting it to a string
     * and then HTML-formatting it.
     *
     * @param obj the object to write to the response buffer 
     */
    public void format(Object obj) {
        res.format(obj);
    }

    /**
     * Redirect the request to a different URL
     *
     * @param url the URL to redirect to
     * @throws RedirectException to immediately terminate the request
     */
    public void redirect(String url) throws RedirectException {
        res.redirect(url);
    }

    /**
     * Internally forward the request to a different URL
     *
     * @param url the URL to forward to
     * @throws RedirectException to immediately terminate the request
     */
    public void forward(String url) throws RedirectException {
        res.forward(url);
    }

    /**
     * Reset the response object, clearing all content previously written to it
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
     * Set a HTTP cookie with the name and value that is discarded when the 
     * HTTP client is closed
     *
     * @param key the cookie name
     * @param value the cookie value
     */
    public void setCookie(String key, String value) {
        res.setCookie(key, value, -1, null, null);
    }

    /**
     * Set a HTTP cookie with the name and value that is stored by the 
     * HTTP client for the given number of days. A days value of 0 means the
     * cookie should be immediately discarded.
     *
     * @param key the cookie name
     * @param value the cookie value
     * @param days number of days the cookie should be stored
     */
    public void setCookie(String key, String value, int days) {
        res.setCookie(key, value, days, null, null);
    }

    /**
     * Set a HTTP cookie with the name and value that is only applied to 
     * the URLs matching the given path and is stored by the 
     * HTTP client for the given number of days. A days value of 0 means the
     * cookie should be immediately discarded.
     *
     * @param key the cookie name
     * @param value the cookie value
     * @param days number of days the cookie should be stored
     * @param path the URL path to apply the cookie to
     */
    public void setCookie(String key, String value, int days, String path) {
        res.setCookie(key, value, days, path, null);
    }

    /**
     * Set a HTTP cookie with the name and value that is only applied to 
     * the URLs matching the given path and is stored by the 
     * HTTP client for the given number of days. A days value of 0 means the
     * cookie should be immediately discarded.
     *
     * @param key the cookie name
     * @param value the cookie value
     * @param days number of days the cookie should be stored
     * @param path the URL path to apply the cookie to
     * @param domain domain
     */
    public void setCookie(String key, String value, int days, String path, String domain) {
        res.setCookie(key, value, days, path, domain);
    }
    
    /**
     * Unset a previously set HTTP cookie, causing it to be discarded immedialtely by the 
     * HTTP client.
     *
     * @param key the name of the cookie to be discarded
     */
    public void unsetCookie(String key) {
        res.setCookie(key, "", 0, null, null);
    }

    /**
     * Directly write a string to the response buffer without any transformation.
     *
     * @param str the string to write to the response buffer
     */
    public void write(String str) {
        res.write(str);
    }

    /**
     * Write string to response buffer and append a platform dependent newline sequence.
     *
     * @param str the string to write to the response buffer
     */
    public void writeln(String str) {
        res.writeln(str);
    }

    /**
     * Directly write a byte array to the response buffer without any transformation.
     *
     * @param bytes the string to write to the response buffer
     */
    public void writeBinary(byte[] bytes) {
        res.writeBinary(bytes);
    }

    /**
     * add an HTML formatted debug message to the end of the page.
     *
     * @param message the message
     */
    public void debug(String message) {
        res.debug(message);
    }

    /**
     * Return a string representation for this object
     *
     * @return string representation
     */
    public String toString() {
        return "[Response]";
    }

    // property-related methods
    
    /**
     * Return the current cachability setting for this response
     * 
     * @return true if the response may be cached by the HTTP client, false otherwise
     */
    public boolean getCache() {
        return res.cache;
    }

    /**
     * Set true cachability setting for this response
     *
     * @param cache true if the response may be cached by the HTTP client, false otherwise
     */
    public void setCache(boolean cache) {
        res.cache = cache;
    }

    /**
     * Get the current charset/encoding name for the response
     *
     * @return The charset name
     */
    public String getCharset() {
        return res.charset;
    }

    /**
     * Set the charset/encoding name for the response
     *
     * @param charset The charset name
     */
    public void setCharset(String charset) {
        res.charset = charset;
    }

    /**
     * Get the current content type name for the response
     *
     * @return the content type
     */
    public String getContentType() {
        return res.contentType;
    }

    /**
     * Set the content type for the response
     *
     * @param contentType The charset name
     */
    public void setContentType(String contentType) {
        res.contentType = contentType;
    }

    /**
     * Get the data map for the response
     *
     * @return the data object
     */
    public Map getData() {
        return res.getResponseData();
    }

    /**
     * Get the macro handlers map for the response
     *
     * @return the macro handlers map
     */
    public Map getHandlers() {
        return res.getMacroHandlers();
    }
    
    /**
     * Get the meta map for the response
     *
     * @return the meta map
     */
    public Map getMeta() {
        return res.getMetaData();
    }

    /**
     * Get the current error message for the response, if any
     *
     * @return the error message
     */
    public String getError() {
        return res.error;
    }

    /**
     * Get the current message for the response, if set
     *
     * @return the message
     */
    public String getMessage() {
        return res.message;
    }

    /**
     * Set the message property for the response
     *
     * @param message the message property
     */
    public void setMessage(String message) {
        res.message = message;
    }

    /**
     * Get the HTTP authentication realm for the response
     *
     * @return the HTTP authentication realm
     */
    public String getRealm() {
        return res.realm;
    }

    /**
     * Set the HTTP authentication realm for the response
     *
     * @param realm the HTTP authentication realm
     */
    public void setRealm(String realm) {
        res.realm = realm;
    }

    /**
     * Set the skin search path for the response
     *
     * @param arr an array containing files or nodes containing skins
     */
    public void setSkinpath(Object[] arr) {
        res.setSkinpath(arr);
    }

    /**
     * Get the skin search path for the response
     *
     * @return The array of files or nodes used to search for skins
     */
    public Object[] getSkinpath() {
        return res.getSkinpath();
    }

    /**
     * Get the HTTP status code for this response
     *
     * @return the HTTP status code
     */
    public int getStatus() {
        return res.status;
    }

    /**
     * Set the HTTP status code for this response
     *
     * @param status the HTTP status code
     */
    public void setStatus(int status) {
        res.status = status;
    }

    /**
     * Get the last modified date for this response
     *
     * @return the last modified date
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
     * Set the last modified date for this response
     *
     * @param date the last modified date
     */
    public void setLastModified(Date date) {
        if (date == null) {
            res.setLastModified(-1);
        } else {
            res.setLastModified(date.getTime());
        }
    }

    /**
     * Get the ETag for this response
     *
     * @return the HTTP etag
     */
    public String getETag() {
        return res.getETag();
    }

    /**
     * Set the HTTP Etag for this response
     *
     * @param etag the HTTP ETag
     */
    public void setETag(String etag) {
        res.setETag(etag);
    }

    /**
     * Add an item to this response's dependencies. If no dependency has changed between 
     * requests, an HTTP not-modified response will be generated.
     *
     * @param what a string item this response depends on
     */
    public void dependsOn(String what) {
        res.dependsOn(what);
    }

    /**
     * Digest this response's dependencies to conditionally create a HTTP not-modified response
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
