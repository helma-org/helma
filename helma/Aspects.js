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
 * $RCSfile: helma.Aspects.js,v $
 * $Author: czv $
 * $Revision: 1.9 $
 * $Date: 2006/04/18 13:06:58 $
 */


if (!global.helma) {
    global.helma = {};
}

/**
 * library for adding Aspects (by roman porotnikov,
 * http://www.jroller.com/page/deep/20030701)
 *
 * Note: Each prototype that uses aspects must implement a method
 * onCodeUpdate() to prevent aspects being lost when the prototype
 * is re-compiled
 */


helma.Aspects = function() {
   return this;
};


helma.Aspects.toString = function() {
   return "[helma.Aspects]";
};


helma.Aspects.prototype.toString = function() {
   return "[helma.Aspects Object]";
};


helma.Aspects.prototype.addBefore = function(obj, fname, before) {
   var oldFunc = obj[fname];
   obj[fname] = function() {
      return oldFunc.apply(this, before(arguments, oldFunc, this));
   }
   return;
};


helma.Aspects.prototype.addAfter = function(obj, fname, after) {
   var oldFunc = obj[fname];
   obj[fname] = function() {
      return after(oldFunc.apply(this, arguments), arguments, oldFunc, this);
   }
   return;
};


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
