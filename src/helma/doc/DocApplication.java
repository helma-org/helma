package helma.doc;

import helma.framework.IPathElement;
import helma.main.Server;
import helma.util.SystemProperties;
import java.io.*;
import java.util.*;

public class DocApplication extends DocDirElement	{

	HashSet excluded;

   public static void main (String args[]) {
//		DocApplication app;
//		app = new DocApplication (args[0], args[1]);
//		app.readApplication ();

//		DocPrototype el = DocPrototype.newInstance (new File(args[0]));
//		el.readFiles ();

//		DocFunction func = DocFunction.newTemplate (new File(args[0]));
//		DocFunction func = DocFunction.newAction (new File(args[0]));

		DocFunction[] func = DocFunction.newFunctions (new File (args[0]));

//		DocSkin skin = DocSkin.newInstance (new File (args[0]));
//		System.out.println (func.getContent ());
//		System.out.println ("\n\n\ncomment = " + func.getComment ());
   }


	public DocApplication (String name, File location) throws DocException	{
		super (name, location, APPLICATION);
		readProps ();
	}

	public DocApplication (String name, String appDir) throws DocException	{
		super (name, new File (appDir), APPLICATION);
		readProps ();
	}

	/**
	  * reads the app.properties file and parses for helma.excludeDocs
	  */
	private void readProps () {
		File propsFile = new File (location, "app.properties");
		SystemProperties serverProps = Server.getServer ().getProperties ();
		SystemProperties appProps = new SystemProperties(propsFile.getAbsolutePath (), serverProps);

		excluded = new HashSet ();
		addExclude ("cvs");
		addExclude (".docs");
		String excludeProps = appProps.getProperty ("helma.excludeDocs");
		if (excludeProps != null) {
			StringTokenizer tok = new StringTokenizer (excludeProps, ",");
			while (tok.hasMoreTokens ()) {
				String str = tok.nextToken ().trim ();
				addExclude (str);
			}
		}
	}

	public void addExclude (String str) {
		excluded.add (str.toLowerCase ());
	}

	public boolean isExcluded (String str) {
		return (excluded.contains (str.toLowerCase ()));
	}


	/**
	  * reads all prototypes and files of the application
	  */
	public void readApplication () {
      readProps ();
		String arr[] = location.list ();
		children.clear ();
		for (int i=0; i<arr.length; i++) {
			if (isExcluded (arr[i]))
				continue;
			File f = new File (location.getAbsolutePath (), arr[i]);
			if (!f.isDirectory ())
				continue;
			try {
				DocPrototype pt = DocPrototype.newInstance (f, this);
				addChild (pt);
				pt.readFiles ();
			} catch (DocException e) {
				debug ("Couldn't read prototype " + arr[i] + ": " + e.getMessage ());
			}				
			System.out.println (f);
		}
		for (Iterator i=children.values ().iterator (); i.hasNext ();) {
			((DocPrototype) i.next ()).checkInheritance ();
		}
	}

	public DocElement[] listFunctions ()	{
		Vector allFunctions = new Vector ();
		for (Iterator i = children.values ().iterator (); i.hasNext ();) {
			DocElement proto = (DocElement) i.next ();
			allFunctions.addAll (proto.children.values ());
		}
		Collections.sort (allFunctions, new DocComparator (DocComparator.BY_NAME, this));
		return (DocElement[]) allFunctions.toArray (new DocElement[allFunctions.size ()]);
	}


	/**
	  * from helma.framework.IPathElement, overridden with "api"
	  * to work in manage-application
	  */
	public String getElementName()	{
		return "api";
	}


	/**
	  * from helma.framework.IPathElement, overridden with
	  * Server.getServer() to work in manage-application
	  */
	public IPathElement getParentElement()	{
		Server s = helma.main.Server.getServer();
		return s.getChildElement(this.name);
	}


}




