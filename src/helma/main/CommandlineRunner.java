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

import helma.framework.core.Application;
import helma.util.SystemProperties;
import java.io.File;
import java.util.*;

/**
 *  Helma command line runner class. This class creates and starts a single application,
 *  invokes a function in, writes its return value to the console and exits.
 *
 *  @author Stefan Pollach
 */
public class CommandlineRunner {

    /**
     * boot method for running a request from the command line.
     * This retrieves the Helma home directory, creates the app and
     * runs the function.
     *
     * @param args command line arguments
     *
     * @throws Exception if the Helma home dir or classpath couldn't be built
     */
    public static void main(String[] args) throws Exception {

        // parse arguments
        String commandStr = null;
        for (int i = 0; i < args.length; i++) {
            if ((i%2)==0 && !args[i].startsWith("-")) {
                commandStr = args[i];
            }
        }

        String appName = null;
        String function = null;
        try {
            int pos1 = commandStr.indexOf(".");
            appName = commandStr.substring(0, pos1);
            function = commandStr.substring(pos1+1);
        } catch (Exception str) {
            System.out.println("Error parsing command");
            System.out.println("");
            System.out.println("Usage: java helma.main.launcher.Commandline [appname].[function]");
            System.out.println("");
            System.exit(1);
        }

        String installDir = System.getProperty("helma.home");

        String propsPath = new File(installDir, "apps.properties").getAbsolutePath();
        // try to load server properties
        SystemProperties props = new SystemProperties(propsPath);

        String appPath = props.getProperty(appName+".appdir");
        String dbPath = props.getProperty(appName+".dbdir");

        File appHome = appPath == null ?
                new File(new File(installDir, "apps"), appName) :
                new File(appPath);
        File dbHome = dbPath == null ?
                new File(new File(installDir, "db"), appName) :
                new File(dbPath);

        // set up helma logging
        System.setProperty("org.apache.commons.logging.LogFactory",
                           "helma.util.Logging");
        System.setProperty("helma.logdir", "console");

        Application app = new Application(appName, appHome, dbHome);

        // init + start the app
        app.init();
        app.start();

        // execute the function
        Vector nargs = new Vector();
        Object result = app.executeExternal(function, nargs);
        System.out.println("got result " + result);

        // stop the app
        app.stop();

    }
}
