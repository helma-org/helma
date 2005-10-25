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
public final class ResponseTrans implements Serializable {

    static final long serialVersionUID = -8627370766119740844L;
    static final int INITIAL_BUFFER_SIZE = 2048;

    static final String newLine = System.getProperty("line.separator");

    /**
     * Set the MIME content type of the response.
     */
    public String contentType = "text/html";

    /**
     * Set the charset (encoding) to use for the response.
     */
    public String charset;

    /**
     * used to allow or disable client side caching
     */
    public boolean cache = true;

    /**
     * Value for HTTP response code, defaults to 200 (OK).
     */
    public int status = 200;

    /**
     * Used for HTTP authentication
     */
    public String realm;

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

    /**
     * string fields that hold a user message
     */
    public transient String message;

    /**
     * string fields that hold an error message
     */
    public transient String error;

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
        cache = true;
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
     * This is called before a skin is rendered as string (renderSkinAsString) to redirect the output
     * to a new string buffer.
     */
    public synchronized void pushStringBuffer() {
        if (buffers == null) {
            buffers = new Stack();
        }

        if (buffer != null) {
            buffers.push(buffer);
        }

        if (cachedBuffer != null) {
            buffer = cachedBuffer;
            cachedBuffer = null;
        } else {
            buffer = new StringBuffer(64);
        }
    }

    /**
     * Returns the content of the current string buffer and switches back to the previos one.
     */
    public synchronized String popStringBuffer() {
        if (buffer == null) {
            throw new RuntimeException("Can't pop string buffer: buffer is null");
        } else if (buffers == null) {
            throw new RuntimeException("Can't pop string buffer: buffer stack is empty");
        }

        String str = buffer.toString();

        buffer.setLength(0);
        cachedBuffer = buffer;

        // restore the previous buffer, which may be null
        buffer = buffers.empty() ? null : (StringBuffer) buffers.pop();

        return str;
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
     * Append a string to the response unchanged. This is often called
     * at the end of a request to write out the whole page, so if buffer
     * is uninitialized we just set it to the string argument.
     */
    public synchronized void write(Object what) {
        if (what != null) {
            String str = what.toString();

            if (buffer == null) {
                buffer = new StringBuffer(Math.max(str.length() + 100, INITIAL_BUFFER_SIZE));
            }

            buffer.append(str);
        }
    }

    /**
     * Write object to response buffer and append a platform dependent newline sequence.
     */
    public synchronized void writeln(Object what) {
        write(what);

        // if what is null, buffer may still be uninitialized
        if (buffer == null) {
            buffer = new StringBuffer(INITIAL_BUFFER_SIZE);
        }

        buffer.append(newLine);
    }

    /**
     *  Append a part from a char array to the response buffer.
     */
    public synchronized void writeCharArray(char[] c, int start, int length) {
        if (buffer == null) {
            buffer = new StringBuffer(Math.max(length, INITIAL_BUFFER_SIZE));
        }

        buffer.append(c, start, length);
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

        debugBuffer.append("<p><span style=\"background: yellow; color: black\">");
        debugBuffer.append(str);
        debugBuffer.append("</span></p>");
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
     * @param str ...
     */
    public synchronized void append(String str) {
        if (str != null) {
            if (buffer == null) {
                buffer = new StringBuffer(Math.max(str.length(), INITIAL_BUFFER_SIZE));
            }

            buffer.append(str);
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
        redir = url;
        throw new RedirectException(url);
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
        forward = url;
        throw new RedirectException(url);
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
     * Write a vanilla error report. Callers should make sure the ResponeTrans is
     * new or has been reset.
     *
     * @param appName the application name
     * @param message the error message
     */
    public void writeErrorReport(String appName, String message) {
        if (reqtrans.isXmlRpc()) {
            writeXmlRpcError(new RuntimeException(message));
        } else {
            write("<html><body><h3>");
            write("Error in application ");
            write(appName);
            write("</h3>");
            write(message);
            write("</body></html>");
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

    /**
     * This has to be called after writing to this response has finished and before it is shipped back to the
     * web server. Transforms the string buffer into a char array to minimize size.
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
            if (debugBuffer != null) {
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

        // if etag is not set, calc MD5 digest and check it, but only if not a redirect
        if (etag == null && lastModified == -1 && redir == null) {
            try {
                digest = MessageDigest.getInstance("MD5");

                // if (contentType != null)
                //     digest.update (contentType.getBytes());
                byte[] b = digest.digest(response);

                etag = "\"" + new String(Base64.encode(b)) + "\"";

                // only set response to 304 not modified if no cookies were set
                if (reqtrans != null && reqtrans.hasETag(etag) && countCookies() == 0) {
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
        }
    }

    /**
     *
     *
     * @return ...
     */
    public byte[] getContent() {
        return (response == null) ? new byte[0] : response;
    }

    /**
     *
     *
     * @return ...
     */
    public int getContentLength() {
        if (response != null) {
            return response.length;
        }

        return 0;
    }

    /**
     *
     *
     * @return ...
     */
    public String getContentType() {
        if (charset != null) {
            return contentType + "; charset=" + charset;
        }

        return contentType;
    }

    /**
     *
     *
     * @param modified ...
     */
    public void setLastModified(long modified) {
        if ((modified > -1) && (reqtrans != null) &&
                (reqtrans.getIfModifiedSince() >= modified)) {
            notModified = true;
            throw new RedirectException(null);
        }

        lastModified = modified;
    }

    /**
     *
     *
     * @return ...
     */
    public long getLastModified() {
        return lastModified;
    }

    /**
     *
     *
     * @param value ...
     */
    public void setETag(String value) {
        etag = (value == null) ? null : ("\"" + value + "\"");

        if ((etag != null) && (reqtrans != null) && reqtrans.hasETag(etag)) {
            notModified = true;
            throw new RedirectException(null);
        }
    }

    /**
     *
     *
     * @return ...
     */
    public String getETag() {
        return etag;
    }

    /**
     *
     *
     * @return ...
     */
    public boolean getNotModified() {
        return notModified;
    }

    /**
     *
     *
     * @param what ...
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
     *
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
     *
     *
     * @param arr ...
     */
    public void setSkinpath(Object[] arr) {
        this.skinpath = arr;
        skincache = null;
    }

    /**
     *
     *
     * @return ...
     */
    public Object[] getSkinpath() {
        if (skinpath == null) {
            skinpath = new Object[0];
        }

        return skinpath;
    }

    /**
     *
     *
     * @param id ...
     *
     * @return ...
     */
    public Skin getCachedSkin(Object id) {
        if (skincache == null) {
            return null;
        }

        return (Skin) skincache.get(id);
    }

    /**
     *
     *
     * @param id ...
     * @param skin ...
     */
    public void cacheSkin(Object id, Skin skin) {
        if (skincache == null) {
            skincache = new HashMap();
        }

        skincache.put(id, skin);
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
        CookieTrans c = null;

        if (cookies == null) {
            cookies = new HashMap();
        } else {
            c = (CookieTrans) cookies.get(key);
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
     *
     */
    public void resetCookies() {
        if (cookies != null) {
            cookies.clear();
        }
    }

    /**
     *
     *
     * @return ...
     */
    public int countCookies() {
        if (cookies != null) {
            return cookies.size();
        }

        return 0;
    }

    /**
     *
     *
     * @return ...
     */
    public CookieTrans[] getCookies() {
        if (cookies == null) {
            return new CookieTrans[0];
        }

        CookieTrans[] c = new CookieTrans[cookies.size()];

        cookies.values().toArray(c);

        return c;
    }

}
