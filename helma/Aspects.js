/*
 * Helma License Notice
 *
 * The contents of this file are subject to the Helma License
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://adele.helma.org/download/helma/license.txt
 *
 * Copyright 1998-2007 Helma Software. All Rights Reserved.
 *
 * $RCSfile: Aspects.js,v $
 * $Author$
 * $Revision$
 * $Date$
 */

/**
 * @fileoverview Methods of the helma.Aspects module.
 * <br /><br />
 * To use this optional module, its repository needs to be added to the 
 * application, for example by calling app.addRepository('modules/helma/Aspects.js')
 */

/**
 * Define the global namespace if not existing
 */
if (!global.helma) {
    global.helma = {};
}

/**
 * Library for adding Aspects
 * <br /><br />
 * Provides static methods to wrap existing functions
 * inside a javascript closure in order to add additional 
 * behavior without overriding the existing one.
 * <br /><br />
 * Based on code by roman porotnikov,
 * http://www.jroller.com/page/deep/20030701
 * <br /><br />
 * Note: Each prototype that uses aspects must implement a method
 * onCodeUpdate() to prevent aspects being lost when the prototype
 * is re-compiled
 * 
 * @constructor
 */
helma.Aspects = function() {
   return this;
};


/** @ignore */
helma.Aspects.toString = function() {
   return "[helma.Aspects]";
};


/** @ignore */
helma.Aspects.prototype.toString = function() {
   return "[helma.Aspects Object]";
};


/**
 * Adds a function to be called before the orginal function.
 * <br /><br />
 * The return value of the added function needs to provide the 
 * array of arguments that is passed to the original function.
 * 
 * @param {Object} obj The object of which the original function is a property
 * @param {String} fname The property name of the original function
 * @param {Function} before The function to be called before the original function
 * @returns Function A new function, wrapping the original function
 * @type Function
 */
helma.Aspects.prototype.addBefore = function(obj, fname, before) {
   var oldFunc = obj[fname];
   obj[fname] = function() {
      return oldFunc.apply(this, before(arguments, oldFunc, this));
   }
   return;
};


/**
 * Adds a function to be called after an existing function.
 * <br /><br />
 * The return value of the original function is passed to the 
 * added function as its first argument. In addition, the added   
 * function also receives an array of the original arguments,
 * the original function and the scope object of the original 
 * function as additional parameters.
 * 
 * @param {Object} obj as Object, the object of which the original function is a property
 * @param {String} fname as String, the property name of the original function
 * @param {Function} after as Function, the function to be called after the original function
 * @returns Function A new function, wrapping the original function
 * @type Function
 */
helma.Aspects.prototype.addAfter = function(obj, fname, after) {
   var oldFunc = obj[fname];
   obj[fname] = function() {
      return after(oldFunc.apply(this, arguments), arguments, oldFunc, this);
   }
   return;
};


/**
 * Wraps an additional function around the original function.
 * <br /><br />
 * The added function receives as its arguments an array of the original 
 * arguments, the original function and the scope object of the original 
 * function. The original function is not called directly and needs 
 * to be invoked by the added function.
 * 
 * @param {Object} obj as Object, the object of which the original function is a property
 * @param {String} fname as String, the property name of the original function
 * @param {Function} around as Function, the function to be called inside the original function
 * @returns Function A new function, wrapping the original function
 * @type Function
 */
helma.Aspects.prototype.addAround = function(obj, fname, around) {
   var oldFunc = obj[fname];
   obj[fname] = function() {
      return around(arguments, oldFunc, this);
   }
   return;
};


helma.lib = "Aspects";
helma.dontEnum(helma.lib);
for (var i in helma[helma.lib])
   helma[helma.lib].dontEnum(i);
for (var i in helma[helma.lib].prototype)
   helma[helma.lib].prototype.dontEnum(i);
delete helma.lib;


helma.aspects = new helma.Aspects();
helma.dontEnum("aspects");
