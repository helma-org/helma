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
 
public class RequestTrans implements Serializable {

    public String path;
    public String session;
    private Hashtable values;
    // this is used to hold the EcmaScript form data object
    public transient Object data;

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

}
