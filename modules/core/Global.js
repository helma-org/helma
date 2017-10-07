/*
 * Helma License Notice
 *
 * The contents of this file are subject to the Helma License
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://adele.helma.org/download/helma/license.txt
 *
 * Copyright 1998-2005 Helma Software. All Rights Reserved.
 *
 * $RCSfile: Global.js,v $
 * $Author$
 * $Revision$
 * $Date$
 */

/**
 * @fileoverview Adds useful global macros.
 * <br /><br />
 * To use this optional module, its repository needs to be added to the
 * application, for example by calling app.addRepository('modules/core/Global.js')
 */

app.addRepository("modules/core/String.js");


/**
 * write out a property contained in app.properties
 * @param Object containing the name of the property
 */
function property_macro(param, name) {
    res.write(getProperty(name || param.name) || String.NULL);
    return;
}


/**
 * wrapper to output a string from within a skin
 * just to be able to use different encodings
 * @param Object containing the string as text property
 */
function write_macro(param, text) {
    res.write(param.text || text || String.NULL);
    return;
}


/**
 * renders the current datetime
 * @param Object containing a formatting string as format property
 */
function now_macro(param) {
    var d = new Date();
    if (param.format) {
        try {
            res.write(d.format(param.format));
        } catch (e) {
            res.write('<span title="' + e + '">[Invalid date format]</span>');
        }
    } else if (param.as == "timestamp") {
        res.write(d.getTime());
    } else {
        res.write(d);
    }
    return;
}


/**
 * renders a global skin
 */
var skin_macro = function(param, name) {
    var skinName = name || param.name;
    if (skinName) {
        renderSkin(skinName, param);
    }
    return;
}

/**
 * Encodes a string for HTML output and inserts linebreak tags.
 *
 * Performs the following string manipulations:
 *  All line breaks (i.e. line feeds) are replaced with <br/> tags.
 *  All special characters are being replaced with their equivalent HTML entity.
 *  Existing markup tags are being encoded.
 *
 * @param {string} text
 *  The string to encode for HTML output.
 * @param {boolean} [encodeNewLine = true]
 *  If or if not to encode line breaks (i.e. line feeds).
 * @return {string}
 *  The encoded string.
 */
function encode(text, encodeNewLine) {
  text = String(text);

  if (text === null || !text.length) return text;
  var buffer = [];
  if (typeof encodeNewline === 'undefined') encodeNewline = true;

  for (var i = 0, len = text.length; i < len; i += 1) {
    var char = text.charAt(i);

    switch (char) {
      case '<':
      buffer.push('&lt;');
      break;

      case '>':
      buffer.push('&gt;');
      break;

      case '&':
      buffer.push('&amp;');
      break;

      case '"':
      buffer.push('&quot;');
      break;

      case '\n':
      if (encodeNewline) {
        buffer.push("<br/>");
      }
      buffer.push('\n');
      break;

      default:
      buffer.push(char);
    }
  }

  return buffer.join('');
}

/**
 * Encodes a string for XML output.
 *
 * Performs the following string manipulations:
 *  All special characters are being replaced with their equivalent HTML entity.
 *  Existing tags, single and double quotes, as well as ampersands are being encoded.
 *  Some invalid XML characters below '0x20' are removed
 *
 * @param {string} text
 *  The string to encode for XML output.
 * @return {string}
 *  The string encoded for XML output.
 */
function encodeXml(text) {
  text = String(text);

  if (text === null || !text.length) return text;
  var buffer = [];

  for (var i = 0, len = text.length; i < len; i += 1) {
    var char = text.charAt(i);

    switch (char) {
      case '<':
      buffer.push('&lt;');
      break;

      case '>':
      buffer.push('&gt;');
      break;

      case '&':
      buffer.push('&amp;');
      break;

      case '"':
      buffer.push('&quot;');
      break;

      case '\'':
      buffer.push('&#39;');
      break;

      default:
      var charCode = str.charCodeAt(i);
      if (charCode < 0x20) {
        // sort out invalid XML characters below 0x20 - all but 0x9, 0xA and 0xD.
        // The trick is an adaption of java.lang.Character.isSpace().
        if (((((1 << 0x9) | (1 << 0xA) | (1 << 0xD)) >> charCode) & 1) !== 0) {
          buffer.push(char);
        }
      } else {
        buffer.push(char);
      }
    }
  }

  return buffer.join('');
}

/**
 * Encodes a string for HTML output, leaving linebreaks untouched.
 *
 * Performs the following string manipulations:
 *  Unlike encode, leaves linebreaks untouched. This is what you usually want to do for encoding form content (esp.
 *  with text input values).
 *  All special characters (i.e. non ASCII) are being replaced with their equivalent HTML entity.
 *  Existing markup tags are being encoded.
 *
 * @param {string} text
 *  The string to format for HTML output.
 * @return {string}
 *  The string formatted for HTML output.
 */
var encodeForm = function(text) {
  text = String(text);

  if (text === null || !text.length) return text;

  return encode(str, false);
};

/**
 * Removes any markup tags contained in the passed string, and returns the modified string.
 *
 * @param {string} markup
 *  The text that is to be stripped of tags.
 * @return {string}
 *  The text with the tags stripped out.
 */
var stripTags = function (markup) {
  if (markup === null) return markup;

  var chars = String(markup).split('');
  var charCounter = 0;
  var inTag = false;

  for (var i = 0, len = markup.length; i < len; i += 1) {
    if (chars[i] === '<') inTag = true;

    if (!inTag) {
      if (i > charCounter) {
        chars[charCounter] = chars[i];
      }

      charCounter += 1;
    }

    if (chars[i] === '>') {
      inTag = false;
    }
  }

  if (i > charCounter) {
    chars.length = charCounter;
    return chars.join('');
  }

  return markup;
};
