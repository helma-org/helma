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
	
    String host = null;
    int port = 0;
    int uploadLimit;       // limit to HTTP uploads in kB
    String hopUrl;
    String cookieDomain;
    boolean caching;
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
	    for (Enumeration e = request.getParameterNames(); e.hasMoreElements(); ) {
	        String nextKey = (String)e.nextElement();
	        String[] paramValues = request.getParameterValues(nextKey);
	        if (paramValues != null) {
	            reqtrans.set (nextKey, paramValues[0]);    // set to single string value
	            if (paramValues.length > 1)
	                reqtrans.set (nextKey+"_array", paramValues);     // set string array
	        }
	    }

	    // check for MIME file uploads
	    String contentType = request.getContentType();
	    if (contentType != null && contentType.indexOf("multipart/form-data")==0) {
	        // File Upload
	        Uploader up;
	        try {
	            if ((up = getUpload (request)) != null) {
	                Hashtable upload = up.getParts ();
	                for (Enumeration e = upload.keys(); e.hasMoreElements(); ) {
	                    String nextKey = (String) e.nextElement ();
	                    Object nextPart = upload.get (nextKey);
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
	    res.setContentLength (trans.getContentLength ());
	    res.setContentType (trans.getContentType ());
	    try {
	        OutputStream out = res.getOutputStream ();
	        out.write (trans.getContent ());
	        out.close ();
	    } catch(Exception io_e) {}
	}
    }
	
    private void redirectResponse (HttpServletRequest request, HttpServletResponse res, ResponseTrans trans, String url) { 		
	try { 
	    res.sendRedirect(url); 
	} catch (Exception e) { 
	    System.err.println ("Exception at redirect: " + e + e.getMessage());
	}
    }
	

    public Uploader getUpload (HttpServletRequest request) throws Exception {
	int contentLength = request.getContentLength ();
	BufferedInputStream in = new BufferedInputStream (request.getInputStream ());
	Uploader up = null;
	try {
	    if (contentLength > uploadLimit*1024) {
	        // consume all input to make Apache happy
	        byte b[] = new byte[1024];
	        int read = 0;
	        while (read > -1)
	             read = in.read (b, 0, 1024);
	        throw new RuntimeException ("Upload exceeds limit of "+uploadLimit+" kb.");
	    }
	    String contentType = request.getContentType ();
	    up = new Uploader(uploadLimit);
	    up.load (in, contentType, contentLength);
	} finally {
	    try { in.close (); } catch (Exception ignore) {}
	}
	return up;
    }


    public Object getUploadPart(Uploader up, String name) {
	return up.getParts().get(name);
    }


    public String getServletInfo(){
	return new String("Hop Servlet Client");
    }


}



















