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
 * a global variable containing the form instance
 * @type jala.Form
 */
var form;


/**
 * Create and configure the form object
 */
var setup = function() {
   form = jala.Form.create(getConfig(), new DataObject());
   // form.render();    // show the test form
   return;
};


/**
 * Test the form rendering mechanism
 */
var testFormRender = function() {
   var html = new jala.HtmlDocument(form.renderAsString());
   var list = html.getAll("*");

   var idx = 2;
   assertEqual(list[idx].name, "form");
   assertAttribute(list[idx].attributes, "id", "test");
   assertAttribute(list[idx].attributes, "class", "form");
   assertAttribute(list[idx].attributes, "name", "test");
   assertAttribute(list[idx].attributes, "enctype", "multipart/form-data");
   assertAttribute(list[idx].attributes, "method", "post");

   
   // alias / input
   assertEqual(list[++idx].name, "div");
   assertAttribute(list[idx].attributes, "id", "testAlias");
   assertAttribute(list[idx].attributes, "class", "component require");

   assertEqual(list[++idx].name, "label");
   assertAttribute(list[idx].attributes, "for", "testAliasControl");

   assertEqual(list[++idx].name, "input");
   assertAttribute(list[idx].attributes, "id", "testAliasControl");
   assertAttribute(list[idx].attributes, "class", "input");
   assertAttribute(list[idx].attributes, "type", "text");
   assertAttribute(list[idx].attributes, "maxlength", "10");
   assertAttribute(list[idx].attributes, "name", "alias");
   assertAttribute(list[idx].attributes, "size", "20");

   assertEqual(list[++idx].name, "div");
   assertEqual(list[idx].value, "Enter alias.");
   assertAttribute(list[idx].attributes, "class", "helpText");


   // desc / textarea
   assertEqual(list[++idx].name, "div");
   assertAttribute(list[idx].attributes, "id", "testDesc");
   assertAttribute(list[idx].attributes, "class", "component require");

   assertEqual(list[++idx].name, "label");
   assertAttribute(list[idx].attributes, "for", "testDescControl");

   assertEqual(list[++idx].name, "textarea");
   assertAttribute(list[idx].attributes, "id", "testDescControl");
   assertAttribute(list[idx].attributes, "class", "textarea");
   assertAttribute(list[idx].attributes, "name", "desc");
   assertAttribute(list[idx].attributes, "cols", "30");
   assertAttribute(list[idx].attributes, "rows", "3");



   // pushdate / date
   assertEqual(list[++idx].name, "div");
   assertAttribute(list[idx].attributes, "id", "testPushdate");
   assertAttribute(list[idx].attributes, "class", "component require");

   assertEqual(list[++idx].name, "label");
   assertAttribute(list[idx].attributes, "for", "testPushdateControl");

   assertEqual(list[++idx].name, "input");
   assertAttribute(list[idx].attributes, "id", "testPushdateControl");
   assertAttribute(list[idx].attributes, "class", "date");
   assertAttribute(list[idx].attributes, "type", "text");
   assertAttribute(list[idx].attributes, "name", "pushdate");


   // isonline / checkbox
   assertEqual(list[++idx].name, "div");
   assertAttribute(list[idx].attributes, "id", "testIsonline");
   assertAttribute(list[idx].attributes, "class", "component optional");

   assertEqual(list[++idx].name, "label");
   assertAttribute(list[idx].attributes, "for", "testIsonlineControl");

   assertEqual(list[++idx].name, "input");
   assertAttribute(list[idx].attributes, "id", "testIsonlineControl");
   assertAttribute(list[idx].attributes, "type", "checkbox");
   assertAttribute(list[idx].attributes, "class", "checkbox");
   assertAttribute(list[idx].attributes, "name", "isonline");
   assertAttribute(list[idx].attributes, "value", "1");


   // category / select
   assertEqual(list[++idx].name, "div");
   assertAttribute(list[idx].attributes, "id", "testCategory");
   assertAttribute(list[idx].attributes, "class", "component optional");

   assertEqual(list[++idx].name, "label");
   assertAttribute(list[idx].attributes, "for", "testCategoryControl");

   assertEqual(list[++idx].name, "select");
   assertAttribute(list[idx].attributes, "id", "testCategoryControl");
   assertAttribute(list[idx].attributes, "class", "select");
   assertAttribute(list[idx].attributes, "name", "category");
   assertAttribute(list[idx].attributes, "size", "1");

   assertEqual(list[++idx].name, "option");
   assertAttribute(list[idx].attributes, "value", "cat0");

   assertEqual(list[++idx].name, "option");
   assertAttribute(list[idx].attributes, "value", "cat1");

   assertEqual(list[++idx].name, "option");
   assertAttribute(list[idx].attributes, "value", "cat2");

   assertEqual(list[++idx].name, "option");
   assertAttribute(list[idx].attributes, "value", "cat3");


   // fieldset
   assertEqual(list[++idx].name, "fieldset");

   assertEqual(list[++idx].name, "legend");
   assertEqual(list[idx].value, "a fieldset");


   // fileupload
   assertEqual(list[++idx].name, "div");
   assertAttribute(list[idx].attributes, "id", "testFileupload");
   assertAttribute(list[idx].attributes, "class", "component optional");

   assertEqual(list[++idx].name, "label");
   assertAttribute(list[idx].attributes, "for", "testFileuploadControl");

   assertEqual(list[++idx].name, "input");
   assertAttribute(list[idx].attributes, "id", "testFileuploadControl");
   assertAttribute(list[idx].attributes, "class", "file");
   assertAttribute(list[idx].attributes, "type", "file");
   assertAttribute(list[idx].attributes, "accept", "application/msword");
   assertAttribute(list[idx].attributes, "name", "fileupload");


   // imageupload
   assertEqual(list[++idx].name, "div");
   assertAttribute(list[idx].attributes, "id", "testImageupload");
   assertAttribute(list[idx].attributes, "class", "component optional");

   assertEqual(list[++idx].name, "label");
   assertAttribute(list[idx].attributes, "for", "testImageuploadControl");

   assertEqual(list[++idx].name, "input");
   assertAttribute(list[idx].attributes, "id", "testImageuploadControl");
   assertAttribute(list[idx].attributes, "class", "image");
   assertAttribute(list[idx].attributes, "type", "file");
   assertAttribute(list[idx].attributes, "name", "imageupload");


   // submit
   assertEqual(list[++idx].name, "div");
   assertAttribute(list[idx].attributes, "id", "testSubmit");
   assertAttribute(list[idx].attributes, "class", "component");

   assertEqual(list[++idx].name, "input");
   assertAttribute(list[idx].attributes, "id", "testSubmitControl");
   assertAttribute(list[idx].attributes, "class", "submit");
   assertAttribute(list[idx].attributes, "name", "submit");
   assertAttribute(list[idx].attributes, "value", "Submit this form");
   assertAttribute(list[idx].attributes, "type", "submit");


   // cancel
   assertEqual(list[++idx].name, "div");
   assertAttribute(list[idx].attributes, "id", "testCancel");
   assertAttribute(list[idx].attributes, "class", "component");

   assertEqual(list[++idx].name, "input");
   assertAttribute(list[idx].attributes, "id", "testCancelControl");
   assertAttribute(list[idx].attributes, "class", "button");
   assertAttribute(list[idx].attributes, "name", "cancel");
   assertAttribute(list[idx].attributes, "value", "Cancel edit");
   assertAttribute(list[idx].attributes, "type", "button");

   return;
}


/**
 * Test the form validation mechanism
 */
var testFormValidate = function() {
   var reqData = getRequestData();

   // default userinput values that should validate
   var tracker = form.validate(reqData);
   assertFalse(tracker.hasError());

   // now try invalid values in userinput:
   reqData["alias"] = "a";
   reqData["desc"]  = "";
   reqData["pushdate"] = "17.5.2007";
   reqData["category"] = "invalidOption";
   tracker = form.validate(reqData);
   assertTrue(tracker.hasError());
   assertEqual(tracker.errors["alias"], "Alias is too short.");
   assertEqual(tracker.errors["desc"], "Please enter text into this field.");
   assertEqual(tracker.errors["pushdate"], "This date cannot be parsed.");
   assertEqual(tracker.errors["category"], "Please select a valid option.");

   // reset to default userinput:
   reqData = getRequestData();
   // require a smaller image:
   form.components.uploadfieldset.components.imageupload.require("maxwidth", 100, "Maximum width exceeded.");
   tracker = form.validate(reqData);
   assertTrue(tracker.hasError());
   assertEqual(tracker.errors["imageupload"], "Maximum width exceeded.");
   // undo image restriction:
   form.components.uploadfieldset.components.imageupload.require("maxwidth", 200, "Maximum width exceeded.");
   tracker = form.validate(reqData);
   assertFalse(tracker.hasError());
   
   return;
};


/**
 * Test the form rendering mechanism in the case of an error
 */
var testFormRenderWithError = function() {
   var reqData = getRequestData();
   reqData["alias"] = "a";
   var tracker = form.validate(reqData);

   var html = new jala.HtmlDocument(form.renderAsString());
   var list = html.getAll("*");
   assertEqual(list[4].name, "div");
   assertEqual(list[4].value, "Alias is too short.");
   assertAttribute(list[4].attributes, "class", "errorText");
};


/**
 * Test the form save mechanism
 */
var testFormSave = function() {
   var dataObj = form.getDataObject();

   var reqData = getRequestData();
   var tracker = form.validate(reqData);
   assertFalse(tracker.hasError());
   form.save();
   assertEqual(dataObj.alias, "aliasValue");
   assertEqual(dataObj.getProperty("desc"), "descriptionValue");
   assertEqual(dataObj.pushdate.toString(), new Date(2007, 4, 17, 11, 32, 0).toString());
   assertEqual(dataObj.isonline, 1);
   assertEqual(dataObj.getProperty("category"), "cat2");
   
   return;
}



/**
 * Helper function to dump an html element to the response
 * @param {Object} el
 */
var debugElement = function(el) {
   res.write("<b>" + el.name + "</b> (" + el.value + ")<br/>");
   if (el.attributes) {
      var attrList = el.attributes;
      for (var i=0; i<attrList.length; i++) {
         res.write(attrList[i].name + " = " + attrList[i].value + "<br/>");
      }
   }
};


/**
 * Helper function to assert that a given attribute exists
 * in an element
 * @param {Array} attrList Array with attribute objects
 * @param {String} name Name of attribute
 * @param {String} value Value of attribute
 */
var assertAttribute = function(attrList, name, value) {
   for (var i=0; i<attrList.length; i++) {
      if (attrList[i].name == name) {
         if (attrList[i].value != value) {
            throw new jala.Test.TestException("",
               "assertAttribute: Expected value of attribute \"" + name + "\" to be equal to \"" + value + "\" (but it is \"" + attrList[i].value + "\" instead).");
         }
         return;
      }
   }
   throw new jala.Test.TestException("", "assertAttribute: Attribute " + name + " not included in attributes.");
};


var DataObject = function() {
   var props = {};
   this.setProperty = function (name, value) {
      props[name] = value;
   };
   this.getProperty = function(name) {
      return props[name];
   };
   return this;
};


var getRequestData = function() {
   var fileupload = "Form.fileupload.doc";
   var imageupload = "Form.imageupload.jpg";
   return {
      alias:         "aliasValue",
      desc:          "descriptionValue",
      pushdate:      "17.5.2007 11:32",
      category:      "cat2",
      isonline:      "1",
      test_submit:    "Submit",
      imageupload: new Packages.helma.util.MimePart(
         imageupload,
         jala.Test.getTestFile(imageupload).toByteArray(),
         "image/jpeg"
      ),
      fileupload: new Packages.helma.util.MimePart(
         fileupload,
         jala.Test.getTestFile(fileupload).toByteArray(),
         "application/msword"
      )
   };
};


var getConfig = function() {
   return   {
   name:             "test",
   className:        "form",
   components:[
      {
         // name:       "alias",    deliberately left out, should construct name from label
         label:      "Alias",
         help:       "Enter alias.",
         minlength:  4,
         maxlength:  10,
         require:    true,
         messages:   {
            require:   "Alias is required.",
            maxlength: "Alias is too long.",
            minlength: "Alias is too short."
         }
      },
      {
         name:       "desc",
         type:       "textarea",
         rows:       3,
         cols:       30,
         require:    true,
         getter:     function(name) {
            return this.getProperty(name);
         },
         setter:     function(name, val) {
            return this.setProperty(name, val);
         }
      },
      {
         name:       "pushdate",
         type:       "date",
         dateFormat: "d.M.yyyy H:m",
         require:    true
      },
      {
         name:       "isonline",
         type:       "checkbox"
      },
      {  name:       "category",
         type:       "select",
         options:    function() { 
            var arr = [];
            for (var i=0; i<4; i++) {
               arr[arr.length] = {
                  value: "cat" + i,
                  display: "Category " + i
               };
            }
            return arr;
         },
         getter:     function(name) {
            return this.getProperty("category");
         },
         setter:     function(name, value) {
            this.setProperty("category", value);
         }
      },
      {
         name:       "uploadfieldset",
         type:       "fieldset",
         legend:     "a fieldset",
         components:[
            {
               name:       "fileupload",
               type:       "file",
               maxlength:   500000,
               contenttype: "application/msword",
            },
            {
               name:       "imageupload",
               type:       "image",
               minwidth:   10,
               maxwidth:   200,
               minheight:  10,
               maxheight:  200
            }
         ]
      },
      {
         name:       "submit",
         type:       "submit",
         value:      "Submit this form"
      },
      {
         name:       "cancel",
         type:       "button",
         value:      "Cancel edit"
      }
   ]};
};

