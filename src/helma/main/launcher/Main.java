// helma.main.Main

package helma.main.launcher;

import java.net.URLClassLoader;
import java.net.URL;
import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.security.Policy;

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

	// check if home directory is set via command line arg. If not,
	// we'll get it from the location of the jar file this class
	// has been loaded from.
	String home = null;
	// first, try to get helma home dir from command line options
	for (int i=0; i<args.length; i++) {
	    if (args[i].equals ("-h") && i+1<args.length) {
	        home = args[i+1];
	    }
	}
	URLClassLoader apploader = (URLClassLoader) ClassLoader.getSystemClassLoader();
	// try to get Helma installation directory
	if (home == null) {
	    try {
	        URL homeUrl = apploader.findResource("helma/main/launcher/Main.class");
	        // this is a  JAR URL of the form
	        //    jar:<url>!/{entry}
	        // we strip away the jar: prefix and the !/{entry} suffix
	        // to get the original jar file URL
	        home = homeUrl.toString().substring(4);
	        int excl = home.indexOf ("!");
	        if (excl > -1) {
	            home = home.substring(0, excl);
	            homeUrl = new URL (home);
	            File f = new File (homeUrl.getPath());
	            home = f.getParent();
	            // add home dir to the command line arguments
	            String[] newArgs = new String [args.length+2];
	            newArgs[0] = "-h";
	            newArgs[1] = home;
	            System.arraycopy (args, 0, newArgs, 2, args.length);
	            args = newArgs;
	        }
	    } catch (Exception ignore) {
	        // unable to get Helma home dir from launcher jar
	    }
	}
	// set the current working directory to the helma home dir.
	// note that this is not a real cwd, which is not supported
	// by java. It makes sure relative to absolute path name
	// conversion is done right, so for Helma code, this should
	// work.
	System.setProperty ("user.dir", home);

	// set up the class path
	File libdir = new File (home, "lib");
	ArrayList jarlist = new ArrayList ();
	for (int i=0;i<jars.length;i++) {
	    File jar = new File (libdir, jars[i]);
	    jarlist.add (new URL ("file:" + jar.getAbsolutePath()));
	}
	// add all jar files from the lib/ext directory
	File extdir =new File (libdir, "ext");
	File[] files = extdir.listFiles (new FilenameFilter() {
	    public boolean accept (File dir, String name) {
	        return name.toLowerCase().endsWith (".jar");
	    }
	});
	if (files != null)
	    for (int i=0;i<files.length; i++)
	        // WORKAROUND: add the files in lib/ext before
	        // lib/apache-dom.jar, since otherwise putting a full version 
	        // of Xerces in lib/ext would cause a version conflict with the 
	        // xerces classes in lib/apache-dom.jar. Generally, having some pieces 
	        // of Xerces in lib/apache-dom.jar is kind of problematic.
	        jarlist.add (jars.length-3, new URL ("file:" + files[i].getAbsolutePath()));
	URL[] urls = new URL[jarlist.size()];
	jarlist.toArray (urls);
	FilteredClassLoader loader = new FilteredClassLoader (urls);
	// set the new class loader as context class loader
	Thread.currentThread().setContextClassLoader (loader);
	// get the main server class
	Class clazz = loader.loadClass ("helma.main.Server");
	Class[] cargs = new Class[] { args.getClass() };
	Method main = clazz.getMethod ("main", cargs);
	Object[] nargs = new Object[] { args };
	// run
	main.invoke (null, nargs);
    }

}
