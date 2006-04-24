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
 * $RCSfile: helma.Skin.js,v $
 * $Author: czv $
 * $Revision: 1.5 $
 * $Date: 2006/04/18 13:06:58 $
 */


if (!global.helma) {
    global.helma = {};
}

helma.Skin = function(source, encFlag) {
    var Base64 = Packages.helma.util.Base64;

    if (!encFlag) {
        var skin = createSkin(source);
    } else {
        var encoded = source;
        source = new java.lang.String(source);
        var bytes = Base64.decode(source.toCharArray());
        var skin = createSkin(new java.lang.String(bytes, "UTF-8"));
    }

    this.toString = function() {
        return source;
    };

    this.valueOf = function() {
        if (encFlag)
            return encoded;
        var bytes = new java.lang.String(source).getBytes("UTF-8");
        return new java.lang.String(Base64.encode(bytes));
    };

    this.render = function(param) {
        return renderSkin(skin, param);
    };

    this.renderAsString = function(param) {
        return renderSkinAsString(skin, param);
    };

    this.containsMacro = function(name, handler) {
        res.push();
        res.write("<% *");
        if (handler) {
            res.write(handler);
            res.write(".");
        }
        res.write(name);
        res.write(" *%>");
        var re = new RegExp(res.pop(), "g");
        return re.test(source);
    };

    for (var i in this)
        this.dontEnum(i);

    return this;
};


helma.Skin.BASE64 = true;


helma.lib = "Skin";
helma.dontEnum(helma.lib);
for (var i in helma[helma.lib])
    helma[helma.lib].dontEnum(i);
for (var i in helma[helma.lib].prototype)
    helma[helma.lib].prototype.dontEnum(i);
delete helma.lib;
