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
import helma.framework.repository.Repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Provides common methods and fields for the default implementations of the
 * repository interface
 */
public abstract class AbstractRepository implements Repository {

    /**
     * Defines wether this repository is the top-level repository
     * Defaults to reflect a non top-level repository
     */
    boolean notRoot = true;

    /**
     * Upper repository the repository was belongs to
     */
    Repository rootRepository;

    /**
     * Holds direct sub-repositories
     */
    Repository[] repositories;

    /**
     * Holds direct resources
     */
    HashMap resources;

    /**
     * Cached name for faster access
     */
    String name;

    /**
     * Cached short name for faster access
     */
    String shortName;

    public String getName() {
        return name;
    }

    public String getShortName() {
        return shortName;
    }

    public Repository getRootRepository() {
        if (notRoot == false) {
            return this;
        } else {
            return rootRepository.getRootRepository();
        }
    }

    public Resource getResource(String name) {
        update();

        return (Resource) resources.get(getName() + "/" + name);
    }

    public Resource[] getResources() {
        update();

        return (Resource[]) resources.values().toArray(new Resource[resources.size()]);
    }

    public Repository[] getRepositories() {
        update();

        return repositories;
    }

    public boolean isRootRepository() {
        return !notRoot;
    }

    public Repository getRepository() {
        return rootRepository;
    }

    public Resource[] getAllResources() {
        update();

        ArrayList allResources = new ArrayList();
        allResources.addAll(resources.values());

        for (int i = 0; i < repositories.length; i++) {
            allResources.addAll(Arrays.asList(repositories[i].getAllResources()));
        }

        return (Resource[]) allResources.toArray(new Resource[allResources.size()]);
    }

}
