// ESMail.java
// Copyright (c) Hannes Wallnöfer 1998-2000


package helma.scripting.fesi.extensions;

import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.*;
import java.io.*;
import java.util.*;
import helma.util.*;
import FESI.Data.*;
import FESI.Interpreter.*;
import FESI.Exceptions.*;

/**
 * A JavaScript wrapper around a JavaMail message class to send 
 * mail via SMTP from Helma
 */

public class ESMail extends ESObject implements Serializable {

    MailExtension mailx;
    Properties mprops;
    MimeMessage message;
    Multipart multipart;
    StringBuffer buffer;

    int status;
    public static final int OK=0;
    public static final int SUBJECT=10;
    public static final int TEXT=11;
    public static final int MIMEPART=12;
    public static final int TO=20;
    public static final int CC=21;
    public static final int BCC=22;
    public static final int FROM=23;
    public static final int REPLYTO=24;
    public static final int SEND=30;


    public ESMail (MailExtension mailx) {
	super (mailx.esMailPrototype, mailx.evaluator);
	this.status = OK;
	this.mailx = mailx;
	this.mprops = mailx.mprops;

	// create some properties and get the default Session
	try {
	    Properties props = new Properties();
	    props.put ("mail.smtp.host", mprops.getProperty ("smtp", "mail"));

	    Session session = Session.getDefaultInstance(props, null);
	    message = new MimeMessage (session);
	} catch (Throwable t) {
	    this.evaluator.reval.app.logEvent ("Error in mail constructor: "+t);
	}
    }

    public void setStatus (int status) {
	// Only register the first error that occurrs
	if (this.status == 0)
	    this.status = status;
    }

    public int getStatus () {
	return status;
    }

    public ESValue getProperty(String propertyName, int hash) throws EcmaScriptException {
	if ("status".equalsIgnoreCase (propertyName))
	    return new ESNumber (status);
	return super.getProperty (propertyName, hash);
    }

    /**
     *
     */

    public void setText (ESValue val) throws Exception {
	if (buffer == null)
	    buffer = new StringBuffer ();
	if (val != null)
	    buffer.append (val.toString ());
    }

    public void addPart (ESValue val[]) throws Exception {
	if (val == null || val.length == 0) return;
	if (multipart == null) {
	    multipart = new MimeMultipart ();
	}
	for (int i=0; i<val.length; i++) {
	    // FIXME: addPart is broken.
	    MimeBodyPart part = new MimeBodyPart ();
	    Object obj = val[i].toJavaObject ();
	    if (obj instanceof String) {
	        part.setContent (obj.toString (), "text/plain");
	    } else if (obj instanceof File) {
	        FileDataSource source = new FileDataSource ((File) obj);
	        part.setDataHandler (new DataHandler (source));
	    }
	    multipart.addBodyPart (part);
	}
    }

    public void setSubject (ESValue val) throws Exception {
	if (val == null)
	    return;
	message.setSubject (MimeUtility.encodeWord (val.toString (), "iso-8859-1", null));
    }

    public void setReplyTo (ESValue add) throws Exception {
	String addstring = add.toString ();
	if (addstring.indexOf ("@") < 0)
	    throw new AddressException ();
	Address replyTo[] = new Address[1];
	replyTo[0] = new InternetAddress (addstring);
	message.setReplyTo (replyTo);
    }

    public void setFrom (ESValue add[]) throws Exception {
	String addstring = add[0].toString ();
	if (addstring.indexOf ("@") < 0)
	    throw new AddressException ();
	Address address  = null;
	if (add.length > 1)
	    address =  new InternetAddress (addstring, MimeUtility.encodeWord (add[1].toString (), "iso-8859-1", null));
	else
	    address = new InternetAddress (addstring);
	message.setFrom (address);
    }

    public void addTo (ESValue add[]) throws Exception {
	String addstring = add[0].toString ();
	if (addstring.indexOf ("@") < 0)
	    throw new AddressException ();
	Address address  = null;
	if (add.length > 1)
	    address =  new InternetAddress (addstring, MimeUtility.encodeWord (add[1].toString (), "iso-8859-1", null));
	else
	    address = new InternetAddress (addstring);
	message.addRecipient (Message.RecipientType.TO, address);
    }

    public void addCC (ESValue add[]) throws Exception {
	String addstring = add[0].toString ();
	if (addstring.indexOf ("@") < 0)
	    throw new AddressException ();
	Address address  = null;
	if (add.length > 1)
	    address =  new InternetAddress (addstring, MimeUtility.encodeWord (add[1].toString (), "iso-8859-1", null));
	else
	    address = new InternetAddress (addstring);
	message.addRecipient (Message.RecipientType.CC, address);
    }

    public void addBCC (ESValue add[]) throws Exception {
	String addstring = add[0].toString ();
	if (addstring.indexOf ("@") < 0)
	    throw new AddressException ();
	Address address  = null;
	if (add.length > 1)
	    address =  new InternetAddress (addstring, MimeUtility.encodeWord (add[1].toString (), "iso-8859-1", null));
	else
	    address = new InternetAddress (addstring);
	message.addRecipient (Message.RecipientType.BCC, address);
    }

    public void send () throws Exception {
	if (buffer != null)
	    message.setText (buffer.toString ());
	else if (multipart != null)
	    message.setContent (multipart);
	else
	    message.setText ("");
	Transport.send (message);
    }


}

