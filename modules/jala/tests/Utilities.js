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
 * Unit test for #jala.util.createPassword.
 */
var testCreatePassword = function() {
   assertMatch(jala.util.createPassword(),         /^[^\d]{8}$/);
   assertMatch(jala.util.createPassword(100),      /^[^\d]{100}$/);
   assertMatch(jala.util.createPassword(null, 0),  /^[^\d]{8}$/);
   assertMatch(jala.util.createPassword(100, 0),   /^[^\d]{100}$/);
   assertMatch(jala.util.createPassword(null, 1),  /^[\d\w]{8}$/);
   assertMatch(jala.util.createPassword(100, 1),   /^[\d\w]{100}$/);
   assertEqual(jala.util.createPassword(null, 2).length, 8);
   assertEqual(jala.util.createPassword(100, 2).length, 100);
   return;
};

var o1 = {a: 1, b: 2, d: 4, e: {f: 6, g: 7}, h: {i: 9}};
var o2 = {a: 2, c: 3, d: 4, e: {f: 7, h: 8}, i: {j: 10}};
var diff;

/**
 * Unit test for #jala.util.diffObjects.
 */
var testDiffObjects = function() {
   // diffing various simple objects
   diff = jala.util.diffObjects({}, {a: 1});
   assertNotNull(diff);
   assertEqual(diff.a.status, jala.Utilities.VALUE_ADDED);

   diff = jala.util.diffObjects({a: 1}, {});
   assertNotNull(diff);
   assertEqual(diff.a.status, jala.Utilities.VALUE_REMOVED);

   diff = jala.util.diffObjects({a: {b: 1}}, {a: 1});
   assertNotNull(diff);
   assertEqual(diff.a.status, jala.Utilities.VALUE_MODIFIED);

   diff = jala.util.diffObjects({a: {b: 1}}, {a: {b: 1, c: 1}});
   assertNotNull(diff);
   assertEqual(diff.a.c.status, jala.Utilities.VALUE_ADDED);

   diff = jala.util.diffObjects({a: {b: 1}}, {a: {c: 1}});
   assertNotNull(diff);
   assertEqual(diff.a.b.status, jala.Utilities.VALUE_REMOVED);
   assertEqual(diff.a.c.status, jala.Utilities.VALUE_ADDED);

   // diffing pre-defined objects
   diff = jala.util.diffObjects(o1, o2);

   assertNotNull(diff);
   assertNotUndefined(diff);
   assertEqual(diff.constructor, Object);

   assertNotUndefined(diff.a);
   assertNotUndefined(diff.b);
   assertNotUndefined(diff.c);
   assertUndefined(diff.d);

   assertNotNull(diff.a);
   assertNotNull(diff.b);
   assertNotNull(diff.c);

   assertEqual(diff.a.value, o2.a);
   assertUndefined(diff.b.value);
   assertEqual(diff.c.value, o2.c);

   assertEqual(diff.a.status, jala.Utilities.VALUE_MODIFIED);
   assertEqual(diff.b.status, jala.Utilities.VALUE_REMOVED);
   assertEqual(diff.c.status, jala.Utilities.VALUE_ADDED);
   assertEqual(diff.e.f.status, jala.Utilities.VALUE_MODIFIED);
   assertEqual(diff.e.g.status, jala.Utilities.VALUE_REMOVED);
   assertEqual(diff.e.h.status, jala.Utilities.VALUE_ADDED);
   assertEqual(diff.h.status, jala.Utilities.VALUE_REMOVED);
   assertEqual(diff.i.status, jala.Utilities.VALUE_ADDED);

   return;
};

/**
 * Unit test for #jala.util.patchObject.
 */
var testPatchObject = function() {
   diff = jala.util.diffObjects(o1, o2);
   jala.util.patchObject(o1, diff);
   
   assertNotNull(o1);
   assertNotUndefined(o1);
   assertEqual(o1.constructor, Object);

   assertNotUndefined(o1.a);
   assertUndefined(o1.b);
   assertNotUndefined(o1.c);
   assertNotUndefined(o1.d);

   assertNotNull(o1.a);
   assertNotNull(o1.c);
   assertNotNull(o1.d);

   assertEqual(o1.a, o2.a);
   assertEqual(o1.c, o2.c);
   assertEqual(o1.d, o2.d);

   assertEqual(o1.e.f, o2.e.f);
   assertUndefined(o1.e.g);
   assertEqual(o1.e.h, o2.e.h);
   assertUndefined(o1.h);
   assertEqual(o1.i.j, o2.i.j);
   return;
};
