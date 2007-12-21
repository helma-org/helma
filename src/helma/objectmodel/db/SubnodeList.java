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

package helma.objectmodel.db;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Iterator;

/**
 * A subclass of ArrayList that adds an addSorted(Object) method to
 */
public class SubnodeList extends ArrayList {

    transient WrappedNodeManager nmgr;
    transient HashMap views = null;
    transient Relation rel;

    /**
     * Hide/disable zero argument constructor for subclasses
     */
    private SubnodeList()  {}

    /**
     * Creates a new subnode list
     * @param nmgr
     */
    public SubnodeList(WrappedNodeManager nmgr, Relation rel) {
        this.nmgr = nmgr;
        this.rel = rel;
    }

   /**
    * Inserts the specified element at the specified position in this
    * list without performing custom ordering
    *
    * @param obj element to be inserted.
    */
    public boolean addSorted(Object obj)  {
        return add(obj);
    }

    /**
     * Adds the specified object to this list performing
     * custom ordering
     *
     * @param obj element to be inserted.
     */
    public boolean add(Object obj) {
        addToViews(obj);
        return super.add(obj);
    }
    /**
     * Adds the specified object to the list at the given position
     * @param idx the index to insert the element at
     * @param obj the object t add
     */
    public void add(int idx, Object obj) {
        addToViews(obj);
        super.add(idx, obj);
    }

    /**
     * remove the object specified by the given index-position
     * @param idx the index-position of the NodeHandle to remove
     */
    public Object remove (int idx) {
        Object obj = get(idx);
        if (obj != null) {
            removeFromViews(obj);
        }
        return super.remove(idx);
    }

    /**
     * remove the given Object from this List
     * @param obj the NodeHandle to remove
     */
    public boolean remove (Object obj) {
        removeFromViews(obj);
        return super.remove(obj);
    }

    protected void removeFromViews(Object obj) {
        if (views == null || views.isEmpty())
            return;
        for (Iterator i = views.values().iterator(); i.hasNext(); ) {
            OrderedSubnodeList osl = (OrderedSubnodeList) i.next();
            osl.remove(obj);
        }
    }

    public List getOrderedView (String order) {
        String key = order.trim().toLowerCase();
        // long start = System.currentTimeMillis();
        if (views == null) {
            views = new HashMap();
        }
        OrderedSubnodeList osl = (OrderedSubnodeList) views.get(key);
        if (osl == null) {
            osl = new OrderedSubnodeList (nmgr, rel, this, order);
            views.put(key, osl);
        }
        return osl;
    }

    protected void addToViews (Object obj) {
        if (views == null || views.isEmpty())
            return;
        for (Iterator i = views.values().iterator(); i.hasNext(); ) {
            OrderedSubnodeList osl = (OrderedSubnodeList) i.next();
            osl.sortIn(obj);
        }
    }

}
