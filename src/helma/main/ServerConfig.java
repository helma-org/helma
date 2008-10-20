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

import org.mortbay.util.InetAddrPort;
import java.io.File;

/**
 * Utility class for server config
 */
 
public class ServerConfig {

    private InetAddrPort rmiPort    = null;
    private InetAddrPort xmlrpcPort = null;
    private InetAddrPort websrvPort = null;
    private InetAddrPort ajp13Port  = null;
    private File         propFile   = null;
    private File         homeDir    = null;

    public boolean hasPropFile() {
        return (propFile != null);
    }

    public boolean hasHomeDir() {
        return (homeDir != null);
    }

    public boolean hasRmiPort() {
        return (rmiPort != null);
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

    public InetAddrPort getRmiPort() {
        return rmiPort;
    }

    public void setRmiPort(InetAddrPort rmiPort) {
        this.rmiPort = rmiPort;
    }

    public InetAddrPort getXmlrpcPort() {
        return xmlrpcPort;
    }

    public void setXmlrpcPort(InetAddrPort xmlrpcPort) {
        this.xmlrpcPort = xmlrpcPort;
    }

    public InetAddrPort getWebsrvPort() {
        return websrvPort;
    }

    public void setWebsrvPort(InetAddrPort websrvPort) {
        this.websrvPort = websrvPort;
    }

    public InetAddrPort getAjp13Port() {
        return ajp13Port;
    }

    public void setAjp13Port(InetAddrPort ajp13Port) {
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
}
