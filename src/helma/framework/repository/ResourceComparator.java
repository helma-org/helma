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

import java.util.Comparator;
import java.util.Iterator;
import helma.scripting.ScriptingResource;
import helma.framework.core.Application;

/**
 * Sorts resources according to the order of their repositories
 */
public class ResourceComparator implements Comparator {

    // the application where the top-level repositories can be found
    protected Application app;

    /**
     * Constructcs a ResourceComparator sorting according to the top-level
     * repositories of the given application
     * @param app application that provides the top-level repositories
     */
    public ResourceComparator(Application app) {
        this.app = app;
    }

    /**
     * Compares two repositories, resources or ScriptingResources
     * @param objA repository, resource or scripting resource
     * @param objB repository, resource or scripting resource
     * @return 0 if the two top-level repositories of the given objects are
     * equally sorted, 1 if the top-level repository of the first object has
     * a higher priority or -1 if the top-level repository of the second
     * object has a higher priority
     */
    public int compare(Object objA, Object objB) {
        Repository repositoryA = null;
        Repository repositoryB = null;

        if (objA instanceof Resource && objB instanceof Resource) {
            repositoryA = ((Resource) objA).getRepository().getRootRepository();
            repositoryB = ((Resource) objB).getRepository().getRootRepository();
        } else if (objA instanceof Repository && objB instanceof Repository) {
            repositoryA = ((Repository) objA).getRootRepository();
            repositoryB = ((Repository) objB).getRootRepository();
        } else if (objA instanceof ScriptingResource && objB instanceof ScriptingResource) {
            repositoryA = ((ScriptingResource) objA).getResource().getRepository().getRootRepository();
            repositoryB = ((ScriptingResource) objB).getResource().getRepository().getRootRepository();
        }

        if (repositoryA == null || repositoryB == null) {
            return 0;
        }

        Iterator iterator = app.getRepositories();
        int positionA = -1;
        int positionB = -1;
        int i = 0;

        while (iterator.hasNext()) {
            Repository repository = (Repository) iterator.next();
            if (repository == repositoryA) {
                positionA = i;
            } else if (repository == repositoryB) {
                positionB = i;
            }
            i++;
        }

        if (positionA == positionB) {
            return 0;
        } else if ((positionA != -1 && positionB != -1 && positionA < positionB) || (positionA > -1 && positionB == -1)) {
            return 1;
        } else {
            return -1;
        }
    }

    /**
     * Checks if the comparator is equal to the given comparator
     * A ResourceComparator is equal to another ResourceComparator if the
     * applications they belong to are equal
     * @param obj comparator
     * @return true if the given comparator equals
     */
    public boolean equals(Object obj) {
        if (this.app == (Application) obj) {
            return true;
        } else {
            return false;
        }
    }

}
