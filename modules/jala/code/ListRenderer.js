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
 * @fileoverview Fields and methods of the jala.ListRenderer class.
 */


// Define the global namespace for Jala modules
if (!global.jala) {
   global.jala = {};
}


/**
 * HelmaLib dependencies
 */
app.addRepository("modules/helma/Html.js");

/**
 * @class
 * @param {HopObject|ArrayList|Array} coll The collection this ListRenderer
 * operates on, or - for backwards compatibility only - a parameter object containing
 * the collection and any other optional configuration parameters.
 * @param {Object} renderer An optional renderer to use. If this is set,
 * any rendering method defined in this renderer overrides the default renderer.
 * @constructor
 */
jala.ListRenderer = function(coll, renderer) {
   
   /**
    * The collection this ListRenderer operates on
    * @type HopObject|ArrayList
    * @private
    */
   var collection = null;

   /**
    * Private variable containing the number of items to display
    * on one page. Defaults to 10.
    * @type Number
    * @private
    */
   var pageSize = 10;
   
   /**
    * Private variable containing the maximum number of pages to display
    * within this ListRenderer instance
    * @type Number
    * @private
    */
   var maxPages = Number.MAX_VALUE;

    /**
    * Private variable containing the maximum number of days to display
    * within this ListRenderer instance. If set to null this check
    * is not used.
    * @type Number
    * @private
    */
   var maxDays = null;

   /**
    * Private variable containing the base href of this ListRenderer
    * @type String
    * @private
    */
   var baseHref = null;

   /**
    * Private variable containing the name of the skin to render for
    * a single list item
    * @type String
    * @private
    */
   var itemSkin = null;

   /**
    * Private variable containing any optional url parameters to append to
    * every navigation link rendered by this ListRenderer instance.
    * @type String
    * @private
    */
   var urlParameters = null;

   /**
    * Private variable containing the name of the url parameter containing
    * the page number to display. Defaults to "page".
    * @type String
    * @private
    */
   var urlParameterName = "page";

   /**
    * Internal cache for rendered navigation elements
    * @private
    */
   this.cache = {
      pageNavigation: null,
      prevLink: null,
      nextLink: null,
      maxDayDate: null,
      nextItem: null
   };
   
   /**
    * Returns the collection this ListRenderer instance operates on
    * @returns The collection of this ListRenderer
    * @type HopObject|Array
    */
   this.getCollection = function() {
      return collection;
   };

   /**
    * Sets the collection of this ListRenderer
    * @param {HopObject|ArrayList|Array} coll The collection this ListRenderer instance
    * should operate on
    */
   this.setCollection = function(coll) {
      if (coll != null) {
         if (coll instanceof Array) {
            // wrap array in an ArrayList instance
            collection = new jala.ListRenderer.ArrayList(coll);
         } else {
            collection = coll;
         }
      }
      return;
   };

   /**
    * Returns the number of items displayed on one page
    * @returns The number of items displayed on a single page
    * @type Number
    */
   this.getPageSize = function() {
      return pageSize;
   };

   /**
    * Sets the number of items to display on a single page
    * @param {Number} size The number of items to display on one page
    */
   this.setPageSize = function(size) {
      if (size != null && !isNaN(size)) {
         pageSize = parseInt(size, 10);
      }
      return;
   };

   /**
    * Returns the current page index. This is either the page url parameter
    * or the page number 1.
    * @returns The current page number (starts with 1).
    * @type Number
    * @see #setUrlParameterName
    */
   this.getCurrentPage = function() {
      var pageNr = parseInt(req.data[this.getUrlParameterName()], 10);
      if (!pageNr || isNaN(pageNr)) {
         pageNr = 1;
      }
      return Math.min(Math.max(1, pageNr), this.getTotalPages());
   };

   /**
    * Returns the maximum number of pages handled by this ListRenderer instance
    * @returns The maximum number of pages
    * @type Number
    */
   this.getMaxPages = function() {
      return maxPages;
   };

   /**
    * Sets the maximum number of pages to display
    * @param {Number} pages The maximum number of pages to display
    */
   this.setMaxPages = function(pages) {
      if (pages != null && !isNaN(pages)) {
         maxPages = parseInt(pages, 10);
      }
      return;
   };

   /**
    * Returns the maximum number of days handled by this ListRenderer instance
    * @returns The maximum number of days
    * @type Number
    */
   this.getMaxDays = function() {
      return maxDays;
   };

   /**
    * Sets the maximum number of days to display
    * @param {Number} days The maximum number of days to display
    */
   this.setMaxDays = function(days) {
      if (days != undefined && !isNaN(days)) {
         maxDays = parseInt(days, 10);
      }
      return;
   };

   /**
    * Gets the Date offset indicated by parameter maxDays as Number for runtime efficent comparison
    * @type Number
    */
   this.getMaxDayDate = function() {
      if (this.cache.maxDayDate != null) {
         return this.cache.maxDayDate;
      }
      this.cache.maxDayDate = parseInt((new Date((new Date()).getTime() - (maxDays * Date.ONEDAY))).format("yyyyMMdd"), 10);
      return this.cache.maxDayDate;
   };

   /**
    * @returns {Object} the next Item
    */
   this.getNextItem = function() {
      if (this.cache.nextItem !== null) {
         return this.cache.nextItem;
      }
      var nextItemIndex = this.getEndIndex() + 1;
      this.cache.nextItem = "none";
      if (collection.size() > nextItemIndex) {
         this.cache.nextItem = collection.get(nextItemIndex);
      }
      return this.cache.nextItem;
   };

   /**
    * @returns {Boolean} wether there is a next item
    */
   this.hasNext = function() {
      var nextItem = this.getNextItem();
      var nextIsDisplayable = false;
      var collection = this.getCollection();
      if (maxDays != undefined) {
         if (nextItem != "none" && nextItem.getDayDate() >= this.getMaxDayDate()) {
            nextIsDisplayable = true;
         }
      } else {
         if (nextItem != "none") {
            nextIsDisplayable = true;
         }
      }
      if (collection.size() &&
            nextIsDisplayable === true) {
         return true;
      }
      return false;
   };

   /**
    * Returns the total number of pages handled by this ListRenderer instance
    * (which is the collection size divided by the page size).
    * @returns The total number of pages
    * @type Number
    */
   this.getTotalPages = function() {
      var collectionSize = collection.size();
      var pages = Math.ceil(collectionSize / pageSize);
      if (maxPages > 0) {
         return Math.min(maxPages, pages);
      }
      return pages;
   };

   /**
    * Returns the base href of this ListRenderer instance
    * @returns The base href of this ListRenderer instance
    * @type String
    */
   this.getBaseHref = function() {
      return baseHref;
   };

   /**
    * Sets the base href of this ListRenderer instance. All links rendered
    * will start with the href passed as argument
    * @param {String} href The base href to use for rendering links
    */
   this.setBaseHref = function(href) {
      if (href != null) {
         baseHref = href;
      }
      return;
   };
   
   /**
    * Returns the name of the skin rendered for a single list item
    * @returns The name of the list item skin
    * @type Number
    */
   this.getItemSkin = function() {
      return itemSkin;
   };

   /**
    * Sets the name of the skin to render for every list item
    * @param {String} name The name of the skin to render for every list item
    */
   this.setItemSkin = function(name) {
      if (name != null) {
         itemSkin = name;
      }
      return;
   };

   /**
    * Returns the name of the URL parameter name containing the index
    * of the page to display
    * @returns The name of the page URL parameter name
    * @type String
    */
   this.getUrlParameterName = function() {
      return urlParameterName;
   };

   /**
    * Sets the name of the URL parameter name containing the index of the page
    * to display
    * @param {String} name The name of the page URL parameter
    */
   this.setUrlParameterName = function(name) {
      if (name != null) {
         urlParameterName = name;
      }
      return;
   };

   /**
    * Returns any additional URL parameters included in every navigation link
    * rendered by this ListRenderer instance.
    * @returns A string containing additional URL parameters
    * @type String
    */
   this.getUrlParameters = function() {
      return urlParameters;
   };

   /**
    * Sets additional parameters to include in every navigation link
    * @param {String} params A string to append to every navigation URL
    */
   this.setUrlParameters = function(params) {
      if (params != null) {
         urlParameters = params;
      }
      return;
   };

   /**
    * Returns the renderer used by this ListRenderer instance
    */
   this.getRenderer = function() {
      return renderer;
   };

   /**
    * Sets the renderer to be used by this ListRenderer instance
    * @param {Object} r The renderer to use
    */
   this.setRenderer = function(r) {
      if (r != null) {
         renderer = r;
      }
      return;
   };

   /**
    * Main constructor body
    */
   if (!coll) {
      throw "jala.ListRenderer: insufficient arguments";
   } else if (coll instanceof jala.ListRenderer.ArrayList ||
              coll instanceof Array ||
              coll instanceof HopObject) {
      this.setCollection(coll);
   } else if (coll.collection != null) {
      // this is for backwards compatibility only - the former ListRenderer
      // signature allowed just one parameter object as argument
      this.setCollection(coll.collection);
      this.setBaseHref(coll.href);
      this.setUrlParameters(coll.urlParams);
      this.setUrlParameterName(coll.urlParamName);
      this.setPageSize(coll.itemsPerPage);
      this.setMaxPages(coll.maxPages);
      this.setMaxDays(coll.maxDays);
      this.setItemSkin(coll.itemSkin);
   } else {
      throw "jala.ListRenderer: invalid argument " + coll;
   }
   return this;
};

/**
 * Static instance of helma.Html
 * @type helma.Html
 * @private
 */
jala.ListRenderer.html = new helma.Html();

/** @ignore */
jala.ListRenderer.prototype.toString = function() {
   return "[jala.ListRenderer]";
};

/**
 * Returns the href of a page. If no argument is given, the href
 * of the current page is returned. Any URL parameters set with
 * {@link #setUrlParameters} are added to the href.
 * @param {Number} page The optional page number to include in the href.
 * @returns The href of the page
 * @type String 
 * @see #setUrlParameters
 * @see #setUrlParameterName
 */
jala.ListRenderer.prototype.getPageHref = function(page) {
   var pageNr = (page != null && !isNaN(page)) ? page : this.getCurrentPage();
   var urlParams = this.getUrlParameters();
   res.push();
   res.write(this.getBaseHref());
   if (pageNr || urlParams) {
      res.write("?");
      if (urlParams) {
         res.write(urlParams);
         res.write("&");
      }
      if (pageNr) {
         res.write(this.getUrlParameterName());
         res.write("=");
         res.write(pageNr);
      }
   }
   return res.pop();
};

/**
 * Returns the zero-based index position of the first item of the current page
 * in the collection this ListRenderer operates on.
 * @returns The index position of the first item in the list
 * @type Number
 */
jala.ListRenderer.prototype.getStartIndex = function() {
   return (this.getCurrentPage() -1) * this.getPageSize();
};

/**
 * Returns the zero-based index position of the last item of the current page
 * in the collection this ListRenderer operates on.
 * @returns The index position of the last item in the list
 * @type Number
 */
jala.ListRenderer.prototype.getEndIndex = function() {
   var start = this.getStartIndex();
   return Math.min(start + this.getPageSize(), this.getCollection().size()) - 1;
};

/**
 * Returns the render function to use for a given part of the list. If this
 * ListRenderer doesn't have a renderer attached, or if the renderer doesn't
 * have the appropriate rendering function, the default renderer is used.
 * @param {String} part The part of the page. Valid arguments are
 * "list", "pageNavigation" and "pageLink".
 * @param {String} fName The name of the rendering function to return
 * @returns The function to call for rendering the desired part of the list
 * @type Function
 * @private
 * @see jala.ListRenderer#defaultRenderer
 */
jala.ListRenderer.prototype.getRenderFunction = function(part, fName) {

   var getFunction = function(renderer, name) {
      var handler;
      if ((handler = renderer[part]) != null) {
         if (handler[name] instanceof Function) {
            return handler[name];
         }
      }
      return null;
   };

   var result;
   var renderer = this.getRenderer();
   if (renderer != null) {
      if (!fName || !(result = getFunction(renderer, fName))) {
         result = getFunction(renderer, "default");
      }
   }
   if (!result) {
      result = getFunction(jala.ListRenderer.defaultRenderer, "default");
   }
   return result;
};

/**
 * Renders the list of items for one page directly to response.
 * @param {Object} param Object containing extra parameters (e.g. from a macro call).
 * @see #getList
 */
jala.ListRenderer.prototype.renderList = function(param) {
   var collection = this.getCollection();
   var totalPages = this.getTotalPages();
   var currentPage = this.getCurrentPage();
   var pageSize = this.getPageSize();
   var maxDays = this.getMaxDays();
   var itemSkin = this.getItemSkin();

   if (totalPages > 0) {
      if (!param) {
         param = {};
      }
      var idx = this.getStartIndex();
      var stop = this.getEndIndex();
      // preload objects if collection is a HopObject one
      if (collection instanceof HopObject) {
         collection.prefetchChildren(idx, stop - idx);
      }
      // add various item and list related properties to the parameter object
      param.counter = 1;
      param.index = idx + 1;
      param.stop = stop;
      param.pageSize = pageSize;
      param.itemsPerPage = pageSize; // for backwards compatibility only
      param.collectionSize = collection.size();
      if (!param.skin && itemSkin) {
         param.skin = itemSkin;
      }

      var renderFunc = this.getRenderFunction("list", param.type);
      var item, prevItem;
      while (idx <= stop) {
         item = collection.get(idx++);
         if ((maxDays != undefined) && (item.getDayDate() < this.getMaxDayDate())) {
            idx = stop;
            break;
         }
         renderFunc(item, prevItem, param);
         prevItem = item;
         param.counter += 1;
         param.index += 1;
      }
   }
   return;
};

/**
 * Returns the rendered list of collection items as string
 * @param {Object} param Object containing extra parameters (e.g. from a macro call).
 * @returns The rendered list
 * @type String
 * @see #renderList
 */
jala.ListRenderer.prototype.getList = function(param) {
   res.push();
   this.renderList(param);
   return res.pop() || null;
};

/**
 * Returns the rendered list of collection items as string
 * @param {Object} param Object containing extra parameters (e.g. from a macro call).
 * @returns The rendered list
 * @type String
 * @see #renderList
 * @deprecated Use {@link #getList} instead
 */
jala.ListRenderer.prototype.renderListAsString = function(param) {
   return this.getList(param);
};

/**
 * Renders a link to the previous page directly to response.
 * @param {Object} param Object containing extra parameters (e.g. from a macro call).
 * @see #getPrevLink
 */
jala.ListRenderer.prototype.renderPrevLink = function(param) {
   res.write(this.getPrevLink(param));
   return;
};

/**
 * Returns a rendered link to the previous page as string. For performance
 * reasons this method caches the rendered link in the local cache of this
 * ListRenderer instance.
 * @param {Object} param Object containing extra parameters (e.g. from a macro call).
 * @returns A rendered link to the previous page
 * @type String
 * @see #renderPrevLink
 */
jala.ListRenderer.prototype.getPrevLink = function(param) {
   if (!this.cache.prevLink) {
      res.push();
      var collection = this.getCollection();
      var currentPage = this.getCurrentPage();
      if (collection.size() && currentPage > 1) {
         param.index = currentPage - 1;
         param.href = this.getPageHref(param.index);
         this.getRenderFunction("pageLink", param.type)("prev", param);
      }
      this.cache.prevLink = res.pop();
   }
   return this.cache.prevLink || null;
};

/**
 * Returns a rendered link to the previous page as string
 * @param {Object} param Object containing extra parameters (e.g. from a macro call).
 * @returns A rendered link to the previous page
 * @type String
 * @deprecated Use {@link #getPrevLink} instead
 */
jala.ListRenderer.prototype.renderPrevLinkAsString = function(param) {
   return this.getPrevLink(param);
};

/**
 * Renders a link to the next page directly to response.
 * @param {Object} param Object containing extra parameters (e.g. from a macro call).
 * @see #getNextLink
 */
jala.ListRenderer.prototype.renderNextLink = function(param) {
   res.write(this.getNextLink(param));
   return;
};

/**
 * Returns a rendered link to the previous page as string. For performance
 * reasons this method caches the rendered link in the local cache of this
 * ListRenderer instance.
 * @param {Object} param Object containing extra parameters (e.g. from a macro call).
 * @returns A rendered link to the previous page
 * @type String
 * @see #renderNextLink
 */
jala.ListRenderer.prototype.getNextLink = function(param) {
   if (!this.cache.nextLink) {
      res.push();
      var collection = this.getCollection();
      var currentPage = this.getCurrentPage();
      var totalPages = this.getTotalPages();
      var nextItem = this.getNextItem();
      var nextIsDisplayable = false;
      if (this.getMaxDays() != undefined) {
         if (nextItem != "none" && nextItem.getDayDate() >= this.getMaxDayDate()) {
            nextIsDisplayable = true;
         }
      } else {
         if (nextItem != "none") {
            nextIsDisplayable = true;
         }
      }
      if (collection.size() && currentPage < totalPages && nextIsDisplayable === true) {
         param.index = currentPage + 1;
         param.href = this.getPageHref(param.index);
         this.getRenderFunction("pageLink", param.type)("next", param);
      }
      this.cache.nextLink = res.pop();
   }
   return this.cache.nextLink || null;
};

/**
 * Returns a rendered link to the previous page as string
 * @returns A rendered link to the next page
 * @type String
 * @deprecated Use {@link #getNextLink} instead
 */
jala.ListRenderer.prototype.renderNextLinkAsString = function(param) {
   return this.getNextLink(param);
};

/**
 * Renders the page navigation bar directly to response. For performance reasons
 * this method caches the rendered page navigation in the local cache of this
 * ListRenderer instance.
 * @param {Object} param Object containing extra parameters (e.g. from a macro call).
 * @see #getPageNavigation
 */
jala.ListRenderer.prototype.renderPageNavigation = function(param) {
   if (!this.cache.pageNavigation) {
      var collection = this.getCollection();
      var totalPages = this.getTotalPages();
      var currentPage = this.getCurrentPage();
      var pageSize = this.getPageSize();
   
      if (totalPages > 1) {
         var renderFunc = this.getRenderFunction("pageNavigation", param.type);
         if (!renderFunc) {
            return "[Render function missing]";
         }
   
         // render the navigation-bar
         res.push();
         if (currentPage > 1) {
            renderFunc("item", {
               text: param.previous || "prev",
               url: this.getPageHref(currentPage -1),
            });
         }
         var navLength = parseInt(param.length, 10) || 10;
         var pageNr = 1 + Math.floor((currentPage -1) / navLength) * navLength;
         if (pageNr > 1) {
            renderFunc("item", {
               text: param.previousN || "[..]",
               url: this.getPageHref(pageNr - navLength),
            });
         }
         var stop = Math.min(pageNr + navLength, totalPages +1);
         do {
            renderFunc("item", {
               text: (param.itemPrefix || "") + pageNr + (param.itemSuffix || ""),
               url: this.getPageHref(pageNr),
               selected: pageNr == currentPage
            });
         } while ((pageNr += 1) < stop);

         if (pageNr <= totalPages) {
            renderFunc("item", {
               text: param.nextN || "[..]",
               url: this.getPageHref(pageNr),
            });
         }
         if (currentPage < totalPages) {
            renderFunc("item", {
               text: param.next || "next",
               url: this.getPageHref(currentPage +1),
            });
         }
         var navigation = res.pop();
         res.push();
         renderFunc("navigation", {
            from: ((currentPage -1) * pageSize) +1,
            to: Math.min(((currentPage -1) * pageSize) + pageSize, collection.size()),
            total: collection.size(),
            pageNavigation: navigation,
         });
         this.cache.pageNavigation = res.pop();
      }
   }
   res.write(this.cache.pageNavigation);
   return;
};

/**
 * Returns the rendered page navigation bar as string
 * @param {Object} param Object containing extra parameters (e.g. from a macro call).
 * @returns The rendered page navigation
 * @type String
 * @see #renderPageNavigation
 */
jala.ListRenderer.prototype.getPageNavigation = function(param) {
   res.push();
   this.renderPageNavigation(param);
   return res.pop() || null;
};

/**
 * Returns the rendered page navigation bar as string
 * @returns The rendered page navigation bar
 * @type String
 * @deprecated Use {@link #getPageNavigation} instead
 */
jala.ListRenderer.prototype.renderPageNavigationAsString = function(param) {
   return this.getPageNavigation(param);
};



/*********************************
 ********** M A C R O S **********
 *********************************/


/**
 * Either renders the maximum number of items per page, or
 * sets the limit to a given number.
 * @param {Object} param Extra macro parameters:
 * <ul>
 * <li>to - The maximum number of items per page to be set.
 * </ul>
 * If no limit is set, this macro returns the current number
 * of items per page.
 * @returns The current maximum number of items per page
 * @type Number
 */
jala.ListRenderer.prototype.limit_macro = function(param) {
   if (param.to) {
      this.setPageSize(param.to);
      return;
   } else {
      return this.getPageSize();
   }
};

/**
 * Returns a rendered link to the previous page.
 * @param {Object} param Extra macro parameters:
 * <ul>
 * <li>type - The type of renderer to be applied.</li>
 * </ul>
 * @returns A rendered link to the previous page
 * @type String
 * @see #renderPrevLink
 */
jala.ListRenderer.prototype.prevLink_macro = function(param) {
   return this.getPrevLink(param);
};

/**
 * Returns a rendered link to the next page.
 * @param {Object} param Extra macro parameters:
 * <ul>
 * <li>type - The type of renderer to be applied.</li>
 * </ul>
 * @returns A rendered link to the next page
 * @type String
 * @see #renderNextLink
 */
jala.ListRenderer.prototype.nextLink_macro = function(param) {
   return this.getNextLink(param);
};

/**
 * Returns the rendered page navigation bar.
 * @param {Object} param Extra macro parameters:
 * <ul>
 * <li>type - The type of renderer to be applied.</li>
 * </ul>
 * @returns The rendered page navigation bar
 * @type String
 * @see #getPageNavigation
 */
jala.ListRenderer.prototype.pageNavigation_macro = function(param) {
   return this.getPageNavigation(param);
};

/**
 * Returns the total number of items
 * @returns The total number of items in the collection this ListRenderer
 * instance is working on
 * @type Number
 */
jala.ListRenderer.prototype.size_macro = function() {
   return Math.min(this.getMaxPages() * this.getPageSize(),
                   this.getCollection().size());
};

/**
 * Returns the total number of pages
 * @returns The total number of pages available
 * @type Number
 */
jala.ListRenderer.prototype.totalPages_macro = function() {
   return this.getTotalPages();
};

/**
 * Returns the current page number
 * @returns The current page number
 * @type Number
 */
jala.ListRenderer.prototype.currentPage_macro = function() {
   return this.getCurrentPage();
};

/**
 * Returns the start item number in the current page
 * @returns The start item number in the current page
 * @type Number
 */
jala.ListRenderer.prototype.currentStart_macro = function() {
   return this.getStartIndex() + 1;
};

/**
 * Returns the end item number in the current page
 * @returns The end item number in the current page
 * @type Number
 */
jala.ListRenderer.prototype.currentEnd_macro = function() {
   return this.getEndIndex() + 1;
};

/**
 * Renders the current page of this list.
 * @param {Object} param Extra macro parameters:
 * <ul>
 * <li>skin - The name of the list skin to render for each item in the list.</li>
 * <li>type - The type of renderer to be applied.</li>
 * </ul>
 * @see #renderList
 */
jala.ListRenderer.prototype.render_macro = function(param) {
   var skinName;
   if (!(skinName = param.skin || this.getItemSkin())) {
      res.write("[Name of skin missing]");
   } else {
      this.renderList(param);
   }
   return;
};



/*****************************************************
 ********** D E F A U L T   R E N D E R E R **********
 *****************************************************/


/**
 * Default Renderer object containing functions
 * used for rendering different list items (eg. page navigation,
 * prev/next links and list items).
 * @final
 */
jala.ListRenderer.defaultRenderer = {};

/**
 * List renderer object
 */
jala.ListRenderer.defaultRenderer.list = {};

/**
 * Default renderer method for a list
 * @param {Object} item The current list item to render.
 * @param {Object} prevItem The previous list item
 * @param {Object} param A parameter object containing macro attributes
 * and some parameters set by the ListRenderer.
 */
jala.ListRenderer.defaultRenderer.list["default"] = function(item, prevItem, param) {
   var p = {"class": (param.index % 2 == 0 ? "even" : "odd")};
   item.renderSkin(param.skin, p);
   return;
};

/**
 * Pagenavigation renderer object
 */
jala.ListRenderer.defaultRenderer.pageNavigation = {};

/**
 * Default renderer method for a page navigation bar.
 * @param {String} what A string indicating what should be rendered. Can be
 * either "item" or "navigation" (the former is a single page link, the latter
 * is the whole navigation.
 * @param {Object} A parameter object containing the macro attributes and some
 * attributes set by the ListRenderer.
 */
jala.ListRenderer.defaultRenderer.pageNavigation["default"] = function(what, param) {
   var skin;
   switch (what) {
      case "item":
         if (param.selected == true) {
            param["class"] = "selected";
         } else {
            delete param["class"];
         }
         param.text = jala.ListRenderer.html.linkAsString({href: param.url}, param.text);
         if (param.skin != null) {
            renderSkin(param.skin, param);
         } else if ((skin = app.getSkin("Global", "pageNavigationItem", res.skinpath)) != null) {
            renderSkin(skin, param);
         } else {
            if (param["class"]) {
               res.write('<span class="' + param["class"] + '">');
            } else {
               res.write("<span>");
            }
            res.write(param.text);
            res.write('</span>');
         }
         break;

      case "navigation":
         if (param.skin != null) {
            renderSkin(param.skin, param);
         } else if ((skin = app.getSkin("Global", "pageNavigation", res.skinpath)) != null) {
            renderSkin(skin, param);
         } else {
            res.write('<div class="pageNavigation">');
            res.write('<span class="summary">displaying ');
            res.write(param.from);
            res.write("-");
            res.write(param.to);
            res.write(" (of ");
            res.write(param.total);
            res.write(")</span>");
            res.write('<span class="pages">');
            res.write(param.pageNavigation);
            res.write("</span></div>");
         }
         break;
   }
   return;
};

/**
 * Pagelink renderer object
 */
jala.ListRenderer.defaultRenderer.pageLink = {};

/**
 * Default rendering method for a page link (aka "prev/next" link)
 * @param {String} what A string indicating what should be rendered. Can be
 * either "prev" or "next"
 * @param {Object} param A parameter object containing macro attributes and
 * some set by the ListRenderer.
 */
jala.ListRenderer.defaultRenderer.pageLink["default"] = function(what, param) {
   delete param.index;
   if (param.skin) {
      renderSkin(param.skin, param);
   } else {
      jala.ListRenderer.html.link(param, param.text || what);
   }
   return;
};



/*****************************************
 ********** A R R A Y   L I S T **********
 *****************************************/


/**
 * Creates a new ArrayList instance.
 * @class A simple wrapper around an array to use in conjunction
 * with jala.ListRenderer. This wrapper can either handle complete arrays
 * or subsections of an array. In the latter case the wrapper needs offset
 * and total size information as argument to mimick a complete array.
 * @param {Array} arr The array (or a subsection of an array) to wrap
 * @param {Number} offset An optional offset to use (mandatory if the array
 * is just a subsection).
 * @param {Number} total An optional total size of the array. This argument is
 * mandatory if the wrapped array is just a subsection.
 * @returns A newly created ArrayList instance
 * @constructor
 */
jala.ListRenderer.ArrayList = function(arr, offset, total) {
   /**
    * The offset of this ArrayList instance. This might be > zero for
    * ArrayList instances wrapping just a subsection, that is
    * mimicking a bigger list.
    * @type Number
    */
   this.offset = offset || 0;

   /**
    * The length of this ArrayList instance.
    * @type Number
    */
   this.length = total || arr.length;
   
   /**
    * Returns the element at the index position passed
    * as argument. If the wrapped array is just a subsection
    * the index position passed will be corrected using
    * the offset.
    * @param {Number} idx The index position of the element
    * to return
    * @returns The element at the given index position
    */
   this.get = function(idx) {
      return arr[(this.offset > 0) ? idx - offset : idx];
   };

   /**
    * Returns the size of this ArrayList, which is either
    * the length of the wrapped array or the total size
    * passed as argument to the constructor (in case the wrapped
    * array is just a subsection).
    * @returns The size of this ArrayList instance
    * @type Number
    */
   this.size = function() {
      return this.length;
   };
   
   /**
    * Returns true if this ArrayList is a subsection of a bigger array
    * @returns True if this ArrayList is a subsection of a bigger array
    * @type Boolean
    */
   this.isSubset = function() {
      return offset || total ? true : false;
   };
   
   /**
    * Returns the actual size of this ArrayList's wrapped array.
    * @returns The actual size of this ArrayList's wrapped array.
    * @type Number
    */
   this.subsetSize = function() {
      return arr.length;
   };

   return this;
};

/** @ignore */
jala.ListRenderer.ArrayList.prototype.toString = function() {
   return "[jala.ListRenderer.ArrayList]";
};
