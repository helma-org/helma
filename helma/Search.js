//
// A wrapper for Apache Lucene for use with Helma Object Publisher
// Copyright (c) 2005-2006 Robert Gaggl
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
//
//
// $Revision: 1.8 $
// $Author: robert $
// $Date: 2007/01/30 14:56:51 $
//


/**
 * @fileoverview Fields and methods of the helma.Search class
 */

// take care of any dependencies
app.addRepository('modules/helma/lucene-core.jar');
app.addRepository('modules/helma/lucene-analyzers.jar');


if (!global.helma) {
    global.helma = {};
}

/**
 * Constructs a new instance of helma.Search. This merely
 * checks if the Apache Lucene library is in the application
 * classpath.
 * @class This class provides functionality for
 * creating a fulltext search index based on Apache Lucene.
 * @returns A newly created instance of this prototype.
 * @constructor
 * @author Robert Gaggl <robert@nomatic.org> 
 */
helma.Search = function() {
    try {
        var c = java.lang.Class.forName("org.apache.lucene.analysis.Analyzer",
                                        true, app.getClassLoader());
       // FIXME: add additional check for version, since we need >= 1.9
    } catch (e) {
        throw("helma.Search needs lucene.jar in version > 1.9 \
               in lib/ext or application directory \
               [http://lucene.apache.org/]");
    }
    return this;
};


/***************************************
 ***** S T A T I C   M E T H O D S *****
 ***************************************/

/** @ignore */
helma.Search.toString = function() {
    return "[helma.Search]";
};

/**
 * Returns a new Analyzer instance depending on the key
 * passed as argument. Currently supported arguments are
 * "br" (BrazilianAnalyzer), "cn" (ChineseAnalyzer), "cz" (CzechAnalyzer),
 * "nl" (DutchAnalyzer), "fr" (FrenchAnalyzer), "de" (GermanAnalyzer),
 * "el" (GreekAnalyzer), "keyword" (KeywordAnalyzer), "ru" (RussianAnalyzer),
 * "simple" (SimpleAnalyzer), "snowball" (SnowballAnalyzer), "stop" (StopAnalyzer)
 * "whitespace" (WhitespaceAnalyzer). If no argument is given, a StandardAnalyzer
 * is returned.
 * @param {String} key The key identifying the analyzer
 * @returns A newly created Analyzer instance
 * @type {org.apache.lucene.analysis.Analyzer}
 */
helma.Search.getAnalyzer = function(key) {
    var pkg = Packages.org.apache.lucene;
    switch (key) {
        case "br":
            return new pkg.analysis.br.BrazilianAnalyzer();
        case "cn":
            return new pkg.analysis.cn.ChineseAnalyzer();
        case "cz":
            return new pkg.analysis.cz.CzechAnalyzer();
        case "nl":
            return new pkg.analysis.nl.DutchAnalyzer();
        case "fr":
            return new pkg.analysis.fr.FrenchAnalyzer();
        case "de":
            return new pkg.analysis.de.GermanAnalyzer();
        case "el":
            return new pkg.analysis.el.GreekAnalyzer();
        case "keyword":
            return new pkg.analysis.KeywordAnalyzer();
        case "ru":
            return new pkg.analysis.ru.RussianAnalyzer();
        case "simple":
            return new pkg.analysis.SimpleAnalyzer();
        case "snowball":
            return new pkg.analysis.snowball.SnowballAnalyzer();
        case "stop":
            return new pkg.analysis.StopAnalyzer();
        case "whitespace":
            return new pkg.analysis.WhitespaceAnalyzer();
        default:
            return new pkg.analysis.standard.StandardAnalyzer();
    }
};

/**
 * Constructs a new QueryFilter instance. This class
 * wraps a lucene QueryFilter.
 * @param {helma.Search.Query} q The query object to use as
 * the basis for the QueryFilter instance.
 * @returns A newly created QueryFilter instance.
 * @constructor
 */
helma.Search.QueryFilter = function(q) {
    var wrappedFilter = new Packages.org.apache.lucene.search.QueryFilter(q.getQuery());

    /**
     * Returns the wrapped filter instance
     * @type org.apache.lucene.search.QueryFilter
     */
    this.getFilter = function() {
        return wrappedFilter;
    };

    /** @ignore */
    this.toString = function() {
        return wrappedFilter.toString();
    };

    return this;
};


/*********************************************
 ***** P R O T O T Y P E   M E T H O D S *****
 *********************************************/


/** @ignore */
helma.Search.prototype.toString = function() {
    return helma.Search.toString();
};

/**
 * Returns an instance of org.apache.lucene.store.FSDirectory. If
 * no index is present in the given directory, it is created on the fly.
 * @param {File|helma.File|java.io.File|String} dir The directory
 * where the index is located or should be created at.
 * @param {Boolean} create If true the index will be created, removing
 * any existing index in the same directory
 * @returns The index directory.
 * @type org.apache.lucene.store.FSDirectory
 */
helma.Search.prototype.getDirectory = function(dir, create) {
    if (!dir) {
        throw("helma.Search.getDirectory(): insufficient arguments.");
    }
    var d;
    if (dir.constructor == String) {
        d = new java.io.File(dir);
    } else if (dir.constructor == File || dir.constructor == helma.File) {
        d = new java.io.File(dir.getAbsolutePath());
    } else if (!((d = dir) instanceof java.io.File)) {
        throw("helma.Search.getDirectory(): " + dir + " is not a valid argument.");
    }
    return Packages.org.apache.lucene.store.FSDirectory.getDirectory(d,
               create == true ? true : !d.exists());
};

/**
 * Returns a RAM directory object.
 * @param {File|helma.File|java.io.File|String} dir Optional directory
 * containing a Lucene index from which this RAM directory should be created.
 * @returns A RAM directory instance.
 * @type org.apache.lucene.store.RAMDirectory
 */
helma.Search.prototype.getRAMDirectory = function(dir) {
    if (dir != null) {
        var d;
        if (dir.constructor == String) {
            d = new java.io.File(dir);
        } else if (dir.constructor == File || dir.constructor == helma.File) {
            d = new java.io.File(dir.getAbsolutePath());
        } else if (!((d = dir) instanceof java.io.File)) {
            throw("helma.Search.getRAMDirectory(): " + dir + " is not a valid argument.");
        }
        if (!d.exists()) {
            throw("helma.Search.getRAMDirectory(): " + dir + " does not exist.");
        }
        return Packages.org.apache.lucene.store.RAMDirectory(d);
    }
    return Packages.org.apache.lucene.store.RAMDirectory();
};

/**
 * Creates a new Lucene index in the directory passed as
 * argument, using an optional analyzer, and returns an instance
 * of helma.Search.Index. Any already existing index in this
 * directory will be preserved.
 * @param {org.apache.lucene.store.Directory} dir The directory
 * where the index should be stored. This can be either a
 * FSDirectory or a RAMDirectory instance.
 * @param {org.apache.lucene.analysis.Analyzer} analyzer The analyzer to
 * use for the index. If not specified a StandardAnalyzer will be used.
 * @returns The index instance.
 * @type helma.Search.Index
 */
helma.Search.prototype.createIndex = function(dir, analyzer) {
    if (!dir || !(dir instanceof Packages.org.apache.lucene.store.Directory)) {
        throw("Index directory missing or invalid.");
    } else if (!analyzer) {
        // no analyzer given, use a StandardAnalyzer
        analyzer = helma.Search.getAnalyzer();
    }
    var index = new helma.Search.Index(dir, analyzer);
    if (!Packages.org.apache.lucene.index.IndexReader.indexExists(dir)) {
        index.create();
    }
    return index;
};


/*********************
 ***** I N D E X *****
 *********************/


/**
 * Creates a new instance of helma.Search.Index
 * @class Instances of this class represent a Lucene search index
 * located in either a directory on disk or in RAM. This class
 * provides various methods for modifying the underlying Lucene index.
 * @param {org.apache.lucene.store.Directory} directory The directory
 * where the Lucene index is located at.
 * @param {org.apache.lucene.analysis.Analyzer} analyzer The analyzer
 * to use when modifying the index.
 * @constructor
 */
helma.Search.Index = function(directory, analyzer) {

    /**
     * Tries to create a new IndexWriter or IndexModifier for up to the
     * number of milliseconds defined in helma.Search.Index.LOCK_TIMEOUT.
     * @private
     */
    var getWriterModifier = function(what, create) {
        var waitFor, elapsed = 0;
        while (elapsed < helma.Search.Index.LOCK_TIMEOUT) {
            if (!this.isLocked()) {
                return new Packages.org.apache.lucene.index[what](directory,
                                   analyzer, create == true ? true : false);
            } else {
                waitFor = Math.round(Math.random() * 100) + 50;
                java.lang.Thread.sleep(waitFor);
                elapsed += waitFor;
                app.logger.debug("[Thread " + java.lang.Thread.currentThread().getId()
                                 + "] waiting for " + elapsed + "ms");
            }
        }
        throw("Failed to create " + what + " due to active lock (Thread "
              + java.lang.Thread.currentThread().getId() + ")");
    };

    /**
     * Returns an IndexModifier instance for adding or deleting
     * documents to resp. from the underlying index. If the
     * index is currently locked this method will try for the next
     * two seconds to create the IndexModifier, otherwise it will
     * throw an error.
     * @returns An IndexModifier instance.
     * @type org.apache.lucene.index.IndexModifier
     */
    this.getModifier = function(create) {
        return getWriterModifier.call(this, "IndexModifier", create);
    };

    /**
     * Returns an IndexWriter instance that can be used to add documents to
     * the underlying index or to perform various other modifying operations.
     * If the index is currently locked this method will try for the next
     * two seconds to create the IndexWriter, otherwise it will
     * throw an error.
     * @returns An IndexWriter instance.
     * @type org.apache.lucene.index.IndexWriter
     */
    this.getWriter = function(create) {
        return getWriterModifier.call(this, "IndexWriter", create);
    };

    /**
     * Returns an IndexReader instance. Due to locking issues an
     * IndexModifier should be used for deleting documents.
     * @returns An IndexReader instance
     * @type org.apache.lucene.index.IndexReader
     */
    this.getReader = function() {
        return Packages.org.apache.lucene.index.IndexReader.open(directory);
    };

    /**
     * Returns the directory the underlying Lucene index is located at.
     * @returns The directory of this index
     * @type org.apache.lucene.store.Directory
     */
    this.getDirectory = function() {
        return directory;
    };

    /**
     * Returns the analyzer used within this index.
     * @returns The analyzer used within this index.
     * @type org.apache.lucene.analysis.Analyer
     */
    this.getAnalyzer = function() {
        return analyzer;
    };
    
    /**
     * Returns a searcher for querying this index.
     * @returns A searcher useable for querying the index.
     * @type helma.Search.Searcher
     */
    this.getSearcher = function() {
        return new helma.Search.Searcher(this);
    };
    
    /** @ignore */
    this.toString = function() {
        return ("[Lucene Index " + directory + "]");
    };

    return this;
};

/**
 * Constant containing the number of milliseconds
 * the index will try to create an IndexModifier or IndexWriter
 * in case it is currently locked.
 * @type Number
 */
helma.Search.Index.LOCK_TIMEOUT = 2000;

/**
 * Merges the indexes passed as argument into this one.
 * @param {org.apache.lucene.store.Directory} dir At least one
 * index director to add to this index.
 */
helma.Search.Index.prototype.addIndexes = function(dir /* [, dir[, dir...] */) {
    var dirs = java.lang.reflect.Array.newInstance(helma.Search.PKG.store.Directory,
  	                                               arguments.length);
    for (var i=0;i<arguments.length;i++) {
        dirs[i] = arguments[i];
    }
    try {
        var writer = this.getWriter();
        writer.addIndexes(dirs);
    } finally {
        if (writer != null) {
            writer.close();
        }
    }
    return;
};

/**
 * Creates a new index. This will delete any existing index
 * files in the directory of this index.
 * @returns True if creating the index was successful, false otherwise
 * @type Boolean
 */
helma.Search.Index.prototype.create = function() {
    try {
        // FIXME: IndexWriter is supposed to remove files
        // if instantiated with create == true, but for some reason
        // it doesn't, so instead use FSDirectory.getDirectory()
        // for deletion and then re-create the index segments file
        var dir = this.getDirectory().getFile();
        Packages.org.apache.lucene.store.FSDirectory.getDirectory(dir, true);
        var writer = this.getWriter(true);
        return true;
    } catch (e) {
        app.logger.warn("Unable to create the index " + this.getDirectory());
        return false;
    } finally {
        if (writer != null) {
            writer.close();
        }
    }
};

/**
 * Returns the number of documents in this index.
 * @returns The number of documents in this index.
 * @type Number
 */
helma.Search.Index.prototype.size = function() {
    try {
        var reader = this.getReader();
        return reader.numDocs();
    } finally {
        if (reader != null) {
            reader.close();
        }
    }
    return;
};

/**
 * Optimizes the underlying index.
 */
helma.Search.Index.prototype.optimize = function() {
    try {
        var writer = this.getWriter();
        writer.optimize();
    } finally {
        if (writer != null) {
            writer.close();
        }
    }
    return;
};

/**
 * Returns an array containing all field names in this index.
 * @returns An array with the field names in this index.
 * @type Array
 */
helma.Search.Index.prototype.getFieldNames = function() {
    try {
        var reader = this.getReader();
        var coll = reader.getFieldNames().toArray();
        // convert java array into javascript array
        var result = [];
        for (var i in coll) {
            result[i] = coll[i];
        }
        return result;
    } finally {
        if (reader != null) {
            reader.close();
        }
    }
    return;
};

/**
 * Checks if the index is locked.
 * @returns True if the underlying index is locked,
 * false otherwise.
 * @type Boolean
 */
helma.Search.Index.prototype.isLocked = function() {
    return Packages.org.apache.lucene.index.IndexReader.isLocked(this.getDirectory());
};

/**
 * Unlocks the index. Use this with caution, as it removes
 * any active locks in the Lucene index, which might lead
 * to index corruption.
 */
helma.Search.Index.prototype.unlock = function() {
    Packages.org.apache.lucene.index.IndexReader.unlock(this.getDirectory());
    return;
};

/**
 * Closes the underlying index directory for future operations.
 */
helma.Search.Index.prototype.close = function() {
    this.getDirectory().close();
    return;
};

/**
 * Adds a document to the index.
 * @param {helma.Search.Document} doc The document to add to the index.
 */
helma.Search.Index.prototype.addDocument = function(doc) {
    try {
        var modifier = this.getModifier();
        modifier.addDocument(doc.getDocument());
    } finally {
        if (modifier != null) {
            modifier.close();
        }
    }
    return;
};

/**
 * Adds all documents in the passed collection to this index.
 * @param {java.util.Hashtable|java.util.Vector|Array} docs
 * The documents to add to the index.
 */
helma.Search.Index.prototype.addDocuments = function(docs, mergeFactor) {
    try {
        var modifier = this.getModifier();
        if (mergeFactor) {
            modifier.setMergeFactor(mergeFactor);
        }
        if (docs instanceof java.util.Hashtable || docs instanceof java.util.Vector) {
            var e = docs.elements();
            while (e.hasMoreElements()) {
                modifier.addDocument(e.nextElement().getDocument());
            }
        } else if (doc instanceof Array) {
            for (var i=0;i<docs.length;i++) {
                modifier.addDocument(docs[i]);
            }
        }
    } finally {
        if (modifier != null) {
            modifier.close();
        }
    }
    return;
};

/**
 * Remove those documents from the index whose field-value
 * with the given name matches the passed value argument.
 * @param {String} fieldName The name of the field
 * @param {String} fieldValue The value of the field.
 * @returns The number of documents that have been deleted.
 * @type Number
 */
helma.Search.Index.prototype.removeDocument = function(fieldName, fieldValue) {
    try {
        var modifier = this.getModifier();
        var term;
        term = new Packages.org.apache.lucene.index.Term(fieldName, fieldValue);
        return modifier.deleteDocuments(term);
    } finally {
        if (modifier != null) {
            modifier.close();
        }
    }
    return;
};

/**
 * Removes all documents whose field with the given name matches
 * the values passed as argument.
 * @param {String} fieldName The name of the field
 * @param {java.util.Hashtable|java.util.Vector|Array} values
 * The values that define the documents that should be removed from
 * the index.
 * @returns An array containing the numbers of deleted documents
 * for each field value.
 * @type Array
 */
helma.Search.Index.prototype.removeDocuments = function(fieldName, values) {
    try {
        var modifier = this.getModifier();
        var term, result = [];
        if (values instanceof java.util.Hashtable ||
                values instanceof java.util.Vector) {
            var e = values.elements();
            while (e.hasMoreElements()) {
                term = new Packages.org.apache.lucene.index.Term(fieldName, e.nextElement());
                result.push(modifier.deleteDocuments(term));
            }
            return result;
        } else if (values instanceof Array) {
            for (var i=0;i<values.length;i++) {
                term = new Packages.org.apache.lucene.index.Term(fieldName, values[i]);
                result.push(modifier.deleteDocuments(term));
            }
        }
    } finally {
        if (modifier != null) {
            modifier.close();
        }
    }
    return;
};

/**
 * Updates the index with the document passed as argument. In contrast
 * to addDocument() this removes any existing objects whose fieldName
 * matches the one of the document object. Eg. if the document object
 * has a field "Id" with the value "123", all document objects whose
 * fieldName "Id" matches "123" will be removed from the index before.
 * @param {helma.Search.Document} docObj Document object to add to index.
 * @param {String} fieldName The name of the identifier field.
 */
helma.Search.Index.prototype.updateDocument = function(docObj, fieldName) {
    try {
        var modifier = this.getModifier();
        var doc = docObj.getDocument();
        var term = new Packages.org.apache.lucene.index.Term(fieldName, doc.get(fieldName));
        modifier.deleteDocuments(term);
        modifier.addDocument(doc);
    } finally {
        if (modifier != null) {
            modifier.close();
        }
    }
    return;
};

/**
 * Creates a new instance of helma.Search.Seacher
 * @class This class provides basic functionality for
 * searching an index.
 * @param {helma.Search.Index} index The index to search in.
 * @returns A newly created index searcher
 * @constructor
 */
helma.Search.Searcher = function(index) {
    var searcher = new Packages.org.apache.lucene.search.IndexSearcher(index.getDirectory());

    /**
     * The search results.
     * @type helma.Search.HitCollection
     */
    this.hits = null;

    /**
     * A vector with SortField instances, if any have been defined.
     * @type java.util.Vector
     * @see #sortBy
     */
    this.sortFields = null;

    /**
     * Returns the wrapped IndexSearcher instance.
     * @type org.apache.lucene.search.IndexSearcher
     */
    this.getSearcher = function() {
        return searcher;
    };

    return this;
};

/** @ignore */
helma.Search.Searcher.prototype.toString = function() {
    return "[Index Searcher]";
};

/**
 * Searches an index using the query passed as argument.
 * The resulting collection of hits is stored in the property "hits"
 * of this Searcher instance. Don't forget to close the searcher
 * when finished processing its hits.
 * @param {helma.Search.Query} query The query to use for searching
 * @param {helma.Search.QueryFilter} filter An optional query filter
 * for filtering the results.
 * @returns The number of hits.
 * @type Number
 */
helma.Search.Searcher.prototype.search = function(query, filter) {
    var PKG = Packages.org.apache.lucene.search;
    var hits;
    if (this.sortFields != null && this.sortFields.size() > 0) {
        var arr = java.lang.reflect.Array.newInstance(PKG.SortField, this.sortFields.size());
        var sort = PKG.Sort(this.sortFields.toArray(arr));
        if (filter) {
            hits = this.getSearcher().search(query.getQuery(), filter.getFilter(), sort);
        } else {
            hits = this.getSearcher().search(query.getQuery(), sort);
        }
    } else if (filter) {
        hits = this.getSearcher().search(query.getQuery(), filter.getFilter());
    } else {
        hits = this.getSearcher().search(query.getQuery());
    }
    this.hits = new helma.Search.HitCollection(hits);
    return this.hits.length();
};

/**
 * Sets a field as result sorting field. This method can be called
 * with a different number of arguments:
 * sortBy(fieldName)
 * sortBy(fieldName, type)
 * sortBy(fieldName, reverse)
 * sortBy(fieldName, type, reverse)
 * @param {String} fieldName The name of the field in the index by which
 * the search result should be ordered.
 * @param {String} type The type of the field defined by argument fieldName.
 * Valid arguments are "string", "float", "int", "score", "doc", "auto", "custom".
 * Default is "auto". See http://lucene.apache.org/java/docs/api/org/apache/lucene/search/SortField.html
 * for an explanation.
 */
helma.Search.Searcher.prototype.sortBy = function(fieldName /** type, reverse */) {
    var PKG = Packages.org.apache.lucene.search;
    if (!this.sortFields)
        this.sortFields = new java.util.Vector();
    var f = fieldName;
    var t = PKG.SortField.AUTO;
    var r = false;
    if (arguments.length == 3) {
        t = PKG.SortField[arguments[1].toUpperCase()];
        r = arguments[2];
    } else if (arguments.length == 2) {
        if (arguments[1].constructor == Boolean) {
            r = arguments[1];
        } else {
            t = PKG.SortField[arguments[1].toUpperCase()];
        }
    }
    this.sortFields.add(new PKG.SortField(f, t, r));
    return;
};

/**
 * Closes the wrapped IndexSearcher instance.
 */
helma.Search.Searcher.prototype.close = function() {
    var s = this.getSearcher();
    if (s != null) {
        s.close();
    }
    return;
};

/**
 * Creates a new instance of helma.Search.HitCollection
 * @class This class provides Helma-like methods for accessing
 * a collection of search hits.
 * @param {org.lucene.search.Hits} hits The hit collection returned
 * by lucene.
 * @constructor
 */
helma.Search.HitCollection = function(hits) {
    /**
     * Silently converts the hit at the given index position into
     * an instance of helma.Search.Document.
     * @param {Number} idx The index position of the hit
     * @returns The document object at the given index position
     * @type helma.Search.Document
     */
    this.get = function(idx) {
        var doc = new helma.Search.Document(hits.doc(idx));
        doc.score = hits.score(idx);
        doc.rank = idx +1;
        return doc;
    };

    /**
     * Returns the number of hits in this collection.
     * @returns The number of hits.
     * @type Number
     */
    this.size = function() {
        return (hits != null) ? hits.length() : 0;
    };

    /**
     * Returns the number of hits in this collection
     * @returns The number of hits.
     * @type Number
     * @deprecated
     * @see #size
     */

    return this;
};


/***********************************************
 ***** Q U E R Y   C O N S T R U C T O R S *****
 ***********************************************/

/**
 * Creates a new instance of helma.Search.Query
 * @class Base class for the various query constructors. Don't
 * call this directly, as it provides just basic methods used
 * in all of its extends.
 * @constructor
 */
helma.Search.Query = function() {
    /**
     * The wrapped query instance
     * @type org.apache.lucene.search.Query
     * @private
     */
    this.query = null;

    return this;
};

/**
 * Returns the wrapped Lucene Query object.
 * @returns The wrapped query object
 * @type org.apache.lucene.search.Query
 */
helma.Search.Query.prototype.getQuery = function() {
    return this.query;
};

/** @ignore */
helma.Search.Query.prototype.toString = function(field) {
    return "[" + this.getQuery().toString(field) + "]";
};

/**
 * Returns the boost factor of this query.
 * @type Number
 */
helma.Search.Query.prototype.getBoost = function() {
    return this.getQuery().getBoost();
};

/**
 * Sets the boost factor of this query clause to
 * the given number. Documents matching this query
 * will have their score multiplied with the given
 * factor
 * @param {Number} fact The factor to multiply the score
 * of matching documents with.
 */
helma.Search.Query.prototype.setBoost = function(fact) {
    this.getQuery().setBoost(fact);
    return;
};

/**
 * Creates a new instance of helma.Search.TermQuery
 * @class This class represents a simple Term Query.
 * @param {String} field The name of the field
 * @param {String} str The value of the field
 * @constructor
 * @extends helma.Search.Query
 */
helma.Search.TermQuery = function(field, str) {
    var t = new Packages.org.apache.lucene.index.Term(field, str);
    /**
     * Contains the wrapped TermQuery instance
     * @type org.apache.lucene.search.TermQuery
     */
    this.query = new Packages.org.apache.lucene.search.TermQuery(t);
    return this;
};
helma.Search.TermQuery.prototype = new helma.Search.Query;

/**
 * Creates a new instance of helma.Search.BooleanQuery
 * @class This class represents a Boolean Query, providing
 * various methods for combining other Query instances using boolean operators.
 * @param String name of the field
 * @param String query string
 * @returns Object BooleanQuery object
 * @constructor
 * @extends helma.Search.Query
 */
helma.Search.BooleanQuery = function(field, str, clause, analyzer) {
    /**
     * Contains the wrapped BooleanQuery instance
     * @type org.apache.lucene.search.BooleanQuery
     */
    this.query = new Packages.org.apache.lucene.search.BooleanQuery();

    /**
     * Main constructor body
     */
    if (field && str) {
        this.addTerm.apply(this, arguments);
    }

    return this;
};
helma.Search.BooleanQuery.prototype = new helma.Search.Query;

/**
 * Adds a term to the wrapped query object. This method can be called
 * with two, three or four arguments, eg.:
 * <pre>addTerm("fieldname", "querystring")
 * addTerm("fieldname", "querystring", "and")
 * addTerm("fieldname", "querystring", helma.Search.getAnalyzer("de"))
 * addTerm("fieldname", "querystring", "not", helma.Search.getAnalyzer("simple"))</pre>
 * @param {String|Array} field Either a String or an Array containing Strings
 * that determine the index field(s) to match
 * @param {String} str Query string to match
 * @param {String} clause Boolean clause ("or", "not" or "and", default is "and")
 * @param {org.apache.lucene.analysis.Analyzer} analyzer An analyzer to use
 */
helma.Search.BooleanQuery.prototype.addTerm = function(field, str, clause, analyzer) {
    if (arguments.length == 3 && arguments[2] instanceof Packages.org.apache.lucene.analysis.Analyzer) {
        analyzer = arguments[2];
        clause = "or";
    }
    if (!analyzer) {
        analyzer = helma.Search.getAnalyzer();
    }

    var q;
    if (field instanceof Array) {
        q = Packages.org.apache.lucene.queryParser.MultiFieldQueryParser.parse(str, field, analyzer);
    } else {
        q = Packages.org.apache.lucene.queryParser.QueryParser.parse(str, field, analyzer);
    }
    switch (clause) {
        case "or":
            this.getQuery().add(q, false, false);
            break;
        case "not":
            this.getQuery().add(q, false, true);
            break;
        default:
            this.getQuery().add(q, true, false);
    }
    return;
};

/**
 * Adds an additional query clause to this query.
 * @param {helma.Search.Query} q The query to add
 * @param {String} clause Boolean clause ("or", "not", or "and", default is "and")
 */
helma.Search.BooleanQuery.prototype.addQuery = function(q, clause) {
    switch (clause) {
        case "or":
            this.getQuery().add(q.getQuery(), false, false);
            break;
        case "not":
            this.getQuery().add(q.getQuery(), false, true);
            break;
        default:
            this.getQuery().add(q.getQuery(), true, false);
    }
    return;
};

/**
 * Constructs a new helma.Search.PhraseQuery instance that wraps
 * a Lucene Phrase Query object.
 * @class Instances of this class represent a phrase query.
 * @param {String} field The name of the field
 * @param {String} str The phrase query string
 * @returns A newly created PhraseQuery instance
 * @constructor
 * @extends helma.Search.Query
 */
helma.Search.PhraseQuery = function(field, str) {
    /**
     * Contains the wrapped PhraseQuery instance
     * @type org.apache.lucene.search.PhraseQuery
     */
    this.query = new Packages.org.apache.lucene.search.PhraseQuery();

    /**
     * add a term to the end of the phrase query
     */
    this.addTerm = function(field, str) {
        var t = new Packages.org.apache.lucene.index.Term(field, str);
        this.query.add(t);
        return;
    };

    if (field && str)
        this.addTerm(field, str);
    delete this.base;
    return this;
};
helma.Search.PhraseQuery.prototype = new helma.Search.Query;

/**
 * Constructs a new helma.Search.RangeQuery instance.
 * @class Instances of this class represent a range
 * query, wrapping a Lucene RangeQuery instance.
 * @param {String} field The name of the field
 * @param {String} from The minimum value to match (can be null)
 * @param {String} to The maximum value to match (can be null)
 * @param {Boolean} inclusive If true the given min/max values are included
 * @returns A newly created RangeQuery instance
 * @constructor
 * @extends helma.Search.Query
 */
helma.Search.RangeQuery = function(field, from, to, inclusive) {
    if (!field)
        throw("Missing field name in RangeQuery()");
    if (arguments.length < 4)
        inclusive = true;
    var t1 = from ? new Packages.org.apache.lucene.index.Term(field, from) : null;
    var t2 = to ? new Packages.org.apache.lucene.index.Term(field, to) : null;
    /**
     * Contains the wrapped RangeQuery instance
     * @type org.apache.lucene.search.RangeQuery
     */
    this.query = new Packages.org.apache.lucene.search.RangeQuery(t1, t2, inclusive);
    return this;
};
helma.Search.RangeQuery.prototype = new helma.Search.Query;

/**
 * Constructs a new helma.Search.FuzzyQuery instance.
 * @class Instances of this class represent a fuzzy query
 * @param {String} field The name of the field
 * @param {String} str The query string to match
 * @returns A newly created FuzzyQuery instance
 * @constructor
 * @extends helma.Search.Query
 */
helma.Search.FuzzyQuery = function(field, str) {
    var t = new Packages.org.apache.lucene.index.Term(field, str);
    /**
     * Contains the wrapped FuzzyQuery instance
     * @type org.apache.lucene.search.FuzzyQuery
     */
    this.query = new Packages.org.apache.lucene.search.FuzzyQuery(t);
    return this;
};
helma.Search.FuzzyQuery.prototype = new helma.Search.Query;

/**
 * Constructs a new helma.Search.PrefixQuery instance.
 * @class Instances of this class represent a prefix query
 * @param {String} field The name of the field
 * @param {String} str The query string to match
 * @returns A newly created PrefixQuery instance
 * @constructor
 * @extends helma.Search.Query
 */
helma.Search.PrefixQuery = function(field, str) {
    var t = new Packages.org.apache.lucene.index.Term(field, str);
    /**
     * Contains the wrapped PrefixQuery instance
     * @type org.apache.lucene.search.PrefixQuery
     */
    this.query = new Packages.org.apache.lucene.search.PrefixQuery(t);
    return this;
};
helma.Search.PrefixQuery.prototype = new helma.Search.Query;

/**
 * Constructs a new helma.Search.WildcardQuery instance.
 * @class Instances of this class represent a wildcard query.
 * @param {String} field The name of the field
 * @param {String} str The query string to match
 * @returns A newly created WildcardQuery instance
 * @constructor
 * @extends helma.Search.Query
 */
helma.Search.WildcardQuery = function(field, str) {
    var t = new Packages.org.apache.lucene.index.Term(field, str);
    /**
     * Contains the wrapped WildcardQuery instance
     * @type org.apache.lucene.search.WildcardQuery
     */
    this.query = new Packages.org.apache.lucene.search.WildcardQuery(t);
    return this;
};
helma.Search.WildcardQuery.prototype = new helma.Search.Query;


/***************************
 ***** D O C U M E N T *****
 ***************************/


/**
 * Creates a new instance of helma.Search.Document.
 * @class Instances of this class represent a single
 * index document. This class provides various methods for
 * adding content to such documents.
 * @param {org.apache.lucene.document.Document} document Optional Lucene Document object
 * that should be wrapped by this Document instance.
 * @constructor
 */
helma.Search.Document = function(document) {
    if (!document)
        document = new Packages.org.apache.lucene.document.Document();

    /**
     * Returns the wrapped Lucene Document object
     * @returns The wrapped Document object
     * @type org.apache.lucene.document.Document
     */
    this.getDocument = function() {
        return document;
    };

    return this;
};

/**
 * Adds a field to this document.
 * @param {String} name The name of the field
 * @param {String} value The value of the field
 * @param {Object} param Optional parameter object containing the following properties:
 * <ul>
 * <li><code>store</code> (Boolean) defaults to true</li>
 * <li><code>index</code> (Boolean) defaults to true</li>
 * <li><code>tokenize</code> (Boolean) defaults to true</li>
 * </ul>
 */
helma.Search.Document.prototype.addField = function(name, value, param) {
    if (!param) {
        param = {};
    }
    if (value != null) {
        var pkg = Packages.org.apache.lucene.document;
        if (value.constructor == Date) {
            // Convert the value 
            value = pkg.DateTools.timeToString(value.getTime(),
                            pkg.DateTools.Resolution.MINUTE);
        } else if (value.constructor != String) {
            value = String(value);
        }
        var f = new pkg.Field(String(name),
                              value,
                              typeof(param.store) == "boolean" ? param.store : true,
                              typeof(param.index) == "boolean" ? param.index : true,
                              typeof(param.tokenize) == "boolean" ? param.tokenize : true);
        this.getDocument().add(f);
    }
    return;
};

/**
 * Returns a single document field containing a value other than
 * a date object, for which you should use {@link #getDateField}.
 * @param {String} name The name of the field in this document object.
 * @returns An object containing the following properties:
 * <ul>
 * <li><code>name</code> (String) The name of the field</li>
 * <li><code>boost</code> (Int) The boost factor</li>
 * <li><code>indexed</code> (Boolean) True if the field is indexed, false otherwise</li>
 * <li><code>stored</code> (Boolean) True if the field is stored, false otherwise</li>
 * <li><code>tokenized</code> (Boolean) True if the field is tokenized, false otherwise</li>
 * <li><code>value</code> (String) The value of the field</li>
 * </ul>
 * @type Object
 */
helma.Search.Document.prototype.getField = function(name) {
    var f = this.getDocument().getField(name);
    if (f != null) {
        var result = {name: name,
                      boost: f.getBoost(),
                      indexed: f.isIndexed(),
                      stored: f.isStored(),
                      tokenized: f.isTokenized(),
                      value: f.stringValue()};
        return result;
   }
   return null;
};

/**
 * Returns the value of a single document field converted into a
 * Date object. For retrieving a value without this conversion
 * see {@link #getField}.
 * @param {String} name The name of the field in this document object.
 * @returns An object containing the following properties:
 * <ul>
 * <li><code>name</code> (String) The name of the field</li>
 * <li><code>boost</code> (Int) The boost factor</li>
 * <li><code>indexed</code> (Boolean) True if the field is indexed, false otherwise</li>
 * <li><code>stored</code> (Boolean) True if the field is stored, false otherwise</li>
 * <li><code>tokenized</code> (Boolean) True if the field is tokenized, false otherwise</li>
 * <li><code>value</code> (String) The value of the field as Date</li>
 * </ul>
 * @type Object
 * @see #getField
 */
helma.Search.Document.prototype.getDateField = function(name) {
    var result = this.getField(name);
    if (result != null) {
        // convert value to Date object
        result.value = Packages.org.apache.lucene.document.DateTools.stringToDate(result.value);
    }
    return result;
};

/**
 * Returns the fields of a document object.
 * @returns An array containing all fields in this document object.
 * @type Array
 */
helma.Search.Document.prototype.getFields = function() {
    var e = this.getDocument().fields();
    var result = [];
    while (e.hasMoreElements()) {
        result.push(this.getField(e.nextElement().name()));
    }
    return result;
};

/**
 * Returns the boost factor of a document.
 * @returns The boost factor of a document
 * @type Number
 */
helma.Search.Document.prototype.getBoost = function() {
    return this.getDocument().getBoost();
};

/**
 * Sets the boost factor of a document.
 * @param {Number} boost The boost factor of the document
 */
helma.Search.Document.prototype.setBoost = function(boost) {
    this.getDocument().setBoost(boost);
    return;
};

/** @ignore */
helma.Search.Document.prototype.toString = function() {
    return "[Document Object]";
};


helma.lib = "Search";
helma.dontEnum(helma.lib);
for (var i in helma[helma.lib])
    helma[helma.lib].dontEnum(i);
for (var i in helma[helma.lib].prototype)
    helma[helma.lib].prototype.dontEnum(i);
delete helma.lib;
