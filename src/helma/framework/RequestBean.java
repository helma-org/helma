package helma.framework;

import java.io.Serializable;
import java.util.Map;

import helma.framework.core.Application;

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

}



