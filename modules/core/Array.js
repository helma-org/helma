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
 */

/**
 * Check if this array contains a specific value.
 * @external
 * @memberof {Array}
 * @param {Object} val the value to check
 * @return {boolean} true if the value is contained
 */
Array.prototype.contains = Array.prototype.includes

/**
 * Retrieve the union set of a bunch of arrays
 * @external
 * @memberof {Array}
 * @param {Array} array1,... the arrays to unify
 * @return {Array} the union set
 */
Array.union = function() {
   return Array.from(arguments).reduce((result, array) => {
      return result.concat(array.filter(element => !result.includes(element)));
   }, []);
};

/**
 * Retrieve the intersection set of a bunch of arrays
 * @external
 * @memberof {Array}
 * @param {Array} array1,... the arrays to intersect
 * @return {Array} the intersection set
 */
Array.intersection = function() {
   return Array.from(arguments).reduce((result, array) => {
      return result.filter(element => array.includes(element));
   });
};

// prevent any newly added properties from being enumerated
for (var i in Array)
   Array.dontEnum(i);
for (var i in Array.prototype)
   Array.prototype.dontEnum(i);
