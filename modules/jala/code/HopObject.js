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
 * @fileoverview Additional fields and methods of the HopObject class.
 */

/**
 * HelmaLib dependencies
 */
app.addRepository("modules/core/String.js");
app.addRepository("modules/helma/File.js");

/**
 * Constructs a name from an object which
 * is unique in the underlying HopObject collection. 
 * @param {Object} obj The object representing or containing
 * the alias name. Possible object types include:
 * <ul>
 * <li>File</li>
 * <li>helma.File</li>
 * <li>java.io.File</li>
 * <li>String</li>
 * <li>java.lang.String</li>
 * <li>Packages.helma.util.MimePart</li>
 * </ul>
 * @param {Number} maxLength The maximum length of the alias
 * @returns The resulting alias
 * @type String
 */
HopObject.prototype.getAccessName = function(obj, maxLength) {
   /**
    * Private method checking if the key passed as argument is already
    * existing in this object
    * @param {String} key The key string to test
    * @returns True in case the key is already in use, false otherwise
    * @type Boolean
    */
   var isReserved = function(obj, key) {
      return key === "." ||
             key === ".." ||
             obj[key] != null ||
             obj[key + "_action"] != null ||
             obj.get(key) != null
   };

   // prepare name depending on argument
   var name;
   var clazz = obj.constructor || obj.getClass();
   switch (clazz) {
      case File:
      case helma.File:
      case java.io.File:
      case Packages.helma.util.MimePart:
         // first fix bloody ie/win file paths containing backslashes
         name = obj.getName().split(/[\\\/]/).pop();
         if (name.contains("."))
            name = name.substring(0, name.lastIndexOf("."));
         break;
      case String:
      case java.lang.String:
         name = obj;
         break;
      default:
         name = obj.toString();
   }

   // remove all (back)slashes
   var accessName = name.replace(/[\\\/]/g, "");
   // remove all plus signs
   accessName = accessName.replace("+","");
   if (accessName.length > maxLength) {
      accessName = accessName.substring(0, maxLength);
   }
   var result = accessName;
   if (isReserved(this, result)) {
      var len = result.length;
      var counter = 1;
      var overflow;
      while (isReserved(this, result)) {
         result = accessName + "-" + counter.toString();
         if ((overflow = result.length - maxLength) > 0) {
            result = accessName.substring(0, accessName.length - overflow) +
                     "-" + counter.toString();
            if (result.length > maxLength) {
               throw "Unable to create accessname due to limit restriction";
            }
         }
         counter += 1;
      }
   }
   return result;
};


/**
 * Returns true if the internal state of this HopObject is TRANSIENT.
 * @returns True if this HopObject is marked as <em>transient</em>, false otherwise.
 * @type Boolean
 */
HopObject.prototype.isTransient = function() {
   return this.__node__.getState() === Packages.helma.objectmodel.INodeState.TRANSIENT;
};

/**
 * Returns true if the internal state of this HopObject is VIRTUAL.
 * @returns True if this HopObject is marked as <em>virtual</em>, false otherwise.
 * @type Boolean
 */
HopObject.prototype.isVirtual = function() {
   return this.__node__.getState() === Packages.helma.objectmodel.INodeState.VIRTUAL;
};

/**
 * Returns true if the internal state of this HopObject is INVALID.
 * @returns True if this HopObject is marked as <em>invalid</em>, false otherwise.
 * @type Boolean
 */
HopObject.prototype.isInvalid = function() {
   return this.__node__.getState() === Packages.helma.objectmodel.INodeState.INVALID;
};

/**
 * Returns true if the internal state of this HopObject is CLEAN.
 * @returns True if this HopObject is marked as <em>clean</em>, false otherwise.
 * @type Boolean
 */
HopObject.prototype.isClean = function() {
   return this.__node__.getState() === Packages.helma.objectmodel.INodeState.CLEAN;
};

/**
 * Returns true if the internal state of this HopObject is NEW.
 * @returns True if this HopObject is marked as <em>new</em>, false otherwise.
 * @type Boolean
 */
HopObject.prototype.isNew = function() {
   return this.__node__.getState() === Packages.helma.objectmodel.INodeState.NEW;
};

/**
 * Returns true if the internal state of this HopObject is MODIFIED.
 * @returns True if this HopObject is marked as <em>modified</em>, false otherwise.
 * @type Boolean
 */
HopObject.prototype.isModified = function() {
   return this.__node__.getState() === Packages.helma.objectmodel.INodeState.MODIFIED;
};

/**
 * Returns true if the internal state of this HopObject is DELETED.
 * @returns True if this HopObject is marked as <em>deleted</em>, false otherwise.
 * @type Boolean
 */
HopObject.prototype.isDeleted = function() {
   return this.__node__.getState() === Packages.helma.objectmodel.INodeState.DELETED;
};
