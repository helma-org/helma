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
 * $RCSfile: Color.js,v $
 * $Author$
 * $Revision$
 * $Date$
 */


/**
 * @fileoverview Fields and methods of the helma.Chart prototype
 */

// take care of any dependencies
app.addRepository("modules/core/String.js");

/**
 * Define the global namespace if not existing
 */
if (!global.helma) {
    global.helma = {};
}

/**
 * Constructs a new instance of helma.Color.
 * @class Instances of this class provide methods for
 * converting HTML color names into their corresponding
 * RGB values and vice versa, or retrieving single RGB color values.
 * @param {Number|String} R Either the red fraction of the color,
 * or the name of the color.
 * @param {Number} G The green fraction
 * @param {Number} B The blue fraction
 * @returns A newly created helma.Color instance
 * @constructor
 */
helma.Color = function(R, G, B) {
    var value = null;
    var name = null;
    var hex = null;
    var rgb = null;

    /**
     * Returns the decimal value of this color, or of a specified
     * color channel.
     * @param {String} channel An optional color channel which
     * decimal value should be returned. Must be either "red",
     * "green" or "blue". If no channel is specified this
     * method returns the decimal value of the color itself.
     * @returns The decimal value of this color or a single channel.
     * @type Number
     */
    this.valueOf = function(channel) {
        if (channel) {
            if (!rgb) {
                var compose = function(n, bits) {
                    var div = Math.pow(2, bits);
                        remainder = n % div;          
                        return Math.floor(n/div);
                }
                var remainder = value;
                rgb = {
                    red: compose(remainder, 16),
                    green: compose(remainder, 8),
                    blue: compose(remainder, 0)
                };
            }
            return rgb[channel];
        }
        return value;
    };

    /**
     * Returns the hexidecimal value of this color (without
     * a leading hash sign).
     * @returns The hexidecimal value of this color
     * @type String
     */
    this.toString = function() {
        if (!value)
            return null;
        if (!hex)
            hex = value.toString(16).pad("0", 6, String.LEFT);
        return hex;
    };


    /**
     * Returns the trivial name of this color
     * @returns The trivial name of this color
     * @type String
     */
    this.getName = function() {
        return helma.Color.COLORVALUES[value];
    };

    /**
     * Main constructor body
     */
    if (arguments.length % 2 == 0)
        throw("Insufficient arguments for creating Color");
    if (arguments.length == 1) {
        if (R.constructor == Number) {
            value = R;
        } else if (R.constructor == String) {
            R = R.toLowerCase();
            if (helma.Color.COLORNAMES[R]) {
                this.name = R;
                value = helma.Color.COLORNAMES[R];
            } else {
                if (R.startsWith("#")) {
                    R = R.substring(1);
                }
                value = parseInt(R, 16);
                this.name = helma.Color.COLORVALUES[value];
            }
        }
    } else {
        value = R * Math.pow(2, 16) + G * Math.pow(2, 8) + B;
    }

    if (value == null || isNaN(value))
        throw("helma.Color: invalid argument " + R);

    for (var i in this)
        this.dontEnum(i);

    return this;
};


/**
 * Creates a new helma.Color instance based on a color name.
 * @param {String} name The color name (eg. "darkseagreen")
 * @returns An instance of helma.Color representing the color specified
 * @type helma.Color
 */
helma.Color.fromName = function(name) {
    var value = helma.Color.COLORNAMES[name.toLowerCase()];
    return new helma.Color(value || 0);
};


/**
 * Creates a new helma.Color instance based on a HSL color
 * representation. This method is adapted from the HSLtoRGB
 * conversion method as described at
 * <a href="http://www1.tip.nl/~t876506/ColorDesign.html#hr">http://www1.tip.nl/~t876506/ColorDesign.html#hr</a>.
 * @param {Number} H The hue fraction of the color definition
 * @param {Number} S The saturation fraction
 * @param {Number} L The lightness fraction
 * @returns An instance of helma.Color representing the corresponding
 * RGB color definition.
 * @type helma.Color
 */
helma.Color.fromHsl = function(H,S,L) {
    function H1(H,S,L) {
        var R = 1; var G = 6*H; var B = 0;
        G = G*S + 1 - S; B = B*S + 1 - S;
        R = R*L; G = G*L; B = B*L;
        return [R,G,B];
    }
    
    function H2(H,S,L) {
        var R = 1-6*(H - 1/6); var G = 1; var B = 0;
        R = R*S + 1 - S; B = B*S + 1 - S;
        R = R*L; G = G*L; B = B*L;
        return [R,G,B];
    }
    
    function H3(H,S,L) {
        var R = 0; var G = 1; var B = 6*(H - 1/3);
        R = R*S + 1 - S; B = B*S + 1 - S;
        R = R*L; G = G*L; B = B*L
        return [R,G,B];
    }
    
    function H4(H,S,L) {
        var R = 0; var G = 1-6*(H - 1/2); var B = 1;
        R = R*S + 1 - S; G = G*S + 1 - S;
        R = R*L; G = G*L; B = B*L;
        return [R,G,B];
    }
    
    function H5(H,S,L) {
        var R = 6*(H - 2/3); var G = 0; var B = 1;
        R = R*S + 1 - S; G = G*S + 1 - S;
        R = R*L; G = G*L; B = B*L;
        return [R,G,B];
    }
    
    function H6(H,S,L) {
        var R = 1; var G = 0; var B = 1-6*(H - 5/6);
        G = G*S + 1 - S; B = B*S + 1 - S;
        R = R*L; G = G*L; B = B*L;
        return [R,G,B];
    }
    
    // H  [0-1] is divided into 6 equal sectors.
    // From within each sector the proper conversion function is called.
    var rgb;
    if (H < 1/6)        rgb = H1(H,S,L);
    else if (H < 1/3) rgb = H2(H,S,L);
    else if (H < 1/2) rgb = H3(H,S,L);
    else if (H < 2/3) rgb = H4(H,S,L);
    else if (H < 5/6) rgb = H5(H,S,L);
    else                  rgb = H6(H,S,L);

    return new helma.Color(
        Math.round(rgb[0]*255),
        Math.round(rgb[1]*255),
        Math.round(rgb[2]*255)
    );
};


/**
 * Contains the hexadecimal values of named colors.
 * @type Object
 * @final
 */
helma.Color.COLORNAMES = {
    black: 0x000000,
    maroon: 0x800000,
    green: 0x008000,
    olive: 0x808000,
    navy: 0x000080,
    purple: 0x800080,
    teal: 0x008080,
    silver: 0xc0c0c0,
    gray: 0x808080,
    red: 0xff0000,
    lime: 0x00ff00,
    yellow: 0xffff00,
    blue: 0x0000ff,
    fuchsia: 0xff00ff,
    aqua: 0x00ffff,
    white: 0xffffff,
    aliceblue: 0xf0f8ff,
    antiquewhite: 0xfaebd7,
    aquamarine: 0x7fffd4,
    azure: 0xf0ffff,
    beige: 0xf5f5dc,
    blueviolet: 0x8a2be2,
    brown: 0xa52a2a,
    burlywood: 0xdeb887,
    cadetblue: 0x5f9ea0,
    chartreuse: 0x7fff00,
    chocolate: 0xd2691e,
    coral: 0xff7f50,
    cornflowerblue: 0x6495ed,
    cornsilk: 0xfff8dc,
    crimson: 0xdc143c,
    darkblue: 0x00008b,
    darkcyan: 0x008b8b,
    darkgoldenrod: 0xb8860b,
    darkgray: 0xa9a9a9,
    darkgreen: 0x006400,
    darkkhaki: 0xbdb76b,
    darkmagenta: 0x8b008b,
    darkolivegreen: 0x556b2f,
    darkorange: 0xff8c00,
    darkorchid: 0x9932cc,
    darkred: 0x8b0000,
    darksalmon: 0xe9967a,
    darkseagreen: 0x8fbc8f,
    darkslateblue: 0x483d8b,
    darkslategray: 0x2f4f4f,
    darkturquoise: 0x00ced1,
    darkviolet: 0x9400d3,
    deeppink: 0xff1493,
    deepskyblue: 0x00bfff,
    dimgray: 0x696969,
    dodgerblue: 0x1e90ff,
    firebrick: 0xb22222,
    floralwhite: 0xfffaf0,
    forestgreen: 0x228b22,
    gainsboro: 0xdcdcdc,
    ghostwhite: 0xf8f8ff,
    gold: 0xffd700,
    goldenrod: 0xdaa520,
    greenyellow: 0xadff2f,
    honeydew: 0xf0fff0,
    hotpink: 0xff69b4,
    indianred: 0xcd5c5c,
    indigo: 0x4b0082,
    ivory: 0xfffff0,
    khaki: 0xf0e68c,
    lavender: 0xe6e6fa,
    lavenderblush: 0xfff0f5,
    lawngreen: 0x7cfc00,
    lemonchiffon: 0xfffacd,
    lightblue: 0xadd8e6,
    lightcoral: 0xf08080,
    lightcyan: 0xe0ffff,
    lightgoldenrodyellow: 0xfafad2,
    lightgreen: 0x90ee90,
    lightgrey: 0xd3d3d3,
    lightpink: 0xffb6c1,
    lightsalmon: 0xffa07a,
    lightseagreen: 0x20b2aa,
    lightskyblue: 0x87cefa,
    lightslategray: 0x778899,
    lightsteelblue: 0xb0c4de,
    lightyellow: 0xffffe0,
    limegreen: 0x32cd32,
    linen: 0xfaf0e6,
    mediumaquamarine: 0x66cdaa,
    mediumblue: 0x0000cd,
    mediumorchid: 0xba55d3,
    mediumpurple: 0x9370db,
    mediumseagreen: 0x3cb371,
    mediumslateblue: 0x7b68ee,
    mediumspringgreen: 0x00fa9a,
    mediumturquoise: 0x48d1cc,
    mediumvioletred: 0xc71585,
    midnightblue: 0x191970,
    mintcream: 0xf5fffa,
    mistyrose: 0xffe4e1,
    moccasin: 0xffe4b5,
    navajowhite: 0xffdead,
    oldlace: 0xfdf5e6,
    olivedrab: 0x6b8e23,
    orange: 0xffa500,
    orangered: 0xff4500,
    orchid: 0xda70d6,
    palegoldenrod: 0xeee8aa,
    palegreen: 0x98fb98,
    paleturquoise: 0xafeeee,
    palevioletred: 0xdb7093,
    papayawhip: 0xffefd5,
    peachpuff: 0xffdab9,
    peru: 0xcd853f,
    pink: 0xffc0cb,
    plum: 0xdda0dd,
    powderblue: 0xb0e0e6,
    rosybrown: 0xbc8f8f,
    royalblue: 0x4169e1,
    saddlebrown: 0x8b4513,
    salmon: 0xfa8072,
    sandybrown: 0xf4a460,
    seagreen: 0x2e8b57,
    seashell: 0xfff5ee,
    sienna: 0xa0522d,
    skyblue: 0x87ceeb,
    slateblue: 0x6a5acd,
    slategray: 0x708090,
    snow: 0xfffafa,
    springgreen: 0x00ff7f,
    steelblue: 0x4682b4,
    tan: 0xd2b48c,
    thistle: 0xd8bfd8,
    tomato: 0xff6347,
    turquoise: 0x40e0d0,
    violet: 0xee82ee,
    wheat: 0xf5deb3,
    whitesmoke: 0xf5f5f5,
    yellowgreen: 0x9acd32
};


/**
 * Contains the color names for specific hex values
 * @type Object
 * @final
 */
helma.Color.COLORVALUES = {};

for (var i in helma.Color.COLORNAMES) {
    helma.Color.COLORVALUES[helma.Color.COLORNAMES[i]] = i;
}


/** @ignore */
helma.Color.toString = function() {
    return "[helma.Color]";
};


helma.lib = "Color";
helma.dontEnum(helma.lib);
for (var i in helma[helma.lib])
    helma[helma.lib].dontEnum(i);
for (var i in helma[helma.lib].prototype)
    helma[helma.lib].prototype.dontEnum(i);
delete helma.lib;
