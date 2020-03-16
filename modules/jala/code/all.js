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
 * @fileoverview Wrapper for automatic inclusion of all Jala modules.
 */


// Define the global namespace for Jala modules
if (!global.jala) {
   global.jala = {};
}


(function() {
   var packages = [
      "AsyncRequest",
      "BitTorrent",
      "Date",
      "DnsClient",
      "Captcha", 
      "Form",
      "History",
      "HtmlDocument",
      "HopObject",
      "I18n",
      "ImageFilter",
      "IndexManager",
      "ListRenderer",
      "Mp3",
      "PodcastWriter",
      "RemoteContent",
      "Rss20Writer",
      "Utilities",
      "XmlRpcRequest",
      "XmlWriter"
   ];
   var jalaDir = getProperty("jala.dir", "modules/jala");
   for (var i in packages) {
      app.addRepository(jalaDir + "/code/" + packages[i] + ".js");
   }
   return;
})();


/**
 * Get a string representation of the Jala library.
 * @returns [Jala JavaScript Application Library]
 * @type String
 */
jala.toString = function() {
   return "[Jala JavaScript Application Library]";
};
