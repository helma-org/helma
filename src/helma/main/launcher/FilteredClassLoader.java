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

import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.util.Hashtable;

/**
 * ClassLoader that is able to exclude certain packages from loading.
 */
public class FilteredClassLoader extends URLClassLoader {
    /**
     *  Create a server wide class loader that doesn't see the scripting engine(s)
     *  embedded in helma.jar. These files should be loaded by the per-application
     *  class loaders so that special security policies can be applied to them and
     *  so that they can load classes from jar files in the app directories.
     */
    public FilteredClassLoader(URL[] urls) {
        super(urls, null);
    }

    /**
     *  Mask classes that implement the scripting engine(s) contained in helma.jar
     */
    protected Class findClass(String name) throws ClassNotFoundException {
        if ((name != null) && (name.startsWith("helma")) &&
                              (name.endsWith("PhantomEngine"))) {
            throw new ClassNotFoundException(name);
        }

        return super.findClass(name);
    }
}
