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

package helma.objectmodel;

import java.io.*;

/**
 * This is passed to NodeListeners when a node is modified.
 */
public class NodeEvent implements Serializable {
    public static final int CONTENT_CHANGED = 0;
    public static final int PROPERTIES_CHANGED = 1;
    public static final int NODE_REMOVED = 2;
    public static final int NODE_RENAMED = 3;
    public static final int SUBNODE_ADDED = 4;
    public static final int SUBNODE_REMOVED = 5;
    public int type;
    public String id;
    public transient INode node;
    public transient Object arg;

    /**
     * Creates a new NodeEvent object.
     *
     * @param node ...
     * @param type ...
     */
    public NodeEvent(INode node, int type) {
        super();
        this.node = node;
        this.id = node.getID();
        this.type = type;
    }

    /**
     * Creates a new NodeEvent object.
     *
     * @param node ...
     * @param type ...
     * @param arg ...
     */
    public NodeEvent(INode node, int type, Object arg) {
        super();
        this.node = node;
        this.id = node.getID();
        this.type = type;
        this.arg = arg;
    }

    /**
     *
     *
     * @return ...
     */
    public String toString() {
        switch (type) {
            case CONTENT_CHANGED:
                return "NodeEvent: content changed";

            case PROPERTIES_CHANGED:
                return "NodeEvent: properties changed";

            case NODE_REMOVED:
                return "NodeEvent: node removed";

            case NODE_RENAMED:
                return "NodeEvent: node moved";

            case SUBNODE_ADDED:
                return "NodeEvent: subnode added";

            case SUBNODE_REMOVED:
                return "NodeEvent: subnode removed";
        }

        return "NodeEvent: invalid type";
    }
}
