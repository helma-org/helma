package helma.doc;

import helma.framework.IPathElement;
import helma.util.SystemProperties;
import java.io.*;
import java.util.*;

public class DocProperties extends DocFileElement	{

	Properties props = null;

	/**
	  * creates a new independent DocProperties object
	  */
	public static DocProperties newInstance (File location) {
		return newInstance (location, null);
	}

	/**
	  * creates a new DocProperties object connected to another DocElement
	  */
	public static DocProperties newInstance (File location, DocElement parent) {
		try {
			return new DocProperties (location, parent);
		} catch (DocException doc) {
			return null;
		}
	}

	protected DocProperties (File location, DocElement parent) throws DocException {
		super (location.getName (), location, PROPERTIES);
		this.parent = parent;
		content = readFile (location);
		props = new SystemProperties ();
		try {
			props.load (new FileInputStream (location));
		} catch (IOException e) {
			debug ("couldn't read file: " + e.toString ());
		} catch (Exception e) {
			throw new DocException (e.toString ());
		}
	}

	protected Properties getProperties () {
		return props;
	}

}

