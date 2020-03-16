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
 * @fileoverview Fields and methods of the Global prototype.
 */


/**
 * Returns true if the value passed as argument is either a string literal,
 * an instance of String or of java.lang.String.
 * @param {Object} val The value to test
 * @returns True if the value is a string, false otherwise
 * @type Boolean
 */
function isString(val) {
   return typeof(val) == "string" ||
             val instanceof java.lang.String ||
             val instanceof String;
};

/**
 * Returns true if the value passed as argument is either a boolean
 * literal or an instance of Boolean.
 * @param {Object} val The value to test
 * @returns True if the value is a boolean, false otherwise
 * @type Boolean
 */
function isBoolean(val) {
   return typeof(val) == "boolean" ||
             val instanceof Boolean;
};

/**
 * Returns true if the value passed as argument is either a number,
 * an instance of Number or of java.lang.Number.
 * @param {Object} val The value to test
 * @returns True if the value is a number, false otherwise
 * @type Boolean
 */
function isNumber(val) {
   return typeof(val) == "number" ||
             val instanceof java.lang.Number ||
             val instanceof Number;
};

/**
 * Returns true if the value passed as argument is null.
 * @param {Object} val The value to test
 * @returns True if the value is null, false otherwise
 * @type Boolean
 */
function isNull(val) {
   return val === null;
};

/**
 * Returns true if the value passed as argument is undefined.
 * @param {Object} val The value to test
 * @returns True if the value is undefined, false otherwise
 * @type Boolean
 */
function isUndefined(val) {
   return val === undefined;
};

/**
 * Returns true if the value passed as argument is an array.
 * @param {Object} val The value to test
 * @returns True if the value is an array, false otherwise
 * @type Boolean
 */
function isArray(val) {
   return val instanceof Array;
};

/**
 * Returns true if the value passed as argument is either a Javascript date
 * or an instance of java.util.Date.
 * @param {Object} val The value to test
 * @returns True if the value is a date, false otherwise
 * @type Boolean
 */
function isDate(val) {
   return val instanceof Date ||
             val instanceof java.util.Date;
};

/**
 * Returns true if the value passed as argument is either a Javascript
 * object or an instance of java.lang.Object.
 * @param {Object} val The value to test
 * @returns True if the value is an object, false otherwise
 * @type Boolean
 */
function isObject(val) {
   return val instanceof Object ||
             val instanceof java.lang.Object;
};

/**
 * Returns true if the value passed as argument is a function.
 * @param {Object} val The value to test
 * @returns True if the argument is a function, false otherwise
 * @type Boolean
 */
function isFunction(val) {
   return val instanceof Function;
};