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
 * Dependencies
 */
app.addRepository("modules/core/String.js");
app.addRepository(getProperty("jala.dir", "modules/jala") + 
                  "/code/XmlRpcRequest.js");

/**
 * A safe eval method that uses a standard Javascript scope
 * without any Helma specifics for evaluation. This method
 * does a double evaluation: first it evaluates the code
 * in a separate javascript scope without any Helma specifics, and only
 * if that doesn't throw an exception it evaluates the expression in the
 * application scope, so that objects constructed during evaluation
 * belong to the correct scope (and eg. testing with instanceof returns
 * the expected result). Keep in mind that due to the double
 * evaluation using this method is quite costly.
 * @param {String} code The code to evaluate
 * @returns The result of the evaluated code
 */
var safeEval = function(code) {
   var context = new Packages.org.mozilla.javascript.Context();
   try {
      context.enter();
      // first evaluation in separate scope
      context.evaluateString(safeEval.SCOPE, code, null, 0, null);
      return eval(code);
   } finally {
      context.exit();
   }
};
safeEval.SCOPE = Packages.org.mozilla.javascript.Context.getCurrentContext().initStandardObjects();

/**
 * Returns true if the value passed as argument is a string. Since
 * this value might be constructed using the safeEval's scope
 * this method tests both the application's scope and the safe one.
 * @param {Object} val The value to test
 * @returns True if the value is a string, false otherwise
 */
var isString = function(val) {
   return typeof(val) == "string" ||
             val instanceof java.lang.String ||
             val instanceof String;
};

/**
 * Returns true if the value passed as argument is a boolean. Since
 * this value might be constructed using the safeEval's scope
 * this method tests both the application's scope and the safe one.
 * @param {Object} val The value to test
 * @returns True if the value is a boolean, false otherwise
 */
var isBoolean = function(val) {
   return typeof(val) == "boolean" ||
             val instanceof Boolean;
};

/**
 * Returns true if the value passed as argument is a number. Since
 * this value might be constructed using the safeEval's scope
 * this method tests both the application's scope and the safe one.
 * @param {Object} val The value to test
 * @returns True if the value is a number, false otherwise
 */
var isNumber = function(val) {
   return typeof(val) == "number" ||
             val instanceof java.lang.Integer ||
             val instanceof Number;
};

/**
 * Returns true if the value passed as argument is null.
 * @param {Object} val The value to test
 * @returns True if the value is null, false otherwise
 */
var isNull = function(val) {
   return val === null;
};

/**
 * Returns true if the value passed as argument is undefined.
 * @param {Object} val The value to test
 * @returns True if the value is undefined, false otherwise
 */
var isUndefined = function(val) {
   return val === undefined;
};

/**
 * Returns true if the value passed as argument is an array. Since
 * this value might be constructed using the safeEval's scope
 * this method tests both the application's scope and the safe one.
 * @param {Object} val The value to test
 * @returns True if the value is an array, false otherwise
 */
var isArray = function(val) {
   return val instanceof Array;
};

/**
 * Returns true if the value passed as argument is a date. Since
 * this value might be constructed using the safeEval's scope
 * this method tests both the application's scope and the safe one.
 * @param {Object} val The value to test
 * @returns True if the value is a date, false otherwise
 */
var isDate = function(val) {
   return val instanceof Date ||
             val instanceof java.util.Date;
};

/**
 * Returns true if the value passed as argument is an object. Since
 * this value might be constructed using the safeEval's scope
 * this method tests both the application's scope and the safe one.
 * @param {Object} val The value to test
 * @returns True if the value is an object, false otherwise
 */
var isObject = function(val) {
   return val instanceof Object ||
             val instanceof java.lang.Object;
};

/**
 * Parses the argument string passed into an array containing
 * evaluated arguments. The string can contain object and array literals,
 * strings, numbers and dates (using standard Javascript syntax).
 * @param {String} str The string to parse
 * @returns The parsed arguments
 * @type Array
 */
var parseArguments = function(str) {
   var result = [];
   var c, literalLevel = 0;
   var buf = new java.lang.StringBuffer();
   for (var i=0;i<str.length;i++) {
      c = str.charAt(i);
      if (c == "," && literalLevel == 0) {
         result.push(evalArgument(buf.toString()));
         buf.setLength(0);
      } else {
         if (c == "[" || c == "{") {
            literalLevel += 1;
         } else if (c == "]" || c == "}") {
            literalLevel -= 1;
         }
         buf.append(c);
      }
   }
   if (buf.length() > 0) {
      result.push(evalArgument(buf.toString()));
   }
   return result;
};

/**
 * Parses a single argument string using the safeEval's method
 * eval(). This way users can't do any harm since all they have is
 * a plain Javascript environment without any Helma specifics.
 * @param {String} str The string to evaluate
 * @returns The evaluated argument
 */
var evalArgument = function(str) {
   if (str) {
      str = str.trim();
      return safeEval("(" + str + ")");
   }
   return null;
};

/**
 * Returns the object passed as argument as formatted JSOn compatible
 * string.
 * @param {Object} obj The object to format as string
 * @returns The formatted string
 */
var prettyPrint = function(obj) {

   var pad = function(str) {
      return "&nbsp;".repeat((lvl) * 6) + str;
   };

   var printString = function(str) {
      return '"' + encode(str) + '"';
   };

   var printInteger = function(nr) {
      return nr.toString();
   };

   var printBoolean = function(bool) {
      return bool.toString();
   };

   var printUndefined = function() {
      return "undefined";
   };
   
   var printNull = function() {
      return "null";
   };
   
   var printDate = function(date) {
      return date.toString();
   };

   var printArray = function(arr) {
      var buf = new java.lang.StringBuffer();
      buf.append("[");
      lvl += 1;
      for (var i=0;i<arr.length;i++) {
         if (i > 0) {
            buf.append(",");
         }
         buf.append("\n");
         buf.append(pad(printValue(arr[i])));
      }
      lvl -= 1;
      buf.append("\n");
      buf.append(pad("]"));
      return buf.toString();
   };

   var printObject = function(obj) {
      var buf = new java.lang.StringBuffer();
      buf.append("{");
      lvl += 1;
      var first = true;
      for (var i in obj) {
         if (first) {
            first = !first;
         } else {
            buf.append(",");
         }
         buf.append("\n");
         buf.append(pad(printString(i) + ": "));
         buf.append(printValue(obj[i]));
      }
      lvl -= 1;
      buf.append("\n");
      buf.append(pad("}"));
      return buf.toString();
   };
   
   var printValue = function(val) {
      if (isArray(val)) {
         return printArray(val);
      } else if (isDate(val)) {
         return printDate(val);
      } else if (isString(val)) {
         return printString(val);
      } else if (isNumber(val)) {
         return printInteger(val);
      } else if (isBoolean(val)) {
         return printBoolean(val);
      } else if (isNull(val)) {
         return printNull();
      } else if (isUndefined(val)) {
         return printUndefined();
      } else if (isObject(val)) {
         return printObject(val);
      } else if (val.toString != null) {
         return val.toString();
      }
      return;
   };

   var lvl = 0;
   return printValue(obj);
};

/**
 * Returns the xml source passed as argument as readable string
 * with appropriate linefeeds and indents. This method uses a
 * regular expression instead of converting the xml source into
 * a DOM tree to be able to format invalid xml which might be useful
 * for debugging.
 * @param {String} xmlSource The XML source for format
 * @returns The formatted source
 */
var prettyPrintXml = function(xmlSource) {
   var pad = function(str) {
      res.write("&nbsp;".repeat((lvl) * 6) + encode(str));
   };

   // remove all linefeeds and carriage returns
   var xml = xmlSource.replace(/\r\n|\n\r|\n|\r/g, "");
   var re = /<(\/?)([^>]+)[^<]+(?=<|$)/gm;
   var lvl = 0;
   var match;
   var tag, prevTag;
   res.push();
   while (match = re.exec(xml)) {
      tag = match[2];
      if (!match[1]) {
         // opening or contentless tag
         if (match.index > 0) {
            res.write("\n");
            lvl += 1;
         }
         pad(match[0]);
         if (tag.indexOf("/") > -1) {
            lvl -= 1;
         }
      } else {
         // closing tag
         if (tag == prevTag) {
            lvl -= 1;
            res.encode(match[0]);
         } else {
            res.write("\n");
            pad(match[0]);
            lvl -= 1;
         }
      }
      prevTag = tag;
   }
   return res.pop();
};

/**
 * Basic selection macro useable for checkboxes
 * and select dropdowns. This macro checks if
 * req.data[param.name] equals param.value, and if
 * true it writes the specified param.attribute in
 * the form 'attribute="attribute"' to response.
 */
var selection_macro = function(param) {
   if (req.data[param.name] == param.value) {
      res.write(" ");
      res.write(param.attribute);
      res.write('="');
      res.write(param.attribute);
      res.write('"');
   }
   return;
};
