// ParanoidServerSocket.java
// Copyright (c) Hannes Wallnöfer 1999-2000

package helma.util;

import java.net.*;
import java.io.IOException;

/**
 * A server socket that can allow connections to only a few selected hosts.
 */

public class ParanoidServerSocket extends ServerSocket {

    private InetAddressFilter filter;


    public ParanoidServerSocket (int port) throws IOException {
	super (port);
    }

    public ParanoidServerSocket (int port, InetAddressFilter filter) throws IOException {
	super (port);
	this.filter = filter;
    }

    public Socket accept () throws IOException {
	Socket s = null;
	while (s == null) {
	    s = super.accept ();
	    if (filter != null && !filter.matches (s.getInetAddress ())) {
	        System.err.println ("Refusing connection from "+s.getInetAddress ());
	        try {
	            s.close();
	        } catch (IOException ignore) {}
	        s = null;
	    }
	}
	return s;
    }

    public void setFilter (InetAddressFilter filter) {
	this.filter = filter;
    }

    public InetAddressFilter getFilter () {
	return this.filter;
    }

}
