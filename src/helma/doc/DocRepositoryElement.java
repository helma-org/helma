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

import helma.framework.repository.Repository;
import helma.framework.repository.Resource;

import java.io.*;

/**
 * 
 */
public abstract class DocRepositoryElement extends DocElement {

    protected Repository repos;

    // a default file that is read as comment for applications 
    // and prototypes if found in their directories
    public static final String[] DOCFILES = {
                                                "doc.html", "doc.htm", "app.html",
                                                "app.htm", "prototype.html",
                                                "prototype.htm", "index.html", "index.htm"
                                            };

    protected DocRepositoryElement(String name, Repository repos, int type) throws IOException {
        super(name, type);
        this.repos = repos;
        checkCommentFiles();
    }

    /**
     * Get a string describing this element's location
     *
     * @return lstring representation of the element's repository
     */
    public String toString() {
        return repos.getName();
    }

    /**
     * @return absolute path to location of element
     * (directory for apps and prototypes, file for
     * methods and properties files)
     */
    public Repository getRepository() {
        return repos;
    }


    private void checkCommentFiles() throws DocException, IOException {
        if (repos == null) {
            return;
        }
        for (int i = 0; i < DOCFILES.length; i++) {
            Resource res = repos.getResource(DOCFILES[i]);

            if (res.exists()) {
                String rawComment = res.getContent();

                parseComment(rawComment);

                return;
            }
        }
    }
}
