package helma.doc;

import helma.framework.IPathElement;
import java.io.*;
import java.util.*;

public class DocSkin extends DocFileElement	{

	/**
	  * creates a new independent DocSkin object
	  */
	public static DocSkin newInstance (File location) {
		return newInstance (location, null);
	}

	/**
	  * creates a new DocSkin object connected to another DocElement
	  */
	public static DocSkin newInstance (File location, DocElement parent) {
		String skinname = nameFromFile (location, ".skin");
		DocSkin skin = new DocSkin (skinname, location, parent);
		return skin;
	}

	protected DocSkin (String name, File location, DocElement parent) {
		super (name, location, SKIN);
		this.parent = parent;
		content = readFile (location);
		parseHandlers ();
	}


	/**
	  * parses the source code of the skin and
	  * extracts all included macros. code taken
	  * from helma.framework.core.Skin
	  * @see helma.framework.core.Skin
	  */
	private void parseHandlers () {
		ArrayList partBuffer = new ArrayList ();
		char[] source = content.toCharArray ();
		int sourceLength = source.length;
		int start = 0;
		for (int i = 0; i < sourceLength-1; i++) {
			if (source[i] == '<' && source[i+1] == '%') {
				// found macro start tag
				int j = i+2;
				// search macro end tag
				while (j < sourceLength-1 && (source[j] != '%' || source[j+1] != '>')) {
					j++;
				}
				if (j > i+2) {
					String str = (new String (source, i+2, j-i)).trim ();
					str = str.substring (0, str.indexOf(" "));
					if (str.indexOf(".")>-1 && 
						(str.startsWith ("param.")
						 || str.startsWith ("response.")
						 || str.startsWith("request.")
						 || str.startsWith ("session.")
						) && !partBuffer.contains(str)) {
						partBuffer.add (str);
					}
					start = j+2;
				}
				i = j+1;
			}
		}
		String[] strArr = (String[]) partBuffer.toArray (new String [0]);
		Arrays.sort (strArr);
		for (int i=0; i<strArr.length; i++) {
			addParameter (strArr[i]);
		}
	}

	/**
	  * from helma.framework.IPathElement. Use the same prototype as functions etc.
	  */
	public java.lang.String getPrototype ()	{
		return "docfunction";
	}


}

