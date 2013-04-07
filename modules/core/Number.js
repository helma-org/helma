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
 * $Author$
 * $Revision$
 * $Date$
 */

/**
 * @fileoverview Adds useful methods to the JavaScript Number type.
 * <br /><br />
 * To use this optional module, its repository needs to be added to the 
 * application, for example by calling app.addRepository('modules/core/Number.js')
 */

/**
 * format a Number to a String
 * @param String Format pattern
 * @param java.util.Locale An optional Locale instance
 * @return String Number formatted to a String
 */
Number.prototype.format = function(fmt, locale) {
    var symbols;
    if (locale != null) {
        symbols = new java.text.DecimalFormatSymbols(locale);
    } else {
        symbols = new java.text.DecimalFormatSymbols();
    }
    var df = new java.text.DecimalFormat(fmt || "###,##0.##", symbols);
    return df.format(0 + this); // addition with 0 prevents exception
};


/** 
 * return the percentage of a Number
 * according to a given total Number
 * @param Int Total
 * @param String Format Pattern
 * @param java.util.Locale An optional Locale instance
 * @return Int Percentage
 */
Number.prototype.toPercent = function(total, fmt, locale) {
    if (!total)
        return (0).format(fmt, locale);
    var p = this / (total / 100);
    return p.format(fmt, locale);
};


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


// prevent any newly added properties from being enumerated
for (var i in Number)
   Number.dontEnum(i);
for (var i in Number.prototype)
   Number.prototype.dontEnum(i);
