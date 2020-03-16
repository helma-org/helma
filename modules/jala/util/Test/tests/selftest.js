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
 * Declare which test methods should be run in which order
 * @type Array
 * @final
 */

/**
 * Test for jala.Test.evalArguments
 */
var testEvalArguments = function testEvalArguments() {
   var args;
   // test arguments without a comment
   args = [true, false, 1, "one", new Date()];
   jala.Test.evalArguments(args, 5);
   // test arguments with a comment
   args = ["a comment", true, false, 1, "one", new Date()];
   jala.Test.evalArguments(args, 5);
   return;
};

/**
 * Test for jala.Test.containsComment
 */
var testArgsContainComment = function testArgsContainComment() {
   var args = ["a comment", true];
   if (jala.Test.argsContainComment(args, 1) !== true) {
      throw new jala.Test.TestException(null,
                      "Argument array is supposed to contain a comment, but doesn't");
   }
   return;
};

/**
 * Test for jala.Test.getComment
 */
var testGetComment = function testGetComment() {
   var args = ["a comment", true];
   if (jala.Test.getComment(args, 1) !== args[0]) {
      throw new jala.Test.TestException(null, "Couldn't get comment");
   }
   return;
};

/**
 * Test for jala.Test.getValue
 */
var testGetValue = function testGetValue() {
   var args = ["a comment", 1, 2, 3];
   if (jala.Test.getValue(args, 3, 1) !== args[2]) {
      throw new jala.Test.TestException("Couldn't get correct argument value");
   }
   return;
};

/**
 * Testing assertion functions
 */
var testBasicAssertionFunctions = function testAssertionFunctions() {
   assertTrue("just a comment", true);
   assertFalse("just a comment", false);
   assertEqual(1, 1);
   assertEqualArrays("asserting arrays", [1,2,3], [1,2,3]);
   assertEqualArrays(["1","2"], ["1","2"]);
   assertNotEqual(1, 2);
   assertNull(null);
   assertNotNull(true);
   assertUndefined(undefined);
   assertNotUndefined(true);
   assertNaN("one");
   assertNotNaN(1);
   assertStringContains("just a self test", "self");
   assertMatch("just a self test", /^just/);
   return;
};

/**
 * Testing assertThrows
 */
var testAssertThrows = function testAssertThrows() {
   // throw undefined (yes, you can do that...)
   assertThrows(function() {
      throw undefined;
   }, undefined);
   // throw custom javascript object
   assertThrows(function() {
      throw new jala.Test.TestException("", "message");
   }, jala.Test.TestException);
   // throw string
   assertThrows(function() {
      throw "my message";
   }, "my message");
   // throw java exception
   assertThrows(function() {
      var x = new java.util.Vector(0);
      res.debug(x.get(1));
   }, java.lang.ArrayIndexOutOfBoundsException);
   // throw anything, but don't check further
   assertThrows(function() {
      throw new Date();
   });
   // don't throw an expected exception
   assertThrows(function() {
      assertThrows(function() {
         return;
      }, "oy");
   }, jala.Test.TestException);
   return;
};

var testInclude = function() {
   var dir = java.lang.System.getProperty("java.io.tmpdir");
   var content = "var INCLUDED = true;";
   // create include file with the above content
   var file = new helma.File(dir, "testInclude." + (new Date()).getTime());
   file.open();
   file.write(content);
   file.close();
   include(file);
   // now include the file and test if everything works
   assertTrue(global["INCLUDED"]);
   // finally remove the include file again
   file.remove();
   return;
};

/**
 * Testing testAssertEqualFile
 */
var testAssertEqualFile = function testAssertEqualFile() {
   var str = "This is just a simple test\r\n";
   var dir = java.lang.System.getProperty("java.io.tmpdir");
   // create test file and write the string into the file
   var testFile = new helma.File(dir, "testAssertEqualFile." + (new Date()).getTime());
   testFile.open();
   testFile.write(str);
   testFile.close();
   // test string comparison
   assertEqualFile(str, testFile);
   // test byte array comparison
   var arr = new java.lang.String(str).getBytes();
   assertEqualFile(arr, testFile);
   // finally, remove testFile again
   testFile.remove();
   return;
};
