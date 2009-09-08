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

import java.io.File;
import java.net.InetSocketAddress;

/**
 * Utility class for server config
 */
 
public class ServerConfig {

    private InetSocketAddress xmlrpcPort = null;
    private InetSocketAddress websrvPort = null;
    private InetSocketAddress ajp13Port  = null;
    private File propFile   = null;
    private File homeDir    = null;
    private File configFile = null;
    private String[] apps = null;

    public boolean hasPropFile() {
        return (propFile != null);
    }

    public boolean hasConfigFile() {
        return (configFile != null);
    }

    public boolean hasHomeDir() {
        return (homeDir != null);
    }

    public boolean hasXmlrpcPort() {
        return (xmlrpcPort != null);
    }

    public boolean hasWebsrvPort() {
        return (websrvPort != null);
    }

    public boolean hasAjp13Port() {
        return (ajp13Port != null);
    }

    public boolean hasApps() {
        return (apps != null);
    }

    public InetSocketAddress getXmlrpcPort() {
        return xmlrpcPort;
    }

    public void setXmlrpcPort(InetSocketAddress xmlrpcPort) {
        this.xmlrpcPort = xmlrpcPort;
    }

    public InetSocketAddress getWebsrvPort() {
        return websrvPort;
    }

    public void setWebsrvPort(InetSocketAddress websrvPort) {
        this.websrvPort = websrvPort;
    }

    public InetSocketAddress getAjp13Port() {
        return ajp13Port;
    }

    public void setAjp13Port(InetSocketAddress ajp13Port) {
        this.ajp13Port = ajp13Port;
    }

    public File getPropFile() {
        return propFile;
    }

    public void setPropFile(File propFile) {
        this.propFile = propFile == null ? null : propFile.getAbsoluteFile();
    }

    public File getHomeDir() {
        return homeDir;
    }

    public void setHomeDir(File homeDir) {
        this.homeDir = homeDir == null ? null : homeDir.getAbsoluteFile();
    }
    
    public File getConfigFile() {
		return configFile;
	}

	public void setConfigFile(File configFile) {
		this.configFile = configFile == null ? null : configFile.getAbsoluteFile();
	}

    public String[] getApps() {
        return apps;
    }

    public void setApps(String[] apps) {
        this.apps = apps;
    }
}
