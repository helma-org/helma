package helma.doc;

import java.io.*;
import java.util.*;
import helma.util.SystemProperties;

/**
 *  Description of the Class
 *
 *@author     Stefan Pollach
 *@created    August 20, 2001
 */
public class DocRun {

    /**
     *  Description of the Field
     */
    public static String propfile;
    /**
     *  Description of the Field
     */
    public static SystemProperties sysProps, dbProps;
    /**
     *  Description of the Field
     */
    public static String actionExtension = ".hac";
    /**
     *  Description of the Field
     */
    public static String scriptExtension = ".js";
    /**
     *  Description of the Field
     */
    public static String templateExtension = ".hsp";
    /**
     *  Description of the Field
     */
    public static String skinExtension = ".skin";

    /**
     *  Description of the Field
     */
    public static String hopHomeDir;

    public static Hashtable options = new Hashtable();

    String appName;
    DocApplication app;


    /**
     *  Constructor for the DocRun object
     *
     *@param  appDir            Description of Parameter
     *@exception  DocException  Description of Exception
     */
    public DocRun(String appDir) throws DocException {
        File d = new File(appDir);
        if (!d.exists()) {
            throw new DocException(d.toString() + " doesn't exist");
        }
        if (!d.isDirectory()) {
            throw new DocException(d.toString() + " is not a directory");
        }
        log("parsing application " + d.getName() + " located in " + d.getAbsolutePath());
        log("writing output to " + getOption("-d", new File(hopHomeDir, "/appdocs/" + d.getName()).getAbsolutePath()));
        app = new DocApplication(d.getName(), d.getAbsolutePath());
        DocWriter.start(getOption("-d", new File(hopHomeDir, "/appdocs/" + d.getName()).getAbsolutePath()), app);
    }


    /**
     *  Gets the option attribute of the DocRun class
     *
     *@param  name  Description of Parameter
     *@return       The option value
     */
    public static String getOption(String name) {
        return getOption(name, "");
    }


    /**
     *  Gets the option attribute of the DocRun class
     *
     *@param  name  Description of Parameter
     *@param  def   Description of Parameter
     *@return       The option value
     */
    public static String getOption(String name, String def) {
        if (options.containsKey(name)) {
            return (String) options.get(name);
        } else {
            return (def);
        }
    }


    /**
     *  Description of the Method
     *
     *@param  args  Description of Parameter
     */
    public static void main(String args[]) {
        boolean usageError = false;
        // parse options from command line
        options = new Hashtable();
        StringBuffer buf = new StringBuffer();
        String name = "";
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("-")) {
                if (i > 0) {
                    if (buf.toString().length() == 0) {
                        usageError = true;
                    } else {
                        options.put(name, buf.toString());
                    }
                }
                name = args[i];
                buf = new StringBuffer();
            } else {
                buf.append(((buf.toString().length() > 0) ? " " : "") + args[i]);
            }
        }
        options.put(name, buf.toString());
        // include last option
        //	now check parameter
        if (options.containsKey("-h")) {
            hopHomeDir = (String) options.get("-h");
        } else {
            hopHomeDir = System.getProperty("user.dir");
        }
        readHopProperties(hopHomeDir);
        String parAppDir = "";
        if (options.containsKey("-a")) {
            parAppDir = (String) options.get("-a");
        } else {
            usageError = true;
        }
        if (usageError == true) {
            help();
            System.exit(0);
        }
        try {
            new DocRun(parAppDir);
        } catch (DocException e) {
            System.out.println("doc error: " + e.getMessage());
        }
    }


    /**
     *  Description of the Method
     */
    public static void help() {
        System.out.println("usage: java helma.doc.DocApplication -a appdir [-f] [-h hopdir] [-d docdir] [-i ignore]");
        System.out.println("  -a appdir  Specify source directory");
        System.out.println("  -h hopdir  Specify hop home directory");
        System.out.println("  -d docdir  Specify destination directory");
        System.out.println("  -f true    Link functions to source code");
        System.out.println("  -i ignore  Specify prototypes to ignore (like: \"-i CVS mistsack\")");
        System.out.println("  -debug");
        System.out.println("\n");
    }


    /**
     *  Description of the Method
     *
     *@param  name  Description of Parameter
     *@return       Description of the Returned Value
     */
    public static boolean prototypeAllowed(String name) {
        String ig = " " + getOption("-i").toLowerCase() + " ";
        if (ig.equals("")) {
            return true;
        }
        name = name.toLowerCase();
        if (ig.indexOf(" " + name + " ") > -1) {
            return false;
        } else {
            return true;
        }
    }


    /**
     *  reads server.properties, apps.properties and db.properties from
     *  hop-home-directory TBD: should be cleaned up to work exactly like the
     *  helma server
     *
     *@param  hopHomeDir  Description of Parameter
     */
    public static void readHopProperties(String hopHomeDir) {
        propfile = new File(hopHomeDir, "server.properties").getAbsolutePath();
        sysProps = new SystemProperties(propfile);
        dbProps = new SystemProperties(new File(hopHomeDir, "db.properties").getAbsolutePath());
        actionExtension = sysProps.getProperty("actionExtension", ".hac");
        scriptExtension = sysProps.getProperty("scriptExtension", ".js");
        templateExtension = sysProps.getProperty("templateExtension", ".hsp");
    }


    /**
     *  Description of the Method
     *
     *@param  msg  Description of Parameter
     */
    public static void debug(String msg) {
        if (options.containsKey("-debug")) {
            System.out.println(msg);
        }
    }


    /**
     *  Description of the Method
     *
     *@param  msg  Description of Parameter
     */
    public static void log(String msg) {
        System.out.println(msg);
    }

}


