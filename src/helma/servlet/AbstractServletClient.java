// ServletClient.java
// Copyright (c) Hannes Wallnöfer, Raphael Spannocchi 1998-2000

/* Portierung von helma.asp.AspClient auf Servlets */
/* Author: Raphael Spannocchi Datum: 27.11.1998 */

package helma.servlet;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.*;
import helma.framework.*;
import helma.util.*;

/**
 * This is an abstract Hop servlet adapter. This class communicates with hop applications
 * via RMI. Subclasses are either one servlet per app, or one servlet that handles multiple apps
 */
 
public abstract class AbstractServletClient extends HttpServlet {

    // host on which Helma app is running
    String host = null;
    // port of Helma RMI server
    int port = 0;
    // limit to HTTP uploads in kB
    int uploadLimit = 1024;
    // RMI url of Helma app
    String hopUrl;
    // cookie domain to use
    String cookieDomain;
    // default encoding for requests
    String  defaultEncoding;
    // allow caching of responses
    boolean caching;
    // enable debug output
    boolean debug;

    static final byte HTTP_GET = 0;
    static final byte HTTP_POST = 1;

    public void init (ServletConfig init) throws ServletException {
	super.init (init);

	host =  init.getInitParameter ("host");
	if (host == null) host = "localhost";

	String portstr = init.getInitParameter ("port");
	port =  portstr == null ? 5055 : Integer.parseInt (portstr);

	String upstr = init.getInitParameter ("uploadLimit");
	uploadLimit =  upstr == null ? 1024 : Integer.parseInt (upstr);

	cookieDomain = init.getInitParameter ("cookieDomain");

	hopUrl = "//" + host + ":" + port + "/";

	defaultEncoding = init.getInitParameter ("charset");

	debug = ("true".equalsIgnoreCase (init.getInitParameter ("debug")));

	caching = ! ("false".equalsIgnoreCase (init.getInitParameter ("caching")));
    }


    abstract IRemoteApp getApp (String appID) throws Exception;

    abstract void invalidateApp (String appID);

    abstract String getAppID (String reqpath);

    abstract String getRequestPath (String reqpath);


    public void doGet (HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException {
	execute (request, response, HTTP_GET);
    }
	
    public void doPost (HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException {
	execute (request, response, HTTP_POST);
    }


    protected void execute (HttpServletRequest request, HttpServletResponse response, byte method) {
	String protocol = request.getProtocol ();
	Cookie[] cookies = request.getCookies();

	// get app and path from original request path
	String pathInfo = request.getPathInfo ();
	String appID = getAppID (pathInfo);
	RequestTrans reqtrans = new RequestTrans (method);
	reqtrans.path = getRequestPath (pathInfo);

	try {

	    // read and set http parameters
	    Map parameters = parseParameters (request);
	    for (Iterator i=parameters.entrySet().iterator(); i.hasNext(); ) {
	        Map.Entry entry = (Map.Entry) i.next ();
	        String key = (String) entry.getKey ();
	        String[] values = (String[]) entry.getValue ();
	        if (values != null && values.length > 0) {
	            reqtrans.set (key, values[0]);    // set to single string value
	            if (values.length > 1)
	                reqtrans.set (key+"_array", values);     // set string array
	        }
	    }

	    // check for MIME file uploads
	    String contentType = request.getContentType();
	    if (contentType != null && contentType.indexOf("multipart/form-data")==0) {
	        // File Upload
	        try {
	            FileUpload upload = getUpload (request);
	            if (upload != null) {
	                Hashtable parts = upload.getParts ();
	                for (Enumeration e = parts.keys(); e.hasMoreElements(); ) {
	                    String nextKey = (String) e.nextElement ();
	                    Object nextPart = parts.get (nextKey);
	                    reqtrans.set (nextKey, nextPart);
	                }
	            }
	        } catch (Exception upx) {
	            String uploadErr = upx.getMessage ();
	            if (uploadErr == null || uploadErr.length () == 0)
	                uploadErr = upx.toString ();
	            reqtrans.set ("uploadError", uploadErr);
	        }
	    }

	    // read cookies
	    if (cookies != null) {
	        for (int i=0; i < cookies.length;i++) try {
	            // get Cookies
	            String nextKey = cookies[i].getName ();
	            String nextPart = cookies[i].getValue ();
	            if ("HopSession".equals (nextKey))
	                reqtrans.session = nextPart;
	            else
	                reqtrans.set (nextKey, nextPart);
	        } catch (Exception badCookie) {}
	    }

	    // check if we need to create a session id
	    if (reqtrans.session == null) {
	        reqtrans.session = Long.toString (Math.round (Math.random ()*Long.MAX_VALUE), 16);
	        reqtrans.session += "@"+Long.toString (System.currentTimeMillis (), 16);
	        Cookie c = new Cookie("HopSession", reqtrans.session);
	        c.setPath ("/");
	        if (cookieDomain != null)
	            c.setDomain (cookieDomain);
	        response.addCookie(c);
	    }

	    // do standard HTTP variables
	    String host = request.getHeader ("Host");
	    if (host != null) {
	        host = host.toLowerCase();
	        reqtrans.set ("http_host", host);
	    }

	    String referer = request.getHeader ("Referer");
	    if (referer != null)
	        reqtrans.set ("http_referer", referer);

	    String remotehost = request.getRemoteAddr ();
	    if (remotehost != null)
	        reqtrans.set ("http_remotehost", remotehost);

	    String browser = request.getHeader ("User-Agent");
	    if (browser != null)
	        reqtrans.set ("http_browser", browser);

	    String authorization = request.getHeader("authorization");
	    if ( authorization != null )
	        reqtrans.set ("authorization", authorization );

	    // get RMI ref to application and execute request
	    IRemoteApp app = getApp (appID);
	    ResponseTrans restrans = null;
	    try {
	        restrans = app.execute (reqtrans);
	    } catch (RemoteException cnx) {
	        invalidateApp (appID);
	        app = getApp (appID);
	        app.ping ();
                restrans = app.execute (reqtrans);
	    }
	    writeResponse (response, restrans, cookies, protocol);

	} catch (Exception x) {
	    invalidateApp (appID);
	    try {
	        response.setContentType ("text/html");
	        Writer out = response.getWriter ();
	        if (debug)
	            out.write ("<b>Error:</b><br>" +x);
	        else
	            out.write ("This server is temporarily unavailable. Please check back later.");
	        out.flush ();
	    } catch (Exception io_e) {}
	}
    }


    void writeResponse (HttpServletResponse res, ResponseTrans trans, Cookie[] cookies, String protocol) {

	for (int i = 0; i < trans.countCookies(); i++) try {
	    Cookie c = new Cookie(trans.getKeyAt(i), trans.getValueAt(i));
	    c.setPath ("/");
	    if (cookieDomain != null)
	        c.setDomain (cookieDomain);
	    int expires = trans.getDaysAt(i);
	    if (expires > 0)
	        c.setMaxAge(expires * 60*60*24);   // Cookie time to live, days -> seconds
	    res.addCookie(c);
	} catch (Exception ign) {}

	if (trans.getRedirect () != null) {
	    try {
	        res.sendRedirect(trans.getRedirect ());
	    } catch(Exception io_e) {}

	} else {

	    if (!trans.cache || ! caching) {
	        // Disable caching of response.
	        if (protocol == null || !protocol.endsWith ("1.1"))
	            res.setHeader ("Pragma", "no-cache"); // for HTTP 1.0
	        else
	            res.setHeader ("Cache-Control", "no-cache"); // for HTTP 1.1
	    }
	    if ( trans.realm!=null )
	        res.setHeader( "WWW-Authenticate", "Basic realm=\"" + trans.realm + "\"" );
	    if (trans.status > 0)
	        res.setStatus (trans.status);
	    // if we don't know which charset to use for parsing HTTP params,
	    // take the one from the response. This usually works because
	    // browsers send parrameters in the same encoding as the page
	    // containing the form has. Problem is we can do this only per servlet,
	    // not per session or even per page, which would produce too much overhead
	    if (defaultEncoding == null)
	        defaultEncoding = trans.charset;
	    res.setContentLength (trans.getContentLength ());
	    res.setContentType (trans.getContentType ());
	    try {
	        OutputStream out = res.getOutputStream ();
	        out.write (trans.getContent ());
	        out.close ();
	    } catch(Exception io_e) {
	        System.err.println ("Exception in writeResponse: "+io_e);
	    }
	}
    }
	
    private void redirectResponse (HttpServletRequest request, HttpServletResponse res, ResponseTrans trans, String url) { 		
	try { 
	    res.sendRedirect(url); 
	} catch (Exception e) { 
	    System.err.println ("Exception at redirect: " + e + e.getMessage());
	}
    }
	

    public FileUpload getUpload (HttpServletRequest request) throws Exception {
	int contentLength = request.getContentLength ();
	BufferedInputStream in = new BufferedInputStream (request.getInputStream ());
	if (contentLength > uploadLimit*1024) {
	    // consume all input to make Apache happy
	    byte b[] = new byte[1024];
	    int read = 0;
	    while (in.available () > 0)
	         read = in.read (b, 0, 1024);
	    throw new RuntimeException ("Upload exceeds limit of "+uploadLimit+" kb.");
	}
	String contentType = request.getContentType ();
	FileUpload upload = new FileUpload(uploadLimit);
	upload.load (in, contentType, contentLength);
	return upload;
    }


    public Object getUploadPart(FileUpload upload, String name) {
	return upload.getParts().get(name);
    }


    /**
     * Put name value pair in map.
     *
     * @param b the character value byte
     *
     * Put name and value pair in map.  When name already exist, add value
     * to array of values.
     */
    private static void putMapEntry( Map map, String name, String value) {
        String[] newValues = null;
        String[] oldValues = (String[]) map.get(name);
        if (oldValues == null) {
            newValues = new String[1];
            newValues[0] = value;
        } else {
            newValues = new String[oldValues.length + 1];
            System.arraycopy(oldValues, 0, newValues, 0, oldValues.length);
            newValues[oldValues.length] = value;
        }
        map.put(name, newValues);
    }



    protected Map parseParameters (HttpServletRequest request) {

        String encoding = request.getCharacterEncoding ();
        if (encoding == null)
            // no encoding from request, use standard one
            encoding = defaultEncoding;
        if (encoding == null)
            encoding = "ISO-8859-1";

        HashMap parameters = new HashMap ();
        // Parse any query string parameters from the request
        try {
            parseParameters (parameters, request.getQueryString().getBytes(), encoding);
        } catch (Exception e) {
        }

        // Parse any posted parameters in the input stream
        if ("POST".equals(request.getMethod()) &&
            "application/x-www-form-urlencoded".equals(request.getContentType())) {
            try {
                int max = request.getContentLength();
                int len = 0;
                byte buf[] = new byte[max];
                ServletInputStream is = request.getInputStream();
                while (len < max) {
                    int next = is.read(buf, len, max - len);
                    if (next < 0 ) {
                        break;
                    }
                    len += next;
                }
                // is.close();
                parseParameters(parameters, buf, encoding);
            } catch (IllegalArgumentException e) {
            } catch (IOException e) {
            }
        }

	return parameters;
    }

    /**
     * Append request parameters from the specified String to the specified
     * Map.  It is presumed that the specified Map is not accessed from any
     * other thread, so no synchronization is performed.
     * <p>
     * <strong>IMPLEMENTATION NOTE</strong>:  URL decoding is performed
     * individually on the parsed name and value elements, rather than on
     * the entire query string ahead of time, to properly deal with the case
     * where the name or value includes an encoded "=" or "&" character
     * that would otherwise be interpreted as a delimiter.
     *
     * NOTE: byte array data is modified by this method.  Caller beware.
     *
     * @param map Map that accumulates the resulting parameters
     * @param data Input string containing request parameters
     * @param encoding Encoding to use for converting hex
     *
     * @exception UnsupportedEncodingException if the data is malformed
     */
    public static void parseParameters (Map map, byte[] data, String encoding)
        throws UnsupportedEncodingException {

        if (data != null && data.length > 0) {
            int    pos = 0;
            int    ix = 0;
            int    ox = 0;
            String key = null;
            String value = null;
            while (ix < data.length) {
                byte c = data[ix++];
                switch ((char) c) {
                case '&':
                    value = new String(data, 0, ox, encoding);
                    if (key != null) {
                        putMapEntry(map, key, value);
                        key = null;
                    }
                    ox = 0;
                    break;
                case '=':
                    key = new String(data, 0, ox, encoding);
                    ox = 0;
                    break;
                case '+':
                    data[ox++] = (byte)' ';
                    break;
                case '%':
                    data[ox++] = (byte)((convertHexDigit(data[ix++]) << 4)
                                    + convertHexDigit(data[ix++]));
                    break;
                default:
                    data[ox++] = c;
                }
            }
            //The last value does not end in '&'.  So save it now.
            if (key != null) {
                value = new String(data, 0, ox, encoding);
                putMapEntry(map, key, value);
            }
        }
    }

    /**
     * Convert a byte character value to hexidecimal digit value.
     *
     * @param b the character value byte
     */
    private static byte convertHexDigit( byte b ) {
        if ((b >= '0') && (b <= '9')) return (byte)(b - '0');
        if ((b >= 'a') && (b <= 'f')) return (byte)(b - 'a' + 10);
        if ((b >= 'A') && (b <= 'F')) return (byte)(b - 'A' + 10);
        return 0;
    }

    public String getServletInfo(){
	return new String("Helma Servlet Client");
    }


}

