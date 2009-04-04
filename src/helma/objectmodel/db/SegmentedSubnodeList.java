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

import java.util.BitSet;
import java.util.List;

public class SegmentedSubnodeList extends SubnodeList {

    transient BitSet loadmap;
    transient int loadstatus;
    transient List[] keys;

    transient long lastSubnodeCount = 0; // these two are only used
    transient int subnodeCount = -1; // for aggressive loading relational subnodes

    /**
     * Creates a new subnode list
     * @param node the node we belong to
     */
    public SegmentedSubnodeList(Node node) {
        super(node); 
    }

    public int size() {
        // If the subnodes are loaded aggressively, we really just
        // do a count statement, otherwise we just return the size of the id index.
        // (after loading it, if it's coming from a relational data source).
        DbMapping dbmap = getSubnodeMapping();
        Relation rel = getSubnodeRelation();

        if (dbmap == null || !dbmap.isRelational() || rel.getGroup() != null) {
            return super.size();
        }

        if (node.getSubnodeRelation() == null &&
                (node.getState() == Node.TRANSIENT || node.getState() == Node.NEW)) {
            return super.size();
        }

        // we don't want to load *all* nodes if we just want to count them
        long lastChange = getLastSubnodeChange();

        if (lastChange == lastSubnodeFetch) {
            // we can use the nodes vector to determine number of subnodes
            subnodeCount = list.size();
            lastSubnodeCount = lastChange;
        } else if (lastChange != lastSubnodeCount || subnodeCount < 0) {
            // count nodes in db without fetching anything
            subnodeCount = node.nmgr.countNodes(node, rel);
            lastSubnodeCount = lastChange;
        }
        return subnodeCount;
    }

}

