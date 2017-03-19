var stripTags = function (str) {
  if (str === null) return str;

  var chars = String(str).split('');
  var charCounter = 0;
  var inTag = false;

  for (var i = 0, len = str.length; i < len; i += 1) {
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

  return str;
};

