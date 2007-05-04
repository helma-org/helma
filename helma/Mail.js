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
 * $RCSfile: Mail.js,v $
 * $Author: tobi $
 * $Revision: 1.7 $
 * $Date: 2007/03/15 09:54:56 $
 */

/**
 * @fileoverview Fields and methods of the helma.Mail class.
 */

// take care of any dependencies
app.addRepository('modules/helma/File.js');

/**
 * Define the global namespace if not existing
 */
if (!global.helma) {
    global.helma = {};
}

/**
 * Mail client enabling you to send e-mail via SMTP using Packages.javax.mail.
 * <br /><br />
 * @class This class provides functionality to sending
 * Email messages. 
 * A mail client object is created by using the helma.Mail() 
 * constructor. The mail object then can be manipulated and sent 
 * using the methods listed below.
 * <br /><br />
 * You will either need to set your mail server via the smtp
 * property in the app.properties or server.properties file
 * or pass the hostname of the mail server you want to use as a 
 * parameter to the constructor.
 * <br /><br />
 * Note: Make sure that the SMTP server itself is well-configured, 
 * so that it accepts e-mails coming from your server and does 
 * not deny relaying. Best and fastest configuration is of course 
 * if you run your own SMTP server (e.g. postfix) which might be 
 * a bit tricky to set up, however.</p>
 * 
 * @param {String} smtp as String, the hostname of the mail server
 * @constructor
 */
helma.Mail = function(host, port) {
    // Error code values for this.status
    var OK          =  0;
    var SUBJECT     = 10;
    var TEXT        = 11;
    var MIMEPART    = 12;
    var TO          = 20;
    var CC          = 21;
    var BCC         = 22;
    var FROM        = 23;
    var REPLYTO     = 24;
    var SEND        = 30;
    var MAILPKG     = Packages.javax.mail;

    var self = this;
    var errStr = "Error in helma.Mail";

    var System              = java.lang.System;
    var Properties          = java.util.Properties;
    var IOException         = java.io.IOException;
    var Wrapper             = Packages.org.mozilla.javascript.Wrapper;
    var FileDataSource      = Packages.javax.activation.FileDataSource;
    var DataHandler         = Packages.javax.activation.DataHandler;

    var MimePart            = Packages.helma.util.MimePart;
    var MimePartDataSource  = Packages.helma.util.MimePartDataSource;

    var Address             = MAILPKG.Address;
    var BodyPart            = MAILPKG.BodyPart;
    var Message             = MAILPKG.Message;
    var Multipart           = MAILPKG.Multipart;
    var Session             = MAILPKG.Session;
    var Transport           = MAILPKG.Transport;
    var InternetAddress     = MAILPKG.internet.InternetAddress;
    var AddressException    = MAILPKG.internet.AddressException;
    var MimeBodyPart        = MAILPKG.internet.MimeBodyPart;
    var MimeMessage         = MAILPKG.internet.MimeMessage;
    var MimeUtility         = MAILPKG.internet.MimeUtility;
    var MimeMultipart       = MAILPKG.internet.MimeMultipart;

    var buffer, multipart, multipartType = "mixed";
    var username, password;

    var setStatus = function(status) {
        if (self.status === OK) {
            self.status = status;
        }
        return;
    };

    var getStatus = function() {
        return self.status;
    };

    var addRecipient = function(addstr, name, type) {
        if (addstr.indexOf("@") < 0) {
            throw new AddressException();
        }
        if (name != null) {
            var address = new InternetAddress(addstr, 
                              MimeUtility.encodeWord(name.toString()));
        } else {
            var address = new InternetAddress(addstr);
        }
        message.addRecipient(type, address);
        return;
    };

    /**
     * Adds the content stored in this helma.Mail instance
     * to the wrapped message.
     * @private
     */
    var setContent = function() {
        if (buffer != null) {
            if (multipart != null) {
                var part = new MimeBodyPart();
                part.setContent(buffer.toString(), "text/plain");
                multipart.addBodyPart(part, 0);
                message.setContent(multipart);
            } else {
                message.setText(buffer.toString());
            }
        } else if (multipart != null) {
            message.setContent(multipart);
        } else {
            message.setText("");
        }
        return;
    };

    /**
     * Sets username and password to use for SMTP authentication.
     * @param {String} uname The username to use
     * @param {String} pwd The password to use
     */
    this.setAuthentication = function(uname, pwd) {
        if (username && password) {
            username = uname;
            password = pwd;
            // enable smtp authentication
            props.put("mail.smtp.auth", "true");
        }
        return;
    }

    /**
     * Returns the wrapped message
     * @returns The wrapped message
     * @type javax.mail.internet.MimeMessage
     */
    this.getMessage = function() {
        return message;
    };

    /**
     * Switches debug mode on or off.
     * @param {Boolean} debug If true debug mode is enabled
     */
    this.setDebug = function(debug) {
        session.setDebug(debug === true);
    };

    /**
     * The status of this Mail object. This equals <code>0</code> unless
     * an error occurred. See  {@link helma.Mail Mail.js} source code for a list of
     * possible error codes.
     */
    this.status = OK;
    
    /**
     * Sets the sender of an e-mail message.
     * <br /><br />
     * The first argument specifies the receipient's 
     * e-mail address. The optional second argument 
     * specifies the name of the recipient.
     *
     * @param {String} addstr as String, sender email address
     * @param {String} name as String, optional sender name
     */
    this.setFrom = function(addstr, name) {
        try {
            if (addstr.indexOf("@") < 0) {
                throw new AddressException();
            }
            if (name != null) {
                var address = new InternetAddress(addstr, 
                                  MimeUtility.encodeWord(name.toString()));
            } else {
                var address = new InternetAddress(addstr);
            }
            message.setFrom(address);
        } catch (mx) {
            app.logger.error(errStr + ".setFrom(): " + mx);
            setStatus(FROM);
        }
        return;
    };
    
    /**
     * Sets the Reply-To address of an e-mail message.
     *
     * @param {String} addstr as String, the reply-to email address
     */
    this.setReplyTo = function(addstr) {
        try {
            if (addstr.indexOf("@") < 0) {
                throw new AddressException();
            }
            var address = [new InternetAddress(addstr)];
            message.setReplyTo(address);
        } catch (mx) {
            app.logger.error(errStr + ".setReplyTo(): " + mx);
            setStatus(REPLYTO);
        }
        return;
    }

    /**
     * Sets the recipient of an e-mail message.
     * &nbsp;
     * The first argument specifies the receipient's 
     * e-mail address. The optional second argument 
     * specifies the name of the recipient.
     *
     * @param {String} addstr as String, receipients email address
     * @param {String} name as String, optional receipients name
     * @see #addTo
     */
    this.setTo = function(addstr, name) {
        try {
            addRecipient(addstr, name, Message.RecipientType.TO);
        } catch (mx) {
            app.logger.error(errStr + ".setTo(): " + mx);
            setStatus(TO);
        }
        return;
    };

    /**
     * Adds a recipient to the address list of an e-mail message.
     * <br /><br />
     * The first argument specifies the receipient's 
     * e-mail address. The optional second argument 
     * specifies the name of the recipient.
     *
     * @param {String} addstr as String, receipients email address
     * @param {String} name as String, optional receipients name
     * @see setTo
     */
    this.addTo = function(addstr, name) {
        try {
            addRecipient(addstr, name, Message.RecipientType.TO);
        } catch (mx) {
            app.logger.error(errStr + ".addTo(): " + mx);
            setStatus(TO);
        }
        return;
    }

    /**
     * Adds a recipient to the list of addresses to get a "carbon copy"
     * of an e-mail message.
     * <br /><br />
     * The first argument specifies the receipient's 
     * e-mail address. The optional second argument 
     * specifies the name of the recipient.
     *
     * @param {String} addstr as String, receipients email address
     * @param {String} name as String, optional receipients name
     */
    this.addCC = function(addstr, name) {
        try {
            addRecipient(addstr, name, Message.RecipientType.CC);
        } catch (mx) {
            app.logger.error(errStr + ".addCC(): " + mx);
            setStatus(CC);
        }
        return;
    }

    /**
     * Adds a recipient to the list of addresses to get a "blind carbon copy" of an e-mail message.
     * <br /><br />
     * The first argument specifies the receipient's 
     * e-mail address. The optional second argument 
     * specifies the name of the recipient.
     *
     * @param {String} addstr as String, receipients email address
     * @param {String} name as String, optional receipients name
     */
    this.addBCC = function(addstr, name) {
        try {
            addRecipient(addstr, name, Message.RecipientType.BCC);
        } catch (mx) {
            app.logger.error(errStr + ".addBCC(): " + mx);
            setStatus(BCC);
        }
        return;
    }

    /**
     * Sets the subject of an e-mail message.
     *
     * @param {String} subject as String, the email subject
     */
    this.setSubject = function(subject) {
        if (!subject) {
            return;
        }
        try {
            message.setSubject(MimeUtility.encodeWord(subject.toString()));
        } catch (mx) {
            app.logger.error(errStr + ".setSubject(): " + mx);
            setStatus(SUBJECT);
        }
        return;
    };

    /**
     * Sets the body text of an e-mail message.
     * 
     * @param {String} text as String, to be appended to the message body
     * @see #addText
     */
    this.setText = function(text) {
        if (text) {
            buffer = new java.lang.StringBuffer(text);
        }
        return;
    };

    /**
     * Appends a string to the body text of an e-mail message.
     *
     * @param {String} text as String, to be appended to the message body
     * @see #setText
     */
    this.addText = function(text) {
        if (buffer == null) {
            buffer = new java.lang.StringBuffer(text);
        } else {
            buffer.append(text);
        }
        return;
    };

    /**
     * Sets the MIME multiparte message subtype. The default value is
     * "mixed" for messages of type multipart/mixed. A common value
     * is "alternative" for the multipart/alternative MIME type.
     * @param {String} messageType the MIME subtype such as "mixed" or "alternative".
     * @see #setMultipartType
     * @see #addPart
     */
    this.setMultipartType = function(messageType) {
        multipartType = messageType;
        return;
    };

    /**
     * Returns the MIME multiparte message subtype. The default value is
     * "mixed" for messages of type multipart/mixed.
     * @return the MIME subtype
     * @type String
     * @see #getMultipartType
     * @see #addPart
     */
    this.getMultipartType = function(messageType) {
        return multipartType;
    };

    /**
     * Adds an attachment to an e-mail message.
     * <br /><br />
     * The attachment needs to be either a helma.util.MimePart Object retrieved
     * through the global getURL function, or a {@link helma.File} object, or a String.
     * <br /><br />
     * Use the getURL() function to retrieve a MIME object or wrap a
     * helma.File object around a file of the local file system.
     *
     * @param {fileOrMimeObjectOrString} obj File, Mime object or String to attach to the email
     * @param {String} filename optional name of the attachment
     * @param {String} contentType optional content type (only if first argument is a string)
     * @see global.getUrl
     * @see helma.util.MimePart
     * @see helma.File
     */
    this.addPart = function(obj, filename, contentType) {
        try {
            if (obj == null) {
                throw new IOException(
                    errStr + ".addPart: method called with wrong number of arguments."
                );
            }
            if (multipart == null) {
                multipart = new MimeMultipart(multipartType);
            }
            if (obj instanceof Wrapper) {
                obj = obj.unwrap();
            }

            var part;
            if (obj instanceof BodyPart) {
                // we already got a body part, no need to convert it
                part = obj;
            } else {
                part = new MimeBodyPart();
                if (typeof obj == "string") {
                    part.setContent(obj.toString(), contentType || "text/plain");
                } else if (obj instanceof File || obj instanceof helma.File) {
                    var source = new FileDataSource(obj.getPath());
                    part.setDataHandler(new DataHandler(source));
                } else if (obj instanceof MimePart) {
                    var source = new MimePartDataSource(obj);
                    part.setDataHandler(new DataHandler(source));
                }
            }
            if (filename != null) {
                try {
                    part.setFileName(filename.toString());
                } catch (x) {}
            } else if (obj instanceof File || obj instanceof helma.File) {
                try {
                    part.setFileName(obj.getName());
                } catch (x) {}
            }
            multipart.addBodyPart(part);
        } catch (mx) {
            app.logger.error(errStr + ".addPart(): " + mx);
            setStatus(MIMEPART);
        }
        return;
    };

    /**
     * Saves this mail RFC 822 formatted into a file. The name of the
     * file is prefixed with "helma.Mail" followed by the current time
     * in milliseconds and a random number.
     * @param {helma.File} dir An optional directory where to save
     * this mail to. If omitted the mail will be saved in the system's
     * temp directory.
     */
    this.writeToFile = function(dir) {
        if (!dir || !dir.exists() || !dir.canWrite()) {
            dir = new java.io.File(System.getProperty("java.io.tmpdir"));
        }
        var fileName = "helma.Mail." + (new Date()).getTime() +
                       "." + Math.round(Math.random() * 1000000);
        var file = new java.io.File(dir, fileName);
        if (file.exists()) {
            file["delete"]();
        }
        try {
            setContent();
            var fos = new java.io.FileOutputStream(file);
            var os = new java.io.BufferedOutputStream(fos);
            message.writeTo(os);
            os.close();
            app.logger.info("helma.Mail.saveTo(): saved mail to " +
                            file.getAbsolutePath());
        } catch (e) {
            app.logger.error(errStr + ".saveTo(): " + e);
        }
        return;
    };

    /**
     * Returns the source of this mail as RFC 822 formatted string.
     * @returns The source of this mail as RFC 822 formatted string
     * @type String
     */
    this.getSource = function() {
        try {
            setContent();
            var buf = new java.io.ByteArrayOutputStream();
            var os = new java.io.BufferedOutputStream(buf);
            message.writeTo(os);
            os.close();
            return buf.toString();
        } catch (e) {
            app.logger.error(errStr + ".getSource(): " + e);
        }
        return null;
    };

    /**
     * Sends an e-mail message.
     * <br /><br />
     * This function sends the message using the SMTP
     * server as specified when the Mail object was
     * constructed using helma.Mail.
     * <br /><br />
     * If no smtp hostname was specified when the Mail
     * object was constructed, the smtp property in either
     * the app.properties or server.properties file needs
     * to be set in order for this to work.
     * <br /><br />
     * As a fallback, the message will then be written to
     * file using the {@link #writeToFile} method. 
     * Additionally, the location of the message files can
     * be determined by setting smtp.dir in app.properties
     * to the desired file path.
     */
    this.send = function() {
        if (this.status > OK) {
            app.logger.error(errStr + ".send(): Status is " + this.status);
            return;
        }
        if (host != null) {
            try {
                setContent();
                message.setSentDate(new Date());
                var transport = session.getTransport("smtp");
                if (username && password) {
                    transport.connect(host, username, password);
                } else {
                    transport.connect();
                }
                message.saveChanges();
                transport.sendMessage(message, message.getAllRecipients());
            } catch (mx) {
                app.logger.error(errStr + ".send(): " + mx);
                setStatus(SEND);
            } finally {
                if (transport != null && transport.isConnected()) {
                    transport.close();
                }
            }
        } else {
            // no smtp host is given, so write the mail to a file
            this.writeToFile(getProperty("smtp.dir"));
        }
        return;
    };

    /**
     * Main constructor body
     */
    var props = new Properties();
    if (host || (host = getProperty("smtp"))) {
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.host", String(host));
        props.put("mail.smtp.port", String(port || 25));
        props.put("mail.mime.charset",
                  System.getProperty("mail.charset", "ISO-8859-15"));
    }

    this.setAuthentication(getProperty("smtp.username"),
                           getProperty("smtp.password"));
    var session = Session.getInstance(props);
    var message = new MimeMessage(session);

    for (var i in this)
        this.dontEnum(i);

    return this;
}


/** @ignore */
helma.Mail.toString = function() {
    return "[helma.Mail]";
};


/** @ignore */
helma.Mail.prototype.toString = function() {
    return "[helma.Mail Object]";
};

helma.Mail.example = function(host, sender, addr, subject, text) {
    // var smtp = "smtp.host.dom";
    // var sender = "sender@host.dom";
    // var addr = "recipient@host.dom";
    // var subject = "Hello, World!";
    // var text = "This is a test.";
    var port = 25;
    var msg = new helma.Mail(host, port);
    msg.setFrom(sender);
    msg.addTo(addr);
    msg.setSubject(subject);
    msg.setText(text);
    msg.send();
    res.write(msg.status);
    return;
};


helma.lib = "Mail";
helma.dontEnum(helma.lib);
for (var i in helma[helma.lib])
    helma[helma.lib].dontEnum(i);
for (var i in helma[helma.lib].prototype)
    helma[helma.lib].prototype.dontEnum(i);
delete helma.lib;
