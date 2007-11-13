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

import helma.framework.core.Skin;
import helma.framework.core.Application;
import helma.util.*;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.security.*;
import java.util.*;

import org.apache.xmlrpc.XmlRpcResponseProcessor;

/**
 * A Transmitter for a response to the servlet client. Objects of this
 * class are directly exposed to JavaScript as global property res.
 */
public final class ResponseTrans extends Writer implements Serializable {

    static final long serialVersionUID = -8627370766119740844L;
    static final int INITIAL_BUFFER_SIZE = 2048;

    static final String newLine = System.getProperty("line.separator");

    //  MIME content type of the response.
    private String contentType = "text/html";

    // Charset (encoding) to use for the response.
    private String charset;

    // Used to allow or disable client side caching
    private boolean cacheable = true;

    // HTTP response code, defaults to 200 (OK).
    private int status = 200;

    // HTTP authentication realm
    private String realm;

    // the actual response
    private byte[] response = null;

    // contains the redirect URL
    private String redir = null;

    // the forward (internal redirect) URL
    private String forward = null;

    // the last-modified date, if it should be set in the response
    private long lastModified = -1;

    // flag to signal that resource has not been modified
    private boolean notModified = false;

    // Entity Tag for this response, used for conditional GETs
    private String etag = null;

    // cookies
    Map cookies;

    // the buffer used to build the response
    private transient StringBuffer buffer = null;

    // an idle StringBuffer waiting to be reused
    private transient StringBuffer cachedBuffer = null;

    // these are used to implement the _as_string variants for Hop templates.
    private transient Stack buffers;

    // the path used to tell where to look for skins
    private transient Object[] skinpath = null;

    // hashmap for skin caching
    private transient HashMap skincache;

    // buffer for debug messages - will be automatically appended to response
    private transient StringBuffer debugBuffer;

    // field for generic message to be displayed
    private transient String message;

    // field for error message
    private transient String error;

    // the res.data map of form and cookie data
    private transient Map values = new SystemMap();

    // the res.handlers map of macro handlers
    private transient Map handlers = new SystemMap();

    // the res.meta map for meta response data
    private transient Map meta = new SystemMap();

    // the request trans for this response
    private transient RequestTrans reqtrans;

    // the message digest used to generate composed digests for ETag headers
    private transient MessageDigest digest;

    // the skin current or last rendered skin
    private transient volatile Skin activeSkin;

    // the application
    Application app;


    /**
     * Creates a new ResponseTrans object.
     *
     * @param req the RequestTrans for this response
     */
    public ResponseTrans(Application app, RequestTrans req) {
        this.app = app;
        reqtrans = req;
    }

    /**
     *  Get a value from the responses map by key.
     */
    public Object get(String name) {
        try {
            return values.get(name);
        } catch (Exception x) {
            return null;
        }
    }

    /**
     *  Get the data map for this response transmitter.
     */
    public Map getResponseData() {
        return values;
    }

    /**
     *  Get the macro handlers map for this response transmitter.
     */
    public Map getMacroHandlers() {
        return handlers;
    }

    /**
     *  Get the meta info map for this response transmitter.
     */
    public Map getMetaData() {
        return meta;
    }

    /**
     * Returns the ServletResponse instance for this ResponseTrans.
     * Returns null for internal and XML-RPC requests.
     */
    public HttpServletResponse getServletResponse() {
        return reqtrans.getServletResponse();
    }

    /**
     * Reset the response object to its initial empty state.
     */
    public synchronized void reset() {
        if (buffer != null) {
            buffer.setLength(0);
        }

        buffers = null;
        response = null;
        cacheable = true;
        redir = forward = message = error = null;
        etag = realm = charset = null;
        contentType =  "text/html";
        values.clear();
        handlers.clear();
        meta.clear();
        lastModified = -1;
        notModified = false;
        skinpath = null;
        skincache = null;
        cookies = null;

        if (digest != null) {
            digest.reset();
        }
    }

    /**
     * This is called before a skin is rendered as string
     * (renderSkinAsString) to redirect the output to a new
     * string buffer.
     * @param buf the StringBuffer to use, or null
     * @return the new StringBuffer instance
     */
    public synchronized StringBuffer pushBuffer(StringBuffer buf) {
        if (buffers == null) {
            buffers = new Stack();
        }

        if (buffer != null) {
            buffers.push(buffer);
        }

        if (buf != null) {
            buffer = buf;
        } else if (cachedBuffer != null) {
            buffer = cachedBuffer;
            cachedBuffer = null;
        } else {
            buffer = new StringBuffer(64);
        }
        return buffer;
    }

    /**
     * Returns the content of the current string buffer and switches back to the previos one.
     */
    public synchronized String popString() {
        StringBuffer buf = popBuffer();
        String str = buf.toString();
        // store stringbuffer for later reuse
        buf.setLength(0);
        cachedBuffer = buf;
        return str;
    }

    public synchronized StringBuffer popBuffer() {
        if (buffer == null) {
            throw new RuntimeException("Can't pop string buffer: buffer is null");
        } else if (buffers == null) {
            throw new RuntimeException("Can't pop string buffer: buffer stack is empty");
        }
        // get local reference
        StringBuffer buf = buffer;
        // restore the previous buffer, which may be null
        buffer = buffers.empty() ? null : (StringBuffer) buffers.pop();
        return buf;
    }

    /**
     *  Get the response buffer, creating it if it doesn't exist
     */
    public synchronized StringBuffer getBuffer() {
        if (buffer == null) {
            buffer = new StringBuffer(INITIAL_BUFFER_SIZE);
        }

        return buffer;
    }

    /**
     * Append a string to the response unchanged.
     */
    public synchronized void write(String str) {
        if (str != null) {
            if (buffer == null) {
                buffer = new StringBuffer(Math.max(str.length() + 100, INITIAL_BUFFER_SIZE));
            }
            buffer.append(str);
        }
    }

    /**
     * Appends a objct to the response unchanged.
     * The object is first converted to a string.
     */
    public void write(Object what) {
        if (what != null) {
            write(what.toString());
        }
    }

    /**
     *  Appends a part from a char array to the response buffer.
     *
     * @param chars
     * @param offset
     * @param length
     */
    public synchronized void write(char[] chars, int offset, int length) {
        if (buffer == null) {
            buffer = new StringBuffer(Math.max(length + 100, INITIAL_BUFFER_SIZE));
        }
        buffer.append(chars, offset, length);
    }

    /**
     *  Appends a char array to the response buffer.
     *
     * @param chars
     */
    public void write(char chars[]) {
        write(chars, 0, chars.length);
    }


    /**
     * Appends a signle character to the response buffer.
     * @param c
     */
    public synchronized void write(int c) {
        if (buffer == null) {
            buffer = new StringBuffer(INITIAL_BUFFER_SIZE);
        }
        buffer.append((char) c);
    }

    /**
     * Appends a part from a string to the response buffer.
     * @param str
     * @param offset
     * @param length
     */
    public void write(String str, int offset, int length) {
        char cbuf[]  = new char[length];
        str.getChars(offset, (offset + length), cbuf, 0);
        write(cbuf, 0, length);
    }

    /**
     * Write object to response buffer and append a platform dependent newline sequence.
     */
    public synchronized void writeln(Object what) {
        if (what != null) {
            write(what.toString());
        } else if (buffer == null) {
            // if what is null, buffer may still be uninitialized
            buffer = new StringBuffer(INITIAL_BUFFER_SIZE);
        }
        buffer.append(newLine);
    }

    /**
     * Writes a platform dependent newline sequence to response buffer.
     */
    public synchronized void writeln() {
        // buffer may still be uninitialized
        if (buffer == null) {
            buffer = new StringBuffer(INITIAL_BUFFER_SIZE);
        }
        buffer.append(newLine);
    }

    /**
     *  Insert string somewhere in the response buffer. Caller has to make sure
     *  that buffer exists and its length is larger than offset. str may be null, in which
     *  case nothing happens.
     */
    public void debug(Object message) {
        if (debugBuffer == null) {
            debugBuffer = new StringBuffer();
        }

        String str = (message == null) ? "null" : message.toString();

        debugBuffer.append("<div class=\"helma-debug-line\" style=\"background: yellow; ");
        debugBuffer.append("color: black; border-top: 1px solid black;\">");
        debugBuffer.append(str);
        debugBuffer.append("</div>");
    }

    /**
     * Replace special characters with entities, including <, > and ", thus allowing
     * no HTML tags.
     */
    public synchronized void encode(Object what) {
        if (what != null) {
            String str = what.toString();

            if (buffer == null) {
                buffer = new StringBuffer(Math.max(str.length() + 100, INITIAL_BUFFER_SIZE));
            }

            HtmlEncoder.encodeAll(str, buffer);
        }
    }

    /**
     * Replace special characters with entities but pass through HTML tags
     */
    public synchronized void format(Object what) {
        if (what != null) {
            String str = what.toString();

            if (buffer == null) {
                buffer = new StringBuffer(Math.max(str.length() + 100, INITIAL_BUFFER_SIZE));
            }

            HtmlEncoder.encode(str, buffer);
        }
    }

    /**
     * Replace special characters with entities, including <, > and ", thus allowing
     * no HTML tags.
     */
    public synchronized void encodeXml(Object what) {
        if (what != null) {
            String str = what.toString();

            if (buffer == null) {
                buffer = new StringBuffer(Math.max(str.length() + 100, INITIAL_BUFFER_SIZE));
            }

            HtmlEncoder.encodeXml(str, buffer);
        }
    }

    /**
     * Encode HTML entities, but leave newlines alone. This is for the content of textarea forms.
     */
    public synchronized void encodeForm(Object what) {
        if (what != null) {
            String str = what.toString();

            if (buffer == null) {
                buffer = new StringBuffer(Math.max(str.length() + 100, INITIAL_BUFFER_SIZE));
            }

            HtmlEncoder.encodeAll(str, buffer, false);
        }
    }

    /**
     *
     *
     * @param url ...
     *
     * @throws RedirectException ...
     */
    public void redirect(String url) throws RedirectException {
        // remove newline chars to prevent response splitting attack
        redir = url == null ?
                null : url.replaceAll("[\r\n]", "");
        throw new RedirectException(redir);
    }

    /**
     *
     *
     * @return ...
     */
    public String getRedirect() {
        return redir;
    }

    /**
     *
     *
     * @param url ...
     *
     * @throws RedirectException ...
     */
    public void forward(String url) throws RedirectException {
        // remove newline chars to prevent response splitting attack
        forward = url == null ?
                null : url.replaceAll("[\r\n]", "");
        throw new RedirectException(forward);
    }

    /**
     *
     *
     * @return ...
     */
    public String getForward() {
        return forward;
    }

    /**
     *  Allow to directly set the byte array for the response. Calling this more than once will
     *  overwrite the previous output. We take a generic object as parameter to be able to
     * generate a better error message, but it must be byte[].
     */
    public void writeBinary(byte[] what) {
        response = what;
    }

    /**
     * Proxy to HttpServletResponse.addHeader()
     * @param name the header name
     * @param value the header value
     */
    public void addHeader(String name, String value) {
        HttpServletResponse res = getServletResponse();
        if (res != null)
            res.addHeader(name, value);
    }

    /**
     * Proxy to HttpServletResponse.addDateHeader()
     * @param name the header name
     * @param value the header value
     */
    public void addDateHeader(String name, Date value) {
        HttpServletResponse res = getServletResponse();
        if (res != null)
            res.addDateHeader(name, value.getTime());
    }

    /**
     * Proxy to HttpServletResponse.setHeader()
     * @param name the header name
     * @param value the header value
     */
    public void setHeader(String name, String value) {
        HttpServletResponse res = getServletResponse();
        if (res != null)
            res.setHeader(name, value);
    }

    /**
     * Proxy to HttpServletResponse.setDateHeader()
     * @param name the header name
     * @param value the header value
     */
    public void setDateHeader(String name, Date value) {
        HttpServletResponse res = getServletResponse();
        if (res != null)
            res.setDateHeader(name, value.getTime());
    }

    /**
     * Write a vanilla error report. Callers should make sure the ResponeTrans is
     * new or has been reset.
     *
     * @param appName the application name
     * @param message the error message
     */
    public void reportError(String appName, String message) {
        if (reqtrans.isXmlRpc()) {
            writeXmlRpcError(new RuntimeException(message));
        } else {
            status = 500;
            if (!"true".equalsIgnoreCase(app.getProperty("suppressErrorPage"))) {
                write("<html><body><h3>");
                write("Error in application ");
                write(appName);
                write("</h3>");
                write(message);
                write("</body></html>");
            }
        }
    }

    public void writeXmlRpcResponse(Object result) {
        try {
            reset();
            contentType = "text/xml";
            if (charset == null) {
                charset = "UTF-8";
            }
            XmlRpcResponseProcessor xresproc = new XmlRpcResponseProcessor();
            writeBinary(xresproc.encodeResponse(result, charset));
        } catch (Exception x) {
            writeXmlRpcError(x);
        }
    }

    public void writeXmlRpcError(Exception x) {
        contentType = "text/xml";
        if (charset == null) {
            charset = "UTF-8";
        }
        XmlRpcResponseProcessor xresproc = new XmlRpcResponseProcessor();
        writeBinary(xresproc.encodeException(x, charset));
    }

    public void flush() {
        // does nothing!
    }

    /**
     * This has to be called after writing to this response has finished and before it is shipped back to the
     * web server. Transforms the string buffer into a byte array for transmission.
     */
    public void close() throws UnsupportedEncodingException {
        close(null);
    }

    /**
     * This has to be called after writing to this response has finished and before it is shipped back to the
     * web server. Transforms the string buffer into a byte array for transmission.
     */
    public synchronized void close(String cset) throws UnsupportedEncodingException {
        // if the response was already written and committed by the application
        // there's no point in closing the response buffer
        HttpServletResponse res = reqtrans.getServletResponse();
        if (res != null && res.isCommitted()) {
            return;
        }

        // only use default charset if not explicitly set for this response.
        if (charset == null) {
            charset = cset;
        }

        // if charset is not set, use western encoding
        if (charset == null) {
            charset = "ISO-8859-1";
        }

        boolean encodingError = false;

        // only close if the response hasn't been closed yet
        if (response == null) {
            // if debug buffer exists, append it to main buffer
            if (contentType != null &&
                    contentType.startsWith("text/html") && 
                    debugBuffer != null) {
                debugBuffer.append("</div>");
                if (buffer == null) {
                    buffer = debugBuffer;
                } else {
                    buffer.append(debugBuffer);
                }
            }

            // get the buffer's bytes in the specified encoding
            if (buffer != null) {
                try {
                    response = buffer.toString().getBytes(charset);
                } catch (UnsupportedEncodingException uee) {
                    encodingError = true;
                    response = buffer.toString().getBytes();
                }

                // make sure this is done only once, even with more requsts attached
                buffer = null;
            } else {
                response = new byte[0];
            }
        }

        boolean autoETags = "true".equals(app.getProperty("autoETags", "true"));
        // if etag is not set, calc MD5 digest and check it, but only if
        // not a redirect or error
        if (autoETags &&
                etag == null &&
                lastModified == -1 &&
                status == 200 &&
                redir == null) {
            try {
                digest = MessageDigest.getInstance("MD5");
                // if (contentType != null)
                //     digest.update (contentType.getBytes());
                byte[] b = digest.digest(response);
                etag = "\"" + new String(Base64.encode(b)) + "\"";
                // only set response to 304 not modified if no cookies were set
                if (reqtrans.hasETag(etag) && countCookies() == 0) {
                    response = new byte[0];
                    notModified = true;
                }
            } catch (Exception ignore) {
                // Etag creation failed for some reason. Ignore.
            }
        }

        notifyAll();

        // if there was a problem with the encoding, let the app know
        if (encodingError) {
            throw new UnsupportedEncodingException(charset);
        }
    }

    /**
     * If we just attached to evaluation we call this instead of close because only the primary thread
     * is responsible for closing the result
     */
    public synchronized void waitForClose() {
        try {
            if (response == null) {
                wait(10000L);
            }
        } catch (InterruptedException ix) {
            // Ignore
        }
    }

    /**
     * Get the body content for this response as byte array, encoded using the
     * response's charset.
     *
     * @return the response body
     */
    public byte[] getContent() {
        return (response == null) ? new byte[0] : response;
    }

    /**
     * Get the number of bytes of the response body.
     *
     * @return the length of the response body
     */
    public int getContentLength() {
        if (response != null) {
            return response.length;
        }

        return 0;
    }

    /**
     * Get the response's MIME content type
     *
     * @return the MIME type for this response
     */
    public String getContentType() {
        if (charset != null) {
            return contentType + "; charset=" + charset;
        }

        return contentType;
    }


    /**
     * Set the response's MIME content type
     *
     * @param contentType MIME type for this response
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * Set the Last-Modified header for this response
     *
     * @param modified the Last-Modified header in milliseconds
     */
    public void setLastModified(long modified) {
        // date headers don't do milliseconds, round to seconds
        lastModified = (modified / 1000) * 1000;
        if (reqtrans.getIfModifiedSince() == lastModified) {
            notModified = true;
            throw new RedirectException(null);
        }
    }

    /**
     * Get the value of the Last-Modified header for this response.
     *
     * @return the Last-Modified header in milliseconds
     */
    public long getLastModified() {
        return lastModified;
    }

    /**
     * Set the ETag header value for this response.
     *
     * @param value the ETag header value
     */
    public void setETag(String value) {
        etag = (value == null) ? null : ("\"" + value + "\"");
        if (etag != null && reqtrans.hasETag(etag)) {
            notModified = true;
            throw new RedirectException(null);
        }
    }

    /**
     * Get the ETag header value for this response.
     *
     * @return the ETag header value
     */
    public String getETag() {
        return etag;
    }

    /**
     * Check if this response should generate a Not-Modified response.
     *
     * @return true if the the response wasn't modified since the client last saw it.
     */
    public boolean getNotModified() {
        return notModified;
    }

    /**
     * Add a dependency to this response.
     *
     * @param what an item this response's output depends on.
     */
    public void dependsOn(Object what) {
        if (digest == null) {
            try {
                digest = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException nsa) {
                // MD5 should always be available
            }
        }

        if (what == null) {
            digest.update(new byte[0]);
        } else if (what instanceof Date) {
            digest.update(MD5Encoder.toBytes(((Date) what).getTime()));
        } else if (what instanceof byte[]) {
            digest.update((byte[]) what);
        } else {
            String str = what.toString();

            if (str != null) {
                digest.update(str.getBytes());
            } else {
                digest.update(new byte[0]);
            }
        }
    }

    /**
     * Digest all dependencies to a checksum to see if the response has changed.
     */
    public void digestDependencies() {
        if (digest == null) {
            return;
        }

        // add the application checksum as dependency to make ETag
        // generation sensitive to changes in the app
        byte[] b = digest.digest(MD5Encoder.toBytes(app.getChecksum()));

        setETag(new String(Base64.encode(b)));
    }

    /**
     * Set the path in which to look for skins. This may contain file locations and
     * HopObjects.
     *
     * @param arr the skin path
     */
    public void setSkinpath(Object[] arr) {
        this.skinpath = arr;
        skincache = null;
    }

    /**
     * Get the path in which to look for skins. This may contain file locations and
     * HopObjects.
     *
     * @return the skin path
     */
    public Object[] getSkinpath() {
        if (skinpath == null) {
            skinpath = new Object[0];
        }

        return skinpath;
    }

    /**
     * Look up a cached skin.
     *
     * @param id the skin key
     * @return the skin, or null if no skin is cached for the given key
     */
    public Skin getCachedSkin(Object id) {
        if (skincache == null) {
            return null;
        }

        return (Skin) skincache.get(id);
    }

    /**
     * Cache a skin for the length of this response.
     *
     * @param id the skin key
     * @param skin the skin to cache
     */
    public void cacheSkin(Object id, Skin skin) {
        if (skincache == null) {
            skincache = new HashMap();
        }

        skincache.put(id, skin);
    }

    /**
     * Set the skin currently being rendered, returning the previously active skin.
     * @param skin the new active skin
     * @return the previously active skin
     */
    public Skin switchActiveSkin(Skin skin) {
        Skin previousSkin = activeSkin;
        activeSkin = skin;
        return previousSkin;
    }

    /**
     * Return the skin currently being rendered, or none.
     * @return the currently active skin
     */
    public Skin getActiveSkin() {
        return activeSkin;
    }

    /**
     * Set a cookie.
     *
     * @param key the cookie key
     * @param value the cookie value
     * @param days the cookie's lifespan in days
     * @param path the URL path to apply the cookie to
     * @param domain the domain to apply the cookie to
     */
    public void setCookie(String key, String value, int days, String path, String domain) {
        CookieTrans c = null;

        if (cookies == null) {
            cookies = new HashMap();
        } else {
            c = (CookieTrans) cookies.get(key);
        }

        // remove newline chars to prevent response splitting attack
        if (value != null) {
            value = value.replaceAll("[\r\n]", "");
        }

        if (c == null) {
            c = new CookieTrans(key, value);
            cookies.put(key, c);
        } else {
            c.setValue(value);
        }

        c.setDays(days);
        c.setPath(path);
        c.setDomain(domain);
    }

    /**
     * Reset all previously set cookies.
     */
    public void resetCookies() {
        if (cookies != null) {
            cookies.clear();
        }
    }

    /**
     * Get the number of cookies set in this response.
     *
     * @return the number of cookies
     */
    public int countCookies() {
        if (cookies != null) {
            return cookies.size();
        }

        return 0;
    }

    /**
     * Get the cookies set in this response.
     *
     * @return the cookies
     */
    public CookieTrans[] getCookies() {
        if (cookies == null) {
            return new CookieTrans[0];
        }

        CookieTrans[] c = new CookieTrans[cookies.size()];
        cookies.values().toArray(c);
        return c;
    }

    /**
     * Get the message to display to the user, if any.
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Set a message to display to the user.
     * @param message the message
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Get the error message to display to the user, if any.
     * @return the error message
     */
    public String getError() {
        return error;
    }

    /**
     * Set a message to display to the user.
     * @param error the error message
     */
    public void setError(String error) {
        this.error = error;
    }

    /**
     * Get debug messages to append to the response, if any.
     * @return the response's debug buffer
     */
    public StringBuffer getDebugBuffer() {
        return debugBuffer;
    }

    /**
     * Set debug messages to append to the response.
     * @param debugBuffer the response's debug buffer
     */
    public void setDebugBuffer(StringBuffer debugBuffer) {
        this.debugBuffer = debugBuffer;
    }

    /**
     * Get the charset/encoding for this response
     * @return the charset name
     */
    public String getCharset() {
        return charset;
    }

    /**
     * Set the charset/encoding for this response
     * @param charset the charset name
     */
    public void setCharset(String charset) {
        this.charset = charset;
    }

    /**
     * Returns true if this response may be cached by the client
     * @return true if the response may be cached
     */
    public boolean isCacheable() {
        return cacheable;
    }

    /**
     * Set the cacheability of this response
     * @param cache true if the response may be cached
     */
    public void setCacheable(boolean cache) {
        this.cacheable = cache;
    }

    /**
     * Get the HTTP response status code
     * @return the HTTP response code
     */
    public int getStatus() {
        return status;
    }

    /**
     * Set the HTTP response status code
     * @param status the HTTP response code
     */
    public void setStatus(int status) {
        this.status = status;
    }

    /**
     * Get the HTTP authentication realm
     * @return the name of the authentication realm
     */
    public String getRealm() {
        return realm;
    }

    /**
     * Set the HTTP authentication realm
     * @param realm the name of the authentication realm
     */
    public void setRealm(String realm) {
        this.realm = realm;
    }
}