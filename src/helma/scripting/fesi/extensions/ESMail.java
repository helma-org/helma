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

package helma.scripting.fesi.extensions;

import FESI.Data.*;
import FESI.Exceptions.*;
import FESI.Interpreter.*;
import helma.util.*;
import java.io.*;
import java.util.*;
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
public class ESMail extends ESObject implements Serializable {
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
    MailExtension mailx;
    Properties mprops;
    MimeMessage message;
    Multipart multipart;
    StringBuffer buffer;
    int status;

    /**
     * Creates a new ESMail object.
     *
     * @param mailx ...
     */
    public ESMail(MailExtension mailx) {
        super(mailx.esMailPrototype, mailx.eval);
        this.status = OK;
        this.mailx = mailx;
        this.mprops = mailx.mprops;

        // create some properties and get the default Session
        try {
            Properties props = new Properties();

            props.put("mail.smtp.host", mprops.getProperty("smtp", "mail"));

            Session session = Session.getDefaultInstance(props, null);

            message = new MimeMessage(session);
        } catch (Throwable t) {
            this.evaluator.engine.getApplication().logEvent("Error in mail constructor: " +
                                                            t);
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
     *
     * @param propertyName ...
     * @param hash ...
     *
     * @return ...
     *
     * @throws EcmaScriptException ...
     */
    public ESValue getProperty(String propertyName, int hash)
                        throws EcmaScriptException {
        if ("status".equalsIgnoreCase(propertyName)) {
            return new ESNumber(status);
        }

        return super.getProperty(propertyName, hash);
    }

    /**
     *
     */
    public void setText(ESValue val) throws Exception {
        if (buffer == null) {
            buffer = new StringBuffer();
        }

        if (val != null) {
            buffer.append(val.toString());
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
    public void addPart(ESValue[] val) throws Exception {
        if ((val == null) || (val.length == 0) || (val.length > 2)) {
            throw new IOException("mail.addPart called with wrong number of arguments.");
        }

        if (multipart == null) {
            multipart = new MimeMultipart();
        }

        MimeBodyPart part = new MimeBodyPart();
        Object obj = val[0].toJavaObject();

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
        if (val.length == 2) {
            try {
                part.setFileName(val[1].toString());
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
    public void setSubject(ESValue val) throws Exception {
        if (val == null) {
            return;
        }

        message.setSubject(MimeUtility.encodeWord(val.toString()));
    }

    /**
     *
     *
     * @param add ...
     *
     * @throws Exception ...
     * @throws AddressException ...
     */
    public void setReplyTo(ESValue add) throws Exception {
        String addstring = add.toString();

        if (addstring.indexOf("@") < 0) {
            throw new AddressException();
        }

        Address[] replyTo = new Address[1];

        replyTo[0] = new InternetAddress(addstring);
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
    public void setFrom(ESValue[] add) throws Exception {
        String addstring = add[0].toString();

        if (addstring.indexOf("@") < 0) {
            throw new AddressException();
        }

        Address address = null;

        if (add.length > 1) {
            address = new InternetAddress(addstring,
                                          MimeUtility.encodeWord(add[1].toString()));
        } else {
            address = new InternetAddress(addstring);
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
    public void addTo(ESValue[] add) throws Exception {
        String addstring = add[0].toString();

        if (addstring.indexOf("@") < 0) {
            throw new AddressException();
        }

        Address address = null;

        if (add.length > 1) {
            address = new InternetAddress(addstring,
                                          MimeUtility.encodeWord(add[1].toString()));
        } else {
            address = new InternetAddress(addstring);
        }

        message.addRecipient(Message.RecipientType.TO, address);
    }

    /**
     *
     *
     * @param add ...
     *
     * @throws Exception ...
     * @throws AddressException ...
     */
    public void addCC(ESValue[] add) throws Exception {
        String addstring = add[0].toString();

        if (addstring.indexOf("@") < 0) {
            throw new AddressException();
        }

        Address address = null;

        if (add.length > 1) {
            address = new InternetAddress(addstring,
                                          MimeUtility.encodeWord(add[1].toString()));
        } else {
            address = new InternetAddress(addstring);
        }

        message.addRecipient(Message.RecipientType.CC, address);
    }

    /**
     *
     *
     * @param add ...
     *
     * @throws Exception ...
     * @throws AddressException ...
     */
    public void addBCC(ESValue[] add) throws Exception {
        String addstring = add[0].toString();

        if (addstring.indexOf("@") < 0) {
            throw new AddressException();
        }

        Address address = null;

        if (add.length > 1) {
            address = new InternetAddress(addstring,
                                          MimeUtility.encodeWord(add[1].toString()));
        } else {
            address = new InternetAddress(addstring);
        }

        message.addRecipient(Message.RecipientType.BCC, address);
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
