/*
 * Helma License Notice
 *
 * The contents of this file are subject to the Helma License
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://adele.helma.org/download/helma/license.txt
 *
 * Copyright 1998-2006 Helma Software. All Rights Reserved.
 *
 * $RCSfile: Http.js,v $
 * $Author: czv $
 * $Revision: 1.2 $
 * $Date: 2006/04/24 07:02:17 $
 */


if (!global.helma) {
    global.helma = {};
}

helma.Http = function() {
    var proxy = null;
    var content = "";
    var userAgent = "Helma Http Client";
    var method = "GET";
    var credentials = null;
    var followRedirects = true;
    var binaryMode = false;
    var headers = {};
    var timeout = {
        "connect": 0,
        "socket": 0
    };

    var setTimeout = function(type, value) {
        var v = java.lang.System.getProperty("java.specification.version");
        if (parseFloat(v, 10) >= 1.5) {
            timeout[type] = value;
        } else {
            app.log("[Helma Http Client] WARNING: timeouts can only be set with Java Runtime version >= 1.5");
        }
        return true;
    }

    /**
     * sets the proxy host and port for later use
     */
    this.setProxy = function(proxyString) {
        var idx = proxyString.indexOf(":");
        var host = proxyString.substring(0, idx);
        var port = proxyString.substring(idx+1);
        if (java.lang.Class.forName("java.net.Proxy") != null) {
            // construct a proxy instance
            var socket = new java.net.InetSocketAddress(host, port);
            proxy = new java.net.Proxy(java.net.Proxy.Type.HTTP, socket);
        } else {
            // the pre jdk1.5 way: set the system properties
            var sys = java.lang.System.getProperties();
            if (host) {
                app.log("[Helma Http Client] WARNING: setting system http proxy to " + host + ":" + port);
                sys.put("http.proxySet", "true");
                sys.put("http.proxyHost", host);
                sys.put("http.proxyPort", port);
            }
        }
        return true;
    };

    /**
     * returns the proxy in "host:port" format
     */
    this.getProxy = function() {
        if (proxy != null) {
            return proxy.address().getHostName() + ":" + proxy.address().getPort();
        } else if (sys.get("http.proxySet") == "true") {
            return sys.get("http.proxyHost") + ":" + sys.get("http.proxyPort");
        } else {
            return null;
        }
    };

    /**
     * sets the credentials for basic http authentication
     */
    this.setCredentials = function(username, password) {
        var str = new java.lang.String(username + ":" + password);
        credentials = (new Packages.sun.misc.BASE64Encoder()).encode(str.getBytes());
        return true;
    }

    this.setContent = function(stringOrObject) {
        if (stringOrObject.constructor == Object) {
            res.push();
            var value;
            for (var key in stringOrObject) {
                value = stringOrObject[key];
                res.write(encodeURIComponent(key));
                res.write("=");
                res.write(encodeURIComponent(value));
                res.write("&");
            }
            content = res.pop();
            content = content.substring(0, content.length-1);
        } else
            content = stringOrObject.toString();
        return;
    };
    
    /**
     * getter/setter for method
     */
    this.setMethod = function(m) {
        method = m;
        return true;
    };
    this.getMethod = function() {
        return method;
    };

    this.setHeader = function(name, value) {
        headers[name] = value;
        return;
    };
    this.getHeader = function(name) {
        return headers[name];
    };

    /**
     * getter/setter for timeouts
     */
    this.setTimeout = function(timeout) {
        setTimeout("connect", timeout);
        return true;
    };
    this.setReadTimeout = function(timeout) {
        setTimeout("socket", timeout);
        return true;
    };
    this.getTimeout = function() {
        return timeout.connect;
    };
    this.getReadTimeout = function() {
        return timeout.socket;
    };

    /**
     * getter/setter for following redirects
     */
    this.setFollowRedirects = function(value) {
        followRedirects = value;
        return;
    };
    this.getFollowRedirects = function() {
        return followRedirects;
    };

    /**
     * getter/setter for user agent string
     */
    this.setUserAgent = function(agent) {
        userAgent = agent;
        return true;
    };
    this.getUserAgent = function() {
        return userAgent;
    };

    /**
     * switches content text encoding on/off
     */
    this.setBinaryMode = function(mode) {
        binaryMode = mode;
    };
    this.getBinaryMode = function() {
        return binaryMode;
    };

    /**
     * executes a http request
     */
    this.getUrl = function(url, opt) {
        if (typeof url == "string") {
            if (!(url = helma.Http.evalUrl(url)))
                throw new Error("'" + url + "' is not a valid URL.");
        } else if (!(url instanceof java.net.URL)) {
            throw new Error("'" + url + "' is not a valid URL.");
        }
        
        var conn = proxy ? url.openConnection(proxy) : url.openConnection();
        conn.setFollowRedirects(followRedirects);
        conn.setAllowUserInteraction(false);
        conn.setRequestMethod(method);
        conn.setRequestProperty("User-Agent", userAgent);

        if (opt) {
            if (opt instanceof Date)
                conn.setIfModifiedSince(opt.getTime());
            else if ((typeof opt == "string") && (opt.length > 0))
                conn.setRequestProperty("If-None-Match", opt);
        }
        if (credentials != null) {
            conn.setRequestProperty("Authorization", "Basic " + credentials);
        }
        if (parseFloat(java.lang.System.getProperty("java.specification.version"), 10) >= 1.5) {
            conn.setConnectTimeout(timeout.connect);
            conn.setReadTimeout(timeout.socket);
        } else {
            app.debug("WARNING: timeouts can only be set due to Java version < 1.5");
        }

        for (var i in headers)
            conn.setRequestProperty(i, headers[i]);

        if (content) {
            conn.setRequestProperty("Content-Length", content.length);
            conn.setDoOutput(true);
            var out = new java.io.OutputStreamWriter(conn.getOutputStream());
            out.write(content);
            out.flush();
            out.close();
        }

        var result = {
            url: conn.getURL(),
            location: conn.getHeaderField("location"),
            code: conn.getResponseCode(),
            message: conn.getResponseMessage(),
            length: conn.getContentLength(),
            type: conn.getContentType(),
            encoding: conn.getContentEncoding(),
            lastModified: null,
            eTag: null,
            content: null
        }
        var lastmod = conn.getLastModified();
        if (lastmod)
            result.lastModified = new Date(lastmod);
        result.eTag = conn.getHeaderField("ETag");
    
        if (result.length != 0 && result.code == 200) {
            var body = new java.io.ByteArrayOutputStream();
            var input = new java.io.BufferedInputStream(conn.getInputStream());
            var buf = java.lang.reflect.Array.newInstance(java.lang.Byte.TYPE, 1024);
            var str;
            while ((str = input.read(buf)) > -1) {
                body.write(buf, 0, str);
            }
            input.close();
            if (binaryMode) {
                result.content = body.toByteArray();
            } else {
                result.content = result.encoding ?
                            body.toString(result.encoding) :
                            body.toString();
            }
            result.length = result.content.length;
        }
        conn.disconnect();
        return result;
    }

    this.toString = function() {
        return "[Helma Http Client]";
    };

    for (var i in this)
        this.dontEnum(i);

    return this;
};


/**
 * removes trailing slash from and evaluates a url
 * @param String the url or uri string
 * @return Object the result with error and result properties
 */
helma.Http.evalUrl = function(url) {
    try {
        return new java.net.URL(url);
    } catch (err) {
        return null;
    }
};


/**
 * set global http proxy
 */
helma.Http.setProxy = function(proxyString) {
    var sys = java.lang.System.getProperties();
    if (proxyString) {
        var idx = proxyString.indexOf(":");
        var host = proxyString.substring(0, idx);
        var port = proxyString.substring(idx+1);
        if (!port)
            port = "3128";
        else if (typeof port == "number")
            port = port.toString();
        app.log("helma.Http.setProxy " + proxyString);
        sys.put("http.proxySet", "true");
        sys.put("http.proxyHost", host);
        sys.put("http.proxyPort", port);
    } else {
        sys.put("http.proxySet", "false");
        sys.put("http.proxyHost", "");
        sys.put("http.prodyPort", "");
    }
    return;
    
};


/**
 * return global proxy settings
 */
helma.Http.getProxy = function() {
    var sys = java.lang.System.getProperties();
    if (sys.get("http.proxySet") == "true")
        return sys.get("http.proxyHost") + ":" + sys.get("http.proxyPort");
    return false;
};


/**
 * static method to check if a request is authorized or not
 * @param String username to check req.username against
 * @param String password to check req.password against
 * @return Boolean true if request is authorized, false otherwise
 */
helma.Http.isAuthorized = function(name, pwd) {
    if (!req.username || !req.password || 
        req.username != name || req.password != pwd) {
        res.reset();
        res.status = 401;
        res.realm = "Helma Http Authorization";
        res.write("Authorization required.");
        return false;
    } else {
        return true;
    }
};


helma.Http.toString = function() {
    return "[helma.Http]";
};


helma.lib = "Http";
helma.dontEnum(helma.lib);
for (var i in helma[helma.lib])
    helma[helma.lib].dontEnum(i);
for (var i in helma[helma.lib].prototype)
    helma[helma.lib].prototype.dontEnum(i);
delete helma.lib;
