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
 * $Author: hannes $
 * $Revision: 1.4 $
 * $Date: 2006/11/27 12:47:04 $
 */


/**
 * Define the global namespace if not existing
 */
if (!global.helma) {
    global.helma = {};
}

/**
 * Mail client enabling you to send e-mail via SMTP using Packages.javax.mail.
 * <br /><br />
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
helma.Mail = function(smtp) {
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

    var Address             = MAILPKG.Address;
    var BodyPart            = MAILPKG.BodyPart;
    var Message             = MAILPKG.Message;
    var Multipart           = MAILPKG.Multipart;
    var Session             = MAILPKG.Session;
    var Transport           = MAILPKG.Transport;
    var InternetAddress     = MAILPKG.internet.InternetAddress;
    var AddressException    = MAILPKG.internet.AddressException;
    var MimePart            = MAILPKG.internet.MimePart;
    var MimeBodyPart        = MAILPKG.internet.MimeBodyPart;
    var MimeMessage         = MAILPKG.internet.MimeMessage;
    var MimeUtility         = MAILPKG.internet.MimeUtility;
    var MimeMultipart       = MAILPKG.internet.MimeMultipart;
    var MimePartDataSource  = MAILPKG.internet.MimePartDataSource;

    var buffer, multipart;

    var props = new Properties();
    System.setProperty(
        "mail.mime.charset",
        System.getProperty("mail.charset", "ISO-8859-15")
    );

    var host = smtp || getProperty("smtp");
    if (host != null) {
        props.put("mail.smtp.host", host);
    }

    var session = Session.getInstance(props);
    var message = new MimeMessage(session);

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
     * The status of this Mail object.
     */
    this.status = OK;
    
    /**
     * Sets the sender of an e-mail message.
     * <br /><br />
     * The first argument specifies the receipient's 
     * e-mail address. The optional second argument 
     * specifies the name of the recipient.
     * <br /><br />
     * Example:
     * <pre>var mail = new helma.Mail();
     * mail.setFrom("tobi@helma.at", "Tobi Schaefer");</pre>
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
            res.debug(errStr + ".setFrom(): " + mx);
            setStatus(FROM);
        }
        return;
    };
    
    /**
     * Sets the Reply-To address of an e-mail message.
     * <br /><br />
     * Example:
     * <pre>var mail = new helma.Mail();
     * mail.setReplyTo("tobi@helma.at");</pre>
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
            res.debug(errStr + ".setReplyTo(): " + mx);
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
     * <br /><br />
     * Example:
     * <pre>var mail = new helma.Mail();
     * mail.setTo("hop@helma.at");</pre>
     * 
     * @param {String} addstr as String, receipients email address
     * @param {String} name as String, optional receipients name
     * @see #addTo
     */
    this.setTo = function(addstr, name) {
        try {
            addRecipient(addstr, name, Message.RecipientType.TO);
        } catch (mx) {
            res.debug(errStr + ".setTo(): " + mx);
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
     * <br /><br />
     * Example:
     * <pre>var mail = new helma.Mail();
     * mail.setTo("hop@helma.at");
     * mail.addTo("hopdoc@helma.at");
     * mail.addTo("tobi@helma.at", "Tobi Schaefer");</pre>
     * 
     * @param {String} addstr as String, receipients email address
     * @param {String} name as String, optional receipients name
     * @see setTo
     */
    this.addTo = function(addstr, name) {
        try {
            addRecipient(addstr, name, Message.RecipientType.TO);
        } catch (mx) {
            res.debug(errStr + ".addTo(): " + mx);
            setStatus(TO);
        }
        return;
    }

    this.addCC = function(addstr, name) {
        try {
            addRecipient(addstr, name, Message.RecipientType.CC);
        } catch (mx) {
            res.debug(errStr + ".addCC(): " + mx);
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
     * <br /><br />
     * Example:
     * <pre>var mail = new helma.Mail();
     * mail.addBCC("hop@helma.at");
     * mail.addBCC("tobi@helma.at", "Tobi Schaefer");</pre>
     * 
     * @param {String} addstr as String, receipients email address
     * @param {String} name as String, optional receipients name
     */
    this.addBCC = function(addstr, name) {
        try {
            addRecipient(addstr, name, Message.RecipientType.BCC);
        } catch (mx) {
            res.debug(errStr + ".addBCC(): " + mx);
            setStatus(BCC);
        }
        return;
    }

    /**
     * Sets the subject of an e-mail message.
     * <br /><br />
     * Example:
     * <pre>var mail = new helma.Mail();
     * mail.setSubject("Hello, World!");</pre>
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
            res.debug(errStr + ".setSubject(): " + mx);
            setStatus(SUBJECT);
        }
        return;
    };

    /**
     * Sets the body text of an e-mail message.
     * <br /><br />
     * Example:
     * <pre>var mail = new helma.Mail();
     * mail.setText("Hello, World!");</pre>
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
     * <br /><br />
     * Example:
     * <pre>var mail = new Mail();
     * mail.addText("Hello, World!");</pre>
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
     * Adds an attachment to an e-mail message.
     * <br /><br />
     * The attachment needs to be either a MIME Object or a java.io.file object.
     * <br /><br />
     * Use the getURL() function to retrieve a MIME object or wrap a 
     * java.io.File object around a file of the local file system.
     * <br /><br />
     * Example:
     * <pre>var file1 = getURL("http://localhost:8080/static/image.gif");
     * var file2 = getURL("file:////home/snoopy/woodstock.jpg");
     * var file3 = new java.io.File("/home/snoopy/woodstock.jpg");
     * var mail = new Mail();
     * mail.addPart(file1);
     * mail.addPart(file2);
     * mail.addPart(file3);
     * &nbsp;
     * mail.setFrom("snoopy@doghouse.com");
     * mail.setTo("woodstock@birdcage.com");
     * mail.setSubject("Look at this!");
     * mail.addText("I took a photograph from you. Neat, isn't it? -Snoop");
     * mail.send();</pre>
     * 
     * @param {fileOrMimeObject} File or Mime object to attach to the email
     * @param {String} nameString as String, optional name of the attachment
     * @see global.getUrl
     * @see mimePart
     * @see java.io.File
     */
    this.addPart = function(obj, filename) {
        try {
            if (obj == null) {
                throw new IOException(
                    errStr + ".addPart: method called with wrong number of arguments."
                );
            }
            if (multipart == null) {
                multipart = new MimeMultipart();
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
                    part.setContent(obj.toString(), "text/plain");
                } else if (obj instanceof File) {
                    // FIXME: the following line did not work under windows:
                    //var source = new FileDataSource(obj);
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
            } else if (obj instanceof File) {
                try {
                    part.setFileName(obj.getName());
                } catch (x) {}
            }
            multipart.addBodyPart(part);
        } catch (mx) {
            res.debug(errStr + ".addPart(): " + mx);
            setStatus(MIMEPART);
        }
        return;
    }

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
     * Example:
     * <pre>var mail = new helma.Mail('smtp.example.com');
     * mail.setTo("watching@michi.tv", "michi");
     * mail.addCC("franzi@home.at", "franzi");
     * mail.addBCC("monie@home.at");
     * mail.setFrom("chef@frischfleisch.at", "Hannes");
     * mail.setSubject("Registration Conformation");
     * mail.addText("Thanks for your Registration...");
     * mail.send();</pre>
     */
    this.send = function() {
        if (this.status > OK) {
            res.debug(errStr + ".send(): Status is " + this.status);
            return;
        }
        try {
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
            message.setSentDate(new Date());
            Transport.send(message);
        } catch (mx) {
            res.debug(errStr + ".send(): " + mx);
            setStatus(SEND);
        }
        return;
    };

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


helma.Mail.example = function(smtp, sender, addr, subject, text) {
    // var smtp = "smtp.host.dom";
    // var sender = "sender@host.dom";
    // var addr = "recipient@host.dom";
    // var subject = "Hello, World!";
    // var text = "This is a test.";
    var msg = new helma.Mail(smtp);
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
