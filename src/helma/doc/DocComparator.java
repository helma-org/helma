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

package helma.doc;

import java.util.Comparator;

/**
 * 
 */
public class DocComparator implements Comparator {
    public static final int BY_TYPE = 0;
    public static final int BY_NAME = 1;
    int mode;
    DocElement docEl;

    /**
     * Creates a new DocComparator object.
     *
     * @param mode ...
     * @param docEl ...
     */
    public DocComparator(int mode, DocElement docEl) {
        this.mode = mode;
        this.docEl = docEl;
    }

    /**
     * Creates a new DocComparator object.
     *
     * @param docEl ...
     */
    public DocComparator(DocElement docEl) {
        this.mode = 0;
        this.docEl = docEl;
    }

    /**
     *
     *
     * @param obj1 ...
     * @param obj2 ...
     *
     * @return ...
     */
    public int compare(Object obj1, Object obj2) {
        DocElement e1 = (DocElement) obj1;
        DocElement e2 = (DocElement) obj2;

        if ((mode == BY_TYPE) && (e1.getType() > e2.getType())) {
            return 1;
        } else if ((mode == BY_TYPE) && (e1.getType() < e2.getType())) {
            return -1;
        } else {
            return e1.name.compareTo(e2.name);
        }
    }

    /**
     *
     *
     * @param obj ...
     *
     * @return ...
     */
    public boolean equals(Object obj) {
        DocElement el = (DocElement) obj;

        if (el.name.equals(docEl.name) && (el.getType() == docEl.getType())) {
            return true;
        } else {
            return false;
        }
    }
}
