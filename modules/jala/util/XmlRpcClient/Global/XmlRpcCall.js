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
 * Wrapper for making XmlRpc calls to remote servers.
 * @class Instances of this class can make calls to remote
 * XmlRpc servers, plus function as macro handlers for displaying
 * results, errors etc.
 * @param {String} url The url of the entry-point
 * @param {String} methodName The name of the method to call (eg. "xmlrpcclient.echo")
 * @param {Array} args An array containing arguments to pass to the remote function
 * @returns A newly created XmlRpcCall instance
 * @type XmlRpcCall
 */
var XmlRpcCall = function(url, methodName) {
   this.request =  new jala.XmlRpcRequest(url, methodName);
   this.result = null;
   return this;
};

/**
 * Executes the XmlRpc call
 */
XmlRpcCall.prototype.execute = function() {
   this.args = arguments;
   this.response = jala.XmlRpcRequest.prototype.execute.apply(this.request, arguments);
   return;
};

/** @ignore */ 
XmlRpcCall.prototype.toString = function() {
   return "[XmlRpcCall]";
};

/**
 * Returns the Url of this XmlRpcCall instance.
 * @returns The url of this call
 * @type String
 */
XmlRpcCall.prototype.url_macro = function() {
   return this.url;
};

/**
 * Returns the method name of this XmlRpcCall instance.
 * @returns The method name of this call
 * @type String
 */
XmlRpcCall.prototype.method_macro = function() {
   return this.methodName;
};

/**
 * Displays the arguments of this XmlRpcCall instance.
 */
XmlRpcCall.prototype.arguments_macro = function() {
   var arg;
   for (var i=0;i<this.args.length;i++) {
      arg = this.args[i];
      res.write('<div class="argument ' + (i%2 == 0 ? "even" : "odd") + '">');
      res.write('<div class="type">' + i + " ");
      if (isArray(arg)) {
         res.write("(Array)");
      } else if (isDate(arg)) {
         res.write("(Date)");
      } else if (isString(arg)) {
         res.write("(String)");
      } else if (isNumber(arg)) {
         res.write("(Integer)");
      } else if (isBoolean(arg)) {
         res.write("(Boolean)");
      } else if (isNull(arg)) {
         res.write("(null)");
      } else if (isUndefined(arg)) {
         res.write("(undefined)");
      } else if (isObject(arg)) {
         res.write("(Object)");
      } else {
         res.write("(unknown type)");
      }
      res.write('</div>\n<div class="value"><pre>');
      res.write(prettyPrint(arg));
      res.write("</pre></div></div>");
   }
   return;
};

/**
 * Returns the result of this XmlRpcCall instance.
 * @returns The result as human readable string
 * @type String
 */
XmlRpcCall.prototype.result_macro = function() {
   if (this.response.result != null) {
      return prettyPrint(this.response.result);
   }
   return;
};

/**
 * Returns the error of this XmlRpcCall instance, if any.
 * @returns The error string
 * @type String
 */
XmlRpcCall.prototype.error_macro = function() {
   if (this.response.error != null) {
      return this.response.error;
   }
   return;
};

/**
 * Displays the xml source of either request or response
 * @param {Object} param A parameter object containing the
 * macro attributes
 */
XmlRpcCall.prototype.xml_macro = function(param) {
   var xml = this.response[param.of + "Xml"];
   if (xml != null) {
      res.write("<pre>");
      res.write(prettyPrintXml(xml));
      res.write("</pre>");
   }
   return;
};
