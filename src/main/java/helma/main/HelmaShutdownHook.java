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

import helma.util.*;
import org.apache.commons.logging.LogFactory;

/**
 * ShutdownHook that shuts down all running Helma applications on exit.
 */
public class HelmaShutdownHook extends Thread {


    /**
     *
     */
    public void run() {
        System.err.println("Shutting down Helma - please stand by...");

        Server server = Server.getServer();
        if (server != null) {
            server.stop();
            server.shutdown();
        }
     }
}
