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
 * $RCSfile: Mail.js,v $
 * $Author: czv $
 * $Revision: 1.2 $
 * $Date: 2006/04/24 07:02:17 $
 */


if (!global.helma) {
    global.helma = {};
}

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

    this.status = OK;
 
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

    this.setTo = function(addstr, name) {
        try {
            addRecipient(addstr, name, Message.RecipientType.TO);
        } catch (mx) {
            res.debug(errStr + ".setTo(): " + mx);
            setStatus(TO);
        }
        return;
    };

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

    this.addBCC = function(addstr, name) {
        try {
            addRecipient(addstr, name, Message.RecipientType.BCC);
        } catch (mx) {
            res.debug(errStr + ".addBCC(): " + mx);
            setStatus(BCC);
        }
        return;
    }

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

    this.setText = function(text) {
        if (text) {
            buffer = new java.lang.StringBuffer(text);
        }
        return;
    };

    this.addText = function(text) {
        if (buffer == null) {
            buffer = new java.lang.StringBuffer(text);
        } else {
            buffer.append(text);
        }
        return;
    };

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

            var part = new MimeBodyPart();
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


helma.Mail.toString = function() {
    return "[helma.Mail]";
};


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
