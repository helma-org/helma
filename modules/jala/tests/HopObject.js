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

var result = undefined;

/**
 * Unit test for {@link #HopObject.getAccessName}.
 */
var testGetAccessName = function() {
   var name = "foobar";
   var collection = new HopObject();
   
   assertEqual(collection.getAccessName(name), name);
   // Test alias with the same name as a default HopObject method
   assertNotEqual(collection.getAccessName("get"), "get");
   assertEqual(collection.getAccessName("get"), "get-1");
   // Set a custom property of the collection and test it
   collection[name] = true;
   assertNotEqual(collection.getAccessName(name), name);
   assertEqual(collection.getAccessName(name), name + "-1");
   
   // Set custom properties equally to the method's numbering
   collection[name + "-1"] = true;
   collection[name + "-12"] = true;
   assertNotEqual(collection.getAccessName(name), name + "1");
   assertNotEqual(collection.getAccessName(name), name + "12");
   assertEqual(collection.getAccessName(name), name + "-2");

   assertNotEqual(collection.getAccessName(name, name.length), name);
   assertEqual(collection.getAccessName(name, name.length), "foob-1");
   return;
};

