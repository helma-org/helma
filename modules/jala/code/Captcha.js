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
 * @fileoverview Fields and methods of the jala.Captcha class.
 */


// Define the global namespace for Jala modules
if (!global.jala) {
   global.jala = {};
}

/** 
 * Construct a new captcha.
 * @returns A new captcha.
 * @class Wrapper class for the 
 * {@link http://jcaptcha.sourceforge.net/ JCaptcha library}.
 * A captcha (an acronym for "completely automated public 
 * Turing test to tell computers and humans apart") is a 
 * type of challenge-response test used in computing to 
 * determine whether or not the user is human.
 * @constructor
 */
jala.Captcha = function() {
   /**
    * Jala dependencies
    */
   app.addRepository(getProperty("jala.dir", "modules/jala") + 
                     "/lib/jcaptcha-all-1.0-RC3.jar");

   var gimpy;

   try {
      var ref = Packages.com.octo.captcha.engine.image.gimpy;
      gimpy = ref.DefaultGimpyEngine();
   } catch(e) {
      throw("Cannot instantiate object due to missing java class: " +
            arguments.callee.toString());
   }
   var captcha = gimpy.getNextCaptcha();

   /**
    * Get a new captcha object.
    * @returns A new captcha object.
    * @type com.octo.captcha.Captcha
    */
   this.getCaptcha = function getCaptcha() {
      return captcha;
   };

   /**
    * Render a new captcha image.
    */
   this.renderImage = function renderImage() {
      var image = captcha.getImageChallenge();
      var stream = new java.io.ByteArrayOutputStream();
      var ref = Packages.com.sun.image.codec.jpeg.JPEGCodec;
      var encoder = ref.createJPEGEncoder(stream);
      encoder.encode(image);
      res.contentType = "image/jpeg";
      res.writeBinary(stream.toByteArray());
      return;
   };

   /**
    * Validate a user's input with the prompted captcha.
    * @param {String} input The user's input.
    * @returns True if the user's input matches the captcha.
    * @type Boolean
    */
   this.validate = function validate(input) {
      return !input || captcha.validateResponse(input);
   };

   return this;
};


/**
 * Get a string representation of the captcha class.
 * @returns A string representation of the capthca class.
 * @type String
 * @ignore
 */
jala.Captcha.toString = function toString() {
   return "[jala.Captcha http://jcaptcha.sourceforge.net]";
};


/**
 * Get a string representation of the captcha object.
 * @returns A string representation of the captcha object.
 * @type String
 */
jala.Captcha.prototype.toString = function toString() {
   return "[jala.Captcha Object]";
};
