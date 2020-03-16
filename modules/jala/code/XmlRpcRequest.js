//
// Jala Project [http://opensvn.csie.org/traccgi/jala]
//
// Copyright 2004 ORF Online und Teletext GmbH
//
// Licensed under the Apache License, Version 2.0 (the ``License'');
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an ``AS IS'' BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// $Revision$
// $LastChangedBy$
// $LastChangedDate$
// $HeadURL$
//


/**
 * @fileoverview Fields and methods of the jala.XmlRpcRequest class.
 */


// Define the global namespace for Jala modules
if (!global.jala) {
   global.jala = {};
}


/**
 * A constructor for XmlRpc request objects
 * @class Instances of this class provide the necessary functionality
 * for issueing XmlRpc requests to a remote service.
 * @param {String} url The url of the XmlRpc entry point
 * @param {String} methodName The name of the method to call
 * @returns A newly created jala.XmlRpcRequest instance
 * @constructor
 */
jala.XmlRpcRequest = function(url, methodName) {
   /** @ignore */
   var proxy = null;
   /** @ignore */
   var timeout = {
      "connect": 0,
      "socket": 0
   };
   /** @ignore */
   var debug = false;
   /** @ignore */
   var credentials = null;
   // default input and output encoding
   /** @ignore */
   var inputEncoding = "UTF-8";
   /** @ignore */
   var outputEncoding = "UTF-8";

   /**
    * Returns the URL of this request
    * @returns The URL of this request
    * @type java.net.URL
    */
   this.getUrl = function() {
      return new java.net.URL(url);
   };

   /**
    * Sets the proxy host and port. For Java runtimes < 1.5 this method
    * sets the appropriate system properties (so this has an effect on
    * all requests based on java.net.URL), for all others the proxy
    * is only set for this request.
    * @param {String} proxyString The proxy string in the form 'fqdn:port'
    * (eg. my.proxy.com:3128)
    */
   this.setProxy = function(proxyString) {
      if (proxyString && proxyString.trim()) {
         var idx = proxyString.indexOf(":");
         if (idx > 0) {
            var host = proxyString.substring(0, idx);
            var port = proxyString.substring(idx+1);
            if (host != null && port != null) {
               if (java.lang.Class.forName("java.net.Proxy") != null) {
                  // construct a proxy instance
                  var socket = new java.net.InetSocketAddress(host, port);
                  proxy = new java.net.Proxy(java.net.Proxy.Type.HTTP, socket);
               } else {
                  // the pre jdk1.5 way: set the system properties
                  var sys = java.lang.System.getProperties();
                  if (host) {
                     app.log("[Jala XmlRpc Client] WARNING: setting system http proxy to "
                             + host + ":" + port);
                     sys.put("http.proxySet", "true");
                     sys.put("http.proxyHost", host);
                     sys.put("http.proxyPort", port);
                  }
               }
            }
         }
      }
      return;
   };

   /**
    * Returns the proxy object. This method will only return
    * a value if using a java runtime > 1.5
    * @returns The proxy to use for this request
    * @type java.net.Proxy
    * @see #setProxy
    */
   this.getProxy = function() {
      return proxy;
   };
   
   /**
    * Sets the credentials for basic http authentication to
    * use with this request.
    * @param {String} username The username
    * @param {String} password The password
    */
   this.setCredentials = function(username, password) {
      var str = new java.lang.String(username + ":" + password);
      credentials = (new Packages.sun.misc.BASE64Encoder()).encode(str.getBytes());
      return;
   };
   
   /**
    * Returns the credentials of this request
    * @returns The base46 encoded credentials of this request
    * @type String
    */
   this.getCredentials = function() {
      return credentials;
   };

   /**
    * Sets the connection timeout to the specified milliseconds.
    * @param {Number} millis The timeout to use as connection timeout
    */
   this.setTimeout = function(millis) {
      timeout.connect = millis;
      return;
   };

   /**
    * Sets the socket timeout to the specified milliseconds.
    * @param {Number} millis The timeout to use as socket timeout
    */
   this.setReadTimeout = function(millis) {
      timeout.socket = millis;
      return;
   };

   /**
    * Returns the connection timeout of this request
    * @returns The connection timeout value in milliseconds
    * @type Number
    */
   this.getTimeout = function() {
      return timeout.connect;
   };

   /**
    * Returns the socket timeout of this request
    * @returns The socket timeout value in milliseconds
    * @type Number
    */
   this.getReadTimeout = function() {
      return timeout.socket;
   };

   /**
    * Returns the name of the remote function to call
    * @returns The name of the remote function
    * @type String
    */
   this.getMethodName = function() {
      return methodName;
   };

   /**
    * Sets both input and output encoding to the
    * specified encoding string
    * @param {String} enc The encoding to use for
    * both input and output. This must be a valid
    * java encoding string.
    */
   this.setEncoding = function(enc) {
      inputEncoding = enc;
      outputEncoding = enc;
      return;
   };

   /**
    * Sets the input encoding to the specified encoding string
    * @param {String} enc The encoding to use for input. This must be a valid
    * java encoding string.
    */
   this.setInputEncoding = function(enc) {
      inputEncoding = enc;
      return;
   };

   /**
    * Sets the output encoding to the specified encoding string
    * @param {String} enc The encoding to use for output. This must be a valid
    * java encoding string.
    */
   this.setOutputEncoding = function(enc) {
      outputEncoding = enc;
      return;
   };

   /**
    * Returns the input encoding
    * @returns The input encoding used by this request
    * @type String
    */
   this.getInputEncoding = function() {
      return inputEncoding;
   };

   /**
    * Returns the output encoding
    * @returns The output encoding used by this request
    * @type String
    */
   this.getOutputEncoding = function() {
      return outputEncoding;
   };

   /**
    * Enables or disables the debug mode. If enabled the xml source
    * of both request and response is included in the result properties
    * 'requestXml' and 'responseXml'
    * @param {Boolean} flag True or false.
    */
   this.setDebug = function(flag) {
      debug = flag;
      return;
   };

   /**
    * Returns true if debug is enabled for this request, false otherwise
    * @returns True if debugging is enabled, false otherwise
    * @type Boolean
    */
   this.debug = function() {
      return debug == true;
   };

   return this;
};

/** @ignore */
jala.XmlRpcRequest.prototype.toString = function() {
   return "[Jala XmlRpc Request]";
};

/**
 * Calling this method executes the remote method using
 * the arguments specified.
 * @returns The result of this XmlRpc request
 * @type Object
 */
jala.XmlRpcRequest.prototype.execute = function(/** [arg1][, arg2][, ...] */) {
   // if in debug mode, log the time the request took to event log
   if (app.__app__.debug() == true) {
      var start = new Date();
   }

   var tz = java.util.TimeZone.getDefault();
   var reqProcessor = new Packages.org.apache.xmlrpc.XmlRpcClientRequestProcessor(tz);
   var resProcessor = new Packages.org.apache.xmlrpc.XmlRpcClientResponseProcessor(tz);
   // create the result object
   var result = {
      error: null,
      result: null,
      requestXml: null,
      responseXml: null
   };

   // convert arguments into their appropriate java representations
   var params = new java.util.Vector();
   for (var i=0;i<arguments.length;i++) {
      params.add(jala.XmlRpcRequest.convertArgument(arguments[i]));
   }

   var r = new Packages.org.apache.xmlrpc.XmlRpcRequest(this.getMethodName(), params);
   var requestBytes = reqProcessor.encodeRequestBytes(r, this.getOutputEncoding());
   if (this.debug() == true) {
      result.requestXml = String(new java.lang.String(requestBytes));
   }
   // open the connection
   var proxy = this.getProxy();
   var url = this.getUrl();
   var conn = proxy ? url.openConnection(proxy) : url.openConnection();
   conn.setAllowUserInteraction(false);
   conn.setRequestMethod("POST");
   conn.setRequestProperty("User-Agent", "Jala XmlRpc Client");
   conn.setRequestProperty("Content-Type", "text/xml");
   conn.setRequestProperty("Content-Length", requestBytes.length);
   // set timeouts if defined and possible
   if (parseFloat(java.lang.System.getProperty("java.specification.version"), 10) >= 1.5) {
      conn.setConnectTimeout(this.getTimeout());
      conn.setReadTimeout(this.getReadTimeout());
   } else {
      app.logger.debug("WARNING: timeouts can only be using a Java runtime >= 1.5");
   }
   // set authentication credentials if defined
   if (this.getCredentials() != null) {
      conn.setRequestProperty("Authorization", "Basic " + this.getCredentials());
   }

   try {
      conn.setDoOutput(true);
      var outStream = conn.getOutputStream();
      outStream["write(byte[])"](requestBytes);
      outStream.flush();
      outStream.close();
      
      if (conn.getContentLength() > 0) {
         var inStream = conn.getInputStream();
         if (this.debug() == true) {
            inStream = new java.io.BufferedInputStream(conn.getInputStream());
            var outStream = new java.io.ByteArrayOutputStream();
            var buf = java.lang.reflect.Array.newInstance(java.lang.Byte.TYPE, 1024);
            var bytes;
            while ((bytes = inStream.read(buf)) > -1) {
                outStream.write(buf, 0, bytes);
            }
            result.responseXml = outStream.toString(this.getInputEncoding());
            inStream.close();
            // change the inStream and don't set the input encoding of
            // the response processor, since the conversion already happened above
            inStream = new java.io.ByteArrayInputStream(outStream.toByteArray());
         }
         resProcessor.setInputEncoding(this.getInputEncoding());
         var parsedResult = resProcessor.decodeResponse(inStream);
         if (parsedResult instanceof java.lang.Exception) {
            result.error = parsedResult;
         } else {
            result.result = jala.XmlRpcRequest.convertResult(parsedResult);
         }
      }
   } catch (e) {
      result.error = "[Jala XmlRpc Request] Error executing " + this.getMethodName()
                     + " with arguments " + jala.XmlRpcRequest.argumentsToString(arguments)
                     + ", the error is: " + e.toString();
   }
   if (app.__app__.debug() == true) {
      app.logger.debug("[Jala XmlRpc Request] (" + ((new Date()) - start) + " ms): executed '"
                       + this.getMethodName() + "' with arguments: "
                       + jala.XmlRpcRequest.argumentsToString(arguments));
   }
   return result;
};

/**
 * Helper method for converting a Javascript object into
 * its appropriate Java object.
 * @param {Object} obj The Javascript object to convert
 * @returns The appropriate Java representation of the object
 * @type java.lang.Object
 */
jala.XmlRpcRequest.convertArgument = function(obj) {
   var result;
   if (obj instanceof Array) {
      // convert into Vector
      result = new java.util.Vector(obj.length);
      for (var i=0;i<obj.length;i++) {
         result.add(i, jala.XmlRpcRequest.convertArgument(obj[i]));
      }
   } else if (obj instanceof Date) {
      // convert into java.util.Date
      result = new java.util.Date(obj.getTime());
   } else if (typeof(obj) == "boolean" || obj instanceof Boolean) {
      result = obj;
   } else if (typeof(obj) == "string" || obj instanceof String) {
      result = obj;
   } else if (isNaN(obj) == false) {
      // convert into Integer or Double
      if (obj - obj.toFixed() > 0) {
         result = new java.lang.Double(obj);
      } else {
         result = new java.lang.Integer(obj);
      }
   } else if (obj instanceof Object) {
      // convert into Hashtable
      result = new java.util.Hashtable();
      for (var key in obj) {
         if (obj[key] != null) {
            result.put(key, jala.XmlRpcRequest.convertArgument(obj[key]));
         }
      }
   } else {
      result = obj;
   }
   return result;
};

/**
 * Converts a Java object into its appropriate Javascript representation.
 * @param {java.lang.Object} obj The Java object to convert
 * @returns The appropriate Javascript representation of the Java object
 * @type Object
 */
jala.XmlRpcRequest.convertResult = function(obj) {
   var result;
   if (obj instanceof java.util.Vector) {
      // convert into Array
      result = [];
      var e = obj.elements();
      while (e.hasMoreElements()) {
         result.push(jala.XmlRpcRequest.convertResult(e.nextElement()));
      }
   } else if (obj instanceof java.util.Hashtable) {
      // convert into Object
      result = {};
      var e = obj.keys();
      var key;
      while (e.hasMoreElements()) {
         key = e.nextElement();
         result[key] = jala.XmlRpcRequest.convertResult(obj.get(key));
      }
   } else if (obj instanceof java.lang.String) {
      result = String(obj);
   } else if (obj instanceof java.lang.Number) {
      result = Number(obj);
   } else if (obj instanceof java.lang.Boolean) {
      result = Boolean(obj);
   } else if (obj instanceof java.lang.Date) {
      result = new Date(obj.getTime());
   } else {
      result = obj;
   }
   return result;
};

/**
 * Helper method to format an arguments array into
 * a string useable for debugging output.
 * @param {Object} args An arguments array
 * @returns The arguments array formatted as string
 * @type String
 */
jala.XmlRpcRequest.argumentsToString = function(args) {
   var arr = [];
   for (var i=0;i<args.length;i++) {
      if (typeof(args[i]) == "string") {
         arr.push('"' + args[i] + '"');
      } else {
         arr.push(args[i]);
      }
   }
   return arr.join(", ");
};
