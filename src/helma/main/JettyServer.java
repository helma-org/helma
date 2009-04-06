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

package helma.main;


import org.mortbay.jetty.Connector;
import org.mortbay.jetty.ajp.Ajp13SocketConnector;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.nio.SelectChannelConnector;

import java.util.StringTokenizer;
import java.net.URL;
import java.net.InetSocketAddress;
import java.io.IOException;

public class JettyServer {

    // the embedded web server
    protected org.mortbay.jetty.Server http;

    // the AJP13 Listener, used for connecting from external webserver to servlet via JK
    protected Ajp13SocketConnector ajp13;

    public static JettyServer init(Server server) throws IOException {
        if (server.configFile != null && server.configFile.exists()) {
            return new JettyServer(server.configFile.toURI().toURL());
        } else if (server.websrvPort != null || server.ajp13Port != null) {
            return new JettyServer(server.websrvPort, server.ajp13Port, server);
        }
        return null;
    }

    private JettyServer(URL url) throws IOException {
        // TODO: this is wrong. url is supposed to be the url of a jetty config file.
        http = new org.mortbay.jetty.Server(url.getPort());
        openListeners();
    }

    private JettyServer(InetSocketAddress webPort, InetSocketAddress ajpPort, Server server)
            throws IOException {
    	
        http = new org.mortbay.jetty.Server();
        http.setServer(http);
        
        // start embedded web server if port is specified
        if (webPort != null) {
        	Connector conn = new SelectChannelConnector();
        	conn.setHost(webPort.getAddress().getHostAddress());
        	conn.setPort(webPort.getPort());
        	
        	http.addConnector(conn);
        }

        // activate the ajp13-listener
        if (ajpPort != null) {
            // create AJP13Listener
        	ajp13 = new Ajp13SocketConnector();
        	ajp13.setHost(ajpPort.getAddress().getHostAddress());
        	ajp13.setPort(ajpPort.getPort());
        	
        	http.addConnector(ajp13);

            String jkallow = server.sysProps.getProperty("allowAJP13");

            // by default the AJP13-connection just accepts requests from 127.0.0.1
            if (jkallow == null) {
                jkallow = "127.0.0.1";
            }

            StringTokenizer st = new StringTokenizer(jkallow, " ,;");
            String[] jkallowarr = new String[st.countTokens()];
            int cnt = 0;

            while (st.hasMoreTokens()) {
                jkallowarr[cnt] = st.nextToken();
                cnt++;
            }

            // TODO:
            //ajp13.setRemoteServers(jkallowarr);
            server.getLogger().info("Starting AJP13-Listener on port " + (ajpPort));            
        }
        openListeners();
    }

    public org.mortbay.jetty.Server getHttpServer() {
        return http;
    }
/* TODO:
    public HttpContext getContext(String contextPath) {
        return http.getContext(contextPath);
    }

    public HttpContext addContext(String contextPath) {
        return http.addContext(contextPath);
    }
*/

    public void start() throws Exception {
        http.start();
        if (ajp13 != null) {
            ajp13.start();
        }
    }

    public void stop() throws Exception {
        http.stop();
        if (ajp13 != null) {
            ajp13.stop();
        }
    }

    public void destroy() {
        http.destroy();
    }

    private void openListeners() throws IOException {
        // opening the listener here allows us to run on priviledged port 80 under jsvc
        // even as non-root user, because init() is called with root privileges
        // while start() will be called with the user we will actually run as
        Connector[] connectors = http.getConnectors();
        for (int i = 0; i < connectors.length; i++) {
            if (connectors[i] instanceof SocketConnector) {
                SocketConnector connector = (SocketConnector) connectors[i];
                connector.open();
            }
        }
    }
}
