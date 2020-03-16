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
 * @fileoverview Fields and methods of the jala.AsyncRequest class.
 */


// Define the global namespace for Jala modules
if (!global.jala) {
   global.jala = {};
}


/**
 * Creates a new AsyncRequest instance.
 * @class This class is used to create requests of type "INTERNAL"
 * (like cron-jobs) that are processed in a separate thread and
 * therefor asynchronous.
 * @param {Object} obj Object in whose context the method should be called
 * @param {String} funcName Name of the function to call
 * @param {Array} args Array containing the arguments that should be passed
 * to the function (optional). This option is <em>deprecated</em>, instead
 * pass the arguments directly to the {@link #run} method.
 * @constructor
 * @returns A new instance of AsyncRequest
 * @type AsyncRequest
 * @deprecated Use the {@link http://helma.zumbrunn.net/reference/core/app.html#invokeAsync
 * app.invokeAsync} method instead (built-in into Helma as 
 * of version 1.6)
 */
jala.AsyncRequest = function(obj, funcName, args) {
   app.logger.warn("Use of jala.AsyncRequest is deprecated in this version.");
   app.logger.warn("This module will probably be removed in a " +
                   "future version of Jala.");
   
   /**
    * Contains a reference to the thread started by this AsyncRequest
    * @type java.lang.Thread
    * @private
    */
   var thread;
   
   /**
    * Contains the timeout defined for this AsyncRequest (in milliseconds)
    * @type Number
    * @private
    */
   var timeout;
   
   /**
    * Contains the number of milliseconds to wait before starting
    * the asynchronous request.
    * @type Number
    * @private
    */
   var delay;

   /**
    * Run method necessary to implement java.lang.Runnable.
    * @private
    */
   var runner = function() {
      // evaluator that will handle the request
      var ev = app.__app__.getEvaluator();

      if (delay != null) {
         java.lang.Thread.sleep(delay);
      }
      try {
         if (args === undefined || args === null || args.constructor != Array) {
            args = [];
         }
         if (timeout != null) {
            ev.invokeInternal(obj, funcName, args, timeout);
         } else {
            ev.invokeInternal(obj, funcName, args);
         }
      } catch (e) {
         // ignore it, but log it
         app.log("[Runner] Caught Exception: " + e);
      } finally {
         // release the ev in any case
         app.__app__.releaseEvaluator(ev);
         // remove reference to underlying thread
         thread = null;
      }
      return;
   };

   /**
    * Sets the timeout of this asynchronous request.
    * @param {Number} seconds Thread-timeout.
    */
   this.setTimeout = function(seconds) {
      timeout = seconds * 1000;
      return;
   };
   
   /**
    * Defines the delay to wait before evaluating this asynchronous request.
    * @param {Number} millis Milliseconds to wait
    */
   this.setDelay = function(millis) {
      delay = millis;
      return;
   };

   /**
    * Starts this asynchronous request. Any arguments passed to
    * this method will be passed to the method executed by
    * this AsyncRequest instance.
    */
   this.run  = function() {
      if (arguments.length > 0) {
         // convert arguments object into array
         args = Array.prototype.slice.call(arguments, 0, arguments.length);
      }
      thread = (new java.lang.Thread(new java.lang.Runnable({"run": runner})));
      thread.start();
      return;
   };

   /**
    * Starts this asynchronous request.
    * @deprecated Use {@link #run} instead
    */
   this.evaluate = function() {
      this.run.apply(this, arguments);
      return;
   };

   /**
    * Returns true if the underlying thread is alive
    * @returns True if the underlying thread is alive,
    * false otherwise.
    * @type Boolean
    */
   this.isAlive = function() {
      return thread != null && thread.isAlive();
   }   

   /** @ignore */
   this.toString = function() {
      return "[jala.AsyncRequest]";
   };

   /**
    * Main constructor body
    */
   if (!obj || !funcName)
      throw "jala.AsyncRequest: insufficient arguments.";
   return this;
}
