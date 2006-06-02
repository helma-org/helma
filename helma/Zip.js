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
 * $Author: czv $
 * $Revision: 1.2 $
 * $Date: 2006/04/24 07:02:17 $
 */

// take care of any dependencies
app.addRepository('modules/helma/File.js');


if (!global.helma) {
    global.helma = {};
}

/**
 * constructor function for Zip Objects
 * @param either
 *      - (Object) a File object
 *      - (Object) an instance of Helma.File
 *      - (Object) an instance of java.io.File
 *      - (String) the path to the zip file
 */
helma.Zip = function (file) {

    /**
     * private function that extracts the data of a file
     * in a .zip archive. If a destination Path is given it
     * writes the extracted data directly to disk using the
     * name of the ZipEntry Object, otherwise it returns the
     * byte array containing the extracted data
     * @param Object jAva.util.zip.ZipFile Object
     * @param Object java.util.zip.ZipEntry Object to extract
     * @param String destination path to extract ZipEntry Object to
     * @return Object ByteArray containing the data of the ZipEntry
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
     * private function for adding a single file to the .zip archive
     * @param Object java.util.zip.ZipOutputStream
     * @param Object instance of java.io.File representing the file to be added
     * @param Int compression-level (0-9)
     * @param String path of the directory in the archive to which the
     *                    file should be added (optional)
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
        return true;
    };

    /**
     * private function that constructs an instance
     * of java.io.File based on a JS File or Helma.File object
     * @param Object either a string or an instance of java.io.File, File or Helma.File
     * @return Object instance of java.io.File
     */
    var evalFile = function(f) {
        if (f instanceof java.io.File)
            return f;
        var result;
        if (typeof f == "string")
            result = new java.io.File(f);
        else
            result = new java.io.File(f.getAbsolutePath());
        if (!result.exists())
            throw("Error creating Zip Object: File '" + f + "' doesn't exist.");
        return result;
    };
    
    /**
     * returns an array containing the entries of a .zip file as objects
     * (see Entry for description)
     * @param Object File object representing the .zip file on disk
     * @return Object result object
     */
    this.list = function() {
        var result = new helma.Zip.Content();
        var zFile = new java.util.zip.ZipFile(file);
        var entries = zFile.entries();
        while (entries.hasMoreElements())
            result.add(new helma.Zip.Entry(entries.nextElement()));
        zFile.close();
        return result;
    };

    /**
     * extracts a single file from a .zip archive
     * if a destination path is given it directly writes
     * the extracted file to disk
     * @param Object File object representing the .zip file on disk
     * @param String Name of the file to extract
     * @param String (optional) destination path to write file to
     * @return Object JS Object (see Entry for description)
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
     * extracts all files in a .zip archive
     * if a destination path is given it directly writes
     * the extracted files to disk (preserves directory structure
     * of .zip archive if existing!)
     * @param String (optional) destination path to write file to
     * @return Object Array containing JS objects for each entry
     *                     (see Entry for description)
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
     * adds a single file or a whole directory (recursive!) to the .zip archive
     * @param either
     *          - (Object) a File object
     *          - (Object) an instance of java.io.File
     *          - (String) the path to the file that should be added
     * @param Int Level to use for compression (default: 9 = best compression)
     * @param String name of the directory in the archive into which the
     *                    file should be added (optional)
     * @return Boolean true
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
        } else
            addFile(zOutStream, f, level, pathPrefix);
        return true;
    };

    /**
     * adds a new entry to the zip file
     * @param Object byte[] containing the data to add
     * @param String name of the file to add
     * @param Int compression level (0-9, default: 9)
     * @return Boolean true
     */
    this.addData = function(buf, name, level) {
        var entry = new java.util.zip.ZipEntry(name);
        entry.setSize(buf.length);
        entry.setTime(new Date());
        if (level == null || isNaN(level))
            zOutStream.setLevel(9);
        else
            zOutStream.setLevel(Math.max(0, Math.min(9, parseInt(level, 10))));
        zOutStream.putNextEntry(entry);
        zOutStream.write(buf, 0, buf.length);
        zOutStream.closeEntry();
        return true;
    };

    /**
     * closes the ZipOutputStream
     */
    this.close = function() {
        zOutStream.close();
        return;
    };

    /**
     * returns the binary data of the zip file
     * @return Object ByteArray containing the binary data of the zip file
     */
    this.getData = function() {
        return bOutStream.toByteArray();
     };
    
    /**
     * saves the archive by closing the output stream
     * @param String path (including the name) to save the zip file to
     * @return Boolean true
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
        return true;
    };

    this.toString = function() {
        if (file)
            return "[Zip Object " + file.getAbsolutePath() + "]";
        else
            return "[Zip Object]";
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
 * constructor for Content Objects
 */
helma.Zip.Content = function() {
    this.toc = [];
    this.files = {};

    /**
     * adds a Zip Entry object to the table of contents
     * and the files collection
     * @param Object instance of helma.Zip.Entry
     */
    this.add = function(entry) {
        // add the file to the table of contents array
        this.toc.push(entry);
        // plus add it to the files tree
        var re = /[\\\/]/;
        var arr = entry.name.split(re);
        var cnt = 0;
        var curr = this.files;
        var propName;
        while (cnt < arr.length-1) {
            propName = arr[cnt++];
            if (!curr[propName])
                curr[propName] = new Object();
            curr = curr[propName];
        }
        curr[arr[cnt]] = entry;
        return;
    };

    for (var i in this)
        this.dontEnum(i);

    return this;
};


/**
 * constructor for Entry objects holding the meta-data of a zip entry:
 *     .name (String) name of the entry
 *     .size (Int) decompressed size of the entry in bytes
 *     .time (Date) last modification timestamp of the entry or null
 *     .isDirectory (Boolean) true in case entry is a directory, false otherwise
 *     .data (ByteArray) ByteArray containing the data of the entry
 * @param Object java.util.zip.ZipEntry Object
 */
helma.Zip.Entry = function(entry) {
    this.name = entry.getName();
    this.size = entry.getSize();
    this.time = entry.getTime() ? new Date(entry.getTime()) : null;
    this.isDirectory = entry.isDirectory();
    this.data = null;
    for (var i in this)
        this.dontEnum(i);
    return this;
};


/**
 * extract all files in a ByteArray passed as argument
 * and return them as result Array
 * @param Object ByteArray containing the data of the .zip File
 * @return Object instance of helma.Zip.Content
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
