/** 
 * Copyright 1999 Hannes Wallnoefer
 */
 
package helma.xmlrpc.test;

import helma.xmlrpc.*;
import java.util.*;
import java.io.IOException;

public class TestBase64 implements Runnable {

    XmlRpcClient client;
    static String url;
    static int clients = 1; // 6;
    static int loops = 1; //00;

    int gCalls = 0, gErrors = 0;

    byte[] data;

    public TestBase64 () throws Exception {
	client = new XmlRpcClientLite (url);
	
	Vector args = new Vector ();
	// Some JITs (Symantec, IBM) have problems with several Threads
	// starting all at the same time.
	// This initial XML-RPC call seems to pacify them.
    	args.addElement (new Integer (123));
	client.execute ("math.abs", args);

	data = new byte[20000];
	for (int j=0; j<data.length; j++)
	    data[j] = (byte) j;
	
    	for (int i=0; i<clients; i++) 
	    new Thread (this).start ();
    }
    
    public void run () {
	int errors = 0;
	int calls = 0;
	long start = System.currentTimeMillis ();
	try {
	    int val = (int) (-100 * Math.random ());
	    Vector args = new Vector ();

	    args.addElement (data);

	    for (int i=0; i<loops; i++) {

	        Vector v = (Vector) client.execute ("echo", args);
	        byte[] d = (byte[]) v.elementAt (0);
	        for (int j=0; j<d.length; j++)
	            if (d[j] != (byte) j) errors += 1;
	        calls += 1;
	    }
	} catch (IOException x) {
	    System.err.println ("Exception in client: "+x);
	    x.printStackTrace ();
	} catch (XmlRpcException x) {
	    System.err.println ("Server reported error: "+x);
	} catch (Exception other) {
	    System.err.println ("Exception in Benchmark client: "+other);
	}
	int millis = (int) (System.currentTimeMillis () - start);
	checkout (calls, errors, millis);
    }

    private synchronized void checkout (int calls, int errors, int millis) {
	clients--;	
	gCalls += calls;
	gErrors += errors;
	System.err.println ("Benchmark thread finished: "+calls+" calls, "+errors+" errors in "+millis+" milliseconds.");
	if (clients == 0) {
	    System.err.println ("");
	    System.err.println ("Benchmark result: "+(1000*gCalls/millis)+" calls per second.");
	}
    }
    
    public static void main (String args[]) throws Exception {
    	if (args.length > 0 && args.length < 3) {
    	    url = args[0];
	    XmlRpc.setKeepAlive (true);
	    // XmlRpc.setDebug (true);
    	    if (args.length == 2)
    	        XmlRpc.setDriver (args[1]);
    	    new TestBase64 ();
    	} else {
	    System.err.println ("Usage: java helma.xmlrpc.Benchmark URL [SAXDriver]");
	}
    }
    
}
