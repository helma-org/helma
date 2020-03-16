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
 * @fileoverview Helper methods for use as embedded JavaScript in Ant.
 */


Util = {
   counter:0
};

Util.readFile = function (filename) {
   Util.counter++;
   var loader = project.createTask("loadfile");
   loader.setSrcFile(new java.io.File(filename));
   loader.setProperty("loaderResult" + Util.counter);
   try {
      loader.execute();
      return String(project.getProperty("loaderResult" + Util.counter));
   } catch (anyerror) {
      return "";
   }
};

Util.loadProperties = function(filename) {
   var props = new java.util.Properties();
   var inStream = new java.io.FileInputStream(filename);
   props.load(inStream);
   return props;
};

Util.log = function (str) {
   java.lang.System.out.println(str);
};

Util.getFile = function(dir, file) {
   return new java.io.File(new java.io.File(dir).getCanonicalPath(), file);
};

Util.writeToFile = function(filename, str) {
   var echo = project.createTask("echo");
   echo.setMessage(str);
   echo.setFile(new java.io.File(filename));
   echo.execute();
   return true;
};

Util.setProperty = function(propName, propValue) {
   var prop = project.createTask("property");
   prop.setName(propName);
   prop.setValue(propValue);
   prop.execute();
};

String.prototype.trim = function() {
   return this.match(/^\s*(.*?)\s*$/)[1];
};

/**
 * transforms the first n characters of a string to uppercase
 * @param Number amount of characters to transform
 * @return String the resulting string
 */
String.prototype.capitalize = function(limit) {
    if (limit == null)
        limit = 1;
    var head = this.substring(0, limit);
    var tail = this.substring(limit, this.length);
    return head.toUpperCase() + tail.toLowerCase();
};
