// HopSocketFactory.java
// Copyright (c) Hannes Wallnöfer 1999-2000


package helma.main;

import helma.util.*;
import java.net.*;
import java.rmi.server.*;
import java.io.IOException;

/**
 * An RMI socket factory that has a "paranoid" option to filter clients.
 * We only do direct connections, no HTTP proxy stuff, since this is
 * server-to-server.
 */

public class HopSocketFactory extends RMISocketFactory {

    private InetAddressFilter filter;

    public HopSocketFactory () {
	filter = new InetAddressFilter ();
    }

    public void addAddress (String address) {
	try {
	    filter.addAddress (address);
	} catch (IOException x) {
	    Server.getLogger().log ("Could not add "+address+" to Socket Filter: invalid address.");
	}
    }

    public Socket createSocket(String host, int port) throws IOException {
	return new Socket (host, port);
    }

    public ServerSocket createServerSocket(int port) throws IOException {
	return new ParanoidServerSocket (port, filter);
    }

}
