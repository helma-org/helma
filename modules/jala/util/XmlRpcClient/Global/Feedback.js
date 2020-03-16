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
 * Error- and Confirmation message container
 * @class Instances of this class can contain numerous error- and confirm messages
 * and function as a macro handler object.
 * @constructor
 * @returns A newly created Feedback instance
 * @type Feedback
 */
var Feedback = function() {
   this.errors = {};
   this.messages = {};
   this.isError = false;
   return this;
};

/**
 * Adds the message with the given name to the list of errors and
 * sets the isError flag to true.
 * @param {String} name The name of the message
 * @param {msg} msg The message to use
 */
Feedback.prototype.setError = function(name, msg) {
   this.errors[name] = msg;
   this.isError = true;
   return;
};

/**
 * Adds the message with the given name to the list of confirmation messages
 * and sets the isError flag to true.
 * @param {String} name The name of the message
 * @param {msg} msg The message to use
 */
Feedback.prototype.setMessage = function(name, msg) {
   this.messages[name] = msg;
   return;
};

/**
 * Returns the message with the given name
 * @returns The message with the given name
 * @type String
 */
Feedback.prototype.message_macro = function(param) {
   if (param.name != null) {
      return this.messages[param.name];
   }
   return;
};

/**
 * Returns the error message with the given name
 * @returns The error message with the given name
 * @type String
 */
Feedback.prototype.error_macro = function(param) {
   if (param.name != null) {
      return this.errors[param.name];
   }
   return;
};
