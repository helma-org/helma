/*
 * Helma License Notice
 *
 * The contents of this file are subject to the Helma License
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://adele.helma.org/download/helma/license.txt
 *
 * Copyright 1998-2007 Helma Software. All Rights Reserved.
 *
 * $RCSfile: Ftp.js,v $
 * $Author$
 * $Revision$
 * $Date$
 */


/**
 * @fileoverview Default properties and methods of the FtpClient prototype.
 * <br /><br />
 * To use this optional module, its repository needs to be added to the 
 * application, for example by calling app.addRepository('modules/helma/Ftp.js')
 */

// requires helma.File
app.addRepository("modules/helma/File.js");

/**
 * Define the global namespace if not existing
 */
if (!global.helma) {
    global.helma = {};
}

/**
 * Constructor for FTP client objects, to send and receive files from an FTP server.
 * <br /><br />
 * @class This class represents a FTP client, providing 
 * access to an FTP server.
 * The FTP client needs Daniel Savarese's NetComponents 
 * library in the classpath in order to work.
 * 
 * @example var ftp = new FtpClient("ftp.mydomain.com");
 * @param {String} server as String, the address of the FTP Server to connect to
 * @constructor
 */
helma.Ftp = function(server) {
   var OK      =  0;
   var SOCKET  =  1;
   var TIMEOUT =  2;
   var LOGIN   = 10;
   var LOGOUT  = 11;
   var BINARY  = 20;
   var ASCII   = 21;
   var ACTIVE  = 22;
   var PASSIVE = 23;
   var CD      = 30;
   var LCD     = 31;
   var PWD     = 32;
   var DIR     = 33;
   var MKDIR   = 34;
   var RMDIR   = 35;
   var GET     = 40;
   var PUT     = 41;
   var DELETE  = 42;

   var FTP                    = Packages.org.apache.commons.net.ftp.FTP;
   var FtpClient              = Packages.org.apache.commons.net.ftp.FTPClient;
   var BufferedInputStream    = java.io.BufferedInputStream;
   var BufferedOutputStream   = java.io.BufferedOutputStream;
   var FileInputStream        = java.io.FileInputStream;
   var FileOutputStream       = java.io.FileOutputStream;
   var ByteArrayInputStream   = java.io.ByteArrayInputStream;
   var ByteArrayOutputStream  = java.io.ByteArrayOutputStream;

   var self = this;
   var className = "helma.Ftp";

   var ftpclient = new FtpClient();
   var localDir;

   var error = function(methName, errMsg) {
      var tx = java.lang.Thread.currentThread();
      tx.dumpStack();
      app.log("Error in " + className + ":" + methName + ": " + errMsg);
      return;
   };

   var debug = function(methName, msg) {
      msg = msg ? " " + msg : "";
      app.debug(className + ":" + methName + msg);
      return;
   };

   var setStatus = function(status) {
      if (self.status === OK) {
         self.status = status;
      }
      return;
   };

   var getStatus = function() {
      return self.status;
   };

   this.server = server;
   this.status = OK;
   
   /** @ignore */
   this.toString = function() {
      return "[helma.Ftp " + server + "]";
   };

   /**
    * Set the default timeout in milliseconds to use when opening a socket.
    */
   this.setReadTimeout = function(timeout) {
      try {
         ftpclient.setDefaultTimeout(timeout);
         debug("setReadTimeout", timeout);
         return true;
      } catch(x) {
         error("setReadTimeout", x);
         setStatus(SOCKET);
      }
      return false;
   };

   /**
    * Sets the timeout in milliseconds to use when reading from the data connection.
    */
   this.setTimeout = function(timeout) {
      try {
         ftpclient.setDataTimeout(timeout);
         debug("setTimeout", timeout);
         return true;
      } catch(x) {
         error("setTimeout", x);
         setStatus(TIMEOUT);
      }
      return false;
   };

   /**
    * Logs in to the FTP server.
    * 
    * @param {String} username as String
    * @param {String} password as String
    * @return Boolean true if the login was successful, otherwise false
    * @type Boolean
    */
   this.login = function(username, password) {
      try {
         ftpclient.connect(this.server);
         var result = ftpclient.login(username, password);
         debug("login", username + "@" + server);
         return result;
      } catch(x) {
         error("login", x);
         setStatus(LOGIN);
      }
      return false;
   };

   /**
    * Sets transfer mode to binary for transmitting images and other non-text files.
    * 
    * @example ftp.binary();
    */
   this.binary = function() {
      try {
         var result = ftpclient.setFileType(FTP.BINARY_FILE_TYPE);
         debug("binary");
         return result;
      } catch(x) {
         error("binary", x);
         setStatus(BINARY);
      }
      return false;
   };

   /**
    * Sets transfer mode to ascii for transmitting text-based data.
    * 
    * @example ftp.ascii();
    */
   this.ascii = function() {
      try {
         var result = ftpclient.setFileType(FTP.ASCII_FILE_TYPE);
         debug("ascii");
         return result;
      } catch(x) {
         error("ascii", x);
         setStatus(ASCII);
      }
      return false;
   };

   /**
    * Switches the connection to use active mode.
    * 
    * @example ftp.active();
    */
   this.active = function() {
      try {
         ftpclient.enterLocalActiveMode();
         debug("active");
         return true;
      } catch(x) {
         error("active", x);
         setStatus(ACTIVE);
      }
      return false;
   };

   /**
    * Switches the connection to use passive mode.
    * 
    * @example ftp.passive();
    */
   this.passive = function() {
      try {
         ftpclient.enterLocalPassiveMode();
         debug("passive");
         return true;
      } catch(x) {
         error("passive", x);
         setStatus(PASSIVE);
      }
      return false;
   };

   /**
    * Returns the path of the current working directory.
    * 
    * @example var remotepath = ftp.pwd();
    * @type String
    * @return String containing the current working directory path
    */
   this.pwd = function() {
      try {
         debug("pwd");
         return ftpclient.printWorkingDirectory();
      } catch(x) {
         error("pwd", x);
         setStatus(PWD);
      }
      return;
   };

   /**
    * Returns a listing of the files contained in a directory on the FTP server.
    * <br /><br />
    * Lists the files contained in the current working 
    * directory or, if an alternative path is specified, the
    * files contained in the specified directory.
    * 
    * @example var filelist = ftp.dir();
    * @param {String} path as String, optional alternative directory
    * @return Array containing the list of files in that directory
    * @type Array
    */
   this.dir = function(path) {
      try {
         debug("dir", path);
         return ftpclient.listNames(path ? path : ".");
      } catch(x) {
         error("dir", x);
         setStatus(DIR);
      }
      return;
   };

   /**
    * Creates a new directory on the server.
    * <br /><br />
    * The name of the directory is determined as the function's 
    * string parameter. Returns false when an error occured 
    * (e.g. due to access restrictions, directory already 
    * exists etc.), otherwise true.
    * 
    * @param {String} dir as String, the name of the directory to be created
    * @return Boolean true if the directory was successfully created, false if there was an error
    * @type Boolean
    */
   this.mkdir = function(dir) {
      try {
         var result = ftpclient.makeDirectory(dir);
         debug("mkdir", dir);
         return result;
      } catch(x) {
         error("mkdir", x);
         setStatus(MKDIR);
      }
      return false;
   };

   /**
    * Deletes a directory on the FTP server.
    * 
    * @param {String} dir as String, the name of the directory to be deleted
    * @return Boolean true if the deletion was successful, false otherwise
    * @type Boolean
    */
   this.rmdir = function(dir) {
      try {
         var result = ftpclient.removeDirectory(dir);
         debug("rmdir", dir);
         return result;
      } catch(x) {
         error("rmdir", x);
         setStatus(RMDIR);
      }
      return false;
   };

   /**
    * Changes the working directory on the FTP server.
    * 
    * @example ftp.cd("/home/users/fred/www"); // use absolute pathname
    * @example ftp.cd(".."); // change to parent directory
    * @example ftp.cd("images"); // use relative pathname
    * @param {String} dir as String, the path that the remote working directory should be changed to
    */
   this.cd = function(path) {
      try {
         var result = ftpclient.changeWorkingDirectory(path);
         debug("cd", path);
         return result;
      } catch(x) {
         error("cd", x);
         setStatus(CD);
      }
      return false;
   };

   /**
    * Changes the working directory of the local machine when being connected to an FTP server.
    * 
    * @example ftp.lcd("/home/users/fred/www"); // use absolute pathname
    * @example ftp.lcd(".."); // change to parent directory
    * @example ftp.lcd("images"); // use relative pathname
    * @param {String} dir as String, the path that the local working directory should be changed to
    */
   this.lcd = function(dir) {
      try {
         localDir = new helma.File(dir);
         if (!localDir.exists()) {
            localDir.mkdir();
            debug("lcd", dir);
         }
         return true;
      } catch(x) {
         error("lcd", x);
         setStatus(LCD);
      }
      return false;
   };

   /**
    * Transfers a file from the local file system to the remote server.
    * <br /><br />
    * Returns true if the transmission was successful, otherwise false.
    * 
    * @param {String} localFile as String, the name of the file to be uploaded
    * @param {String} remoteFile as String, the name of the remote destination file
    * @return Boolean true if the file was successfully uploaded, false if there was an error
    * @type Boolean
    */
   this.putFile = function(localFile, remoteFile) {
      try {
         if (localFile instanceof File || localFile instanceof helma.File) {
            var f = localFile;
         } else if (typeof localFile == "string") {
            if (localDir == null)
               var f = new helma.File(localFile);
            else
               var f = new helma.File(localDir, localFile);
         }
         var stream = new BufferedInputStream(
            new FileInputStream(f.getPath())
         );
         if (!remoteFile) {
            remoteFile = f.getName();
         }
         var result = ftpclient.storeFile(remoteFile, stream);
         stream.close();
         debug("putFile", remoteFile);
         return result;
      } catch(x) {
         error("putFile", x);
         setStatus(PUT);
      }
      return false;
   };

   /**
    * Transfers text from a string to a file on the FTP server.
    * 
    * @example ftp.putString("Hello, World!", "message.txt");
    * @param {String} str as String, the text content that should be uploaded 
    * @param {String} remoteFile as String, the name of the remote destination file
    * @param {String} charset as String, optional
    * @return Boolean true if the file was successfully uploaded, false if there was an error
    * @type Boolean
    */
   this.putString = function(str, remoteFile, charset) {
      try {
         str = new java.lang.String(str);
         var bytes = charset ? str.getBytes(charset) : str.getBytes();
         var stream = ByteArrayInputStream(bytes);
         var result = ftpclient.storeFile(remoteFile, stream);
         debug("putString", remoteFile);
         return result;
      } catch(x) {
         error("putString", x);
         setStatus(PUT);
      }
      return false;
   };

   /**
    * Transfers a byte array to a file on the FTP server.
    * @param {Array} bytes The byte array that should be uploaded 
    * @param {String} remoteFile The name of the remote destination file
    * @return Boolean True if the file was successfully uploaded, false if there was an error
    * @type Boolean
    */
   this.putBytes = function(bytes, remoteFile) {
      try {
         var stream = ByteArrayInputStream(bytes);
         var result = ftpclient.storeFile(remoteFile, stream);
         debug("putBytes", remoteFile);
         return result;
      } catch(x) {
         error("putBytes", x);
         setStatus(PUT);
      }
      return false;
   };

   /**
    * Transfers a file from the FTP server to the local file system.
    * 
    * @example ftp.getFile(".htaccess", "htaccess.txt");
    * @param {String} remoteFile as String, the name of the file that should be downloaded
    * @param {String} localFile as String, the name which the file should be stored under
    * @see #cd
    * @see #lcd
    */
   this.getFile = function(remoteFile, localFile) {
      try {
         if (localDir == null)
            var f = new helma.File(localFile);
         else
            var f = new helma.File(localDir, localFile);
         var stream = new BufferedOutputStream(
            new FileOutputStream(f.getPath())
         );
         var result = ftpclient.retrieveFile(remoteFile, stream);
         stream.close();
         debug("getFile", remoteFile);
         return result;
      } catch(x) {
         error("getFile", x);
         setStatus(GET);
      }
      return false;
   };

   /**
    * Retrieves a file from the FTP server and returns it as string.
    * 
    * @example var str = ftp.getString("messages.txt");
    * @param {String} remoteFile as String, the name of the file that should be downloaded
    * @return String containing the data of the downloaded file
    * @type String
    * @see #cd
    */
   this.getString = function(remoteFile) {
      try {
         var stream = ByteArrayOutputStream();
         ftpclient.retrieveFile(remoteFile, stream);
         debug("getString", remoteFile);
         return stream.toString();
      } catch(x) {
         error("getString", x);
         setStatus(GET);
      }
      return;
   };

   /**
    * Deletes a file on the FTP server.
    * 
    * @example var str = ftp.delete("messages.txt");
    * @param {String} remoteFile as String, the name of the file to be deleted
    * @return Boolean true if the deletion was successful, false otherwise
    * @type Boolean
    */
   this.deleteFile = function(remoteFile) {
      try {
         var result = ftpclient.deleteFile(remoteFile);
         debug("deleteFile", remoteFile);
         return result;
      } catch(x) {
         error("deleteFile", x);
         setStatus(DELETE);
      }
      return false;
   };

   /**
    * Terminates the current FTP session.
    */
   this.logout = function() {
      try {
         var result = ftpclient.logout();
         ftpclient.disconnect();
         debug("logout");
         return result;
      } catch(x) {
         error("logout", x);
         setStatus(LOGOUT);
      }
      return false;
   };

   for (var i in this)
      this.dontEnum(i);

   return this;
}


/** @ignore */
helma.Ftp.toString = function() {
   return "[helma.Ftp]";
};


helma.lib = "Ftp";
helma.dontEnum(helma.lib);
for (var i in helma[helma.lib])
   helma[helma.lib].dontEnum(i);
for (var i in helma[helma.lib].prototype)
   helma[helma.lib].prototype.dontEnum(i);
delete helma.lib;
