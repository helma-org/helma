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

package helma.main.launcher;

import java.lang.reflect.*;

/**
 *  Helma bootstrap class. Figures out Helma home directory, sets up class path and
 *  lauchnes main class. This class must be invoked from a jar file in order to work.
 *
 *  @author Stefan Pollach
 */
public class Commandline {

    /**
     * boot method for running a request from the command line.
     * This retrieves the Helma home directory, creates the
     * classpath, get the request properties, creates the app and
     * runs it
     *-
     * @param args command line arguments
     */
    public static void main(String[] args) {
        try {
            String installDir = Main.getInstallDir(args);

            FilteredClassLoader loader = Main.createClassLoader(installDir);

            // get the main server class
            Class clazz = loader.loadClass("helma.main.CommandlineRunner");
            Class[] cargs = new Class[]{args.getClass()};
            Method main = clazz.getMethod("main", cargs);
            Object[] nargs = new Object[]{args};

            // and invoke the static main(String, String[]) method
            main.invoke(null, nargs);
        } catch (Exception x) {
            // unable to get Helma installation dir from launcher jar
            System.err.println("Unable to get Helma installation directory: ");
            System.err.println(x.getMessage());
            System.exit(2);
        }
    }
}
