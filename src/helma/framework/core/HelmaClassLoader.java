// HelmaClassLoader.java

package helma.framework.core;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Hashtable;

/** 
 * ClassLoader subclass with package accessible addURL method.
 */
public class HelmaClassLoader extends URLClassLoader {

   /** 
	*  Create a HelmaClassLoader with the exact same URLs as the parent class loader. 
	*  We have to do this because we want to load some classes such as the scripting engine
	*  implementation or the root object from this specific class loader instead of the parent 
	*  class loader even if the parent class loader knows about them so we are isolated from 
	*  other applications running within this server.  Note that these classes have to be loaded 
	*  via findClass(), since loadClass() would consult the parent class loader as first thing.
 	*/
    public HelmaClassLoader(URL[] urls) {
	super ( urls, HelmaClassLoader.class.getClassLoader());
    }

    public void addURL (URL url) {
	super.addURL (url);
    }

}
