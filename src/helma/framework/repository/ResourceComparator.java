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
     * Compares two Repositories, Resources or RepositoryTrackers
     * @param obj1 Repository, Resource or RepositoryTrackers
     * @param obj2 Repository, Resource or RepositoryTrackers
     * @return a negative integer, zero, or a positive integer as the
     * 	       first argument is less than, equal to, or greater than the
     *	       second.
     * @throws ClassCastException if the arguments' types prevent them from
     * 	       being compared by this Comparator.
     */
    public int compare(Object obj1, Object obj2) {
        if (obj1.equals(obj2))
            return 0;

        Repository rep1 = getRootRepository(obj1);
        Repository rep2 = getRootRepository(obj2);

        int pos1 = app.getRepositoryIndex(rep1);
        int pos2 = app.getRepositoryIndex(rep2);

        if (rep1 == rep2 || (pos1 == -1 && pos2 == -1)) {
            // Same root repository, but we must not return 0 unless objects are equal
            // (see JavaDoc on java.util.TreeSet) so we compare full names
            return getFullName(obj1).compareTo(getFullName(obj2));
        }

        return pos1 - pos2;
    }

    /**
     * Checks if the comparator is equal to the given comparator
     * A ResourceComparator is equal to another ResourceComparator if the
     * applications they belong to are equal
     *
     * @param obj comparator
     * @return true if the given comparator equals
     */
    public boolean equals(Object obj) {
        return (obj instanceof ResourceComparator) &&
                app == ((ResourceComparator) obj).getApplication();
    }

    /**
     * Return the application we're comparing resources for
     *
     * @return the application instance
     */
    public Application getApplication() {
        return app;
    }

    private Repository getRootRepository(Object obj) {
        if (obj instanceof Resource)
            return ((Resource) obj).getRepository()
                                   .getRootRepository();
        if (obj instanceof ResourceTracker)
            return ((ResourceTracker) obj).getResource()
                                          .getRepository()
                                          .getRootRepository();
        if (obj instanceof Repository)
            return ((Repository) obj).getRootRepository();

        // something we can't compare
        throw new IllegalArgumentException("Can't compare "+obj);
    }

    private String getFullName(Object obj) {
        if (obj instanceof Resource)
            return ((Resource) obj).getName();
        if (obj instanceof ResourceTracker)
            return ((ResourceTracker) obj).getResource()
                                          .getName();
        if (obj instanceof Repository)
            return ((Repository) obj).getName();

        // something we can't compare
        throw new IllegalArgumentException("Can't compare "+obj);
    }

}
