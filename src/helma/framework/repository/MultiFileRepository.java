/*
 * Helma License Notice
 *
 * The contents of this file are subject to the Helma License
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://adele.helma.org/download/helma/license.txt
 *
 * Copyright 2005 Helma Software. All Rights Reserved.
 *
 * $RCSfile$
 * $Author$
 * $Revision$
 * $Date$
 */

package helma.framework.repository;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Repository implementation that provides all of its subdirectories
 * as top-level FileRepositories
 *
 * @author Barbara Ondrisek
 */
public class MultiFileRepository extends FileRepository {

    /**
     * Constructs a MultiFileRepository using the given argument
     * @param initArgs absolute path to the directory
     */
    public MultiFileRepository(String initArgs) {
        this(new File(initArgs));
    }

    /**
     * Constructs a MultiFileRepository using the given directory as top-level
     * repository
     * @param dir directory
     */
    public MultiFileRepository(File dir) {
        super(dir, null);
    }

    /**
     * Updates the content cache of the repository. We override this
     * to create child repositories that act as top-level script repositories
     * rather than prototype repositories. Zip files are handled as top-level
     * script repositories like in FileRepository, while resources are ignored.
     */
    public synchronized void update() {
        if (!directory.exists()) {
            repositories = new Repository[0];
            if (resources != null)
                resources.clear();
            lastModified = 0;
            return;
        }

        if (directory.lastModified() != lastModified) {
            lastModified = directory.lastModified();

            File[] list = directory.listFiles();

            ArrayList newRepositories = new ArrayList(list.length);
            HashMap newResources = new HashMap(list.length);

            for (int i = 0; i < list.length; i++) {
                // create both directories and zip files as top-level repositories,
                // while resources (files) are ignored.
                if (list[i].isDirectory()) {
                    // a nested directory aka child file repository
                    newRepositories.add(new FileRepository(list[i], null));
                } else if (list[i].getName().endsWith(".zip")) {
                    // a nested zip repository
                    newRepositories.add(new ZipRepository(list[i], this));
                }
            }

            repositories = (Repository[])
                    newRepositories.toArray(new Repository[newRepositories.size()]);
            resources = newResources;
        }
    }

    /**
     * get hashcode
     * @return int
     */
    public int hashCode() {
        return 37 + (37 * directory.hashCode());
    }

    /**
     * equals object
     * @param obj Object
     * @return boolean
     */
    public boolean equals(Object obj) {
        return obj instanceof MultiFileRepository &&
               directory.equals(((MultiFileRepository) obj).directory);
    }

    /**
     * get object serialized as string
     * @return String
     */
    public String toString() {
        return new StringBuffer("MultiFileRepository[").append(name).append("]").toString();
    }
}
