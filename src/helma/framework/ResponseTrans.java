// ResponseTrans.java
// Copyright (c) Hannes Wallnöfer 1998-2000

package helma.framework;

import java.io.*;
import java.util.*;
import helma.framework.core.Skin;
import helma.objectmodel.*;
import helma.util.*;

/**
 * A Transmitter for a response to the servlet client. Objects of this 
 * class are directly exposed to JavaScript as global property res. 
 */
 
public final class ResponseTrans implements Externalizable {

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


    // cookies
    String cookieKeys[];
    String cookieValues[];
    int cookieDays[];
    int nCookies = 0;

    // the buffer used to build the response
    private transient StringBuffer buffer = null;
    // these are used to implement the _as_string variants for Hop templates.
    private transient Stack buffers;

    // the path used to tell where to look for skins
    private transient Object[] skinpath = null;
    // hashmap for skin caching
    private transient HashMap skincache;

    static final long serialVersionUID = -8627370766119740844L;

    /**
     * the buffers used to build the single body parts -
     * transient, response must be constructed before this is serialized
     */
    public transient String title, head, body, message, error;
    
    static final int INITIAL_BUFFER_SIZE = 2048;

    /**
     *  JavaScript object to make the values Map accessible to
     *  script code as res.data
     */
    public transient Object data;

    // the map of form and cookie data
    private transient Map values;


    public ResponseTrans () {
	super ();
	title = head = body = message = error = null;
	values = new HashMap ();
    }


    /**
     *  Get a value from the responses map by key.
     */
    public Object get (String name) {
	try {
	    return values.get (name);
	} catch (Exception x) {
	    return null;
	}
    }

    /**
     *  Get the data map for this response transmitter.
     */
    public Map getResponseData () {
	return values;
    }

    /**
     * Reset the response object to its initial empty state.
     */
    public void reset () {
	if (buffer != null)
	    buffer.setLength (0);
	response = null;
	redir = null;
	skin = null;
	title = head = body = message = error = null;
	values.clear ();
	lastModified = -1;
	notModified = false;
    }


    /**
     * This is called before a skin is rendered as string (renderSkinAsString) to redirect the output
     * to a new string buffer.
     */
    public void pushStringBuffer () {
	if (buffers == null)
	    buffers = new Stack();
	if (buffer != null)
	    buffers.push (buffer);
	buffer = new StringBuffer (64);
    }

    /**
     * Returns the content of the current string buffer and switches back to the previos one.
     */
    public String popStringBuffer () {
	StringBuffer b = buffer;
	buffer = buffers.empty() ? null : (StringBuffer) buffers.pop ();
	return b.toString ();
    }

    /**
     * Returns the number of characters written to the response buffer so far.
     */
    public int getBufferLength() {
	if (buffer == null)
	    return 0;
	return buffer.length ();
    }

    /**
     * Append a string to the response unchanged. This is often called 
     * at the end of a request to write out the whole page, so if buffer
     * is uninitialized we just set it to the string argument.
     */
    public void write (Object what) {
	if (what != null) {
	    String str = what.toString ();
	    if (buffer == null)
	        buffer = new StringBuffer (Math.max (str.length()+100, INITIAL_BUFFER_SIZE));
	    buffer.append (what.toString ());
	}
    }

    /**
     * Utility function that appends a <br> to whatever is written.
     */
    public void writeln (Object what) {
	if (buffer == null)
	    buffer = new StringBuffer (INITIAL_BUFFER_SIZE);
	if (what != null)
	    buffer.append (what.toString ());
	buffer.append ("<br />\r\n");
    }

    /**
     *  Append a part from a char array to the response buffer.
     */
    public void writeCharArray (char[] c, int start, int length) {
	if (buffer == null)
	    buffer = new StringBuffer (Math.max (length, INITIAL_BUFFER_SIZE));
	buffer.append (c, start, length);
    }

    /**
     *  Insert string somewhere in the response buffer. Caller has to make sure
     *  that buffer exists and its length is larger than offset. str may be null, in which
     *  case nothing happens.
     */
    public void insert (int offset, String str) {
	if (str != null)
	    buffer.insert (offset, str);
    }

    /**
     * Replace special characters with entities, including <, > and ", thus allowing
     * no HTML tags.
     */
    public void encode (Object what) {
	if (what != null) {
	    String str = what.toString ();
	    if (buffer == null)
	        buffer = new StringBuffer (Math.max (str.length()+100, INITIAL_BUFFER_SIZE));
	    HtmlEncoder.encodeAll (str, buffer);
	}
    }

    /**
     * Replace special characters with entities but leave <, > and ", allowing HTML tags
     *  in the response.
     */
    public void format (Object what) {
	if (what != null) {
	    String str = what.toString ();
	    if (buffer == null)
	        buffer = new StringBuffer (Math.max (str.length()+100, INITIAL_BUFFER_SIZE));
	    HtmlEncoder.encode (str, buffer);
	}
    }


    /**
     * Replace special characters with entities, including <, > and ", thus allowing
     * no HTML tags.
     */
    public void encodeXml (Object what) {
	if (what != null) {
	    String str = what.toString ();
	    if (buffer == null)
	        buffer = new StringBuffer (Math.max (str.length()+100, INITIAL_BUFFER_SIZE));
	    HtmlEncoder.encodeXml (str, buffer);
	}
    }


    /**
     * Encode HTML entities, but leave newlines alone. This is for the content of textarea forms.
     */
    public void encodeForm (Object what) {
	if (what != null) {
	    String str = what.toString ();
	    if (buffer == null)
	        buffer = new StringBuffer (Math.max (str.length()+100, INITIAL_BUFFER_SIZE));
	    HtmlEncoder.encodeAll (str, buffer, false);
	}
    }


    public void append (String str) {
	if (str != null) {
	    if (buffer == null)
	        buffer = new StringBuffer (Math.max (str.length(), INITIAL_BUFFER_SIZE));
	    buffer.append (str);
	}
    }

    public void redirect (String url) throws RedirectException {
	redir = url;
	throw new RedirectException (url);
    }

    public String getRedirect () {
	return redir;
    }

    /**
     *  Allow to directly set the byte array for the response. Calling this more than once will
     *  overwrite the previous output. We take a generic object as parameter to be able to
     * generate a better error message, but it must be byte[].
     */
    public void writeBinary (byte[] what) {
	response = what;
    }


    /**
     * This has to be called after writing to this response has finished and before it is shipped back to the
     * web server. Transforms the string buffer into a char array to minimize size.
     */
    public synchronized void close (String cset) throws UnsupportedEncodingException {
	// only use default charset if not explicitly set for this response.
	if (charset == null)
	    charset = cset;
	// if charset is not set, use western encoding
	if (charset == null)
	    charset = "ISO-8859-1";
	boolean error = false;
	if (response == null) {
	    if (buffer != null) {
	        try {
	            response = buffer.toString ().getBytes (charset);
	        } catch (UnsupportedEncodingException uee) {
	            error = true;
	            response = buffer.toString ().getBytes ();
	        }
	        buffer = null; // make sure this is done only once, even with more requsts attached
	    } else {
	        response = new byte[0];
	    }
	}
	notifyAll ();
	// if there was a problem with the encoding, let the app know
	if (error)
	    throw new UnsupportedEncodingException (charset);
    }

    /**
     * If we just attached to evaluation we call this instead of close because only the primary thread
     * is responsible for closing the result
     */
    public synchronized void waitForClose () {
	try {
	    if (response == null)
	        wait (10000l);
	} catch (InterruptedException ix) {}
    }

    public byte[] getContent () {
	return response == null ? new byte[0] : response;
    }

    public int getContentLength () {
	if (response != null)
	    return response.length;
	return 0;
    }

    public String getContentType () {
	if (charset != null)
	    return contentType+"; charset="+charset;
	return contentType;
    }

    public void setLastModified (long modified) {
	lastModified = modified;
    }
    
    public long getLastModified () {
	return lastModified;
    }
    
    public void setNotModified (boolean notmod) throws RedirectException {
	notModified = notmod;
	if (notmod)
	    throw new RedirectException (null);
    }
    
    public boolean getNotModified () {
	return notModified; 
    }

    public void setSkinpath (Object[] arr) {
	this.skinpath = arr;
	skincache = null;
    }

    public Object[] getSkinpath () {
	if (skinpath == null)
	    skinpath = new Object[0];
	return skinpath;
    }

    public Skin getCachedSkin (String id) {
	if (skincache == null)
	    return null;
	return (Skin) skincache.get (id);
    }

    public void cacheSkin (String id, Skin skin) {
	if (skincache == null)
	    skincache = new HashMap ();
	skincache.put (id, skin);
    }

    public synchronized void setCookie (String key, String value) {
	setCookie (key, value, -1);
    }

    public synchronized void setCookie (String key, String value, int days) {
	if (nCookies == 0) {
	    cookieKeys = new String [3];
	    cookieValues = new String [3];
	    cookieDays = new int [3];
	}
	if (nCookies == cookieKeys.length) {
	    String nk[] = new String [nCookies+3];
	    System.arraycopy (cookieKeys, 0, nk, 0, nCookies);
	    String nv[] = new String [nCookies+3];
	    System.arraycopy (cookieValues, 0, nv, 0, nCookies);
	    int nd[] = new int [nCookies+3];
	    System.arraycopy (cookieDays, 0, nd, 0, nCookies);
	    cookieKeys = nk;
	    cookieValues = nv;
	    cookieDays = nd;
	}
	cookieKeys [nCookies] = key;
	cookieValues [nCookies] = value;
	cookieDays [nCookies] = days;
	nCookies += 1;
    }

    public void resetCookies () {
	nCookies = 0;
    }

    public int countCookies () {
	return nCookies;
    }

    public int getDaysAt (int i) {
	return cookieDays[i];
    }

    public String getKeyAt (int i) {
	return cookieKeys[i];
    }

    public String getValueAt (int i) {
	return cookieValues[i];
    }

    public void readExternal (ObjectInput s) throws ClassNotFoundException, IOException {
	contentType = (String) s.readObject ();
	response = (byte[]) s.readObject ();
	redir = (String) s.readObject ();
	cookieKeys = (String[]) s.readObject ();
	cookieValues = (String[]) s.readObject ();
	cookieDays = (int[]) s.readObject ();
	nCookies = s.readInt ();
	cache = s.readBoolean ();
	status = s.readInt ();
	realm = (String) s.readObject ();
	lastModified = s.readLong ();
	notModified = s.readBoolean ();
	charset = (String) s.readObject ();
    }

    public void writeExternal (ObjectOutput s) throws IOException {
	s.writeObject (contentType);
	s.writeObject (response);
	s.writeObject (redir);
	s.writeObject (cookieKeys);
	s.writeObject (cookieValues);
	s.writeObject (cookieDays);
	s.writeInt (nCookies);
	s.writeBoolean (cache);
	s.writeInt (status);
	s.writeObject (realm);
	s.writeLong (lastModified);
	s.writeBoolean (notModified);
	s.writeObject (charset);
    }

}


