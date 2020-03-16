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


var result = undefined;

/**
 * A simple test of jala.AsyncRequest. It constructs a new AsyncRequest
 * with a test function defined below that sets various properties
 * of the global result object above. After evaluating the async request
 * the current thread sleeps for a short period of time to wait for
 * the other request to finish, and then does the testing of the result.
 */
var testAsyncRequest = function() {
   var r = new jala.AsyncRequest(global, "testFunction");
   r.run("jala");
   // wait until the async request started above has finished
   // before testing the result, but no longer than 1 second.
   var elapsed = 0;
   var interval = 5;
   while (result === undefined && elapsed < 1000) {
      elapsed += interval;
      java.lang.Thread.sleep(interval);
   }
   assertNotUndefined(result);
   assertEqual(result.name, "jala");
   assertEqual(result.request, req);
   assertEqual(result.response, res);
   assertFalse(r.isAlive());
   return;
};

/**
 * A simple test function that assigns an object to the global
 * property "result".
 * @param {String} name A string to use as name
 */
var testFunction = function(name) {
   result = {
      name: name,
      request: req,
      response: res
   };
   return;
};
