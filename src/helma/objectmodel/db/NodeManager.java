// NodeManager.java
// Copyright (c) Hannes Wallnöfer 1998-2000

package helma.objectmodel.db;

import java.io.*;
import java.util.Vector;
import java.util.Properties;
import helma.util.CacheMap;
import helma.objectmodel.*;
import helma.framework.core.Application;
import com.sleepycat.db.*;
import java.sql.*;
import java.util.Vector;
import java.util.Enumeration;
import com.workingdogs.village.*;

/**
 * The NodeManager is responsible for fetching Nodes from the internal or 
 * external data sources, caching them in a least-recently-used Hashtable, 
 * and writing changes back to the databases.
 */
 
public final class NodeManager {

    private Application app;

    private CacheMap cache;

    protected DbWrapper db;

    protected IDGenerator idgen;

    private long idBaseValue = 1l;

    private boolean logSql;

    // a wrapper that catches some Exceptions while accessing this NM
    public final WrappedNodeManager safe;

    public NodeManager (Application app, String dbHome, Properties props) throws DbException {
	this.app = app;
	int cacheSize = Integer.parseInt (props.getProperty ("cachesize", "1000"));
	// Make actual cache size bigger, since we use it only up to the threshold
	cache = new CacheMap ((int) Math.ceil (cacheSize/0.75f), 0.75f);
	IServer.getLogger().log ("set up node cache ("+cacheSize+")");

	safe = new WrappedNodeManager (this);

	// get the initial id generator value
	String idb = props.getProperty ("idBaseValue");
	if (idb != null) try {
	    idBaseValue = Long.parseLong (idb);
	    idBaseValue = Math.max (1l, idBaseValue); // 0 and 1 are reserved for root nodes
	} catch (NumberFormatException ignore) {}

	db = new DbWrapper (dbHome, Server.dbFilename, Server.useTransactions);
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
	        db.save (txn, node.getID (), node);
	        registerNode (node); // register node with nodemanager cache
	    }

	    try {
	        node = db.getNode (txn, "1");
	        node.nmgr = safe;
	    } catch (ObjectNotFoundException notfound) {
	        node = new Node ("users", "1", null, safe);
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

   /*  private synchronized Node storeNodeInCache (Node n, Key k, Key pk) {

    } */

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


    public Node getNode (String kstr, DbMapping dbmap) throws Exception {

	if (kstr == null)
	    return null;

	Transactor tx = (Transactor) Thread.currentThread ();
	// tx.timer.beginEvent ("getNode "+kstr);
	Key key = tx.key;
	key.recycle (dbmap, kstr);

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
	    node = getNodeByKey (db, tx.txn, kstr, dbmap);
	    key.recycle (dbmap, kstr);
	    if (node != null) {
	        synchronized (cache) {
	            Node oldnode = (Node) cache.put (node.getKey (), node);
	            if (oldnode != null && oldnode.getState () != Node.INVALID) {
	                cache.put (node.getKey (), oldnode);
	                node = oldnode;
	            }
	        }  // synchronized
	    }
	}

	if (node != null) {
	    tx.visitCleanNode (key.duplicate(), node);
	}

	// tx.timer.endEvent ("getNode "+kstr);
	return node;
    }


    public Node getNode (Node home, String kstr, Relation rel) throws Exception {

	if (kstr == null)
	    return null;

	Transactor tx = (Transactor) Thread.currentThread ();
	// tx.timer.beginEvent ("getNode "+kstr);

	Key key = tx.key;
	// If what we want is a virtual node create a "synthetic" key
	if (rel.virtual  || rel.groupby != null)
	    key.recycle (null,  home.getKey ().getVirtualID (kstr));
	// if a key for a node from within the DB
	else
	    key.recycle (rel.other, rel.getKeyID (home, kstr));

	// See if Transactor has already come across this node
	Node node = tx.getVisitedNode (key);

	if (node != null && node.getState() != Node.INVALID) {
	    // tx.timer.endEvent ("getNode "+kstr);

	    // if we didn't fetch the node via its primary key, refresh the primary key in the cache.
	    // otherwise we risk cache corroption (duplicate node creation) if the node is fetched by its primary key
	    if (!rel.usesPrimaryKey ()) {
	        synchronized (cache) {
	            Node oldnode = (Node) cache.put (node.getKey (), node);
	            if (oldnode != null && oldnode.getState () != Node.INVALID) {
	                cache.put (node.getKey (), oldnode);
	                cache.put (key.duplicate(), oldnode);
	                node = oldnode;
	            }
	        }
	    }
	    return node;
	}

	// try to get the node from the shared cache
	node = (Node) cache.get (key);

	if (node == null || node.getState() == Node.INVALID) {

	    // The requested node isn't in the shared cache. Synchronize with key to make sure only one
	    // version is fetched from the database.

	    node = getNodeByRelation (db, tx.txn, home, kstr, rel);

	    // HACK: on some occasions (groupby nodes), transactor.key is used recursively
	    // so re-initializing it here is the safest thing to do.
	    if (rel.virtual  || rel.groupby != null)
	        key.recycle (null,  home.getKey ().getVirtualID (kstr));
	    else
	        key.recycle (rel.other, rel.getKeyID (home, kstr));

	    if (node != null) {

	        Key primKey = node.getKey ();
	        boolean keyIsPrimary = primKey.equals (key);
	        synchronized (cache) {
	            // check if node is already in cache with primary key
	            Node oldnode = (Node) cache.put (primKey, node);
	            if (oldnode != null && oldnode.getState () != Node.INVALID) {
	                cache.put (primKey, oldnode);
	                if (!keyIsPrimary)
	                    cache.put (key.duplicate(), oldnode);
	                node = oldnode;
	            } else if (!keyIsPrimary) // cache node with secondary key
	                cache.put (key.duplicate(), node);
	        } // synchronized
	    }
	} else {
	    // update primary key in cache, see above
	    if (!rel.usesPrimaryKey ()) {
	        synchronized (cache) {
	            Node oldnode = (Node) cache.put (node.getKey (), node);
	            if (oldnode != null && oldnode.getState () != Node.INVALID) {
	                cache.put (node.getKey (), oldnode);
	                cache.put (key.duplicate(), oldnode);
	                node = oldnode;
	            }
	        }
	    }
	}

	if (node != null) {
	    tx.visitCleanNode (key.duplicate(), node);
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
	    IServer.getLogger().log ("inserting relational node: "+node.getID ());
	    TableDataSet tds = null;
	    try {
	        tds = new TableDataSet (dbm.getConnection (), dbm.getSchema (), dbm.getKeyDef ());
	        Record rec = tds.addRecord ();
	        rec.setValue (dbm.getIDField (), node.getID ());

	        String nameField = dbm.getNameField ();
	        if (nameField != null)
	            rec.setValue (nameField, node.getName ());

	        for (Enumeration e=dbm.prop2db.keys(); e.hasMoreElements(); ) {
	            String propname = (String) e.nextElement ();
	            Property p = node.getProperty (propname, false);
	            Relation rel = dbm.propertyToColumnName (propname);

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
	                        if (rel.direction == Relation.FORWARD) {
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
	        rec.markForInsert ();
	        tds.save ();
	    } finally {
	        if (tds != null) {
	            tds.close ();
	        }
	    }
	    dbm.lastDataChange = System.currentTimeMillis ();
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

	        for (Enumeration e=dbm.prop2db.keys(); e.hasMoreElements(); ) {
	            String propname = (String) e.nextElement ();
	            Property p = node.getProperty (propname, false);
	            Relation rel = dbm.propertyToColumnName (propname);
	            if (rel != null && rel.readonly)
	                continue;

	            if (p != null && rel != null) {

	                if (p.dirty) {
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
	                            if (rel.direction == Relation.FORWARD) {
	                                // INode n = p.getNodeValue ();
	                                // String foreignID = n == null ? null : n.getID ();
	                                rec.setValue (rel.getDbField(), p.getStringValue ());
	                            }
	                            break;
	                    }
	                    updated++;
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
	    dbm.lastDataChange = System.currentTimeMillis ();
	}
	// update may cause changes in the node's parent subnode array
	if (node.isAnonymous()) {
	    Node parent = (Node) node.getParent ();
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
	    dbm.lastDataChange = System.currentTimeMillis ();
	}
	// node may still be cached via non-primary keys. mark as invalid
	node.setState (Node.INVALID);
	// tx.timer.endEvent ("deleteNode "+node);
    }

    /**
     * Generates an ID for the table by finding out the maximum current value
     */
    public String generateSQLID (DbMapping map) throws Exception {

	Transactor tx = (Transactor) Thread.currentThread ();
	// tx.timer.beginEvent ("generateID "+map);

	QueryDataSet qds = null;
	String retval = null;
	try {
	    Connection con = map.getConnection ();
	    String q = "SELECT MAX("+map.getIDField()+") FROM "+map.getTableName();
	    qds = new QueryDataSet (con, q);
	    qds.fetchRecords ();
	    long currMax = qds.getRecord (0).getValue (1).asLong ();
	    retval = Long.toString (currMax+1);
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
    public Vector getNodeIDs (Node home, Relation rel) throws Exception {

	Transactor tx = (Transactor) Thread.currentThread ();
	// tx.timer.beginEvent ("getNodeIDs "+home);

	if (rel == null || rel.other == null || !rel.other.isRelational ()) {
	    // this should never be called for embedded nodes
	    throw new RuntimeException ("NodeMgr.countNodes called for non-relational node "+home);
	} else {
	    Vector retval = new Vector ();
	    // if we do a groupby query (creating an intermediate layer of groupby nodes),
	    // retrieve the value of that field instead of the primary key
	    String idfield = rel.groupby == null ? rel.other.getIDField () : rel.groupby;
	    Connection con = rel.other.getConnection ();
	    String table = rel.other.getTableName ();

	    QueryDataSet qds = null;
	    try {
	        Relation subrel = rel;
	        if (subrel.getFilter () != null)
	            subrel = subrel.getFilter ();

	        if (home.getSubnodeRelation() != null) {
	            // subnode relation was explicitly set
	            qds = new QueryDataSet (con, "SELECT "+idfield+" FROM "+table+" "+home.getSubnodeRelation());
	        } else {
	            String q = "SELECT "+idfield+" FROM "+table;
	            if (subrel.direction == Relation.BACKWARD) {
	                String homeid = home.getNonVirtualHomeID (); //  home.getState() == Node.VIRTUAL ? home.parentID : home.getID ();
	                q += " WHERE "+subrel.getRemoteField()+" = '"+homeid+"'";
	            }
	            // set order, if specified and if not using subnode's relation
	            if (rel.groupby != null)
	                q += " GROUP BY "+rel.groupby+" ORDER BY "+(rel.groupbyorder == null ? rel.groupby : rel.groupbyorder);
	            else if (rel.order != null)
	                q += " ORDER BY "+rel.order;
	            qds = new QueryDataSet (con, q);
	        }

	        if (logSql)
	           IServer.getLogger().log ("### getNodeIDs: "+qds.getSelectString());

	        qds.fetchRecords ();
	        for (int i=0; i<qds.size (); i++) {
	            Record rec = qds.getRecord (i);
	            retval.addElement (rec.getValue (1).asString ());
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
    public Vector getNodes (Node home, Relation rel) throws Exception {

	Transactor tx = (Transactor) Thread.currentThread ();
	// tx.timer.beginEvent ("getNodes "+home);

	if (rel == null || rel.other == null || !rel.other.isRelational ()) {
	    // this should never be called for embedded nodes
	    throw new RuntimeException ("NodeMgr.countNodes called for non-relational node "+home);
	} else {
	    Vector retval = new Vector ();
	    DbMapping dbm = rel.other;

	    TableDataSet tds =  new TableDataSet (dbm.getConnection (), dbm.getSchema (), dbm.getKeyDef ());
	    try {
	        Relation subrel = rel;
	        if (subrel.getFilter () != null)
	            subrel = subrel.getFilter ();

	        if (home.getSubnodeRelation() != null) {
	            // HACK: subnodeRelation includes a "where", but we need it without
	            tds.where (home.getSubnodeRelation().trim().substring(5));
	        } else if (subrel.direction == Relation.BACKWARD) {
	            String homeid = home.getState() == Node.VIRTUAL ? home.parentID : home.getID ();
	            tds.where (subrel.getRemoteField()+" = '"+homeid+"'");
	            // set order if specified
	            if (rel.order != null)
	                 tds.order (rel.order);
	        } else {
	            //  don't set where clause, but set order.
	            if (rel.order != null)
	                 tds.order (rel.order);
	        }

	        if (logSql)
	           IServer.getLogger().log ("### getNodes: "+tds.getSelectString());

	        tds.fetchRecords ();
	        for (int i=0; i<tds.size (); i++) {
	            // create new Nodes.
	            Record rec = tds.getRecord (i);
	            Node node = new Node (rel.other, rec, safe);
	            retval.addElement (node.getID());
	            Key primKey = node.getKey ();
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

	if (rel == null || rel.other == null || !rel.other.isRelational ()) {
	    // this should never be called for embedded nodes
	    throw new RuntimeException ("NodeMgr.countNodes called for non-relational node "+home);
	} else {
	    int retval = 0;
	    Connection con = rel.other.getConnection ();
	    String table = rel.other.getTableName ();

	    QueryDataSet qds = null;
	    try {
	        Relation subrel = rel;
	        if (subrel.getFilter () != null)
	            subrel = subrel.getFilter ();

	        if (home.getSubnodeRelation() != null) {
	            qds = new QueryDataSet (con, "SELECT count(*) FROM "+table+" "+home.getSubnodeRelation());
	        } else if (subrel.direction == Relation.BACKWARD) {
	            String homeid = home.getState() == Node.VIRTUAL ? home.parentID : home.getID ();
	            qds = new QueryDataSet (con, "SELECT count(*) FROM "+table+" WHERE "+subrel.getRemoteField()+" = '"+homeid+"'");
	        } else {
	            qds = new QueryDataSet (con, "SELECT count(*) FROM "+table);
	        }

	        if (logSql)
	            IServer.getLogger().log ("### countNodes: "+qds.getSelectString());

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

	if (rel == null || rel.other == null || !rel.other.isRelational ()) {
	    // this should never be called for embedded nodes
	    throw new RuntimeException ("NodeMgr.countNodes called for non-relational node "+home);
	} else {
	    Vector retval = new Vector ();
	    // if we do a groupby query (creating an intermediate layer of groupby nodes),
	    // retrieve the value of that field instead of the primary key
	    String namefield = rel.getRemoteField ();
	    Connection con = rel.other.getConnection ();
	    String table = rel.other.getTableName ();

	    QueryDataSet qds = null;

	    try {
	        String q = "SELECT "+namefield+" FROM "+table+" ORDER BY "+namefield;
	        qds = new QueryDataSet (con, q);

	        if (logSql)
	           IServer.getLogger().log ("### getPropertyNames: "+qds.getSelectString());

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

    private Node getNodeByKey (DbWrapper db, DbTxn txn, String kstr, DbMapping dbm) throws Exception {
	Node node = null;
	if (dbm == null || !dbm.isRelational ()) {
	    node = db.getNode (txn, kstr);
	    node.nmgr = safe;
	    if (node != null && dbm != null)
	        node.setDbMapping (dbm);
	} else {
	    TableDataSet tds = null;
	    try {
	        tds = new TableDataSet (dbm.getConnection (), dbm.getSchema (), dbm.getKeyDef ());
	        tds.where (dbm.getIDField ()+" = '"+kstr+"'");

	        if (logSql)
	            IServer.getLogger().log ("### getNodeByKey: "+tds.getSelectString());

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

    private Node getNodeByRelation (DbWrapper db, DbTxn txn, Node home, String kstr, Relation rel) throws Exception {

	Node node = null;

	if (rel != null && rel.virtual && home.getState() != INode.VIRTUAL) {
	    Key k = home.getKey ().getVirtualKey (kstr);
	    node = (Node) cache.get (k);
	    if (node != null && node.getState() != INode.INVALID) {
	        if (rel.prototype != null && !rel.prototype.equals (node.getPrototype ()))
	            node.setPrototype (rel.prototype);
	        return node;
	    }

	    // if subnodes are stored in embedded db we have to actually assign it the virtual node,
	    // otherwise it and its subnodes will be lost across restarts.
	    if (rel.other == null || (!rel.other.isRelational() && !home.getDbMapping().isRelational())) {
	        node = (Node) home.createNode (rel.propname);
	        if (rel.prototype != null)
	            node.setPrototype (rel.prototype);
	    } else {
	        node = new Node (home, kstr, safe, rel.prototype);
	    }
	    if (rel.prototype != null) {
	        node.setDbMapping (app.getDbMapping (rel.prototype));
	    } else {
	        // make a db mapping good enough that the virtual node finds its subnodes
	        DbMapping dbm = new DbMapping ();
	        dbm.setSubnodeMapping (rel.other);
	        dbm.setSubnodeRelation (rel.getVirtualSubnodeRelation());
	        dbm.setPropertyMapping (rel.other);
	        dbm.setPropertyRelation (rel.getVirtualPropertyRelation());
	        node.setDbMapping (dbm);
	    }

	} else if (rel != null && rel.groupby != null) {
	    node = home.getGroupbySubnode (kstr, false);
	    if (node == null && (rel.other == null || !rel.other.isRelational ())) {
	        node = db.getNode (txn, kstr);
	        node.nmgr = safe;
	    }
	    return node;

	} else if (rel == null || rel.other == null || !rel.other.isRelational ()) {
	    node = db.getNode (txn, kstr);
	    node.nmgr = safe;
	    node.setDbMapping (rel.other);
	    return node;

	} else {
	    TableDataSet tds = null;
	    try {
	        DbMapping dbm = rel.other;
	        tds = new TableDataSet (dbm.getConnection (), dbm.getSchema (), dbm.getKeyDef ());
	        StringBuffer where = new StringBuffer ();

	        where.append (rel.getRemoteField ());
	        where.append (" = '");
	        where.append (escape(kstr));
	        where.append ("'");

	        // Additionally filter properties through subnode relation?
	        if (rel.subnodesAreProperties) {
	            String homeid = home.getState() == Node.VIRTUAL ? home.parentID : home.getID ();
	            // first check for dynamic subrel from node
	            String nodesubrel = home.getSubnodeRelation();
	            if (nodesubrel != null && nodesubrel.trim().length() > 5) {
	                where.append (" and ");
	                where.append (nodesubrel.trim().substring(5).trim());
	            } else {
	                Relation subrel = home.getDbMapping().getSubnodeRelation ();
	                if (subrel != null) {
	                    if (subrel.getFilter () != null)
	                        subrel = subrel.getFilter ();
	                    if (subrel != null && subrel.direction == Relation.BACKWARD) {
	                        where.append (" and ");
	                        where.append (subrel.getRemoteField());
	                        where.append (" = '");
	                        where.append (homeid);
	                        where.append ("'");
	                    }
	                }
	            }
	        }
	
	        tds.where (where.toString ());

	        if (logSql)
	            IServer.getLogger().log ("### getNodeByRelation: "+tds.getSelectString());

	        tds.fetchRecords ();

	        if (tds.size () == 0)
	            return null;
	        if (tds.size () > 1)
	            throw new RuntimeException ("More than one value returned by query.");
	        Record rec = tds.getRecord (0);
	        node = new Node (rel.other, rec, safe);

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


}































































































































































