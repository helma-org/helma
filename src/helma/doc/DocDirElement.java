package helma.doc;

import java.io.*;
import java.util.*;

import FESI.Parser.*;

public abstract class DocDirElement extends DocElement {

	// a default file that is read as comment for applications 
	// and prototypes if found in their directories
	public static final String[] DOCFILES = {
		"doc.html", "doc.htm",
		"app.html", "app.htm",
		"prototype.html", "prototype.htm",
		"index.html", "index.htm"
	};

	protected DocDirElement (String name, File location, int type) {
		super (name, location, type);
		checkCommentFiles ();
	}

	private void checkCommentFiles () throws DocException {
		for (int i=0; i<DOCFILES.length; i++) {
			File f = new File (location, DOCFILES[i]);
			if (f.exists ()) {
				String rawComment = readFile (f);
				parseComment (rawComment);
				return;
			}
		}
	}

}

