package helma.doc;

import java.io.*;
import java.util.*;

import helma.framework.IPathElement;

public abstract class DocElement implements IPathElement	{

	// identifiers of this element
	String		name;
	int			type;
	File			location;
	DocElement	parent = null;
	Map			children = new HashMap ();

	// content
	String	content = "";
	String	comment  = "";
	List		tags       = new Vector ();
	List		parameters = new Vector ();

	public static final int APPLICATION	= 0;
	public static final int PROTOTYPE	= 1;
	public static final int ACTION		= 2;
	public static final int TEMPLATE		= 3;
	public static final int FUNCTION		= 4;
	public static final int MACRO			= 5;
	public static final int SKIN			= 6;
	public static final int PROPERTIES	= 7;

	// above constants are used as array indices
	public static final String[] typeNames  = {
		"Application",
		"Prototype",
		"Action",
		"Template",
		"Function",
		"Macro",
		"Skin",
		"Properties"
	};

	protected DocElement (String name, String location, int type) throws DocException	{
		this (name, new File (location), type);
	}

	protected DocElement (String name, File location, int type) throws DocException	{
		if (location.exists()==false)
			throw new DocException( name + " not found in " + location.toString ());
		this.name = name;
		this.location = location;
		this.type = type;
	}

	/**
	  * the simple name of the element
	  */
	public String getName()	{
		return name;
	}

	/**
	  * @return absolute path to location of element
	  * (directory for apps and prototypes, file for
	  * methods and properties files)
	  */
	public File getLocation()	{
		return location;
	}

	public int getType()	{
		return type;
	}

	public String getTypeName ()	{
		return typeNames [type];
	}


	/**
	  * returns the comment string, empty string if no comment is set.
	  */
	public String getComment ()	{
		return comment;
	}


	/**
	  * the actual content of the doc element (the function body, the properties
	  * list, the file list etc.
	  */
	public String getContent ()	{
		return content;
	}


	public void addTag (String rawContent) {
		if (tags==null) {
			tags = new Vector ();
		}
		try {
			DocTag tag = DocTag.parse (rawContent);
			tags.add (tag);
		} catch (DocException doc) {
			debug (doc.toString ());
		}
	}


	/**
	  * list all tags
	  */
	public DocTag[] listTags ()	{
		return (DocTag[]) tags.toArray (new DocTag[0]);
	}


	/**
	  * filter the tags according to DocTag.TYPE
	  */
	public DocTag[] listTags (int type) {
		Vector retval = new Vector ();
		for (int i=0; i<tags.size (); i++) {
			if ( ((DocTag) tags.get (i)).getType() == type)
				retval.add (tags.get (i));
		}
		return (DocTag[]) retval.toArray ();
	}


	public boolean hasParameter (String param) {
		return parameters.contains (param);
	}

	/**
	  * the list of parameters
	  */
	public String[] listParameters () {
		return (String[]) parameters.toArray (new String[0]);
	}


	/**
	  * add a string to the parameters-list
	  */
	protected void addParameter (String param) {
		parameters.add (param);
	}


	/**
	  * parse rawComment, render DocTags
	  */
	void parseComment (String rawComment)	{
	try {
		StringTokenizer tok = new StringTokenizer (rawComment, "\n", true);
		int BLANK = 0;
		int TEXT  = 1;
		int TAGS  = 2;
		boolean lastEmpty = false;
		int mode = BLANK;
		StringBuffer buf = new StringBuffer ();
		while (tok.hasMoreTokens ()) {
			String line = Util.chopDelimiters (tok.nextToken ().trim ());
			if ("".equals(line)) {
				// if we've already had text, store that this line was empty
				lastEmpty = (mode!=BLANK) ? true : false;
				continue;
			}
			// if we come here the first time, start with TEXT mode
			mode = (mode==BLANK) ? TEXT : mode;
			// check if we have a new tag
			if (DocTag.isTagStart (line)) {
				// if we appended to comment text until now, store that ...
				if (mode==TEXT)
					comment = buf.toString ();
				// if we appended to a tag, store that ...
				if (mode==TAGS)
					addTag (buf.toString ());
				// reset buffer
				buf = new StringBuffer ();
				mode = TAGS;
			}
			// append to current buffer
			if (lastEmpty==true)
				buf.append ("\n");
			buf.append (line);
			buf.append (" ");
			lastEmpty = false;
		}
		// store the last element, if there was at least one element ...
		if (mode==TEXT)
			comment = buf.toString ();
		else if (mode==TAGS)
			addTag (buf.toString ());
	}  catch (RuntimeException rt) {
	   debug ("parse error in " + location + ": " + rt.getMessage());
	}
	}

	/**
	  * utility: read a complete file into a string
	  */
	protected static String readFile (File file)	{
		try	{
			StringBuffer buf = new StringBuffer ();
			BufferedReader in = new BufferedReader (new FileReader (file));
 			String line = in.readLine ();
 			while(line!=null)	{
	 			buf.append (line+"\n");
	 			line = in.readLine ();
 			}
 			in.close ();
 			return buf.toString();
 		}	catch (IOException e)	{
 			return ("");
 		}
 	}


	public void setParent (DocElement parent) {
		this.parent = parent;
	}

	public void addChild (DocElement child) {
		if (child==null)
			return;
		children.put (child.getElementName (), child);
	}

	public int countChildren () {
		return children.size ();
	}

	public Map getChildren () {
		return children;
	}

	/**
	  * returns an array of doc elements, sorted by their name
	  */
	public DocElement[] listChildren () {
		String[] keys = (String[]) children.keySet ().toArray (new String[0]);
		Arrays.sort (keys);
		DocElement[] arr = new DocElement [keys.length];
		for (int i=0; i<keys.length; i++) {
			arr [i] = (DocElement) children.get (keys[i]);
		}
		return arr;
	}

	/**
	  * walks up the tree and tries to find a DocApplication object
	  */
	public DocApplication getDocApplication () {
		DocElement el = this;
		while (el != null) {
			if (el instanceof DocApplication) {
				return (DocApplication) el;
			}
			el = (DocElement) el.getParentElement ();
		}
		return null;
	}


	/**
	  * from helma.framework.IPathElement. Elements are named
	  * like this: typename_name
	  */
	public String getElementName()	{
		return typeNames[type].toLowerCase() + "_" + name;
	}


	/**
	  * from helma.framework.IPathElement. Retrieves a child from the
	  * children map.
	  */
	public IPathElement getChildElement (String name) {
		try {
			return (IPathElement) children.get (name);
		} catch (ClassCastException cce) {
			debug (cce.toString ());
			cce.printStackTrace ();
			return null;
		}
	}


	/**
	  * from helma.framework.IPathElement. Returns the parent object
	  * of this instance if assigned.
	  */
	public IPathElement getParentElement ()	{
		return parent;
	}


	/**
	  * from helma.framework.IPathElement. Prototypes are assigned like
	  * this: "doc" + typename (e.g. docapplication, docprototype etc)
	  */
	public java.lang.String getPrototype ()	{
		return "doc" + typeNames[type].toLowerCase ();
	}


	public String toString () {
		return "[" + typeNames [type] + " " + name + "]";
	}

	public static void debug (String msg) {
		System.out.println(msg);
	}

}

