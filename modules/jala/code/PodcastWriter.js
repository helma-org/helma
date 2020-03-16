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
 * @fileoverview Fields and methods of the jala.PodcastWriter class.
 */


// Define the global namespace for Jala modules
if (!global.jala) {
   global.jala = {};
}


/**
 * Jala dependencies
 */
app.addRepository(getProperty("jala.dir", "modules/jala") + 
                  "/code/Rss20Writer.js");

/**
 * @class Class to create, modify and render standard-compliant
 * RSS 2.0 feeds including support for Apple's Podcast specification.
 * @constructor
 * @extends jala.Rss20Writer
 * @param {String} header Optional XML header.
 */
jala.PodcastWriter = function(header) {
   jala.Rss20Writer.apply(this, arguments);

   var CATEGORY = {
      name: "itunes:category",
      attributes: {
         name: "text"
      }
   };
   
   var OWNER = {
      name: "itunes:owner",
      value: [{
         name: "itunes:name" 
      }, {
         name: "itunes:email"
      }]
   };

   this.addNamespace("itunes", "http://www.itunes.com/dtds/podcast-1.0.dtd");

   this.extendChannel([{
      name: "itunes:author"
   }, {
      name: "itunes:subtitle"
   }, {
      name: "itunes:summary"
   }, {
      name: "itunes:new-feed-url"
   }, {
      name: "itunes:image",
      attributes: [{
         name: "href"
      }]
   }, {
      name: "itunes:link",
      attributes: [{
         name: "rel"
      }, {
         name: "type"
      }, {
         name: "href"
      }]
   }]);

   this.getChannel().setValue(this.createElement(OWNER));

   this.extendItem([{
      name: "itunes:duration"
   }, {
      name: "itunes:subtitle"
   }]);

   /**
    * Add an iTunes Podcast category.
    * @param {String} name The category's name.
    * @param {String} subName The (optional) sub-category's name.
    * @param {jala.XmlWriter.XmlElement} parent Optional parent 
    * element to add the category to.
    */
   this.addItunesCategory = function(name, subName, parent) {
      if (!parent)
         parent = this.getChannel();
      var cat = this.createElement(CATEGORY);
      cat.populate({attributes: {text: name}});
      if (subName) {
         var subCat = this.createElement(CATEGORY);
         subCat.populate({attributes: {text: subName}});
         cat.addValue(subCat);
      }
      parent.addValue(cat);
      return;
   };

   return this;
};


/** A typical XML header as default.
    @type String @final */
jala.PodcastWriter.XMLHEADER = '<?xml version="1.0" encoding="UTF-8"?>';
