// WrappedNodeManager.java
// Copyright (c) Hannes Wallnöfer 1998-2000
 
package helma.objectmodel.db;

import helma.objectmodel.*;
import java.util.List;
import java.util.Vector;


/**
 * A wrapper around NodeManager that catches most Exceptions, or rethrows them as RuntimeExceptions.
 * The idea behind this is that we don't care a lot about Exception classes, since Hop programming is done
 * in JavaScript which doesn't know about them (except for the exception message).
 */
 
 public final class WrappedNodeManager {

    NodeManager nmgr;

    public WrappedNodeManager (NodeManager nmgr) {
	this.nmgr = nmgr;
    }

    public Node getNode (String id, DbMapping dbmap) {
	return getNode (new DbKey (dbmap, id));
    }

    public Node getNode (Key key) {
	try {
	    return nmgr.getNode (key);
	} catch (ObjectNotFoundException x) {
	    return null;
	} catch (Exception x) {
	    nmgr.app.logEvent ("Error retrieving Node via DbMapping: "+x);
	    if (nmgr.app.debug ())
	        x.printStackTrace();
	    throw new RuntimeException ("Error retrieving Node: "+x);
	}
    }

    public Node getNode (Node home, String id, Relation rel) {
	try {
	    return nmgr.getNode (home, id, rel);
	} catch (ObjectNotFoundException x) {
	    return null;
	} catch (Exception x) {
	    nmgr.app.logEvent ("Error retrieving Node \""+id+"\" from "+home+": "+x);
	    if (nmgr.app.debug ())
	        x.printStackTrace();
	    throw new RuntimeException ("Error retrieving Node: "+x);
	}
    }

    public List getNodes (Node home, Relation rel) {
	try {
	    return nmgr.getNodes (home, rel);
	} catch (Exception x) {
	    if (nmgr.app.debug ())
	        x.printStackTrace();
	    throw new RuntimeException ("Error retrieving Nodes: "+x);
	}
    }

    public List getNodeIDs (Node home, Relation rel) {
	try {
	    return nmgr.getNodeIDs (home, rel);
	} catch (Exception x) {
	    if (nmgr.app.debug ())
	        x.printStackTrace();
	    throw new RuntimeException ("Error retrieving NodeIDs: "+x);
	}
    }

    public int countNodes (Node home, Relation rel) {
	try {
	    return nmgr.countNodes (home, rel);
	} catch (Exception x) {
	    if (nmgr.app.debug ())
	        x.printStackTrace();
	    throw new RuntimeException ("Error counting Node: "+x);
	}
    }

    public void deleteNode (Node node) {
	try {
	    nmgr.deleteNode (node);
	} catch (Exception x) {
	    if (nmgr.app.debug ())
	        x.printStackTrace();
	    throw new RuntimeException ("Error deleting Node: "+x);
	}
    }

    public Vector getPropertyNames (Node home, Relation rel) {
	try {
	    return nmgr.getPropertyNames (home, rel);
	} catch (Exception x) {
	    if (nmgr.app.debug ())
	        x.printStackTrace();
	    throw new RuntimeException ("Error retrieving property names: "+x);
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
	    // check if we use internal id generator
	    if (map == null || !map.isRelational () || "[hop]".equalsIgnoreCase (map.getIDgen()))
	        return nmgr.idgen.newID ();
	    // or if we query max key value
	    else if (map.getIDgen() == null || "[max]".equalsIgnoreCase (map.getIDgen()))
	        return nmgr.generateMaxID (map);
	    else
	        return nmgr.generateID (map);
	        // otherwise, we use an oracle sequence
	} catch (Exception x) {
	    if (nmgr.app.debug ())
	        x.printStackTrace();
	    throw new RuntimeException (x.toString ());
	}
    }

    public Object[] getCacheEntries () {
	return nmgr.getCacheEntries ();
    }

    public void logEvent (String msg) {
	nmgr.app.logEvent (msg);
    }

    public DbMapping getDbMapping (String name) {
	return nmgr.app.getDbMapping (name);
    }

}
