package helma.framework;

import java.io.Serializable;
import java.util.Map;

import helma.framework.core.Application;
import java.util.Date;

public class RequestBean implements Serializable {

	RequestTrans req;

	public RequestBean(RequestTrans req)	{
	this.req = req;
	}

	public Object get (String name) {
	return req.get (name);
	}

	public boolean isGet () {
	return req.isGet ();
	}

	public boolean isPost () {
	return req.isPost ();
	}

	public String toString() {
	return "[Request]";
	}

	// property related methods:

	public String getaction () {
	return req.action;
	}

	public Map getdata () {
	return req.getRequestData ();
	}

	public long getruntime () {
	return (System.currentTimeMillis() - req.startTime);
	}

	public String getpassword () {
	return req.getPassword ();
	}

	public String getpath () {
	return req.path;
	}

	public String getusername () {
	return req.getUsername ();
	}

	/* public Date getLastModified () {
	long since = req.getIfModifiedSince ();
	if (since < 0)
	    return null;
	else
	    return new Date (since);
	}

	public void setLastModified () {
	throw new RuntimeException ("The lastModified property of the Request object is read-only. "+
		"Set lastModified on the Response object if you want to mark the last modification date of a resource.");
	} */
}



