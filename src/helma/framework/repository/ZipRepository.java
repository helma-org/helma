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

import helma.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class ZipRepository extends AbstractRepository {

    // zip file serving sub-repositories and zip file resources
    private File file;

    // the nested directory depth of this repository
    private int depth;

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
     * @param file a zip file
     */
    protected ZipRepository(File file, Repository parent) {
        this(file, parent, null);
    }

    /**
     * Constructs a ZipRepository using the zip entry belonging to the given
     * zip file and top-level repository
     * @param file a zip file
     * @param zipentry zip entry
     * @param parent repository
     */
    private ZipRepository(File file, Repository parent, ZipEntry zipentry) {
        this.file = file;
        this.parent = parent;

        if (zipentry == null) {
            name = shortName = file.getName();
            depth = 0;
        } else {
            String[] entrypath = StringUtils.split(zipentry.getName(), "/");
            depth = entrypath.length;
            shortName = entrypath[depth - 1];
            name = new StringBuffer(parent.getName())
                                   .append('/').append(shortName).toString();
        }
    }

    /**
     * Returns a java.util.zip.ZipFile for this repository. It is the caller's
     * responsability to call close() in it when it is no longer needed.
     * @return a ZipFile for reading
     * @throws IOException
     */
    protected ZipFile getZipFile() throws IOException {
        return new ZipFile(file);
    }

    public synchronized void update() {
        if (file.lastModified() != lastModified) {
            lastModified = file.lastModified();
            ZipFile zipfile = null;

            try {
                zipfile = getZipFile();
                Enumeration en = zipfile.entries();
                ArrayList newRepositories = new ArrayList();
                HashMap newResources = new HashMap();

                while (en.hasMoreElements()) {
                    ZipEntry entry = (ZipEntry) en.nextElement();
                    String entryname = entry.getName();
                    String[] entrypath = StringUtils.split(entryname, "/");

                    // create new repositories and resources for all entries with a
                    // path depth of this.depth + 1
                    if (entrypath.length == depth + 1) {
                        if (entry.isDirectory()) {
                            newRepositories.add(new ZipRepository(file, this, entry));
                        } else {
                            ZipResource resource = new ZipResource(file, entry, this);
                            newResources.put(resource.getName(), resource);
                        }
                    }
                }

                repositories = (Repository[])
                        newRepositories.toArray(new Repository[newRepositories.size()]);
                resources = newResources;

            } catch (IOException ex) {
                ex.printStackTrace();
                repositories = new Repository[0];
                if (resources == null) {
                    resources = new HashMap();
                } else {
                    resources.clear();
                }

            } finally {
                try {
                    // unlocks the zip file in the underlying filesystem
                    zipfile.close();
                } catch (Exception ex) {}
            }
        }
    }

    public long getChecksum() {
        return file.lastModified();
    }

    public boolean exists() {
        ZipFile zipfile = null;
        try {
            /* a ZipFile needs to be created to see if the zip file actually
             exists; this is not cached to provide blocking the zip file in
             the underlying filesystem */
            zipfile = getZipFile();
            return true;
        } catch (IOException ex) {
            return false;
        }
        finally {
            try {
                // unlocks the zip file in the underlying filesystem
                zipfile.close();
            } catch (Exception ex) {
                return false;
            }
        }
    }

    public void create() {
        // we do not create zip files as it makes no sense
        throw new UnsupportedOperationException("create() not implemented for ZipRepository");
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
        return depth == 0;
    }

    public long lastModified() {
        return file.lastModified();
    }

    public int hashCode() {
        return 17 + (37 * file.hashCode()) + (37 * name.hashCode());
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof ZipRepository)) {
            return false;
        }

        ZipRepository rep = (ZipRepository) obj;
        return (file.equals(rep.file) && name.equals(rep.name));
    }

    public String toString() {
        return new StringBuffer("ZipRepository[").append(name).append("]").toString();
    }

}
