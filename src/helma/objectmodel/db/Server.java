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

package helma.objectmodel.db;

import java.io.IOException;

/**
 * Helma server main class. This moved to the helma.main package but a
 * simple redirector is kept here for backwards compatibility.
 */
public class Server {
    /**
     * Just invoke the main method in the new Server class.
     */
    public static void main(String[] args) throws IOException {
        System.err.println("The Helma main class is now in helma.main.Server. Please update your start script accordingly.");
        helma.main.Server.main(args);
    }
}
