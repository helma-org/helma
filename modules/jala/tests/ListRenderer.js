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
 * Construct different collections as basis for the tests
 */
// 1. ArrayList
var arrayList = new jala.ListRenderer.ArrayList((function() {
   var coll = [];
   for (var i=0; i<19; i++) {
      coll[i] = {id: i, title: "Object Nr. " + i};
   }
   return coll;
})());

// 2. HopObject
var hopObject = new HopObject();
var obj;
for (var i=0; i<19; i++) {
   obj = new HopObject();
   obj.title = "HopObject Nr. " + i;
   hopObject.add(obj);
}

// 3. Array
var array = [];
for (var i=0; i<19; i++) {
   array[i] = {id: i, title: "Object Nr. " + i};
}

/**
 * Test constructor
 */
var testConstructor = function() {
   // should throw an exception when called without or with invalid arguments
   assertThrows(function() {
      new jala.ListRenderer(true);
   });
   assertThrows(function() {
      new jala.ListRenderer();
   });
   // test constructor with arrayList
   var list = new jala.ListRenderer(arrayList);
   assertEqual(list.getCollection(), arrayList);
   // test constructor with hopObjectCollection
   list = new jala.ListRenderer(hopObject);
   assertEqual(list.getCollection(), hopObject);
   // test constructor with an array
   list = new jala.ListRenderer(array);
   assertEqual(list.getCollection().constructor, jala.ListRenderer.ArrayList);

   // test backwards compatibility
   var listParam = {
      collection: hopObject,
      href: "http://localhost/test",
      urlParams: "one=two&three=four",
      urlParamName: "seite",
      itemsPerPage: 5,
      maxPages: 3,
      itemSkin: "preview",
   };
   var list = new jala.ListRenderer(listParam);
   assertEqual(list.getCollection(), listParam.collection);
   assertEqual(list.getBaseHref(), listParam.href);
   assertEqual(list.getUrlParameters(), listParam.urlParams);
   assertEqual(list.getUrlParameterName(), listParam.urlParamName);
   assertEqual(list.getPageSize(), listParam.itemsPerPage);
   assertEqual(list.getMaxPages(), listParam.maxPages);
   assertEqual(list.getItemSkin(), listParam.itemSkin);
   return;
};

/**
 * Test the calculation of the page number under various circumstances
 */
var testPageCalculation = function() {
   var list = new jala.ListRenderer(arrayList);
   list.setPageSize(3);
   assertEqual(list.getTotalPages(), 7);
   // after setting maxPages, getTotalPages() should return this value
   list.setMaxPages(3);
   assertEqual(list.getTotalPages(), 3);
   return;
};

/**
 * Test the start and end index calculation 
 */
var testIndexCalculation = function() {
   var list = new jala.ListRenderer(arrayList);
   list.setPageSize(5);
   req.data.page = "456";
   assertEqual(list.getStartIndex(), 15);
   assertEqual(list.getEndIndex(), 18);
   // test the appropriate macros too
   assertEqual(list.currentStart_macro(), 16);
   assertEqual(list.currentEnd_macro(), 19);
   assertEqual(list.size_macro(), 19);
   // limit the number of pages - the size_macro() should return the correct value
   list.setMaxPages(2);
   assertEqual(list.size_macro(), 10);
   // reset req.data.page
   delete req.data.page;
   return;
};

/**
 * Test the construction of page hrefs
 */
var testHrefs = function() {
   var baseHref = "http://localhost/test/list";
   var list = new jala.ListRenderer(arrayList);
   list.setBaseHref(baseHref);
   assertEqual(list.getBaseHref(), baseHref);
   // getPageHref without argument should return the href of the first page
   assertEqual(list.getPageHref(), baseHref + "?page=1");
   // tweak req.data to simulate a request for a certain page
   req.data.page = "2";
   assertEqual(list.getPageHref(), baseHref + "?page=2");
   // getPageHref with page number as argument
   assertEqual(list.getPageHref(10), baseHref + "?page=10");
   // invalid page arguments
   req.data.page = "nada";
   assertEqual(list.getPageHref(), baseHref + "?page=1");
   // for page numbers < 0 return the href of the first page
   req.data.page = "-10";
   assertEqual(list.getPageHref(), baseHref + "?page=1");
   // for page numbers exceeding the max. number of pages, return
   // the href of the last page
   req.data.page = "300";
   assertEqual(list.getPageHref(), baseHref + "?page=2");
   
   // now test changing the page url parameter name
   list.setUrlParameterName("seite");
   assertEqual(list.getPageHref(2), baseHref + "?seite=2"); 

   // add additional url parameters
   var params = "one=two&three=four";
   list.setUrlParameters(params);
   assertEqual(list.getPageHref(3), baseHref + "?" + params + "&seite=3");

   // reset req.data.page
   delete req.data.page;
   return;
};

/**
 * Test custom renderer
 */
var testRenderer = function() {
   // a pseudo renderer to check if overriding default renderer works
   var renderer = {
      "list": {
         "custom": function() {
            return;
         },
         "default": function() {
            return;
         },
      },
   };

   // use default renderer
   var list = new jala.ListRenderer(arrayList);
   assertEqual(list.getRenderFunction("list"),
               jala.ListRenderer.defaultRenderer.list["default"]);
   assertEqual(list.getRenderFunction("list", "nonexisting"),
               jala.ListRenderer.defaultRenderer.list["default"]);
   assertNull(list.getRenderFunction("nonexisting"));

   // use custom renderer
   list = new jala.ListRenderer(arrayList, renderer);
   assertEqual(list.getRenderFunction("list", "custom"), renderer.list["custom"]);
   assertEqual(list.getRenderFunction("list", "nonexisting"), renderer.list["default"]);
   assertEqual(list.getRenderFunction("list"), renderer.list["default"]);
   return;
};
