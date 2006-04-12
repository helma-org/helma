/*
 * Helma License Notice
 *
 * The contents of this file are subject to the Helma License
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://adele.helma.org/download/helma/license.txt
 *
 * Copyright 1998-2006 Helma Software. All Rights Reserved.
 *
 * $RCSfile$
 * $Author$
 * $Revision$
 * $Date$
 */

package helma.framework.repository;

import java.io.IOException;
import java.io.File;
import java.util.List;
import java.util.Iterator;
import java.util.LinkedList;

public class SingleFileRepository implements Repository {

    final Resource res;
    final Repository[] repositories;
    final LinkedList resources = new LinkedList();
    final LinkedList allResources = new LinkedList();
    final boolean isScriptFile;

    /**
     * Constructs a SingleFileRepository using the given argument
     * @param initArgs absolute path to the script file
     */
    public SingleFileRepository(String initArgs) {
        this(new File(initArgs));
    }

    /**
     * Constructs a SingleFileRepository using the given argument
     * @param file the script file
     */
    public SingleFileRepository(File file) {
        res = new FileResource(file, this);
        allResources.add(res);
        isScriptFile = file.getName().endsWith(".js");
        if (isScriptFile) {
            repositories = new Repository[] { new FakeGlobal() };
        } else {
            repositories = AbstractRepository.emptyRepositories;
            resources.add(res);
        }
    }

    /**
     * Checksum of the repository and all its content. Implementations
     * should make sure
     *
     * @return checksum
     * @throws java.io.IOException
     */
    public long getChecksum() throws IOException {
        return res.lastModified();
    }

    /**
     * Returns the name of the repository.
     *
     * @return name of the repository
     */
    public String getShortName() {
        return res.getShortName();
    }

    /**
     * Returns the name of the repository; this is a full name including all
     * parent repositories.
     *
     * @return full name of the repository
     */
    public String getName() {
        return res.getName();
    }

    /**
     * Get this repository's logical script root repository.
     *
     * @return top-level repository
     * @see {isScriptRoot()}
     */
    public Repository getRootRepository() {
        return this;
    }

    /**
     * Returns this repository's parent repository.
     * Returns null if this repository already is the top-level repository
     *
     * @return the parent repository
     */
    public Repository getParentRepository() {
        return null;
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
        return false;
    }

    /**
     * Creates the repository if does not exist yet
     *
     * @throws java.io.IOException
     */
    public void create() throws IOException {
        // noop
    }

    /**
     * Checks wether the repository actually (or still) exists
     *
     * @return true if the repository exists
     * @throws java.io.IOException
     */
    public boolean exists() throws IOException {
        return res.exists();
    }

    /**
     * Returns this repository's direct child repositories
     *
     * @return direct repositories
     * @throws java.io.IOException
     */
    public Repository[] getRepositories() throws IOException {
        return repositories;
    }

    /**
     * Returns all direct and indirect resources
     *
     * @return resources recursive
     * @throws java.io.IOException
     */
    public List getAllResources() throws IOException {
        return resources;
    }

    /**
     * Returns all direct resources
     *
     * @return direct resources
     * @throws java.io.IOException
     */
    public Iterator getResources() throws IOException {
        return resources.iterator();
    }

    /**
     * Returns a specific direct resource of the repository
     *
     * @param resourceName name of the child resource to return
     * @return specified child resource
     */
    public Resource getResource(String resourceName) {
        if (!isScriptFile && res.getName().equals(resourceName)) {
            return res;
        }
        return null;
    }

    /**
     * Returns the date the repository was last modified.
     *
     * @return last modified date
     * @throws java.io.IOException
     */
    public long lastModified() throws IOException {
        return res.lastModified();
    }

    /**
     * Return our single resource.
     * @return the wrapped resource
     */
    protected Resource getResource() {
        return res;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    public boolean equals(Object obj) {
        return (obj instanceof SingleFileRepository &&
                res.equals(((SingleFileRepository) obj).res));
    }

    /**
     * Returns a hash code value for the object.
     */
    public int hashCode() {
        return res.hashCode();
    }

    /**
     * Returns a string representation of the object.
     */
    public String toString() {
        return new StringBuffer("SingleFileRepository[")
                .append(res.getName()).append("]").toString();
    }

    class FakeGlobal implements Repository {

        /**
         * Checksum of the repository and all its content. Implementations
         * should make sure
         *
         * @return checksum
         * @throws java.io.IOException
         */
        public long getChecksum() throws IOException {
            return res.lastModified();
        }

        /**
         * Returns the name of the repository.
         *
         * @return name of the repository
         */
        public String getShortName() {
            // we need to return "Global" here in order to be recognized as
            // global code folder - that's the whole purpose of this class
            return "Global";
        }

        /**
         * Returns the name of the repository; this is a full name including all
         * parent repositories.
         *
         * @return full name of the repository
         */
        public String getName() {
            return res.getName();
        }

        /**
         * Get this repository's logical script root repository.
         *
         * @return top-level repository
         * @see {isScriptRoot()}
         */
        public Repository getRootRepository() {
            return SingleFileRepository.this;
        }

        /**
         * Returns this repository's parent repository.
         * Returns null if this repository already is the top-level repository
         *
         * @return the parent repository
         */
        public Repository getParentRepository() {
            return SingleFileRepository.this;
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
            return false;
        }

        /**
         * Creates the repository if does not exist yet
         *
         * @throws java.io.IOException
         */
        public void create() throws IOException {
        }

        /**
         * Checks wether the repository actually (or still) exists
         *
         * @return true if the repository exists
         * @throws java.io.IOException
         */
        public boolean exists() throws IOException {
            return res.exists();
        }

        /**
         * Returns this repository's direct child repositories
         *
         * @return direct repositories
         * @throws java.io.IOException
         */
        public Repository[] getRepositories() throws IOException {
            return AbstractRepository.emptyRepositories;
        }

        /**
         * Returns all direct and indirect resources
         *
         * @return resources recursive
         * @throws java.io.IOException
         */
        public List getAllResources() throws IOException {
            return allResources;
        }

        /**
         * Returns all direct resources
         *
         * @return direct resources
         * @throws java.io.IOException
         */
        public Iterator getResources() throws IOException {
            return allResources.iterator();
        }

        /**
         * Returns a specific direct resource of the repository
         *
         * @param resourceName name of the child resource to return
         * @return specified child resource
         */
        public Resource getResource(String resourceName) {
            if (res.getName().equals(resourceName)) {
                return res;
            }
            return null;
        }

        /**
         * Returns the date the repository was last modified.
         *
         * @return last modified date
         * @throws java.io.IOException
         */
        public long lastModified() throws IOException {
            return res.lastModified();
        }
    }


}

