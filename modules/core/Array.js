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
 * $RCSfile: Array.js,v $
 * $Author$
 * $Revision$
 * $Date$
 */

/**
 * @fileoverview Adds useful methods to the JavaScript Array type.
 * <br /><br />
 * To use this optional module, its repository needs to be added to the 
 * application, for example by calling app.addRepository('modules/core/Array.js')
 * 
 * @addon
 */


/**
 * Check if this array contains a specific value.
 * @param {Object} val the value to check
 * @return {boolean} true if the value is contained
 */
Array.prototype.contains = function(val) {
   return this.indexOf(val) > -1;
};

/**
 * Retrieve the union set of a bunch of arrays
 * @param {Array} array1,... the arrays to unify
 * @return {Array} the union set
 */
Array.union = function() {
   var result = [];
   var map = {};
   for (var i=0; i<arguments.length; i+=1) {
      for (var n in arguments[i]) {
         var item = arguments[i][n];
         if (!map[item]) {
            result.push(item);
            map[item] = true;
         }
      }
   }
   return result;
};

/**
 * Retrieve the intersection set of a bunch of arrays
 * @param {Array} array1,... the arrays to intersect
 * @return {Array} the intersection set
 */
Array.intersection = function() {
   var all = Array.union.apply(this, arguments);
   var result = [];
   for (var n in all) {
      var chksum = 0;
      var item = all[n];
      for (var i=0; i<arguments.length; i+=1) {
         if (arguments[i].contains(item))
            chksum += 1;
         else
            break;
      }
      if (chksum == arguments.length)
         result.push(item);
   }
   return result;
};

// prevent any newly added properties from being enumerated
for (var i in Array)
   Array.dontEnum(i);
for (var i in Array.prototype)
   Array.prototype.dontEnum(i);
