// NodeManager.java
// Copyright (c) Hannes Wallnöfer 1998-2000

package helma.objectmodel.db;

import helma.util.CacheMap;
import helma.objectmodel.*;
import helma.framework.core.Application;
import java.sql.*;
import java.io.*;
import java.util.*;
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

    protected IDatabase db;

    protected IDGenerator idgen;

    private long idBaseValue = 1l;

    private boolean logSql;
    protected boolean logReplication;

    // a wrapper that catches some Exceptions while accessing this NM
    public final WrappedNodeManager safe;


    /**
    *  Create a new NodeManager for Application app. An embedded database will be
    * created in dbHome if one doesn't already exist.
    */
    public NodeManager (Application app, String dbHome, Properties props) throws DatabaseException {
	this.app = app;
	int cacheSize = Integer.parseInt (props.getProperty ("cachesize", "1000"));
	// Make actual cache size bigger, since we use it only up to the threshold
	// cache = new CacheMap ((int) Math.ceil (cacheSize/0.75f), 0.75f);
	cache = new CacheMap (cacheSize, 0.75f);
	cache.setLogger (app.getLogger ("event"));
	app.logEvent ("Setting up node cache ("+cacheSize+")");

	safe = new WrappedNodeManager (this);
	// nullNode = new Node ();

	logSql = "true".equalsIgnoreCase(props.getProperty ("logsql"));
	logReplication = "true".equalsIgnoreCase(props.getProperty ("logReplication"));


	String replicationUrl = props.getProperty ("replicationUrl");
	if (replicationUrl != null) {
	    if (logReplication)
	        app.logEvent ("Setting up replication listener at "+replicationUrl);
	    replicator = new Replicator (this);
	    replicator.addUrl (replicationUrl);
	} else {
	    replicator = null;
	}

	// get the initial id generator value
	String idb = props.getProperty ("idBaseValue");
	if (idb != null) try {
	    idBaseValue = Long.parseLong (idb);
	    idBaseValue = Math.max (1l, idBaseValue); // 0 and 1 are reserved for root nodes
	} catch (NumberFormatException ignore) {}

	db = new XmlDatabase (dbHome, null, this);
	initDb ();
    }

    /**
     *  app.properties file has been updated. Reread some settings.
     */
    public void updateProperties (Properties props) {
	int cacheSize = Integer.parseInt (props.getProperty ("cachesize", "1000"));
	cache.setCapacity (cacheSize);
	logSql = "true".equalsIgnoreCase(props.getProperty ("logsql"));
	logReplication = "true".equalsIgnoreCase(props.getProperty ("logReplication"));
    }

   /**
    * Method used to create the root node and id-generator, if they don't exist already.
    */
    public void initDb () throws DatabaseException {

	ITransaction txn = null;
	try {
	    txn = db.beginTransaction ();

	    try {
	        idgen = db.getIDGenerator (txn);
	        if (idgen.getValue() < idBaseValue) {
	            idgen.setValue (idBaseValue);
	            db.saveIDGenerator (txn, idgen);
	        }
	    } catch (ObjectNotFoundException notfound) {
	        // will start with idBaseValue+1
	        idgen = new IDGenerator (idBaseValue);
	        db.saveIDGenerator (txn, idgen);
	    }

	    // check if we need to set the id generator to a base value
	
	    Node node = null;
	    try {
	        node = (Node)db.getNode (txn, "0");
	        node.nmgr = safe;
	    } catch (ObjectNotFoundException notfound) {
	        node = new Node ("root", "0", "root", safe);
	        node.setDbMapping (app.getDbMapping ("root"));
	        db.saveNode (txn, node.getID (), node);
	        registerNode (node); // register node with nodemanager cache
	    }

	    try {
	        node = (Node)db.getNode (txn, "1");
	        node.nmgr = safe;
	    } catch (ObjectNotFoundException notfound) {
	        node = new Node ("users", "1", null, safe);
	        node.setDbMapping (app.getDbMapping ("__userroot__"));
	        db.saveNode (txn, node.getID (), node);
	        registerNode (node); // register node with nodemanager cache
	    }

	    db.commitTransaction (txn);
	} catch (Exception x) {
	    System.err.println (">> "+x);
	    x.printStackTrace ();
	    try {
	        db.abortTransaction (txn);
	    } catch (Exception ignore) {}
	    throw (new DatabaseException ("Error initializing db"));
	}
    }


    /**
    *  Shut down this node manager. This is called when the application using this
    *  node manager is stopped.
    */
    public void shutdown () throws DatabaseException {
	db.shutdown ();
	if (cache != null) {
	    synchronized (cache) {
	        cache.clear ();
	        cache = null;
	    }
	}
    }

    /**
    *  Delete a node from the database.
    */
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


    /**
    *  Get a node by key. This is called from a node that already holds
    *  a reference to another node via a NodeHandle/Key.
    */
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


    /**
    *  Get a node by relation, using the home node, the relation and a key to apply.
    *  In contrast to getNode (Key key), this is usually called when we don't yet know
    *  whether such a node exists.
    */
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
	if (node != null && node.getState() != Node.INVALID) {
	    // check if node is null node (cached null)
	    if (node.isNullNode ()) {
	        if (node.created < rel.otherType.getLastDataChange () ||
	            node.created < rel.ownType.getLastTypeChange())
	            node = null; //  cached null not valid anymore
	    } else if (!rel.virtual) {
	        // apply different consistency checks for groupby nodes and database nodes:
	        // for group nodes, check if they're contained
	        if (rel.groupby != null) {
	            if (home.contains (node) < 0)
	                node = null;
	        // for database nodes, check if constraints are fulfilled
	        } else if (!rel.usesPrimaryKey ()) {
	            if (!rel.checkConstraints (home, node))
	                node = null;
	        }
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
	                // reset create time of old node, otherwise Relation.checkConstraints
	                // will reject it under certain circumstances.
	                oldnode.created = oldnode.lastmodified;
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

    /**
    * Register a node in the node cache.
    */
    public void registerNode (Node node) {
	cache.put  (node.getKey (), node);
    }

    /**
    * Remove a node from the node cache. If at a later time it is  accessed again, it will be
    * refetched from the database.
    */
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


    /**
    *  Insert a new node in the embedded database or a relational database table, depending
    * on its db mapping.
    */
    public void insertNode (IDatabase db, ITransaction txn, Node node) throws Exception {

	// Transactor tx = (Transactor) Thread.currentThread ();
	// tx.timer.beginEvent ("insertNode "+node);

	DbMapping dbm = node.getDbMapping ();

	if (dbm == null || !dbm.isRelational ()) {
	    db.saveNode (txn, node.getID (), node);
	} else {
	    // app.logEvent ("inserting relational node: "+node.getID ());
	    TableDataSet tds = null;
	    try {
	        tds = new TableDataSet (dbm.getConnection (), dbm.getSchema (), dbm.getKeyDef ());
	        Record rec = tds.addRecord ();
	        rec.setValue (dbm.getIDField (), node.getID ());

	        String nameField = dbm.getNameField ();
	        if (nameField != null)
	            rec.setValue (nameField, node.getName ());

	        for (Iterator i=dbm.getProp2DB().entrySet().iterator(); i.hasNext(); ) {
	            Map.Entry e = (Map.Entry) i.next ();
	            String propname = (String) e.getKey ();
	            Relation rel = (Relation) e.getValue ();
	            Property p = node.getProperty (propname);

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
	        if (tds != null) try {
	            tds.close ();
	        } catch (Exception ignore) {}
	    }
	    dbm.notifyDataChange ();
	}
	// tx.timer.endEvent ("insertNode "+node);
    }

    /**
    *  Updates a modified node in the embedded db or an external relational database, depending
    * on its database mapping.
    */
    public void updateNode (IDatabase db, ITransaction txn, Node node) throws Exception {

	// Transactor tx = (Transactor) Thread.currentThread ();
	// tx.timer.beginEvent ("updateNode "+node);

	DbMapping dbm = node.getDbMapping ();
	boolean markMappingAsUpdated = false;

	if (dbm == null || !dbm.isRelational ()) {
	    db.saveNode (txn, node.getID (), node);
	} else {

	    TableDataSet tds = null;
	    try {
	        tds = new TableDataSet (dbm.getConnection (), dbm.getSchema (), dbm.getKeyDef ());
	        Record rec = tds.addRecord ();
	        rec.setValue (dbm.getIDField (), node.getID ());

	        int updated = 0;

	        for (Iterator i=dbm.getProp2DB().entrySet().iterator(); i.hasNext(); ) {
	            Map.Entry e = (Map.Entry) i.next ();
	            String propname = (String) e.getKey ();
	            Relation rel = (Relation) e.getValue ();

	            // skip properties that don't need to be updated before fetching them
	            if (rel != null && (rel.readonly || rel.virtual ||
	            		(rel.reftype != Relation.REFERENCE && rel.reftype != Relation.PRIMITIVE)))
	                continue;

	            Property p = node.getProperty (propname);

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
	                    if (!rel.isPrivate())
	                        markMappingAsUpdated = true;
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
	        if (tds != null) try {
	            tds.close ();
	        } catch (Exception ignore) {}
	    }
	    if (markMappingAsUpdated)
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

    /**
    *  Performs the actual deletion of a node from either the embedded or an external SQL database.
    */
    public void deleteNode (IDatabase db, ITransaction txn, Node node) throws Exception {

	// Transactor tx = (Transactor) Thread.currentThread ();
	// tx.timer.beginEvent ("deleteNode "+node);

	DbMapping dbm = node.getDbMapping ();

	if (dbm == null || !dbm.isRelational ()) {
	    db.deleteNode (txn, node.getID ());
	} else {
	    Statement st = null;
	    try {
	        Connection con = dbm.getConnection ();
	        st = con.createStatement ();
	        st.executeUpdate (new StringBuffer ("DELETE FROM ")
	            .append(dbm.getTableName ())
	            .append(" WHERE ")
	            .append(dbm.getIDField())
	            .append(" = ")
	            .append(node.getID())
	            .toString());
	    } finally {
	        if (st != null) try {
	            st.close ();
	        } catch (Exception ignore) {}
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

	// Transactor tx = (Transactor) Thread.currentThread ();
	// tx.timer.beginEvent ("generateID "+map);

	String retval = null;
	Statement stmt = null;
	try {
	    Connection con = map.getConnection ();
	    String q = new StringBuffer("SELECT MAX(")
	        .append(map.getIDField())
	        .append(") FROM ")
	        .append(map.getTableName())
	        .toString();
	    stmt = con.createStatement ();
	    ResultSet rs = stmt.executeQuery (q);
	    // check for empty table
	    if (!rs.next()) {
	        long currMax = map.getNewID (0);
	        retval = Long.toString (currMax);
	    } else {
	        long currMax = rs.getLong (1);
	        currMax = map.getNewID (currMax);
	        retval = Long.toString (currMax);
	    }
	} finally {
	    // tx.timer.endEvent ("generateID "+map);
	    if (stmt != null) try {
	        stmt.close ();
	    } catch (Exception ignore) {}
	}
	return retval;
    }


    /**
     * Generate a new ID from an Oracle sequence.
     */
    public String generateID (DbMapping map) throws Exception {

	// Transactor tx = (Transactor) Thread.currentThread ();
	// tx.timer.beginEvent ("generateID "+map);

	Statement stmt = null;
	String retval = null;
	try {
	    Connection con = map.getConnection ();
	    String q = new StringBuffer("SELECT ")
	        .append(map.getIDgen())
	        .append(".nextval FROM dual")
	        .toString();
	    stmt = con.createStatement();
	    ResultSet rs = stmt.executeQuery (q);
	    if (!rs.next ())
	        throw new SQLException ("Error creating ID from Sequence: empty recordset");
	    retval = rs.getString (1);
	} finally {
	    // tx.timer.endEvent ("generateID "+map);
	    if (stmt != null) try {
	        stmt.close ();
	    } catch (Exception ignore) {}
	}
	return retval;
    }


    /**
     *  Loades subnodes via subnode relation. Only the ID index is loaded, the nodes are
     *  loaded later on demand.
     */
    public List getNodeIDs (Node home, Relation rel) throws Exception {

	// Transactor tx = (Transactor) Thread.currentThread ();
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

	    Statement stmt = null;
	    try {

	        String q = null;

	        if (home.getSubnodeRelation() != null) {
	            // subnode relation was explicitly set
	            q = new StringBuffer("SELECT ")
	                .append(idfield)
	                .append(" FROM ")
	                .append(table)
	                .append(" ")
	                .append(home.getSubnodeRelation())
	                .toString();
	        } else {
	            // let relation object build the query
	            q = new StringBuffer("SELECT ")
	                .append(idfield)
	                .append(" FROM ")
	                .append(table)
	                .append(rel.buildQuery (home,
	                        home.getNonVirtualParent (), null,
	                        " WHERE ", true))
	                .toString();
	        }

	        if (logSql)
	           app.logEvent ("### getNodeIDs: "+q);

	        stmt = con.createStatement ();
	        if (rel.maxSize > 0)
	            stmt.setMaxRows (rel.maxSize);
	        ResultSet result = stmt.executeQuery (q);

	        // problem: how do we derive a SyntheticKey from a not-yet-persistent Node?
	        Key k = rel.groupby != null ?  home.getKey (): null;
	        while (result.next ()) {
	            String kstr = result.getString (1);
	            // jump over null values - this can happen especially when the selected
	            // column is a group-by column.
	            if (kstr == null)
	                continue;
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
	        if (stmt != null) try {
	            stmt.close ();
	        } catch (Exception ignore) {}
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

	// Transactor tx = (Transactor) Thread.currentThread ();
	// tx.timer.beginEvent ("getNodes "+home);

	if (rel == null || rel.otherType == null || !rel.otherType.isRelational ()) {
	    // this should never be called for embedded nodes
	    throw new RuntimeException ("NodeMgr.countNodes called for non-relational node "+home);
	} else {
	    List retval = new ArrayList ();
	    DbMapping dbm = rel.otherType;

	    Connection con = dbm.getConnection ();
	    Statement stmt = con.createStatement ();
	    DbColumn[] columns = dbm.getColumns ();
	    StringBuffer q = dbm.getSelect ();
	    try {
	        if (home.getSubnodeRelation() != null) {
	            q.append (home.getSubnodeRelation());
	        } else {
	            // let relation object build the query
	            q.append (rel.buildQuery (home, home.getNonVirtualParent (), null, " WHERE ", false));
	            if (rel.getOrder () != null)
	                q.append (" order by "+rel.getOrder ());
	        }

	        if (logSql)
	           app.logEvent ("### getNodes: "+q.toString());

	        if (rel.maxSize > 0)
	            stmt.setMaxRows (rel.maxSize);

	        ResultSet rs = stmt.executeQuery (q.toString ());

	        while (rs.next()) {
	            // create new Nodes.
	            Node node = new Node (rel.otherType, rs, columns, safe);
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
	        if (stmt != null)  try {
	            stmt.close ();
	        } catch (Exception ignore) {}
	    }
	    return retval;
	}
    }

    /**
     *
     */
    public void prefetchNodes (Node home, Relation rel, Key[] keys) throws Exception {

	DbMapping dbm = rel.otherType;
	if (dbm == null || !dbm.isRelational ()) {
	    // this does nothing for objects in the embedded database
	    return;
	} else {
	    int missing = cache.containsKeys (keys);
	    if (missing > 0) {
	        Connection con = dbm.getConnection ();
	        Statement stmt = con.createStatement ();
	        DbColumn[] columns = dbm.getColumns ();
	        StringBuffer q = dbm.getSelect ();
	        try {
	            String idfield = rel.groupby != null ? rel.groupby : dbm.getIDField ();
	            boolean needsQuotes = dbm.needsQuotes (idfield);
	            q.append ("WHERE ");
	            q.append (idfield);
	            q.append (" IN (");
	            boolean first = true;
	            for (int i=0; i<keys.length; i++) {
	                if (keys[i] != null) {
	                    if (!first)
	                        q.append (',');
	                    else
	                        first = false;
	                    if (needsQuotes) {
	                        q.append ("'");
	                        q.append (escape (keys[i].getID ()));
	                        q.append ("'");
	                    } else {
	                        q.append (keys[i].getID ());
	                    }
	                }
	            }
	            q.append (") ");
	            if (rel.groupby != null) {
	                q.append (rel.renderConstraints (home, home.getNonVirtualParent ()));
	                if (rel.order != null) {
	                    q.append (" ORDER BY ");
	                    q.append (rel.order);
	                }
	            }

	            if (logSql)
	               app.logEvent ("### prefetchNodes: "+q.toString());

	            ResultSet rs = stmt.executeQuery (q.toString());;

	            String groupbyProp = null;
	            HashMap groupbySubnodes = null;
	            if (rel.groupby != null) {
	                groupbyProp = dbm.columnNameToProperty (rel.groupby);
	                groupbySubnodes = new HashMap();
	            }

	            String accessProp = null;
	            if (rel.accessor != null && !rel.usesPrimaryKey ())
	                accessProp = dbm.columnNameToProperty (rel.accessor);

	            while (rs.next ()) {
	                // create new Nodes.
	                Node node = new Node (dbm, rs, columns, safe);
	                Key primKey = node.getKey ();

	                // for grouped nodes, collect subnode lists for the intermediary
	                // group nodes.
	                String groupName = null;
	                if (groupbyProp != null) {
	                    groupName = node.getString (groupbyProp);
	                    List sn = (List) groupbySubnodes.get (groupName);
	                    if (sn == null) {
	                        sn = new ExternalizableVector ();
	                        groupbySubnodes.put (groupName, sn);
	                    }
	                    sn.add (new NodeHandle (primKey));
	                }

	                // if relation doesn't use primary key as accessor, get accessor value
	                String accessName = null;
	                if (accessProp != null) {
	                    accessName = node.getString (accessProp);
	                }

	                // register new nodes with the cache. If an up-to-date copy
	                // existed in the cache, use that.
	                synchronized (cache) {
	                    Node oldnode = (Node) cache.put (primKey, node);
	                    if (oldnode != null && oldnode.getState() != INode.INVALID) {
	                        // found an ok version in the cache, use it.
	                        cache.put (primKey, oldnode);
	                    } else if (accessName != null) {
	                        // put the node into cache with its secondary key
	                        if (groupName != null)
	                            cache.put (new SyntheticKey (new SyntheticKey (home.getKey(), groupName), accessName), node);
	                        else
	                            cache.put (new SyntheticKey (home.getKey(), accessName), node);
	                    }
	                }
	            }
	            // If these are grouped nodes, build the intermediary group nodes
	            // with the subnod lists we created
	            if (groupbyProp != null) {
	                for (Iterator i=groupbySubnodes.keySet().iterator(); i.hasNext(); ) {
	                    String groupname = (String) i.next();
	                    if (groupname == null) continue;
	                    Node groupnode = home.getGroupbySubnode (groupname, true);
	                    cache.put (groupnode.getKey(), groupnode);
	                    groupnode.setSubnodes ((List) groupbySubnodes.get(groupname));
	                    groupnode.lastSubnodeFetch = System.currentTimeMillis ();
	                }
	            }
	        } finally {
	            if (stmt != null)  try {
	                stmt.close ();
	            } catch (Exception ignore) {}
	        }
	    }
	}
    }



    /**
     * Count the nodes contained in the child collection of the home node
     * which is defined by Relation rel.
     */
    public int countNodes (Node home, Relation rel) throws Exception {

	// Transactor tx = (Transactor) Thread.currentThread ();
	// tx.timer.beginEvent ("countNodes "+home);

	if (rel == null || rel.otherType == null || !rel.otherType.isRelational ()) {
	    // this should never be called for embedded nodes
	    throw new RuntimeException ("NodeMgr.countNodes called for non-relational node "+home);
	} else {
	    int retval = 0;
	    Connection con = rel.otherType.getConnection ();
	    String table = rel.otherType.getTableName ();

	    Statement stmt = null;
	    try {

	        String q = null;
	        if (home.getSubnodeRelation() != null) {
	            // use the manually set subnoderelation of the home node
	            q = new StringBuffer("SELECT count(*) FROM ")
	                .append(table)
	                .append(" ")
	                .append(home.getSubnodeRelation())
	                .toString();
	        } else {
	            // let relation object build the query
	            q = new StringBuffer("SELECT count(*) FROM ")
	                .append(table)
	                .append(rel.buildQuery (home, home.getNonVirtualParent(), 
	                        null, " WHERE ", false))
	                .toString();
	        }

	        if (logSql)
	            app.logEvent ("### countNodes: "+q);

	        stmt = con.createStatement();

	        ResultSet rs = stmt.executeQuery (q);
	        if (!rs.next())
	            retval = 0;
	        else
	            retval = rs.getInt (1);

	    } finally {
	        // tx.timer.endEvent ("countNodes "+home);
	        if (stmt != null) try {
	            stmt.close ();
	        } catch (Exception ignore) {}
	    }
	    return rel.maxSize > 0 ? Math.min (rel.maxSize, retval) : retval;
	}
    }

    /**
     *  Similar to getNodeIDs, but returns a Vector that return's the nodes property names instead of IDs
     */
    public Vector getPropertyNames (Node home, Relation rel) throws Exception {

	// Transactor tx = (Transactor) Thread.currentThread ();
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

	    Statement stmt = null;

	    try {
	        String q = new StringBuffer("SELECT ")
	            .append(namefield)
	            .append(" FROM ")
	            .append(table)
	            .append(" ORDER BY ")
	            .append(namefield)
	            .toString();
	        stmt = con.createStatement ();

	        if (logSql)
	           app.logEvent ("### getPropertyNames: "+q);

	        ResultSet rs = stmt.executeQuery (q);
	        while (rs.next()) {
	            String n = rs.getString (1);
	            if (n != null)
	                retval.addElement (n);
	        }

	    } finally {
	        // tx.timer.endEvent ("getNodeIDs "+home);
	        if (stmt != null) try {
	            stmt.close ();
	        } catch (Exception ignore) {}
	    }
	    return retval;
	}
    }


    ///////////////////////////////////////////////////////////////////////////////////////
    // private getNode methods
    ///////////////////////////////////////////////////////////////////////////////////////

    private Node getNodeByKey (ITransaction txn, DbKey key) throws Exception {
	// Note: Key must be a DbKey, otherwise will not work for relational objects
	Node node = null;
	DbMapping dbm = app.getDbMapping (key.getStorageName ());
	String kstr = key.getID ();

	if (dbm == null || !dbm.isRelational ()) {
	    node = (Node)db.getNode (txn, kstr);
	    node.nmgr = safe;
	    if (node != null && dbm != null)
	        node.setDbMapping (dbm);
	} else {
	    String idfield =dbm.getIDField ();

	    Statement stmt = null;
	    try {
	        Connection con = dbm.getConnection ();
	        stmt = con.createStatement ();

	        DbColumn[] columns = dbm.getColumns ();
	        StringBuffer q = dbm.getSelect ();
	        q.append ("WHERE ");
	        q.append (idfield);
	        q.append (" = ");
	        q.append (kstr);

	        if (logSql)
	            app.logEvent ("### getNodeByKey: "+q.toString());

	        ResultSet rs = stmt.executeQuery (q.toString());

	        if (!rs.next ())
	            return null;
	        node = new Node (dbm, rs, columns, safe);
	        if (rs.next ())
	            throw new RuntimeException ("More than one value returned by query.");

	    } finally {
	        if (stmt != null) try {
	            stmt.close ();
	        } catch (Exception ignore) {}
	    }
	}
	return node;
    }

    private Node getNodeByRelation (ITransaction txn, Node home, String kstr, Relation rel) throws Exception {
	Node node = null;

	if (rel != null && rel.virtual) {
	    if (rel.needsPersistence ())
	        node = (Node) home.createNode (kstr);
	    else
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
	        node = (Node)db.getNode (txn, kstr);
	        node.nmgr = safe;
	    }
	    return node;

	} else if (rel == null || rel.otherType == null || !rel.otherType.isRelational ()) {
	    node = (Node)db.getNode (txn, kstr);
	    node.nmgr = safe;
	    node.setDbMapping (rel.otherType);
	    return node;

	} else {
	    Statement stmt = null;
	    try {
	        DbMapping dbm = rel.otherType;

	        Connection con = dbm.getConnection ();
	        DbColumn[] columns = dbm.getColumns ();
	        StringBuffer q = dbm.getSelect ();
	        if (home.getSubnodeRelation () != null) {
	            // combine our key with the constraints in the manually set subnode relation
	            q.append ("WHERE ");
	            q.append (rel.accessor);
	            q.append (" = '");
	            q.append (escape(kstr));
	            q.append ("'");
	            q.append (" AND ");
                    q.append (home.getSubnodeRelation().trim().substring(5));
	        } else {
	            q.append (rel.buildQuery (home, home.getNonVirtualParent (), kstr, "WHERE ", false));
	        }
	        if (logSql)
	            app.logEvent ("### getNodeByRelation: "+q.toString());

	        stmt = con.createStatement ();
	        ResultSet rs = stmt.executeQuery (q.toString());

	        if (!rs.next ())
	            return null;
	        node = new Node (rel.otherType, rs, columns, safe);
	        if (rs.next ())
	            throw new RuntimeException ("More than one value returned by query.");

	        // Check if node is already cached with primary Key.
	        if (!rel.usesPrimaryKey()) {
	            Key pk = node.getKey();
	            Node existing = (Node) cache.get (pk);
	            if (existing != null && existing.getState() != Node.INVALID) {
	                node = existing;
	            }
	        }

	    } finally {
	        if (stmt != null) try {
	            stmt.close ();
	        } catch (Exception ignore) {}
	    }
	}
	return node;
    }

    /**
     * Get a DbMapping for a given prototype name. This is just a proxy
     * method to the app's getDbMapping() method.
     */
    public DbMapping getDbMapping (String protoname) {
	return app.getDbMapping (protoname);
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
                sbuf.append ('\'');
            sbuf.append (c);
        }
        return sbuf.toString ();
    }


    /**
     *  Get an array of the the keys currently held in the object cache
     */
    public Object[] getCacheEntries () {
	return cache.getEntryArray ();
    }

    /**
     * Get the number of elements in the object cache
     */
    public int countCacheEntries () {
    return cache.size ();
    }

    /**
    * Clear the object cache, causing all objects to be recreated.
    */
    public void clearCache () {
	synchronized (cache) {
	    cache.clear ();
	}
    }

    /**
     * Get a replicator for this node cache. A replicator is used to transfer updates
     * in this node manager to other node managers in remote servers via RMI.
     */
    protected Replicator getReplicator () {
	return replicator;
    }


    /**
    *  Receive notification from a remote app that objects in its cache have been
    * modified.
    */
    public void replicateCache (Vector add, Vector delete) {
	if (logReplication)
	    app.logEvent ("Received cache replication event: "+add.size()+
	                  " added, "+delete.size()+" deleted");
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
	        // NOTE: it would be more efficient to transfer just the keys
	        // of nodes that are to be deleted.
	        Node n = (Node) en.nextElement ();
	        DbMapping dbm = app.getDbMapping (n.getPrototype ());
	        if (dbm != null)
	            dbm.notifyDataChange ();
	        n.setDbMapping (dbm);
	        n.nmgr = safe;
	        Node oldNode = (Node) cache.get (n.getKey());
	        if (oldNode != null)
	            evictNode (oldNode);
	    }
	}
    }

}

