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

package helma.framework.core;

import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.util.Enumeration;

/**
 * ClassLoader subclass with package accessible addURL method.
 */
public class AppClassLoader extends URLClassLoader {
    private final String appname;

    /**
     *  Create a HelmaClassLoader with the given application name and the given URLs
     */
    public AppClassLoader(String appname, URL[] urls) {
        super(urls, AppClassLoader.class.getClassLoader());
        this.appname = appname;
    }

    protected void addURL(URL url) {
        super.addURL(url);
    }

    /**
     *
     *
     * @return ...
     */
    public String getAppName() {
        return appname;
    }
}
