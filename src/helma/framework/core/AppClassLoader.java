// ApplicationClassLoader.java

package helma.framework.core;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;


/**
 * ClassLoader subclass with package accessible addURL method.
 */
public class AppClassLoader extends URLClassLoader {

    private final String appname;

   /**
    *  Create a HelmaClassLoader with the given application name and the given URLs
    */
    public AppClassLoader(String appname, URL[] urls) {
	super ( urls, AppClassLoader.class.getClassLoader());
	this.appname = appname;
    }

    protected void addURL (URL url) {
	super.addURL (url);
    }

    public String getAppName () {
	return appname;
    }

}
