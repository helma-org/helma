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

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;


/**
 * @author manfred andres
 * This subnode-collection may be used to add nodes in an ordered way depending on
 * the given order. It is also possible to retrieve views of this list in a different
 * order. These views will be cached and automatically updated if this List's add- 
 * or remove-methods are called.
 */
public class OrderedSubnodeList extends SubnodeList {
    HashMap views = null;
    private final OrderedSubnodeList origin;

    // an array containing the order-fields
    private final String orderProperties[];
    // an array containing the direction for ordering 
    private final boolean orderIsDesc[];

    // the relation which is the basis for this collection
    final Relation rel;

    /**
     * Construct a new OrderedSubnodeList. The Relation is needed
     * to get the information about the ORDERING
     */
    public OrderedSubnodeList (Relation rel) {
        this.rel = rel;
        this.origin = null;
        // check the order of this collection for automatically sorting
        // in the values in the correct order
        if (rel.order == null) {
            orderProperties=null;
            orderIsDesc=null;
        } else {
            String singleOrders[] = rel.order.split(",");
            orderProperties = new String[singleOrders.length];
            orderIsDesc = new boolean[singleOrders.length];
            DbMapping dbm = rel.otherType;
            for (int i = 0; i < singleOrders.length; i++) {
                String currOrder[] = singleOrders[i].trim().split(" ");
                if (currOrder[0].equalsIgnoreCase(rel.otherType.getIDField())) {
                    orderProperties[i]=null;
                } else {
                    orderProperties[i] = dbm.columnNameToProperty(currOrder[0]);
                }
                if (currOrder.length < 2
                        || "ASC".equalsIgnoreCase(currOrder[1]))
                    orderIsDesc[i]=false;
                else
                    orderIsDesc[i]=true;
            }
        }
    }

    /**
     * This constructor is used to create a view for the OrderedSubnodeList origin.
     * @param origin the origin-list which holds the elements
     * @param expr the new order for this view
     * @param rel the relation given for the origin-list
     */
    public OrderedSubnodeList (OrderedSubnodeList origin, String expr, Relation rel) {
        this.origin = origin;
        this.rel = rel;
        if (expr==null) {
            orderProperties=null;
            orderIsDesc=null;
        } else {
            String singleOrders[] = expr.split(",");
            orderProperties = new String[singleOrders.length];
            orderIsDesc = new boolean[singleOrders.length];
            DbMapping dbm = rel.otherType;
            for (int i = 0; i<singleOrders.length; i++) {
                String currOrder[] = singleOrders[i].trim().split(" ");
                if (currOrder[0].equalsIgnoreCase("_id")) {
                    orderProperties[i]=null;
                } else {
                    if (dbm.propertyToColumnName(currOrder[0])==null)
                        throw new RuntimeException ("Properties must be mapped to get an ordered collection for these properties.");
                    orderProperties[i]=currOrder[0];
                }
                if (currOrder.length < 2
                        || "ASC".equalsIgnoreCase(currOrder[1]))
                    orderIsDesc[i]=false;
                else
                    orderIsDesc[i]=true;
            }
        }
        if (origin == null)
            return;
        this.sortIn(origin, false);
    }

   /**
    * Adds the specified object to this list performing
    * custom ordering
    *
    * @param obj element to be inserted.
    */    public boolean add(Object obj) {
        return add(obj, false);
    }

   /**
    * Adds the specified object to this list without performing
    * custom ordering.
    *
    * @param obj element to be inserted.
    */
    public boolean addSorted(Object obj) {
        return add(obj, true);
    }

    private boolean add(Object obj, boolean sorted) {
        if (origin != null)
            return origin.add(obj);
        vAdd(obj);
        while (rel.maxSize>0 && this.size() >= rel.maxSize)
            super.remove(0);
        // escape sorting for presorted adds and grouped nodes
        if (sorted || rel.groupby != null) {
            super.add(obj);
        } else {
            sortIn(obj);
        }
        return true;
    }

    /**
     * add a new node honoring the Nodes SQL-Order
     * @param obj the object to add
     */
    public boolean sortIn(Object obj) {
        // no order, just add
        if (this.orderProperties==null)
            return super.add(obj);
        vAdd(obj);
        long start = System.currentTimeMillis();
        try {
            int idx = this.determineNodePosition((NodeHandle) obj, 0);
            if (idx<0)
                return super.add(obj);
            else
                super.add(idx, obj);
            return true;
        } finally {
            System.out.println("Sortmillis: " + (System.currentTimeMillis() - start));
        }
    }

    private void vAdd (Object obj) {
        if (views==null || origin!=null || views.size()<1)
            return;
        for (Iterator i = views.values().iterator(); i.hasNext(); ) {
            OrderedSubnodeList osl = (OrderedSubnodeList) i.next();
            osl.sortIn(obj);
        }
    }

    public boolean addAll(Collection col) {
        return sortIn(col, true) > 0;
    }

    private void vAddAll (Collection col) {
        if (views==null || origin!=null || views.size()<1)
            return;
        for (Iterator i = views.values().iterator(); i.hasNext(); ) {
            OrderedSubnodeList osl = (OrderedSubnodeList) i.next();
            osl.addAll(col);
        }
    }

    public void add(int idx, Object obj) {
        if (this.orderProperties!=null)
            throw new RuntimeException ("Indexed add isn't alowed for ordered subnodes");
        super.add(idx, obj);
        vAdd(obj);
    }

    /**
     * Add all nodes contained inside the specified Collection to this
     * UpdateableSubnodeList. The order of the added Nodes is asumed to
     * be ordered according to the SQL-Order-Clausel given for this 
     * Subnodecollection but doesn't prevent adding of unordered Collections.
     * Ordered Collections will be sorted in more efficient than unordered ones.
     * @param col the collection containing all elements to add in the order returned by the select-statement
     * @param colHasDefaultOrder true if the given collection does have the default-order defined by the relation
     */
    public int sortIn (Collection col, boolean colHasDefaultOrder) {
        vAddAll(col);
        int cntr=0;
        // there is no order specified, add on top
        if (orderProperties==null) {
            for (Iterator i = col.iterator(); i.hasNext(); ) {
                super.add(cntr, i.next());
                cntr++;
            }
            if (rel.maxSize > 0) {
                int diff = this.size() - rel.maxSize;
                if (diff > 0)
                    super.removeRange(this.size()-1-diff, this.size()-1);
            }
        } else if (!colHasDefaultOrder || origin!=null) {
            // this collection is a view or the given collection doesn't have the
            // default order
            for (Iterator i = col.iterator(); i.hasNext(); ) {
                Object obj = i.next();
                if (obj==null)
                    continue;
                sortIn (obj);
                cntr++;
            }
        } else {
            NodeHandle[] nhArr = (NodeHandle[]) col.toArray (new NodeHandle[0]);
            int locIdx=determineNodePosition(nhArr[0], 0); // determine start-point
            if (locIdx==-1)
                locIdx=this.size();
            // int interval=Math.max(1, this.size()/2);
            int addIdx=0;
            for (; addIdx < nhArr.length; addIdx++) {
                while (locIdx < this.size() && compareNodes(nhArr[addIdx], (NodeHandle) this.get(locIdx)) >= 0)
                    locIdx++;
                if (locIdx >= this.size())
                    break;
                this.add(locIdx, nhArr[addIdx]);
                cntr++;
            }
            for (; addIdx < nhArr.length; addIdx++) {
                this.add(nhArr[addIdx]);
                cntr++;
            }
        }
        return cntr;
    }

    /**
     * remove the object specified by the given index-position
     * @param idx the index-position of the NodeHandle to remove
     */
    public Object remove (int idx) {
        vRemove(idx);
        return super.remove(idx);
    }

    private void vRemove(int idx) {
        if (views==null || origin!=null || views.size()<1)
            return;
        for (Iterator i = views.values().iterator(); i.hasNext(); ) {
            OrderedSubnodeList osl = (OrderedSubnodeList) i.next();
            osl.remove(idx);
        }
    }

    /**
     * remove the given Object from this List
     * @param obj the NodeHandle to remove
     */
    public boolean remove (Object obj) {
        vRemove(obj);
        return super.remove(obj);
    }

    private void vRemove(Object obj) {
        if (views==null || origin!=null || views.size()<1)
            return;
        for (Iterator i = views.values().iterator(); i.hasNext(); ) {
            OrderedSubnodeList osl = (OrderedSubnodeList) i.next();
            osl.remove(obj);
        }
    }

    /**
     * remove all elements conteined inside the specified collection
     * from this List
     * @param c the Collection containing all Objects to remove from this List
     * @return true if the List has been modified
     */
    public boolean removeAll(Collection c) {
        vRemoveAll(c);
        return super.removeAll(c);
    }

    private void vRemoveAll(Collection c) {
        if (views==null || origin!=null || views.size()<1)
            return;
        for (Iterator i = views.values().iterator(); i.hasNext(); ) {
            OrderedSubnodeList osl = (OrderedSubnodeList) i.next();
            osl.removeAll(c);
        }
    }

    /**
     * remove all elements from this List, which are NOT specified
     * inside the specified Collecion
     * @param c the Collection containing all Objects to keep on the List 
     * @return true if the List has been modified
     */
    public boolean retainAll (Collection c) {
        vRetainAll(c);
        return super.retainAll(c);
    }

    private void vRetainAll(Collection c) {
        if (views==null || origin!=null || views.size()<1)
            return;
        for (Iterator i = views.values().iterator(); i.hasNext(); ) {
            OrderedSubnodeList osl = (OrderedSubnodeList) i.next();
            osl.retainAll (c);
        }
    }

    private int determineNodePosition (NodeHandle nh, int startIdx) {
        int size = this.size();
        int interval = Math.max(1, (size-startIdx)/2);
        boolean dirUp=true;
        int cntr = 0;
        for (int i = 0; i < size
                && (i < rel.maxSize || rel.maxSize <= 0)
                && cntr<(size*2); cntr++) {  // cntr is used to avoid endless-loops which shouldn't happen
            NodeHandle curr = (NodeHandle) this.get(i);
            int comp = compareNodes(nh, curr);
            // current NodeHandle is below the given NodeHandle
            // interval has to be 1 and 
            // idx must be zero or the node before the current node must be higher or equal
            // all conditions must be met to determine the correct position of a node
            if (comp < 0 && interval==1 && (i==0 || compareNodes(nh, (NodeHandle) this.get(i-1)) >= 0)) {
                return i;
            } else if (comp < 0) {
                dirUp=false;
            } else if (comp > 0) {
                dirUp=true;
            } else if (comp == 0) {
                dirUp=true;
                interval=1;
            }
            if (dirUp) {
                i=i+interval;
                if (i >= this.size()) {
                    if (compareNodes(nh, (NodeHandle) this.get(size-1)) >= 0)
                        break;
                    interval = Math.max(1, (i - size-1)/2);
                    i = this.size()-1;
                } else {
                    interval = Math.max(1,interval/2);
                }
            } else {
                i=i-interval;
                if (i < 0) { // shouldn't happen i think
                    interval=Math.max(1,(interval+i)/2);
                    i=0;
                }
            }

        }
        if (cntr >= size*2 && size>1) {
            System.err.println("determineNodePosition needed more than the allowed iterations" + this.rel.prototype);
        }
        return -1;
    }

    /**
     * Compare two nodes depending on the specified ORDER for this collection.
     * @param nh1 the first NodeHandle
     * @param nh2 the second NodeHandle
     * @return an integer lesser than zero if nh1 is less than, zero if nh1 is equal to and a value greater than zero if nh1 is bigger than nh2.
     */
    private int compareNodes(NodeHandle nh1, NodeHandle nh2) {
        WrappedNodeManager wnmgr=null;
        for (int i = 0; i < orderProperties.length; i++) {
            if (orderProperties[i]==null) {
                // we have the id as order-criteria-> avoid loading node
                // and compare numerically instead of lexicographically
                String s1 = nh1.getID();
                String s2 = nh2.getID();
                int j = compareNumericString (s1, s2);
                if (j==0)
                    continue;
                if (orderIsDesc[i])
                    j=j*-1;
                return j;
            }
            if (wnmgr == null)
                wnmgr = rel.otherType.getWrappedNodeManager();
            Property p1 = nh1.getNode(wnmgr).getProperty(orderProperties[i]);
            Property p2 = nh2.getNode(wnmgr).getProperty(orderProperties[i]);
//            System.out.println ("Comparing " + p1.getStringValue() + " - " + p2.getStringValue());
            int j;
            if (p1==null && p2==null)
                continue;
            else if (p1==null)
                j = -1;
            else
                j = p1.compareTo(p2);
            if (j == 0)
                continue;
            if (orderIsDesc[i])
                j = j * -1;
            return j;
        }
        return 0;
    }

    /**
     * Compare two strings containing numbers depending on their numeric values
     * instead of doing a lexicographical comparison.
     * @param a the first String
     * @param b the second String
     */
    public static int compareNumericString(String a, String b) {
        if (a == null && b != null)
            return -1;
        if (a != null && b == null)
            return 1;
        if (a == null && b == null)
            return 0;
        if (a.length() < b.length())
            return -1;
        if (a.length() > b.length())
            return 1;
        for (int i = 0; i < a.length(); i++) {
            if (a.charAt(i) < b.charAt(i))
                return -1;
            if (a.charAt(i) > b.charAt(i))
                return 1;
        }
        return 0;
    }

    public List getOrderedView (String order) {
        if (origin != null)
            return origin.getOrderedView(order);
        String key = order.trim().toLowerCase();
        if (key.equalsIgnoreCase(rel.order))
            return this;
        long start = System.currentTimeMillis();
        if (views == null)
            views = new HashMap();
        OrderedSubnodeList osl = (OrderedSubnodeList) views.get(key);
        if (osl == null) {
            osl = new OrderedSubnodeList (this, order, rel);
            views.put(key, osl);
            System.out.println("getting view cost me " + (System.currentTimeMillis()-start) + " millis");
        } else
            System.out.println("getting cached view cost me " + (System.currentTimeMillis()-start) + " millis");
        return osl;
    }
}
