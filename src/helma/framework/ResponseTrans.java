// ResponseTrans.java
// Copyright (c) Hannes Wallnöfer 1998-2000

package helma.framework;

import java.io.*;
import java.util.*;
import helma.objectmodel.*;
import helma.util.*;

/**
 * A Transmitter for a response to the servlet client. Objects of this 
 * class are directly exposed to JavaScript as global property res. 
 */
 
public class ResponseTrans implements Serializable {

    public String contentType = "text/html";
    // the actual response
    private char[] response = null;
    // contains the redirect URL
    public String redirect = null;

    // cookies
    public String cookieKeys[];
    public String cookieValues[];
    public int cookieDays[];
    int nCookies = 0;

    // used to allow or disable client side caching
    public boolean cache = true;

    // the buffer used to build the response
    private transient StringBuffer buffer = null;
    // these are used to implement the _as_string variants for Hop templates.
    private transient Stack buffers;

    // the buffers used to build the single body parts -
    // transient, response must be constructed before this is serialized
    public transient String title, head, body, message, error;


    public ResponseTrans () {
	super ();
	title = head = body = message = error = "";
    }
    
    public void reset () {
	if (buffer != null)
	    buffer.setLength (0);
	redirect = null;
    }


    /**
     * This is called before a template is rendered as string (xxx_as_string) to redirect the output
     * to a new string buffer.
     */
    public void pushStringBuffer () {
	if (buffers == null)
	    buffers = new Stack();
	if (buffer != null)
	    buffers.push (buffer);
	buffer = new StringBuffer (128);
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
     * Append a string to the response unchanged.
     */
    public void write (Object what) {
	if (what != null) {
	    if (buffer == null)
	        buffer = new StringBuffer (512);
	    buffer.append (what.toString ());
	}
    }

    /**
     * Replace special characters with entities, including <, > and ", thus allowing
     * no HTML tags.
     */
    public void encode (Object what) {
	if (what != null) {
	    if (buffer == null)
	        buffer = new StringBuffer (512);
	    HtmlEncoder.encodeAll (what.toString (), buffer);
	}
    }

    /**
     * Replace special characters with entities but leave <, > and ", allowing HTML tags
     *  in the response.
     */
    public void format (Object what) {
	if (what != null) {
	    if (buffer == null)
	        buffer = new StringBuffer (512);
	    HtmlEncoder.encode (what.toString (), buffer);
	}
    }


    /**
     * Replace special characters with entities, including <, > and ", thus allowing
     * no HTML tags.
     */
    public void encodeXml (Object what) {
	if (what != null) {
	    if (buffer == null)
	        buffer = new StringBuffer (512);
	    HtmlEncoder.encodeXml (what.toString (), buffer);
	}
    }


    public void append (String what) {
	if (what != null) {
	    if (buffer == null)
	        buffer = new StringBuffer (512);
	    buffer.append (what);
	}
    }

    public void redirect (String url) throws RedirectException {
	redirect = url;
	throw new RedirectException (url);
    }


    /**
     * This has to be called after writin to this response has finished and before it is shipped back to the
     * web server. Transforms the string buffer into a char array to minimize size.
     */
    public void close () {
	if (buffer != null)
	    response = new String (buffer).toCharArray();
    }

    public String getContentString () {
	return response == null ? "" : new String (response);
    }

    public int getContentLength () {
	if (response != null)
	    return response.length;
	return 0;
    }

    public String getContentType () {
    	return contentType;
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


}


