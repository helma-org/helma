/** 
 * Copyright 1999 Hannes Wallnoefer
 * Implements a XML-RPC client. See http://www.xmlrpc.com/
 */
 
package helma.xmlrpc;

import java.net.*;
import java.io.*;
import java.util.*;
import org.xml.sax.*;

/**
 * A multithreaded, reusable XML-RPC client object. Use this if you need a full-grown
 * HTTP client (e.g. for Proxy and Cookies support). If you don't need that, <code>XmlRpcClientLite</code>
 * may work better for you.
 */
public class XmlRpcClient implements XmlRpcHandler {
     
    URL url;
    String auth;
    int maxThreads = 100;

    // pool of worker instances
    Stack pool = new Stack ();
    int workers = 0;

    // average roundtrip of this method call. This is used to decide if
    // additional threads are needed or not in async mode
    int roundtrip = 1000;

    // a queue of calls to be handled asynchronously
    CallData first, last;

    /**
     * Construct a XML-RPC client with this URL.
     */
    public XmlRpcClient (URL url) {
	this.url = url;
    }

    /** 
     * Construct a XML-RPC client for the URL represented by this String.
     */
    public XmlRpcClient (String url) throws MalformedURLException {
	this.url = new URL (url);
    }
   
    /** 
     * Construct a XML-RPC client for the specified hostname and port.
     */
    public XmlRpcClient (String hostname, int port) throws MalformedURLException {
	this.url = new URL ("http://"+hostname+":"+port+"/RPC2");
    }
   
    /**
     * Return the URL for this XML-RPC client.
     */
    public URL getURL () {
	return url;
    }

    /**
     * Sets Authentication for this client. This will be sent as Basic Authentication header
     * to the server as described in <a href="http://www.ietf.org/rfc/rfc2617.txt">http://www.ietf.org/rfc/rfc2617.txt</a>.
     */
    public void setBasicAuthentication (String user, String password) {
	if (user == null || password == null)
	    auth = null;
	else {
	    char[] basicAuth = Base64.encode ((user+":"+password).getBytes());
	    auth = new String (basicAuth).trim();
	}
    }


    /**
     * Generate an XML-RPC request and send it to the server. Parse the result and
     * return the corresponding Java object.
     *
     * @exception XmlRpcException: If the remote host returned a fault message.
     * @exception IOException: If the call could not be made because of lower level problems.
     */
    public Object execute (String method, Vector params) throws XmlRpcException, IOException {
    	Worker worker = getWorker ();
	long start = System.currentTimeMillis ();
	try {
    	    Object retval =  worker.execute (method, params);
	    long end = System.currentTimeMillis ();
	    roundtrip = (int) ((roundtrip*4)+(end-start))/5;
	    return retval;
	} finally {
	    releaseWorker (worker);
	}
    }

    /**
     * Generate an XML-RPC request and send it to the server in a new thread.
     * This method returns immediately.
     * If the callback parameter is not null, it will be called later to handle the result or error when the call is finished.
     * 
     */
    public void executeAsync (String method, Vector params, AsyncCallback callback) {
	if (workers > 2) {
	    enqueue (method, params, callback);
	    return;
	}
    	Worker worker = null;
	try {
	    worker = getWorker ();
    	    worker.executeAsync (method, params, callback);
	} catch (IOException iox) {
	    // make a queued worker that doesn't run immediately
	    enqueue (method, params, callback);
	}
    }
    

    synchronized Worker getWorker () throws IOException {
	try {
	    Worker w =  (Worker) pool.pop ();
	     workers += 1;
	     return w;
	} catch (EmptyStackException x) {
	    if (workers < maxThreads) {
	        workers += 1;
	        return new Worker ();
	    }
	    throw new IOException ("XML-RPC System overload");
	}
    }

   /**
     * Release possibly big per-call object references to allow them to be garbage collected
     */
    synchronized void releaseWorker (Worker w) {
	w.result = null;
	w.call = null;
	if (pool.size() < 20 && !w.fault)
	    pool.push (w);
	workers -= 1;
    }


    synchronized void enqueue (String method, Vector params, AsyncCallback callback) {
	CallData call = new CallData (method, params, callback);
	if (last == null)
	    first = last = call;
	else {
	    last.next = call;
	    last = call;
	}
    }

    synchronized CallData dequeue () {
	if (first == null)
	    return null;
	// if (workers > 2 && workers*4 > roundtrip)
	//     return null;
	CallData call = first;
	if (first == last)
	    first = last = null;
	else
	    first = first.next;
	return call;
    }

    class Worker extends XmlRpc implements Runnable {

    boolean fault;
    Object result = null; 
    StringBuffer strbuf;

    CallData call;

    public Worker () {
    	super ();
    }


    public void executeAsync (String method, Vector params, AsyncCallback callback) {
	this.call = new CallData (method, params, callback);
	Thread t = new Thread (this);
	t.start ();
    }

    public void run () {
	while (call != null) {
	    runAsync (call.method, call.params, call.callback);
// System.err.println (workers+" ---- "+ roundtrip);
	    call = dequeue ();
	    // if (call != null) System.err.print (".");
	}
	releaseWorker (this);
    }

    void runAsync (String method, Vector params, AsyncCallback callback) {
	Object res = null;
	long start = System.currentTimeMillis ();
	try {
	    res = execute (method, params);
	    // notify callback object
	    if (callback != null)
	        callback.handleResult (res, url, method);
	} catch (Exception x) {
                 if (callback != null)
	        callback.handleError (x, url, method);
	}
	long end = System.currentTimeMillis ();
	roundtrip = (int) ((roundtrip*4)+(end-start))/5;
    }

    Object execute (String method, Vector params) throws XmlRpcException, IOException {
	fault = false;
	long now = System.currentTimeMillis ();
    	try {
	    ByteArrayOutputStream bout = new ByteArrayOutputStream ();

	    if (strbuf == null)
	        strbuf = new StringBuffer ();
	    else
	        strbuf.setLength (0);
	    XmlWriter writer = new XmlWriter (strbuf);
	    writeRequest (writer, method, params);
	    byte[] request = writer.getBytes();

	    URLConnection con = url.openConnection ();
	    con.setDoInput (true);
	    con.setDoOutput (true);
	    con.setUseCaches (false);
	    con.setAllowUserInteraction(false);
	    con.setRequestProperty ("Content-Length", Integer.toString (request.length));
	    con.setRequestProperty ("Content-Type", "text/xml");
	    if (auth != null)
	        con.setRequestProperty ("Authorization", "Basic "+auth);
	    // con.connect ();
	    OutputStream out = con.getOutputStream ();
	    out.write (request);
	    out.flush ();
	    InputStream in = con.getInputStream ();
	    parse (in);
	} catch (Exception x) {
	    x.printStackTrace ();
	    throw new IOException (x.getMessage ());
	}
	if (fault) { // generate an XmlRpcException
	    XmlRpcException exception = null;
	    try {
	        Hashtable f = (Hashtable) result;
	        String faultString = (String) f.get ("faultString");
	        int faultCode = Integer.parseInt (f.get ("faultCode").toString ());
	        exception = new XmlRpcException (faultCode, faultString.trim ());
	    } catch (Exception x) {
	        throw new XmlRpcException (0, "Invalid fault response");
	    }
	    throw exception;
	}
	if (debug)
	    System.err.println ("Spent "+(System.currentTimeMillis () - now)+" in request");
	return result;
    }


    /**
     * Called when the return value has been parsed. 
     */
    void objectParsed (Object what) {
	result = what;
    }
    
    
    /**
     * Generate an XML-RPC request from a method name and a parameter vector.
     */
    void writeRequest (XmlWriter writer, String method, Vector params) throws IOException {
	writer.startElement ("methodCall");

	writer.startElement ("methodName");
	writer.write (method);
	writer.endElement ("methodName");

	writer.startElement ("params");
	int l = params.size ();
	for (int i=0; i<l; i++) {
	    writer.startElement ("param");
	    writeObject (params.elementAt (i), writer);
	    writer.endElement ("param");
	}
	writer.endElement ("params");
	writer.endElement ("methodCall");
    }

    /**
     * Overrides method in XmlRpc to handle fault repsonses.
     */
    public void startElement (String name, AttributeList atts) throws SAXException {
	if ("fault".equals (name))
	    fault = true;
	else
	    super.startElement (name, atts);
    }


    } // end of inner class Worker
    

    class CallData {

    String method;
    Vector params;
    AsyncCallback callback;
    CallData next;

    /**
     * Make a call to be queued and then executed by the next free async thread
     */
    public CallData (String method, Vector params, AsyncCallback callback)  {
	this.method = method;
	this.params = params;
	this.callback = callback;
	this.next = null;
    }

    }


    /** 
     * Just for testing.
     */
    public static void main (String args[]) throws Exception {
	// XmlRpc.setDebug (true);
	// XmlRpc.setKeepAlive (true);
	try {
	    String url = args[0];
	    String method = args[1];
	    Vector v = new Vector ();
	    for (int i=2; i<args.length; i++) try {
	        v.addElement (new Integer (Integer.parseInt (args[i])));
	    } catch (NumberFormatException nfx) {
	        v.addElement (args[i]);
	    }
	    XmlRpcClient client = new XmlRpcClientLite (url);
	    try {
	        System.err.println (client.execute (method, v));
	    } catch (Exception ex) {
	        System.err.println ("Error: "+ex.getMessage());
	    }
	} catch (Exception x) {
	    System.err.println (x);
	    System.err.println ("Usage: java helma.xmlrpc.XmlRpcClient <url> <method> <arg> ....");
	    System.err.println ("Arguments are sent as integers or strings.");
	}
    }
}


