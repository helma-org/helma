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
 * $RCSfile: Zip.js,v $
 * $Author$
 * $Revision$
 * $Date$
 */

/**
 * @fileoverview Fields and methods of the helma.Zip class.
 */

// take care of any dependencies
app.addRepository('modules/helma/File.js');

// define the helma namespace, if not existing
if (!global.helma) {
    global.helma = {};
}

/**
 * Constructs a new helma.Zip instance
 * @class Instances of this class represent a single zip archive
 * and provide various methods for extracting entries or manipulating
 * the contents of the archive.
 * @param {helma.File|java.io.File|String} file Either
 * a file object representing the .zip file on disk, or the
 * path to the .zip file as string.
 * @constructor
 * @returns A newly created instance of helma.Zip.
 * @author Robert Gaggl <robert@nomatic.org> 
 */
helma.Zip = function(file) {

    /**
     * Private method that extracts the data of a single file
     * in a .zip archive. If a destination path is given it
     * writes the extracted data directly to disk using the
     * name of the ZipEntry Object, otherwise it returns the
     * byte array containing the extracted data.
     * @param {java.util.zip.ZipFile} zFile The zip archive to extract
     * the file from.
     * @param {java.util.zip.ZipEntry} entry The zip entry to extract
     * @param {String} destPath The destination path where the extracted
     * file should be stored.
     * @returns If no destination path is given, this method returns
     * the contents of the extracted file as ByteArray, otherwise
     * it returns null.
     * @private
     */
    var extractEntry = function(zFile, entry, destPath) {
        var size = entry.getSize();
        if (entry.isDirectory() || size <= 0)
            return null;

        var zInStream = new java.io.BufferedInputStream(zFile.getInputStream(entry));
        var buf = new java.lang.reflect.Array.newInstance(java.lang.Byte.TYPE, size);
        zInStream.read(buf, 0, size);
        zInStream.close();

        if (!destPath) {
            // no filesystem destination given, so return
            // the byte array containing the extracted data
            return buf;
        }
        // extract the file to the given path
        var dest = new java.io.File(destPath, entry.getName());
        if (entry.isDirectory())
            dest.mkdirs();
        else if (buf) {
            if (!dest.getParentFile().exists())
                dest.getParentFile().mkdirs();
            try {
                var outStream = new java.io.BufferedOutputStream(new java.io.FileOutputStream(dest));
                outStream.write(buf, 0, size);
            } finally {
                outStream.close();
            }
        }
        return null;
    };

    /**
     * Private method for adding a single file to the Zip archive
     * represented by this helma.Zip instance
     * @param {java.util.zip.ZipOutputStream} zOutStream The output
     * stream to write to
     * @param {java.io.File} f The file that should be added to the
     * Zip archive.
     * @param {Number} level The compression-level between 0-9.
     * @param {String} pathPrefix The path of the directory within the
     * Zip archive where the file should be added (optional).
     * @private
     */
    var addFile = function(zOutStream, f, level, pathPrefix) {
        var fInStream = new java.io.BufferedInputStream(new java.io.FileInputStream(f));
        buf = new java.lang.reflect.Array.newInstance(java.lang.Byte.TYPE, f.length());
        fInStream.read(buf, 0, f.length());

        var name = new java.lang.StringBuffer();
        if (pathPrefix) {
            // append clean pathPrefix to name buffer
            var st = new java.util.StringTokenizer(pathPrefix, "\\/");
            while (st.hasMoreTokens()) {
                name.append(st.nextToken());
                name.append("/");
            }
        }
        name.append(f.getName());
        var entry = new java.util.zip.ZipEntry(name.toString());
        entry.setSize(f.length());
        entry.setTime(f.lastModified());
        zOutStream.setLevel(level);
        zOutStream.putNextEntry(entry);
        zOutStream.write(buf, 0, buf.length);
        zOutStream.closeEntry();
        fInStream.close();
        return;
    };

    /**
     * Private helper method that converts the argument into
     * an instance of java.io.File.
     * @param {helma.File|java.io.File} f Either a file object or
     * the path to a file as string
     * @return The argument converted into a file object
     * @type java.io.File
     * @private
     */
    var evalFile = function(f) {
        var result;
        if (f instanceof java.io.File) {
            result = f;
        } else if (f instanceof helma.File || typeof(f) == "string") {
            result = new java.io.File(f);
        }
        if (!result.exists()) {
            throw "Error creating Zip Object: File '" + f + "' doesn't exist.";
        }
        return result;
    };
    
    /**
     * Returns an array containing the entries of the archive
     * represented by this helma.Zip instance.
     * @returns The entries stored in the zip archive
     * @type helma.Zip.Content
     */
    this.list = function() {
        var result = new helma.Zip.Content();
        var zFile = new java.util.zip.ZipFile(file);
        var entries = zFile.entries();
        while (entries.hasMoreElements()) {
            result.add(new helma.Zip.Entry(entries.nextElement()));
        }
        zFile.close();
        return result;
    };

    /**
     * Extracts a single file from the zip archive represented
     * by this helma.Zip instance. If a destination path is given it
     * writes the extracted data directly to disk using the
     * name of the zip entry, otherwise the resulting entry object
     * contains the extracted data in the property <code>data</code>.
     * @param {String} name The name of the file to extract
     * @param {String} destPath An optional destination path where
     * the extracted file should be stored.
     * @returns An object containing the entry's properties
     * @type helma.Zip.Entry
     * @see helma.Zip.Entry
     */
    this.extract = function(name, destPath) {
        var zFile = new java.util.zip.ZipFile(file);
        var entry = zFile.getEntry(name);
        if (!entry)
            return null;
        var result = new helma.Zip.Entry(entry);
        result.data = extractEntry(zFile, entry, destPath);
        zFile.close();
        return result;
    };

    /**
     * Extracts all files within the zip archive represented by
     * this helma.Zip instance. If a destination path is given it
     * stores the files directly on disk, while preserving any directory
     * structure within the archive. If no destination path is given,
     * the resulting entry objects will contain the extracted data
     * in their property <code>data</code>.
     * @param {String} destPath An optional destination path where the
     * files in the zip archive should be stored.
     * @returns An object containing the extracted entries.
     * @type helma.Zip.Content
     * @see helma.Zip.Entry
     */
    this.extractAll = function(destPath) {
        var result = new helma.Zip.Content();
        var zFile = new java.util.zip.ZipFile(file);
        var entries = zFile.entries();
        while (entries.hasMoreElements()) {
            var entry = entries.nextElement();
            var e = new helma.Zip.Entry(entry);
            e.data = extractEntry(zFile, entry, destPath);
            result.add(e);
        }
        zFile.close();
        return result;
    };

    /**
     * Adds a single file or a whole directory (recursive!) to the zip archive
     * @param {helma.File|java.io.File|String} f Either a file object
     * or the path to a file or directory on disk that should be added to the
     * archive. If the argument represents a directory, its contents will be added
     * <em>recursively</em> to the archive.
     * @param {Number} level An optional compression level to use. The argument 
     * must be between zero and 9 (default: 9 = best compression).
     * @param {String} pathPrefix An optional path prefix to use within the archive.
     */
    this.add = function (f, level, pathPrefix) {
        var f = evalFile(f);

        // evaluate arguments
        if (arguments.length == 2) {
            if (typeof arguments[1] == "string") {
                pathPrefix = arguments[1];
                level = 9;
            } else {
                level = parseInt(arguments[1], 10);
                pathPrefix = null;
            }
        } else if (level == null || isNaN(level)) {
            level = 9;
        }
        // only levels between 0 and 9 are allowed
        level = Math.max(0, Math.min(9, level));

        if (f.isDirectory()) {
            // add a whole directory to the zip file (recursive!)
            var files = (new helma.File(f.getAbsolutePath())).listRecursive();
            for (var i in files) {
                var fAdd = new java.io.File(files[i]);
                if (!fAdd.isDirectory()) {
                    var p = fAdd.getPath().substring(f.getAbsolutePath().length, fAdd.getParent().length);
                    if (pathPrefix)
                        p = pathPrefix + p;
                    addFile(zOutStream, fAdd, level, p);
                }
            }
        } else {
            addFile(zOutStream, f, level, pathPrefix);
        }
        return;
    };

    /**
     * Adds a new entry to the zip file.
     * @param {ByteArray} buf A byte array containing the data to add
     * to the archive.
     * @param {String} name The name of the file to add, containing
     * an optional path prefix
     * @param {Number} level The compression level to use (0-9, defaults to 9).
     */
    this.addData = function(buf, name, level) {
        var entry = new java.util.zip.ZipEntry(name);
        entry.setSize(buf.length);
        entry.setTime(new Date());
        if (level == null || isNaN(level)) {
            zOutStream.setLevel(9);
        } else {
            zOutStream.setLevel(Math.max(0, Math.min(9, parseInt(level, 10))));
        }
        zOutStream.putNextEntry(entry);
        zOutStream.write(buf, 0, buf.length);
        zOutStream.closeEntry();
        return;
    };

    /**
     * Closes the zip archive. This method should be called when
     * all operations have been finished, to ensure that no open
     * file handles are left.
     */
    this.close = function() {
        zOutStream.close();
        return;
    };

    /**
     * Returns the binary data of the zip archive.
     * @returns A ByteArray containing the binary data of the zip archive
     * @type ByteArray
     */
    this.getData = function() {
        return bOutStream.toByteArray();
     };
    
    /**
     * Saves the archive.
     * @param {String} dest The full destination path including the name
     * where the zip archive should be saved.
     */
    this.save = function(dest) {
        if (!dest)
            throw new Error("no destination for ZipFile given");
        // first of all, close the ZipOutputStream
        zOutStream.close();
        var destFile = new java.io.File(dest);
        try {
            var outStream = new java.io.FileOutputStream(destFile);
            bOutStream.writeTo(outStream);
        } finally {
            outStream.close();
        }
        return;
    };

    /** @ignore */
    this.toString = function() {
        if (file) {
            return "[helma.Zip " + file.getAbsolutePath() + "]";
        } else {
            return "[helma.Zip]";
        }
    };

    /**
     * constructor body
     */
    var bOutStream = new java.io.ByteArrayOutputStream();
    var zOutStream = new java.util.zip.ZipOutputStream(bOutStream);
    if (file) {
        file = evalFile(file);
    }

    for (var i in this)
        this.dontEnum(i);

    return this;
}

/**
 * Creates a new helma.Zip.Content instance
 * @class Instances of this class represent the content
 * of a zip archive.
 * @constructor
 * @returns A newly created instance of helma.Zip.Content
 */
helma.Zip.Content = function() {
    /**
     * The table of contents of the archive
     * @type Array
     */
    this.toc = [];

    /**
     * The files contained in the zip archive, where
     * each directory level is a separate object containing
     * the entries (files and directories) as properties.
     */
    this.files = {};

    /**
     * Adds a zip entry object to the table of contents
     * and the files collection
     * @param {helma.Zip.Entry} entry The entry to add to the
     * zip archive
     */
    this.add = function(entry) {
        // add the file to the table of contents array
        this.toc[this.toc.length] = entry;
        // plus add it to the files tree
        var arr = entry.name.split(/[\\\/]/);
        var cnt = 0;
        var curr = this.files;
        var propName;
        while (cnt < arr.length-1) {
            propName = arr[cnt++];
            if (!curr[propName]) {
                curr[propName] = {};
            }
            curr = curr[propName];
        }
        curr[arr[cnt]] = entry;
        return;
    };

    for (var i in this)
        this.dontEnum(i);

    return this;
};


/** @ignore */
helma.Zip.Content.prototype.toString = function() {
    return "[helma.Zip.Content]";
}


/**
 * Creates a new instance of helma.Zip.Entry
 * @class Instances of this class represent a single zip archive entry,
 * containing the (meta)data of the entry.
 * @param {java.util.zip.ZipEntry} entry The zip entry object whose metadata
 * should be stored in this instance
 * @constructor
 * @returns A newly created helma.Zip.Entry instance.
 */
helma.Zip.Entry = function(entry) {
    /**
     * The name of the zip archive entry
     * @type String
     */
    this.name = entry.getName();

    /**
     * The size of the entry in bytes
     * @type Number
     */
    this.size = entry.getSize();

    /**
     * The file date of the entry
     * @type Date
     */
    this.time = entry.getTime() ? new Date(entry.getTime()) : null;

    /**
     * True if the entry is a directory, false otherwise
     * @type Boolean
     */
    this.isDirectory = entry.isDirectory();

    /**
     * The data of the zip entry
     * @type ByteArray 
     */
    this.data = null;

    for (var i in this)
        this.dontEnum(i);
    return this;
};


/** @ignore */
helma.Zip.Entry.prototype.toString = function() {
    return "[helma.Zip.Entry]";
}


/**
 * Extracts all files in the zip archive data passed as argument
 * and returns them.
 * @param {ByteArray} zipData A ByteArray containing the data of the zip archive
 * @returns The entries of the zip archive
 * @type helma.Zip.Content
 */
helma.Zip.extractData = function(zipData) {
    var zInStream = new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(zipData));
    var result = new helma.Zip.Content();

    var entry;
    while ((entry = zInStream.getNextEntry()) != null) {
        var eParam = new helma.Zip.Entry(entry);
        if (eParam.isDirectory)
            continue;
        if (eParam.size == -1)
            eParam.size = 16384;
        var bos = new java.io.ByteArrayOutputStream(eParam.size);
        var buf = java.lang.reflect.Array.newInstance(java.lang.Byte.TYPE, 8192);
        var count;
        while ((count = zInStream.read(buf)) != -1)
            bos.write(buf, 0, count);
        eParam.data = bos.toByteArray();
        eParam.size = bos.size();
        result.add(eParam);
    }
    zInStream.close();
    return result;
};


helma.lib = "Zip";
helma.dontEnum(helma.lib);
for (var i in helma[helma.lib])
    helma[helma.lib].dontEnum(i);
for (var i in helma[helma.lib].prototype)
    helma[helma.lib].prototype.dontEnum(i);
delete helma.lib;
