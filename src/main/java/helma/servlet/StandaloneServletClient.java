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

package helma.servlet;

import helma.framework.repository.Repository;
import helma.framework.core.Application;
import helma.framework.repository.FileRepository;
import helma.main.ServerConfig;
import helma.main.Server;

import java.io.*;
import javax.servlet.*;
import java.util.*;

/**
 *  Standalone servlet client that runs a Helma application all by itself
 *  in embedded mode without relying on a central instance of helma.main.Server
 *  to start and manage the application.
 *
 *  StandaloneServletClient takes the following init parameters:
 *     <ul>
 *       <li> application - the application name </li>
 *       <li> appdir - the path of the application home directory </li>
 *       <li> dbdir - the path of the embedded XML data store </li>
 *     </ul>
 */
public final class StandaloneServletClient extends AbstractServletClient {
    private static final long serialVersionUID = 6515895361950250466L;

    private Application app = null;
    private String appName;
    private String appDir;
    private String dbDir;
    private String hopDir;
    private Repository[] repositories;

    /**
     *
     *
     * @param init ...
     *
     * @throws ServletException ...
     */
    public void init(ServletConfig init) throws ServletException {
        super.init(init);

        hopDir = init.getInitParameter("hopdir");

        if (hopDir == null) {
            // assume helmaDir to be current directory
            hopDir = ".";
        }

        appName = init.getInitParameter("application");

        if ((appName == null) || (appName.trim().length() == 0)) {
            throw new ServletException("application parameter not specified");
        }

        appDir = init.getInitParameter("appdir");

        dbDir = init.getInitParameter("dbdir");

        if ((dbDir == null) || (dbDir.trim().length() == 0)) {
            throw new ServletException("dbdir parameter not specified");
        }

        Class[] parameters = { String.class };
        ArrayList repositoryList = new ArrayList();

        for (int i = 0; true; i++) {
            String repositoryArgs = init.getInitParameter("repository." + i);
            if (repositoryArgs != null) {
                // lookup repository implementation
                String repositoryImpl = init.getInitParameter("repository." + i +
                        ".implementation");
                if (repositoryImpl == null) {
                    // implementation not set manually, have to guess it
                    if (repositoryArgs.endsWith(".zip")) {
                        repositoryImpl = "helma.framework.repository.ZipRepository";
                    } else if (repositoryArgs.endsWith(".js")) {
                        repositoryImpl = "helma.framework.repository.SingleFileRepository";
                    } else {
                        repositoryImpl = "helma.framework.repository.FileRepository";
                    }
                }
        
                try {
                    Repository newRepository = (Repository) Class.forName(repositoryImpl)
                        .getConstructor(parameters)
                        .newInstance(new Object[] {repositoryArgs});
                    repositoryList.add(newRepository);
                    log("adding repository: " + repositoryArgs);
                } catch (Exception ex) {
                    log("Adding repository " + repositoryArgs + " failed. " +
                        "Will not use that repository. Check your initArgs!", ex);
                }
            } else {
                // we always scan repositories 0-9, beyond that only if defined
                if (i > 9) {
                    break;
                }
            }
        }
        
        // add app dir
        FileRepository appRep = new FileRepository(appDir);
        log("adding repository: " + appDir);
        if (!repositoryList.contains(appRep)) {
            repositoryList.add(appRep);
        }

        repositories = new Repository[repositoryList.size()];
        repositories = (Repository[]) repositoryList.toArray(repositories);

    }

    /**
     * Returns the {@link helma.framework.core.Application Applicaton}
     * instance the servlet is talking to.
     *
     * @return this servlet's application instance
     */
    public Application getApplication() {
        if (app == null) {
            createApp();
        }

        return app;
    }

    /**
     * Create the application. Since we are synchronized only here, we
     * do another check if the app already exists and immediately return if it does.
     */
    protected synchronized void createApp() {

        if (app != null) {
            return;
        }

        try {
            File dbHome = new File(dbDir);
            File appHome = new File(appDir);
            File hopHome = new File(hopDir);

            ServerConfig config = new ServerConfig();
            config.setHomeDir(hopHome);
            Server server = new Server(config);
            server.init();

            app = new Application(appName, server, repositories, appHome, dbHome);
            app.init();
            app.start();
        } catch (Exception x) {
            log("Error starting Application " + appName + ": " + x);
            x.printStackTrace();
        }
    }

    /**
     * The servlet is being destroyed. Close and release the application if
     * it does exist.
     */
    public void destroy() {
        if (app != null) {
            try {
                app.stop();
            } catch (Exception x) {
                log("Error shutting down app " + app.getName() + ": ");
                x.printStackTrace();
            }
        }

        app = null;
    }
}
