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
import helma.objectmodel.Node;
import helma.util.*;

/**
 * This is the HOP servlet adapter. This class communicates with hop applications
 * via RMI. 
 */
 
public class ServletClient extends HttpServlet{
	
    private String host = null;
    private int port = 0;
    private int uploadLimit;       // limit to HTTP uploads in kB
    private Hashtable apps;
    private String appName;
    private String appUrl;
    private String cookieDomain;
    private boolean caching;
    private boolean debug;


    public void init (ServletConfig init) {
	apps = new Hashtable();

	appName = init.getInitParameter ("application");
	if (appName == null) appName = "base";

	host =  init.getInitParameter ("host");
	if (host == null) host = "localhost";

	String portstr = init.getInitParameter ("port");
	port =  portstr == null ? 5055 : Integer.parseInt (portstr);

	String upstr = init.getInitParameter ("uploadLimit");
	uploadLimit =  upstr == null ? 500 : Integer.parseInt (upstr);

	cookieDomain = init.getInitParameter ("cookieDomain");

	appUrl = "//" + host + ":" + port + "/";
	debug = ("true".equalsIgnoreCase (init.getInitParameter ("debug")));

	caching = ! ("false".equalsIgnoreCase (init.getInitParameter ("caching")));
    }


    private IRemoteApp getApp (String appID) throws Exception {
	IRemoteApp retval = (IRemoteApp) apps.get (appID);
	if (retval != null) {
	    return retval;
	}
	retval = (IRemoteApp) Naming.lookup (appUrl + appID);
	apps.put (appID, retval);
	return retval;
    }

    public void doGet (HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException {
	execute (appName, request, response);
    }
	
    public void doPost (HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException {
	execute (appName, request, response);
    }
	
    // not used anymore
    private void get(String appID, HttpServletRequest request, HttpServletResponse response) {
    }
	
    private void execute (String appID, HttpServletRequest request, HttpServletResponse response) {
	String protocol = request.getProtocol ();
	Cookie[] cookies = request.getCookies();
	try {						
	    RequestTrans reqtrans = new RequestTrans ();
	    if (cookies != null) {
	        for (int i=0; i < cookies.length;i++) try {	// get Cookies
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

	    // get optional path info
	    String pathInfo = request.getPathInfo ();
	    if (pathInfo != null) 
	        reqtrans.path = trim (pathInfo);	    
	    else
	        reqtrans.path = "";

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

	    for (Enumeration e = request.getParameterNames(); e.hasMoreElements(); ) {
	        // Params parsen
	        String nextKey = (String)e.nextElement();
	        String[] paramValues = request.getParameterValues(nextKey);
	        String nextValue = paramValues[0];   // Only take first value
	        reqtrans.set (nextKey, nextValue);    // generic Header, Parameter
	    }			

	    String contentType = request.getContentType();
	    if (contentType != null && contentType.indexOf("multipart/form-data")==0) {
	        // File Upload
	        Uploader up;
	        try {
	            if ((up = getUpload (uploadLimit, request)) != null) {
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

	    // get RMI ref to application and execute request
	    IRemoteApp app = getApp (appID);
	    ResponseTrans restrans = null;
	    try {
	        restrans = app.execute (reqtrans);
	    } catch (RemoteException cnx) {
	        apps.remove (appID);
	        app = getApp (appID);
	        app.ping ();
                     restrans = app.execute (reqtrans);
	    }
	    writeResponse (response, restrans, cookies, protocol);

	} catch (Exception x) {
	    apps.remove (appID);
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


    private void writeResponse (HttpServletResponse res, ResponseTrans trans, Cookie[] cookies, String protocol) {			

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

	if (trans.redirect != null) {
	    try { 
	        res.sendRedirect(trans.redirect); 
	    } catch(Exception io_e) {}

	} else {
                 if (!trans.cache || ! caching) {
	        // Disable caching of response.
	        if (protocol == null || !protocol.endsWith ("1.1"))
	            res.setHeader ("Pragma", "no-cache"); // for HTTP 1.0
	        else
	            res.setHeader ("Cache-Control", "no-cache"); // for HTTP 1.1
	    }
	    res.setContentLength (trans.getContentLength ());			
	    res.setContentType (trans.contentType);
	    try {
	        Writer writer = new BufferedWriter (res.getWriter ());
	        writer.write (trans.getContentString ());
	        writer.flush ();
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
	return getUpload (500, request);
    }

    public Uploader getUpload (int maxKbytes, HttpServletRequest request) throws Exception {
	int contentLength = request.getContentLength ();
	BufferedInputStream in = new BufferedInputStream (request.getInputStream ());
	Uploader up = null;
	try {
	    if (contentLength > maxKbytes*1024) {
	        // consume all input to make Apache happy
	        byte b[] = new byte[1024];
	        int read = 0;
	        while (read > -1)
	             read = in.read (b, 0, 1024);
	        throw new RuntimeException ("Upload exceeds limit of "+maxKbytes+" kb.");
	    }
	    String contentType = request.getContentType ();
	    up = new Uploader(maxKbytes);
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
	return new String("Helma ServletClient");
    }


    private String trim (String str) {

	if (str == null) 
	    return null;
	char[] val = str.toCharArray ();
	int len = val.length;
	int st = 0;

	while ((st < len) && (val[st] <= ' ' || val[st] == '/')) {
	    st++;
	}
	while ((st < len) && (val[len - 1] <= ' ' || val[len - 1] == '/')) {
	    len--;
	}
	return ((st > 0) || (len < val.length)) ? new String (val, st, len-st) : str;
    }

}



















