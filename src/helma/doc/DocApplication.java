package helma.doc;

import helma.framework.IPathElement;
import helma.main.Server;
import java.io.*;
import java.util.*;

public class DocApplication extends DocDirElement	{

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
	}

	public DocApplication (String name, String appDir) throws DocException	{
		super (name, new File (appDir), APPLICATION);
	}


	/**
	  * reads all prototypes and files of the application
	  */
	public void readApplication () {
		String arr[] = location.list ();
		children.clear ();
		for (int i=0; i<arr.length; i++) {
			if (Util.isExcluded (arr[i]))
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




