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
 * @fileoverview Fields and methods of the jala.HtmlDocument class.
 */

// Define the global namespace for Jala modules
if (!global.jala) {
   global.jala = {};
}

/**
 * Jala dependencies
 */
(function() {
   var jalaDir = getProperty("jala.dir", "modules/jala");
   app.addRepository(jalaDir + "/lib/dom4j-1.6.1.jar");
   app.addRepository(jalaDir + "/lib/jaxen-1.1-beta-8.jar");
})();

/**
 * Construct a new HTML document.
 * @class This class provides easy access to the elements of
 * an arbitrary HTML document. By using TagSoup, Dom4J and Jaxen
 * even invalid HTML can be parsed, turned into an object tree
 * and easily be processed with XPath expressions.
 * @param {String} source The HTML source code.
 * @returns A new HTML document.
 * @constructor
 */
jala.HtmlDocument = function(source) {
   var REQUIREMENTS = {
      "dom4j-1.6.1": "http://www.dom4j.org",
      "jaxen-1.1-beta-8": "http://www.jaxen.org"
   };

   var reader = new java.io.StringReader(source);
   var dom4j = Packages.org.dom4j;
   var tagsoup = "org.ccil.cowan.tagsoup.Parser";

   try {
      var saxReader = new dom4j.io.SAXReader(tagsoup);
      var document = saxReader.read(reader);
      document.normalize();
   } catch(e) {
      res.push();
      res.write("\njala.HtmlDocument requires the following Java ");
      res.write("packages in ext/lib or application directory:\n");
      for (var i in REQUIREMENTS) {
         res.write(i);
         res.write(".jar");
         res.write(" [");
         res.write(REQUIREMENTS[i]);
         res.write("]\n");
      }
      throw (e + res.pop());
   }

   /**
    * Get all document nodes from an XPath expression.
    * @param {String} xpathExpr An XPath expression.
    * @returns A list of HTML elements.
    * @type org.dom4j.tree.DefaultElement
    */
   this.scrape = function(xpathExpr) {
      return document.selectNodes(xpathExpr);
   };
   
   /**
    * Get all link elements of the HTML document.
    * @returns A list of link elements.
    * @type Array
    */
   this.getLinks = function() {
      var result = [];
      var list = this.scrape("//html:a");
      for (var i=0; i<list.size(); i+=1) {
         var element = list.get(i);
         var text = element.getText();
         var href = element.attribute("href");
         if (text && href) {
            result.push({
               text: text,
               url: href.getText()
            });
         }
      }
      return result;
   };
   
   /**
    * Retrieves all elements by name from the document.
    * The returned object structure is compatible for usage
    * in {@link jala.XmlWriter}.
    * @param {String} elementName The name of the desired element
    * @returns The list of available elements in the document
    * @type Array
    */
   this.getAll = function(elementName) {
      var result = [], object;
      var list = this.scrape("//html:" + elementName);
      var i, n, element, text, attributes, attr, size;
      for (i=0; i<list.size(); i+=1) {
         element = list.get(i);
         object = {
            name: element.getName(),
            value: element.getText() || null
         };
         attributes = element.attributes();
         if ((size = attributes.size()) > 0) {
            object.attributes = new Array;
            for (n=0; n<size; n+=1) {
               attr = attributes.get(n);
               object.attributes.push({
                  name: attr.getName(),
                  value: attr.getData() || null
               });
            }
         }
         result.push(object);
      }
      return result;
   };

   /**
    * Get a string representation of the HTML document.
    * @returns A string representation of the HTML document.
    * @type String
    */
   this.toString = function() {
      return "[jala.HtmlDocument " + source.length + " Bytes]";
   };

   return this;
};
