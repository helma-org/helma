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

    InetAddrPort rmiPort    = null;
    InetAddrPort xmlrpcPort = null;
    InetAddrPort websrvPort = null;
    InetAddrPort ajp13Port  = null;
    File         propFile   = null;
    File         homeDir    = null;

    public boolean hasPropFile() {
        return (propFile != null);
    }

    public boolean hasHomeDir() {
        return (homeDir != null);
    }

}
