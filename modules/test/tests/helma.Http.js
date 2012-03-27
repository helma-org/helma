var tests = [
    "testSimple",
    "testError", 
    "testNotFound",
    "testRedirect",
    "testRedirectNoFollow",
    "testMaxResponseSize",
    "testLongResponse"
];

app.addRepository("modules/helma/Http.js");

var http = new helma.Http();

var testSimple = function() {
    var result = http.getUrl(root.href("hello"));
    assertEqual(result.content, "Hello");
    assertEqual(result.code, 200);
};

var testError = function() {
    var result = http.getUrl(root.href("throwerror"));
    assertEqual(result.content, "Error");
    assertEqual(result.code, 500);
};

var testNotFound = function() {
    var result = http.getUrl(root.href("nonExistingAction"));
    assertEqual(result.content, "Not found");
    assertEqual(result.code, 404);
};

var testRedirect = function() {
    var result = http.getUrl(root.href("redirect"));
    assertEqual(result.content, "Hello");
    assertEqual(result.code, 200);
};

var testRedirectNoFollow = function() {
    http.setFollowRedirects(false);
    var result = null;
    try {
        result = http.getUrl(root.href("redirect"));
    } finally {
        http.setFollowRedirects(true);
    }
    assertEqual(result.content, "");
    // response codes 302 and 303 are both ok
    assertTrue(result.code == 302 || result.code == 303);
};

var testMaxResponseSize = function() {
    http.setMaxResponseSize(3);
    var error = null;
    try {
        http.getUrl(root.href("hello"));
    } catch (err) {
        error = err;
    } finally {
        http.setMaxResponseSize(null);
    }
    assertNotNull(error);
};

var testLongResponse = function() {
    var result = http.getUrl(root.href("long"));
    assertEqual(result.content, makeLongString());
};
