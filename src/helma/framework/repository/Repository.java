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

import helma.framework.repository.Resource;

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
     */
    public long getChecksum();

    /**
     * Returns the date the repository was last modified.
     *
     * @return last modified date
     */
    public long lastModified();

    /**
     * Returns all direct resources
     *
     * @return direct resources
     */
    public Resource[] getResources();

    /**
     * Returns all direct and indirect resources
     *
     * @return resources recursive
     */
    public Resource[] getAllResources();

    /**
     * Returns this repository's direct child repositories
     *
     * @return direct repositories
     */
    public Repository[] getRepositories();

    /**
     * Checks wether the repository actually (or still) exists
     *
     * @return true if the repository exists
     */
    public boolean exists();

    /**
     * Checks wether the repository is the top-level repository
     *
     * @return true if the repository is the top-level repository
     */
    public boolean isRootRepository();

    /**
     * Returns this repository's parent repository.
     * Returns null if this repository already is the top-level repository
     *
     * @return the parent repository
     */
    public Repository getParentRepository();

    /**
     * Returns the top-level repository this repository is contained in
     *
     * @return top-level repository
     */
    public Repository getRootRepository();

    /**
     * Creates the repository if does not exist yet
     */
    public void create();

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

    /**
     * Returns a specific direct resource of the repository
     *
     * @param resourceName name of the child resource to return
     * @return specified child resource
     */
    public Resource getResource(String resourceName);

}
