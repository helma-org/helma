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
 * $RCSfile: Skin.js,v $
 * $Author: czv $
 * $Revision: 1.2 $
 * $Date: 2006/04/24 07:02:17 $
 */


/**
 * @fileoverview Fields and methods of the helma.Skin class.
 */


// define the helma namespace, if not existing
if (!global.helma) {
    global.helma = {};
}

/**
 * Constructs a new instance of helma.Skin
 * @class Instances of this class represent a Helma skin. In addition
 * to the standard skin functionality this class allows creation of
 * a skin based on a Base64 encoded source.
 * @param {String} source The source of the skin
 * @param {Boolean} encFlag If true the source will be Base64-decoded.
 * @constructor
 * @returns A newly created instance of helma.Skin
 */
helma.Skin = function(source, encFlag) {
    /** @ignore */    
    var Base64 = Packages.helma.util.Base64;

    if (!encFlag) {
        var skin = createSkin(source);
    } else {
        var encoded = source;
        source = new java.lang.String(source);
        var bytes = Base64.decode(source.toCharArray());
        var skin = createSkin(new java.lang.String(bytes, "UTF-8"));
    }

    /** @ignore */
    this.toString = function() {
        return source;
    };

    /**
     * Returns the source of the skin as Base64 encoded string
     * @returns The source of the skin as Base64 encoded string
     * @type String
     */
    this.valueOf = function() {
        if (encFlag) {
            return encoded;
        }
        var bytes = new java.lang.String(source).getBytes("UTF-8");
        return new java.lang.String(Base64.encode(bytes));
    };

    /**
     * Renders the skin.
     * @param {Object} param An optional parameter object to pass to the skin.
     */
    this.render = function(param) {
        return renderSkin(skin, param);
    };

    /**
     * Returns the rendered skin.
     * @param {Object} param An optional parameter object to pass to the skin.
     */
    this.renderAsString = function(param) {
        return renderSkinAsString(skin, param);
    };

    /**
     * Returns true if the skin contains a macro with the name
     * and optional handler passed as argument.
     * @param {String} name The name of the macro
     * @param {String} handler An optional macro handler name
     * @returns True if the skin contains this macro at least once,
     * false otherwise.
     * @type Boolean
     */
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


helma.lib = "Skin";
helma.dontEnum(helma.lib);
for (var i in helma[helma.lib])
    helma[helma.lib].dontEnum(i);
for (var i in helma[helma.lib].prototype)
    helma[helma.lib].prototype.dontEnum(i);
delete helma.lib;
