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
 * Declare which test methods should be run in which order
 * @type Array
 * @final
 */

var oldMessages;

/**
 * Called before running the tests
 */
var setup = function() {
   // save any currently used message bundle before
   // creating the one for this test
   oldMessages = jala.i18n.getMessages();
   // setup the test message bundle
   var messages = {};
   messages.de_AT = {
      "Hello, World!": "Hallo, Welt!",
      "This is {0}.": "Das ist {0}.",
      "You've got one new mail.": "Du hast eine neue E-Mail.",
      "You've got {0} new mails.": "Du hast {0} neue E-Mails."
   };
   // tell jala where to find the messages
   jala.i18n.setMessages(messages);
   // assign a method for retrieving the locale for translation
   jala.i18n.setLocaleGetter(new Function("return res.meta.locale;"));
   // set the locale to use by jala.i18n
   res.meta.locale = new java.util.Locale("de", "AT");
   return;
};


/**
 * Called after tests have finished. This method will be called
 * regarless whether the test succeeded or failed.
 */
var cleanup = function() {
    // restore any previous message set
    jala.i18n.setMessages(oldMessages);
    return;
};

/**
 * Tests for formatMessage
 */
var testFormatMessage = function() {
   var msg = "Es ist jetzt {0,date,EEEE, dd. MMMM yyyy, HH:mm} Uhr.";
   var args = [new Date(2007, 1, 8, 17, 58)];
   var result = jala.i18n.formatMessage(msg, args);
   var expected = "Es ist jetzt Donnerstag, 08. Februar 2007, 17:58 Uhr.";
   assertEqual(expected, result);
   return;
};

/**
 * Tests for gettext()
 */
var testGettext = function() {
   assertEqual("Hallo, Welt!", gettext("Hello, World!"));
   // test not found message key
   assertEqual("Hello You!", gettext("Hello You!"));
   // test gettext with additional replacement value
   assertEqual("Das ist Jala I18n.", gettext("This is {0}.", "Jala I18n"));
   return;
};

/**
 * Tests for ngettext()
 */
var testNgettext = function() {
   // zero
   assertEqual("Du hast 0 neue E-Mails.",
         ngettext("You've got one new mail.",
                  "You've got {0} new mails.",
                  0));
   // one
   assertEqual("Du hast eine neue E-Mail.",
         ngettext("You've got one new mail.",
                  "You've got {0} new mails.",
                  1));
   // more
   assertEqual("Du hast 23 neue E-Mails.",
         ngettext("You've got one new mail.",
                  "You've got {0} new mails.",
                  23));
   return;
};

/**
 * Tests for message macro
 */
var testMessageMacro = function() {
   // singular
   var skin = createSkin('<% message text="You\'ve got one new mail." %>');
   assertEqual("Du hast eine neue E-Mail.", renderSkinAsString(skin));

   res.handlers.testHandler = {value: 0};
   // value replacement using testHandler
   skin = createSkin('<% message text="You\'ve got {0} new mails." values="testHandler.value" %>');
   assertEqual("Du hast 0 neue E-Mails.", renderSkinAsString(skin));
   // plural including replacement using testHandler
   res.handlers.testHandler.value = 23;
   skin = createSkin('<% message text="You\'ve got one new mail." plural="You\'ve got {0} new mails." values="testHandler.value" %>');
   assertEqual("Du hast 23 neue E-Mails.", renderSkinAsString(skin));
   // using a value of the param object passed to the skin
   // FIXME: appearently this doesn't work, but why?
   /*
   skin = createSkin('<% message text="You\'ve got one new mail." plural="You\'ve got {0} new mails." values="param.spam" %>');
   assertEqual("Du hast 45 neue E-Mails.", renderSkinAsString(skin, {spam: 45}));
   */
};
