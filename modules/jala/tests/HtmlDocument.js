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

var source = '<html><head><title>Test</title></head><body>' +
             '<h1>Hello, World!</h1>' +
             '<a href="http://localhost/1">foo</a>' +
             '<div><a href="http://localhost/2">bar</a></div>' +
             '<a href="http://localhost/3">foobar</a>' +
             '</body></html>';

/**
 * Simple test of the HtmlDocument.getLinks method.
 * An instance of HtmlDocument is created from a very 
 * simple HTML source. The result of getLinks is then
 * evaluated and tested.
 */
var testGetLinks = function() {
   var html = new jala.HtmlDocument(source);
   var links = html.getLinks();
   assertEqual(links.constructor, Array);
   assertEqual(links.length, 3);
   assertEqual(links[0].constructor, Object);
   for (var i in links) {
     assertNotUndefined(links[i].url);
     assertNotUndefined(links[i].text);
   }
   assertEqual(links[0].url, "http://localhost/1");
   assertEqual(links[0].text, "foo");
   assertEqual(links[1].url, "http://localhost/2");
   assertEqual(links[1].text, "bar");
   assertEqual(links[2].url, "http://localhost/3");
   assertEqual(links[2].text, "foobar");
   return;
};

/**
 * Simple test of the HtmlDocument.geAll method.
 * An instance of HtmlDocument is created from a very 
 * simple HTML source. The result of getAll is then
 * evaluated and tested.
 */
var testGetAll = function() {
   var names = ["html", "head", "title", "body", "h1", "a", "div", "a", "a"];
   var html = new jala.HtmlDocument(source);
   var list = html.getAll("*");
   for (var i in list) {
     assertNotUndefined(list[i].name);
     assertEqual(list[i].name, names[i]);
   }
   assertEqual(list[2].value, "Test");
   assertEqual(list[4].value, "Hello, World!");
   assertEqual(list[5].value, "foo");
   assertEqual(list[7].value, "bar");
   assertEqual(list[8].value, "foobar");
   assertEqual(html.getAll("h1")[0].value, "Hello, World!");
   return;
};
