/*
 * Helma License Notice
 *
 * The contents of this file are subject to the Helma License
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://adele.helma.org/download/helma/license.txt
 *
 * Copyright 1998-2003 Helma Software. All Rights Reserved.
 *
 * $RCSfile$
 * $Author$
 * $Revision$
 * $Date$
 */

package helma.scripting.rhino.extensions;

import helma.util.*;
import org.mozilla.javascript.*;
import java.io.*;
import java.util.*;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import javax.activation.*;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;

/**
 * A JavaScript wrapper around a JavaMail message class to send
 * mail via SMTP from Helma
 */
public class MailObject extends ScriptableObject implements Serializable {

    public static final int OK = 0;
    public static final int SUBJECT = 10;
    public static final int TEXT = 11;
    public static final int MIMEPART = 12;
    public static final int TO = 20;
    public static final int CC = 21;
    public static final int BCC = 22;
    public static final int FROM = 23;
    public static final int REPLYTO = 24;
    public static final int SEND = 30;

    MimeMessage message;
    Multipart multipart;
    StringBuffer buffer;
    int status;

    /**
     * Creates a new MailObject prototype object.
     */
    MailObject() {
    }


    /**
     * Creates a new ESMail object.
     *
     * @param mailx ...
     */
    MailObject(Properties mprops) {
        this.status = OK;

        // create some properties and get the default Session
        Properties props = new Properties();

        props.put("mail.smtp.host", mprops.getProperty("smtp", "mail"));

        Session session = Session.getDefaultInstance(props, null);

        message = new MimeMessage(session);
    }

    /**
     *  Overrides abstract method in ScriptableObject
     */
    public String getClassName() {
        return "Mail";
    }

    public static MailObject mailObjCtor(Context cx, Object[] args,
                Function ctorObj, boolean inNewExpr) {
        Properties props = (Properties) ctorObj.get("props", ctorObj);
        if (props == null) {
            props = new Properties();
        }
        return new MailObject(props);
    }

    public static void init(Scriptable scope, Properties props) {
        Method[] methods = MailObject.class.getDeclaredMethods();
        ScriptableObject proto = new MailObject();
        proto.setPrototype(getObjectPrototype(scope));
        Member ctorMember = null;
        for (int i=0; i<methods.length; i++) {
            if ("mailObjCtor".equals(methods[i].getName())) {
                ctorMember = methods[i];
                break;
            }
        }
        FunctionObject ctor = new FunctionObject("Mail", ctorMember, scope);
        ctor.addAsConstructor(scope, proto);
        ctor.put("props", ctor, props);
        String[] mailFuncs = {
                "addBCC", "addCC", "addPart", "addText", "addTo",
                "send", "setFrom", "setSubject", "setTo"
                            };
        try {
            proto.defineFunctionProperties(mailFuncs, MailObject.class, 0);
            proto.defineProperty("status", MailObject.class, 0);
        } catch (Exception ignore) {
            System.err.println ("Error defining function properties: "+ignore);
        }
    }


    /**
     *
     *
     * @param status ...
     */
    public void setStatus(int status) {
        // Only register the first error that occurrs
        if (this.status == 0) {
            this.status = status;
        }
    }

    /**
     *
     *
     * @return ...
     */
    public int getStatus() {
        return status;
    }


    /**
     *
     */
    public void addText(String text) {
        if (text != null) {
            if (buffer == null) {
                buffer = new StringBuffer();
            }
            buffer.append(text);
        }
    }

    /**
     *
     *
     * @param val ...
     *
     * @throws Exception ...
     * @throws IOException ...
     */
    public void addPart(Object obj, Object filename) throws Exception {
        if (obj == null || obj == Undefined.instance) {
            throw new IOException("mail.addPart called with wrong number of arguments.");
        }

        if (multipart == null) {
            multipart = new MimeMultipart();
        }

        MimeBodyPart part = new MimeBodyPart();

        if (obj instanceof String) {
            part.setContent(obj.toString(), "text/plain");
        } else if (obj instanceof File) {
            FileDataSource source = new FileDataSource((File) obj);

            part.setDataHandler(new DataHandler(source));
        } else if (obj instanceof MimePart) {
            MimePartDataSource source = new MimePartDataSource((MimePart) obj);

            part.setDataHandler(new DataHandler(source));
        }

        // check if an explicit file name was given for this part
        if (filename != null && filename != Undefined.instance) {
            try {
                part.setFileName(filename.toString());
            } catch (Exception x) {
                // FIXME: error setting file name ... should we ignore this or throw an exception?
            }
        }

        multipart.addBodyPart(part);
    }

    /**
     *
     *
     * @param val ...
     *
     * @throws Exception ...
     */
    public void setSubject(Object subject) throws Exception {
        if (subject == null || subject == Undefined.instance) {
            return;
        }

        message.setSubject(MimeUtility.encodeWord(subject.toString()));
    }

    /**
     *
     *
     * @param add ...
     *
     * @throws Exception ...
     * @throws AddressException ...
     */
    public void setReplyTo(String addstr) throws Exception {
        if (addstr.indexOf("@") < 0) {
            throw new AddressException();
        }

        Address[] replyTo = new Address[1];

        replyTo[0] = new InternetAddress(addstr);
        message.setReplyTo(replyTo);
    }

    /**
     *
     *
     * @param add ...
     *
     * @throws Exception ...
     * @throws AddressException ...
     */
    public void setFrom(String addstr, Object name) throws Exception {
        if (addstr.indexOf("@") < 0) {
            throw new AddressException();
        }

        Address address = null;

        if (name != null && name != Undefined.instance) {
            address = new InternetAddress(addstr,
                                          MimeUtility.encodeWord(name.toString()));
        } else {
            address = new InternetAddress(addstr);
        }

        message.setFrom(address);
    }


    /**
     *
     *
     * @param add ...
     *
     * @throws Exception ...
     * @throws AddressException ...
     */
    public void setTo(String addstr, Object name) throws Exception {
        addRecipient(addstr, name, Message.RecipientType.TO);
    }


    /**
     *
     *
     * @param add ...
     *
     * @throws Exception ...
     * @throws AddressException ...
     */
    public void addTo(String addstr, Object name) throws Exception {
        addRecipient(addstr, name, Message.RecipientType.TO);
    }

    /**
     *
     *
     * @param add ...
     *
     * @throws Exception ...
     * @throws AddressException ...
     */
    public void addCC(String addstr, Object name) throws Exception {
        addRecipient(addstr, name, Message.RecipientType.CC);
    }

    /**
     *
     *
     * @param add ...
     *
     * @throws Exception ...
     * @throws AddressException ...
     */
    public void addBCC(String addstr, Object name) throws Exception {
        addRecipient(addstr, name, Message.RecipientType.BCC);
    }

    /**
     *
     *
     * @param add ...
     *
     * @throws Exception ...
     * @throws AddressException ...
     */
    private void addRecipient(String addstr,
                              Object name,
                              Message.RecipientType type) throws Exception {
        if (addstr.indexOf("@") < 0) {
            throw new AddressException();
        }

        Address address = null;

        if (name != null && name != Undefined.instance) {
            address = new InternetAddress(addstr,
                                          MimeUtility.encodeWord(name.toString()));
        } else {
            address = new InternetAddress(addstr);
        }

        message.addRecipient(type, address);
    }


    /**
     *
     *
     * @throws Exception ...
     */
    public void send() throws Exception {
        if (buffer != null) {
            // if we also have a multipart body, add
            // plain string as first part to it.
            if (multipart != null) {
                MimeBodyPart part = new MimeBodyPart();

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

        Transport.send(message);
    }
}
