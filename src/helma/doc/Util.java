package helma.doc;

import java.io.*;
import java.util.*;

public final class Util {
	
	static Vector excluded = new Vector ();
	
	public static void addExlude (String str) {
		excluded.add (str.toLowerCase ());
	}

	public static boolean isExcluded (String str) {
		if (excluded.size ()==0)
			excluded.add ("cvs");
		return (excluded.contains (str.toLowerCase ()));
	}

	public static String chopDelimiters (String line) {
		if (line==null)
			return null;
		else if (line.startsWith("/**"))
			return line.substring (3).trim ();
		else if (line.startsWith("/*"))
			return line.substring (2).trim ();
		else if (line.endsWith ("*/"))
			return line.substring (0, line.length ()-2);
		else if (line.startsWith("*"))
			return line.substring (1).trim ();
		else if (line.startsWith("//"))
			return line.substring (2).trim ();
		else
			return line;
	}


}
