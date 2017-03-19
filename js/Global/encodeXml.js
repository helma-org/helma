var encodeXml = function(str, buffer) {
  str = String(str);

  if (str === null || !str.length) return str;
  if (!buffer) buffer = [];

  for (var i = 0, len = str.length; i < len; i += 1) {
    var char = str.charAt(i);

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
