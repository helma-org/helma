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
 * $RCSfile: helma.Ssh.js,v $
 * $Author: czv $
 * $Revision: 1.4 $
 * $Date: 2006/04/18 13:06:58 $
 */


// take care of any dependencies
app.addRepository('modules/helma/File.js');
app.addRepository('modules/helma/ganymed-ssh2.jar');


if (!global.helma) {
    global.helma = {};
}

/** 
 * constructor
 */
helma.Ssh = function(server, hosts) {
    var SSHPKG = Packages.ch.ethz.ssh2;
    var SSHPKGNAME = "ganymed-ssh2.jar";
    var SSHPKGURL = "http://www.ganymed.ethz.ch/ssh2";
    var className = "helma.Ssh";
    var paranoid = false;
    var verifier = null;
    var knownHosts, connection;

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
     * private method for creating an instance of
     * java.io.File based on various argument types
     * @param a String or an instance of helma.File or java.io.File
     */
    var getFile = function(arg) {
        if (arg instanceof helma.File) {
            return new java.io.File(arg.getAbsolutePath());
        } else if (arg instanceof java.io.File) {
            return arg;
        } else if (arg.constructor == String) {
            return new java.io.File(arg);
        }
        return null;
    };

    /**
     * private method that connects to the remote server
     * @return Boolean
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
     * debug output method
     * @param String name of method
     * @param String debug message
     */
    var debug = function(methodName, msg) {
        var msg = msg ? " " + msg : "";
        app.debug(className + ":" + methodName + msg);
        return;
    };

    /**
     * error output method
     * @param String name of method
     * @param String error message
     */
    var error = function(methodName, msg) {
        var tx = java.lang.Thread.currentThread();
        tx.dumpStack();
        app.log("Error in " + className + ":" + methodName + ": " + msg);
        return;
    };

    /**
     * reads a known hosts file
     * @param Object String or instance of helma.File or java.io.File
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
     * connects to a remote host with username/password authentication
     * @param String username
     * @param String password
     * @return Boolean
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
     * connects to a remote host using private key and passphrase
     * @param String username
     * @param String path to keyfile
     * @param String passphrase (if needed by key)
     * @return Boolean
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
     * disconnect a session
     */
    this.disconnect = function() {
        connection.close();
        debug("disconnect", "disconnected from server " + server);
        return;
    };

    /**
     * returns true if the ssh client is connected
     * @return Boolean
     */
    this.isConnected = function() {
        return (connection != null && connection.isAuthenticationComplete());
    };

    /**
     * copies a file to the remote server
     * @param String path to local file that should be copied
     *                    to remote server. If the argument is a
     *                    String Array, all files specified in the
     *                    Array are copied to the server
     * @param String path to remote destination directory
     * @param String (optional) 4-digit permission mode string (eg. "0755");
     * @return Boolean
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
     * copies a file from the remote server.
     * @param String path to remote file that should be copied
     *                    to remote server. If the argument is a
     *                    String Array, all files specified in the
     *                    Array are copied from the remote server
     * @param String path to local destination directory
     * @return Boolean
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
     * executes a command on the remote server
     * (scp must be in PATH on the remote server)
     * @param String the command to execute
     * @return String result of the command
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
     * toggle paranoia mode
     * @param Boolean
     */
    this.setParanoid = function(p) {
        paranoid = (p == true) ? true : false;
        return;
    };

    /**
     * returns true if in paranoid mode
     * @return Boolean
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
