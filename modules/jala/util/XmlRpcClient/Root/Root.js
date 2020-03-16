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
 * Main action
 */
Root.prototype.main_action = function() {
   res.handlers.xmlrpc = {};
   res.handlers.feedback = new Feedback();
   if (req.isPost()) {
      if (!req.data.url) {
         res.handlers.feedback.setError("url", "Please enter the URL of the XmlRpc entry point");
      }
      if (!req.data.method) {
         res.handlers.feedback.setError("method", "Please specify the method to call");
      }
      try {
         var args = parseArguments(req.data.args);
      } catch (e) {
         res.handlers.feedback.setError("arguments", "The method arguments are invalid");
      }
      if (!res.handlers.feedback.isError) {
         var xmlRpcCall = new XmlRpcCall(req.data.url, req.data.method);
         xmlRpcCall.request.setEncoding(req.data.encoding);
         xmlRpcCall.request.setProxy(req.data.proxy);
         xmlRpcCall.request.setDebug(req.data.debug == 1);
         if (app.properties.username != null && app.properties.password != null) {
            xmlRpcCall.request.setCredentials(app.properties.username, app.properties.password);
         }
         XmlRpcCall.prototype.execute.apply(xmlRpcCall, args);
         res.handlers.xmlrpc = xmlRpcCall;
      }
   }
   this.renderSkin("main");
   return;
};

/**
 * Main XmlRpc action. The only supported method name is "echo".
 * If no additional arguments are given this action
 * returns "echo" to the client. A single additional argument is returned
 * as-is, multiple additional arguments are returned as array.
 */
Root.prototype.main_action_xmlrpc = function(methodName) {
   switch (methodName) {
      case "echo":
         if (arguments.length == 1) {
            return "echo";
         } else if (arguments.length == 2) {
            return arguments[1];
         } else {
            var result = [];
            for (var i=1;i<arguments.length;i++) {
               result.push(arguments[i]);
            }
            return result;
         }
      default:
         throw "Unknown XmlRpc method '" + methodName + "'";
         break;
   }
   return;
};
