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
 * $RCSfile: Ssh.js,v $
 * $Author$
 * $Revision$
 * $Date$
 */


/**
 * @fileoverview Fields and methods of the helma.Ssh class.
 */

// take care of any dependencies
app.addRepository('modules/helma/File.js');
app.addRepository('modules/helma/ganymed-ssh2.jar');

// define the helma namespace, if not existing
if (!global.helma) {
    global.helma = {};
}

/** 
 * Creates a new instance of helma.Ssh
 * @class This class provides methods for connecting to a remote
 * server via secure shell (ssh) and copying files from/to a remote
 * server using secure copy (scp). It utilizes "Ganymed SSH-2 for Java"
 * (see <a href="http://www.ganymed.ethz.ch/ssh2/">http://www.ganymed.ethz.ch/ssh2/</a>).
 * @param {String} server The server to connect to
 * @param {helma.File|java.io.File|String} hosts Either a file
 * containing a list of known hosts, or the path pointing to a
 * file. This argument is optional.
 * @constructor
 * @returns A newly created instance of helma.Ssh
 * @author Robert Gaggl <robert@nomatic.org> 
 */
helma.Ssh = function(server, hosts) {
    var SSHPKG = Packages.ch.ethz.ssh2;
    var SSHPKGNAME = "ganymed-ssh2.jar";
    var SSHPKGURL = "http://www.ganymed.ethz.ch/ssh2";
    var className = "helma.Ssh";
    var paranoid = false;
    var verifier = null;
    var knownHosts, connection;

    // check if necessary jar file is in classpath
    try {
        knownHosts = new SSHPKG.KnownHosts();
        connection = new SSHPKG.Connection(server);
    } catch (e) {
        if (e instanceof TypeError == false)
            throw(e);
        throw("helma.Ssh needs " + SSHPKGNAME + 
              " in lib/ext or application directory " +
              "[" + SSHPKGURL + "]");
    }
    
    /**
     * A simple verifier for verifying host keys
     * @private
     */
    var SimpleVerifier = {
        verifyServerHostKey: function(hostname, port, serverHostKeyAlgorithm, serverHostKey) {
            var result = knownHosts.verifyHostkey(hostname, serverHostKeyAlgorithm, serverHostKey);
            switch (result) {
                case SSHPKG.KnownHosts.HOSTKEY_IS_OK:
                    debug("verifyServerHostKey", "received known host key, proceeding");
                    return true;
                case SSHPKG.KnownHosts.HOSTKEY_IS_NEW:
                    if (paranoid == true) {
                        debug("verifyServerHostKey", "received unknown host key, rejecting");
                        return false;
                    } else {
                        debug("verifyServerHostKey", "received new host key, adding temporarily to known hosts");
                        var hn = java.lang.reflect.Array.newInstance(java.lang.String, 1);
                        hn[0] = hostname;
                        knownHosts.addHostkey(hn, serverHostKeyAlgorithm, serverHostKey);
                        return true;
                    }
                case SSHPKG.KnownHosts.HOSTKEY_HAS_CHANGED:
                    debug("verifyServerHostKey", "WARNING: host key has changed, rejecting");
                default:
                    return false;
            }
            return;
        }
    };
    
    /**
     * Converts the argument into an instance of java.io.File
     * @param {helma.File|java.io.File|String} file Either a file
     * object or the path to a file as string
     * @returns The argument converted into a file object
     * @type java.io.File
     * @private
     */
    var getFile = function(file) {
        if (file instanceof helma.File) {
            return new java.io.File(file.getAbsolutePath());
        } else if (file instanceof java.io.File) {
            return file;
        } else if (file.constructor == String) {
            return new java.io.File(file);
        }
        return null;
    };

    /**
     * Connects to the remote server
     * @return Boolean
     * @private
     */
    var connect = function() {
        try {
            var info = connection.connect(verifier);
            debug("connect", "connected to server " + server);
            return true;
        } catch (e) {
            error("connect", "connection to " + server + " failed.");
        }
        return false;
    };
    
    /**
     * Private helper method for debugging output using app.logger
     * @param {String} methodName The name of the method
     * @param {String} msg The debug message to write to event log file
     * @private
     */
    var debug = function(methodName, msg) {
        var msg = msg ? " " + msg : "";
        app.logger.debug(className + ":" + methodName + msg);
        return;
    };

    /**
     * Private helper method for error output using app.logger
     * @param {String} methodName The name of the method
     * @param {String} msg The error message to write to event log file
     * @private
     */
    var error = function(methodName, msg) {
        var tx = java.lang.Thread.currentThread();
        tx.dumpStack();
        app.logger.error(className + ":" + methodName + ": " + msg);
        return;
    };

    /**
     * Opens the file passed as argument and adds the known hosts
     * therein to the list of known hosts for this client.
     * @param {helma.File|java.io.File|String} file Either a file object
     * or the path to a file containing a list of known hosts
     * @returns True if adding the list was successful, false otherwise
     * @type Boolean
     */
    this.addKnownHosts = function(file) {
        try {
            knownHosts.addHostkeys(getFile(file));
            verifier = new SSHPKG.ServerHostKeyVerifier(SimpleVerifier);
            return true;
        } catch (e) {
            error("addKnownHosts", "Missing or invalid known hosts file '" + file + "'");
        }
        return false;
    };
    
    /**
     * Connects to a remote host using plain username/password authentication.
     * @param {String} username The username
     * @param {String} password The password
     * @returns True in case the connection attempt was successful, false otherwise.
     * @type Boolean
     */
    this.connect = function(username, password) {
        if (!username || !password) {
            error("connect", "Insufficient arguments.");
        } else if (connect() && connection.authenticateWithPassword(username, password)) {
            debug("connect", "authenticated using password");
            return true;
        } else {
            error("connect", "Authentication failed!");
        }
        return false;
    };

    /**
     * Connects to a remote host using a private key and the corresponding
     * passphrase.
     * @param {String} username The username
     * @param {helma.File|java.io.File|String} key Either a file object
     * representing the private key file, or the path to it.
     * @param {String} passphrase The passphrase of the private key, if necessary.
     * @returns True in case the connection attempt was successful, false otherwise.
     * @type Boolean
     */
    this.connectWithKey = function(username, key, passphrase) {
        var keyFile;
        if (!username || !(keyFile = getFile(key))) {
            error("connectWithKey", "Insufficient or wrong arguments.");
        } else if (connect() && connection.authenticateWithPublicKey(username, keyFile, passphrase)) {
            debug("connectWithKey", "authenticated with key");
            return true;
        } else {
            error("connectWithKey", "Authentication failed!");
        }
        return false;
    };
    
    /**
     * Disconnects this client from the remote server.
     */
    this.disconnect = function() {
        connection.close();
        debug("disconnect", "disconnected from server " + server);
        return;
    };

    /**
     * Returns true if this client is currently connected.
     * @returns True in case this client is connected, false otherwise.
     * @type Boolean
     */
    this.isConnected = function() {
        return (connection != null && connection.isAuthenticationComplete());
    };

    /**
     * Copies a local file to the remote server
     * @param {String|Array} localFile Either the path to a single local
     * file or an array containing multiple file paths that should be
     * copied to the remote server.
     * @param {String} remoteDir The path to the remote destination directory
     * @param {String} mode An optional 4-digit permission mode string (eg.
     * <code>0755</code>);
     * @returns True in case the operation was successful, false otherwise.
     * @type Boolean
     */
    this.put = function(localFile, remoteDir, mode) {
        if (!localFile || !remoteDir) {
            error("put", "Insufficient arguments.");
        } else if (!this.isConnected()) {
            error("put", "Not connected. Please establish a connection first.");
        } else {
            try {
                var scp = connection.createSCPClient();
                if (mode != null)
                    scp.put(localFile, remoteDir, mode);
                else
                    scp.put(localFile, remoteDir);
                debug("put", "copied '" + localFile + "' to '" + remoteDir + "'");
                return true;
            } catch (e) {
                error("put", e);
            }
        }
        return false;
    };

    /**
     * Retrieves a file from the remote server and stores it locally.
     * @param {String|Array} remoteFile Either the path to a single remote
     * file or an array containing multiple file paths that should be
     * copied onto the local disk.
     * @param {String} targetDir The path to the local destination directory
     * @returns True if the copy process was successful, false otherwise.
     * @type Boolean
     */
    this.get = function(remoteFile, targetDir) {
        if (!remoteFile || !targetDir) {
            error("get", "Insufficient arguments.");
        } else if (!this.isConnected()) {
            error("get", "Not connected. Please establish a connection first.");
        } else {
            try {
                var scp = connection.createSCPClient();
                scp.get(remoteFile, targetDir);
                debug("get", "copied '" + remoteFile + "' to '" + targetDir + "'");
                return true;
            } catch (e) {
                error("get", e);
            }
        }
        return false;
    };

    /**
     * Executes a single command on the remote server.
     * @param {String} cmd The command to execute on the remote server.
     * @return The result of the command execution
     * @type String
     */
    this.execCommand = function(cmd) {
        if (!this.isConnected()) {
            error("execCommand", "Not connected. Please establish a connection first.");
        } else {
            var session = connection.openSession();
            try {
                session.execCommand(cmd);
                var stdout = new SSHPKG.StreamGobbler(session.getStdout());
                var br = new java.io.BufferedReader(new java.io.InputStreamReader(stdout));
                res.push();
    			while (true) {
    				if (!(line = br.readLine()))
    					break;
    				res.writeln(line);
    			}
                debug("execCommand", "executed command '" + cmd + "'");
                return res.pop();
            } catch (e) {
                error("execCommand", e);
            } finally {
                session.close();
            }
        }
    };
    
    /**
     * Toggles paranoid mode. If set to true this client tries to
     * verify the host key against the its list of known hosts
     * during connection and rejects if the host key is not found
     * therein or is different.
     * @param {Boolean} p Either true or false
     */
    this.setParanoid = function(p) {
        paranoid = (p === true);
        return;
    };

    /**
     * Returns true if this client is in paranoid mode.
     * @return Boolean
     * @see #setParanoid
     */
    this.isParanoid = function() {
        return paranoid;
    };
    
    /**
     * main constructor body
     */
    if (hosts) {
        this.addKnownHosts(hosts);
    }

    for (var i in this)
        this.dontEnum(i);
    return this;
};


/** @ignore */
helma.Ssh.toString = function() {
    return "[helma.Ssh]";
};


helma.lib = "Ssh";
helma.dontEnum(helma.lib);
for (var i in helma[helma.lib])
    helma[helma.lib].dontEnum(i);
for (var i in helma[helma.lib].prototype)
    helma[helma.lib].prototype.dontEnum(i);
delete helma.lib;
