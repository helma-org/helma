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

/* Portierung von helma.asp.AspClient auf Servlets */
/* Author: Raphael Spannocchi Datum: 27.11.1998 */

package helma.servlet;

import helma.framework.*;
import helma.framework.core.Application;
import helma.util.*;
import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.servlet.ServletRequestContext;

/**
 * This is an abstract Hop servlet adapter. This class communicates with hop applications
 * via RMI. Subclasses are either one servlet per app, or one servlet that handles multiple apps
 */
public abstract class AbstractServletClient extends HttpServlet {

    // host on which Helma app is running
    String host = null;

    // port of Helma RMI server
    int port = 0;

    // RMI url of Helma app
    String hopUrl;

    // limit to HTTP uploads in kB
    int uploadLimit = 1024;

    // cookie domain to use
    String cookieDomain;

    // cookie name for session cookies
    String sessionCookieName = "HopSession";

    // this tells us whether to bind session cookies to client ip subnets
    // so they can't be easily used from other ip addresses when hijacked
    boolean protectedSessionCookie = true;

    // allow caching of responses
    boolean caching;

    // enable debug output
    boolean debug;

    // soft fail on file upload errors by setting flag "helma_upload_error" in RequestTrans
    // if fals, an error response is written to the client immediately without entering helma
    boolean uploadSoftfail = false;

    /**
     * Init this servlet.
     *
     * @param init the servlet configuration
     *
     * @throws ServletException ...
     */
    public void init(ServletConfig init) throws ServletException {
        super.init(init);

        // get max size for file uploads
        String upstr = init.getInitParameter("uploadLimit");
        try {
            uploadLimit = (upstr == null) ? 1024 : Integer.parseInt(upstr);
        } catch (NumberFormatException x) {
            System.err.println("Bad number format for uploadLimit: " + upstr);
            uploadLimit = 1024;
        }

        // soft fail mode for upload errors
        uploadSoftfail = ("true".equalsIgnoreCase(init.getInitParameter("uploadSoftfail")));

        // get cookie domain
        cookieDomain = init.getInitParameter("cookieDomain");
        if (cookieDomain != null) {
            cookieDomain = cookieDomain.toLowerCase();
        }

        // get session cookie name
        sessionCookieName = init.getInitParameter("sessionCookieName");
        if (sessionCookieName == null) {
            sessionCookieName = "HopSession";
        }

        // disable binding session cookie to ip address?
        protectedSessionCookie = !("false".equalsIgnoreCase(init.getInitParameter("protectedSessionCookie")));

        // debug mode for printing out detailed error messages
        debug = ("true".equalsIgnoreCase(init.getInitParameter("debug")));

        // generally disable response caching for clients?
        caching = !("false".equalsIgnoreCase(init.getInitParameter("caching")));
    }

    /**
     * Abstract method to get the {@link helma.framework.core.Application Applicaton}
     * instance the servlet is talking to.
     *
     * @return this servlet's application instance
     */
    abstract Application getApplication();

    /**
     * Handle a request.
     *
     * @param request ...
     * @param response ...
     *
     * @throws ServletException ...
     * @throws IOException ...
     */
    protected void service (HttpServletRequest request, HttpServletResponse response)
                throws ServletException, IOException {

        RequestTrans reqtrans = new RequestTrans(request, response, getPathInfo(request));

        try {
            // get the character encoding
            String encoding = request.getCharacterEncoding();

            if (encoding == null) {
                // no encoding from request, use the application's charset
                encoding = getApplication().getCharset();
            }

            // read and set http parameters
            Map parameters = parseParameters(request, encoding);

            for (Iterator i = parameters.entrySet().iterator(); i.hasNext();) {
                Map.Entry entry = (Map.Entry) i.next();
                String key = (String) entry.getKey();
                String[] values = (String[]) entry.getValue();

                if ((values != null) && (values.length > 0)) {
                    reqtrans.set(key, values[0]); // set to single string value

                    if (values.length > 1) {
                        reqtrans.set(key + "_array", values); // set string array
                    }
                }
            }

            try {
                ServletRequestContext reqcx = new ServletRequestContext(request);
                if (ServletFileUpload.isMultipartContent(reqcx)) {
                    // File Upload
                    ServletFileUpload upload = new ServletFileUpload(new DiskFileItemFactory());

                    upload.setSizeMax(uploadLimit * 1024);
                    upload.setHeaderEncoding(encoding);

                    List uploads = upload.parseRequest(request);
                    Iterator it = uploads.iterator();

                    while (it.hasNext()) {
                        FileItem item = (FileItem) it.next();
                        String name = item.getFieldName();
                        Object value = null;
                        // check if this is an ordinary HTML form element or a file upload
                        if (item.isFormField()) {
                            value =  item.getString(encoding);
                        } else {
                            value = new MimePart(item.getName(),
                                    item.get(),
                                    item.getContentType());
                        }
                        item.delete();
                        // if multiple values exist for this name, append to _array
                        if (reqtrans.get(name) != null) {
                            appendFormValue(reqtrans, name, value);
                        } else {
                            reqtrans.set(name, value);
                        }
                    }

                }
            } catch (Exception upx) {
                System.err.println("Error in file upload: " + upx);
                if (uploadSoftfail) {
                    String msg = upx.getMessage();
                    if (msg == null || msg.length() == 0) {
                        msg = upx.toString();
                    }
                    reqtrans.set("helma_upload_error", msg);
                } else if (upx instanceof FileUploadBase.SizeLimitExceededException) {
                    sendError(response, HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE,
                            "File upload size exceeds limit of " + uploadLimit + "kB");
                    return;
                } else {
                    sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                            "Error in file upload: " + upx);
                    return;
                }
            }

            // read cookies
            Cookie[] reqCookies = request.getCookies();

            if (reqCookies != null) {
                for (int i = 0; i < reqCookies.length; i++)
                    try {
                        // get Cookies
                        String nextKey = reqCookies[i].getName();
                        String nextPart = reqCookies[i].getValue();

                        if (sessionCookieName.equals(nextKey)) {
                            reqtrans.setSession(nextPart);
                        } else {
                            reqtrans.set(nextKey, nextPart);
                        }               
                    } catch (Exception badCookie) {
                        // ignore
                    }
            }

            // do standard HTTP variables
            String host = request.getHeader("Host");

            if (host != null) {
                host = host.toLowerCase();
                reqtrans.set("http_host", host);
            }

            String referer = request.getHeader("Referer");

            if (referer != null) {
                reqtrans.set("http_referer", referer);
            }

            try {
                long ifModifiedSince = request.getDateHeader("If-Modified-Since");

                if (ifModifiedSince > -1) {
                    reqtrans.setIfModifiedSince(ifModifiedSince);
                }
            } catch (IllegalArgumentException ignore) {
            }

            String ifNoneMatch = request.getHeader("If-None-Match");

            if (ifNoneMatch != null) {
                reqtrans.setETags(ifNoneMatch);
            }

            String remotehost = request.getRemoteAddr();

            if (remotehost != null) {
                reqtrans.set("http_remotehost", remotehost);
            }

            // get the cookie domain to use for this response, if any.
            String resCookieDomain = cookieDomain;

            if (resCookieDomain != null) {
                // check if cookieDomain is valid for this response.
                // (note: cookieDomain is guaranteed to be lower case)
                // check for x-forwarded-for header, fix for bug 443
                String proxiedHost = request.getHeader("x-forwarded-host");
                if (proxiedHost != null) {
                    if (proxiedHost.toLowerCase().indexOf(cookieDomain) == -1) {
                        resCookieDomain = null;
                    }
                } else if ((host != null) &&
                        host.toLowerCase().indexOf(cookieDomain) == -1) {
                    resCookieDomain = null;
                }
            }

            // check if session cookie is present and valid, creating it if not.
            checkSessionCookie(request, response, reqtrans, resCookieDomain);

            String browser = request.getHeader("User-Agent");

            if (browser != null) {
                reqtrans.set("http_browser", browser);
            }
           
            String language = request.getHeader("Accept-Language");
            
            if (language != null) {
                reqtrans.set("http_language", language);
            } 
            
            String authorization = request.getHeader("authorization");

            if (authorization != null) {
                reqtrans.set("authorization", authorization);
            }

            ResponseTrans restrans = getApplication().execute(reqtrans);

            // if the response was already written and committed by the application
            // we can skip this part and return
            if (response.isCommitted()) {
                return;
            }

            // set cookies
            if (restrans.countCookies() > 0) {
                CookieTrans[] resCookies = restrans.getCookies();

                for (int i = 0; i < resCookies.length; i++)
                    try {
                        Cookie c = resCookies[i].getCookie("/", resCookieDomain);

                        response.addCookie(c);
                    } catch (Exception ignore) {
                    }
            }

            // write response
            writeResponse(request, response, reqtrans, restrans);
        } catch (Exception x) {
            try {
                if (debug) {
                    sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                              "Server error: " + x);
                    x.printStackTrace();
                } else {
                    sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                              "The server encountered an error while processing your request. " +
                              "Please check back later.");
                }

                log("Exception in execute: " + x);
            } catch (IOException io_e) {
                log("Exception in sendError: " + io_e);
            }
        }
    }

    protected void writeResponse(HttpServletRequest req, HttpServletResponse res,
                                 RequestTrans hopreq, ResponseTrans hopres)
            throws IOException {
        if (hopres.getForward() != null) {
            sendForward(res, req, hopres);
            return;
        }

        if (hopres.getETag() != null) {
            res.setHeader("ETag", hopres.getETag());
        }

        if (hopres.getRedirect() != null) {
            sendRedirect(req, res, hopres.getRedirect());
        } else if (hopres.getNotModified()) {
            res.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
        } else {
            if (!hopres.isCacheable() || !caching) {
                // Disable caching of response.
                // for HTTP 1.0
                res.setDateHeader("Expires", System.currentTimeMillis() - 10000);
                res.setHeader("Pragma", "no-cache");

                // for HTTP 1.1
                res.setHeader("Cache-Control",
                              "no-cache, no-store, must-revalidate, max-age=0");
            }

            if (hopres.getRealm() != null) {
                res.setHeader("WWW-Authenticate", "Basic realm=\"" + hopres.getRealm() + "\"");
            }

            if (hopres.getStatus() > 0) {
                res.setStatus(hopres.getStatus());
            }

            // set last-modified header to now
            long modified = hopres.getLastModified();
            if (modified > -1) {
                res.setDateHeader("Last-Modified", modified);
            }

            res.setContentLength(hopres.getContentLength());
            res.setContentType(hopres.getContentType());

            if ("HEAD".equalsIgnoreCase(req.getMethod())) {
                return;
            }

            try {
                OutputStream out = res.getOutputStream();

                out.write(hopres.getContent());
                out.flush();
            } catch (Exception io_e) {
                log("Exception in writeResponse: " + io_e);
            }
        }
    }

    void sendError(HttpServletResponse response, int code, String message)
            throws IOException {
        response.reset();
        response.setStatus(code);
        response.setContentType("text/html");

        Writer writer = response.getWriter();

        writer.write("<html><body><h3>");
        writer.write("Error in application ");
        try {
            writer.write(getApplication().getName());
        } catch (Exception besafe) {}
        writer.write("</h3>");
        writer.write(message);
        writer.write("</body></html>");
        writer.flush();
    }

    void sendRedirect(HttpServletRequest req, HttpServletResponse res, String url) {
        String location = url;

        if (url.indexOf("://") == -1) {
            // need to transform a relative URL into an absolute one
            String scheme = req.getScheme();
            StringBuffer loc = new StringBuffer(scheme);

            loc.append("://");
            loc.append(req.getServerName());

            int p = req.getServerPort();

            // check if we need to include server port
            if ((p > 0) &&
                    (("http".equals(scheme) && (p != 80)) ||
                    ("https".equals(scheme) && (p != 443)))) {
                loc.append(":");
                loc.append(p);
            }

            if (!url.startsWith("/")) {
                String requri = req.getRequestURI();
                int lastSlash = requri.lastIndexOf("/");

                if (lastSlash == (requri.length() - 1)) {
                    loc.append(requri);
                } else if (lastSlash > -1) {
                    loc.append(requri.substring(0, lastSlash + 1));
                } else {
                    loc.append("/");
                }
            }

            loc.append(url);
            location = loc.toString();
        }

        // send status code 303 for HTTP 1.1, 302 otherwise
        if (isOneDotOne(req.getProtocol())) {
            res.setStatus(HttpServletResponse.SC_SEE_OTHER);
        } else {
            res.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
        }

        res.setContentType("text/html");
        res.setHeader("Location", location);
    }

    /**
     * Forward the request to a static file. The file must be reachable via
     * the context's protectedStatic resource base.
     */
    void sendForward(HttpServletResponse res, HttpServletRequest req,
                     ResponseTrans hopres) throws IOException {
        String forward = hopres.getForward();
        ServletContext cx = getServletConfig().getServletContext();
        String path = cx.getRealPath(forward);
        if (path == null)
            throw new IOException("Resource " + forward + " not found");

        File file = new File(path);
        // check if the client has an up-to-date copy so we can
        // send a not-modified response
        if (checkNotModified(file, req, res)) {
            res.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }
        int length = (int) file.length();
        res.setContentLength(length);
        res.setContentType(hopres.getContentType());

        InputStream in = cx.getResourceAsStream(forward);
        if (in == null)
            throw new IOException("Can't read " + path);
        try {
            OutputStream out = res.getOutputStream();
    
            int bufferSize = 4096;
            byte buffer[] = new byte[bufferSize];
            int l;
    
            while (length>0) {
                if (length < bufferSize)
                    l = in.read(buffer, 0, length);
                else
                    l = in.read(buffer, 0, bufferSize);
    
                if (l == -1)
                    break;
    
                length -= l;
                out.write(buffer, 0, l);
            }
        } finally {
            in.close();
        }
    }

    private boolean checkNotModified(File file, HttpServletRequest req, HttpServletResponse res) {
        // we do two rounds of conditional requests:
        // first ETag based, then based on last modified date.
        // calculate ETag checksum on last modified date and content length.
        byte[] checksum = new byte[16];
        long n = file.lastModified();
        for (int i=0; i<8; i++) {
            checksum[i] = (byte) (n);
            n >>>= 8;
        }
        n = file.length();
        for (int i=8; i<16; i++) {
            checksum[i] = (byte) (n);
            n >>>= 8;
        }
        String etag = "\"" + new String(Base64.encode(checksum)) + "\"";
        res.setHeader("ETag", etag);
        String etagHeader = req.getHeader("If-None-Match");
        if (etagHeader != null) {
            StringTokenizer st = new StringTokenizer(etagHeader, ", \r\n");
            while (st.hasMoreTokens()) {
                if (etag.equals(st.nextToken())) {
                    return true;
                }
            }
        }
        // as a fallback, since some browsers don't support ETag based
        // conditional GET for embedded images and stuff, check last modified date.
        // date headers don't do milliseconds, round to seconds
        long lastModified = (file.lastModified() / 1000) * 1000;
        long ifModifiedSince = req.getDateHeader("If-Modified-Since");
        if (lastModified == ifModifiedSince) {
            return true;
        }
        res.setDateHeader("Last-Modified", lastModified);
        return false;
    }

    /**
     * Used to build the form value array when a multipart (file upload) form has
     * multiple values for one form element name.
     * 
     * @param reqtrans
     * @param name
     * @param value
     */
    private void appendFormValue(RequestTrans reqtrans, String name, Object value) {
        String arrayName = name + "_array";
        try {
            Object[] values = (Object[]) reqtrans.get(arrayName);
            if (values == null) {
                reqtrans.set(arrayName, new Object[] {reqtrans.get(name), value});
            } else {
                Object[] newValues = new Object[values.length + 1];
                System.arraycopy(values, 0, newValues, 0, values.length);
                newValues[values.length] = value;
                reqtrans.set(arrayName, newValues);
            }
        } catch (ClassCastException x) {
            // name_array is defined as something else in the form - don't overwrite it
        }
    }

    /**
     *  Check if the session cookie is set and valid for this request.
     *  If not, create a new one.
     */
    private void checkSessionCookie(HttpServletRequest request,
                                    HttpServletResponse response,
                                    RequestTrans reqtrans,
                                    String domain) {
        // check if we need to create a session id.
        if (protectedSessionCookie) {
            // If protected session cookies are enabled we also force a new session
            // if the existing session id doesn't match the client's ip address
            StringBuffer b = new StringBuffer();
            addIPAddress(b, request.getRemoteAddr());
            addIPAddress(b, request.getHeader("X-Forwarded-For"));
            addIPAddress(b, request.getHeader("Client-ip"));
            if (reqtrans.getSession() == null || !reqtrans.getSession().startsWith(b.toString())) {
                response.addCookie(createSessionCookie(b, reqtrans, domain));
            }
        } else if (reqtrans.getSession() == null) {
            response.addCookie(createSessionCookie(new StringBuffer(), reqtrans, domain));
        }
    }

    /**
     * Create a new session cookie.
     *
     * @param b
     * @param reqtrans
     * @param domain
     * @return the session cookie
     */
    private Cookie createSessionCookie(StringBuffer b,
                                       RequestTrans reqtrans,
                                       String domain) {
        b.append (Long.toString(Math.round(Math.random() * Long.MAX_VALUE) -
                    System.currentTimeMillis(), 36));

        reqtrans.setSession(b.toString());
        Cookie cookie = new Cookie(sessionCookieName, reqtrans.getSession());

        cookie.setPath("/");

        if (domain != null) {
            cookie.setDomain(domain);
        }
        return cookie;
    }

    /**
     *  Adds an the 3 most significant bytes of an IP address header to the
     *  session cookie id. Some headers may contain a list of IP addresses
     *  separated by comma - in that case, care is taken that only the first
     *  one is considered.
     */
    private void addIPAddress(StringBuffer b, String addr) {
        if (addr != null) {
            int cut = addr.indexOf(',');
            if (cut > -1) {
                addr = addr.substring(0, cut);
            }
            cut = addr.lastIndexOf('.');
            if (cut == -1) {
                cut = addr.lastIndexOf(':');
            }
            if (cut > -1) {
                b.append(addr.substring(0, cut+1));
            }
        }
    }


    /**
     * Put name and value pair in map.  When name already exist, add value
     * to array of values.
     */
    private static void putMapEntry(Map map, String name, String value) {
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

    protected Map parseParameters(HttpServletRequest request, String encoding) {
        HashMap parameters = new HashMap();

        // Parse any query string parameters from the request
        String queryString = request.getQueryString();
        if (queryString != null) {
            try {
                parseParameters(parameters, queryString.getBytes(), encoding);
            } catch (Exception e) {
                System.err.println("Error parsing query string: "+e);
            }
        }

        // Parse any posted parameters in the input stream
        if ("POST".equals(request.getMethod()) &&
                "application/x-www-form-urlencoded".equals(request.getContentType())) {
            try {
                int max = request.getContentLength();
                int len = 0;
                byte[] buf = new byte[max];
                ServletInputStream is = request.getInputStream();

                while (len < max) {
                    int next = is.read(buf, len, max - len);

                    if (next < 0) {
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
    public static void parseParameters(Map map, byte[] data, String encoding)
                                throws UnsupportedEncodingException {
        if ((data != null) && (data.length > 0)) {
            int ix = 0;
            int ox = 0;
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
                        data[ox++] = (byte) ' ';

                        break;

                    case '%':
                        data[ox++] = (byte) ((convertHexDigit(data[ix++]) << 4) +
                                     convertHexDigit(data[ix++]));

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
    private static byte convertHexDigit(byte b) {
        if ((b >= '0') && (b <= '9')) {
            return (byte) (b - '0');
        }

        if ((b >= 'a') && (b <= 'f')) {
            return (byte) (b - 'a' + 10);
        }

        if ((b >= 'A') && (b <= 'F')) {
            return (byte) (b - 'A' + 10);
        }

        return 0;
    }

    boolean isOneDotOne(String protocol) {
        if (protocol == null) {
            return false;
        }
        return protocol.endsWith("1.1");
    }

    String getPathInfo(HttpServletRequest req)
            throws UnsupportedEncodingException {
        StringTokenizer t = new StringTokenizer(req.getContextPath(), "/");
        int prefixTokens = t.countTokens();

        t = new StringTokenizer(req.getServletPath(), "/");
        prefixTokens += t.countTokens();

        String uri = req.getRequestURI();
        t = new StringTokenizer(uri, "/");

        int uriTokens = t.countTokens();
        StringBuffer pathbuffer = new StringBuffer("/");

        String encoding = getApplication().getCharset();

        for (int i = 0; i < uriTokens; i++) {
            String token = t.nextToken();

            if (i < prefixTokens) {
                continue;
            }

            if (i > prefixTokens) {
                pathbuffer.append('/');
            }

            pathbuffer.append(UrlEncoded.decode(token, encoding));
        }

        // append trailing "/" if it is contained in original URI
        if (uri.endsWith("/"))
            pathbuffer.append('/');

        return pathbuffer.toString();
    }

    /**
     * Return servlet info
     * @return the servlet info
     */
    public String getServletInfo() {
        return "Helma Servlet Client";
    }
}
