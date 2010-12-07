/*
 * Helma License Notice
 *
 * The contents of this file are subject to the Helma License
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://adele.helma.org/download/helma/license.txt
 *
 * Copyright 2009 Helma Project. All Rights Reserved.
 */

package helma.objectmodel.db;

import java.util.*;

public class SegmentedSubnodeList extends SubnodeList {

    private static final long serialVersionUID = -4947752577517584610L;

    transient Segment[] segments = null;
    static int SEGLENGTH = 1000;

    transient private int subnodeCount = -1;

    /**
     * Creates a new subnode list
     * @param node the node we belong to
     */
    public SegmentedSubnodeList(Node node) {
        super(node);
    }

    /**
     * Adds the specified object to this list performing
     * custom ordering
     *
     * @param handle element to be inserted.
     */
    public synchronized boolean add(NodeHandle handle) {
        if (!hasRelationalNodes() || segments == null) {
            return super.add(handle);
        }
        if (subnodeCount == -1) {
            update();
        }
        subnodeCount++;
        segments[segments.length - 1].length += 1;
        return list.add(handle);
    }
    /**
     * Adds the specified object to the list at the given position
     * @param index the index to insert the element at
     * @param handle the object to add
     */
    public synchronized void add(int index, NodeHandle handle) {
        if (!hasRelationalNodes() || segments == null) {
            super.add(index, handle);
            return;
        }
        if (subnodeCount == -1) {
            update();
        }
        subnodeCount++;
        list.add(index, handle);
        // shift segment indices by one
        int s = getSegment(index);
        segments[s].length += 1;
        for (int i = s + 1; i < segments.length; i++) {
            segments[i].startIndex += 1;
        }
    }

    public NodeHandle get(int index) {
        if (!hasRelationalNodes() || segments == null) {
            return super.get(index);
        }
        if (index < 0 || index >= subnodeCount) {
            return null;
        }
        loadSegment(getSegment(index), false);
        return (NodeHandle) list.get(index);
    }

    public synchronized boolean contains(Object object) {
        if (!hasRelationalNodes() || segments == null) {
            return super.contains(object);
        }
        if (list.contains(object)) {
            return true;
        }
        for (int i = 0; i < segments.length; i++) {
            if (loadSegment(i, false).contains(object)) {
                return true;
            }
        }
        return false;
    }

    public synchronized int indexOf(Object object) {
        if (!hasRelationalNodes() || segments == null) {
            return super.indexOf(object);
        }
        int index;
        if ((index = list.indexOf(object)) > -1) {
            return index;
        }
        for (int i = 0; i < segments.length; i++) {
            if ((index = loadSegment(i, false).indexOf(object)) > -1) {
                return segments[i].startIndex + index;
            }
        }
        return -1;
    }

    /**
     * remove the object specified by the given index-position
     * @param index the index-position of the NodeHandle to remove
     */
    public synchronized Object remove(int index) {
        if (!hasRelationalNodes() || segments == null) {
            return super.remove(index);
        }
        if (subnodeCount == -1) {
            update();
        }
        Object removed = list.remove(index);
        int s = getSegment(index);
        segments[s].length -= 1;
        for (int i = s + 1; i < segments.length; i++) {
            segments[i].startIndex -= 1;
        }
        subnodeCount--;
        return removed;
    }

    /**
     * remove the given Object from this List
     * @param object the NodeHandle to remove
     */
    public synchronized boolean remove(Object object) {
        if (!hasRelationalNodes() || segments == null) {
            return super.remove(object);
        }
        if (subnodeCount == -1) {
            update();
        }
        int index = indexOf(object);
        if (index > -1) {
            list.remove(object);
            int s = getSegment(index);
            segments[s].length -= 1;
            for (int i = s + 1; i < segments.length; i++) {
                segments[i].startIndex -= 1;
            }
            subnodeCount--;
            return true;
        }
        return false;
    }

    public synchronized Object[] toArray() {
        if (!hasRelationalNodes() || segments == null) {
            return super.toArray();
        }
        node.nmgr.logEvent("Warning: toArray() called on large segmented collection: " + node);
        for (int i = 0; i < segments.length; i++) {
            loadSegment(i, false);
        }
        return list.toArray();
    }

    private int getSegment(int index) {
        for (int i = 1; i < segments.length; i++) {
            if (index < segments[i].startIndex) {
                return i - 1;
            }
        }
        return segments.length - 1;
    }

    private List loadSegment(int seg, boolean deep) {
        Segment segment = segments[seg];
        if (segment != null && !segment.loaded) {
            Relation rel = getSubnodeRelation().getClone();
            rel.offset = segment.startIndex;
            int expectedSize = rel.maxSize = segment.length;
            List seglist =  deep ?
                    node.nmgr.getNodes(node, rel) :
                    node.nmgr.getNodeIDs(node, rel);
            int actualSize = seglist.size();
            if (actualSize != expectedSize) {
                node.nmgr.logEvent("Inconsistent segment size in " + node + ": " + segment);
            }
            int listSize = list.size();
            for (int i = 0; i < actualSize; i++) {
                if (segment.startIndex + i < listSize) {
                    list.set(segment.startIndex + i, seglist.get(i));
                } else {
                    list.add(seglist.get(i));
                }
                // FIXME how to handle inconsistencies?
            }
            segment.loaded = true;
            return seglist;
        }
        return Collections.EMPTY_LIST;
    }

    protected synchronized void update() {
        if (!hasRelationalNodes()) {
            segments = null;
            super.update();
            return;
        }
        // also reload if the type mapping has changed.
        long lastChange = getLastSubnodeChange();
        if (lastChange != lastSubnodeFetch) {
            // count nodes in db without fetching anything
            subnodeCount = node.nmgr.countNodes(node, getSubnodeRelation());
            if (subnodeCount > SEGLENGTH) {
                float size = subnodeCount;
                int nsegments = (int) Math.ceil(size / SEGLENGTH);
                int remainder = (int) size % SEGLENGTH;
                segments = new Segment[nsegments];
                for (int s = 0; s < nsegments; s++) {
                    int length = (s == nsegments - 1 && remainder > 0) ?
                        remainder : SEGLENGTH;
                    segments[s] = new Segment(s * SEGLENGTH, length);
                }
                list = new ArrayList((int) size + 5);
                for (int i = 0; i < size; i++) {
                    list.add(null);
                }
            } else {
                segments = null;
                super.update();
            }
            lastSubnodeFetch = lastChange;
        }
    }

    public int size() {
        if (!hasRelationalNodes() || segments == null) {
            return super.size();
        }
        return subnodeCount;
    }

    class Segment {

        int startIndex, length;
        boolean loaded;

        Segment(int startIndex, int length) {
            this.startIndex = startIndex;
            this.length = length;
            this.loaded = false;
        }

        int endIndex() {
            return startIndex + length;
        }

        public String toString() {
            return "Segment{startIndex: " + startIndex + ", length: " + length + "}";
        }
    }

}

