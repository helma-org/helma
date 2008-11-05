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

import org.mortbay.http.HttpServer;
import org.mortbay.http.HttpContext;
import org.mortbay.http.ajp.AJP13Listener;
import org.mortbay.util.InetAddrPort;

import java.util.StringTokenizer;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.IOException;

public class JettyServer {

    // the embedded web server
    protected HttpServer http;

    // the AJP13 Listener, used for connecting from external webserver to servlet via JK
    protected AJP13Listener ajp13;

    public static JettyServer init(Server server)
            throws MalformedURLException, IOException {
        if (server.configFile != null && server.configFile.exists()) {
            return new JettyServer(server.configFile.toURI().toURL());
        } else if (server.websrvPort != null || server.ajp13Port != null) {
            return new JettyServer(server.websrvPort, server.ajp13Port, server);
        }
        return null;
    }

    private JettyServer(URL url) throws IOException {
        http = new org.mortbay.jetty.Server(url);
    }

    private JettyServer(InetEndpoint webPort, InetEndpoint ajpPort, Server server)
            throws IOException {
        http = new HttpServer();

        // start embedded web server if port is specified
        if (webPort != null) {
            http.addListener(new InetAddrPort(webPort.getInetAddress(), webPort.getPort()));
        }

        // activate the ajp13-listener
        if (ajpPort != null) {
            // create AJP13Listener
            ajp13 = new AJP13Listener(new InetAddrPort(ajpPort.getInetAddress(), ajpPort.getPort()));
            ajp13.setHttpServer(http);

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

            ajp13.setRemoteServers(jkallowarr);
            server.getLogger().info("Starting AJP13-Listener on port " + (ajpPort));            
        }
    }

    public HttpServer getHttpServer() {
        return http;
    }

    public HttpContext getContext(String contextPath) {
        return http.getContext(contextPath);
    }

    public HttpContext addContext(String contextPath) {
        return http.addContext(contextPath);
    }

    public void start() throws Exception {
        http.start();
        if (ajp13 != null) {
            ajp13.start();
        }
    }

    public void stop() throws InterruptedException {
        http.stop();
        if (ajp13 != null) {
            ajp13.stop();
        }
    }

    public void destroy() {
        http.destroy();
    }
}
