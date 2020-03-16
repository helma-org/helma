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
 * @fileoverview Fields and methods of the jala.History class.
 */


// Define the global namespace for Jala modules
if (!global.jala) {
   global.jala = {};
}


/**
 * Constructs a new History object.
 * @class This class is an implementation of a Browser-like history
 * stack suitable to use in any Helma application. The difference
 * to a Browser's history is that this implementation ignores
 * POST requests and checks if Urls in the stack are still valid to
 * prevent eg. redirections to a HopObject's url that has been deleted.
 * Plus it is capable to create new "intermediate" history-stacks
 * and this way maintain a "history of histories" which is needed for
 * eg. editing sessions in a popup window that should use their own
 * request history without interfering with the history of the
 * main window.
 * @constructor
 */
jala.History = function() {
   var MAX = 40;

   /**
    * Stack constructor
    * @private
    */
   var Stack = function(id) {
      this.items = [];
      this.id = id;
      return this;
   };

   /**
    * Returns the current url including query string
    * @private
    */
   var getUrl = function() {
      var query;
      var url = path.href(req.action);
      try {
         if (query = req.getServletRequest().getQueryString())
            url += "?" + query;
      } catch (e) {
         // ignore
      }
      return url;
   }

   /**
    * Checks if a request is valid for being added
    * to the history stack. This method returns false
    * for all POST-requests or requests whose action name
    * contains a dot (to prevent requests for external stylesheets
    * or the like from being recorded).
    * @private
    * @type Boolean
    */
   var isValid = function() {
      // FIXME: we should check for mimetype of response instead
      // of assuming that requests containing a dot aren't worth
      // being put into history stack ...
      if (req.isPost() || (req.action && req.action.contains(".")))
         return false;
      return true;
   };

   /**
    * returns a single Stack instance
    * @private
    */
   var getStack = function() {
      if (history.length < 1)
         history.push(new Stack(getUrl()));
      return history[history.length -1];
   };

   /**
    * Variable containing the history-stacks
    * @private
    */
   var history = [];

   /**
    * Initializes a new history stack, adds
    * it to the array of stacks (which makes it
    * the default one to use for further requests)
    * and records the current request Url.
    */
   this.add = function() {
      if (!isValid())
         return;
      var url = getUrl();
      if (getStack().id != url) {
         history.push(new Stack(url));
         this.push();
      }
      return;
   };

   /**
    * Removes the current history stack
    */
   this.remove = function() {
      history.pop();
      return;
   };

   /**
    * Records a request Url in the currently active
    * history stack.
    */
   this.push = function() {
      if (isValid()) {
         var obj = path[path.length-1];
         var url = getUrl();
         var stack = getStack();
         if (stack.items.length < 1 || stack.items[stack.items.length -1].url != url) {
            if (stack.items.length >= MAX)
               stack.items.shift();
            stack.items.push({
               url: url,
               path: path.href().substring(root.href().length).replace(/\+/g, " ")
            });
         }
      }
      return;
   };
   
   /**
    * Clears the currently active history stack
    */
   this.clear = function() {
      getStack().items.length = 0;
      return;
   };   

   /**
    * Redirects the client back to the first valid
    * request in history. Please mind that searching for
    * a valid Url starts at <em>history.length - 2</em>.
    * @param {Number} offset The index position in the stack to start
    * searching at
    */
   this.redirect = function(offset) {
      res.redirect(this.pop(offset));
      return;
   };
   
   /**
    * Retrieves the first valid request Url in history
    * stack starting with a given offset. The default offset is 1.
    * Any valid Url found is removed from the stack, therefor
    * this method <em>alters the contents of the history stack</em>.
    * @param {Number} offset The index position in history stack to start
    * searching at
    * @return The Url of the request
    * @type String
    */
   this.pop = function(offset) {
      /**
       * checks if a referrer is stil valid
       * @private
       */
      var isValidPath = function(p) {
         var arr = p.split("/");
         var obj = root;
         for (var i=0;i<arr.length -1;i++) {
            if (!(obj = obj.get(unescape(arr[i]))) || obj.__node__.getState() == 3)
               return false;
         }
         return true;
      };

      var obj;
      var cut = offset != null ? offset : 1;
      var stack = getStack();
      if (stack.items.length > 0) {
         while (cut-- > 0) {
            obj = stack.items.pop();
         }
      }
      while (stack.items.length > 0) {
         obj = stack.items.pop();
         // check if url is valid
         if (isValidPath(obj.path)) {
            return obj.url;
         }
      }
      return path.href();
   };

   /**
    * Retrieves the request Url at the given position
    * in the current history stack. If no offset is given
    * the last Url in the stack is returned. This method
    * <em>does not alter the stack contents</em>!
    * @param {Number} offset The index position in history stack to start
    * searching at
    * @return The Url of the request
    * @type String
    */
   this.peek = function(offset) {
      var stack = getStack();
      return stack.items[stack.items.length - (offset != null ? offset : 1)];
   };

   /**
    * Returns the contents of all history stacks
    * as string
    * @return The history stacks as string
    * @type String
    */
   this.dump = function() {
      return history.toSource();
   };

   /** @ignore */
   this.toString = function() {
      return "[History " + getStack().toSource() + "]";
   };

   return this;
}
