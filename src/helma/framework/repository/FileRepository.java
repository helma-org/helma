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

import helma.framework.repository.ZipRepository;
import helma.framework.repository.Repository;
import helma.framework.repository.FileResource;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
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
    private final long cacheTime = 1500L;

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

    /**
     * Checks wether the repository needs to be updated
     * @return true if the repository needs to be updated
     */
    private boolean needsUpdate() {
        return dir.lastModified() != lastModified;
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

    public long lastModified() {
        return dir.lastModified();
    }

    public long getChecksum() {

        // delay checksum check if already checked recently
        if (System.currentTimeMillis() > lastChecksumTime + cacheTime) {

            long checksum;
            if (dir.lastModified() != lastModified) {
                update();
                checksum = lastModified;
            } else {
                checksum = lastChecksum;
            }

            for (int i = 0; i < repositories.length; i++) {
                checksum += repositories[i].getChecksum();
            }

            lastChecksum = checksum;
            lastChecksumTime = System.currentTimeMillis();

            return checksum;
        } else {
            return lastChecksum;
        }
    }

    /**
     * Updates the content cache of the repository
     * Gets called from within all methods returning sub-repositories or
     * resources
     */
    public synchronized void update() {
        if (needsUpdate()) {
            lastModified = dir.lastModified();
            String[] list = dir.list();
            ArrayList newRepositories = new ArrayList(list.length);
            if (resources == null) {
                resources = new HashMap();
            } else {
                resources.clear();
            }

            for (int i = 0; i < list.length; i++) {
                File file = new File(dir, list[i]);
                if (file.isDirectory()) {
                    /* the content of a file repository either is another
                     file repository or */
                    newRepositories.add(new FileRepository(file, this));
                } else if (list[i].endsWith(".zip")) {
                    /* although zip files should / could be used as top-level
                     repositories, backwards compatibility is provided here */
                    newRepositories.add(new ZipRepository(file));
                } else if (!list[i].equals(".") && !list[i].equals("..")) {
                    // a file resource
                    FileResource resource = new FileResource(file, this);
                    resources.put(resource.getName(), resource);
                }
            }

            repositories = (Repository[]) newRepositories.toArray(new Repository[newRepositories.size()]);
        }

        return;
    }

    public String toString() {
        return "FileRepository["+name+"]";
    }
}
