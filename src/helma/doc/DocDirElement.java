/*
 * Helma License Notice
 *
 * The contents of this file are subject to the Helma License
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://adele.helma.org/download/helma/license.txt
 *
 * Copyright 1998-2003 Helma Software. All Rights Reserved.
 *
 * $RCSfile$
 * $Author$
 * $Revision$
 * $Date$
 */

package helma.doc;

import java.io.*;

/**
 * 
 */
public abstract class DocDirElement extends DocElement {
    // a default file that is read as comment for applications 
    // and prototypes if found in their directories
    public static final String[] DOCFILES = {
                                                "doc.html", "doc.htm", "app.html",
                                                "app.htm", "prototype.html",
                                                "prototype.htm", "index.html", "index.htm"
                                            };

    protected DocDirElement(String name, File location, int type) {
        super(name, location, type);
        checkCommentFiles();
    }

    private void checkCommentFiles() throws DocException {
        for (int i = 0; i < DOCFILES.length; i++) {
            File f = new File(location, DOCFILES[i]);

            if (f.exists()) {
                String rawComment = Util.readFile(f);

                parseComment(rawComment);

                return;
            }
        }
    }
}
