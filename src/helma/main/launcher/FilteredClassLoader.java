// FilteredClassLoader.java

package helma.main.launcher;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Hashtable;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.CodeSource;

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
	super (urls);
    }

    /**
     *  Mask classes that implement the scripting engine(s) contained in helma.jar
     */
    protected Class findClass (String name) throws ClassNotFoundException {
	if (name != null && "helma.scripting.fesi.PhantomEngine".equals (name))
	    throw new ClassNotFoundException (name);
	return super.findClass (name);
    }
}
