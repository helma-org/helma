// helma.main.Main

package helma.main.launcher;

import java.net.URLClassLoader;
import java.net.URL;
import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Method;
import java.util.ArrayList;

/** 
 *  Helma bootstrap class. Figures out Helma home directory, sets up class path and 
 *  lauchnes main class.
 */
public class Main {

    public static final String[] jars = { "helma.jar",  "jetty.jar", "crimson.jar", "xmlrpc.jar", 
                                                         "village.jar", "servlet.jar", "regexp.jar", "mail.jar", 
                                                          "activation.jar",  "netcomponents.jar", "jimi.jar",
                                                          "apache-dom.jar", "jdom.jar"};


    public static void main (String[] args) throws Exception {

	String home = null;
	// try to get Helma installation directory
	URLClassLoader apploader = null;
	try {
	    apploader = (URLClassLoader) Main.class.getClassLoader();
	    URL homeUrl = apploader.findResource("helma/main/launcher/Main.class");
	    home = homeUrl.toString().substring(4);
	    int excl = home.indexOf ("!");
	    if (excl > -1) {
	        home = home.substring(0, excl);
	        homeUrl = new URL (home);
	        File f = new File (homeUrl.getPath());
	        home = f.getParent();
	    }
	} catch (Exception ignore) {
	    // unable to get Helma home dir from launcher jar
	}
	args = new String[] { "-h", home, "-w", "8080" };
	File libdir = new File (home, "lib");
	ArrayList jarlist = new ArrayList ();
	for (int i=0;i<jars.length;i++) {
	    File jar = new File (libdir, jars[i]);
		if (jar.exists())
		    jarlist.add (new URL ("file:" + jar.getAbsolutePath()));
	}
	File extdir =new File (libdir, "ext");
	File[] files = extdir.listFiles (new FilenameFilter() {
	    public boolean accept (File dir, String name) {
	        return name.endsWith (".jar");
	    }
	});
	if (files != null) 
	    for (int i=0;i<files.length; i++)
	        jarlist.add (new URL ("file:" + files[i].getAbsolutePath()));
	URL[] urls = new URL[jarlist.size()];
	jarlist.toArray (urls);
	FilteredClassLoader loader = new FilteredClassLoader (urls, apploader);
	Thread.currentThread().setContextClassLoader (loader);
	Class clazz = loader.loadClass ("helma.main.Server");
	Class[] cargs = new Class[] { args.getClass() };
	Method main = clazz.getMethod ("main", cargs);
	Object[] nargs = new Object[] { args };
	main.invoke (null, nargs);
    }

}
