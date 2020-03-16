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

import java.util.List;
import java.util.Iterator;
import java.io.IOException;

/**
 * Repository represents an abstract container of resources (e.g. code, skins, ...).
 * In addition to resources, repositories may contain other repositories, building
 * a hierarchical structure.
 */
public interface Repository {

    /**
     * Checksum of the repository and all its content. Implementations
     * should make sure
     *
     * @return checksum
     * @throws IOException
     */
    public long getChecksum() throws IOException;

    /**
     * Returns the date the repository was last modified.
     *
     * @return last modified date
     * @throws IOException
     */
    public long lastModified() throws IOException;


    /**
     * Returns a specific direct resource of the repository
     *
     * @param resourceName name of the child resource to return
     * @return specified child resource
     */
    public Resource getResource(String resourceName);

    /**
     * Returns all direct resources
     *
     * @return direct resources
     * @throws IOException
     */
    public Iterator getResources() throws IOException;

    /**
     * Returns all direct and indirect resources
     *
     * @return resources recursive
     * @throws IOException
     */
    public List getAllResources() throws IOException;

    /**
     * Returns this repository's direct child repositories
     *
     * @return direct repositories
     * @throws IOException
     */
    public Repository[] getRepositories() throws IOException;

    /**
     * Checks wether the repository actually (or still) exists
     *
     * @return true if the repository exists
     * @throws IOException
     */
    public boolean exists() throws IOException;

    /**
     * Creates the repository if does not exist yet
     *
     * @throws IOException
     */
    public void create() throws IOException;

    /**
     * Checks wether the repository is to be considered a top-level
     * repository from a scripting point of view. For example, a zip
     * file within a file repository is not a root repository from
     * a physical point of view, but from the scripting point of view it is.
     *
     * @return true if the repository is to be considered a top-level script repository
     */
    public boolean isScriptRoot();

    /**
     * Returns this repository's parent repository.
     * Returns null if this repository already is the top-level repository
     *
     * @return the parent repository
     */
    public Repository getParentRepository();

    /**
     * Get this repository's logical script root repository.
     *
     * @see {isScriptRoot()}
     * @return top-level repository
     */
    public Repository getRootRepository();

    /**
     * Returns the name of the repository; this is a full name including all
     * parent repositories.
     *
     * @return full name of the repository
     */
    public String getName();

    /**
     * Returns the name of the repository.
     *
     * @return name of the repository
     */
    public String getShortName();

}
