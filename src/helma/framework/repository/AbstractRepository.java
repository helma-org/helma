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

    /**
     * Called to check the repository's content.
     */
    public abstract void update();

    /**
     * Called to create a child resource for this repository
     */
    protected abstract Resource createResource(String name);

    /**
     *
     * @return
     */
    public String getName() {
        return name;
    }

    public String getShortName() {
        return shortName;
    }

    public Repository getRootRepository() {
        if (parent == null) {
            return this;
        } else {
            return parent.getRootRepository();
        }
    }

    public Resource getResource(String name) {
        update();

        Resource res = (Resource) resources.get(name);
        // if resource does not exist, create it
        if (res == null) {
            res = createResource(name);
            resources.put(name, res);
        }
        return res;
    }

    public Iterator getResources() {
        update();

        return resources.values().iterator();
    }

    public Repository[] getRepositories() {
        update();

        return repositories;
    }

    public Repository getParentRepository() {
        return parent;
    }

    public List getAllResources() throws IOException {
        update();

        ArrayList allResources = new ArrayList();
        allResources.addAll(resources.values());

        for (int i = 0; i < repositories.length; i++) {
            allResources.addAll(repositories[i].getAllResources());
        }

        return allResources;
    }

    public String toString() {
        return getName();
    }

}
