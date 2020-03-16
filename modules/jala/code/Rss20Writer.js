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
 * @fileoverview Fields and methods of the jala.Rss20Writer class.
 */


// Define the global namespace for Jala modules
if (!global.jala) {
   global.jala = {};
}


/**
 * Jala dependencies
 */
app.addRepository(getProperty("jala.dir", "modules/jala") + 
                  "/code/XmlWriter.js");

/**
 * @class Class to create, modify and render standard-compliant
 * RSS 2.0 feeds.
 * @constructor
 * @extends jala.XmlWriter
 * @param {String} header Optional XML header.
 */
jala.Rss20Writer = function(header) {
   // defines the prototype of this constructor
   jala.XmlWriter.apply(this, arguments);

   // this should do the same but alas, helma throws
   // an error the very first time it is executed:
   //arguments.callee.prototype = new jala.XmlWriterInterface();

   var DATEFMT = "EEE, dd MMM yyyy HH:mm:ss Z";

   var CATEGORY = {
      name: "category",
      amount: Infinity,
      attributes: {
         name: "domain",
      }
   };

   var ITEM = {
      name: "item",
      amount: Infinity,
      value: [{
         name: "title",
         required: true
      }, {
         name: "link",
      }, {
         name: "description",
      }, {
         name: "author",
      }, {
         name: "comments",
      }, {
         name: "enclosure",
         attributes: [{
            name: "url",
            required: true
         }, {
            name: "length",
            required: true
         }, {
            name: "type",
            required: true
         }]
      }, {
         name: "guid",
         attributes: [{
            name: "isPermaLink",
            type: Boolean
         }]
      }, {
         name: "pubDate",
         type: Date,
         format: DATEFMT
      }, {
         name: "source",
         attributes: [{
            name: "url",
            required: true
         }]
      }]
   };

   var CHANNEL = {
      name: "channel",
      value: [{
         name: "title",
         required: true
      }, {
         name: "link",
         required: true
      }, {
         name: "description",
         required: true
      }, {
         name: "language",
      }, {
         name: "copyright",
      }, {
         name: "managingEditor",
      }, {
         name: "webMaster",
      }, {
         name: "pubDate",
         type: Date,
         format: DATEFMT
      }, {
         name: "lastBuildDate",
         type: Date,
         format: DATEFMT
      }, {
         name: "generator",
      }, {
         name: "docs",
      }, {
         name: "cloud",
         attributes: [{
            name: "domain",
         }, {
            name: "port",
            type: Number,
            format: "#"
         }, {
            name: "path",
         }, {
            name: "registerProcedure",
         }, {
            name: "protocol",
         }]
      }, {
         name: "ttl",
         type: Number,
         format: "#"
      }, {
         name: "rating",
      }, {
         name: "skipHours",
      }, {
         name: "skipDays",
      }]
   };

   var IMAGE = {
      name: "image",
      value: [{
         name: "url",
         required: true
      }, {
         name: "title",
         required: true
      }, {
         name: "link",
         required: true
      }, {
         name: "width",
         type: Number,
         format: "#"
      }, {
         name: "height",
         type: Number,
         format: "#"
      }, {
         name: "description",
      }]
   };

   var TEXTINPUT = {
      name: "textinput",
      value: [{
         name: "title",
         required: true
      }, {
         name: "description",
         required: true
      }, {
         name: "name",
         required: true
      }, {
         name: "link",
         required: true
      }]
   };

   var ROOT = {
      name: "rss",
      attributes: [{
         name: "version",
         value: "2.0"
      }]
   };

   var xmlroot = this.createElement(ROOT);
   var channel = this.createElement(CHANNEL);
   xmlroot.setValue(channel);

   /**
    * Get the writer's root element.
    * @returns The writer's root element.
    * @type jala.XmlWriter.XmlElement
    */
   this.getRoot = function() {
      return xmlroot;
   };

   /**
    * Add child elements to the channel template.
    * @param {Array} ext List of additional child elements.
    */
   this.extendChannel = function(ext) {
      this.extend(CHANNEL, ext);
      channel = this.createElement(CHANNEL);
      xmlroot.setValue(channel);
      return;
   };

   /**
    * Get the writer's channel element.
    * @returns The writer's channel element.
    * @type jala.XmlWriter.XmlElement
    */
   this.getChannel = function() {
      return channel;
   };

   /**
    * Populate the channel element with data.
    * @param {Object} data An XmlWriter-compliant object structure.
    * @returns The populated channel element.
    * @type jala.XmlWriter.XmlElement
    */
   this.setChannel = function(data) {
      return channel.populate(data);
   };

   /**
    * Add child elements to the item template.
    * @param {Array} ext List of additional child elements.
    */
   this.extendItem = function(ext) {
      this.extend(ITEM, ext);
      return;
   };

   /**
    * Get a new and innocent item element.
    * @param {Object} data An XmlWriter-compliant object structure.
    * @returns A new and innocent item element.
    * @type jala.XmlWriter.XmlElement
    */
   this.createItem = function(data) {
      var item = this.createElement(ITEM);
      item.populate(data);
      return item;
   };

   /**
    * Add an item element to the channel element.
    * @param {jala.XmlWriter.XmlElement} item The item element to add.
    */
   this.addItem = function(item) {
      channel.addValue(item);
      return;
   };

   /**
    * Add a category element to an arbitrary element.
    * @param {String} name The name of the category.
    * @param {String} domain The domain of the category.
    * @param {jala.XmlWriter.XmlElement} parent The optional parent element.
    */
   this.addCategory = function(name, domain, parent) {
      if (!parent)
         parent = channel;
      var cat = this.createElement(CATEGORY);
      cat.populate({
         value: name,
         attributes: {domain: domain}
      });
      parent.addValue(cat);
      return;
   };

   /**
    * Populate the image element with data.
    * @param {Object} data An XmlWriter-compliant object structure.
    */
   this.setImage = function(data) {
      var image = this.createElement(IMAGE);
      image.populate(data);
      channel.setValue(image);
      return;
   };

   /**
    * Populate the textInput element with data.
    * @param {Object} data An XmlWriter-compliant object structure.
    */
   this.setTextInput = function(data) {
      var textInput = this.createElement(TEXTINPUT);
      textInput.populate(data);
      channel.setValue(textInput);
      return;
   };

   return this;
};
