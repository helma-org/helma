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


import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.xml.XmlConfiguration;

import java.net.URL;
import java.net.InetSocketAddress;
import java.io.IOException;
import java.io.File;

public class JettyServer {

    // the embedded web server
    protected org.eclipse.jetty.server.Server http;

    public static JettyServer init(Server server, ServerConfig config) throws IOException {
        File configFile = config.getConfigFile();
        if (configFile != null && configFile.exists()) {
            return new JettyServer(configFile.toURI().toURL());
        } else if (config.hasWebsrvPort()) {
            return new JettyServer(config.getWebsrvPort(), server);
        }
        return null;
    }

    private JettyServer(URL url) throws IOException {
        http = new org.eclipse.jetty.server.Server();

        try {
            XmlConfiguration config = new XmlConfiguration(url);
            config.configure(http);

        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Jetty configuration problem: " + e);
        }
    }

    private JettyServer(InetSocketAddress webPort, Server server)
            throws IOException {
    	
        http = new org.eclipse.jetty.server.Server();

        // start embedded web server if port is specified
        if (webPort != null) {
            HttpConfiguration httpConfig = new HttpConfiguration();
            httpConfig.setSendServerVersion(false);
            httpConfig.setSendDateHeader(false);
            HttpConnectionFactory connectionFactory = new HttpConnectionFactory(httpConfig);

            ServerConnector connector = new ServerConnector(http, -1, -1, connectionFactory);
            connector.setHost(webPort.getAddress().getHostAddress());
            connector.setPort(webPort.getPort());
            connector.setIdleTimeout(30000);
            connector.setSoLingerTime(-1);
            connector.setAcceptorPriorityDelta(0);
            connector.setAcceptQueueSize(0);

            http.addConnector(connector);
        }

    }

    public org.eclipse.jetty.server.Server getHttpServer() {
        return http;
    }

    public void start() throws Exception {
        openListeners();
        http.start();
    }

    public void stop() throws Exception {
        http.stop();
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
            ((ServerConnector) connectors[i]).open();
        }
    }
}
