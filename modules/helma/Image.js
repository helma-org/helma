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
 * $RCSfile: Image.js,v $
 * $Author$
 * $Revision$
 * $Date$
 */

/**
 * @fileoverview Methods of the helma.Image module.
 * <br /><br />
 * To use this optional module, its repository needs to be added to the 
 * application, for example by calling app.addRepository('modules/helma/Image.js')
 */

if (!global.helma) {
    global.helma = {};
}

/**
 * Returns an Image object, generated from the specified source. 
 * <br /><br />
 * If the JIMI package is installed, an instance of 
 * helma.image.jimi.JimiGenerator will be returned. Otherwise, 
 * if the javax.imageio package is available, an instance of 
 * helma.image.imageio.ImageIOGenerator is returned. 
 * Additionally, the class of the ImageGenerator implementation
 * to be used can be set using the <code>imageGenerator</code> 
 * property in either the app.properties or server.properties 
 * file.
 * 
 * 
 * @param {helma.File|java.io.File|String} arg image source, filename or url
 * @return a new Image object
 * @singleton
 * @see Packages.helma.image.ImageGenerator
 * @see Packages.helma.image.jimi.JimiGenerator
 * @see Packages.helma.image.imageio.ImageIOGenerator 
 */
helma.Image = function(arg) {
    // according to 
    // http://grazia.helma.org/pipermail/helma-dev/2004-June/001253.html
    var generator = Packages.helma.image.ImageGenerator.getInstance();
    return generator.createImage(arg);
}

/** @ignore */
helma.Image.toString = function() {
    return "[helma.Image]";
};


/**
 * Returns an ImageInfo object for the specified image file. 
 * 
 * @param {helma.File|java.io.File|String} arg image source, filename or url
 * @returns an ImageInfo object
 * @memberof helma.Image
 * @see Packages.helma.image.ImageInfo
 */
helma.Image.getInfo = function(arg) {
    if (arguments.length != 1) {
        throw new java.lang.IllegalArgumentException(
            "Image.getInfo() expects one argument"
        );
    }

    var inp, result;
    var info = new Packages.helma.image.ImageInfo();
    // FIXME: we need a byte array for class comparison
    var b = java.lang.reflect.Array.newInstance(java.lang.Byte.TYPE, 0);

    try {
        if (arg instanceof java.io.InputStream) {
            inp = new java.io.InputStream(arg);
        // FIXME: here comes a dirty hack to check for a byte array
        } else if (arg.getClass && arg.getClass() == b.getClass()) {
            inp = new java.io.ByteArrayInputStream(arg);
        } else if (arg instanceof java.io.File) {
            inp = new java.io.FileInputStream(arg);
        } else if (arg instanceof helma.File) {
            inp = new java.io.FileInputStream(arg.getFile());
        } else if (typeof arg == "string") {
            var str = arg;
            // try to interpret argument as URL if it contains a colon,
            // otherwise or if URL is malformed interpret as file name.
            if (str.indexOf(":") > -1) {
                try {
                    var url = new java.net.URL(str);
                    inp = url.openStream();
                } catch (mux) {
                    inp = new java.io.FileInputStream(str);
                }
            } else {
                inp = new java.io.FileInputStream(str);
            }
        }
        if (inp == null) {
            var msg = "Unrecognized argument in Image.getInfo(): ";
            msg += (arg == null ? "null" : arg.getClass().toString());
            throw new java.lang.IllegalArgumentException(msg);
        }
        info.setInput(inp);
        if (info.check()) {
            result = info;
        }
    } catch (e) {
        // do nothing, returns null later
    } finally {
        if (inp != null) {
            try {
                inp.close();
            } catch (e) {}
        }
    }

    return result;
};


/**
 * Writes a 1x1 pixel transparent spacer GIF image to the
 * response buffer and sets the content type to image/gif.
 * 
 * @memberof helma.Image
 */
helma.Image.spacer = function() {
    res.contentType = "image/gif";
    res.writeBinary([71,73,70,56,57,97,2,0,2,0,-128,-1,0,-64,-64,-64,0,0,0,33,-7,4,1,0,0,0,0,44,0,0,0,0,1,0,1,0,64,2,2,68,1,0,59]);
    return;
};

helma.lib = "Image";
helma.dontEnum(helma.lib);
for (var i in helma[helma.lib])
    helma[helma.lib].dontEnum(i);
for (var i in helma[helma.lib].prototype)
    helma[helma.lib].prototype.dontEnum(i);
delete helma.lib;



