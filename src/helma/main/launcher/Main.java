// helma.main.Main

package helma.main.launcher;

import java.net.URLClassLoader;
import java.net.URL;
import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Method;

/** 
 *  Helma bootstrap class. Figures out Helma home directory, sets up class path and 
 *  lauchnes main class.
 */
public class Main {


    public static void main (String[] args) throws Exception {

	// try to get Helma installation directory
	try {
	    URLClassLoader apploader = (URLClassLoader) Main.class.getClassLoader();
	    // (URLClassLoader) Thread.currentThread().getContextClassLoader();
	    URL homeUrl = apploader.findResource("helma/main/launcher/Main.class");
	    String home = homeUrl.toString().substring(4);
	    int excl = home.indexOf ("!");
	    if (excl > -1) {
	        home = home.substring(0, excl);
	        homeUrl = new URL (home);
	        File f = new File (homeUrl.getPath());
	        f = f.getParentFile();
	        System.err.println ("GOT HOME: "+f);
	    }
	} catch (Exception ignore) {
	    // unable to get Helma home dir from launcher jar
	}
	File libdir = new File ("lib");
	File[] files = libdir.listFiles (new FilenameFilter() {
	    public boolean accept (File dir, String name) {
	        if (name.startsWith ("helma"))
	            return "helma.jar".equals (name);
	        return name.endsWith (".jar");
	    }
	});
	URL[] urls = new URL[files.length];
	for (int i=0; i<files.length; i++) {
	    urls[i] = new URL ("file:"+files[i].getAbsolutePath());
	}
	URLClassLoader loader = new URLClassLoader (urls);
	Thread.currentThread().setContextClassLoader (loader);
	Class clazz = loader.loadClass ("helma.main.Server");
	Class[] cargs = new Class[] { args.getClass() };
	Method main = clazz.getMethod ("main", cargs);
	Object[] nargs = new Object[] { args };
	main.invoke (null, nargs);
    }

}
