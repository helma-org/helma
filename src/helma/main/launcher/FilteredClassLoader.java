// FilteredClassLoader.java

package helma.main.launcher;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Hashtable;

/** 
 * ClassLoader that is able to exclude certain packages from loading.
 */
public class FilteredClassLoader extends URLClassLoader {

    private Hashtable cache;
	
   /** 
	*  Create a HelmaClassLoader with the exact same URLs as the parent class loader. 
	*  We have to do this because we want to load some classes such as the scripting engine
	*  implementation or the root object from this specific class loader instead of the parent 
	*  class loader even if the parent class loader knows about them so we are isolated from 
	*  other applications running within this server.  Note that these classes have to be loaded 
	*  via findClass(), since loadClass() would consult the parent class loader as first thing.
 	*/
    public FilteredClassLoader(URL[] urls, ClassLoader parent) {
	super (urls, parent);
    }
	
	protected Class findClass (String name) throws ClassNotFoundException {
	if (name != null && (name.startsWith ("helma.scripting.fesi") || name.startsWith ("FESI")))
	    throw new ClassNotFoundException (name);
	return super.findClass (name);
	}
}
