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

package helma.framework.repository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Repository implementation for directories providing file resources
 */
public class FileRepository extends AbstractRepository {

    // Directory serving sub-repositories and file resources
    private File dir;

    private long lastModified = -1;
    private long lastChecksum = 0;
    private long lastChecksumTime = 0;

    /**
     * Defines how long the checksum of the repository will be cached
     */
    private final long cacheTime = 1000L;

    /**
     * Constructs a FileRepository using the given argument
     * @param initArgs absolute path to the directory
     */
    public FileRepository(String initArgs) {
        this(new File(initArgs), null);
    }

    /**
     * Constructs a FileRepository using the given directory as top-level
     * repository
     * @param dir directory
     */
    public FileRepository(File dir) {
        this(dir, null);
    }

    /**
     * Constructs a FileRepository using the given directory and top-level
     * repository
     * @param dir directory
     * @param parent top-level repository
     */
    private FileRepository(File dir, FileRepository parent) {
        this.dir = dir;
        if (!dir.exists()) {
            create();
        }

        if (parent == null) {
            name = shortName = dir.getAbsolutePath();
        } else {
            this.parent = parent;
            shortName = dir.getName();
            name = parent.getName() + "/" + shortName;
        }
    }

    public boolean exists() {
        if (dir.exists() && dir.isDirectory()) {
            return true;
        } else {
            return false;
        }
    }

    public void create() {
        if (!dir.exists() || !dir.isDirectory()) {
            dir.mkdirs();
        }
        return;
    }

    /**
     * Checks wether the repository is to be considered a top-level
     * repository from a scripting point of view. For example, a zip
     * file within a file repository is not a root repository from
     * a physical point of view, but from the scripting point of view it is.
     *
     * @return true if the repository is to be considered a top-level script repository
     */
    public boolean isScriptRoot() {
        return parent == null;
    }

    public long lastModified() {
        return dir.lastModified();
    }

    public long getChecksum() throws IOException {
        // delay checksum check if already checked recently
        if (System.currentTimeMillis() > lastChecksumTime + cacheTime) {

            update();
            long checksum = lastModified;

            for (int i = 0; i < repositories.length; i++) {
                checksum += repositories[i].getChecksum();
            }

            lastChecksum = checksum;
            lastChecksumTime = System.currentTimeMillis();
        }

        return lastChecksum;
    }

    /**
     * Updates the content cache of the repository
     * Gets called from within all methods returning sub-repositories or
     * resources
     */
    public synchronized void update() {
        if (!dir.exists()) {
            repositories = new Repository[0];
            resources.clear();
            return;
        }

        if (dir.lastModified() != lastModified) {
            lastModified = dir.lastModified();

            File[] list = dir.listFiles();

            ArrayList newRepositories = new ArrayList(list.length);
            HashMap newResources = new HashMap(list.length);

            for (int i = 0; i < list.length; i++) {
                if (list[i].isDirectory()) {
                    // a nested directory aka child file repository
                    newRepositories.add(new FileRepository(list[i], this));
                } else if (list[i].getName().endsWith(".zip")) {
                    // a nested zip repository
                    newRepositories.add(new ZipRepository(list[i], this));
                } else if (list[i].isFile()) {
                    // a file resource
                    FileResource resource = new FileResource(list[i], this);
                    newResources.put(resource.getName(), resource);
                }
            }

            repositories = (Repository[])
                    newRepositories.toArray(new Repository[newRepositories.size()]);
            resources = newResources;
        }
    }

    public int hashCode() {
        return 17 + (37 * dir.hashCode());
    }

    public boolean equals(Object obj) {
        return obj instanceof FileRepository &&
               dir.equals(((FileRepository) obj).dir);
    }

    public String toString() {
        return new StringBuffer("FileRepository[").append(name).append("]").toString();
    }
}
