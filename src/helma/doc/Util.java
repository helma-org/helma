package helma.doc;

import java.io.*;
import java.util.*;

public final class Util {
	
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
