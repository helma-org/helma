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
import java.rmi.Naming;
import javax.servlet.*;

/**
 * This is the standard Helma servlet adapter. This class represents a servlet
 * that is dedicated to one Helma application over RMI.
 */
public class ServletClient extends AbstractServletClient {
    private IRemoteApp app = null;
    private String appName = null;

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
        host = init.getInitParameter("host");

        if (host == null) {
            host = "localhost";
        }

        String portstr = init.getInitParameter("port");

        port = (portstr == null) ? 5055 : Integer.parseInt(portstr);
        hopUrl = "//" + host + ":" + port + "/";

        if (appName == null) {
            throw new ServletException("Application name not specified for helma.servlet.ServletClient");
        }
    }

    /**
     *
     */
    public void destroy() {
        if (app != null) {
            app = null;
        }
    }

    ResponseTrans execute(RequestTrans req) throws Exception {
        if (app == null) {
            initApp();
        }

        try {
            return app.execute(req);
        } catch (Exception x) {
            initApp();

            return app.execute(req);
        }
    }

    synchronized void initApp() throws Exception {
        app = (IRemoteApp) Naming.lookup(hopUrl + appName);
    }
}
