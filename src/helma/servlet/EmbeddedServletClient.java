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
import helma.main.*;
import javax.servlet.*;

/**
 *  Servlet client that runs a Helma application for the embedded
 *  web server
 */
public final class EmbeddedServletClient extends AbstractServletClient {
    private Application app = null;
    private String appName;

    /**
     * Creates a new EmbeddedServletClient object.
     */
    public EmbeddedServletClient() {
        super();
    }

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

        if (appName == null) {
            throw new ServletException("Application name not set in init parameters");
        }
    }

    /**
     * Returns the {@link helma.framework.core.Application Applicaton}
     * instance the servlet is talking to.
     *
     * @return this servlet's application instance
     */
    Application getApplication() {
        if (app == null) {
            app = Server.getServer().getApplication(appName);
        }

        return app;
    }
}
