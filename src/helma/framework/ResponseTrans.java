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
import helma.objectmodel.*;
import helma.util.*;
import java.io.*;
import java.security.*;
import java.util.*;

/**
 * A Transmitter for a response to the servlet client. Objects of this
 * class are directly exposed to JavaScript as global property res.
 */
public final class ResponseTrans implements Externalizable {
    static final long serialVersionUID = -8627370766119740844L;
    static final int INITIAL_BUFFER_SIZE = 2048;

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
     * Used for HTTP response code, if 0 code 200 OK will be used.
     */
    public int status = 0;

    /**
     * Used for HTTP authentication
     */
    public String realm;

    // name of the skin to be rendered  after completion, if any
    public transient String skin = null;

    // the actual response
    private byte[] response = null;

    // contains the redirect URL
    private String redir = null;

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

    // the map of form and cookie data
    private transient Map values;

    // the map of macro handlers
    private transient Map handlers;

    // the request trans for this response
    private transient RequestTrans reqtrans;

    // the message digest used to generate composed digests for ETag headers
    private transient MessageDigest digest;

    // the appliciation checksum to make ETag headers sensitive to app changes
    long applicationChecksum;

    /**
     * Creates a new ResponseTrans object.
     */
    public ResponseTrans() {
        super();
        message = error = null;
        values = new SystemMap();
        handlers = new SystemMap();
    }

    /**
     * Creates a new ResponseTrans object.
     *
     * @param req ...
     */
    public ResponseTrans(RequestTrans req) {
        this();
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
     * Reset the response object to its initial empty state.
     */
    public void reset() {
        if (buffer != null) {
            buffer.setLength(0);
        }

        buffers = null;
        response = null;
        redir = null;
        skin = null;
        message = error = null;
        values.clear();
        lastModified = -1;
        notModified = false;
        etag = null;

        if (digest != null) {
            digest.reset();
        }
    }

    /**
     * This is called before a skin is rendered as string (renderSkinAsString) to redirect the output
     * to a new string buffer.
     */
    public void pushStringBuffer() {
        if (buffers == null) {
            buffers = new Stack();
        }

        if (buffer != null) {
            buffers.push(buffer);
        }

        buffer = new StringBuffer(64);
    }

    /**
     * Returns the content of the current string buffer and switches back to the previos one.
     */
    public String popStringBuffer() {
        StringBuffer b = buffer;

        buffer = buffers.empty() ? null : (StringBuffer) buffers.pop();

        return b.toString();
    }

    /**
     *  Get the response buffer, creating it if it doesn't exist
     */
    public StringBuffer getBuffer() {
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
    public void write(Object what) {
        if (what != null) {
            String str = what.toString();

            if (buffer == null) {
                buffer = new StringBuffer(Math.max(str.length() + 100, INITIAL_BUFFER_SIZE));
            }

            buffer.append(what.toString());
        }
    }

    /**
     * Utility function that appends a <br> to whatever is written.
     */
    public void writeln(Object what) {
        if (buffer == null) {
            buffer = new StringBuffer(INITIAL_BUFFER_SIZE);
        }

        if (what != null) {
            buffer.append(what.toString());
        }

        buffer.append("<br />\r\n");
    }

    /**
     *  Append a part from a char array to the response buffer.
     */
    public void writeCharArray(char[] c, int start, int length) {
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
    public void encode(Object what) {
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
    public void format(Object what) {
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
    public void encodeXml(Object what) {
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
    public void encodeForm(Object what) {
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
    public void append(String str) {
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
     *  Allow to directly set the byte array for the response. Calling this more than once will
     *  overwrite the previous output. We take a generic object as parameter to be able to
     * generate a better error message, but it must be byte[].
     */
    public void writeBinary(byte[] what) {
        response = what;
    }

    /**
     * This has to be called after writing to this response has finished and before it is shipped back to the
     * web server. Transforms the string buffer into a char array to minimize size.
     */
    public synchronized void close(String cset) throws UnsupportedEncodingException {
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

        // if etag is not set, calc MD5 digest and check it
        if ((etag == null) && (lastModified == -1) && (redir == null)) {
            try {
                digest = MessageDigest.getInstance("MD5");

                // if (contentType != null)
                //     digest.update (contentType.getBytes());
                byte[] b = digest.digest(response);

                etag = "\"" + new String(Base64.encode(b)) + "\"";

                if ((reqtrans != null) && reqtrans.hasETag(etag)) {
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

        byte[] b = digest.digest(MD5Encoder.toBytes(applicationChecksum));

        /* StringBuffer buf = new StringBuffer(b.length*2);
           for ( int i=0; i<b.length; i++ ) {
               int j = (b[i]<0) ? 256+b[i] : b[i];
               if ( j<16 ) buf.append("0");
               buf.append(Integer.toHexString(j));
           }
           setETag (buf.toString ()); */
        setETag(new String(Base64.encode(b)));
    }

    /**
     *
     *
     * @param n ...
     */
    public void setApplicationChecksum(long n) {
        applicationChecksum = n;
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
    public Skin getCachedSkin(String id) {
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
    public void cacheSkin(String id, Skin skin) {
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

    /**
     *
     *
     * @param s ...
     *
     * @throws ClassNotFoundException ...
     * @throws IOException ...
     */
    public void readExternal(ObjectInput s) throws ClassNotFoundException, IOException {
        contentType = (String) s.readObject();
        response = (byte[]) s.readObject();
        redir = (String) s.readObject();
        cookies = (Map) s.readObject();
        cache = s.readBoolean();
        status = s.readInt();
        realm = (String) s.readObject();
        lastModified = s.readLong();
        notModified = s.readBoolean();
        charset = (String) s.readObject();
        etag = (String) s.readObject();
    }

    /**
     *
     *
     * @param s ...
     *
     * @throws IOException ...
     */
    public void writeExternal(ObjectOutput s) throws IOException {
        s.writeObject(contentType);
        s.writeObject(response);
        s.writeObject(redir);
        s.writeObject(cookies);
        s.writeBoolean(cache);
        s.writeInt(status);
        s.writeObject(realm);
        s.writeLong(lastModified);
        s.writeBoolean(notModified);
        s.writeObject(charset);
        s.writeObject(etag);
    }
}
