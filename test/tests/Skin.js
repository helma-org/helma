var tests = [
   "testCommentMacro",
   "testDeepResolve",
   "testDeepUnhandled",
   "testDeep",
   "testDeepFail",
   "testDeepFailSilent",
   "testJavaProp",
   "testJavaMissing",
   "testJavaMissingSilent",
   "testJavaMissingVerbose",
   "testUndefinedHandler",
   "testJSProp",
   "testJSMacro",
   "testJSFunction",
   "testJSMissing",
   "testJSMissingSilent",
   "testJSMissingVerbose",
   "testDateFilter",
   "testNestedRootMacro",
   "testNestedParamMacro",
   "testNestedResponseMacro",
   "testNestedPrefixSuffix",
   "testResponseProp",
   "testResponseMacro",
   "testResponseFunction",
   "testResponseMissing",
   "testResponseMissingSilent",
   "testResponseMissingVerbose",
   "testRootProp",
   "testRootMacro",
   "testRootMissing",
   "testRootMissingSilent",
   "testRootMissingVerbose",
   "testSessionProp",
   "testSessionMacro",
   "testSessionFunction",
   "testSessionMissing",
   "testSessionMissingDefault",
   "testSessionMissingSilent",
   "testSessionMissingVerbose",
   "testSessionDeepMissing",
   "testSubskins"
];

var setup = function() {
    res.handlers.color = new java.awt.Color["(int,int,int)"](0, 255, 0);
    res.handlers.file = new java.io.File["(java.lang.String,java.lang.String)"](null, "file");
    res.handlers.jsobject = {
        banana: "yellow",
        kiwi_macro: function() { return this.kiwiColor },
        apple: function() {},
        kiwiColor: "green"
    };
    res.data.date = new Date();
    res.data.banana = "yellow";
    res.data.kiwi_macro = function() { return "green" };
    res.data.apple = function() {};
    session.data.banana = "yellow";
    session.data.kiwi_macro = function() { return "green" };
    session.data.apple = function() {};
};

var testCommentMacro = function() {
    var skin = createSkin("<% // this.foo bar=<% this.foobar %> FIXME %>ok");
    var result = renderSkinAsString(skin);
    assertEqual(result, "ok");
}

var testDeepResolve = function() {
    res.handlers.deep = {
       getMacroHandler: function(name) {
          if (name != "foo") return null;
          return {
              bar_macro: function() {
                  return "ok";
              }
          }
       }
    }
    var result = renderSkinAsString(createSkin("<% deep.foo.bar %>"));
    assertEqual(result, "ok");
};

var testDeepUnhandled = function() {
    res.handlers.deep = {
       getMacroHandler: function(name) {
          if (name != "foo") return null;
          return {
              onUnhandledMacro: function(name) {
                  if (name == "bar") return "ok";
              }
          }
       }
    }
    var result = renderSkinAsString(createSkin("<% deep.foo.bar %>"));
    assertEqual(result, "ok");
};

var testDeep = function() {
    res.handlers.deep = {
       foo: {
           bar: "ok"
       }
    }
    var result = renderSkinAsString(createSkin("<% deep.foo.bar %>"));
    assertEqual(result, "ok");
};

var testDeepFail = function() {
    var result = renderSkinAsString(createSkin("<% root.foo.bar %>"));
    assertStringContains(result, "Unhandled");
    var result = renderSkinAsString(createSkin("<% root.foo.bar failmode=silent %>"));
    assertEqual(result, "");
};

var testDeepFailSilent = function() {
    res.handlers.deep = {
        foo: {}
    };
    var result = renderSkinAsString(createSkin("<% deep.foo.bar %>"));
    assertEqual(result, "");
    var result = renderSkinAsString(createSkin("<% deep.foo.bar failmode=verbose %>"));
    assertStringContains(result, "Unhandled");
};


var testJavaProp = function() {
    var result = renderSkinAsString(createSkin("<% color.green %>"));
    assertEqual(result, "255");
    result = renderSkinAsString(createSkin("<% color.red %>"));
    assertEqual(result, "0");
};

var testJavaMissing = function() {
    var result = renderSkinAsString(createSkin("<% colo.foo %>"));
    assertStringContains(result, "Unhandled");
};

var testJavaMissingSilent = function() {
    var result = renderSkinAsString(createSkin("<% color.foo failmode=silent default=ok %>"));
    assertEqual(result, "ok");
};

var testJavaMissingVerbose = function() {
    var result = renderSkinAsString(createSkin("<% color.foo failmode=verbose %>"));
    assertStringContains(result, "Unhandled");
}

var testUndefinedHandler = function() {
    var result = renderSkinAsString(createSkin("<% file.parentFile %>"));
    assertEqual(result, "");
};

var testJSProp = function() {
    var result = renderSkinAsString(createSkin("<% jsobject.banana %>"));
    assertEqual(result, "yellow");
};

var testJSMacro = function() {
    var result = renderSkinAsString(createSkin("<% jsobject.kiwi %>"));
    assertEqual(result, "green");
};

var testJSFunction = function() {
    var result = renderSkinAsString(createSkin("<% jsobject.apple failmode=silent %>"));
    assertEqual(result, "");
};

var testJSMissing = function() {
    var result = renderSkinAsString(createSkin("<% jsobject.papaya %>"));
    assertEqual(result, "");
};

var testJSMissingSilent = function() {
    var result = renderSkinAsString(createSkin("<% jsobject.papaya failmode=silent %>"));
    assertEqual(result, "");
};

var testJSMissingVerbose = function() {
    var result = renderSkinAsString(createSkin("<% jsobject.papaya failmode=verbose %>"));
    assertStringContains(result, "Unhandled");
};

var testDateFilter = function() {
    var result = renderSkinAsString(createSkin("<% root.date | isDate %>"));
    assertEqual(result, "true");
};
        
var testNestedRootMacro = function() {
    var skin = "<% echo <% root.date %> | isRootDate %>"; 
    var result = renderSkinAsString(createSkin(skin));
    assertEqual(result, "true");
}

var testNestedParamMacro = function() {
    var skin = "<% echo <% param.date %> | isDate %>"; 
    var result = renderSkinAsString(createSkin(skin), { date: new Date() });
    assertEqual(result, "true");
}
            

var testNestedResponseMacro = function() {
    var skin = "<% echo what=<% response.date %> | isResponseDate %>"; 
    var result = renderSkinAsString(createSkin(skin));
    assertEqual(result, "true");
};

var testNestedPrefixSuffix = function() {
    var skin = "<% root.macro prefix=<% root.string %> suffix=<% root.macro %> %>";
    var result = renderSkinAsString(createSkin(skin));
    // root.macro changes suffix to "."
    assertEqual(result, "rootroot.");
}

var testResponseProp = function() {
    var result = renderSkinAsString(createSkin("<% response.banana %>"));
    assertEqual(result, "yellow");
};

var testResponseMacro = function() {
    var result = renderSkinAsString(createSkin("<% response.kiwi %>"));
    assertEqual(result, "green");
};

var testResponseFunction = function() {
    var result = renderSkinAsString(createSkin("<% response.apple failmode=silent %>"));
    assertEqual(result, "");
};

var testResponseMissing = function() {
    var result = renderSkinAsString(createSkin("<% response.papaya %>"));
    assertEqual(result, "");
};

var testResponseMissingSilent = function() {
    var result = renderSkinAsString(createSkin("<% response.papaya failmode=silent %>"));
    assertEqual(result, "");
};

var testResponseMissingVerbose = function() {
    var result = renderSkinAsString(createSkin("<% response.papaya failmode=verbose %>"));
    assertStringContains(result, "Unhandled");
};

var testRootProp = function() {
    var result = renderSkinAsString(createSkin("<% root.string %>"));
    assertEqual(result, "root");
};

var testRootMacro = function() {
    var result = renderSkinAsString(createSkin("<% root.macro %>"));
    assertEqual(result, "root");
};

var testRootMissing = function() {
    var result = renderSkinAsString(createSkin("<% root.undefinedmacro %>"));
    assertStringContains(result, "Unhandled");
};

var testRootMissingSilent = function() {
    var result = renderSkinAsString(createSkin("<% root.undefinedmacro failmode=silent %>"));
    assertEqual(result, "");
};

var testRootMissingVerbose = function() {
    var result = renderSkinAsString(createSkin("<% root.undefinedmacro failmode=verbose %>"));
    assertStringContains(result, "Unhandled");
};

var testSessionProp = function() {
    var result = renderSkinAsString(createSkin("<% session.banana %>"));
    assertEqual(result, "yellow");
};

var testSessionMacro = function() {
    var result = renderSkinAsString(createSkin("<% session.kiwi %>"));
    assertEqual(result, "green");
};

var testSessionFunction = function() {
    var result = renderSkinAsString(createSkin("<% session.apple failmode=silent %>"));
    assertEqual(result, "");
};

var testSessionMissingDefault = function() {
    var result = renderSkinAsString(createSkin("<% session.papaya default=nope %>"));
    assertEqual(result, "nope");
};

var testSessionMissing = function() {
    var result = renderSkinAsString(createSkin("<% session.papaya %>"));
    assertEqual(result, "");
};

var testSessionMissingSilent = function() {
    var result = renderSkinAsString(createSkin("<% session.papaya failmode=silent %>"));
    assertEqual(result, "");
};

var testSessionMissingVerbose = function() {
    var result = renderSkinAsString(createSkin("<% session.papaya failmode=verbose %>"));
    assertStringContains(result, "Unhandled");
};

var testSessionDeepMissing = function() {
    var result = renderSkinAsString(createSkin("<% session.user.name %>"));
    assertStringContains(result, "Unhandled");
    // assertEqual(result, "");
};

var testSubskins = function() {
    var result = renderSkinAsString("subskins");
    assertEqual(result, "mainskin");
    result = renderSkinAsString("subskins#subskin1");
    assertEqual(result, "subskin1");
    result = renderSkinAsString("subskins#subskin2");
    assertEqual(result, "subskin2");
};
