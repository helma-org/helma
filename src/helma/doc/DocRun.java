package helma.doc;

import java.io.*;
import java.util.*;
import helma.objectmodel.*;

public class DocRun	{

	public static String propfile;
	public static SystemProperties sysProps, dbProps;
	public static String actionExtension;
	public static String scriptExtension;
	public static String templateExtension;
	public static String skinExtension = ".skin";

	public static String hopHomeDir;

	private static Hashtable options;

	String appName;
	DocApplication app;

	public static void main ( String args[] )	{
		boolean usageError = false;
		// parse options from command line
		options = new Hashtable();
		StringBuffer buf = new StringBuffer();
		String name = "";
		for ( int i=0; i<args.length; i++ )	{
			if ( args[i].startsWith("-") )	{
				if ( i>0 )	{
					if ( buf.toString().length()==0 )
						usageError = true;
					else
						options.put(name,buf.toString() );
				}
				name = args[i];
				buf = new StringBuffer();
			}	else	{
				buf.append( ( (buf.toString().length()>0) ? " ":"" ) + args[i]);
			}
		}
		options.put(name,buf.toString());	// include last option
		//	now check parameter
		if ( options.containsKey("-h") )	{
			hopHomeDir = (String)options.get("-h");
		}	else	{
			hopHomeDir = System.getProperty("user.dir");
		}
		readHopProperties(hopHomeDir);
		String parAppDir = "";
		if ( options.containsKey("-a") )	{
			parAppDir = (String)options.get("-a");
		}	else	{
			usageError = true;
		}
		if ( usageError==true )	{
			help();
			System.exit(0);
		}
		try	{
			new DocRun(parAppDir);
		}	catch ( DocException e )	{
			System.out.println("doc error: " + e.getMessage());
		}
	}

	public static void help()	{
		System.out.println ("usage: java helma.doc.DocApplication -a appdir [-f] [-h hopdir] [-d docdir] [-i ignore]");
    	System.out.println ("  -a appdir  Specify source directory");
    	System.out.println ("  -h hopdir  Specify hop home directory");
    	System.out.println ("  -d docdir  Specify destination directory");
    	System.out.println ("  -f true    Link functions to source code");
    	System.out.println ("  -i ignore  Specify prototypes to ignore (like: \"-i CVS mistsack\")");
    	System.out.println ("  -debug");
    	System.out.println ("\n");
	}

	public static boolean prototypeAllowed(String name)	{
		String ig = " " + getOption("-i").toLowerCase() +  " ";
		if ( ig.equals("") )	return true;
		name = name.toLowerCase();
		if ( ig.indexOf(" "+name+" ")>-1 )
			return false;
		else
			return true;
	}

	/** 
	  * reads server.properties, apps.properties and db.properties from hop-home-directory
	  * TBD: should be cleaned up to work exactly like the helma server
	  * @param homeDir hop-home-directory with server.properties-file
	  */
	public static void readHopProperties(String hopHomeDir)	{
		propfile = new File (hopHomeDir, "server.properties").getAbsolutePath();
		sysProps = new SystemProperties (propfile);
		dbProps  = new SystemProperties ( new File(hopHomeDir,"db.properties").getAbsolutePath() );
		actionExtension = sysProps.getProperty("actionExtension",".hac");
		scriptExtension = sysProps.getProperty("scriptExtension",".js");
		templateExtension = sysProps.getProperty("templateExtension",".hsp");
	}

	public DocRun (String appDir) throws DocException	{
		File d = new File(appDir);
		if ( !d.exists() )
			throw new DocException( d.toString()  + " doesn't exist");
		if ( !d.isDirectory() )
			throw new DocException( d.toString()  + " is not a directory");
		log ( "parsing application " + d.getName() + " located in " + d.getAbsolutePath() );
		log ( "writing output to " + getOption("-d", new File(hopHomeDir,"/appdocs/"+d.getName()).getAbsolutePath()) );
		app = new DocApplication(d.getName(),d.getAbsolutePath() );
		DocWriter.start( getOption("-d", new File(hopHomeDir,"/appdocs/"+d.getName()).getAbsolutePath()), app);
	}

	public static String getOption(String name)	{
		return getOption(name,"");
	}

	public static String getOption(String name, String def)	{
		if ( options.containsKey(name) )
			return(String)options.get(name);
		else
			return(def);
	}

	public static void debug(String msg)	{
		if ( options.containsKey("-debug") )
			System.out.println(msg);
	}

	public static void log( String msg )	{
		System.out.println(msg);
	}

}



