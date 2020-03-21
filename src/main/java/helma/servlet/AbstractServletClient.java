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
import java.security.SecureRandom;
import java.security.NoSuchAlgorithmException;
import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.*;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.servlet.ServletRequestContext;

/**
 * This is an abstract Hop servlet adapter. This class communicates with hop applications
 * via RMI. Subclasses are either one servlet per app, or one servlet that handles multiple apps
 */
public abstract class AbstractServletClient extends HttpServlet {

    private static final long serialVersionUID = -6096445259839663680L;

    // limit to HTTP uploads per file in kB
    int uploadLimit = 1024;

    // limit to HTTP upload
    int totalUploadLimit = 1024;

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

    // Random number generator for session ids
    Random random;
    // whether the random number generator is secure
    boolean secureRandom;

    /**
     * Init this servlet.
     *
     * @param init the servlet configuration
     *
     * @throws ServletException ...
     */
    public void init(ServletConfig init) throws ServletException {
        super.init(init);

        // get max size for file uploads per file
        String upstr = init.getInitParameter("uploadLimit");
        try {
            uploadLimit = (upstr == null) ? 1024 : Integer.parseInt(upstr);
        } catch (NumberFormatException x) {
            log("Bad number format for uploadLimit: " + upstr);
            uploadLimit = 1024;
        }
        // get max total upload size
        upstr = init.getInitParameter("totalUploadLimit");
        try {
            totalUploadLimit = (upstr == null) ? uploadLimit : Integer.parseInt(upstr);
        } catch (NumberFormatException x) {
            log("Bad number format for totalUploadLimit: " + upstr);
            totalUploadLimit = uploadLimit;
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

        // Get random number generator for session ids
        try {
            random = SecureRandom.getInstance("SHA1PRNG");
            secureRandom = true;
        } catch (NoSuchAlgorithmException nsa) {
            random = new Random();
            secureRandom = false;
        }
        random.setSeed(random.nextLong() ^ System.currentTimeMillis()
                                         ^ hashCode()
                                         ^ Runtime.getRuntime().freeMemory());
        random.nextLong();

    }

    /**
     * Abstract method to get the {@link helma.framework.core.Application Applicaton}
     * instance the servlet is talking to.
     *
     * @return this servlet's application instance
     */
    public abstract Application getApplication();

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

            // read cookies
            Cookie[] reqCookies = request.getCookies();

            if (reqCookies != null) {
                for (int i = 0; i < reqCookies.length; i++) {
                    try {
                        // get Cookies
                        String key = reqCookies[i].getName();

                        if (sessionCookieName.equals(key)) {
                            reqtrans.setSession(reqCookies[i].getValue());
                        }
                        reqtrans.setCookie(key, reqCookies[i]);
                    } catch (Exception badCookie) {
                        log("Error setting cookie", badCookie);
                    }
                }
            }

            // get the cookie domain to use for this response, if any.
            String resCookieDomain = cookieDomain;

            if (resCookieDomain != null) {
                // check if cookieDomain is valid for this response.
                // (note: cookieDomain is guaranteed to be lower case)
                // check for x-forwarded-for header, fix for bug 443
                String proxiedHost = request.getHeader("x-forwarded-host");
                if (proxiedHost != null) {
                    if (proxiedHost.toLowerCase().indexOf(resCookieDomain) == -1) {
                        resCookieDomain = null;
                    }
                } else {
                    String host = (String) reqtrans.get("http_host");
                    // http_host is guaranteed to be lower case
                    if (host != null && host.indexOf(resCookieDomain) == -1) {
                        resCookieDomain = null;
                    }
                }
            }

            // check if session cookie is present and valid, creating it if not.
            checkSessionCookie(request, response, reqtrans, resCookieDomain);

            // read and set http parameters
            parseParameters(request, reqtrans, encoding);

            // read file uploads
            List uploads = null;
            ServletRequestContext reqcx = new ServletRequestContext(request);

            if (ServletFileUpload.isMultipartContent(reqcx)) {
                // get session for upload progress monitoring
                UploadStatus uploadStatus = getApplication().getUploadStatus(reqtrans);
                try {
                    uploads = parseUploads(reqcx, reqtrans, uploadStatus, encoding);
                } catch (Exception upx) {
                    log("Error in file upload", upx);
                    String message;
                    boolean tooLarge = (upx instanceof FileUploadBase.SizeLimitExceededException);
                    if (tooLarge) {
                        message = "File upload size exceeds limit of " + uploadLimit + " kB";
                    } else {
                        message = upx.getMessage();
                        if (message == null || message.length() == 0) {
                            message = upx.toString();
                        }
                    }
                    if (uploadStatus != null) {
                        uploadStatus.setError(message);
                    }

                    if (uploadSoftfail || uploadStatus != null) {
                        reqtrans.set("helma_upload_error", message);
                    } else {
                        int errorCode = tooLarge ?
                                HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE:
                                HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
                        sendError(response, errorCode, "Error in file upload: " + message);
                        return;
                    }
                }
            }

            ResponseTrans restrans = getApplication().execute(reqtrans);

            // delete uploads if any
            if (uploads != null) {
                for (int i = 0; i < uploads.size(); i++) {
                    ((FileItem) uploads.get(i)).delete();
                }
            }

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
                    } catch (Exception x) {
                        getApplication().logEvent("Error adding cookie: " + x);
                    }
            }

            // write response
            writeResponse(request, response, reqtrans, restrans);
        } catch (Exception x) {
            log("Exception in execute", x);
            try {
                if (debug) {
                    sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                              "Server error: " + x);
                } else {
                    sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                              "The server encountered an error while processing your request. " +
                              "Please check back later.");
                }
            } catch (IOException iox) {
                log("Exception in sendError", iox);
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
            sendRedirect(req, res, hopres.getRedirect(), hopres.getStatus());
        } else if (hopres.getNotModified()) {
            res.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
        } else {
            if (!hopres.isCacheable() || !caching) {
                // Disable caching of response.
                if (isOneDotOne(req.getProtocol())) {
                    // for HTTP 1.1
                    res.setHeader("Cache-Control",
                                  "no-cache, no-store, must-revalidate, max-age=0");
                } else {
                    // for HTTP 1.0
                    res.setDateHeader("Expires", System.currentTimeMillis() - 10000);
                    res.setHeader("Pragma", "no-cache");
                }
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

            if (!"HEAD".equalsIgnoreCase(req.getMethod())) {
                byte[] content = hopres.getContent();
                if (content != null) {
                    try {
                        OutputStream out = res.getOutputStream();
                        out.write(content);
                        out.flush();
                    } catch (Exception iox) {
                        log("Exception in writeResponse: " + iox);
                    }
                }
            }
        }
    }

    void sendError(HttpServletResponse response, int code, String message)
            throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.reset();
        response.setStatus(code);
        response.setContentType("text/html");

        if (!"true".equalsIgnoreCase(getApplication().getProperty("suppressErrorPage"))) {
            Writer writer = response.getWriter();

            writer.write("<html><body><h3>");
            writer.write("Error in application ");
            try {
                writer.write(getApplication().getName());
            } catch (Exception besafe) {
                // ignore
            }
            writer.write("</h3>");
            writer.write(message);
            writer.write("</body></html>");
            writer.flush();
        }
    }

    void sendRedirect(HttpServletRequest req, HttpServletResponse res, String url, int status) {
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

        // if status code was explicitly set use that, or use 303 for HTTP 1.1,
        // 302 for earlier protocol versions
        if (status >= 301 && status <= 303) {
            res.setStatus(status);
        } else if (isOneDotOne(req.getProtocol())) {
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
        // Jetty 5.1 bails at forward paths without leading slash, so fix it
        if (!forward.startsWith("/")) {
            forward = "/" + forward;
        }
        ServletContext cx = getServletConfig().getServletContext();
        String path = cx.getRealPath(forward);
        if (path == null) {
            throw new IOException("Resource " + forward + " not found");
        }

        File file = new File(path);
        // check if the client has an up-to-date copy so we can
        // send a not-modified response
        if (checkNotModified(file, req, res)) {
            res.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }
        int length = (int) file.length();
        res.setContentLength(length);
        // Erase charset so content-type is not messed with.
        hopres.setCharset(null);
        res.setContentType(hopres.getContentType());
        // Define full Content-Range, as required by HTML5 video and audio
        res.setHeader("Content-Range", "bytes 0-" + length + "/" + length);

        InputStream in = cx.getResourceAsStream(forward);
        if (in == null) {
            throw new IOException("Can't read " + path);
        }
        try {
            OutputStream out = res.getOutputStream();

            int bufferSize = 4096;
            byte buffer[] = new byte[bufferSize];
            int l;

            while (length > 0) {
                if (length < bufferSize) {
                    l = in.read(buffer, 0, length);
                } else {
                    l = in.read(buffer, 0, bufferSize);
                }
                if (l == -1) {
                    break;
                }

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
        String etag = "\"" + new String(Base64.encodeBase64(checksum)) + "\"";  //$NON-NLS-1$//$NON-NLS-2$
        res.setHeader("ETag", etag); //$NON-NLS-1$
        String etagHeader = req.getHeader("If-None-Match"); //$NON-NLS-1$
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
            StringBuffer buffer = new StringBuffer();
            addIPAddress(buffer, request.getRemoteAddr());
            addIPAddress(buffer, request.getHeader("X-Forwarded-For"));
            addIPAddress(buffer, request.getHeader("Client-ip"));
            if (reqtrans.getSession() == null || !reqtrans.getSession().startsWith(buffer.toString())) {
                createSession(response, buffer.toString(), reqtrans, domain);
            }
        } else if (reqtrans.getSession() == null) {
            createSession(response, "", reqtrans, domain);
        }
    }

    /**
     * Create a new session cookie.
     *
     * @param response the servlet response
     * @param prefix the session id prefix
     * @param reqtrans the request object
     * @param domain the cookie domain
     */
    private void createSession(HttpServletResponse response,
                               String prefix,
                               RequestTrans reqtrans,
                               String domain) {
        Application app = getApplication();
        String id = null;
        while (id == null || app.getSession(id) != null) {
            long l = secureRandom ?
                    random.nextLong() :
                    random.nextLong() + Runtime.getRuntime().freeMemory() ^ hashCode();
            if (l < 0)
                l = -l;
            id = prefix + Long.toString(l, 36);
        }

        reqtrans.setSession(id);

        StringBuffer buffer = new StringBuffer(sessionCookieName);
        buffer.append("=").append(id).append("; Path=/");
        if (domain != null) {
            // lowercase domain for IE
            buffer.append("; Domain=").append(domain.toLowerCase());
        }
        if (!"false".equalsIgnoreCase(app.getProperty("cookies.httpOnly"))) {
            buffer.append("; HttpOnly");
        }
        if ("true".equalsIgnoreCase(app.getProperty("cookies.secure"))) {
            buffer.append("; Secure");
        }
        response.addHeader("Set-Cookie", buffer.toString());
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

    protected List parseUploads(ServletRequestContext reqcx, RequestTrans reqtrans,
                                final UploadStatus uploadStatus, String encoding)
            throws FileUploadException, UnsupportedEncodingException {
        // handle file upload
        DiskFileItemFactory factory = new DiskFileItemFactory();
        FileUpload upload = new FileUpload(factory);
        // use upload limit for individual file size, but also set a limit on overall size
        upload.setFileSizeMax(uploadLimit * 1024);
        upload.setSizeMax(totalUploadLimit * 1024);

        // register upload tracker with user's session
        if (uploadStatus != null) {
            upload.setProgressListener(new ProgressListener() {
                public void update(long bytesRead, long contentLength, int itemsRead) {
                    uploadStatus.update(bytesRead, contentLength, itemsRead);
                }
            });
        }

        List uploads = upload.parseRequest(reqcx);
        Iterator it = uploads.iterator();

        while (it.hasNext()) {
            FileItem item = (FileItem) it.next();
            String name = item.getFieldName();
            Object value;
            // check if this is an ordinary HTML form element or a file upload
            if (item.isFormField()) {
                value =  item.getString(encoding);
            } else {
                value = new MimePart(item);
            }
            // if multiple values exist for this name, append to _array
            reqtrans.addPostParam(name, value);
        }
        return uploads;
    }

    protected void parseParameters(HttpServletRequest request, RequestTrans reqtrans,
                                  String encoding)
            throws IOException {
        // check if there are any parameters before we get started
        String queryString = request.getQueryString();
        String contentType = request.getContentType();
        boolean isFormPost = "post".equals(request.getMethod().toLowerCase())
                && contentType != null
                && contentType.toLowerCase().startsWith("application/x-www-form-urlencoded");

        if (queryString == null && !isFormPost) {
            return;
        }

        HashMap parameters = new HashMap();

        // Parse any query string parameters from the request
        if (queryString != null) {
            parseParameters(parameters, queryString.getBytes(), encoding, false);
            if (!parameters.isEmpty()) {
                reqtrans.setParameters(parameters, false);
                parameters.clear();
            }
        }

        // Parse any posted parameters in the input stream
        if (isFormPost) {
            int max = request.getContentLength();
            if (max > totalUploadLimit * 1024) {
                throw new IOException("Exceeded Upload limit");
            }
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
            parseParameters(parameters, buf, encoding, true);
            if (!parameters.isEmpty()) {
                reqtrans.setParameters(parameters, true);
                parameters.clear();
            }
        }
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
    public static void parseParameters(Map map, byte[] data, String encoding, boolean isPost)
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
                        if (key == null) {
                            key = new String(data, 0, ox, encoding);
                            ox = 0;
                        } else {
                            data[ox++] = c;
                        }

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

            if (key != null) {
                // The last value does not end in '&'.  So save it now.
                value = new String(data, 0, ox, encoding);
                putMapEntry(map, key, value);
            } else if (ox > 0) {
                // Store any residual bytes in req.data.http_post_remainder
                value = new String(data, 0, ox, encoding);
                if (isPost) {
                    putMapEntry(map, "http_post_remainder", value);
                } else {
                    putMapEntry(map, "http_get_remainder", value);
                }
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
        StringBuffer pathbuffer = new StringBuffer();

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
