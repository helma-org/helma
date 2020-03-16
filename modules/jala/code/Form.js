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
 * @fileoverview This class can be used to render forms and to validate
 * and store user submits. Further types of form components can be added
 * by subclassing jala.Form.Component.Input.
 */


// Define the global namespace for Jala modules
if (!global.jala) {
   global.jala = {};
}

/**
 * HelmaLib dependencies
 */
app.addRepository("modules/core/Object.js");
app.addRepository("modules/helma/Html.js");
app.addRepository("modules/helma/Http.js");

/**
 * Jala dependencies
 */
app.addRepository(getProperty("jala.dir", "modules/jala") + "/code/I18n.js");

/**
 * Constructs a new Form instance
 * @class A class that renders forms, validates submitted form data and
 * stores the data in a specified object.
 * @param {String} name The name of the form
 * @param {Object} dataObj An optional object used to retrieve values
 * to display in the form input fields contained in this Form instance.
 * @returns A newly created Form instance
 * @constructor
 */
jala.Form = function(name, dataObj) {

   /**
    * Private field containing the tracker used during validation
    * @type jala.Form.Tracker
    * @private
    */
   var tracker = undefined;
   
   /**
    * Private field containing all components of this form
    * @type Array
    * @private
    */
   var components = [];

   /**
    * Private field containing the CSS class name of this form instance.
    * @type String
    */
   var className;

   /**
    * Private field containing the default error message
    * @type String
    * @private
    */
   var errorMessage;

   /**
    * Readonly reference to the name of the form
    * @type String
    */
   this.name = name;     // for doc purposes only, readonly-access through the getter function
   this.__defineGetter__("name", function() {   return name;   });

   /**
    * Sets the data object which is being edited by this form. This object
    * is used to get the default values when first printing the form and 
    * - if no other object is provided - receives the changed values in save.
    * @param {Object} dataObj The object which is being edited by this form.
    * @see #save
    */
   this.setDataObject = function(newDataObj) {
      dataObj = newDataObj;
      return;
   };

   /**
    * Returns the data object containing the values used
    * for rendering the form.
    * @returns The data object of this jala.Form instance
    */
   this.getDataObject = function() {
      return dataObj;
   };


   /**
    * The default getter function for this form. Unless a getter
    * is specified for the component, this function is called
    * to retrieve the original value of a field.
    * When called, the scope is set to the data object and
    * the name of the element is the sole argument.
    * @see jala.Form.Component.Input#getValue
    * @type Function
    */
   this.getter;      // for doc purposes only

   // that's where the value really is stored:
   var getter = jala.Form.propertyGetter;

   this.__defineGetter__("getter", function() {   return getter;   });
   this.__defineSetter__("getter", function(newGetter) {
      if (newGetter instanceof Function) {
         getter = newGetter;
      }
   });


   /**
    * The default setter function for this form. Unless a getter
    * is specified for the component, this function is called to
    * store the a value of a field.
    * When called, the scope is set to the data object and
    * the name and value of the element are provided as arguments.
    * @see jala.Form.Component.Input#setValue
    * @type Function
    */
   this.setter;      // for doc purposes only

   // that's where the value really is stored:
   var setter = jala.Form.propertySetter;

   this.__defineGetter__("setter", function() {   return setter;   });
   this.__defineSetter__("setter", function(newSetter) {
      if (newSetter instanceof Function) {
         setter = newSetter;
      }
   });

   var tracker = undefined;
   
   /**
    * Sets the tracker object this form instance uses for collecting
    * error messages and parsed values.
    * @param {jala.Form.Tracker} newTracker
    */
   this.setTracker = function(newTracker) {
      if (newTracker instanceof jala.Form.Tracker){
         tracker = newTracker;
      }
      return;
   };

   /**
    * Returns the tracker object this form instance uses for collecting
    * error messages and parsed values.
    * @returns tracker object
    * @type jala.Form.Tracker
    */
   this.getTracker = function() {
      return tracker;
   };

   /**
    * Contains the default component skin
    * @type Skin
    */
   this.componentSkin = createSkin("<% param.error prefix=' ' suffix='\n' %><% param.label prefix=' ' suffix='\n' %><% param.controls prefix=' ' suffix='\n' %><% param.help prefix=' ' suffix='\n' %>");

   /**
    * Contains a map of component objects.
    * @type Object
    */
   this.components = {};

   /**
    * Returns an array containing the components
    * of this jala.Form instance.
    * @returns The components of this jala.Form instance.
    * @type Array
    */
   this.listComponents = function() {
      return components;
   };

   /**
    * Adds a component to this jala.Form instance
    * @param {jala.Form.Component.Input} component
    */
   this.addComponent = function(component) {
      component.setForm(this);
      components.push(component);
      this.components[component.name] = component;
      return;
   };

   /**
    * Returns true if this instance of jala.Form contains at least
    * one component doing a file upload.
    * @see jala.Form.Component#containsFileUpload
    * @type Boolean
    */
   this.containsFileUpload = function() {
      for (var i=0; i<components.length; i++) {
         if (components[i].containsFileUpload() == true) {
            return true;
         }
      }
      return false;
   };

   /**
    * Returns the class name set for this form instance.
    * @returns class name
    * @type String
    */
   this.getClassName = function() {
      return className;
   };

   /**
    * Sets an extra classname for this form instance
    * @param {String} newClassName new classname
    */
   this.setClassName = function(newClassName) {
      className = newClassName;
      return;
   };

   /**
    * Returns the general error message printed above the form
    * if any of the components didn't validate.
    * @returns error message
    * @type String
    */
   this.getErrorMessage = function() {
      return errorMessage;
   };

   /**
    * Sets the general error message printed above the form if any
    * of the components didn't validate.
    * @param {String} newErrorMessage error message
    */
   this.setErrorMessage = function(newErrorMessage) {
      errorMessage = newErrorMessage;
      return;
   };

   /**
    * Returns true if this instance of jala.Form holds a jala.Form.Tracker
    * instance and at least one error has been set on this tracker.
    * @returns true if an error has been encountered.
    * @type Boolean
    */
   this.hasError = function() {
      if (tracker) {
         return tracker.hasError();
      }
      return false;
   };

   /**
    * If this instance of jala.Form holds a jala.Form.Tracker
    * instance it returns the number of components that didn't
    * validate.
    * @returns Number of components that didn't validate.
    * @type Number
    */
   this.countErrors = function() {
      if (tracker) {
         return tracker.countErrors();
      }
      return 0;
   };

   /**
    * Main constructor body
    */
   if (!dataObj) {
      dataObj = {};
   }

   return this;
};

/** @ignore */
jala.Form.prototype.toString = function() {
   return "[jala.Form]";
};

/**
 * The HTML renderer used by jala.Form
 * @type helma.Html
 */
jala.Form.html = new helma.Html();

/**
 * Constant used by require function to define that a component
 * should not validate if userinput is shorter than a given length.
 * Value: "minlength"
 * @type String
 * @final
 */
jala.Form.MINLENGTH     = "minlength";

/**
 * Constant used by require function to define that a component
 * should not validate if userinput exceeds a maximum length.
 * Value: "maxlength"
 * @type String
 * @final
 */
jala.Form.MAXLENGTH     = "maxlength";

/**
 * Constant used by require function to define that a component
 * should validate only if the user did provide input.
 * Value: "require"
 * @type String
 * @final
 */
jala.Form.REQUIRE       = "require";

/**
 * Constant used by require function to define that a select or
 * radio component should validate only if the user input is contained
 * in the list of options provided.
 * Value: "checkoptions"
 * @type String
 * @final
 */
jala.Form.CHECKOPTIONS  = "checkoptions";

/**
 * Constant used by require function to define that a file upload
 * component should validate only if the file's content type is
 * in the list of allowed content types provided.
 * Value: "contenttype"
 * @type String
 * @final
 */
jala.Form.CONTENTTYPE   = "contenttype"; 

/**
 * Constant used by require function to define that an image upload
 * component should validate only if the image's width is less than
 * the value provided.
 * Value: "maxwidth"
 * @type String
 * @final
 */
jala.Form.MAXWIDTH      = "maxwidth";

/**
 * Constant used by require function to define that an image upload
 * component should validate only if the image's width is more than
 * the value provided.
 * Value: "minwidth"
 * @type String
 * @final
 */
jala.Form.MINWIDTH      = "minwidth";

/**
 * Constant used by require function to define that an image upload
 * component should validate only if the image's height is less than
 * the value provided.
 * Value: "maxheight"
 * @type String
 * @final
 */
jala.Form.MAXHEIGHT     = "maxheight";

/**
 * Constant used by require function to define that an image upload
 * component should validate only if the image's height is more than
 * the value provided.
 * Value: "min-height"
 * @type String
 * @final
 */
jala.Form.MINHEIGHT     = "minheight";

/**
 * Utility to set up the prototype, constructor, superclass and superconstructor
 * properties to support an inheritance strategy that can chain constructors and methods.
 * @param {Function} subClass the object which inherits superClass' functions
 * @param {Function} superClass the object to inherit
 */
jala.Form.extend = function(subClass, superClass) {
   var f = function() {};
   f.prototype = superClass.prototype;

   subClass.prototype = new f();
   subClass.prototype.constructor = subClass;
   subClass.superClass = superClass.prototype;
   subClass.superConstructor = superClass;
   return;
};

/**
 * Parses a plain javascript object tree and configures a
 * new jala.Form instance according to the properties.
 * Propertynames are matched with constants and setter-functions,
 * the property "type" is used to create new component objects.
 * @param {Object} config object tree containing config
 * @returns A newly created jala.Form instance based on the config specified
 * @type jala.Form
 */
jala.Form.create = function(config, dataObj) {
   if (!config || !config.name || !config.components) {
      return null;
   }
   var form = new jala.Form(config.name, dataObj);
   if (config.legend) {
      form.setLegend(config.legend);
   }
   if (config.className) {
      form.setClassName(config.className);
   }
   if (config.errorMessage) {
      form.setErrorMessage(config.errorMessage);
   }
   if (config.getter) {
      form.getter = config.getter;
   }
   if (config.setter) {
      form.setter = config.setter;
   }
   if (config.components) {
      jala.Form.createComponents(form, config.components);
   }
   return form;
};

/**
 * Parses an array of plain js objects and tries to create components.
 * @param {jala.Form | jala.Form.Component.Fieldset} container
 *        Object whose addComponent method is used for adding new components.
 * @param {Array} arr Array of plain javascript objects used as config.
 * @private
 */
jala.Form.createComponents = function(container, arr) {
   var form = (container.form) ? container.form : container;
   var components = [];
   var element;
   for (var i=0; i<arr.length; i++) {
      element = arr[i];
      var clazzName = (element["type"]) ? element["type"].titleize() : "Input";
      var constr = jala.Form.Component[clazzName];
      if (!constr) {
         // invalid constructor:
         var logStr = "jala.Form encountered unknown component type " + element["type"] + " in config of form ";
         logStr += (container.form) ? container.form.name : container.name;
         app.log(logStr);
         continue;
      }
      var name = element.name;
      if (!name && element.label) {
         name = element.label.toAlphanumeric().toLowerCase();
      } else if (!name && constr == jala.Form.Component.Fieldset) {
         var str = "fieldset";
         while(container.components[str]) {
            str += "1";
         }
         name = str;
      } else if (!name) {
         // couldn't find a name for the component:
         var logStr = "jala.Form encountered component of type " + clazzName.toLowerCase() + " without name or label property in config of form ";
         logStr += (container.form) ? container.form.name : container.name;
         app.log(logStr);
         continue;
      }
      var component = new constr(name);
      component.setForm(form);
      for (var key in element) {
         switch(key) {
            case "name":
            case "type":
               break;
            case "messages":
               for (var msgName in element[key]) {
                  component.setMessage(msgName, element[key][msgName]);
               }
               break;
            case "components":
               jala.Form.createComponents(component, element[key]);
               break;
            case "getter":
            case "setter":
            case "validator":
               component[key] = element[key];
               break;
            case jala.Form.REQUIRE:
               component.require(key, element[key]);
               break;
            default:
               // check if key matches a constant:
               if (jala.Form[key.toUpperCase()] == key.toLowerCase()) {
                  component.require(key.toLowerCase(), element[key]);
               } else {
                  // call setter functions for all fields from config object:
                  // note: String.prototype.titleize from the helma.core module
                  // would uppercase the first letter, but lowercases all ensuing
                  // characters (maxLength would become Maxlength).
                  // note: use try/catch to detect if the setter method really exists
                  // because a check using if(component[method]) would fail for
                  // inherited methods even though executing the inherited method works.
                  try {
                     component["set" + key.charAt(0).toUpperCase() + key.substring(1)](element[key]);
                  } catch (e) {
                     // invalid field for this component
                     app.log("jala.Form encountered unknown field " + key + " in config of form " + component.form.name);
                  }
               }
               break;
         }
      }
      container.addComponent(component);
   }
   return;
};

/**
 * Static validator function to test values for being a valid email address.
 * @param {String} name name of the property being validated.
 * @param {String} value value in form input
 * @param {Object} reqData the whole request-data-object,
           in case properties depend on each other
 * @param {jala.Form} formObj instance of jala.Form
 * @returns Error message or null
 * @type String
 */
jala.Form.isEmail = function(name, value, reqData, formObj) {
   if (value && !value.trim().isEmail()) {
      return "Please enter a valid email address.";
   }
   return null;
};

/**
 * Static validator function to test values for being a valid url.
 * @param {String} name name of the property being validated.
 * @param {String} value value in form input
 * @param {Object} reqData the whole request-data-object,
           in case properties depend on each other
 * @param {jala.Form} formObj instance of jala.Form
 * @returns Error message or null
 * @type String
 */
jala.Form.isUrl = function(name, value, reqData, formObj) {
   if (value && !helma.Http.evalUrl(value)) {
      return "Please enter a valid URL (web address).";
   }
   return null;
};

/**
 * Renders the opening form tag
 * @private
 */
jala.Form.prototype.renderFormOpen = function() {
   var formAttr = {
      id     : this.createDomId(),
      name   : this.name,
      action : (req.action == "main") ? "" : req.action,
      method : "post"
   };
   var className = this.getClassName();
   if (className) {
      formAttr["class"] = className;
   }
   if (this.containsFileUpload()) {
      // if there is an upload element, use multipart-enctype
      formAttr.enctype = "multipart/form-data";
   }
   jala.Form.html.openTag("form", formAttr);
   return;
};

/**
 * Renders this form including all components to response.
 */
jala.Form.prototype.render = function() {

   this.renderFormOpen();
   res.write("\n");
   
   // print optional general error message
   var errorMessage = this.getErrorMessage();
   if (this.hasError() && errorMessage) {
      jala.Form.html.element(
         "div",
         gettext(errorMessage, this.countErrors()),
         {id: this.createDomId("error"), "class": "formError"}
      );
      res.write("\n");
   }

   // loop through elements
   var components  = this.listComponents();
   for (var i=0; i<components.length; i++) {
      components[i].render();
   }

   jala.Form.html.closeTag("form");
   return;
};

/**
 * renders the form as a string
 * @returns rendered form
 * @type String
 */
jala.Form.prototype.renderAsString = function(param) {
   res.push();
   this.render(param);
   return res.pop();
};

/**
 * Creates a DOM identifier based on the arguments passed. The
 * resulting Id will be prefixed with the name of the form.
 * All arguments will be chained using camel casing.
 * @returns The DOM Id
 * @type String
 */
jala.Form.prototype.createDomId = function(/* [part1][, part2][, ...] */) {
   res.push();
   res.write(this.name.charAt(0).toLowerCase());
   res.write(this.name.substring(1));
   for (var i=0;i<arguments.length;i++) {
      if (arguments[i]) {
         res.write(arguments[i].charAt(0).toUpperCase());
         res.write(arguments[i].substring(1));
      }
   }
   return res.pop();
};

/**
 * Validates user input from a submitted form by calling each
 * component's validate method.
 * @param {Object} reqData Optional submitted form data. If not specified
 * req.data is used.
 * @returns tracker object with error fields set.
 * @type jala.Form.Tracker
 */
jala.Form.prototype.validate = function(reqData) {
   var tracker = new jala.Form.Tracker(reqData || req.data);
   var components = this.listComponents();
   for (var i=0; i<components.length; i++) {
      components[i].validate(tracker);
   }
   this.setTracker(tracker);
   return tracker;
};

/**
 * Sets the parsed values on an object. By default the internally 
 * stored tracker and data objects are used, but those may be 
 * overridden here.
 * @param {jala.Form.Tracker} tracker (optional) tracker object
 *       holding parsed data from form input.
 * @param {Object} destObj (optional) object whose values will be changed.
 *       By default the dataObj passed to the constructor or to
 *       setDataObject is used.
 */
jala.Form.prototype.save = function(tracker, destObj) {
   tracker = tracker || this.getTracker();
   destObj = destObj || this.getDataObject();
   var components = this.listComponents();
   for (var i=0; i<components.length; i++) {
      components[i].save(tracker, destObj);
   }
   return;
};

/**
 * Parses form input, applies check functions and stores the values
 * if the form does validate. Otherwise this method returns false
 * without saving so that the form can be reprinted with error messages.
 * @param {Object} reqData input from form
 * @param {Object} destObj object whose values should be chanegd
 * @returns False if one of the checks failed,
 *          true if the element was saved correctly.
 * @type Boolean
 */
jala.Form.prototype.handle = function(reqData, destObj) {
   var tracker = this.validate(reqData);
   if (tracker.hasError()) {
      return false;
   } else {
      this.save(tracker, destObj);
      return true;
   }
};

/**
 * Renders the whole form to response
 */
jala.Form.prototype.render_macro = function() {
   this.render();
   return;
};

/**
 * Returns the id (equal to the name) of the form
 * @returns The id of this Form instance
 * @type String
 */
jala.Form.prototype.id_macro = function() {
   res.write(this.name);
   return;
};

/**
 * Returns the name (equal to the id) of the form
 * @returns The name of this Form instance
 * @type String
 */
jala.Form.prototype.name_macro = function() {
   res.write(this.name);
   return;
};

/**
 * Returns the class name of the form
 * @returns The class name of this Form instance
 * @type String
 */
jala.Form.prototype.class_macro = function() {
   var className = this.getClassName();
   if (className) {
      res.write(className);
   }
   return;
};



/**
 * Writes the form opening tag to response
 */
jala.Form.prototype.open_macro = function() {
   this.renderFormOpen();
   return;
};

/**
 * Writes the form closing tag to response
 */
jala.Form.prototype.close_macro = function() {
   jala.Form.html.closeTag("form");
   return;
};

/**
 * The abstract base class for all components.
 * @constructor
 */
jala.Form.Component = function Component(name) {

   if (!name) {
      throw "jala.Form.Component: missing component name";
   }
   
   /**
    * The Form this component belongs to
    * @type jala.Form
    * @private
    */
   var form;

   /**
    * Private field containing the CSS class name of this component
    * @type String
    */
   var className;
   
   /**
    * Readonly reference to name of component
    * @type String
    */
   this.name;     // for doc purposes only, readonly-access is through the getter function
   this.__defineGetter__("name", function() {   return name;   });

   
   /**
    * Readonly reference to instance of jala.Form.
    * @type jala.Form
    */
   this.form;     // for doc purposes only, readonly-access through the getter function
   this.__defineGetter__("form", function() {   return form;   });
   
   /**
    * Attaches this component to an instance of jala.Form.
    * @param {jala.Form} newForm form object
    * @private
    */
   this.setForm = function(newForm) {
      form = newForm;
      return;
   };

   /**
    * Returns the type of component. This is the lowercase'd name of the
    * constructor function.
    * @type String
    */
   this.getType = function() {
      return this.constructor.name.toLowerCase();
   };

   /**
    * Returns the class name set for this component.
    * @returns class name
    * @type String
    */
   this.getClassName = function() {
      return className;
   };

   /**
    * Sets an extra classname for this component
    * @param {String} newClassName new classname
    */
   this.setClassName = function(newClassName) {
      className = newClassName;
      return;
   };

   /**
    * Function defining wheter a component contains a file upload or not.
    * This value is used to render a form tag with the attribute
    * enctype=multipart/form-data.
    * Subclasses of jala.Form.Component that use a file upload element,
    * have to override this function and let it return true.
    * @type Boolean
    */
   this.containsFileUpload = function() {
      return false;
   };
   
   return this;
};

/** @ignore */
jala.Form.Component.prototype.toString = function() {
   return "[" + this.constructor.name + " component '" + this.name + "']";
};

/**
 * Creates a DOM identifier based on the name of the form,
 * the name of the component and an additional string.
 * The items will be chained using camel casing.
 * @param {String} idPart Optional string appended to component's id.
 * @returns The DOM Id
 * @type String
 */
jala.Form.Component.prototype.createDomId = function(idPart) {
   return this.form.createDomId(this.name, idPart);
}

/**
 * Function to render a component.
 * Subclasses of jala.Form.Component may override this function.
 */
jala.Form.Component.prototype.render = function() {
   return;
};

/**
 * Function to validate a component.
 * Subclasses of jala.Form.Component may override this function.
 * @param {jala.Form.Tracker} tracker object tracking errors and holding
 *    parsed values and request data.
 */
jala.Form.Component.prototype.validate = function(tracker) {
   return tracker;
};

/**
 * Function to save the data of a component.
 * Subclasses of jala.Form.Component may override this function.
 */
jala.Form.Component.prototype.save = function(destObj, val) {
   return;
};

/**
 * Constructs a new Fieldset instance
 * @class Instances of this class represent a form fieldset containing
 * numerous form components
 * @param {String} name The name of the fieldset
 * @returns A newly created Fieldset instance
 * @constructor
 */
jala.Form.Component.Fieldset = function Fieldset(name) {
   jala.Form.Component.Fieldset.superConstructor.apply(this, arguments);
   
   /**
    * Private field containing the components of this fieldset
    * @type Array
    * @private
    */
   var components = [];

   /**
    * Private field containing the legend of this fieldset
    * @type String
    * @private
    */
   var legend;

   /**
    * Contains a map of all component objects of this fieldset
    */
   this.components = {};

   /**
    * Returns an array containing the components
    * of this jala.Form.Component.Fieldset instance.
    * @returns The components of this jala.Form instance.
    * @type Array
    */
   this.listComponents = function() {
      return components;
   };

   /**
    * Adds a component to this jala.Form.Component.Fieldset instance
    * @param {jala.Form.Component.Input} component
    */
   this.addComponent = function(component) {
      component.setForm(this.form);
      components.push(component);
      this.components[component.name] = component;
      return;
   };

   /**
    * Returns true if this instance of jala.Form.Component.Fieldset
    * contains at least one component doing a file upload.
    * @see jala.Form.Component#containsFileUpload
    * @type Boolean
    */
   this.containsFileUpload = function() {
      for (var i=0; i<components.length; i++) {
         if (components[i].containsFileUpload() == true) {
            return true;
         }
      }
      return false;
   };

   /**
    * Returns the legend of the fieldset.
    * @returns legend
    * @type String
    */
   this.getLegend = function() {
      return legend;
   };

   /**
    * Sets the legend text.
    * @param {String} newLegend legend to use when printing the fieldset.
    */
   this.setLegend = function(newLegend) {
      legend = newLegend;
      return;
   };

   /**
    * Attaches this fieldset and all components to an instance of
    * jala.Form.
    * @param {jala.Form} newForm form object
    * @private
    */
   this.setForm = function(newForm) {
      form = newForm;
      for (var i=0; i<components.length; i++) {
         components[i].setForm(newForm);
      }
      return;
   };

};
// extend jala.Form.Component
jala.Form.extend(jala.Form.Component.Fieldset, jala.Form.Component);

/**
 * Renders all components within the fieldset.
 */
jala.Form.Component.Fieldset.prototype.render = function() {

   var attr = {};
   var className = this.getClassName();
   if (className) {
      attr["class"] = className;
   }
   jala.Form.html.openTag("fieldset", attr);
   res.write("\n");

   // optional legend
   var legend = this.getLegend();
   if (legend != null) {
      res.write(" ");
      jala.Form.html.element("legend", legend);
      res.write("\n");
   }

   // loop through elements
   var components  = this.listComponents();
   for (var i=0; i<components.length; i++) {
      components[i].render();
   }

   jala.Form.html.closeTag("fieldset");
   res.write("\n");
   return;
};

/**
 * Validates all components within the fieldset.
 * @param {jala.Form.Tracker} tracker
 */
jala.Form.Component.Fieldset.prototype.validate = function(tracker) {
   var components  = this.listComponents();
   for (var i=0; i<components.length; i++) {
      components[i].validate(tracker);
   }
   return;
};

/**
 * Saves all components within the fieldset.
 * @param {jala.Form.Tracker} tracker
 * @param {Object} destObj
 */
jala.Form.Component.Fieldset.prototype.save = function(tracker, destObj) {
   var components  = this.listComponents();
   for (var i=0; i<components.length; i++) {
      components[i].save(tracker, destObj);
   }
   return;
};

/**
 * @class Subclass of jala.Form.Component that allows rendering a skin
 * within a form.
 * @base jala.Form.Component
 * @param {String} name The name of the component, used as the name of the skin
 * @returns A newly created Skin component instance
 * @constructor
 */
jala.Form.Component.Skin = function Skin(name) {
   jala.Form.Component.Skin.superConstructor.apply(this, arguments);
   
   /**
    * Private field containing the handler object
    */
   var handler = undefined;
   
   /**
    * Returns the handler object for the skin.
    * @type Object
    */
   this.getHandler = function() {
      return handler;
   };

   /**
    * Sets the handler to use when rendering the skin.
    * By default, the form's data object is used a handler.
    * @param {Object} newHandler new skin handler object.
    */
   this.setHandler = function(newHandler) {
      handler = newHandler;
      return;
   };

   return this;
};
// extend jala.Form.Component
jala.Form.extend(jala.Form.Component.Skin, jala.Form.Component);

/**
 * Renders the skin named by this component to the response.
 */
jala.Form.Component.Skin.prototype.render = function() {
   var handler = this.getHandler() || this.form.getDataObject();
   if (handler != null && handler instanceof HopObject) {
      handler.renderSkin(this.name, this);
   } else {
      res.write("Skin component '" + this.name + "' unhandled");
   }
   return;
};

/**
 * Creates a new input component instance.
 * @class Instances of this class represent a single form input field.
 * @param {String} name Name of the component, used as name of the html control.
 * @constructor
 */
jala.Form.Component.Input = function Input(name) {
   jala.Form.Component.Input.superConstructor.apply(this, arguments);

   /**
    * Private map containing the requirements that need to be met
    */
   var requirements = {};
   
   /**
    * Private map containing messages to use when a requirement is not met
    */
   var messages = {};

   /**
    * Private field containing the label of this component
    * @type String
    */
   var label;

   /**
    * Private field containing the help text of this component
    * @type String
    */
   var help;
   
   /**
    * Sets a requirement for this component.
    * If function is called without arguments, jala.Form.REQUIRE
    * is set to true.
    * @param {String} key String defining the type of requirement,
    *             constants in jala.Form may be used.
    * @param {Object} val Value of the requirement.
    * @param {String} msg Optional error message if requirement
    *             is not fulfilled.
    */
   this.require = function(key, val, msg) {
      if (arguments.length == 0) {
         // set default value for arguments
         key = jala.Form.REQUIRE;
         val = true;
      }
      requirements[key] = val;
      if (msg) {
         this.setMessage(key, msg);
      }
      return;
   };

   /**
    * Returns the requirement value for a given key.
    * @param {String} key String defining the type of requirement,
    *             constants in jala.Form may be used.
    * @type Object
    */
   this.getRequirement = function(key) {
      return requirements[key];
   };

   /**
    * Sets a custom error message
    * @param {String} key String defining the type of requirement,
    *             constants in jala.Form may be used.
    * @param {String} msg Error message
    */
   this.setMessage = function(key, msg) {
      messages[key] = msg;
      return;
   };
  
   /**
    * Returns a specific message for a config element.
    * @param {String} key The key of the message as defined by
    *          the constants in jala.Form.* (e.g. "require",
    *          "maxlength", "minlength" ...
    * @param {String} defaultMsg the message to use when no message
    *          was defined.
    * @param {Object} args One or more arguments passed to the gettext
    * message processor which will replace {0}, {1} etc.
    * @returns rendered message
    * @type String
    */
   this.getMessage = function(key, defaultMsg, args) {
      var arr = [(messages[key]) ? messages[key] : defaultMsg];
      for (var i=2; i<arguments.length; i++) {
         arr.push(arguments[i]);
      }
      return gettext.apply(null, arr);
   };

   /**
    * Returns the label set for this component.
    * @returns label
    * @type String
    */
   this.getLabel = function() {
      return label;
   };

   /**
    * Sets the label for this component
    * @param {String} newLabel new label
    */
   this.setLabel = function(newLabel) {
      label = newLabel;
      return;
   };

   /**
    * Returns the help text set for this component.
    * @returns help text
    * @type String
    */
   this.getHelp = function() {
      return help;
   };

   /**
    * Sets the help text for this component
    * @param {String} newHelp new help text
    */
   this.setHelp = function(newHelp) {
      help = newHelp;
      return;
   };

   /**
    * The getter function for this component. If set, the
    * function is called to retrieve the original value of the
    * field. When called, the scope is set to the data object and
    * the name of the element is the sole argument.
    * @see #getValue
    * @type Function
    */
   this.getter;      // for doc purposes only

   // that's where the values really are stored:
   var getter, setter, validator;

   this.__defineGetter__("getter", function() {   return getter;   });
   this.__defineSetter__("getter", function(newGetter) {
      if (newGetter instanceof Function) {
         getter = newGetter;
      } else {
         throw "Invalid argument: getter must be a function";
      }
      return;
   });

   /**
    * The setter function for this component. If set, the
    * function is called to store the new value of the
    * field. When called, the scope is set to the data object and
    * the name and value of the element are provided as arguments.
    * @see #setValue
    * @type Function
    */
   this.setter;      // for doc purposes only

   this.__defineGetter__("setter", function() {   return setter;   });
   this.__defineSetter__("setter", function(newSetter) {
      if (newSetter instanceof Function) {
         setter = newSetter;
      } else {
         throw "Invalid argument: setter must be a function";
      }
      return;
   });

   /**
    * The validator function for this component. If set, the
    * function is called with the scope set to the data object
    * and with four arguments:
    * <li>the name of the element</li>
    * <li>the parsed value of the element if all requirements have
    *     been fulfilled. E.g., for a date editor, the parsed value would
    *     be a date object.</li>
    * <li>the map containing all user inputs as string (req.data)</li>
    * <li>the form object</li>
    * @see #validate
    * @type Function
    */
   this.validator;

   this.__defineGetter__("validator", function() {   return validator;   });
   this.__defineSetter__("validator", function(newValidator) {
      if (newValidator instanceof Function) {
         validator = newValidator;
      } else {
         throw "Invalid argument: validator must be a function";
      }
      return;
   });

   return this;
};
// extend jala.Form.Component
jala.Form.extend(jala.Form.Component.Input, jala.Form.Component);

/**
 * Validates the input provided to this component. First,
 * checkRequirements is called. If no error occurs, the input
 * is parsed using parseValue and passed on to the validator
 * function.
 * @see #checkRequirements
 * @see #parseValue
 * @param {jala.Form.Tracker} tracker Tracker object collecting
 *       request data, error messages and parsed values.
 */
jala.Form.Component.Input.prototype.validate = function(tracker) {
   var error = this.checkRequirements(tracker.reqData);
   if (error != null) {
      tracker.errors[this.name] = error;
   } else {
      tracker.values[this.name] = this.parseValue(tracker.reqData);
      if (this.validator) {
         error = this.validator.call(
            this.form.getDataObject(),
            this.name,
            tracker.values[this.name],
            tracker.reqData,
            this.form
         );
         if (error != null) {
            tracker.errors[this.name] = error;
         }
      }
   }
   return;
};

/**
 * Saves the parsed value using setValue.
 * @see #setValue
 * @param {jala.Form.Tracker} tracker Tracker object collecting
 *       request data, error messages and parsed values.
 * @param {Object} destObj (optional) object whose values will be changed.
 */
jala.Form.Component.Input.prototype.save = function(tracker, destObj) {
   this.setValue(destObj, tracker.values[this.name]);
   return;
};

/**
 * Retrieves the property which is edited by this component.
 * <ul>
 * <li>If no getter is given, the method returns the primitive property
 *    of the data object with the same name as the component.</li>
 * <li>If a getter function is defined, it is executed with the scope
 *    of the data object and the return value is used as default value.
 *    The name of the component is passed to the getter function
 *    as an argument.</li>
 * </ul>
 * @returns The value of the property
 * @type String|Number|Date
 */
jala.Form.Component.Input.prototype.getValue = function() {
   if (this.form.getTracker()) {
      // handling re-rendering
      return null;
   } else {
      var getter = (this.getter) ? this.getter : this.form.getter;
      return getter.call(this.form.getDataObject(), this.name);
   }
};

/**
 * Sets a property of the object passed as argument to the given value.
 * <li>If no setter is set at the component, the primitive property
 *    of the data object is changed.</li>
 * <li>If a setter function is defined it is executed with the data object
 *    as scope and with the name and new value provided as arguments</li>
 * <li>If the setter is explicitly set to null, no changes are made at all.</li>
 * @param {Object} destObj (optional) object whose values will be changed.
 * @param {Object} value The value to set the property to
 * @returns True in case the update was successful, false otherwise.
 * @see jala.Form#setter
 */
jala.Form.Component.Input.prototype.setValue = function(destObj, value) {
   // default value for this.setter is undefined, so if it has been
   // set to explicitly null, we don't save the value. in this case,
   // we assume, the property is handled outside of jala.Form or purposely
   // ignored at all.
   if (this.setter !== null) {
      var setter = (this.setter) ? this.setter : this.form.setter;
      setter.call(destObj, this.name, value);
   }
   return;
};

/**
 * Renders this component including label, error and help messages directly
 * to response.
 */
jala.Form.Component.Input.prototype.render = function() {
   var className = (this.getRequirement(jala.Form.REQUIRE) == true) ? "require" : "optional";
   if (this.getClassName()) {
      className += " " + this.getClassName();
   }
   var tracker = this.form.getTracker();
   if (tracker && tracker.errors[this.name]) {
      className += " error";
   }

   jala.Form.html.openTag("div",
      {id: this.createDomId(),
       "class": "component " + className}
   );
   res.write("\n");
   renderSkin(this.form.componentSkin, this);
   jala.Form.html.closeTag("div");
   res.write("\n");
   return;
};

/**
 * If the error tracker holds an error message for this component,
 * it is wrapped in a div-tag and returned as a string.
 * @returns Rendered string
 * @type String
 */
jala.Form.Component.Input.prototype.renderError = function() {
   var tracker = this.form.getTracker();
   if (tracker && tracker.errors[this.name]) {
      return jala.Form.html.elementAsString("div",
         tracker.errors[this.name],
         {"class": "errorText"}
      );
   }
   return null;
};

/**
 * Returns the rendered label of this component
 * @returns The rendered label of this component
 * @type String
 */
jala.Form.Component.Input.prototype.renderLabel = function() {
   return jala.Form.html.elementAsString(
      "label",
      this.getLabel() || "",
      {"for": this.createDomId("control")}
   );
};

/**
 * If this component contains a help message, it is wrapped in
 * a div-tag and returned as a string.
 * @returns The rendered help message
 * @type String
 */
jala.Form.Component.Input.prototype.renderHelp = function() {
   var help = this.getHelp();
   if (help) {
      return jala.Form.html.elementAsString(
         "div",
         help,
         {"class": "helpText"}
      );
   }
   return null;
};


/**
 * Renders this component including label, error and help messages
 * directly to response
 */
jala.Form.Component.Input.prototype.render_macro = function() {
   this.render();
   return;
};

/**
 * Renders the control(s) of this component
 */
jala.Form.Component.Input.prototype.controls_macro = function() {
   var attr = this.getControlAttributes();
   var tracker = this.form.getTracker()
   if (tracker) {
      this.renderControls(attr, null, tracker.reqData);
   } else {
      this.renderControls(attr, this.getValue());
   }
   return;
};

/**
 * Renders this component's error message (if set) directly to response
 */
jala.Form.Component.Input.prototype.error_macro = function() {
   res.write(this.renderError());
   return;
};

/**
 * Renders this component's label.
 */
jala.Form.Component.Input.prototype.label_macro = function() {
   res.write(this.renderLabel());
   return;
};

/**
 * Renders this component's help text, if set.
 */
jala.Form.Component.Input.prototype.help_macro = function() {
   res.write(this.renderHelp());
   return;
};

/**
 * Renders this component's id
 * @see jala.Form#createDomId
 */
jala.Form.Component.Input.prototype.id_macro = function() {
   res.write(this.createDomId());
   return;
};

/**
 * Renders this component's name
 */
jala.Form.Component.Input.prototype.name_macro = function() {
   res.write(this.name);
   return;
};

/**
 * Renders this component's type
 */
jala.Form.Component.Input.prototype.type_macro = function() {
   res.write(this.getType());
   return;
};

/**
 * Renders this component's class name.
 * Note that this is just the class name that has been explicitly
 * assigned using setClassName.
 * @see #setClassName
 */
jala.Form.Component.Input.prototype.class_macro = function() {
   var className = this.getClassName();
   if (className) {
      res.write(className);
   }
   return;
};


/**
 * Creates a new attribute object for this element.
 * @returns Object with properties id, name, class
 * @type Object
 */
jala.Form.Component.Input.prototype.getControlAttributes = function() {
   var attr = {
      id: this.createDomId("control"),
      name: this.name,
      "class": this.getType() 
   };
   if (this.getClassName()) {
      attr["class"] += " " + this.getClassName();
   }
   return attr;
};

/**
 * Checks user input for maximum length, minimum length and require
 * if the corresponding options have been set using the require method.
 * @param {Object} reqData request data
 * @returns String containing error message or null if everything is ok.
 * @type String
 * @see #require
 */
jala.Form.Component.Input.prototype.checkLength = function(reqData) {
   var require   = this.getRequirement(jala.Form.REQUIRE);
   var minLength = this.getRequirement(jala.Form.MINLENGTH);
   var maxLength = this.getRequirement(jala.Form.MAXLENGTH);
   
   if (require && (reqData[this.name] == null || reqData[this.name].trim() == "")) {
      return this.getMessage(jala.Form.REQUIRE, "Please enter text into this field.");
   } else if (maxLength && reqData[this.name].length > maxLength) {
      return this.getMessage(jala.Form.MAXLENGTH, "Input for this field is too long ({0} characters). Please enter no more than {1} characters.",
                                 reqData[this.name].length, maxLength);
   } else if (minLength) {
      // set an error if the element is required but the input is too short
      // but don't throw an error if the element is optional and empty
      if (reqData[this.name].length < minLength &&
          (require || (!require && reqData[this.name].length > 0))) {
         return this.getMessage(jala.Form.MINLENGTH, "Input for this field is too short ({0} characters). Please enter at least {1} characters.",
               reqData[this.name].length, minLength);
      }
   }
   return null;
};

/**
 * Checks user input against options set using the require method.
 * @param {Object} reqData request data
 * @returns String containing error message or null if everything is ok.
 * @type String
 * @see #checkLength
 * @see #require
 */
jala.Form.Component.Input.prototype.checkRequirements = function(reqData) {
   return this.checkLength(reqData);
};

/**
 * Parses the string input from the form and creates the datatype that
 * is edited with this component. For the input component this method
 * is not of much use, but subclasses that edit other datatypes may use
 * it. For example, a date editor should convert the user input from string
 * to a date object.
 * @param {Object} reqData request data
 * @returns parsed value
 * @type Object
 */
jala.Form.Component.Input.prototype.parseValue = function(reqData) {
   return reqData[this.name];
};

/**
 * Renders the html form elements to the response.
 * This method shall be overridden by subclasses of input component.
 * @param {Object} attr Basic attributes for the html form elements.
 * @param {Object} value Value to be used for rendering this element.
 * @param {Object} reqData Request data for the whole form. This argument is
 *       passed only if the form is re-rendered after an error occured.
 */
jala.Form.Component.Input.prototype.renderControls = function(attr, value, reqData) {
   attr.value = (reqData) ? reqData[this.name] : value;
   if (this.getRequirement(jala.Form.MAXLENGTH)) {
      attr.maxlength = this.getRequirement(jala.Form.MAXLENGTH);
   }
   jala.Form.html.input(attr);
   return;
};

/**
 * Constructs a newly created Password component instance
 * @class Subclass of jala.Form.Component.Input which renders and validates a
 * password input tag.
 * @base jala.Form.Component.Input
 * @param {String} name Name of the component, used as name of the html controls.
 * @returns A newly created Password component instance
 * @constructor
 */
jala.Form.Component.Password = function Password(name) {
   jala.Form.Component.Password.superConstructor.apply(this, arguments);
   return this;
};
// extend jala.Form.Component.Input
jala.Form.extend(jala.Form.Component.Password, jala.Form.Component.Input);

/**
 * Renders a password input tag to the response.
 * @param {Object} attr Basic attributes for this element.
 * @param {Object} value Value to be used for rendering this element.
 * @param {Object} reqData Request data for the whole form. This argument is
 *       passed only if the form is re-rendered after an error occured.
 */
jala.Form.Component.Password.prototype.renderControls = function(attr, value, reqData) {
   attr.value = (reqData) ? reqData[this.name] : value;
   if (this.getRequirement(jala.Form.MAXLENGTH)) {
      attr.maxlength = this.getRequirement(jala.Form.MAXLENGTH);
   }
   jala.Form.html.password(attr);
   return;
};

/**
 * Constructs a newly created Hidden component instance
 * @class Subclass of jala.Form.Component.Input which renders and validates a
 * hidden input tag.
 * @base jala.Form.Component.Input
 * @param {String} name Name of the component, used as name of the html controls.
 * @returns A newly created Hidden component instance
 * @constructor
 */
jala.Form.Component.Hidden = function Hidden(name) {
   jala.Form.Component.Hidden.superConstructor.apply(this, arguments);
   return this;
};
// extend jala.Form.Component.Input
jala.Form.extend(jala.Form.Component.Hidden, jala.Form.Component.Input);

/**
 * Renders this component directly to response. For a hidden tag, this is
 * just an input element, no div tag or anything.
 */
jala.Form.Component.Hidden.prototype.render = function() {
   var attr = this.getControlAttributes();
   var tracker = this.form.getTracker()
   if (tracker) {
      this.renderControls(attr, null, tracker.reqData);
   } else {
      this.renderControls(attr, this.getValue());
   }
   return;
};

/**
 * Renders a hidden input tag to the response.
 * @param {Object} attr Basic attributes for this element.
 * @param {Object} value Value to be used for rendering this element.
 * @param {Object} reqData Request data for the whole form. This argument is
 *       passed only if the form is re-rendered after an error occured.
 */
jala.Form.Component.Hidden.prototype.renderControls = function(attr, value, reqData) {
   attr.value = (reqData) ? reqData[this.name] : value;
   jala.Form.html.hidden(attr);
   return;
};

/**
 * Constructs a new Textarea component.
 * @class Subclass of jala.Form.Component.Input which renders and validates a
 * textarea input field.
 * @base jala.Form.Component.Input
 * @param {String} name Name of the component, used as name of the html controls.
 * @returns A newly created Textarea component instance
 * @constructor
 */
jala.Form.Component.Textarea = function Textarea(name) {
   jala.Form.Component.Textarea.superConstructor.apply(this, arguments);

   var rows, cols = undefined;

   /**
    * Returns the row numbers for this component.
    * @returns row numbers
    * @type String
    */
   this.getRows = function() {
      return rows;
   };

   /**
    * Sets the row numbers for this component.
    * @param {String} newRows new row numbers
    */
   this.setRows = function(newRows) {
      rows = newRows;
      return;
   };

   /**
    * Returns the col numbers for this component.
    * @returns col numbers
    * @type String
    */
   this.getCols = function() {
      return cols;
   };

   /**
    * Sets the col numbers for this component.
    * @param {String} newCols new col numbers
    */
   this.setCols = function(newCols) {
      cols = newCols;
      return;
   };
   
   return this;
};
// extend jala.Form.Component.Input
jala.Form.extend(jala.Form.Component.Textarea, jala.Form.Component.Input);

/**
 * Renders a textarea input field to the response.
 * @param {Object} attr Basic attributes for this element.
 * @param {Object} value Value to be used for rendering this element.
 * @param {Object} reqData Request data for the whole form. This argument is
 *       passed only if the form is re-rendered after an error occured.
 */
jala.Form.Component.Textarea.prototype.renderControls = function(attr, value, reqData) {
   attr.value = (reqData) ? reqData[this.name] : value;
   attr.rows = this.getRows() || 5;
   attr.cols = this.getCols() || 25;
   jala.Form.html.textArea(attr);
   return;
};

/**
 * Constructs a new Date component instance
 * @class Subclass of jala.Form.Component.Input which renders and validates a
 * date editor.
 * @base jala.Form.Component.Input
 * @param {String} name Name of the component, used as name of the html controls.
 * @returns A newly created Date component
 * @constructor
 */
jala.Form.Component.Date = function Date(name) {
   jala.Form.Component.Date.superConstructor.apply(this, arguments);

   var dateFormat = "d.M.yyyy H:m";
   var dateFormatObj;

   /**
    * Returns the date format for this component.
    * @returns date format object
    * @type java.text.SimpleDateFormat
    */
   this.getDateFormat = function() {
      if (!dateFormatObj || dateFormatObj.toPattern() != dateFormat) {
         dateFormatObj = new java.text.SimpleDateFormat(dateFormat);
      }
      return dateFormatObj;
   };

   /**
    * Sets the date format for this component.
    * @param {String} newDateFormat new date format
    */
   this.setDateFormat = function(newDateFormat) {
      dateFormat = newDateFormat;
      return;
   };

   return this;
};
// extend jala.Form.Component.Input
jala.Form.extend(jala.Form.Component.Date, jala.Form.Component.Input);

/**
 * Renders a textarea tag to the response.
 * @param {Object} attr Basic attributes for this element.
 * @param {Object} value Value to be used for rendering this element.
 * @param {Object} reqData Request data for the whole form. This argument is
 *       passed only if the form is re-rendered after an error occured.
 */
jala.Form.Component.Date.prototype.renderControls = function(attr, value, reqData) {
   if (reqData) {
      attr.value = reqData[this.name];
   } else  if (value instanceof Date) {
      attr.value = this.getDateFormat().format(value);
   }
   if (this.getRequirement(jala.Form.MAXLENGTH)) {
      attr.maxlength = this.getRequirement(jala.Form.MAXLENGTH);
   }
   jala.Form.html.input(attr);
   return;
};

/**
 * Validates user input from a date editor.
 * @param {Object} reqData request data
 * @returns null if everything is ok or string containing error message
 * @type String
 */
jala.Form.Component.Date.prototype.checkRequirements = function(reqData) {
   try {
      this.parseValue(reqData);
      return null;
   } catch(e) {
      return this.getMessage("invalid", "This date cannot be parsed.");
   }
};

/**
 * Parses the string input from the form and converts it to a date object.
 * Throws an error if the string cannot be parsed.
 * @param {Object} reqData request data
 * @returns parsed date value
 * @type Date
 */
jala.Form.Component.Date.prototype.parseValue = function(reqData) {
   return this.getDateFormat().parse(reqData[this.name]);
};

/**
 * Constructs a new Select component instance
 * @class Subclass of jala.Form.Component.Input which renders and validates a
 * dropdown element.
 * @base jala.Form.Component.Input
 * @param {String} name Name of the component, used as name of the html controls.
 * @returns A newly created Select component
 * @constructor
 */
jala.Form.Component.Select = function Select(name) {
   jala.Form.Component.Select.superConstructor.apply(this, arguments);
   
   var options, firstOption = undefined;
   
   /**
    * Returns the option list for this component.
    */
   this.getOptions = function() {
      return options;
   };
   
   /**
    * Sets the option list for this component.
    * The argument may either be an array that will be used as option list,
    * or a function that is called when the option component is rendered and
    * has to return an option array.
    * For both arrays those formats are allowed:
    * <li>Array of arrays <code>[ [val, display], [val, display], .. ]</code></li>
    * <li>Array of objects <code>[ {value:val, display:display}, .. ]</code></li>
    * <li>Array of strings <code>[ display, display, .. ]</code> In this case,
    *    the index position of the string will be the value.</li>
    * @param {Array Function} newOptions Array or function defining option list.
    */
   this.setOptions = function(newOptions) {
      options = newOptions;
      return;
   };
   
   /**
    * Returns the text that should be displayed if no value is selected.
    * @type String
    */
   this.getFirstOption = function() {
      return firstOption;
   };
   
   /**
    * Sets the text that is displayed if no value is selected
    * @param {String} newFirstOption text to display as first option element.
    */
   this.setFirstOption = function(newFirstOption) {
      firstOption = newFirstOption;
      return;
   };
  
   return this;
};
// extend jala.Form.Component.Input
jala.Form.extend(jala.Form.Component.Select, jala.Form.Component.Input);

/**
 * Renders a dropdown element to the response.
 * @param {Object} attr Basic attributes for this element.
 * @param {Object} value Value to be used for rendering this element.
 * @param {Object} reqData Request data for the whole form. This argument is
 *       passed only if the form is re-rendered after an error occured.
 */
jala.Form.Component.Select.prototype.renderControls = function(attr, value, reqData) {
   value = (reqData) ? reqData[this.name] : value;
   jala.Form.html.dropDown(attr, this.parseOptions(), value, this.getFirstOption());
   return;
};

/**
 * Validates user input from a dropdown element by making sure that
 * the option value list contains the user input.
 * @see jala.Form.Component.Select#checkOptions
 * @param {Object} reqData request data
 * @returns string containing error message or null if everything is ok.
 * @type String
 */
jala.Form.Component.Select.prototype.checkRequirements = function(reqData) {
   return this.checkOptions(reqData);
};

/**
 * Creates an array of options for a dropdown element or a
 * group of radiobuttons. If options field of this element's
 * config is an array, that array is returned.
 * If options is a function, its return value is returned.
 * @returns array of options
 * @type Array
 */
jala.Form.Component.Select.prototype.parseOptions = function() {
   var options = this.getOptions();
   if (options != null) {
      if (options instanceof Array) {
         return options;
      } else if (options instanceof Function) {
         return options.call(this.form.getDataObject(), this.name);
      }
   }
   return [];
};

/**
 * Checks user input for optiongroups: Unless require("checkoptions")
 * has ben set to false, the user input must exist in the option array.
 * @param {Object} reqData request data
 * @returns null if everything is ok or string containing error message
 * @type String
 */
jala.Form.Component.Select.prototype.checkOptions = function(reqData) {
   // if field is required, an empty option is not allowed:
   var found = (!this.getRequirement(jala.Form.REQUIRE) && !reqData[this.name]);
   if (!found) {
      if (this.getRequirement(jala.Form.CHECKOPTIONS) === false) {
         // exit, if option check shall be suppressed
         return null;
      }
      var options = this.parseOptions();
      var val = reqData[this.name];
      for (var i=0; i<options.length; i++) {
         if ((options[i] instanceof Array) && options[i].length > 0) {
            // option is an array (1st element = value, 2nd = display)
            if (options[i][0] == reqData[this.name]) {
               found = true;
               break;
            }
         } else if (options[i].value && options[i].display) {
            // option is an object with fields value + display
            if (options[i].value == reqData[this.name]) {
               found = true;
               break;
            }
         } else {
            // option is a string, value is index number
            if (i == reqData[this.name]) {
               found = true;
               break;
            }
         }
      }
   }
   if (!found) {
      return "Please select a valid option.";
   }
   return null;
};

/**
 * Creates a new Radio component instance
 * @class Subclass of jala.Form.Component.Input which renders and validates a
 * set of radio buttons.
 * @base jala.Form.Component.Select
 * @param {String} name Name of the component, used as name of the html controls.
 * @returns A newly created Radio component
 * @constructor
 */
jala.Form.Component.Radio = function Radio(name) {
   jala.Form.Component.Radio.superConstructor.apply(this, arguments);
   return this;
};
// extend jala.Form.Component.Select
jala.Form.extend(jala.Form.Component.Radio, jala.Form.Component.Select);

/**
 * Renders a set of radio buttons to the response.
 * @param {Object} attr Basic attributes for this element.
 * @param {Object} value Value to be used for rendering this element.
 */
jala.Form.Component.Radio.prototype.renderControls = function(attr, value) {
   var options = this.parseOptions();
   var optionAttr, optionDisplay;
   for (var i=0; i<options.length; i++) {
      optionAttr = attr.clone({}, false);
      optionAttr.id += i;
      if ((options[i] instanceof Array) && options[i].length > 0) {
         optionAttr.value = options[i][0];
         optionDisplay = options[i][1];
      } else if (options[i].value && options[i].display) {
         optionAttr.value = options[i].value;
         optionDisplay = options[i].display;
      } else {
         optionAttr.value = i;
         optionDisplay = options[i];
      }
      if (String(value) == String(optionAttr.value)) {
         optionAttr.checked = "checked";
      }
      jala.Form.html.radioButton(optionAttr);
      res.write(optionDisplay);
      jala.Form.html.tag("br");
   }
   return;
};

/**
 * Validates user input from a set of radio buttons and makes sure that
 * option value list contains the user input.
 * @see jala.Form.Component.Select#checkOptions
 * @param {Object} reqData request data
 * @returns null if everything is ok or string containing error message
 * @type String
 */
jala.Form.Component.Radio.prototype.checkRequirements = function(reqData) {
   return this.checkOptions(reqData);
};

/**
 * Creates a new Checkbox component instance
 * @class Subclass of jala.Form.Component.Input which renders and validates a
 * checkbox.
 * @base jala.Form.Component.Input
 * @param {String} name Name of the component, used as name of the html controls.
 * @returns A newly created Checkbox component instance
 * @constructor
 */
jala.Form.Component.Checkbox = function Checkbox(name) {
   jala.Form.Component.Checkbox.superConstructor.apply(this, arguments);
   return this;
};
// extend jala.Form.Component.Input
jala.Form.extend(jala.Form.Component.Checkbox, jala.Form.Component.Input);

/**
 * Renders an checkbox to the response.
 * @param {Object} attr Basic attributes for this element.
 * @param {Object} value Value to be used for rendering this element.
 */
jala.Form.Component.Checkbox.prototype.renderControls = function(attr, value, reqData) {
   if (value == 1 || (reqData && reqData[this.name] == "1")) {
      attr.checked = "checked";
   }
   attr.value = "1";
   jala.Form.html.checkBox(attr);
   return;
};

/**
 * Parses the string input from the form. For a checked box, the value is 1,
 * for an unchecked box the value is 0.
 * @param {Object} reqData request data
 * @returns parsed value
 * @type Number
 */
jala.Form.Component.Checkbox.prototype.parseValue = function(reqData) {
   return (reqData[this.name] == "1") ? 1 : 0;
};

/**
 * Validates user input from checkbox.
 * @param {Object} reqData request data
 * @returns null if everything is ok or string containing error message
 * @type String
 */
jala.Form.Component.Checkbox.prototype.checkRequirements = function(reqData) {
   if (reqData[this.name] && reqData[this.name] != "1") {
      return this.getMessage("invalid", "The value of this checkbox is invalid.");
   }
   return null;
};

/**
 * Creates a new File component instance
 * @class Subclass of jala.Form.Component.Input which renders and validates a
 * file upload.
 * @base jala.Form.Component.Input
 * @param {String} name Name of the component, used as name of the html controls.
 * @returns A newly created File component
 * @constructor
 */
jala.Form.Component.File = function File(name) {
   jala.Form.Component.File.superConstructor.apply(this, arguments);

   this.containsFileUpload = function() {
      return true;
   };

   return this;
};
// extend jala.Form.Component.Input
jala.Form.extend(jala.Form.Component.File, jala.Form.Component.Input);

/**
 * Renders a file input tag to the response.
 * @param {Object} attr Basic attributes for this element.
 * @param {Object} value Value to be used for rendering this element.
 * @param {Object} reqData Request data for the whole form. This argument is
 *       passed only if the form is re-rendered after an error occured.
 */
jala.Form.Component.File.prototype.renderControls = function(attr, value, reqData) {
   var contentType = this.getRequirement(jala.Form.CONTENTTYPE);
   if (contentType) {
      attr.accept = (contentType instanceof Array) ? contentType.join(",") : contentType;
   }
   jala.Form.html.file(attr);
   return;
};

/**
 * Validates a file upload by making sure it's there (if REQUIRE is set),
 * checking the file size, the content type and by trying to construct an image.
 * @param {Object} reqData request data
 * @param {jala.Form.Tracker} tracker jala.Form.Tracker object storing possible error messages
 * @returns null if everything is ok or string containing error message
 * @type String
 */
jala.Form.Component.File.prototype.checkRequirements = function(reqData) {

   if (reqData[this.name].contentLength == 0) {
      // no upload
      if (this.getRequirement(jala.Form.REQUIRE) == true) {
         return this.getMessage(jala.Form.REQUIRE, "File upload is required.");
      } else {
         // no further checks necessary, exit here
         return null;
      }
   }

   var maxLength = this.getRequirement(jala.Form.MAXLENGTH);
   if (maxLength && reqData[this.name].contentLength > maxLength) {
      return this.getMessage(jala.Form.MAXLENGTH, "This file is too big ({0} bytes), maximum allowed size {1} bytes.",
            reqData[this.name].contentLength, maxLength);
   }
   
   var contentType = this.getRequirement(jala.Form.CONTENTTYPE);
   if (contentType) {
      var arr = (contentType instanceof Array) ? contentType : [contentType];
      if (arr.indexOf(reqData[this.name].contentType) == -1) {
         return this.getMessage(jala.Form.CONTENTTYPE, "The file type {0} is not allowed.",
            reqData[this.name].contentType);
      }
   }
   
   return null;
};


/**
 * Creates a new Image component instance
 * @class Subclass of jala.Form.Component.File which renders a file upload
 * and validates uploaded files as images.
 * @base jala.Form.Component.File
 * @param {String} name Name of the component, used as name of the html controls.
 * @returns A newly created Image component
 * @constructor
 */
// FIXME: see below
jala.Form.Component.Image = function() {};

/**
 * @ignore
 * FIXME: JSDoc has some sever problems with this class.
 * It's somehow due to the named method ("Image") that it
 * always appears as global static object.
 * Wrapping the method in another function which immediately
 * is executed seems to solve this problem and could be used
 * as a work-around for similar issues.
 */
jala.Form.Component.Image = (function() {
   return function Image(name) {
      jala.Form.Component.Image.superConstructor.apply(this, arguments);
      return this;
   };
})();

// extend jala.Form.Component.File
jala.Form.extend(jala.Form.Component.Image, jala.Form.Component.File);

/**
 * Validates an image upload by making sure it's there (if REQUIRE is set),
 * checking the file size, the content type and by trying to construct an image.
 * If the file is an image, width and height limitations set by require are
 * checked.
 * @param {Object} reqData request data
 * @type String
 */
jala.Form.Component.Image.prototype.checkRequirements = function(reqData) {
   var re = this.constructor.superConstructor.prototype.checkRequirements.call(this, reqData);
   if (re) {
      return re;
   }

   if (reqData[this.name].contentLength > 0) {
      var helmaImg = undefined;
      try {
         helmaImg = new Image(reqData[this.name]);
      } catch (imgError) {
         return this.getMessage("invalid", "This image file can't be processed.");
      }
   
      var maxWidth = this.getRequirement(jala.Form.MAXWIDTH);
      if (maxWidth && helmaImg.getWidth() > maxWidth) {
         return this.getMessage("maxwidth", "This image is too wide.");
      }
      
      var minWidth = this.getRequirement(jala.Form.MINWIDTH);
      if (minWidth && helmaImg.getWidth() < minWidth) {
         return this.getMessage("minwidth", "This image is not wide enough.");
      }
      
      var maxHeight = this.getRequirement(jala.Form.MAXHEIGHT);
      if (maxHeight && helmaImg.getHeight() > maxHeight) {
         return this.getMessage("maxheight", "This image is too tall.");
      }
      
      var minHeight = this.getRequirement(jala.Form.MINHEIGHT);
      if (minHeight && helmaImg.getHeight() < minHeight) {
         return this.getMessage("minheight", "This image is not tall enough.");
      }
   }

   return null;
};



/**
 * Creates a new Button component instance
 * @class Subclass of jala.Form.Component.Input which renders a button.
 * @base jala.Form.Component.Input
 * @param {String} name Name of the component, used as name of the html controls.
 * @returns A newly created Button component
 * @constructor
 */
jala.Form.Component.Button = function Button(name) {
   jala.Form.Component.Button.superConstructor.apply(this, arguments);

   /**
    * Private field containing the value of the button (ie. the visible text)
    * @type String
    */
   var value;
   
   /**
    * Returns the value set for this button.
    * @returns value
    * @type String
    */
   this.getValue = function() {
      return value;
   };

   /**
    * Sets the value for this button.
    * @param {String} newValue new value
    */
   this.setValue = function(newValue) {
      value = newValue;
      return;
   };

   return this;
};
// extend jala.Form.Component
jala.Form.extend(jala.Form.Component.Button, jala.Form.Component.Input);

/**
 * Renders a button to the response.
 * @param {Object} attr Basic attributes for this element.
 * @param {Object} value Value to be used for rendering this element.
 * @param {Object} reqData Request data for the whole form. This argument is
 *       passed only if the form is re-rendered after an error occured.
 */
jala.Form.Component.Button.prototype.render = function(attr, value, reqData) {
   var classStr = (this.getClassName()) ? " " + this.getClassName() : "";
   var attr = {
      id: this.createDomId(),
      "class": "component" + classStr
   };
   jala.Form.html.openTag("div", attr);
   res.write("\n ");

   this.renderControls(this.getControlAttributes(), this.getValue());
   res.write("\n");
   
   jala.Form.html.closeTag("div");
   res.write("\n");
   return;
};

/**
 * Creates a new attribute object for this button.
 * @returns Object with all attributes set for this button.
 * @type Object
 */
jala.Form.Component.Button.prototype.renderControls = function(attr, value) {
   if (value) {
      attr.value = value;
   }
   jala.Form.html.button(attr);
   return;
};


/**
 * Creates a new Submit component instance
 * @class Subclass of jala.Form.Component.Button which renders a submit button.
 * @base jala.Form.Component.Button
 * @param {String} name Name of the component, used as name of the html controls.
 * @returns A newly created Submit component
 * @constructor
 */
jala.Form.Component.Submit = function Submit(name) {
   jala.Form.Component.Submit.superConstructor.apply(this, arguments);

   return this;
};
// extend jala.Form.Component.Button
jala.Form.extend(jala.Form.Component.Submit, jala.Form.Component.Button);

/**
 * Creates a new attribute object for this button.
 * @returns Object with all attributes set for this button.
 * @type Object
 */
jala.Form.Component.Submit.prototype.renderControls = function(attr, value) {
   if (value) {
      attr.value = value;
   }
   jala.Form.html.submit(attr);
   return;
};


/**
 * static default getter function used to return a field 
 * from the data object.
 * @param {String} name Name of the property.
 * @type Object
 */
jala.Form.propertyGetter = function(name, value) {
   return this[name];
};

/**
 * static default setter function used to change a field 
 * of the data object.
 * @param {String} name Name of the property.
 * @param {Object} value New value of the property.
 */
jala.Form.propertySetter = function(name, value) {
   this[name] = value;
};


/**
 * A generic container for error-messages and values
 * @class Instances of this class can contain error-messages and values
 * @constructor
 * @type jala.Form.Tracker
 */
jala.Form.Tracker = function(reqData) {

   /**
    * A map containing input from request data
    * @type Object
    */
   this.reqData = reqData;

   /**
    * A map containing parsed values (only for those fields that didn't
    * fail during checkRequirements method).
    * @type Object
    */
   this.values = {};

   /**
    * A map containing error messages
    * @type Object
    */
   this.errors = {};

   return this;
};

/** @ignore */
jala.Form.Tracker.toString = function() {
   return "[jala.Form.Tracker]";
};

/** @ignore */
jala.Form.Tracker.prototype.toString = jala.Form.Tracker.toString;

/**
 * Returns true if an error has been set for at least one component.
 * @returns true if form encountered an error.
 * @type Boolean
 */
jala.Form.Tracker.prototype.hasError = function() {
   for (var keys in this.errors) {
      return true;
   }
   return false;
};

/**
 * Returns the number of components for which this instance has
 * tracked an error.
 * @returns Number of components that did not validate.
 * @type Number
 */
jala.Form.Tracker.prototype.countErrors = function() {
   var ct = 0;
   for (var keys in this.errors) {
      ct++;
   }
   return ct;
};

/**
 * Helper method.
 * @private
 */
jala.Form.Tracker.prototype.debug = function() {
   for (var key in this.errors) {
      res.debug(key + ":" + this.errors[key]);
   }
   return;
};


