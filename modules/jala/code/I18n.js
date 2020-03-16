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
 * @fileoverview Methods and macros for internationalization
 * of Helma applications.
 */

// Define the global namespace for Jala modules
if (!global.jala) {
   global.jala = {};
}

/**
 * Constructs a new instance of jala.I18n
 * @class This class provides various functions and macros for
 * internationalization of Helma applications.
 * @constructor
 * @type jala.I18n
 */
jala.I18n = function() {
   /**
    * The default object containing the messages.
    * @ignore
    */
   var messages = global.messages;

   /**
    * The default method for retrieving the locale.
    * @ignore
    */
   var localeGetter = function() {
      return java.util.Locale.getDefault();
   };

   /**
    * Overwrite the default object containing
    * the messages (ie. a vanilla EcmaScript object).
    * @param {Object} msgObject The object containing the messages
    */
   this.setMessages = function(msgObject) {
      messages = msgObject;
   };

   /**
    * Get the message object.
    * @returns The object containing the messages
    * @type Object
    */
   this.getMessages = function() {
      return messages;
   };

   /**
    * Set the method for retrieving the locale.
    * @param {Function} func The getter method
    */
   this.setLocaleGetter = function(func) {
      if (func && func.constructor == Function) {
         localeGetter = func;
      } else {
         throw Error("Getter method to retrieve locale must be a function");
      }
      return;
   };

   /**
    * Get the method for retrieving the locale.
    * @returns The getter method
    * @type Function
    */
   this.getLocaleGetter = function() {
      return localeGetter;
   };

   return this;
};

/**
 * The default handler containing the messages.
 * @ignore
 */
jala.I18n.HANDLER = global;

/** @ignore */
jala.I18n.prototype.toString = function() {
   return "[Jala i18n]";
};

/**
 * Set (overwrite) the default handler containing
 * the messages (ie. a vanilla EcmaScript object).
 * @param {Object} handler The handler containing the message object
 * @deprecated Use {@link #setMessages} instead
 */
jala.I18n.prototype.setHandler = function(handler) {
   this.setMessages(handler.messages);
   return;
};

/**
 * Returns the locale for the given id, which is expected to follow
 * the form <code>language[_COUNTRY][_variant]</code>, where <code>language</code>
 * is a valid ISO Language Code (eg. "de"), <code>COUNTRY</code> a valid ISO
 * Country Code (eg. "AT"), and variant an identifier for the variant to use.
 * @returns The locale for the given id
 * @type java.util.Locale
 */
jala.I18n.prototype.getLocale = function(localeId) {
   if (localeId) {
      if (localeId.indexOf("_") > -1) {
         var arr = localeId.split("_");
         if (arr.length == 3) {
            return new java.util.Locale(arr[0], arr[1], arr[2]);
         } else {
            return new java.util.Locale(arr[0], arr[1]);
         }
      } else {
         return new java.util.Locale(localeId);
      }
   }
   return java.util.Locale.getDefault();
}

/**
 * Tries to "translate" the given message key into a localized
 * message.
 * @param {String} key The message to translate (required)
 * @param {String} plural The plural form of the message to translate
 * @param {Number} amount A number to determine whether to use the
 * singular or plural form of the message
 * @returns The localized message or the appropriate key if no
 * localized message was found
 * @type String
 */
jala.I18n.prototype.translate = function(singularKey, pluralKey, amount) {
   var translation = null;
   if (singularKey) {
      // use the getter method for retrieving the locale
      var locale = this.getLocaleGetter()();
      var catalog, key;
      if ((catalog = jala.i18n.getCatalog(locale))) {
         if (arguments.length == 3 && amount != 1) { // is plural
            key = pluralKey;
         } else {
            key = singularKey;
         }
         if (!(translation = catalog[key])) {
            translation = key;
            app.logger.debug("jala.i18n.translate(): Can't find message '" +
                             key + "' for locale '" + locale + "'");
         }
      } else {
         app.logger.debug("jala.i18n.translate(): Can't find message catalog for locale '" + locale + "'");
         if (!pluralKey || amount == 1) {
            translation = singularKey;
         } else {
            translation = pluralKey;
         }
      }
   }
   return translation;
};

/**
 * Helper method to get the message catalog
 * corresponding to the actual locale.
 * @params {java.util.Locale} locale
 * @returns The message catalog.
 */
jala.I18n.prototype.getCatalog = function(locale) {
   if (!jala.I18n.catalogs) {
      jala.I18n.catalogs = {};
   }

   var catalog = jala.I18n.catalogs[locale];

   if (catalog) return catalog;

   var messages = this.getMessages();

   if (locale && messages) {
      catalog = messages[locale.toLanguageTag()];
      jala.I18n.catalogs[locale] = catalog;
   }

   return catalog;
};

/**
 * Converts the message passed as argument into an instance
 * of java.text.MessageFormat, and formats it using the
 * replacement values passed.
 * @param {String} message The message to format
 * @param {Array} values An optional array containing replacement values
 * @returns The formatted message or, if the formatting fails, the
 * message passed as argument.
 * @type String
 * @see http://java.sun.com/j2se/1.5.0/docs/api/java/text/MessageFormat.html
 */
jala.I18n.prototype.formatMessage = function(message, values) {
   if (message) {
      var args = null;
      if (values != null && values.length > 0) {
         args = java.lang.reflect.Array.newInstance(java.lang.Object, values.length);
         var arg;
         for (var i=0;i<values.length;i++) {
            if ((arg = values[i]) != null) {
               // MessageFormat can't deal with javascript date objects
               // so we need to convert them into java.util.Date instances
               if (arg instanceof Date) {
                  args[i] = new java.util.Date(arg.getTime());
               } else {
                  args[i] = arg;
               }
            }
         }
      }
      // use the getter method for retrieving the locale
      var locale = this.getLocaleGetter()();
      // format the message
      try {
         var msgFormat = new java.text.MessageFormat(message, locale);
         return msgFormat.format(args);
      } catch (e) {
         app.logger.warn("jala.i18n.formatMessage(): Unable to format message '"
                         + message + "', reason: " + e, e.javaException);
      }
   }
   return null;
};

/**
 * Returns a localized message for the message key passed as
 * argument. If no localization is found, the message key
 * is returned. Any additional arguments passed to this function
 * will be used as replacement values during message rendering.
 * To reference these values the message can contain placeholders
 * following "{number}" notation, where <code>number</code> must
 * match the number of the additional argument (starting with zero).
 * @param {String} key The message to localize
 * @returns The translated message
 * @type String
 * @see #translate
 * @see #formatMessage
 */
jala.I18n.prototype.gettext = function(key /** [value 0][, value 1][, ...] */) {
   return this.formatMessage(this.translate(key),
                             Array.prototype.splice.call(arguments, 1));
};

/**
 * Returns a localized message for the message key passed as
 * argument. In contrast to gettext() this method
 * can handle plural forms based on the amount passed as argument.
 * If no localization is found, the appropriate message key is
 * returned. Any additional arguments passed to this function
 * will be used as replacement values during message rendering.
 * To reference these values the message can contain placeholders
 * following "{number}" notation, where <code>number</code> must
 * match the number of the additional argument (starting with zero).
 * @param {String} singularKey The singular message to localize
 * @param {String} pluralKey The plural form of the message to localize
 * @param {Number} amount The amount which is used to determine
 * whether the singular or plural form of the message should be returned.
 * @returns The translated message
 * @type String
 * @see #translate
 * @see #formatMessage
 */
jala.I18n.prototype.ngettext = function(singularKey, pluralKey, amount /** [value 0][, value 1][, ...] */) {
   return this.formatMessage(this.translate(singularKey, pluralKey, amount || 0),
                             Array.prototype.splice.call(arguments, 2));
};

/**
 * A simple proxy method which is used to mark a message string
 * for the i18n parser as to be translated.
 * @param {String} key The message that should be seen by the
 * i18n parser as to be translated.
 * @returns The message in unmodified form
 * @type String
 */
jala.I18n.prototype.markgettext = function(key) {
   return key;
};

/**
 * Returns a translated message. The following macro attributes
 * are accepted:
 * <ul>
 * <li>text: The message to translate (required)</li>
 * <li>plural: The plural form of the message</li>
 * <li>values: A list of replacement values. Use a comma to separate more
 * than one value. Each value is either interpreted as a global property
 * (if it doesn't containg a dot) or as a property name of the given macro
 * handler object (eg. "user.name"). If the value of the property is a
 * HopObject or an Array this macro uses the size() resp. length of the
 * object, otherwise the string representation of the object will be used.</li>
 * </ul>
 * @returns The translated message
 * @type String
 * @see #gettext
 * @see #ngettext
 */
jala.I18n.prototype.message_macro = function(param) {
   if (param.text) {
      var args = [param.text];
      if (param.plural) {
         args[args.length] = param.plural;
      }
      if (param.values != null) {
         var arr = param.values.split(/\s*,\s*/g);
         // convert replacement values: if the value name doesn't contain
         // a dot, look for a global property with that name, otherwise
         // for a property of the specified macro handler object.
         var propName, dotIdx, handlerName, handler;
         for (var i=0;i<arr.length;i++) {
            if ((propName = arr[i]) != null) {
               var value = null;
               if ((dotIdx = propName.indexOf(".")) > 0) {
                  var handlerName = propName.substring(0, dotIdx);
                  if (handlerName == "request") {
                     handler = req.data;
                  } else if (handlerName == "response") {
                     handler = res.data;
                  } else if (!(handler = res.handlers[handlerName])) {
                     continue;
                  }
                  propName = propName.substring(dotIdx + 1);
                  // primitive security: don't allow access to internal properties
                  // and a property named "password"
                  if (propName.charAt(0) != "_" && propName.toLowerCase() != "password") {
                     value = handler[propName];
                  }
               } else {
                  value = global[propName];
               }
               if (value != null) {
                  // if its a HopObject collection or Array, use its size/length
                  // as value
                  if (value instanceof HopObject) {
                     value = value.size();
                  } else if (value instanceof Array) {
                     value = value.length;
                  }
               }
               args[args.length] = value;
            }
         }
      }
      if (param.plural) {
         return this.ngettext.apply(this, args);
      } else {
         return this.gettext.apply(this, args);
      }
   }
   return;
};

/**
 * Default i18n class instance.
 * @type jala.I18n
 * @final
 */
jala.i18n = new jala.I18n();

/**
 * For convenience reasons the public methods and macros are
 * put into global scope too
 */
var gettext = function() {
   return jala.i18n.gettext.apply(jala.i18n, arguments);
};
var ngettext = function() {
   return jala.i18n.ngettext.apply(jala.i18n, arguments);
};
var markgettext = function() {
   return jala.i18n.markgettext.apply(jala.i18n, arguments);
};
var message_macro = function() {
   return jala.i18n.message_macro.apply(jala.i18n, arguments);
};
