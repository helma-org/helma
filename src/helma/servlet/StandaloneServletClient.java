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

import helma.framework.*;
import helma.framework.core.Application;
import helma.util.*;
import java.io.*;
import java.util.*;
import javax.servlet.*;

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
    private Application app = null;
    private String appName;
    private String appDir;
    private String dbDir;

    /**
     *
     *
     * @param init ...
     *
     * @throws ServletException ...
     */
    public void init(ServletConfig init) throws ServletException {
        super.init(init);

        appName = init.getInitParameter("application");

        if ((appName == null) || (appName.trim().length() == 0)) {
            throw new ServletException("application parameter not specified");
        }

        appDir = init.getInitParameter("appdir");

        if ((appDir == null) || (appDir.trim().length() == 0)) {
            throw new ServletException("appdir parameter not specified");
        }

        dbDir = init.getInitParameter("dbdir");

        if ((dbDir == null) || (dbDir.trim().length() == 0)) {
            throw new ServletException("dbdir parameter not specified");
        }
    }

    ResponseTrans execute(RequestTrans req) throws Exception {
        if (app == null) {
            createApp();
        }

        return app.execute(req);
    }

    /**
     * Create the application. Since we are synchronized only here, we
     * do another check if the app already exists and immediately return if it does.
     */
    synchronized void createApp() {
        if (app != null) {
            return;
        }

        try {
            File appHome = new File(appDir);
            File dbHome = new File(dbDir);

            app = new Application(appName, appHome, dbHome);
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
