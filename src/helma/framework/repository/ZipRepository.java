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

import helma.framework.repository.ZipResource;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class ZipRepository extends AbstractRepository {

    // zip file serving sub-repositories and zip file resources
    private File zipfile;

    /* zip entry within a zip file serving content if the repository is not the
     top-level repository */
    private ZipEntry zipentry;

    private long lastModified = -1;

    /**
     * Constructs a ZipRespository using the given argument
     * @param initArgs absolute path to the zip file
     */
    public ZipRepository(String initArgs) {
        this(new File(initArgs), null, null);
    }

    /**
     * Constructs a ZipRepository using the given zip file as top-level
     * repository
     * @param zipfile zip file
     */
    protected ZipRepository(File zipfile) {
        this(zipfile, null, null);
    }

    /**
     * Constructs a ZipRepository using the zip entry belonging to the given
     * zip file and top-level repository
     * @param zipfile zip file
     * @param zipentry zip entry
     * @param rootRepository top-level repository
     */
    private ZipRepository(File zipfile, ZipEntry zipentry, Repository rootRepository) {
        this.zipfile = zipfile;

        if (rootRepository == null) {
            name = shortName = zipfile.getName();
            notRoot = false;
        } else {
            this.rootRepository = rootRepository;
            this.zipentry = zipentry;
            String entryname = zipentry.getName();
            shortName = entryname.lastIndexOf("/") == entryname.indexOf("/") ? entryname.substring(0, entryname.length() - 1) : entryname.substring(0, entryname.length() - 1).substring(entryname.substring(0, entryname.length() - 1).lastIndexOf("/") + 1, entryname.substring(0, entryname.length() - 1).length());
            name = rootRepository.getRootRepository().getName() + "/" + entryname.substring(0, entryname.length() - 1);
        }
    }

    public synchronized void update() {
        if (needsUpdate()) {
            lastModified = zipfile.lastModified();
            ZipFile zip = null;
            try {
                zip = new ZipFile(zipfile);
                Enumeration enum = zip.entries();
                ArrayList newRepositories = new ArrayList(zip.size());
                if (resources == null) {
                    resources = new HashMap();
                } else {
                    resources.clear();
                }

                for (int i = 0; enum.hasMoreElements(); i++) {
                    ZipEntry entry = (ZipEntry) enum.nextElement();
                    String entryname = entry.getName();
                    /* ZipFile provide its entries in a matter that some checks
                     are needed to find out if the given entry is a direct or
                     indirect sub-repository */
                    if (notRoot == false) {
                        if (entry.isDirectory() && entryname.indexOf("/") == entryname.lastIndexOf("/")) {
                            newRepositories.add(new ZipRepository(zipfile, entry, this));
                        } else if (entryname.indexOf("/") == -1) {
                            ZipResource resource = new ZipResource(zipfile, entry, this);
                            resources.put(resource.getName(), resource);
                        }
                    } else {
                        if (entry.isDirectory() && !entryname.equals(zipentry.getName()) && entryname.startsWith(zipentry.getName()) && entryname.substring(0, zipentry.getName().length()).indexOf("/") == entryname.substring(0, zipentry.getName().length()).lastIndexOf("/")) {
                            newRepositories.add(new ZipRepository(zipfile, entry, this));
                        } else if (!entry.isDirectory() && entryname.startsWith(zipentry.getName()) && entryname.substring(zipentry.getName().length(), entryname.length()).indexOf("/") ==  -1) {
                            ZipResource resource = new ZipResource(zipfile, entry, this);
                            resources.put(resource.getName(), resource);
                        }
                    }
                }

                repositories = (Repository[]) newRepositories.toArray(new Repository[newRepositories.size()]);

                return;

            } catch (IOException ex) {
                repositories = new Repository[0];
                if (resources == null) {
                    resources = new HashMap();
                } else {
                    resources.clear();
                }

            } finally {
                try {
                    // unlocks the zip file in the underlying filesystem
                    zip.close();
                } catch (IOException ex) {}
            }
        }
    }

    /**
     * Checks wether the repository needs to be updated
     * @return true if the repository needs to be updated
     */
    private boolean needsUpdate() {
        return zipfile.lastModified() != lastModified;
    }

    public long getChecksum() {
        return zipfile.lastModified();
    }

   public boolean exists() {
        ZipFile zip = null;
        try {
            /* a ZipFile needs to be created to see if the zip file actually
             exists; this is not cached to provide blocking the zip file in
             the underlying filesystem */
            zip = new ZipFile(zipfile);
            return true;
        } catch (IOException ex) {
            return false;
        }
        finally {
            try {
                // unlocks the zip file in the underlying filesystem
                zip.close();
            } catch (Exception ex) {
                return false;
            }
        }
    }

    public void create() {
        // we do not create zip files as it makes no sense
        throw new java.lang.UnsupportedOperationException("ZipSource: zip files can't be created!");
    }

    public long lastModified() {
        return zipfile.lastModified();
    }

    public String toString() {
        return "ZipRepository["+name+"]";
    }

}
