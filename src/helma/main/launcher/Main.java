// helma.main.Main

package helma.main.launcher;

import java.net.URLClassLoader;
import java.net.URL;
import java.net.URLDecoder;
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

    public static final String[] jars = {
        "helma.jar",
        "jetty.jar",
        "crimson.jar",
        "xmlrpc.jar",
        "servlet.jar",
        "regexp.jar",
        "mail.jar",
        "activation.jar",
        "netcomponents.jar",
        "jimi.jar",
        "apache-dom.jar",
        "jdom.jar"
    };


    public static void main (String[] args) throws Exception {

	// check if home directory is set via command line arg. If not,
	// we'll get it from the location of the jar file this class
	// has been loaded from.
	String installDir = null;
	// first, try to get helma home dir from command line options
	for (int i=0; i<args.length; i++) {
	    if (args[i].equals ("-i") && i+1<args.length) {
	        installDir = args[i+1];
	    }
	}
	URLClassLoader apploader = (URLClassLoader) ClassLoader.getSystemClassLoader();
	// try to get Helma installation directory
	if (installDir == null) {
	    try {
	        URL launcherUrl = apploader.findResource("helma/main/launcher/Main.class");
	        // this is a  JAR URL of the form
	        //    jar:<url>!/{entry}
	        // we strip away the jar: prefix and the !/{entry} suffix
	        // to get the original jar file URL
	        installDir = launcherUrl.toString().substring(4);
	        int excl = installDir.indexOf ("!");
	        if (excl > -1) {
	            installDir = installDir.substring(0, excl);
	            launcherUrl = new URL (installDir);
	            File f = new File (launcherUrl.getPath());
	            installDir = f.getParentFile().getCanonicalPath();
	        }
	    } catch (Exception x) {
	        // unable to get Helma installation dir from launcher jar
	        System.err.println ("Unable to get Helma installation directory: "+x);
	        System.exit (2);
	    }
	}

	// decode installDir in case it is URL-encoded
	installDir = URLDecoder.decode (installDir);

	// set up the class path
	File libdir = new File (installDir, "lib");
	ArrayList jarlist = new ArrayList ();
	for (int i=0;i<jars.length;i++) {
	    File jar = new File (libdir, jars[i]);
	    jarlist.add (new URL ("file:" + jar.getAbsolutePath()));
	}
	// add all jar files from the lib/ext directory
	File extdir =new File (libdir, "ext");
	File[] files = extdir.listFiles (new FilenameFilter() {
	    public boolean accept (File dir, String name) {
	        String n = name.toLowerCase();
	        return n.endsWith (".jar") || n.endsWith (".zip");
	    }
	});
	if (files != null)
	    for (int i=0;i<files.length; i++) {
	        // WORKAROUND: add the files in lib/ext before
	        // lib/apache-dom.jar, since otherwise putting a full version
	        // of Xerces in lib/ext would cause a version conflict with the
	        // xerces classes in lib/apache-dom.jar. Generally, having some pieces
	        // of Xerces in lib/apache-dom.jar is kind of problematic.
	        jarlist.add (jars.length-3, new URL ("file:" + files[i].getAbsolutePath()));
	        System.err.println ("Adding to classpath: "+files[i].getAbsolutePath());
	    }
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
