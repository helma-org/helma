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
import java.io.Serializable;

/**
 * Container implementation for subnode collections.
 */
public class SubnodeList implements Serializable {

    protected Node node;
    protected List list;

    transient protected long lastSubnodeFetch = 0;
    transient protected long lastSubnodeChange = 0;
    

    /**
     * Hide/disable zero argument constructor for subclasses
     */
    private SubnodeList()  {}

    /**
     * Creates a new subnode list
     * @param node the node we belong to
     */
    public SubnodeList(Node node) {
        this.node = node;
        this.list = new ArrayList();
    }

    /**
     * Adds the specified object to this list performing
     * custom ordering
     *
     * @param handle element to be inserted.
     */
    public boolean add(NodeHandle handle) {
        return list.add(handle);
    }
    /**
     * Adds the specified object to the list at the given position
     * @param idx the index to insert the element at
     * @param handle the object to add
     */
    public void add(int idx, NodeHandle handle) {
        list.add(idx, handle);
    }

    public NodeHandle get(int index) {
        if (index < 0 || index >= list.size()) {
            return null;
        }
        return (NodeHandle) list.get(index);
    }

    public Node getNode(int index) {
        Node retval = null;
        NodeHandle handle = get(index);

        if (handle != null) {
            retval = handle.getNode(node.nmgr);
            // Legacy alarm!
            if ((retval != null) && (retval.parentHandle == null) &&
                    !node.nmgr.isRootNode(retval)) {
                retval.setParent(node);
                retval.anonymous = true;
            }
        }

        return retval;
    }

    public boolean contains(Object object) {
        return list.contains(object);
    }

    public int indexOf(Object object) {
        return list.indexOf(object);
    }

    /**
     * remove the object specified by the given index-position
     * @param idx the index-position of the NodeHandle to remove
     */
    public Object remove (int idx) {
        return list.remove(idx);
    }

    /**
     * remove the given Object from this List
     * @param obj the NodeHandle to remove
     */
    public boolean remove (Object obj) {
        return list.remove(obj);
    }

    public Object[] toArray() {
        return list.toArray();
    }

    /**
     * Return the size of the list.
     * @return the list size
     */
    public int size() {
        return list.size();
    }

    protected void update() {
        // also reload if the type mapping has changed.
        long lastChange = getLastSubnodeChange();
        if (lastChange != lastSubnodeFetch) {
            Relation rel = getSubnodeRelation();
            if (rel.aggressiveLoading && rel.groupby == null) {
                list = node.nmgr.getNodes(node, rel);
            } else {
                list = node.nmgr.getNodeIDs(node, rel);
            }
            lastSubnodeFetch = lastChange;
        }
    }

    protected void prefetch(int start, int length) {
        if (start < 0 || start >= size()) {
            return;
        }
        length =  (length < 0) ?
                size() - start : Math.min(length, size() - start);
        if (length < 0) {
            return;
        }

        DbMapping dbmap = getSubnodeMapping();

        if (dbmap.isRelational()) {
            Relation rel = getSubnodeRelation();
            node.nmgr.prefetchNodes(node, rel, this, start, length);
        }
    }

    /**
     * Compute a serial number indicating the last change in subnode collection
     * @return a serial number that increases with each subnode change
     */
    protected long getLastSubnodeChange() {
        // include dbmap.getLastTypeChange to also reload if the type mapping has changed.
        long checkSum = lastSubnodeChange + node.dbmap.getLastTypeChange();
        Relation rel = getSubnodeRelation();
        return rel == null || rel.aggressiveCaching ?
                checkSum : checkSum + rel.otherType.getLastDataChange();
    }

    protected synchronized void markAsChanged() {
        lastSubnodeChange += 1;
    }

    protected boolean hasRelationalNodes() {
        DbMapping dbmap = getSubnodeMapping();
        return (dbmap != null && dbmap.isRelational()
                && ((node.getState() != Node.TRANSIENT &&  node.getState() != Node.NEW)
                    || node.getSubnodeRelation() != null));
    }

    protected DbMapping getSubnodeMapping() {
        return node.dbmap == null ? null : node.dbmap.getSubnodeMapping();
    }

    protected Relation getSubnodeRelation() {
        return node.dbmap == null ? null : node.dbmap.getSubnodeRelation();
    }
}
