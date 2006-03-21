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

    // the base subnode list, in case this is an ordered view
    private SubnodeList origin;
    // an array containing the order-fields
    private String orderProperties[];
    // an array containing the direction for ordering 
    private boolean orderIsDesc[];

    /**
     * Construct a new OrderedSubnodeList. The Relation is needed
     * to get the information about the ORDERING
     */
    public OrderedSubnodeList (WrappedNodeManager nmgr, Relation rel) {
        super(nmgr, rel);
        this.origin = null;
        // check the order of this collection for automatically sorting
        // in the values in the correct order
        initOrder(rel.order);
    }

    /**
     * This constructor is used to create a view for the OrderedSubnodeList origin.
     * @param origin the origin-list which holds the elements
     * @param expr the new order for this view
     * @param rel the relation given for the origin-list
     */
    public OrderedSubnodeList (WrappedNodeManager nmgr, Relation rel, SubnodeList origin, String expr) {
        super(nmgr, rel);
        this.origin = origin;
        initOrder(expr);
        if (origin != null) {
            sortIn(origin, false);
        }
    }

    private void initOrder(String order) {
        if (order == null) {
            orderProperties=null;
            orderIsDesc=null;
        } else {
            String orderParts[] = order.split(",");
            orderProperties = new String[orderParts.length];
            orderIsDesc = new boolean[orderParts.length];
            for (int i = 0; i < orderParts.length; i++) {
                String part[] = orderParts[i].trim().split(" ");
                orderProperties[i] =  part[0].equals("_id") ?
                    null : part[0];
                orderIsDesc[i] = part.length == 2 &&
                        "DESC".equalsIgnoreCase(part[1]);
            }
        }
    }

   /**
    * Adds the specified object to this list performing
    * custom ordering
    *
    * @param obj element to be inserted.
    */
    public boolean add(Object obj) {
        return add(obj, true);
    }

    /**
     * Adds the specified object to the list at the given position
     * @param idx the index to insert the element at
     * @param obj the object t add
     */
    public void add(int idx, Object obj) {
        if (this.orderProperties!=null)
            throw new RuntimeException ("Indexed add isn't alowed for ordered subnodes");
        super.add(idx, obj);
        addToViews(obj);
    }

   /**
    * Adds the specified object to this list without performing
    * custom ordering.
    *
    * @param obj element to be inserted.
    */
    public boolean addSorted(Object obj) {
        return add(obj, false);
    }

    boolean add(Object obj, boolean sort) {
        if (origin != null) {
            return origin.add(obj);
        }
        addToViews(obj);
        int maxSize = rel == null ? 0 : rel.maxSize;
        while (maxSize > 0 && this.size() >= maxSize) {
            remove(size() - 1);
        }
        // escape sorting for presorted adds and grouped nodes
        if (sort && (rel == null || rel.groupby == null)) {
            sortIn(obj);
        } else {
            super.add(obj);
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
        addToViews(obj);
        Node node = ((NodeHandle) obj).getNode(nmgr);
        int idx = this.determineNodePosition(node, 0);
        // System.err.println("Position: " + idx);
        if (idx<0)
            return super.add(obj);
        else
            super.add(idx, obj);
        return true;
    }

    public boolean addAll(Collection col) {
        return sortIn(col, true) > 0;
    }

    private void addAllToViews (Collection col) {
        if (views == null || origin != null || views.isEmpty())
            return;
        for (Iterator i = views.values().iterator(); i.hasNext(); ) {
            OrderedSubnodeList osl = (OrderedSubnodeList) i.next();
            osl.addAll(col);
        }
    }

    /**
     * Add all nodes contained inside the specified Collection to this
     * UpdateableSubnodeList. The order of the added Nodes is assumed to
     * be ordered according to the SQL-Order-Clause given for this
     * Subnode collection but doesn't prevent adding of unordered Collections.
     * Ordered Collections will be sorted in more efficiently than unordered ones.
     *
     * @param col the collection containing all elements to add in the order returned by the select-statement
     * @param colHasDefaultOrder true if the given collection does have the default-order defined by the relation
     */
    public int sortIn (Collection col, boolean colHasDefaultOrder) {
        addAllToViews(col);
        int cntr=0;
        // there is no order specified, add on top
        if (orderProperties == null) {
            for (Iterator i = col.iterator(); i.hasNext(); ) {
                super.add(cntr, i.next());
                cntr++;
            }
            int maxSize = rel == null ? 0 : rel.maxSize;
            if (maxSize > 0) {
                int diff = this.size() - maxSize;
                if (diff > 0)
                    super.removeRange(this.size()-1-diff, this.size()-1);
            }
        } else if (!colHasDefaultOrder || origin != null) {
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
            Node node = nhArr[0].getNode(nmgr);
            int locIdx = determineNodePosition(node, 0); // determine start-point
            if (locIdx == -1)
                locIdx = this.size();
            // int interval=Math.max(1, this.size()/2);
            for (int addIdx=0; addIdx < nhArr.length; addIdx++) {
                node = nhArr[addIdx].getNode(nmgr);
                while (locIdx < this.size() &&
                        compareNodes(node, (NodeHandle) this.get(locIdx)) >= 0)
                    locIdx++;
                if (locIdx < this.size()) {
                    this.add(locIdx, nhArr[addIdx]);
                } else {
                    this.add(nhArr[addIdx]);
                }
                cntr++;
            }
        }
        return cntr;
    }

    /**
     * remove all elements contained inside the specified collection
     * from this List
     * @param c the Collection containing all Objects to remove from this List
     * @return true if the List has been modified
     */
    public boolean removeAll(Collection c) {
        removeAllFromViews(c);
        return super.removeAll(c);
    }

    private void removeAllFromViews(Collection c) {
        if (views == null || origin != null || views.isEmpty())
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
        retainAllInViews(c);
        return super.retainAll(c);
    }

    private void retainAllInViews(Collection c) {
        if (views == null || origin != null || views.isEmpty())
            return;
        for (Iterator i = views.values().iterator(); i.hasNext(); ) {
            OrderedSubnodeList osl = (OrderedSubnodeList) i.next();
            osl.retainAll (c);
        }
    }

    private int determineNodePosition (Node node, int startIdx) {
        int size = this.size();
        int interval = Math.max(1, (size - startIdx) / 2);
        boolean dirUp=true;
        int cntr = 0;
        int maxSize = rel == null ? 0 : rel.maxSize;
        for (int i = 0; i < size
                && (i < maxSize || maxSize <= 0)
                && cntr < (size * 2); cntr++) {  // cntr is used to avoid endless-loops which shouldn't happen
            NodeHandle curr = (NodeHandle) this.get(i);
            int comp = compareNodes(node, curr);
            // current NodeHandle is below the given NodeHandle
            // interval has to be 1 and 
            // idx must be zero or the node before the current node must be higher or equal
            // all conditions must be met to determine the correct position of a node
            if (comp < 0 && interval==1 && (i==0 || compareNodes(node, (NodeHandle) this.get(i-1)) >= 0)) {
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
                    if (compareNodes(node, (NodeHandle) this.get(size-1)) >= 0)
                        break;
                    interval = Math.max(1, (i - size-1)/2);
                    i = this.size()-1;
                } else {
                    interval = Math.max(1,interval/2);
                }
            } else {
                i = i-interval;
                if (i < 0) { // shouldn't happen i think
                    interval=Math.max(1, (interval+i)/2);
                    i=0;
                }
            }

        }
        if (cntr >= size * 2 && size > 1) {
            System.err.println("determineNodePosition needed more than the allowed iterations");
        }
        return -1;
    }

    /**
     * Compare two nodes depending on the specified ORDER for this collection.
     * @param node the first Node
     * @param nodeHandle the second Node
     * @return an integer lesser than zero if nh1 is less than, zero if nh1 is equal to and a value greater than zero if nh1 is bigger than nh2.
     */
    private int compareNodes(Node node, NodeHandle nodeHandle) {
        for (int i = 0; i < orderProperties.length; i++) {
            if (orderProperties[i]==null) {
                // we have the id as order-criteria-> avoid loading node
                // and compare numerically instead of lexicographically
                String s1 = node.getID();
                String s2 = nodeHandle.getID();
                int j = compareNumericString (s1, s2);
                if (j == 0)
                    continue;
                if (orderIsDesc[i])
                    j = j * -1;
                return j;
            }
            Property p1 = node.getProperty(orderProperties[i]);
            Property p2 = nodeHandle.getNode(nmgr).getProperty(orderProperties[i]);
            int j;
            if (p1 == null && p2 == null) {
                continue;
            } else if (p1 == null) {
                j = 1;
            } else if (p2 == null) {
                j = -1;
            } else {
                j = p1.compareTo(p2);
            }
            if (j == 0) {
                continue;
            }
            if (orderIsDesc[i]) {
                j = j * -1;
            }
            return j;
        }
        return -1;
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
        if (origin != null) {
            return origin.getOrderedView(order);
        } else {
            return super.getOrderedView(order);
        }
    }
}
