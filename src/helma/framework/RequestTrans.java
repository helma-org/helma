// RequestTrans.java
// Copyright (c) Hannes Wallnöfer 1998-2000

package helma.framework;

import java.io.*;
import java.util.*;
import helma.objectmodel.*;

/**
 * A Transmitter for a request from the servlet client. Objects of this 
 * class are directly exposed to JavaScript as global property req. 
 */
 
public class RequestTrans implements Externalizable {

    public String path;
    public String session;
    private Hashtable values;

    // this is used to hold the EcmaScript form data object
    public transient Object data;
    // when was execution started on this request?
    public transient long startTime;

    public RequestTrans () {
	super ();
	values = new Hashtable ();
    }

    public RequestTrans (Hashtable values) {
	this.values = values;
    }

    public void set (String name, Object value) {
	values.put (name, value);
    }

    public Enumeration keys () {
	return values.keys ();
    }

    public Object get (String name) {
	try {
	    return values.get (name);
	} catch (Exception x) {
	    return null; 
	}
    }

    public Hashtable getReqData () {
	return values;
    }

    public int hashCode () {
	return session == null ? super.hashCode () : session.hashCode ();
    }


    /**
     * A request is considered equal to another one if it has the same user, path,
     * and request data. This is used to evaluate multiple simultanous requests only once
     */
    public boolean equals (Object what) {
	try {
	    RequestTrans other = (RequestTrans) what;
	    return (session.equals (other.session) &&
		path.equalsIgnoreCase (other.path) &&
		values.equals (other.getReqData ()));
	} catch (Exception x) {
	    return false;
	}
    }

    public void readExternal (ObjectInput s) throws ClassNotFoundException, IOException {
	path = s.readUTF ();
	session = s.readUTF ();
	values = (Hashtable) s.readObject ();
    }

    public void writeExternal (ObjectOutput s) throws IOException {
	s.writeUTF (path);
	s.writeUTF (session);
	s.writeObject (values);
    }
}
