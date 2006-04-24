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
 * $RCSfile: helma.File.js,v $
 * $Author: czv $
 * $Revision: 1.9 $
 * $Date: 2006/04/18 13:06:58 $
 */


if (!global.helma) {
    global.helma = {};
}

helma.File = function(path) {
   var BufferedReader            = java.io.BufferedReader;
   var File                      = java.io.File;
   var Reader                    = java.io.Reader;
   var Writer                    = java.io.Writer;
   var FileReader                = java.io.FileReader;
   var FileWriter                = java.io.FileWriter;
   var PrintWriter               = java.io.PrintWriter;
   var EOFException              = java.io.EOFException;
   var IOException               = java.io.IOException;
   var IllegalStateException     = java.lang.IllegalStateException;
   var IllegalArgumentException  = java.lang.IllegalArgumentException

   var self = this;

   var file;
   try {
      if (arguments.length > 1)
         file = new File(path, arguments[1]);
      else
         file = new File(path);
   } catch (e) {
      throw(e);
   }

   var readerWriter;
   var atEOF = false;
   var lastLine = null;

   var setError = function(e) {
      this.lastError = e;
   };

   this.lastError = null;

   this.toString = function() {
      return file.toString();
   };

   this.getName = function() {
      var name = file.getName();
      return (name == null ? "" : name);
   };

   this.isOpened = function() {
      return (readerWriter != null);
   };

   this.open = function() {
      if (self.isOpened()) {
         setError(new IllegalStateException("File already open"));
         return false;
      }
      // We assume that the BufferedReader and PrintWriter creation
      // cannot fail except if the FileReader/FileWriter fails.
      // Otherwise we have an open file until the reader/writer
      // get garbage collected.
      try{
         if (file.exists()) {
            readerWriter = new BufferedReader(new FileReader(file));
         } else {
            readerWriter = new PrintWriter(new FileWriter(file));
         }
         return true;
      } catch (e) {
         setError(e);
         return false;
      }
      return;
   };

   this.exists = function() {
      return file.exists();
   };

   this.getParent = function() {
      if (!file.getParent())
         return null;
      return new helma.File(file.getParent());
   };

   this.readln = function() {
      if (!self.isOpened()) {
         setError(new IllegalStateException("File not opened"));
         return null;
      }
      if (!(readerWriter instanceof BufferedReader)) {
         setError(new IllegalStateException("File not opened for reading"));
         return null;
      }
      if (atEOF) {
         setError(new EOFException());
         return null;
      }
      if (lastLine != null) {
         var line = lastLine;
         lastLine = null;
         return line;
      }
      var reader = readerWriter;
      // Here lastLine is null, return a new line
      try {
         var line = readerWriter.readLine();
         if (line == null) {
            atEOF = true;
            setError(new EOFException());
         }
         return line;
      } catch (e) {
         setError(e);
         return null;
      }
      return;
   };

   this.write = function(what) {
      if (!self.isOpened()) {
         setError(new IllegalStateException("File not opened"));
         return false;
      }
      if (!(readerWriter instanceof PrintWriter)) {
         setError(new IllegalStateException("File not opened for writing"));
         return false;
      }
      if (what != null) {
         readerWriter.print(what.toString());
      }
      return true;
   };

   this.writeln = function(what) {
      if (self.write(what)) {
         readerWriter.println();
         return true;
      }
      return false;
   };

   this.isAbsolute = function() {
      return file.isAbsolute();
   };

   this.remove = function() {
      if (self.isOpened()) {
         setError(new IllegalStateException("An openened file cannot be removed"));
         return false;
      }
      return file["delete"]();
   };

   /*
    * will list all files within a directory
    * you may pass a RegExp Pattern to return just
    * files matching this pattern
    * @example var xmlFiles = dir.list(/.*\.xml/);
    * @param RegExp pattern to test each file name against
    * @return Array the list of file names
    */
   this.list = function(pattern) {
      if (self.isOpened())
         return null;
      if (!file.isDirectory())
         return null;
      if (pattern) {
         var fileList = file.list();
         var result = [];
         for (var i in fileList) {
            if (pattern.test(fileList[i]))
               result.push(fileList[i]);
         }
         return result;
      }
      return file.list();   
   };

   this.flush = function() {
      if (!self.isOpened()) {
         setError(new IllegalStateException("File not opened"));
         return false;
      }
      if (readerWriter instanceof Writer) {
         try {
            readerWriter.flush();
         } catch (e) {
           setError(e);
           return false;
         }
      } else {
         setError(new IllegalStateException("File not opened for write"));
         return false; // not supported by reader
      }
      return true;
   };

   this.close = function() {
      if (!self.isOpened())
         return false;
      try {
         readerWriter.close();
         readerWriter = null;
         return true;
      } catch (e) {
         setError(e);
         readerWriter = null;
         return false;
      }
   };

   this.getPath = function() {
      var path = file.getPath();
      return (path == null ? "" : path);
   };

   this.error = function() {
      if (this.lastError == null) {
         return "";
      } else {
         var exceptionName = this.lastError.getClass().getName();
         var l = exceptionName.lastIndexOf(".");
         if (l > 0)
            exceptionName = exceptionName.substring(l + 1);
         return exceptionName + ": " + this.lastError.getMessage();
      }
   };

   this.clearError = function() {
      this.lastError = null;
      return;
   };

   this.canRead = function() {
      return file.canRead();
   };

   this.canWrite = function() {
      return file.canWrite();
   };

   this.getAbsolutePath = function() {
      var absolutPath = file.getAbsolutePath();
      return (absolutPath == null ? "" : absolutPath);
   };

   this.getLength = function() {
      return file.length();
   };

   this.isDirectory = function() {
      return file.isDirectory();
   };

   this.isFile = function() {
      return file.isFile();
   };

   this.lastModified = function() {
      return file.lastModified();
   };

   this.makeDirectory = function() {
      if (self.isOpened())
         return false;
      // don't do anything if file exists or use multi directory version
      return (file.exists() || file.mkdirs());   
   };

   this.renameTo = function(toFile) {
      if (toFile == null) {
         setError(new IllegalArgumentException("Uninitialized target File object"));
         return false;
      }
      if (self.isOpened()) {
         setError(new IllegalStateException("An openened file cannot be renamed"));
         return false;
      }
      if (toFile.isOpened()) {
         setError(new IllegalStateException("You cannot rename to an openened file"));
         return false;
      }
      return file.renameTo(new java.io.File(toFile.getAbsolutePath()));
   };

   this.eof = function() {
      if (!self.isOpened()) {
         setError(new IllegalStateException("File not opened"));
         return true;
      }
      if (!(readerWriter instanceof BufferedReader)) {
         setError(new IllegalStateException("File not opened for read"));
         return true;
      }
      if (atEOF)
         return true;
      if (lastLine != null)
         return false;
      try {
         lastLine = readerWriter.readLine();
         if (lastLine == null)
            atEOF = true;
         return atEOF;
      } catch (e) {
         setError(e);
         return true;
      }
   };

   this.readAll = function() {
      // Open the file for readAll
      if (self.isOpened()) {
         setError(new IllegalStateException("File already open"));
         return null;
      }
      try { 
         if (file.exists()) {
            readerWriter = new BufferedReader(new FileReader(file));
         } else {
            setError(new IllegalStateException("File does not exist"));
            return null;
         }
         if (!file.isFile()) {
            setError(new IllegalStateException("File is not a regular file"));
            return null;
         }
      
         // read content line by line to setup proper eol
         var buffer = new java.lang.StringBuffer(file.length() * 1.10);
         while (true) {
            var line = readerWriter.readLine();
            if (line == null)
               break;
            if (buffer.length() > 0)
               buffer.append("\n");  // EcmaScript EOL
            buffer.append(line);
         }
     
         // Close the file
         readerWriter.close();
         readerWriter = null;
         return buffer.toString();
      } catch (e) {
         readerWriter = null;
         setError(e);
         return null;
      }
   };

   // DANGER! DANGER! HIGH VOLTAGE!
   // this method removes a directory recursively
   // without any warning or precautious measures
   this.removeDirectory = function() {
      if (!file.isDirectory())
         return false;
      var arr = file.list();
      for (var i=0; i<arr.length; i++) {
         var f = new helma.File(file, arr[i]);
         if (f.isDirectory())
            f.removeDirectory();
         else
            f.remove();
      }
      file["delete"]();
      return true;
   };

   /**
    * recursivly lists all files below a given directory
    * you may pass a RegExp Pattern to return just
    * files matching this pattern
    * @param RegExp pattern to test each file name against
    * @returns Array the list of absolute file paths
    */
   this.listRecursive = function(pattern) {
      if (!file.isDirectory())
         return false;
      if (!pattern || pattern.test(file.getName()))
         var result = [file.getAbsolutePath()];
      else
         var result = [];
      var arr = file.list();
      for (var i=0; i<arr.length; i++) {
         var f = new helma.File(file, arr[i]);
         if (f.isDirectory())
            result = result.concat(f.listRecursive(pattern));
         else if (!pattern || pattern.test(arr[i]))
            result.push(f.getAbsolutePath());
      }
      return result;
   }

   /**
    * function makes a copy of a file over partitions
    * @param StringOrFile full path of the new file
    */
   this.hardCopy = function(dest) {
      var inStream = new java.io.BufferedInputStream(
         new java.io.FileInputStream(file)
      );
      var outStream = new java.io.BufferedOutputStream(
         new java.io.FileOutputStream(dest)
      );
      var buffer = java.lang.reflect.Array.newInstance(
         java.lang.Byte.TYPE, 4096
      );
      var bytesRead = 0;
      while ((bytesRead = inStream.read(buffer, 0, buffer.length)) != -1) {
         outStream.write(buffer, 0, bytesRead);
      }
      outStream.flush();
      inStream.close();
      outStream.close();
      return true;
   }

   /**
    * function moves a file to a new destination directory
    * @param String full path of the new file
    * @return Boolean true in case file could be moved, false otherwise
    */
   this.move = function(dest) {
      // instead of using the standard File method renameTo()
      // do a hardCopy and then remove the source file. This way
      // file locking shouldn't be an issue
      self.hardCopy(dest);
      // remove the source file
      file["delete"]();
      return true;
   }

   /**
    * returns file as ByteArray
    * useful for passing it to a function instead of an request object
    */
   this.toByteArray = function() {
      if (!this.exists())
         return null;
      var body = new java.io.ByteArrayOutputStream();
      var stream = new java.io.BufferedInputStream(
         new java.io.FileInputStream(this.getAbsolutePath())
      );
      var buf = java.lang.reflect.Array.newInstance(
         java.lang.Byte.TYPE, 1024
      );
      var read;
      while ((read = stream.read(buf)) > -1)
         body.write(buf, 0, read);
      stream.close();
      return body.toByteArray();
   };

   for (var i in this)
      this.dontEnum(i);

   return this;
}


helma.File.toString = function() {
   return "[helma.File]";
};


helma.File.separator = java.io.File.separator;


helma.lib = "File";
helma.dontEnum(helma.lib);
for (var i in helma[helma.lib])
   helma[helma.lib].dontEnum(i);
for (var i in helma[helma.lib].prototype)
   helma[helma.lib].prototype.dontEnum(i);
delete helma.lib;
