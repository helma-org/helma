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
 * @fileoverview Fields and methods of the jala.XmlWriter class.
 */


// Define the global namespace for Jala modules
if (!global.jala) {
   global.jala = {};
}


/**
 * Construct a new XML writer.
 * @class This class defines a generic interface to write
 * arbitrary and validating XML source code. This is done
 * by first applying data objects onto template objects, 
 * both in a specified format. Then, the resulting object
 * tree is transformed into XML. Moreover, template objects
 * can be extended with other template objects to provide
 * full flexibility in inheriting subclasses.
 * @param {String} header An optional XML header.
 * @returns A new XML writer.
 * @constructor
 */
jala.XmlWriter = function(header) {
   var self = this;
   var XMLHEADER = header || 
                   '<?xml version="1.0" encoding="iso-8859-15"?>';
   var LOCALE = java.util.Locale.ENGLISH;

   /** @ignore FIXME: JSDoc bug */
   var write = function(str) {
      return res.write(str);
   };

   var writeln = function(str) {
      res.write(str);
      res.write("\n");
      return;
   };

   var getString = function(data, format) {
      if (data == null)
         return;
      switch (data.constructor) {
         case String:
            return encodeXml(data);
         case Number:
         case Date:
            if (format && data.format)
               return encodeXml(data.format(format, LOCALE));
            else if (data.toUTCString)
               return encodeXml(data.toUTCString());
            else
               return encodeXml(data.toString());
            break;
         case Object:
            return null;
      }
      return encodeXml(data.toString());
   };

   /** @ignore */
   var XmlElement = function(data) {
      if (!data)
         throw Error("Insufficient arguments to create XmlElement");

      var children = {};
      var properties = [
         "name", 
         "attributes", 
         "type", 
         "required", 
         "format", 
         "readonly"
      ];

      if (data.value) {
         if (data.value.constructor == Object) {
            this.value = [new XmlElement(data.value)];
         } else if (data.value.constructor == Array) {
            this.value = [];
            for (var i in data.value) {
               this.value[i] = new XmlElement(data.value[i]);
            }
         } else
            throw Error("Cannot handle unknown type of template value");
      }

      for (var i in properties) {
         var key = properties[i];
         this[key] = data[key] || null;
      }

      if (this.attributes) {
         this.attributes = self.clone(this.attributes);
         if (this.attributes.constructor == Object)
            this.attributes = [this.attributes];
      } else {
         this.attributes = [];
      }

      return this;
   };

   /** @ignore */
   XmlElement.toString = function() {
      return "[XmlElement constructor]";
   };
   
   /** @ignore */
   XmlElement.prototype.setValue = function(element) {
      if (element.constructor != this.constructor)
         throw Error("Invalid type for XmlElement addition");
      if (!this.value)
         this.value = [];
      else {
         var pos = this.contains(element);
         if (pos > -1)
            this.value.splice(pos, 1);
      }
      this.addValue(element);
      return this;
   };

   /** @ignore */
   XmlElement.prototype.addValue = function(element) {
      if (element.constructor != this.constructor)
         throw Error("Invalid type for XmlElement addition");
      if (!this.value)
         this.value = [];
      this.value.push(element);
      return this;
   };

   /** @ignore */
   XmlElement.prototype.contains = function(element) {
      if (!this.value || !element)
         return -1;
      for (var i in this.value) {
         if (this.value[i].name == element.name)
            return i;
      }
      return -1;
   };

   /** @ignore */
   XmlElement.prototype.populate = function(data) {
      if (this.attributes) {
         var value;
         for (var i in this.attributes) {
            var attr = this.attributes[i];
            if (!attr.name)
               throw Error("Cannot populate unnamed attribute entry");
            if (data && data.attributes)
               value = data.attributes[attr.name];
            if (data && (data.value || data.attributes) && !value && attr.required) {
               throw Error('Missing required ' + (attr.type || Object).name + ' attribute "' + 
                           attr.name + '" in element &lt;' + this.name + '&gt; (' + value + ")");
            }
            if (value && attr.type && attr.type != value.constructor) {
               throw Error('Type mismatch in attribute "' + 
                           this.name + ":" + attr.name + '"');
            }
            if (value) {
               app.debug("populating attribute " + attr.name + 
                         " with " + value.constructor.name + ": " + value.toSource());
            }
            if (!attr.readonly) {
               attr.value = getString(value, attr.format) || attr.value ;
            }
         }
      }

      if (data && data.value) // && data.value.constructor == Object)
         data = data.value;

      if (this.value && data) {
         for (var i in this.value) {
            var element = this.value[i];
            element.populate(data[element.name]);
         }
      } else {
         if (!data && this.required)
            throw Error('Missing required element "' + this.name + '"');
         if (data && this.type && this.type != data.constructor) {
            throw Error('Type mismatch in element "' + this.name + '"');
         }
         if (data) {
            app.debug("populating element &lt;" + this.name +  "&gt; with " + 
                      (this.type || Object).name + ": " + data.toSource());
         }
         if (!this.readonly)
            this.value = getString(data, this.format) || this.value;
      }

     return;
   };

   /** @ignore */
   XmlElement.prototype.write = function(path) {
      if (!path)
         path = "";

      if (!this.value && !this.attributes)
         return;

      var attrBuffer = new java.lang.StringBuffer();
      if (this.attributes) {
         for (var a in this.attributes) {
            var attr = this.attributes[a];
            if (attr.value) {
               attrBuffer.append(" " + attr.name + '="');
               attrBuffer.append(attr.value);
               attrBuffer.append('"');
            }
         }
      }

      var attrSize = attrBuffer.length();
      if (!this.value && attrSize < 1)
         return;

      write("<" + this.name);
      if (attrSize > 0) {
         display = true;
         write(attrBuffer.toString());
      }
      if (this.value) {
         write(">");
         if (this.value && this.value.constructor == Array) {
            for (var i in this.value)
               this.value[i].write(path+"/"+this.name);
         } else
            write(this.value);
         write("</" + this.name + ">\n");
      } else
         write("/>\n");
      return;
   };

   /** @ignore */
   XmlElement.prototype.toString = function() {
      return "[XmlElement: " + this.toSource() + "]";
   };

   /**
    * Get a newly created XML element.
    * @param {Object} data The XML data as object tree.
    * @returns The resulting XML element.
    * @type jala.XmlWriter.XmlElement
    */
   this.createElement = function(data) {
      return new XmlElement(data);
   };

   /**
    * Get the root XML element of this writer.
    * @returns The root XML element.
    * @type jala.XmlWriter.XmlElement
    */
   this.getRoot = function() {
      return new XmlElement({});
   };

   /**
    * Extend a template object. 
    * @param {Object} template The template object.
    * @param {Object} ext The extension object.
    * @returns The XML writer.
    * @type jala.XmlWriter
    */
   this.extend = function(template, ext) {
      if (ext.constructor == Object)
         ext = [ext];
      if (ext.constructor == Array) {
         for (var i in ext)
            template.value.push(ext[i]);
      }
      return this;
   };
   
   /**
    * Add a namespace to this writer. 
    * @param {String} name The name of the namespace.
    * @param {String} url The URL string of the namespace.
    * @returns The XML root element.
    * @type jala.XmlWriter.XmlElement
    */
   this.addNamespace = function(name, url) {
      var ref = this.getRoot();
      ref.attributes.push({
         name: "xmlns:" + name,
         value: url
      });
      return ref;
   };

   /**
    * Write the XML to the response buffer.
    */
   this.write = function() {
      res.contentType = "text/xml";
      writeln(XMLHEADER);
      this.getRoot().write();
      return;
   };

   /**
    * Get the XML output as string.
    * @returns The XML output.
    * @type String
    */
   this.toString = function() {
      res.push();
      this.write();
      return res.pop();
   };

   /**
    * Clone this XML writer.
    * @param {Object} The clone templare.
    * @returns The cloned XML writer.
    * @type jala.XmlWriter
    */
   this.clone = function(obj) {
      if (!obj || typeof obj != "object")
         return obj;
      var copy = new obj.constructor;
      for (var i in obj) {
         if (obj[i].constructor == Object ||
             obj[i].constructor == Array)
            copy[i]= this.clone(obj[i]);
         else
            copy[i] = obj[i];
      }
      return copy;
   };

   return this;
};

