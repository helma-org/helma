// WrappedNodeManager.java
// Copyright (c) Hannes Wallnöfer 1998-2000
 
package helma.objectmodel.db;

import helma.objectmodel.*;
import java.util.Vector;


/**
 * A wrapper around NodeManager that catches most Exceptions, or rethrows them as RuntimeExceptions.
 * The idea behind this is that we don't care a lot about Exception classes, since Hop programming is done
 * in JavaScript which doesn't know about them (except for the exception message).
 */
 
 public class WrappedNodeManager {

    NodeManager nmgr;

    public WrappedNodeManager (NodeManager nmgr) {
	this.nmgr = nmgr;
    }

    public Node getNode (String id, DbMapping dbmap) {
	try {
	    return nmgr.getNode (id, dbmap);
	} catch (ObjectNotFoundException x) {
	    return null;
	} catch (Exception x) {
	    Server.getLogger().log ("Error retrieving Node via DbMapping: "+x.getMessage ());
	    if ("true".equalsIgnoreCase (Server.sysProps.getProperty("debug")))
	        x.printStackTrace();
	    throw new RuntimeException ("Error retrieving Node: "+x.getMessage ());
	}
    }

    public Node getNode (Node home, String id, Relation rel) {
	try {
	    return nmgr.getNode (home, id, rel);
	} catch (ObjectNotFoundException x) {
	    return null;
	} catch (Exception x) {
	    Server.getLogger().log ("Error retrieving Node \""+id+"\" from "+home+": "+x.getMessage ());
	    if ("true".equalsIgnoreCase (Server.sysProps.getProperty("debug")))
	        x.printStackTrace();
	    throw new RuntimeException ("Error retrieving Node: "+x.getMessage ());
	}
    }

    public Vector getNodes (Node home, Relation rel) {
	try {
	    return nmgr.getNodes (home, rel);
	} catch (Exception x) {
	    if ("true".equalsIgnoreCase (Server.sysProps.getProperty("debug")))
	        x.printStackTrace();
	    throw new RuntimeException ("Error retrieving Nodes: "+x.getMessage ());
	}
    }

    public Vector getNodeIDs (Node home, Relation rel) {
	try {
	    return nmgr.getNodeIDs (home, rel);
	} catch (Exception x) {
	    if ("true".equalsIgnoreCase (Server.sysProps.getProperty("debug")))
	        x.printStackTrace();
	    throw new RuntimeException ("Error retrieving NodeIDs: "+x.getMessage ());
	}
    }

    public int countNodes (Node home, Relation rel) {
	try {
	    return nmgr.countNodes (home, rel);
	} catch (Exception x) {
	    if ("true".equalsIgnoreCase (Server.sysProps.getProperty("debug")))
	        x.printStackTrace();
	    throw new RuntimeException ("Error counting Node: "+x.getMessage ());
	}
    }

    public void deleteNode (Node node) {
	try {
	    nmgr.deleteNode (node);
	} catch (Exception x) {
	    if ("true".equalsIgnoreCase (Server.sysProps.getProperty("debug")))
	        x.printStackTrace();
	    throw new RuntimeException ("Error deleting Node: "+x.getMessage ());
	}
    }

    public void registerNode (Node node) {
	nmgr.registerNode (node);
    }

    public void evictNode (Node node) {
	nmgr.evictNode (node);
    }

    public void evictKey (Key key) {
	nmgr.evictKey (key);
    }


    public String generateID () {
	return nmgr.idgen.newID ();
    }

    public String generateID (DbMapping map) {
	try {
	    if (map == null || map.getIDgen() == null)
	        return nmgr.idgen.newID ();
	    else
	        return nmgr.generateID (map);
	} catch (Exception x) {
	    throw new RuntimeException (x.toString ());
	}
    }

}




































































