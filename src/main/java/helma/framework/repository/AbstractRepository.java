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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Iterator;
import java.io.IOException;

/**
 * Provides common methods and fields for the default implementations of the
 * repository interface
 */
public abstract class AbstractRepository implements Repository {


    /**
     * Parent repository this repository is contained in.
     */
    Repository parent;

    /**
     * Holds direct child repositories
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

    /*
     * empty repository array for convenience
     */
    final static Repository[] emptyRepositories = new Repository[0]; 

    /**
     * Called to check the repository's content.
     */
    public abstract void update();

    /**
     * Called to create a child resource for this repository
     */
    protected abstract Resource createResource(String name);

    /**
     * Get the full name that identifies this repository globally
     */
    public String getName() {
        return name;
    }

    /**
     * Get the local name that identifies this repository locally within its
     * parent repository
     */
    public String getShortName() {
        return shortName;
    }

    /**
     * Get this repository's logical script root repository.
     *
     *@see {isScriptRoot()}
     */
    public Repository getRootRepository() {
        if (parent == null || isScriptRoot()) {
            return this;
        } else {
            return parent.getRootRepository();
        }
    }

    /**
     * Get a resource contained in this repository identified by the given local name.
     * If the name can't be resolved to a resource, a resource object is returned
     * for which {@link Resource exists()} returns <code>false<code>.
     */
    public synchronized Resource getResource(String name) {
        update();

        Resource res = (Resource) resources.get(name);
        // if resource does not exist, create it
        if (res == null) {
            res = createResource(name);
            resources.put(name, res);
        }
        return res;
    }

    /**
     * Get an iterator over the resources contained in this repository.
     */
    public synchronized Iterator getResources() {
        update();

        return resources.values().iterator();
    }

    /**
     * Get an iterator over the sub-repositories contained in this repository.
     */
    public synchronized Repository[] getRepositories() {
        update();

        return repositories;
    }

    /**
     * Get this repository's parent repository.
     */
    public Repository getParentRepository() {
        return parent;
    }

    /**
     * Get a deep list of this repository's resources, including all resources
     * contained in sub-reposotories.
     */
    public synchronized List getAllResources() throws IOException {
        update();

        ArrayList allResources = new ArrayList();
        allResources.addAll(resources.values());

        for (int i = 0; i < repositories.length; i++) {
            allResources.addAll(repositories[i].getAllResources());
        }

        return allResources;
    }

    /**
     * Returns the repositories full name as string representation.
     * @see {getName()}
     */
    public String toString() {
        return getName();
    }

}
