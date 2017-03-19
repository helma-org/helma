var encode = function (str, buffer, encodeNewline) {
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

      case '\n':
      if (encodeNewline) {
        buffer.push("<br class='helma-format' />");
      }
      buffer.push('\n');
      break;

      default:
      buffer.push(char);
    }
  }

  return buffer.join('');
};
