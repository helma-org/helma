/*
 * Helma License Notice
 *
 * The contents of this file are subject to the Helma License
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://adele.helma.org/download/helma/license.txt
 *
 * Copyright 1998-2006 Helma Software. All Rights Reserved.
 *
 * $RCSfile: Object.js,v $
 * $Author$
 * $Revision$
 * $Date$
 */

/**
 * @fileoverview Adds useful methods to the JavaScript Object type.
 * <br /><br />
 * To use this optional module, its repository needs to be added to the 
 * application, for example by calling app.addRepository('modules/core/Object.js')
 */

/**
 * Copies the properties of this object into a clone.
 * @param {Object} clone The optional target object
 * @param {Boolean} recursive If true child objects are cloned as well, otherwise
 * the clone contains references to the child objects
 * @returns The resulting object
 */
Object.prototype.clone = function(clone, recursive) {

   var getValue = function(value, recursive) {
      if ((value == null || typeof(value) !== "object") || recursive !== true) {
         return value;
      }
      return value.clone(null, recursive);
   };

   if (typeof(this) === "object") {
      switch (this.constructor) {
         case Array:
            return this.map(function(value) {
               return getValue(value, recursive);
            });

         case null: // e.g. macro parameter objects
            if (clone == null) {
               clone = {};
            }
            // continue below
         case Object:
         case HopObject:
            if (clone == null) {
               clone = new this.constructor();
            }
            for (var propName in this) {
               clone[propName] = getValue(this[propName], recursive);
            }
            return clone;

         default:
            return new this.constructor(this.valueOf());
      }
   } else if (typeof(this) === "function" && this.constructor === RegExp) {
      return new RegExp(this.valueOf());
   }
   return this;
};


/**
 * reduce an extended object (ie. a HopObject)
 * to a generic javascript object
 * @param HopObject the HopObject to be reduced
 * @return Object the resulting generic object
 */
Object.prototype.reduce = function(recursive) {
    var result = {};
    for (var i in this) {
        if (this[i] instanceof HopObject == false) {
            result[i] = this[i];
        } else if (recursive) {
            result[i] = this.reduce(true);
        }
    }
    return result;
};


/**
 * print the contents of an object for debugging
 * @param Object the object to dump
 * @param Boolean recursive flag (if true, dump child objects, too)
 */
Object.prototype.dump = function(recursive) {
    var beginList = "<ul>";
    var endList = "</ul>";
    var beginItem = "<li>";
    var endItem = "</li>";
    var beginKey = "<strong>";
    var endKey = ":</strong> ";
    res.write(beginList);
    for (var p in this) {
        res.write(beginItem);
        res.write(beginKey);
        res.write(p);
        res.write(endKey);
        if (recursive && typeof this[p] == "object") {
            var recurse = true;
            var types = [Function, Date, String, Number];
            for (var i in types) {
                if (this[p] instanceof types[i]) {
                    recurse = false
                    break;
                }
            }
            if (recurse == true)
                this[p].dump(true);
            else {
                res.write(this[p].toSource());
            }
        } else if (this[p]) {
            res.write(encode(this[p].toSource()));
        }
        res.write(endItem);
    }
    res.write(endList);
    return;
};


// prevent any newly added properties from being enumerated
for (var i in Object)
   Object.dontEnum(i);
for (var i in Object.prototype)
   Object.prototype.dontEnum(i);
