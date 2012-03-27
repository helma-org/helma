var tests = [
   "testConstructor",
   "testGetDirectory",
   "testGetRAMDirectory",
   "testCreateIndex",
   "testGetReaderWriter",
   "testIndexLock",
   "testDocument",
   "testAddDocuments",
   "testSearch"
];

app.addRepository("modules/helma/Search.js");

var indexName = "testindex";
var basePath = java.lang.System.getProperty("java.io.tmpdir");
var index;

/**
 * Test preliminaries
 */
var setup = function() {
   // check if there is a (leftover) directory with the same name
   // in the system's temporary directory - if so throw an exception and stop
   var dir = new helma.File(basePath, indexName);
   if (dir.exists()) {
      throw "There is already a directory '" + dir.getAbsolutePath() +
            "', please remove it before trying to run test again";
   }
   return;
};

/**
 * Test the helma.Search constructor to make sure Lucene is loaded
 */
var testConstructor = function() {
   // should not throw an exception
   var s = new helma.Search();
   return;
};

/**
 * Test getDirectory method
 */
var testGetDirectory = function() {
   var search = new helma.Search();
   assertThrows(function() {
      search.getDirectory();
   });
   var dirs = [
      new helma.File(basePath, indexName),
      new File(basePath, indexName),
      new java.io.File(basePath, indexName),
      basePath + "/" + indexName
   ];
   var dir, fsDir, dirPath;
   for (var i in dirs) {
      dir = dirs[i];
      if (dir.constructor != String) {
         dirPath = dir.getAbsolutePath();
      } else {
         dirPath = dir;
      }
      fsDir = search.getDirectory(dir);
      assertNotNull(fsDir);
      assertEqual(fsDir.getFile().getAbsolutePath(), dirPath);
   }
   return;
};

/**
 * Test getRAMDirectory method
 */
var testGetRAMDirectory = function() {
   var search = new helma.Search();
   assertThrows(function() {
      search.getDirectory();
   });
   var dirs = [
      new helma.File(basePath, indexName),
      new File(basePath, indexName),
      new java.io.File(basePath, indexName),
      basePath + "/" + indexName
   ];
   var dir, ramDir;
   for (var i in dirs) {
      dir = dirs[i];
      ramDir = search.getRAMDirectory(dir);
      assertNotNull(ramDir);
   }
   return;
};

/**
 * Test index creation - this method creates a RAMDirectory based
 * index for testing and stores it in the global variable "index"
 */
var testCreateIndex = function() {
   var search = new helma.Search();
   // test creating a file based index
   var fsDir = search.getDirectory(new helma.File(basePath, indexName));
   index = search.createIndex(fsDir);
   assertNotNull(index);
   // explicitly test index.create(true)
   assertTrue(index.create(true));

   // test creating a ram based index
   var ramDir = search.getRAMDirectory();
   index = search.createIndex(ramDir);
   assertNotNull(index);
   // explicitly test index.create(true)
   assertTrue(index.create(true));
   assertEqual(index.constructor, helma.Search.Index);
   assertEqual(index.size(), 0);
   return;
};

/**
 * Test getting index reader, writer and modifier
 */
var testGetReaderWriter = function() {
   // test getReader
   var reader = index.getReader();
   assertNotNull(reader);
   reader.close();
   // test getWriter
   var writer = index.getWriter();
   assertNotNull(writer);
   writer.close();
   return;
};

/**
 * Test index locking
 */
var testIndexLock = function() {
   // test if the index is correctly locked when opening a writer
   var writer = index.getWriter();
   assertTrue(index.isLocked());
   // test if getWriter() throws an exception when trying to open a second writer
   assertThrows(function() {
      index.getWriter();
   });
   writer.close();
   assertFalse(index.isLocked());
   return;
};

/**
 * Test document constructor and methods
 */
var testDocument = function() {
   var doc = new helma.Search.Document();
   var f;

   // test type conversion
   f = new helma.Search.Document.Field("id", 1);
   assertEqual(f.value.constructor, String);
   assertEqual(f.value, "1");
   var now = new Date();
   f = new helma.Search.Document.Field("createtime", now);
   assertEqual(f.dateValue.constructor, Date);
   assertEqual(f.dateValue.getTime(), now.getTime() - (now.getTime() % 60000));

   // test adding field with default store and index options
   doc.addField(new helma.Search.Document.Field("id", 1));
   f = doc.getField("id");
   assertNotNull(f);
   assertTrue(f.isStored());
   assertTrue(f.isIndexed());
   assertTrue(f.isTokenized());

   // test adding date field with changed field options
   f = new helma.Search.Document.Field("createtime", new Date(), {
      store: "no",
      index: "tokenized"
   });
   doc.addField(f);
   f = doc.getField("createtime");
   assertNotNull(f);
   assertFalse(f.isStored());
   assertTrue(f.isIndexed());
   assertTrue(f.isTokenized());

   // test deprecated way of calling addField()
   doc.addField("title", "Just a test", {
      "store": false,
      "index": true,
      "tokenize": false
   });
   f = doc.getField("title");
   assertNotNull(f);
   assertFalse(f.isStored());
   assertTrue(f.isIndexed());
   assertFalse(f.isTokenized());

   // test getFields()
   var fields = doc.getFields();
   assertEqual(fields.length, 3);
   assertEqual(fields[0].name, "id");
   assertEqual(fields[1].name, "createtime");
   assertEqual(fields[2].name, "title");
   return;
};

/**
 * Test adding documents
 */
var testAddDocuments = function() {
   // test addDocument()
   var doc = new helma.Search.Document();
   doc.addField(new helma.Search.Document.Field("id", 1));
   index.addDocument(doc);
   assertEqual(index.size(), 1);

   // test removeDocument()
   index.removeDocument("id", 1);
   assertEqual(index.size(), 0);

   // test addDocuments() and removeDocuments() with an array
   doc = new helma.Search.Document();
   doc.addField(new helma.Search.Document.Field("id", 2));
   index.addDocuments([doc]);
   assertEqual(index.size(), 1);
   index.removeDocuments("id", [2]);
   assertEqual(index.size(), 0);

   // test addDocuments() and removeDocuments() with a Hashtable as argument
   var ht = new java.util.Hashtable();
   ht.put("doc", doc);
   index.addDocuments(ht);
   ht = new java.util.Hashtable();
   ht.put("id", 1);
   ht.put("id", 2);
   index.removeDocuments("id", ht);
   assertEqual(index.size(), 0);

   // test addDocuments() and removeDocuments() with a Vector as argument
   var v = new java.util.Vector();
   v.add(doc);
   index.addDocuments(v);
   v = new java.util.Vector();
   v.add(1);
   v.add(2);
   index.removeDocuments("id", v);
   assertEqual(index.size(), 0);

   // test updateDocument
   index.addDocument(doc);
   doc = new helma.Search.Document();
   doc.addField("id", 2);
   index.updateDocument(doc, "id");
   assertEqual(index.size(), 1);

   // test count()
   doc = new helma.Search.Document();
   doc.addField("id", 3);
   index.addDocument(doc);
   assertEqual(index.count("id", 3), 1);

   return;
};

/**
 * Test searching the index
 */
var testSearch = function() {
   // clear the index
   index.create();
   assertEqual(index.size(), 0);

   // populate the index with test content
   var names = [
      "foo",
      "bar",
      "baz"
   ];

   var now = new Date();
   var doc;
   names.forEach(function(name, idx) {
      doc = new helma.Search.Document();
      doc.addField("id", idx + 1);
      doc.addField("parent", idx % 2);
      doc.addField("name", name);
      doc.addField("timestamp", new Date(now.getTime() - (idx * 1e6)));
      index.addDocument(doc);
   });
   assertEqual(index.size(), 3);

   var searcher = index.getSearcher();
   assertNotNull(searcher);
   assertNull(searcher.sortFields);
   assertNotNull(searcher.getSearcher());
   assertTrue(searcher.getSearcher() instanceof Packages.org.apache.lucene.search.IndexSearcher);

   // test basic search
   var q = new helma.Search.TermQuery("id", 1);
   assertEqual(searcher.search(q), 1);
   assertNotNull(searcher.hits);
   assertEqual(searcher.hits.constructor, helma.Search.HitCollection);
   assertEqual(searcher.hits.size(), 1);
   var hit = searcher.hits.get(0);
   assertNotNull(hit);
   assertEqual(hit.constructor, helma.Search.Document);
   assertEqual(hit.getField("name").constructor, helma.Search.Document.Field);
   assertEqual(hit.getField("name").value, "foo");
   // test date value conversion
   assertEqual(hit.getField("timestamp").value.constructor, String);
   assertEqual(hit.getField("timestamp").dateValue.constructor, Date);

   // test query filter
   var qf = new helma.Search.QueryFilter(new helma.Search.TermQuery("parent", 0));
   q = new helma.Search.WildcardQuery("name", "ba*");
   assertEqual(searcher.search(q, qf), 1);
   assertEqual(searcher.hits.get(0).getField("name").value, names[2]);

   // test sorting of hits
   searcher.sortBy("id", true);
   assertEqual(searcher.search(q), 2);
   assertEqual(searcher.hits.get(0).getField("name").value, names[2]);
   assertEqual(searcher.hits.get(1).getField("name").value, names[1]);

   // test boolean query
   q = new helma.Search.BooleanQuery();
   q.addTerm("parent", "0");
   assertEqual(q.toString(), "[(parent:0)]");
   q = new helma.Search.BooleanQuery();
   q.addTerm("parent", "0", "and");
   assertEqual(q.toString(), "[+(parent:0)]");
   q = new helma.Search.BooleanQuery();
   q.addTerm("parent", "0", "not");
   assertEqual(q.toString(), "[-(parent:0)]");

   searcher.close();
   return;
};

/**
 * Cleanup
 */
var cleanup = function() {
   // remove the directory containing the test index
   var dir = new helma.File(basePath, indexName);
   dir.removeDirectory();
   return;
}