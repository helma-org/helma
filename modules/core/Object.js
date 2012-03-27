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
 * copy the properties of an object into
 * a new object
 * @param Object the source object
 * @param Object the (optional) target object
 * @return Object the resulting object
 */
Object.prototype.clone = function(clone, recursive) {
   if (!clone)
      clone = new this.constructor();
   var value;
   for (var propName in this) {
      value = this[propName];
      if (recursive && (value.constructor == HopObject || value.constructor == Object)) {
         clone[propName] = value.clone(new value.constructor(), recursive);
      } else {
         clone[propName] = value;
      }
   }
   return clone;
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
        if (this[i] instanceof HopObject == false)
            result[i] = this[i];
        else if (recursive)
            result[i] = this.reduce(true);
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
