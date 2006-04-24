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
 * $RCSfile: Number.js,v $
 * $Author: hannes $
 * $Revision: 1.5 $
 * $Date: 2006/04/18 13:06:58 $
 */


/**
 * factory to create functions for sorting objects in an array
 * @param String name of the field each object is compared with
 * @param Number order (ascending or descending)
 * @return Function ready for use in Array.prototype.sort
 */
Number.Sorter = function(field, order) {
    if (!order)
        order = 1;
    return function(a, b) {
        return (a[field] - b[field]) * order;
    };
};

Number.Sorter.ASC = 1;
Number.Sorter.DESC = -1;


/**
 * format a Number to a String
 * @param String Format pattern
 * @return String Number formatted to a String
 * FIXME: this might need some localisation
 */
Number.prototype.format = function(fmt) {
    var df = fmt ? new java.text.DecimalFormat(fmt)
                : new java.text.DecimalFormat("#,##0.00");
    return df.format(0+this);
};


/** 
 * return the percentage of a Number
 * according to a given total Number
 * @param Int Total
 * @param String Format Pattern
 * @return Int Percentage
 */
Number.prototype.toPercent = function(total, fmt) {
	var p = this / (total / 100);
    if (!fmt)
        return Math.round(p * 100) / 100;
    return p.format(fmt);
};


// prevent any newly added properties from being enumerated
for (var i in Number)
   Number.dontEnum(i);
for (var i in Number.prototype)
   Number.prototype.dontEnum(i);
