// NodeManager.java
// Copyright (c) Hannes Wallnöfer 1998-2000

package helma.objectmodel.db;

import helma.util.CacheMap;
import helma.objectmodel.*;
import helma.framework.core.Application;
import com.sleepycat.db.*;
import java.sql.*;
import java.io.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Vector;
import java.util.Enumeration;
import com.workingdogs.village.*;

/**
 * The NodeManager is responsible for fetching Nodes from the internal or 
 * external data sources, caching them in a least-recently-used Hashtable, 
 * and writing changes back to the databases.
 */
 
public final class NodeManager {

    protected Application app;

    private CacheMap cache;
    private Replicator replicator;

    protected DbWrapper db;

    protected IDGenerator idgen;

    private long idBaseValue = 1l;

    private boolean logSql;

    // a wrapper that catches some Exceptions while accessing this NM
    public final WrappedNodeManager safe;

    // an instance of Node that's used to cache null values
    // private Node nullNode;

    public NodeManager (Application app, String dbHome, Properties props) throws DbException {
	this.app = app;
	int cacheSize = Integer.parseInt (props.getProperty ("cachesize", "1000"));
	// Make actual cache size bigger, since we use it only up to the threshold
	// cache = new CacheMap ((int) Math.ceil (cacheSize/0.75f), 0.75f);
	cache = new CacheMap (cacheSize, 0.75f);
	app.logEvent ("set up node cache ("+cacheSize+")");

	safe = new WrappedNodeManager (this);
	// nullNode = new Node ();

	String replicationUrl = props.getProperty ("replicationUrl");
	if (replicationUrl != null) {
	    replicator = new Replicator ();
	    replicator.addUrl (replicationUrl);
	} else
	    replicator = null;

	// get the initial id generator value
	String idb = props.getProperty ("idBaseValue");
	if (idb != null) try {
	    idBaseValue = Long.parseLong (idb);
	    idBaseValue = Math.max (1l, idBaseValue); // 0 and 1 are reserved for root nodes
	} catch (NumberFormatException ignore) {}

	db = new DbWrapper (dbHome, Server.dbFilename, this, Server.useTransactions);
	initDb ();

	logSql = "true".equalsIgnoreCase(props.getProperty ("logsql"));
    }

   /**
    * Method used to create the root node and id-generator, if they don't exist already.
    */
    public void initDb () throws DbException {

	DbTxn txn = null;
	try {
	    txn = db.beginTransaction ();

	    try {
	        idgen = db.getIDGenerator (txn, "idgen");
	        if (idgen.getValue() < idBaseValue) {
	            idgen.setValue (idBaseValue);
	            db.save (txn, "idgen", idgen);
	        }
	    } catch (ObjectNotFoundException notfound) {
	        // will start with idBaseValue+1
	        idgen = new IDGenerator (idBaseValue);
	        db.save (txn, "idgen", idgen);
	    }

	    // check if we need to set the id generator to a base value
	
	    Node node = null;
	    try {
	        node = db.getNode (txn, "0");
	        node.nmgr = safe;
	    } catch (ObjectNotFoundException notfound) {
	        node = new Node ("root", "0", "root", safe);
	        node.setDbMapping (app.getDbMapping ("root"));
	        db.save (txn, node.getID (), node);
	        registerNode (node); // register node with nodemanager cache
	    }

	    try {
	        node = db.getNode (txn, "1");
	        node.nmgr = safe;
	    } catch (ObjectNotFoundException notfound) {
	        node = new Node ("users", "1", null, safe);
	        node.setDbMapping (app.getDbMapping ("__userroot__"));
	        db.save (txn, node.getID (), node);
	        registerNode (node); // register node with nodemanager cache
	    }

	    db.commitTransaction (txn);
	} catch (Exception x) {
	    System.err.println (">> "+x);
	    x.printStackTrace ();
	    try {
	        db.abortTransaction (txn);
	    } catch (Exception ignore) {}
	    throw (new DbException ("Error initializing db"));
	}
    }


    public void shutdown () throws DbException {
	db.shutdown ();
	this.cache = null;
    }

    public void deleteNode (Node node) throws Exception {
	if (node != null) {
	    String id = node.getID ();
	    synchronized (this) {
	        Transactor tx = (Transactor) Thread.currentThread ();
	        node.setState (Node.INVALID);
	        deleteNode (db, tx.txn, node);
	    }
	}
    }	


    public Node getNode (Key key) throws Exception {

	Transactor tx = (Transactor) Thread.currentThread ();
	// tx.timer.beginEvent ("getNode "+kstr);

	// it would be a good idea to reuse key objects one day.
	// Key key = new Key (dbmap, kstr);

	// See if Transactor has already come across this node
	Node node = tx.getVisitedNode (key);

	if (node != null && node.getState() != Node.INVALID) {
	    // tx.timer.endEvent ("getNode "+kstr);
	    return node;
	}

	// try to get the node from the shared cache
	node = (Node) cache.get (key);
	if (node == null || node.getState() == Node.INVALID) {

	    // The requested node isn't in the shared cache. Synchronize with key to make sure only one
	    // version is fetched from the database.
	    if (key instanceof SyntheticKey) {
	        Node parent = getNode (key.getParentKey ());
	        Relation rel = parent.dbmap.getPropertyRelation (key.getID());
	        if (rel == null || rel.groupby != null)
	            node = parent.getGroupbySubnode (key.getID (), true);
	        else if (rel != null)
	            node = getNode (parent, key.getID (), rel);
	        else
	            node = null;
	    } else
	        node = getNodeByKey (tx.txn, (DbKey) key);
	
	    if (node != null) {
	        synchronized (cache) {
	            Node oldnode = (Node) cache.put (node.getKey (), node);
	            if (oldnode != null && !oldnode.isNullNode() && oldnode.getState () != Node.INVALID) {
	                cache.put (node.getKey (), oldnode);
	                node = oldnode;
	            }
	        }  // synchronized
	    }
	}

	if (node != null) {
	    tx.visitCleanNode (key, node);
	}

	// tx.timer.endEvent ("getNode "+kstr);
	return node;
    }


    public Node getNode (Node home, String kstr, Relation rel) throws Exception {

	if (kstr == null)
	    return null;

	Transactor tx = (Transactor) Thread.currentThread ();

	Key key = null;
	// check what kind of object we're looking for and make an apropriate key
	if (rel.virtual || rel.groupby != null  || !rel.usesPrimaryKey())
	    // a key for a virtually defined object that's never actually  stored in the db
	    // or a key for an object that represents subobjects grouped by some property, generated on the fly
	    key = new SyntheticKey (home.getKey (), kstr);
	else
	    // if a key for a node from within the DB
	    // FIXME: This should never apply, since for every relation-based loading Synthetic Keys are used. Right?
	    key = new DbKey (rel.otherType, kstr);
	
	// See if Transactor has already come across this node
	Node node = tx.getVisitedNode (key);

	if (node != null && node.getState() != Node.INVALID) {
	    // we used to refresh the node in the main cache here to avoid the primary key entry being
	    // flushed from cache before the secondary one (risking duplicate nodes in cache) but
	    // we don't need to since we fetched the node from the threadlocal transactor cache and
	    // didn't refresh it in the main cache.
	    return node;
	}

	// try to get the node from the shared cache
	node = (Node) cache.get (key);

	// check if we can use the cached node without further checks.
	// we need further checks for subnodes fetched by name if the subnodes were changed.
	if (!rel.virtual && !rel.usesPrimaryKey () && node != null && node.getState() != Node.INVALID) {
	    // check if node is null node (cached null)
	    if (node.isNullNode ()) {
	        if (node.created() < rel.otherType.getLastDataChange ())
	            node = null; //  cached null not valid anymore
	    // } else if (app.doesSubnodeChecking () && home.contains (node) < 0) {
	    } else if (!rel.checkConstraints (home, node)) {
	        node = null;
	    }
	}

	if (node == null || node.getState() == Node.INVALID) {

	    // The requested node isn't in the shared cache. Synchronize with key to make sure only one
	    // version is fetched from the database.
	    node = getNodeByRelation (tx.txn, home, kstr, rel);

	    if (node != null) {

	        Key primKey = node.getKey ();
	        boolean keyIsPrimary = primKey.equals (key);
	        synchronized (cache) {
	            // check if node is already in cache with primary key
	            Node oldnode = (Node) cache.put (primKey, node);
	            // no need to check for oldnode != node because we fetched a new node from db
	            if (oldnode != null && !oldnode.isNullNode() && oldnode.getState () != Node.INVALID) {
	                cache.put (primKey, oldnode);
	                if (!keyIsPrimary) {
	                    cache.put (key, oldnode);
	                }
	                node = oldnode;
	            } else if (!keyIsPrimary) {
	                // cache node with secondary key
	                cache.put (key, node);
	            }
	        } // synchronized
	    } else {
	        // node fetched from db is null, cache result using nullNode
	        synchronized (cache) {
	            Node oldnode = (Node) cache.put (key, new Node ());
	            // we ignore the case that onother thread has created the node in the meantime
	            return null;
	        }
	    }
	} else if (node.isNullNode ()) {
	    // the nullNode caches a null value, i.e. an object that doesn't exist
	    return null;
	} else {
	    // update primary key in cache to keep it from being flushed, see above
	    if (!rel.usesPrimaryKey ()) {
	        synchronized (cache) {
	            Node oldnode = (Node) cache.put (node.getKey (), node);
	            if (oldnode != node && oldnode != null && oldnode.getState () != Node.INVALID) {
	                cache.put (node.getKey (), oldnode);
	                cache.put (key, oldnode);
	                node = oldnode;
	            }
	        }
	    }
	}

	if (node != null) {
	    tx.visitCleanNode (key, node);
	}

	// tx.timer.endEvent ("getNode "+kstr);
	return node;
    }


    public void registerNode (Node node) {
	cache.put  (node.getKey (), node);
    }


    public void evictNode (Node node) {
	node.setState (INode.INVALID);
	cache.remove (node.getKey ());
    }

    /**
     * Used when a key stops being valid for a node.
     */
    public void evictKey (Key key) {
	cache.remove (key);
    }


    ////////////////////////////////////////////////////////////////////////
    // methods to do the actual db work
    ////////////////////////////////////////////////////////////////////////


    public void insertNode (DbWrapper db, DbTxn txn, Node node) throws Exception {

	Transactor tx = (Transactor) Thread.currentThread ();
	// tx.timer.beginEvent ("insertNode "+node);

	DbMapping dbm = node.getDbMapping ();

	if (dbm == null || !dbm.isRelational ()) {
	    db.save (txn, node.getID (), node);
	} else {
	    app.logEvent ("inserting relational node: "+node.getID ());
	    TableDataSet tds = null;
	    try {
	        tds = new TableDataSet (dbm.getConnection (), dbm.getSchema (), dbm.getKeyDef ());
	        Record rec = tds.addRecord ();
	        rec.setValue (dbm.getIDField (), node.getID ());

	        String nameField = dbm.getNameField ();
	        if (nameField != null)
	            rec.setValue (nameField, node.getName ());

	        for (Enumeration e=dbm.getProp2DB ().keys(); e.hasMoreElements(); ) {
	            String propname = (String) e.nextElement ();
	            Property p = node.getProperty (propname, false);
	            Relation rel = dbm.propertyToRelation (propname);

	            if (p != null && rel != null) {
	                switch (p.getType ()) {
	                    case IProperty.STRING:
	                        rec.setValue (rel.getDbField(), p.getStringValue ());
	                        break;
	                    case IProperty.BOOLEAN:
	                        rec.setValue (rel.getDbField(), p.getBooleanValue ());
	                        break;
	                    case IProperty.DATE:
	                        Timestamp t = new Timestamp (p.getDateValue ().getTime ());
	                        rec.setValue (rel.getDbField(), t);
	                        break;
	                    case IProperty.INTEGER:
	                        rec.setValue (rel.getDbField(), p.getIntegerValue ());
	                        break;
	                    case IProperty.FLOAT:
	                        rec.setValue (rel.getDbField(), p.getFloatValue ());
	                        break;
	                    case IProperty.NODE:
	                        if (rel.reftype == Relation.REFERENCE) {
	                            // INode n = p.getNodeValue ();
	                            // String foreignID = n == null ? null : n.getID ();
	                            rec.setValue (rel.getDbField(), p.getStringValue ());
	                        }
	                        break;
	                }
	                p.dirty = false;
	            } else if (rel != null && rel.getDbField() != null) {
	                rec.setValueNull (rel.getDbField());
	            }
	        }
	
	        if (dbm.getPrototypeField () != null) {
	            rec.setValue (dbm.getPrototypeField (), node.getPrototype ());
	        }
	        rec.markForInsert ();
	        tds.save ();
	    } finally {
	        if (tds != null) {
	            tds.close ();
	        }
	    }
	    dbm.notifyDataChange ();
	}
	// tx.timer.endEvent ("insertNode "+node);
    }

    public void updateNode (DbWrapper db, DbTxn txn, Node node) throws Exception {

	Transactor tx = (Transactor) Thread.currentThread ();
	// tx.timer.beginEvent ("updateNode "+node);

	DbMapping dbm = node.getDbMapping ();

	if (dbm == null || !dbm.isRelational ()) {
	    db.save (txn, node.getID (), node);
	} else {

	    TableDataSet tds = null;
	    try {
	        tds = new TableDataSet (dbm.getConnection (), dbm.getSchema (), dbm.getKeyDef ());
	        Record rec = tds.addRecord ();
	        rec.setValue (dbm.getIDField (), node.getID ());

	        int updated = 0;

	        for (Enumeration e=dbm.getProp2DB ().keys(); e.hasMoreElements(); ) {
	            String propname = (String) e.nextElement ();
	            Relation rel = dbm.propertyToRelation (propname);

	            // skip properties that don't need to be updated before fetching them
	            if (rel != null && (rel.readonly || rel.virtual ||
	            		(rel.reftype != Relation.REFERENCE && rel.reftype != Relation.PRIMITIVE)))
	                continue;

	            Property p = node.getProperty (propname, false);

	            if (p != null && rel != null) {

	                if (p.dirty) {
	                    switch (p.getType ()) {
	                        case IProperty.STRING:
	                            updated++;
	                            rec.setValue (rel.getDbField(), p.getStringValue ());
	                            break;
	                        case IProperty.BOOLEAN:
	                            updated++;
	                            rec.setValue (rel.getDbField(), p.getBooleanValue ());
	                            break;
	                        case IProperty.DATE:
	                            updated++;
	                            Timestamp t = new Timestamp (p.getDateValue ().getTime ());
	                            rec.setValue (rel.getDbField(), t);
	                            break;
	                        case IProperty.INTEGER:
	                            updated++;
	                            rec.setValue (rel.getDbField(), p.getIntegerValue ());
	                            break;
	                        case IProperty.FLOAT:
	                            updated++;
	                            rec.setValue (rel.getDbField(), p.getFloatValue ());
	                            break;
	                        case IProperty.NODE:
	                            if (!rel.virtual && rel.reftype == Relation.REFERENCE) {
	                                // INode n = p.getNodeValue ();
	                                // String foreignID = n == null ? null : n.getID ();
	                                updated++;
	                                rec.setValue (rel.getDbField(), p.getStringValue ());
	                            }
	                            break;
	                    }
	                    p.dirty = false;
	                }

	            } else if (rel != null && rel.getDbField() != null) {

	                updated++;
	                rec.setValueNull (rel.getDbField());
	            }
	        }
	        if (updated > 0) {
	            // mark the key value as clean so no try is made to update it
	            rec.markValueClean (dbm.getIDField ());
	            rec.markForUpdate ();
	            tds.save ();
	        }
	    } finally {
	        if (tds != null) {
	            tds.close ();
	        }
	    }
	    dbm.notifyDataChange ();
	}
	// update may cause changes in the node's parent subnode array
	if (node.isAnonymous()) {
	    Node parent = node.getCachedParent ();
	    if (parent != null)
	        parent.lastSubnodeChange = System.currentTimeMillis ();
	}
	// tx.timer.endEvent ("updateNode "+node);
    }

    public void deleteNode (DbWrapper db, DbTxn txn, Node node) throws Exception {

	Transactor tx = (Transactor) Thread.currentThread ();
	// tx.timer.beginEvent ("deleteNode "+node);

	DbMapping dbm = node.getDbMapping ();

	if (dbm == null || !dbm.isRelational ()) {
	    db.delete (txn, node.getID ());
	} else {
	    Statement st = null;
	    try {
	        Connection con = dbm.getConnection ();
	        st = con.createStatement ();
	        st.executeUpdate ("DELETE FROM "+dbm.getTableName ()+" WHERE "+dbm.getIDField ()+" = "+node.getID ());
	    } finally {
	        if (st != null)
	            st.close ();
	    }
	    dbm.notifyDataChange ();
	}
	// node may still be cached via non-primary keys. mark as invalid
	node.setState (Node.INVALID);
	// tx.timer.endEvent ("deleteNode "+node);
    }

    /**
     * Generates an ID for the table by finding out the maximum current value
     */
    public synchronized String generateMaxID (DbMapping map) throws Exception {

	Transactor tx = (Transactor) Thread.currentThread ();
	// tx.timer.beginEvent ("generateID "+map);

	QueryDataSet qds = null;
	String retval = null;
	try {
	    Connection con = map.getConnection ();
	    String q = "SELECT MAX("+map.getIDField()+") FROM "+map.getTableName();
	    qds = new QueryDataSet (con, q);
	    qds.fetchRecords ();
	    // check for empty table
	    if (qds.size () == 0)
	        return "0";
	    long currMax = qds.getRecord (0).getValue (1).asLong ();
	    currMax = map.getNewID (currMax);
	    retval = Long.toString (currMax);
	} finally {
	    // tx.timer.endEvent ("generateID "+map);
	    if (qds != null) {
	        qds.close ();
	    }
	}
	return retval;
    }


    public String generateID (DbMapping map) throws Exception {

	Transactor tx = (Transactor) Thread.currentThread ();
	// tx.timer.beginEvent ("generateID "+map);

	QueryDataSet qds = null;
	String retval = null;
	try {
	    Connection con = map.getConnection ();
	    String q = "SELECT "+map.getIDgen()+".nextval FROM dual";
	    qds = new QueryDataSet (con, q);
	    qds.fetchRecords ();
                 retval = qds.getRecord (0).getValue (1).asString ();
	} finally {
	    // tx.timer.endEvent ("generateID "+map);
	    if (qds != null) {
	        qds.close ();
	    }
	}
	return retval;
    }


    /**
     *  Loades subnodes via subnode relation. Only the ID index is loaded, the nodes are
     *  loaded later on demand.
     */
    public List getNodeIDs (Node home, Relation rel) throws Exception {

	Transactor tx = (Transactor) Thread.currentThread ();
	// tx.timer.beginEvent ("getNodeIDs "+home);

	if (rel == null || rel.otherType == null || !rel.otherType.isRelational ()) {
	    // this should never be called for embedded nodes
	    throw new RuntimeException ("NodeMgr.getNodeIDs called for non-relational node "+home);
	} else {
	    List retval = new ArrayList ();
	    // if we do a groupby query (creating an intermediate layer of groupby nodes),
	    // retrieve the value of that field instead of the primary key
	    String idfield = rel.groupby == null ? rel.otherType.getIDField () : rel.groupby;
	    Connection con = rel.otherType.getConnection ();
	    String table = rel.otherType.getTableName ();

	    QueryDataSet qds = null;
	    try {
	
	        String q = null;
	
	        if (home.getSubnodeRelation() != null) {
	            // subnode relation was explicitly set
	            q = "SELECT "+idfield+" FROM "+table+" "+home.getSubnodeRelation();
	        } else {
	            // let relation object build the query
	            q = "SELECT "+idfield+" FROM "+table + rel.buildQuery (home, home.getNonVirtualParent (), null);
	        }

	        if (logSql)
	           app.logEvent ("### getNodeIDs: "+q);

	        qds = new QueryDataSet (con, q);
	
	        qds.fetchRecords ();
	
	        // problem: how do we derive a SyntheticKey from a not-yet-persistent Node?
	        Key k = rel.groupby != null ?  home.getKey (): null;
	        for (int i=0; i<qds.size (); i++) {
	            Record rec = qds.getRecord (i);
	            String kstr = rec.getValue (1).asString ();
	            // make the proper key for the object, either a generic DB key or a groupby key
	            Key key = rel.groupby == null ?
	            		(Key) new DbKey (rel.otherType, kstr) :
	            		(Key) new SyntheticKey (k, kstr);
	            retval.add (new NodeHandle (key));
	            // if these are groupby nodes, evict nullNode keys
	            if (rel.groupby != null) {
	                Node n = (Node) cache.get (key);
	                if (n != null && n.isNullNode ())
	                    evictKey (key);
	            }
	        }

	    } finally {
	        // tx.timer.endEvent ("getNodeIDs "+home);
	        if (qds != null) {
	            qds.close ();
	        }
	    }
	    return retval;
	}
    }

    /**
     *  Loades subnodes via subnode relation. This is similar to getNodeIDs, but it
     *  actually loades all nodes in one go, which is better for small node collections.
     *  This method is used when xxx.loadmode=aggressive is specified.
     */
    public List getNodes (Node home, Relation rel) throws Exception {

	// This does not apply for groupby nodes - use getNodeIDs instead
	if (rel.groupby != null)
	    return getNodeIDs (home, rel);

	Transactor tx = (Transactor) Thread.currentThread ();
	// tx.timer.beginEvent ("getNodes "+home);

	if (rel == null || rel.otherType == null || !rel.otherType.isRelational ()) {
	    // this should never be called for embedded nodes
	    throw new RuntimeException ("NodeMgr.countNodes called for non-relational node "+home);
	} else {
	    List retval = new ArrayList ();
	    DbMapping dbm = rel.otherType;

	    TableDataSet tds =  new TableDataSet (dbm.getConnection (), dbm.getSchema (), dbm.getKeyDef ());
	    try {

	        tds.where (rel.buildWhere (home, home.getNonVirtualParent (), null));
	        if (rel.order != null)
	            tds.order (rel.order);
	
	        if (logSql)
	           app.logEvent ("### getNodes: "+tds.getSelectString());

	        tds.fetchRecords ();
	        for (int i=0; i<tds.size (); i++) {
	            // create new Nodes.
	            Record rec = tds.getRecord (i);
	            Node node = new Node (rel.otherType, rec, safe);
	            Key primKey = node.getKey ();
	            retval.add (new NodeHandle (primKey));
	            // do we need to synchronize on primKey here?
	            synchronized (cache) {
	                Node oldnode = (Node) cache.put (primKey, node);
	                if (oldnode != null && oldnode.getState() != INode.INVALID) {
	                    cache.put (primKey, oldnode);
	                }
	            }
	        }

	    } finally {
	        // tx.timer.endEvent ("getNodes "+home);
	        if (tds != null) {
	            tds.close ();
	        }
	    }
	    return retval;
	}
    }


    public int countNodes (Node home, Relation rel) throws Exception {

	Transactor tx = (Transactor) Thread.currentThread ();
	// tx.timer.beginEvent ("countNodes "+home);

	if (rel == null || rel.otherType == null || !rel.otherType.isRelational ()) {
	    // this should never be called for embedded nodes
	    throw new RuntimeException ("NodeMgr.countNodes called for non-relational node "+home);
	} else {
	    int retval = 0;
	    Connection con = rel.otherType.getConnection ();
	    String table = rel.otherType.getTableName ();

	    QueryDataSet qds = null;
	    try {
	        String qstr = "SELECT count(*) FROM "+table+rel.buildQuery (home, home.getNonVirtualParent (), null);
	
	        if (logSql)
	            app.logEvent ("### countNodes: "+qstr);
	
	        qds = new QueryDataSet (con, qstr);

	        qds.fetchRecords ();
	        if (qds.size () == 0)
	            retval = 0;
	        else
	            retval = qds.getRecord (0).getValue (1).asInt ();

	    } finally {
	        // tx.timer.endEvent ("countNodes "+home);
	        if (qds != null) {
	            qds.close ();
	        }
	    }
	    return retval;
	}
    }

    /**
     *  Similar to getNodeIDs, but returns a Vector that return's the nodes property names instead of IDs
     */
    public Vector getPropertyNames (Node home, Relation rel) throws Exception {

	Transactor tx = (Transactor) Thread.currentThread ();
	// tx.timer.beginEvent ("getNodeIDs "+home);

	if (rel == null || rel.otherType == null || !rel.otherType.isRelational ()) {
	    // this should never be called for embedded nodes
	    throw new RuntimeException ("NodeMgr.getPropertyNames called for non-relational node "+home);
	} else {
	    Vector retval = new Vector ();
	    // if we do a groupby query (creating an intermediate layer of groupby nodes),
	    // retrieve the value of that field instead of the primary key
	    String namefield = rel.accessor;
	    Connection con = rel.otherType.getConnection ();
	    String table = rel.otherType.getTableName ();

	    QueryDataSet qds = null;

	    try {
	        String q = "SELECT "+namefield+" FROM "+table+" ORDER BY "+namefield;
	        qds = new QueryDataSet (con, q);

	        if (logSql)
	           app.logEvent ("### getPropertyNames: "+qds.getSelectString());

	        qds.fetchRecords ();
	        for (int i=0; i<qds.size (); i++) {
	            Record rec = qds.getRecord (i);
	            String n = rec.getValue (1).asString ();
	            if (n != null)
	                retval.addElement (n);
	        }

	    } finally {
	        // tx.timer.endEvent ("getNodeIDs "+home);
	        if (qds != null) {
	            qds.close ();
	        }
	    }
	    return retval;
	}
    }


    ///////////////////////////////////////////////////////////////////////////////////////
    // private getNode methods
    ///////////////////////////////////////////////////////////////////////////////////////

    private Node getNodeByKey (DbTxn txn, DbKey key) throws Exception {
	// Note: Key must be a DbKey, otherwise will not work for relational objects
	Node node = null;
	DbMapping dbm = app.getDbMapping (key.getStorageName ());
	String kstr = key.getID ();
	
	if (dbm == null || !dbm.isRelational ()) {
	    node = db.getNode (txn, kstr);
	    node.nmgr = safe;
	    if (node != null && dbm != null)
	        node.setDbMapping (dbm);
	} else {
	    String idfield =dbm.getIDField ();
	
	    TableDataSet tds = null;
	    try {
	        tds = new TableDataSet (dbm.getConnection (), dbm.getSchema (), dbm.getKeyDef ());
	        tds.where (idfield+" = '"+kstr+"'");

	        if (logSql)
	            app.logEvent ("### getNodeByKey: "+tds.getSelectString());

	        tds.fetchRecords ();

	        if (tds.size () == 0)
	            return null;
	        if (tds.size () > 1)
	            throw new RuntimeException ("More than one value returned by query.");
	        Record rec = tds.getRecord (0);
	        node = new Node (dbm, rec, safe);

	    } finally {
	        if (tds != null) {
	            tds.close ();
	        }
	    }
	}
	return node;
    }

    private Node getNodeByRelation (DbTxn txn, Node home, String kstr, Relation rel) throws Exception {
	Node node = null;

	if (rel.virtual) {

	    node = new Node (home, kstr, safe, rel.prototype);
	
	    if (rel.prototype != null) {
	        node.setPrototype (rel.prototype);
	        node.setDbMapping (app.getDbMapping (rel.prototype));
	    } else {
	        node.setDbMapping (rel.getVirtualMapping ());
	    }

	} else if (rel != null && rel.groupby != null) {
	    node = home.getGroupbySubnode (kstr, false);
	    if (node == null && (rel.otherType == null || !rel.otherType.isRelational ())) {
	        node = db.getNode (txn, kstr);
	        node.nmgr = safe;
	    }
	    return node;

	} else if (rel == null || rel.otherType == null || !rel.otherType.isRelational ()) {
	    node = db.getNode (txn, kstr);
	    node.nmgr = safe;
	    node.setDbMapping (rel.otherType);
	    return node;

	} else {
	    System.err.println ("GETNODEBYRELATION "+rel);
	    TableDataSet tds = null;
	    try {
	        DbMapping dbm = rel.otherType;
	        tds = new TableDataSet (dbm.getConnection (), dbm.getSchema (), dbm.getKeyDef ());
	        String where = rel.buildWhere (home, home.getNonVirtualParent (), kstr);

	        tds.where (where);

	        if (logSql)
	            app.logEvent ("### getNodeByRelation: "+tds.getSelectString());

	        tds.fetchRecords ();

	        if (tds.size () == 0)
	            return null;
	        if (tds.size () > 1)
	            throw new RuntimeException ("More than one value returned by query.");
	        Record rec = tds.getRecord (0);
	        node = new Node (rel.otherType, rec, safe);

	        // Check if node is already cached with primary Key.
	        if (!rel.usesPrimaryKey()) {
	            Key pk = node.getKey();
	            Node existing = (Node) cache.get (pk);
	            if (existing != null && existing.getState() != Node.INVALID) {
	                node = existing;
	            }
	        }

	    } finally {
	        if (tds != null) {
	            tds.close ();
	        }
	    }
	}
	return node;
    }

    // a utility method to escape single quotes
    private String escape (String str) {
	if (str == null)
	    return null;
	if (str.indexOf ("'") < 0)
	    return str;
	int l = str.length();
	StringBuffer sbuf = new StringBuffer (l + 10);
	for (int i=0; i<l; i++) {
	    char c = str.charAt (i);
	    if (c == '\'')
	        sbuf.append ("\\");
	    sbuf.append (c);
	}
	return sbuf.toString ();
    }


    public Object[] getCacheEntries () {
	return cache.getEntryArray ();
    }

    protected Replicator getReplicator () {
	return replicator;
    }

    public void registerReplicatedApp (helma.framework.IReplicatedApp rapp) {
	if (replicator == null)
	    replicator = new Replicator ();
	replicator.addApp (rapp);
    }
	
    public void replicateCache (Vector add, Vector delete) {
	synchronized (cache) {
	    for (Enumeration en=add.elements(); en.hasMoreElements(); ) {
	        Node n = (Node) en.nextElement ();
	        DbMapping dbm = app.getDbMapping (n.getPrototype ());
	        if (dbm != null)
	            dbm.notifyDataChange ();
	        n.lastParentSet = -1;
	        n.setDbMapping (dbm);
	        n.nmgr = safe;
	        cache.put (n.getKey(), n);
	    }
	    for (Enumeration en=delete.elements(); en.hasMoreElements(); ) {
	        Node n = (Node) en.nextElement ();
	        DbMapping dbm = app.getDbMapping (n.getPrototype ());
	        if (dbm != null)
	            dbm.notifyDataChange ();
	        n.setDbMapping (dbm);
	        n.nmgr = safe;
	        cache.put (n.getKey(), n);
	        evictNode (n);
	    }
	}
    }

}































































































































































