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
import java.io.*;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.Hashtable;
import javax.servlet.*;
import javax.servlet.http.*;

/**
 * This is the HOP servlet adapter. This class communicates with any
 * Hop application on a given Hop server, extracting the application name
 * from the request path.
 */
public class MultiServletClient extends AbstractServletClient {
    private Hashtable apps;

    /**
     *
     *
     * @param init ...
     *
     * @throws ServletException ...
     */
    public void init(ServletConfig init) throws ServletException {
        super.init(init);
        apps = new Hashtable();
        host = init.getInitParameter("host");

        if (host == null) {
            host = "localhost";
        }

        String portstr = init.getInitParameter("port");

        port = (portstr == null) ? 5055 : Integer.parseInt(portstr);
        hopUrl = "//" + host + ":" + port + "/";
    }

    /**
     *
     */
    public void destroy() {
        if (apps != null) {
            apps.clear();
            apps = null;
        }
    }

    ResponseTrans execute(RequestTrans req) throws Exception {
        // the app-id is the first element in the request path
        // so we have to first get than and than rewrite req.path.
        int slash = req.path.indexOf("/");
        String appId = null;

        if (slash == -1) {
            // no slash found, path equals app-id
            appId = req.path;
            req.path = "";
        } else {
            // cut path into app id and rewritten path
            appId = req.path.substring(0, slash);
            req.path = req.path.substring(slash + 1);
        }

        IRemoteApp app = getApp(appId);

        try {
            return app.execute(req);
        } catch (Exception x) {
            invalidateApp(appId);
            app = getApp(appId);

            return app.execute(req);
        }
    }

    IRemoteApp getApp(String appId) throws Exception {
        IRemoteApp app = (IRemoteApp) apps.get(appId);

        if (app != null) {
            return app;
        }

        app = (IRemoteApp) Naming.lookup(hopUrl + appId);
        apps.put(appId, app);

        return app;
    }

    void invalidateApp(String appId) {
        apps.remove(appId);
    }
}
