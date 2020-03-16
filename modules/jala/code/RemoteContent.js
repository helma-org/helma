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
 * @fileoverview Fields and methods of the jala.RemoteContent class.
 */

// HelmaLib dependencies
app.addRepository("modules/core/String.js");
app.addRepository("modules/core/Object.js");
app.addRepository("modules/core/Date.js");
app.addRepository("modules/helma/Http.js");

// Define the global namespace for Jala modules
if (!global.jala) {
   global.jala = {};
}

/**
 * Construct a new remote content handler.
 * @class API to define, fetch and update content
 * from a remote site.
 * @param {String} url The URL string of the remote site.
 * @param {Integer} method The method to retrieve the remote content.
 * @param {File} storage The cache directory.
 * @returns A new remote content handler.
 * @extends helma.Http
 * @constructor
 */
jala.RemoteContent = function(url, method, storage) {
   if (typeof PropertyMgr == "undefined")
      var PropertyMgr = {};

   var NULLSTR = "";
   var key = url.md5();
   var fname = key + jala.RemoteContent.SUFFIX;
   var cache;
   method = (method != null ? method.toLowerCase() : null);

   // depending on the method argument the instance 
   // becomes extent of the appropriate remote client
   switch (method) {
      case jala.RemoteContent.XMLRPC:
         break;
      default:
         helma.Http.call(this);
         break;
   }

   if (!storage) {
      storage = jala.RemoteContent.CACHEDIR;
      if (!storage.exists() || !storage.isDirectory())
         storage.mkdir(storage.getAbsolutePath());
   }

   var getCache = function() {
      switch (storage.constructor) {
         case HopObject:
            cache = storage;
            break;

         case PropertyMgr:
            cache = storage.getAll();
            break;

         default:
            var f = new File(storage, fname);
            cache = f.exists() ? Xml.read(f) : new HopObject();
      }
      return cache;
   };

   var setCache = function() {
      cache.url = url;
      cache.method = method;
      if (!cache.interval) {
         cache.interval = Date.ONEHOUR;
      }
      cache.lastUpdate = new Date();
      cache = cache.clone(new HopObject());

      switch (storage.constructor) {
         case HopObject:
            for (var i in cache)
               storage[i] = cache[i];
            break;

         case PropertyMgr:
            storage.setAll(cache);
            break;

         default:
            var f = new File(storage, fname);
            Xml.write(cache, f);
      }
      return;
   };

   cache = getCache();

   /**
    * Set the interval the remote content's
    * cache is bound to be updated.
    * @param {Number} interval The interval value in milliseconds.
    */
   this.setInterval = function(interval) {
      cache.interval = parseInt(interval, 10);
      return;
   };
   
   /**
    * Get an arbitrary property of the remote content.
    * @param {String} key The name of the property.
    * @returns The value of the property.
    */
   this.get = function(key) {
      return cache[key];
   }

   /**
    * Get all available property names.
    * @returns The list of property names.
    * @type Array
    */
   this.getKeys = function() {
      var keys = [];
      for (var i in cache) {
         keys.push(i);
      }
      return keys.sort();
   };

   /**
    * Tests whether the remote content needs to be updated.
    * @returns True if the remote content needs to be updated.
    * @type Boolean
    */
   this.needsUpdate = function() {
      if (!cache.lastUpdate) {
         return true;
      } else {
         var max = new Date() - cache.interval;
         if (max - cache.lastUpdate > 0) {
            return true;
         }
      }
      return false;
   };

   /**
    * Get the updated and cached remote content.
    * @returns The content as retrieved from the remote site.
    * @type String
    */
   this.update = function() {
      app.debug("[jala.RemoteContent] Retrieving " + url);
      var result;
      switch (method) {
         case jala.RemoteContent.XMLRPC:
            break;
         default:
            result = this.getUrl(url, cache.lastModified || cache.eTag);
            if (result.code != 200 && cache.content) {
               // preserve the content received before
               result.content = cache.content;
            }
            result.interval = cache.interval;
            cache = result;
      }
      setCache();
      return cache.content;
   };

   /**
    * Flushes (empties) the cached remote content.
    */
   this.clear = function() {
      switch (storage.constructor) {
         case HopObject:
            for (var i in storage)
               delete storage[i];
            break;

         case PropertyMgr:
            storage.reset();
            break;

         default:
            var f = new File(storage, fname);
            f.remove();
      }
      return;
   };
   
   /**
    * Get a string representation of the remote content.
    * @returns The remote content as string.
    * @type String
    */
   this.toString = function() {
      return cache.content || NULLSTR;
   };

   /**
    * Get the value of the remote content.
    * @returns The remote content including response header data.
    * @type Object
    */
   this.valueOf = function() {
      return cache;
   };

   return this;
};

/**
 * A constant representing the HTTP retrieval method.
 * @type int
 * @final
 */
jala.RemoteContent.HTTP = 1;

/**
 * A constant representing the XML-RPC retrieval method.
 * @type int
 * @final
 */
jala.RemoteContent.XMLRPC = 2;

/**
 * The default name of the cache directory.
 * @type String
 * @final
 */
jala.RemoteContent.SUFFIX = ".cache";

/**
 * The default cache directory.
 * @type File
 * @final
 */
jala.RemoteContent.CACHEDIR = new File(app.dir, jala.RemoteContent.SUFFIX);

/**
 * Remove all remote content from a file-based cache.
 * @param {File} cache An optional target directory.
 */
jala.RemoteContent.flush = function(cache) {
   jala.RemoteContent.forEach(function(rc) {
      rc.clear();
      return;
   });
   return;
};

/**
 * Apply a custom method on all remote content in a file-based cache.
 * @param {Function} callback The callback method to be executed
 * for each remote content file.
 * @param {File} cache An optional target directory.
 */
jala.RemoteContent.forEach = function(callback, cache) {
   if (!cache)
      cache = jala.RemoteContent.CACHEDIR;
   var f, rc;
   var files = cache.list();
   for (var i in files) {
      f = new File(cache, files[i]);
      if (!files[i].endsWith(jala.RemoteContent.SUFFIX))
         continue;
      rc = new jala.RemoteContent(Xml.read(f).url);
      if (callback && callback.constructor == Function)
         callback(rc);
   }
   return;
};

/**
 * Apply a custom method on all remote content in a file-based cache.
 * @param {Function} callback The callback method to be executed
 * for each remote content file.
 * @param {File} cache An optional target directory.
 * @deprecated Use {@link #forEach} instead.
 */
jala.RemoteContent.exec = function() {
   jala.RemoteContent.forEach.apply(this, arguments);
};
