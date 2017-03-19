// import 'encode';

var encodeForm = function(str, buffer) {
  str = String(str);

  if (str === null || !str.length) return str;
  if (!buffer) buffer = [];

  return encode(str, buffer, false);
};
