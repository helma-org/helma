// NodeEvent.java
// Copyright (c) Hannes Wallnöfer 1998-2000

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

    public NodeEvent (INode node, int type) {
	super ();
	this.node = node;
	this.id = node.getID ();
	this.type = type;
    }

    public NodeEvent (INode node, int type, Object arg) {
	super ();
	this.node = node;
	this.id = node.getID ();
	this.type = type;
	this.arg = arg;
    }

    public String toString () {
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