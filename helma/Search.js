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
// $Revision: 1.2 $
// $Author: czv $
// $Date: 2006/04/24 07:02:17 $
//


// take care of any dependencies
app.addRepository('modules/helma/lucene.jar');


if (!global.helma) {
    global.helma = {};
}

/**
 * Search constructor
 */
helma.Search = function() {

    this.toString = function() {
        return helma.Search.toString();
    };

    try {
        var c = java.lang.Class.forName("org.apache.lucene.analysis.Analyzer",
                                        true, app.getClassLoader());
    } catch (e) {
        throw("helma.Search needs lucene.jar \
               in lib/ext or application directory \
               [http://lucene.apache.org/]");
    }
    return this;
}

helma.Search.PKG = Packages.org.apache.lucene;

///////////////////////////////////////////
// static methods
///////////////////////////////////////////

helma.Search.toString = function() {
    return "[helma.Search]";
}

/**
 * static method that returns a new Analyzer instance
 * depending on the language passed as argument
 * @param String language
 * @return Object analyzer
 */
helma.Search.getAnalyzer = function(lang) {
    switch (lang) {
        case "de":
            return new helma.Search.PKG.analysis.de.GermanAnalyzer();
        case "ru":
            return new helma.Search.PKG.analysis.ru.RussianAnalyzer();
        case "si":
        case "simple":
            return new helma.Search.PKG.analysis.SimpleAnalyzer();
        case "whitespace":
            return new helma.Search.PKG.analysis.WhitespaceAnalyzer();
        default:
            return new helma.Search.PKG.analysis.standard.StandardAnalyzer();
    }
};

/**
 * constructor for QueryFilter objects
 * @param Object instance of Search.Query
 */
helma.Search.QueryFilter = function(q) {
    var wrappedFilter = new helma.Search.PKG.search.QueryFilter(q.getQuery());

    this.getFilter = function() {
        return wrappedFilter;
    };

    this.toString = function() {
        return wrappedFilter.toString();
    };

    return this;
};


///////////////////////////////////////////
// prototype methods
///////////////////////////////////////////

/**
 * returns an instance of org.apache.lucene.store.FSDirectory
 * @param Object an instance of File, helma.File, java.io.File or the
 * path to the directory as String
 * @return Object org.apache.lucene.store.FSDirectory
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
    return helma.Search.PKG.store.FSDirectory.getDirectory(d,
               create == true ? true : !d.exists());
}

/**
 * returns an instance of org.apache.lucene.store.RAMDirectory
 * @param Object the directory to create the RAMDirectory from. Argument
 *               can be an instance of File, helma.File, java.io.File or the
 *               path to the directory as String
 * @return Object org.apache.lucene.store.RAMDirectory
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
        return helma.Search.PKG.store.RAMDirectory(d);
    }
    return helma.Search.PKG.store.RAMDirectory();
}

/**
 * returns a new index Object
 * @param Object directory containing the index (an instance of either
 * org.apache.lucene.store.FSDirectory or org.apache.lucene.store.RAMDirectory)
 * @param Object (optional) Analyzer to use (instance of org.apache.lucene.analysis.Analyzer)
 * @param Boolean (optional) Flag to force index creation
 * @return Object Index object
 */
helma.Search.prototype.createIndex = function(dir, analyzer, forceCreate) {
    if (arguments.length == 0) {
        throw("helma.Search.createIndex(): insufficient arguments.");
    } else if (arguments.length == 1) {
        analyzer = helma.Search.getAnalyzer();
        forceCreate = false;
    } else if (arguments.length == 2) {
        if (arguments[1].constructor == Boolean) {
            analyzer = helma.Search.getAnalyzer();
            forceCreate = arguments[1];
        } else {
            forceCreate = false;
        }
    }
    var index = new helma.Search.Index(dir, analyzer);
    if (!helma.Search.PKG.index.IndexReader.indexExists(dir) || forceCreate == true) {
        index.create();
    }
    return index;
};


///////////////////////////////////////////
// Index
///////////////////////////////////////////


/**
 * static constructor for Index objects
 * @param Object index directory, either an instance of FSDirectory or RAMDirectory
 * @param Object Lucene analyzer object
 */
helma.Search.Index = function(directory, analyzer) {
    /**
     * returns an IndexWriter object
     * @param Boolean if true a new index is created
     */
    this.getWriter = function(create) {
        return new helma.Search.PKG.index.IndexWriter(directory,
                       analyzer, create == true ? true : false);
    };

    /**
     * returns an IndexReader object
     */
    this.getReader = function() {
        return helma.Search.PKG.index.IndexReader.open(directory);
    };

    /**
     * return the directory of the index
     * @param Object helma.File object representing the index' directory
     */
    this.getDirectory = function() {
        return directory;
    };

    /**
     * constructor function for Searcher objects
     */
    this.Searcher = function() {
        var s = new helma.Search.PKG.search.IndexSearcher(directory);
        var sortFields;

        /**
         * wraps a org.lucene.search.Hits collection
         * @param Object instance of org.lucene.search.Hits
         */
        var HitCollection = function(hits) {
            /**
             * silently converts the requested hit into
             * an instance of helma.Search.Document
             * @param Int index position of hit in collection
             * @return instance of helma.Search.Document
             */
            this.get = function(idx) {
                var doc = new helma.Search.Document(hits.doc(idx));
                doc.score = hits.score(idx);
                doc.rank = idx +1;
                return doc;
            }

            /**
             * returns the nr. of hits
             */
            this.size = this.length = function() {
                return (hits != null) ? hits.length() : 0;
            }
            return this;
        }

        this.hits = null;

        /**
         * main search method. the resulting hits collection is
         * stored in the property hits. don't forget to do a close()
         * when finished processing the resulting hits, otherwise
         * the index files will stay locked!
         * @param Object instance of Search.Query
         * @param Object instance of QueryFilter
         * @return Int number of hits
         */
        this.search = function(query, filter) {
            var pkg = helma.Search.PKG.search;
            var hits;
            if (sortFields) {
                var arr = java.lang.reflect.Array.newInstance(pkg.SortField, sortFields.size());
                var sort = pkg.Sort(sortFields.toArray(arr));
                if (filter) {
                    hits = s.search(query.getQuery(), filter.getFilter(), sort);
                } else {
                    hits = s.search(query.getQuery(), sort);
                }
            } else if (filter) {
                hits = s.search(query.getQuery(), filter.getFilter());
            } else {
                hits = s.search(query.getQuery());
            }
            this.hits = new HitCollection(hits);
            return this.hits.length();
        };

        /**
         * sets a field as result sorting field
         */
        this.sortBy = function(field) {
            var pkg = helma.Search.PKG.search;
            if (!sortFields)
                sortFields = new java.util.Vector();
            var f = field;
            var t = pkg.SortField.AUTO;
            var r = false;
            if (arguments.length == 3) {
                t = pkg.SortField[arguments[1].toUpperCase()];
                r = arguments[2];
            } else if (arguments.length == 2) {
                if (arguments[1].constructor == Boolean) {
                    r = arguments[1];
                } else {
                    t = pkg.SortField[arguments[1].toUpperCase()];
                }
            }
            sortFields.add(new pkg.SortField(f, t, r));
            return;
        };

        /**
         * closes the wrapped IndexSearcher
         */
        this.close = function() {
            s.close();
            return;
        };

        return this;
    };

    this.toString = function() {
        return ("[Lucene Index " + directory + "]");
    };

    return this;
};

/**
 * check if the index is locked. if true wait a bit
 * and return it again until the timeout is reached
 */
helma.Search.Index.prototype.checkWriteLock = function() {
    var interval = 500;
    var timeout = 5000;
    var isLocked = helma.Search.PKG.index.IndexReader.isLocked(this.getDirectory());
    if (isLocked) {
        var elapsed = 0;
        while (elapsed < timeout) {
            java.lang.Thread.sleep(interval);
            elapsed += interval;
            isLocked = helma.Search.PKG.index.IndexReader.isLocked(this.getDirectory());
            if (!isLocked)
                return;
        }
        throw("Timeout while waiting for Index unlock");
    }
};

/**
 * merge indexes into this one
 */
helma.Search.Index.prototype.addIndexes = function(dirs) {
    var arr = java.lang.reflect.Array.newInstance(helma.Search.PKG.store.Directory,
                                                  dirs.length);
    for (var i=0;i<dirs.length;i++) {
        arr[i] = dirs[i];
    }
    try {
        var writer = this.getWriter();
        writer.addIndexes(arr);
    } finally {
        writer.close();
    }
    return;
};

/**
 * return the analyzer used within this index
 */
helma.Search.Index.prototype.getAnalyzer = function() {
    this.checkWriteLock();
    try {
        var writer = this.getWriter();
        return writer.getAnalyzer();
    } finally {
        writer.close();
    }
    return;
};

/**
 * create a new index
 */
helma.Search.Index.prototype.create = function() {
    try {
        var writer = this.getWriter(true);
        return true;
    } finally {
        writer.close();
    }
    return;
};

/**
 * clear the index by re-creating it
 */
helma.Search.Index.prototype.clear = function() {
    this.create();
    return true;
};

/**
 * return the number of documents in the index
 */
helma.Search.Index.prototype.size = function() {
    this.checkWriteLock();
    try {
        var writer = this.getWriter();
        var size = writer.docCount();
        return size;
    } finally {
        writer.close();
    }
    return;
}

/**
 * optimize the index
 */
helma.Search.Index.prototype.optimize = function() {
    this.checkWriteLock();
    try {
        var writer = this.getWriter();
        writer.optimize();
    } finally {
        writer.close();
    }
    return;
};

/**
 * return an array containing all field Names
 * indexed in this index
 * @return Object java Array
 */
helma.Search.Index.prototype.getFieldNames = function() {
    try {
        var reader = this.getReader();
        var coll = reader.getFieldNames();
        // FIXME: should return a JS Array, not a Java Array
        return coll.toArray();
    } finally {
        reader.close();
    }
    return;
};

/**
 * check if the index is locked
 * @return Boolean
 */
helma.Search.Index.prototype.isLocked = function() {
    try {
        var reader = this.getReader();
        return helma.Search.PKG.index.IndexReader.isLocked(reader.directory());
    } finally {
        reader.close();
    }
    return;
};

/**
 * unlock the index
 */
helma.Search.Index.prototype.unlock = function() {
    try {
        var reader = this.getReader();
        helma.Search.PKG.index.IndexReader.unlock(reader.directory());
        return true;
    } finally {
        reader.close();
    }
    return;
};

/**
 * return an Array containing all terms of an index
 * @param String field name (optional)
 * @param String field value (optional)
 * @return Object Array containing terms
 */
helma.Search.Index.prototype.getTerms = function(field, str) {
    try {
        var reader = this.getReader();
        var arr = [];
        var e;
        if (field && str) {
            e = reader.terms(new helma.Search.PKG.index.Term(field, str));
        } else {
            e = reader.terms();
        }
        while (e.next()) {
            arr.push(e.term());
        }
        e.close();
        return arr;
    } finally {
        reader.close();
    }
    return;
};

/**
 * close the directory of this index to
 * future operations
 */
helma.Search.Index.prototype.close = function() {
    this.checkWriteLock();
    try {
        var reader = this.getReader();
        var dir = reader.directory();
        dir.close();
    } finally {
        reader.close();
    }
    return;
};

/**
 * add a document object to the index
 * @param Object either a single Document object
 *                    or a Hashtable/Vector containing numerous
 *                    Document objects to add to the index
 */
helma.Search.Index.prototype.addDocument = function(d) {
    this.checkWriteLock();
    try {
        var writer = this.getWriter();
        if (d instanceof java.util.Hashtable || d instanceof java.util.Vector) {
            var e = d.elements();
            while (e.hasMoreElements())
                writer.addDocument(e.nextElement().getDocument());
        } else {
            writer.addDocument(d.getDocument());
        }
    } finally {
        writer.close();
    }
    return;
};

/**
 * remove those document(s) from the index whose
 * field-value matches the passed arguments
 * @param String Name of the field
 * @param Object either a single string value or a
 *                    Hashtable/Vector containing numerous
 *                    key values of Documents to remove from index
 */
helma.Search.Index.prototype.removeDocument = function(fieldName, fieldValue) {

    /**
     * private method that does the actual
     * removal in the index
     */
    var remove = function(name, value) {
        return reader["delete"](new helma.Search.PKG.index.Term(name, value));
    }

    this.checkWriteLock();
    try {
        var reader = this.getReader();
        if (fieldValue instanceof java.util.Hashtable || fieldValue instanceof java.util.Vector) {
            var result = [];
            var e = fieldValue.elements();
            while (e.hasMoreElements())
                result.push(remove(fieldName, e.nextElement()));
            return result;
        } else
            return remove(fieldName, fieldValue);
    } finally {
        reader.close();
    }
    return;
};

///////////////////////////////////////////
// Query constructors
///////////////////////////////////////////

/**
 * static constructor for Query objects
 * contains basic methods inherited by other types of Query objects
 */
helma.Search.QueryBase = function() {
    this.toString = function(field) {
        return "[" + this.getQuery().toString(field) + "]";
    };
    this.getBoost = function() {
        return this.getQuery().getBoost();
    };
    this.setBoost = function(fact) {
        this.getQuery().setBoost(fact);
        return;
    };
};

/**
 * Term Query constructor
 * @param String name of the field
 * @param String query string
 * @return Object TermQuery object
 */
helma.Search.TermQuery = function(field, str) {
    var t = new helma.Search.PKG.index.Term(field, str);
    var wrappedQuery = new helma.Search.PKG.search.TermQuery(t);

    /**
     * getter for wrapped java object
     */
    this.getQuery = function() {
        return wrappedQuery;
    };
    return this;
};
helma.Search.TermQuery.prototype = new helma.Search.QueryBase;

/**
 * Boolean Query constructor
 * @param String name of the field
 * @param String query string
 * @return Object BooleanQuery object
 */
helma.Search.BooleanQuery = function(field, str, clause, analyzer) {
    var wrappedQuery = new helma.Search.PKG.search.BooleanQuery();

    /**
     * getter for wrapped java object
     */
    this.getQuery = function() {
        return wrappedQuery;
    };

    /**
     * directly call addTerm if constructor was
     * called with arguments
     */
    if (field && str) {
        this.addTerm.apply(this, arguments);
    }

    return this;
};
helma.Search.BooleanQuery.prototype = new helma.Search.QueryBase;

/**
 * add a term to the wrappedQuery object. this method can be called
 * with two, three or four arguments, eg.:
 * addTerm("fieldname", "querystring")
 * addTerm("fieldname", "querystring", "and")
 * addTerm("fieldname", "querystring", helma.Search.getAnalyzer("de"))
 * addTerm("fieldname", "querystring", "not", helma.Search.getAnalyzer("simple"))
 *
 * @param Object either a String or an Array containing Strings
 *                    that determine the index field(s) to match
 * @param String string to match
 * @param String boolean clause ("or"|"not", default is "and")
 * @param Object instance of analysis.Analyzer
 */
helma.Search.BooleanQuery.prototype.addTerm = function(field, str, clause, analyzer) {
    if (arguments.length == 3 && arguments[2] instanceof helma.Search.PKG.analysis.Analyzer) {
        analyzer = arguments[2];
        clause = "or";
    }
    if (!analyzer)
        analyzer = helma.Search.getAnalyzer();

    var q;
    try {
        if (field instanceof Array)
            q = helma.Search.PKG.queryParser.MultiFieldQueryParser.parse(str, field, analyzer);
        else
            q = helma.Search.PKG.queryParser.QueryParser.parse(str, field, analyzer);
    } catch (e) {
        return;
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
 * "merge" a query object with a query object passed as
 * argument
 * @param Object Query object
 * @param String boolean clause ("or"|"not", default is "and")
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
 * Phrase Query constructor
 * @param String name of the field
 * @param String query string
 * @return Object PhraseQuery object
 */
helma.Search.PhraseQuery = function(field, str) {
    var wrappedQuery = new helma.Search.PKG.search.PhraseQuery();

    /**
     * add a term to the end of the phrase query
     */
    this.addTerm = function(field, str) {
        var t = new helma.Search.PKG.index.Term(field, str);
        wrappedQuery.add(t);
        return;
    };

    /**
     * getter for wrapped java object
     */
    this.getQuery = function() {
        return wrappedQuery;
    };

    if (field && str)
        this.addTerm(field, str);
    delete this.base;
    return this;
};
helma.Search.PhraseQuery.prototype = new helma.Search.QueryBase;

/**
 * Range Query constructor
 * @param String name of the field
 * @param String min value (can be null)
 * @param String max value (can be null)
 * @param Boolean if true min/max values are included
 * @return Obj JS wrapper object
 */
helma.Search.RangeQuery = function(field, from, to, inclusive) {
    if (!field)
        throw("Missing field name in RangeQuery()");
    if (arguments.length < 4)
        inclusive = true;
    var t1 = from ? new helma.Search.PKG.index.Term(field, from) : null;
    var t2 = to ? new helma.Search.PKG.index.Term(field, to) : null;
    var wrappedQuery = new helma.Search.PKG.search.RangeQuery(t1, t2, inclusive);

    /**
     * getter for wrapped java object
     */
    this.getQuery = function() {
        return wrappedQuery;
    };

    return this;
};
helma.Search.RangeQuery.prototype = new helma.Search.QueryBase;

/**
 * Fuzzy Query constructor
 * @param String name of the field
 * @param String query string
 * @return Object FuzzyQuery object
 */
helma.Search.FuzzyQuery = function(field, str) {
    var t = new helma.Search.PKG.index.Term(field, str);
    var wrappedQuery = new helma.Search.PKG.search.FuzzyQuery(t);

    /**
     * getter for wrapped java object
     */
    this.getQuery = function() {
        return wrappedQuery;
    };

    return this;
};
helma.Search.FuzzyQuery.prototype = new helma.Search.QueryBase;

/**
 * Prefix Query constructor
 * @param String name of the field
 * @param String query string
 * @return Object PrefixQuery object
 */
helma.Search.PrefixQuery = function(field, str) {
    var t = new helma.Search.PKG.index.Term(field, str);
    var wrappedQuery = new helma.Search.PKG.search.PrefixQuery(t);

    /**
     * getter for wrapped java object
     */
    this.getQuery = function() {
        return wrappedQuery;
    };

    return this;
};
helma.Search.PrefixQuery.prototype = new helma.Search.QueryBase;

/**
 * Wildcard Query constructor
 * @param String name of the field
 * @param String query string
 * @return Object WildcardQuery object
 */
helma.Search.WildcardQuery = function(field, str) {
    var t = new helma.Search.PKG.index.Term(field, str);
    var  wrappedQuery = new helma.Search.PKG.search.WildcardQuery(t);

    /**
     * getter for wrapped java object
     */
    this.getQuery = function() {
        return wrappedQuery;
    };

    return this;
};
helma.Search.WildcardQuery.prototype = new helma.Search.QueryBase;


///////////////////////////////////////////
// Document
///////////////////////////////////////////

/**
 * constructor function for Document objects that wrap
 * a Lucene Document object
 * @param Object (optional) Lucene Document object
 */
helma.Search.Document = function(document) {
    if (!document)
        document = new helma.Search.PKG.document.Document();

    /**
     * return the Lucene Document object wrapped
     * by this javascript Document object
     */
    this.getDocument = function() {
        return document;
    };

    return this;
};

/**
 * adds a field to this document.
 * @param String Name of the field
 * @param String Value of the field
 * @param Object Parameter object (optional) containing
 *                    the following properties:
 *                    .store (Boolean)
 *                    .index (Boolean)
 *                    .tokenize (Boolean)
 */
helma.Search.Document.prototype.addField = function(name, value, param) {
    if (!param)
        param = {store: true, index: true, tokenize: true};
    if (value === null)
        value = "";
    // if value is a date convert it
    if (value instanceof Date)
        value = helma.Search.PKG.document.DateField.timeToString(value.getTime());
    var f = new helma.Search.PKG.document.Field(String(name),
                                                String(value),
                                                param.store,
                                                param.index,
                                                param.tokenize);
    this.getDocument().add(f);
    return;
};

/**
 * return a single document field
 * @param String name of the field
 * @return Object containing the following parameters:
 *                     .name (String) name of the Field
 *                     .boost (Int) boost factor
 *                     .indexed (Boolean) true if Field is indexed, false otherwise
 *                     .stored (Boolean) true if Field is stored, false otherwise
 *                     .tokenized (Boolean) true if Field is tokenized, false otherwise
 *                     .value (String) value of the Field
 */
helma.Search.Document.prototype.getField = function(name) {
    var f = this.getDocument().getField(name);
    if (f != null) {
        return ({name: name,
                    boost: f.getBoost(),
                    indexed: f.isIndexed(),
                    stored: f.isStored(),
                    tokenized: f.isTokenized(),
                    value: f.stringValue()});
   } else {
       return null;
   }
};

/**
 * return a single document field as Date Object
 * @param String name of the field
 */
helma.Search.Document.prototype.getDateField = function(name) {
    var f = this.getDocument().getField(name);
    if (f != null) {
        return ({name: name,
                    boost: f.getBoost(),
                    indexed: f.isIndexed(),
                    stored: f.isStored(),
                    tokenized: f.isTokenized(),
                    value: new Date(helma.Search.PKG.document.DateField.stringToTime(f.stringValue()))});
    } else {
        return null;
    }
};

/**
 * return the fields of a document
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
 * returns the boost factor of a document
 */
helma.Search.Document.prototype.getBoost = function() {
    return this.getDocument().getBoost();
};

/**
 * sets the boost factor of a document
 */
helma.Search.Document.prototype.setBoost = function(boost) {
    this.getDocument().setBoost(boost);
    return;
};

helma.Search.Document.prototype.toString = function() {
    return "[Document Object]";
};


///////////////////////////////////////////
// helma library stuff
///////////////////////////////////////////

helma.lib = "Search";
helma.dontEnum(helma.lib);
for (var i in helma[helma.lib])
    helma[helma.lib].dontEnum(i);
for (var i in helma[helma.lib].prototype)
    helma[helma.lib].prototype.dontEnum(i);
delete helma.lib;
