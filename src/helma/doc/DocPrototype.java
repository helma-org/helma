package helma.doc;

import helma.framework.IPathElement;
import java.io.*;
import java.util.*;
import FESI.Parser.*;


public class DocPrototype extends DocDirElement	{

	private DocProperties typeProperties	= null;
	private DocPrototype  parentPrototype	= null;

	/**
	  * creates a prototype that is independent of an 
	  * application object
	  * @param homedirectory
	  */
	public static DocPrototype newInstance (File location) {
		return newInstance (location, null);
	}

	/**
	  * creates a prototype that is connected to an
	  * application object and resides in app's home dir.
	  * @param application
	  * @param name
	  */
	public static DocPrototype newInstance (File location, DocElement parent) {
		DocPrototype pt = new DocPrototype (location.getName (), location, parent);
		return pt;
	}

	private DocPrototype (String name, File location, DocElement parent) {
		super (name, location, PROTOTYPE);
		this.parent = parent;
		typeProperties = DocProperties.newInstance (new File (location, "type.properties"));
	}


	/**
	  * checks the type.properites for _extend values and connected a possible
	  * parent prototype with this prototype. this can't be successfull at construction
	  * time but only -after- all prototypes are parsed and attached to a parent
	  * DocApplication object.
	  */
	public void checkInheritance () {
		// hopobject is the top prototype:
		if (name.equals("hopobject"))
			return;
		if (typeProperties!=null) {
			// check for "_extends" in the the type.properties
			String ext = typeProperties.getProperties ().getProperty ("_extends");
			if (ext!=null && parent!=null) {
				// try to get the prototype if available
				parentPrototype = (DocPrototype) parent.getChildElement ("prototype_" + ext);
			}
		}
		if (parentPrototype==null && parent!=null) {
			// if no _extend was set, get the hopobject prototype
			parentPrototype = (DocPrototype) parent.getChildElement ("prototype_hopobject");
		}
	}

	public DocPrototype getParentPrototype () {
		return parentPrototype;
	}


	public DocProperties getTypeProperties () {
		return typeProperties;
	}


	/**
	  * runs through the prototype directory and parses all helma files
	  */
	public void readFiles () {
		children.clear ();
		String arr[] = location.list ();
		for (int i=0; i<arr.length; i++) {
			if (Util.isExcluded (arr[i]))
				continue;
			File f = new File (location.getAbsolutePath (), arr[i]);
			if (f.isDirectory ())
				continue;
			if (arr[i].endsWith (".skin")) {
				addChild (DocSkin.newInstance (f, this));
			} else if (arr[i].endsWith (".properties")) {
				continue;
			} else if (arr[i].endsWith (".hac")) {
				addChild (DocFunction.newAction (f, this));
			} else if (arr[i].endsWith (".hsp")) {
				addChild (DocFunction.newTemplate (f, this));
			} else if (arr[i].endsWith (".js")) {
				DocElement[] elements = DocFunction.newFunctions (f, this);
				for (int j=0; j<elements.length; j++) {
					addChild (elements[j]);
				}
			}
		}
	}

}

