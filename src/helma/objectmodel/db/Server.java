// Server.java
// Copyright (c) Hannes Wallnöfer 1998-2000
 
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
    public static void main (String args[]) throws IOException {
	System.err.println ("The Helma main class is now in helma.main.Server. Please update your start script accordingly.");
	helma.main.Server.main (args);
    }

}

