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

        ServerConfig config = new ServerConfig();
        String commandStr = null;
        Vector funcArgs = new Vector();
    
        // get possible environment setting for helma home
        if (System.getProperty("helma.home")!=null) {
            config.setHomeDir(new File(System.getProperty("helma.home")));
        }

        // parse arguments
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-h") && ((i + 1) < args.length)) {
                config.setHomeDir(new File(args[++i]));
            } else if (args[i].equals("-f") && ((i + 1) < args.length)) {
                config.setPropFile(new File(args[++i]));
            } else if (commandStr != null) {
                // we're past the command str, all args for the function
                funcArgs.add (args[i]);
            } else if ((i%2)==0 && !args[i].startsWith("-")) {
                // first argument without a switch
                commandStr = args[i];
            }
        }

        // get server.properties from home dir or vv
        try {
            Server.guessConfig (config);
        } catch (Exception ex) {
            printUsageError(ex.toString());
            System.exit(1);
        }

        String appName = null;
        String function = null;
        // now split application name + path/function-name
        try {
            int pos1 = commandStr.indexOf(".");
            appName = commandStr.substring(0, pos1);
            function = commandStr.substring(pos1+1);
        } catch (Exception ex) {
            printUsageError();
            System.exit(1);
        }

        // init a server instance and start the application
        Server server = new Server(config);
        server.init();
        server.checkAppManager();
        server.startApplication(appName);
        Application app = server.getApplication(appName);

        // execute the function
        try {
            Object result = app.executeExternal(function, funcArgs);
            if (result != null) {
                System.out.println(result.toString());
            }
        } catch (Exception ex) {
            System.out.println("Error in application " + appName + ":");
            System.out.println(ex.getMessage());
            if ("true".equals(server.getProperty("debug"))) {
                System.out.println("");
                ex.printStackTrace();
            }
        }

        // stop the application and server
        server.stop();
    }

    

    /**
      * print the usage hints and prefix them with a message.
      */
    public static void printUsageError(String msg) {
        System.out.println(msg);
        printUsageError();
    }


    /**
      * print the usage hints
      */
    public static void printUsageError() {
        System.out.println("");
        System.out.println("Error parsing command");
        System.out.println("");
        System.out.println("Usage: java helma.main.launcher.Commandline [options] [appname].[function] [argument-list]");
        System.out.println("");
        System.out.println("Possible options:");
        System.out.println("  -h dir       Specify hop home directory");
        System.out.println("  -f file      Specify server.properties file");
        System.out.println("");
    }

}
