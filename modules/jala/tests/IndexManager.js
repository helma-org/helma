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
 * Contains the index manager to test
 * @type jala.IndexManager
 */
var index;

/**
 * Contains the queue of the index manager
 */
var queue;

/**
 * A global id "counter"
 */
var idCounter;

/**
 * Called before running the tests
 */
var setup = function() {
   // create the index to test
   var dir = new java.io.File(java.lang.System.getProperty("java.io.tmpdir"));
   index = new jala.IndexManager("test", dir, "de");
   queue = index.getQueue();
   idCounter = 0;
   index.start();
   index.add(createDocumentObject());
   return;
};

/**
 * Called after tests have finished. This method will be called
 * regarless whether the test succeeded or failed.
 */
var cleanup = function() {
   if (index) {
      // clear the index before removing the index directory itself
      index.getIndex().create();
      var dir = new java.io.File(java.lang.System.getProperty("java.io.tmpdir"), "test");
      if (dir.exists()) {
         var segments = new java.io.File(dir, "segments");
         if (segments.exists()) {
            segments["delete"]();
         } 
         dir["delete"]();
      }
      index.stop();
   }
   return;
};

/**
 * Test adding a document object immediately
 */
var testAdd = function() {
   index.add(createDocumentObject());
   // check queue and job
   assertEqual("q size", queue.size(), 2);
   assertEqual("q 0th type", queue.get(0).type, jala.IndexManager.Job.ADD);
   // check if the document was added correctly
   // but give the index manager time to process
   java.lang.Thread.currentThread().sleep(500);
   assertEqual("index size", index.getIndex().size(), 2);
   assertEqual("new q size", queue.size(), 0);
   // check if the index has been optimized
   var reader = null;
   try {
      reader = index.getIndex().getReader();
      assertTrue(reader.isOptimized());
   } finally {
      if (reader !== null) {
         reader.close();
      }
   }
   return;
};

/**
 * Test removing a document object immediately
 */
var testRemove = function() {
   var id = 0;
   index.remove(id);
   // check queue and job
   assertEqual("queue size", queue.size(), 2);
   assertEqual("type is remove", queue.get(1).type, jala.IndexManager.Job.REMOVE);
   // check if the document was added correctly
   // but give the index manager time to process
   java.lang.Thread.currentThread().sleep(500);
   assertEqual("empty index", index.getIndex().size(), 0);
   assertEqual("empty queue", queue.size(), 0);
   // check if the index has been optimized
   var reader = null;
   try {
      reader = index.getIndex().getReader();
      assertTrue("is optimized", reader.isOptimized());
   } finally {
      if (reader !== null) {
         reader.close();
      }
   }
   return;
};

/**
 * Test immediate index optimization
 */
var testOptimize = function() {
   index.optimize();
   // check queue and job
   assertEqual(queue.size(), 2);
   assertEqual(queue.get(1).type, jala.IndexManager.Job.OPTIMIZE);
   // give the index manager time to process
   java.lang.Thread.currentThread().sleep(300);
   assertFalse(index.hasOptimizingJob());
   // check if the index has been optimized
   var reader = null;
   try {
      reader = index.getIndex().getReader();
      assertTrue(reader.isOptimized());
   } finally {
      if (reader !== null) {
         reader.close();
      }
   }
   return;
};

/**
 * Creates a new document object to be put into the index
 * @returns A newly created document object containing test data
 * @type helma.Search.Document
 */
var createDocumentObject = function() {
   var id = idCounter;
   var doc = new helma.Search.Document();
   doc.addField("id", id, {store: "yes", index: "unTokenized"});
   doc.addField("name", "Document " + id, {store: "yes", index: "tokenized"});
   doc.addField("createtime", (new Date()).format("yyyyMMddHHmm"), {store: "yes", index: "unTokenized"});
   idCounter += 1;
   return doc;
};

/**
 * Test query parsing
 */
var testParseQuery = function() {
   assertThrows(function() {
      index.parseQuery();
   });
   assertThrows(function() {
      index.parseQuery("test");
   });
   var query;
   query = index.parseQuery("test", ["title"]);
   assertTrue(query instanceof Packages.org.apache.lucene.search.TermQuery);
   assertEqual(query.getTerm().field(), "title");
   query = index.parseQuery("test again", ["title"]);
   assertTrue(query instanceof Packages.org.apache.lucene.search.BooleanQuery);
   assertEqual(query.getClauses().length, 2);

   // test with more than one field
   query = index.parseQuery("test", ["title", "body"]);
   assertTrue(query instanceof Packages.org.apache.lucene.search.BooleanQuery);
   assertEqual(query.getClauses().length, 2);
   assertEqual(query.getClauses()[0].getQuery().getTerm().field(), "title");
   assertEqual(query.getClauses()[1].getQuery().getTerm().field(), "body");

   // test boostmap
   query = index.parseQuery("test", ["title", "body", "creator"], {title: 10, body: 5});
   assertEqual(query.getClauses()[0].getQuery().getBoost(), 10);
   assertEqual(query.getClauses()[1].getQuery().getBoost(), 5);
   // default boost factor is 1
   assertEqual(query.getClauses()[2].getQuery().getBoost(), 1);
   return;
};

/**
 * Test query filter parsing
 */
var testParseQueryFilter = function() {
   assertNull(index.parseQueryFilter());
   var query;
   query = index.parseQueryFilter("title:test");
   assertTrue(query instanceof Packages.org.apache.lucene.search.CachingWrapperFilter);
   // FIXME: can't reach the wrapped query filter, therefor this stupid assertion
   assertEqual(query.toString(), "CachingWrapperFilter(QueryWrapperFilter(title:test))");
   query = index.parseQueryFilter(["title:test", "body:test"]);
   assertTrue(query instanceof Packages.org.apache.lucene.search.CachingWrapperFilter);
   assertEqual(query.toString(), "CachingWrapperFilter(QueryWrapperFilter(+title:test +body:test))");
   return;
};

/**
 * Test searching the index
 */
var testSearch = function() {
   for (var i = 0; i < 10; i += 1) {
      index.add(createDocumentObject());
   }
   // check if the documents was added correctly
   // but give the index manager time to process
   java.lang.Thread.currentThread().sleep(300);
   // check if the index has been optimized
   var reader = null;
   try {
      reader = index.getIndex().getReader();
      assertTrue("is optimized", reader.isOptimized());
   } finally {
      if (reader !== null) {
         reader.close();
      }
   }

   var query = index.parseQuery("doc*", ["name"]);
   var hits, filter, sortFields;
   // test basic search
   hits = index.search(query);
   assertNotNull("non null hits", hits);
   assertEqual("hit count", hits.size(), 11);
   // test (stupid) filtering
   filter = index.parseQueryFilter("id:1");
   hits = index.search(query, filter);
   assertEqual("1 hit", hits.size(), 1);
   assertEqual("first hit id", parseInt(hits.get(0).getField("id").value, 10), 1);
   // test range filtering
   filter = index.parseQueryFilter("id:[2 TO 6]");
   hits = index.search(query, filter);
   assertEqual("5 hits", hits.size(), 5);
   // test sorting
   sortFields = [new Packages.org.apache.lucene.search.SortField("id", true)];
   hits = index.search(query, null, sortFields);
   assertEqual("new hit count", hits.size(), 11);
   assertEqual("first hit id", parseInt(hits.get(0).getField("id").value, 10), 10);
   assertEqual("last hit id", parseInt(hits.get(9).getField("id").value, 10), 1);
   return;
};
