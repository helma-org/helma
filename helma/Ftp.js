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
 * $RCSfile: helma.Ftp.js,v $
 * $Author: czv $
 * $Revision: 1.7 $
 * $Date: 2006/04/18 13:06:58 $
 */


if (!global.helma) {
    global.helma = {};
}

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

   var FTP                    = Packages.com.oroinc.net.ftp.FTP;
   var FtpClient              = Packages.com.oroinc.net.ftp.FTPClient;
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
      var msg = msg ? " " + msg : "";
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

   this.toString = function() {
      return "[helma.Ftp " + server + "]";
   };

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

   this.login = function(username, password) {
      try {
         ftpclient.connect(this.server);
         ftpclient.login(username, password);
         debug("login", username + "@" + server);
         return true;
      } catch(x) {
         error("login", x);
         setStatus(LOGIN);
      }
      return false;
   };

   this.binary = function() {
      try {
         ftpclient.setFileType(FTP.BINARY_FILE_TYPE);
         debug("binary");
         return true;
      } catch(x) {
         error("binary", x);
         setStatus(BINARY);
      }
      return false;
   };

   this.ascii = function() {
      try {
         ftpclient.setFileType(FTP.ASCII_FILE_TYPE);
         debug("ascii");
         return true;
      } catch(x) {
         error("ascii", x);
         setStatus(ASCII);
      }
      return false;
   };

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

   this.mkdir = function(dir) {
      try {
         ftpclient.makeDirectory(dir);
         debug("mkdir", dir);
         return true;
      } catch(x) {
         error("mkdir", x);
         setStatus(MKDIR);
      }
      return false;
   };

   this.rmdir = function(dir) {
      try {
         ftpclient.removeDirectory(dir);
         debug("rmdir", dir);
         return true;
      } catch(x) {
         error("rmdir", x);
         setStatus(RMDIR);
      }
      return false;
   };

   this.cd = function(path) {
      try {
         ftpclient.changeWorkingDirectory(path);
         debug("cd", path);
         return true;
      } catch(x) {
         error("cd", x);
         setStatus(CD);
      }
      return false;
   };

   this.lcd = function(dir) {
      try {
         localDir = new File(dir);
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

   this.putFile = function(localFile, remoteFile) {
      try {
         if (localFile instanceof File) {
            var f = localFile;
         } else if (typeof localFile == "string") {
            if (localDir == null)
               var f = new File(localFile);
            else
               var f = new File(localDir, localFile);
         }
         var stream = new BufferedInputStream(
            new FileInputStream(f.getPath())
         );
         if (!remoteFile) {
            remoteFile = f.getName();
         }
         ftpclient.storeFile(remoteFile, stream);
         stream.close();
         debug("putFile", remoteFile);
         return true;
      } catch(x) {
         error("putFile", x);
         setStatus(PUT);
      }
      return false;
   };

   this.putString = function(str, remoteFile, charset) {
      try {
         str = new java.lang.String(str);
         var bytes = charset ? str.getBytes(charset) : str.getBytes();
         var stream = ByteArrayInputStream(bytes);
         ftpclient.storeFile(remoteFile, stream);
         debug("putString", remoteFile);
         return true;
      } catch(x) {
         error("putString", x);
         setStatus(PUT);
      }
      return false;
   };

   this.getFile = function(remoteFile, localFile) {
      try {
         if (localDir == null)
            var f = new File(localFile);
         else
            var f = new File(localDir, localFile);
         var stream = new BufferedOutputStream(
            new FileOutputStream(f.getPath())
         );
         ftpclient.retrieveFile(remoteFile, stream);
         stream.close();
         debug("getFile", remoteFile);
         return true;
      } catch(x) {
         error("getFile", x);
         setStatus(GET);
      }
      return false;
   };

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

   this.deleteFile = function(remoteFile) {
      try {
         ftpclient.deleteFile(remoteFile);
         debug("deleteFile", remoteFile);
         return true;
      } catch(x) {
         error("deleteFile", x);
         setStatus(DELETE);
      }
      return false;
   };

   this.logout = function() {
      try {
         ftpclient.logout();
         ftpclient.disconnect();
         debug("logout");
         return true;
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
