// Node.java
// Copyright (c) Hannes Wallnöfer 1997-2000
 
package helma.objectmodel.db;


import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.io.*;
import java.sql.Types;
import helma.objectmodel.*;
import helma.util.*;
import helma.framework.IPathElement;
import com.workingdogs.village.*;


/**
 * An implementation of INode that can be stored in the internal database or
 * an external relational database. 
 */
 
public final class Node implements INode, Serializable {

    // The handle to the node's parent
    protected NodeHandle parentHandle;
    // Ordered list of subnodes of this node
    private List subnodes;
    // Named subnodes (properties) of this node
    private Hashtable propMap;
    // Other nodes that link to this node. Used for reference counting/checking
    private List links;

    private long created;
    private long lastmodified;

    private String id, name;
    // is this node's main identity as a named property or an
    // anonymous node in a subnode collection?
    protected boolean anonymous = false;

    // the serialization version this object was read from (see readObject())
    protected short version = 0;

    /**
     * Read this object instance from a stream. This does some smart conversion to
     * update from previous serialization formats.
     */
    private void readObject (ObjectInputStream in) throws IOException {
	try {
	    // as a general rule of thumb, if a string can be null use read/writeObject,
	    // if not it's save to use read/writeUTF.
	    // version indicates the serialization version
	    version = in.readShort ();
	    String rawParentID = null;
	    id = in.readUTF ();
	    name = in.readUTF ();
	    if (version < 5)
	        rawParentID = (String) in.readObject ();
	    else
	        parentHandle = (NodeHandle) in.readObject ();
	    created = in.readLong ();
	    lastmodified = in.readLong ();
	    if (version < 4) {
	        // read away content and contentType, which were dropped
	        in.readObject ();
	        in.readObject ();
	    }
	    subnodes = (ExternalizableVector) in.readObject ();
	    links = (ExternalizableVector) in.readObject ();
	    if (version < 6)
	        // read away obsolete proplinks list
	        in.readObject ();
	    propMap = (Hashtable) in.readObject ();
	    anonymous = in.readBoolean ();
	    if (version == 2)
	        prototype = in.readUTF ();
	    else if (version >= 3)
	        prototype = (String) in.readObject ();
	    // if the input version is < 5, we have to do some conversion to make this object work
	    if (version < 5) {
	        if (rawParentID != null)
	            parentHandle = new NodeHandle (new DbKey (null, rawParentID));
	        if (subnodes != null) {
	            for (int i=0; i<subnodes.size(); i++) {
	                String s = (String) subnodes.get (i);
	                subnodes.set (i, new NodeHandle (new DbKey (null, s)));
	            }
	        }
	        if (links != null) {
	            for (int i=0; i<links.size(); i++) {
	                String s = (String) links.get (i);
	                links.set (i, new NodeHandle (new DbKey (null, s)));
	            }
	        }
	    }
	} catch (ClassNotFoundException x) {
	    throw new IOException (x.toString ());
	}
    }

    /**
     * Write out this instance to a stream
     */
    private void writeObject (ObjectOutputStream out) throws IOException {
	out.writeShort (7);  // serialization version
	out.writeUTF (id);
	out.writeUTF (name);
	out.writeObject (parentHandle);
	out.writeLong (created);
	out.writeLong (lastmodified);
	DbMapping smap = dbmap == null ? null : dbmap.getSubnodeMapping ();
	if (smap != null && smap.isRelational ())
	    out.writeObject (null);
	else
	    out.writeObject (subnodes);
	out.writeObject (links);
	out.writeObject (propMap);
	out.writeBoolean (anonymous);
	out.writeObject (prototype);
    }

    private transient String prototype;

    private transient NodeHandle handle;

    private transient INode cacheNode;

    transient WrappedNodeManager nmgr;

    transient DbMapping dbmap;

    transient Key primaryKey = null;

    transient String subnodeRelation = null;
    transient long lastSubnodeFetch = 0;
    transient long lastSubnodeChange = 0;

    transient long lastParentSet = 0;

    transient long lastSubnodeCount = 0;  // these two are only used
    transient int subnodeCount = -1;    // for aggressive loading relational subnodes

    transient private volatile Transactor lock;
    transient private int state;

    // transient String nameProp; // the name of the property which defines the name of this node.
    transient boolean adoptName = true;  // little helper to know if this node is being converted

    static final long serialVersionUID = -3740339688506633675L;

    /**
     * This constructor is only used for instances of the NullNode subclass. Do not use for ordinary Nodes.
     */
    Node () {
	created = lastmodified = System.currentTimeMillis ();
	nmgr = null;
    }

    /**
     * Creates a new Node with the given name. Only used by NodeManager for "root nodes" and
     * not in a Transaction context, which is why we can immediately mark it as CLEAN.
     */
    protected Node (String name, String id, String prototype, WrappedNodeManager nmgr) {
	this.nmgr = nmgr;
 	this.id = id;
	this.name = name == null || "".equals (name) ? id : name;
	if (prototype != null)
	    setPrototype (prototype);
	created = lastmodified = System.currentTimeMillis ();
	markAs (CLEAN);
    }


    /**
     * Constructor used for virtual nodes.
     */
    public Node (Node home, String propname, WrappedNodeManager nmgr, String prototype) {
	this.nmgr = nmgr;
	setParent (home);
	// this.dbmap = null;
	// generate a key for the virtual node that can't be mistaken for a Database Key
	primaryKey = new SyntheticKey (home.getKey (), propname);
	this.id = primaryKey.getID ();
	this.name = propname;
	this.anonymous = false;
	setPrototype (prototype);
	this.state = VIRTUAL;
    }

    /**
     * Creates a new Node with the given name. This is used for ordinary transient nodes.
     */
    public Node (String n, String prototype, WrappedNodeManager nmgr) {
	this.nmgr = nmgr;
	this.prototype = prototype;
	dbmap = nmgr.getDbMapping (prototype);
	// the id is only generated when the node is actually checked into db,
	// or when it's explicitly requested.
	id = null;
	this.name = n == null ? "" : n;
	created = lastmodified = System.currentTimeMillis ();
	adoptName = true;
	state = TRANSIENT;
    }


    /**
     * Constructor used for nodes being stored in a relational database table.
     */
    public Node (DbMapping dbm, Record rec, WrappedNodeManager nmgr) throws DataSetException {

	this.nmgr = nmgr;
	// see what prototype/DbMapping this object should use
	dbmap = dbm;
	String protoField= dbmap.getPrototypeField ();
	if (protoField != null) {
	    Value val = rec.getValue (protoField);
	    if (val != null && !val.isNull ()) {
	        String protoName = val.asString ();
	        dbmap = nmgr.getDbMapping (protoName);
	        if (dbmap == null) {
	            // invalid prototype name!
	            System.err.println ("Warning: Invalid prototype name: "+protoName+" - using default");
	            dbmap = dbm;
	        }
	    }
	}
	setPrototype (dbmap.getTypeName ());

	id = rec.getValue (dbmap.getIDField ()).asString ();
	// checkWriteLock ();
	String nameField =  dbmap.getNameField ();
	name = nameField == null ? id : rec.getValue (nameField).asString ();
	if (name == null || name.length() == 0)
	    name = dbmap.getTypeName() + " " + id;
	// set parent for user objects to internal userroot node
	if ("user".equals (prototype)) {
	    parentHandle = new NodeHandle (new DbKey (null, "1"));
	    anonymous = false;
	}

	created = lastmodified = System.currentTimeMillis ();

	for (Iterator i=dbmap.getDBPropertyIterator(); i.hasNext(); ) {

	    Relation rel = (Relation) i.next ();
	    // NOTE: this should never be the case, since we're just looping through
	    // mappnigs with a local db column
	    if (rel.reftype != Relation.PRIMITIVE && rel.reftype != Relation.REFERENCE)
	        continue;

	    Value val = rec.getValue (rel.getDbField ());

	    if (val.isNull ())
	        continue;

	    Property newprop = new Property (rel.propName, this);

	    switch (val.type ()) {

	        case Types.BIT:
	            newprop.setBooleanValue (val.asBoolean());
	            break;

	        case Types.TINYINT:
	        case Types.BIGINT:
	        case Types.SMALLINT:
	        case Types.INTEGER:
	            newprop.setIntegerValue (val.asLong());
	            break;

	        case Types.REAL:
	        case Types.FLOAT:
	        case Types.DOUBLE:
	            newprop.setFloatValue (val.asDouble());
	            break;

	        case Types.DECIMAL:
	        case Types.NUMERIC:
	            java.math.BigDecimal num = val.asBigDecimal ();
	            if (num.scale() > 0)
	                newprop.setFloatValue (val.asDouble());
	            else
	                newprop.setIntegerValue (val.asLong());
	            break;

	        case Types.LONGVARBINARY:
	        case Types.VARBINARY:
	        case Types.BINARY:
	            newprop.setStringValue (val.asString());
	            break;

	        case Types.LONGVARCHAR:
	        case Types.CHAR:
	        case Types.VARCHAR:
	        case Types.OTHER:
	            newprop.setStringValue (val.asString());
	            break;

	        case Types.DATE:
	        case Types.TIME:
	        case Types.TIMESTAMP:
	            newprop.setDateValue (val.asTimestamp());
	            break;

	        case Types.NULL:
	            continue;

	        default:
	            newprop.setStringValue (val.asString());
	            break;
	    }

	    if(propMap == null)
	        propMap = new Hashtable ();
	    propMap.put (rel.propName.toLowerCase(), newprop);
	    // mark property as clean, since it's fresh from the db
	    newprop.dirty = false;

	    // if the property is a pointer to another node, change the property type to NODE
	    if (rel.reftype == Relation.REFERENCE && rel.usesPrimaryKey ()) {
	        // FIXME: References to anything other than the primary key are not supported
	        newprop.nhandle = new NodeHandle (new DbKey (rel.otherType, newprop.getStringValue ()));
	        newprop.type = IProperty.NODE;
	    }
	}
	// again set created and lastmodified. This is because
	// lastmodified has been been updated, and we want both values to
	// be identical to show that the node hasn't been changed since
	// it was first created.
	created = lastmodified = System.currentTimeMillis ();
	markAs (CLEAN);
    }



    protected synchronized void checkWriteLock () {
	// System.err.println ("registering writelock for "+this.getName ()+" ("+lock+") to "+Thread.currentThread ());
	if (state == TRANSIENT)
	    return; // no need to lock transient node
	Transactor current = (Transactor) Thread.currentThread ();

	if (!current.isActive ())
	    throw new helma.framework.TimeoutException ();
	if (state == INVALID) {
	    nmgr.logEvent ("Got Invalid Node: "+this);
	    Thread.dumpStack ();
	    throw new ConcurrencyException ("Node "+this+" was invalidated by another thread.");
	}

	if (lock != null && lock != current && lock.isAlive () && lock.isActive ()) {
	    nmgr.logEvent ("Concurrency conflict for "+this+", lock held by "+lock);
	    throw new ConcurrencyException ("Tried to modify "+this+" from two threads at the same time.");
	}

	current.visitNode (this);
	lock = current;
    }

    protected synchronized void clearWriteLock () {
	lock = null;
    }


    protected void markAs (int s) {
	if (state == INVALID || state == VIRTUAL || state == TRANSIENT)
	    return;
	state = s;
	if (Thread.currentThread () instanceof Transactor) {
	    Transactor tx = (Transactor) Thread.currentThread ();
	    if (s == CLEAN) {
	        clearWriteLock ();
	        tx.dropNode (this);
	    } else {
	        tx.visitNode (this);
	        if (s == NEW) {
	            clearWriteLock ();
	            tx.visitCleanNode (this);
	        }
	    }
	}
    }

    public int getState () {
	return state;
    }

    public void setState (int s) {
	this.state = s;
    }

    /**
     *  Mark node as invalid so it is re-fetched from the database
     */
    public void invalidate () {
	// This doesn't make sense for transient nodes
	if (state == TRANSIENT || state == NEW)
	    return;
	checkWriteLock ();
	nmgr.evictNode (this);
    }


    /** 
     *  Get the ID of this Node. This is the primary database key and used as part of the
     *  key for the internal node cache.
     */
    public String getID () {
	// if we are transient, we generate an id on demand. It's possible that we'll never need
	// it, but if we do it's important to keep the one we have.
	if (state == TRANSIENT && id == null) {
	    id = TransientNode.generateID ();
	}
	return id;
    }

    /**
     * Returns true if this node is accessed by id from its aprent, false if it
     * is accessed by name
     */
    public boolean isAnonymous () {
 	return anonymous;
    }

    /**
     * Return this node' name, which may or may not have some meaning
     */
    public String getName () {
	return name; 
    }
    
    /**
     * Get something to identify this node within  a URL. This is the ID for anonymous nodes
     * and a property value for named properties.
     */
    public String getElementName () {
	// if subnodes are also mounted as properties, try to get the "nice" prop value
	// instead of the id by turning the anonymous flag off.
	// Work around this for user objects to alsways return a URL like /users/username
	if ("user".equalsIgnoreCase (prototype)) {
	    anonymous = false;
	} else if (parentHandle != null) {
	    try {
	        Node p = parentHandle.getNode (nmgr);
	        DbMapping parentmap = p.getDbMapping ();
	        Relation prel = parentmap.getPropertyRelation();
	        if (prel != null && prel.subnodesAreProperties && prel.accessor != null) {
	            String propname = dbmap.columnNameToProperty (prel.accessor);
	            String propvalue = getString (propname, false);
	            if (propvalue != null && propvalue.length() > 0) {
	                setName (propvalue);
	                anonymous = false;
	                // nameProp = localrel.propName;
	            } else {
	                anonymous = true;
	            }
	        }
	    } catch (Exception ignore) {
	        // just fall back to default method
	    }
	}
    	return anonymous || name == null || name.length() == 0 ? id : name;
    }


    public String getFullName () {
	return getFullName (null);
    }

    public String getFullName (INode root) {
	String fullname = "";
	String divider = null;
	StringBuffer b = new StringBuffer ();
	INode p = this;
	int loopWatch = 0;

	while  (p != null && p.getParent () != null && p != root) {
	    if (divider != null)
	        b.insert (0, divider);
	    else
	        divider = "/";
	    b.insert (0, p.getElementName ());
	    p = p.getParent ();

	    loopWatch++;
	    if (loopWatch > 10) {
	        b.insert (0, "...");
	        break;
	    }
	}
	return b.toString ();
    }


    public String getPrototype () {
	if (prototype == null && propMap != null) {
	    // retrieve prototype name from properties
	    Property pp = (Property) propMap.get ("prototype");
	    if (pp != null)
	        prototype = pp.getStringValue ();
	}
	return prototype;
    }

    public void setPrototype (String proto) {
	this.prototype = proto;
    }


    public void setDbMapping (DbMapping dbmap) {
	if (this.dbmap != dbmap) {
	    this.dbmap = dbmap;
	    // primaryKey = null;
	}
    }

    public DbMapping getDbMapping () {
	return dbmap;
    }

    public Key getKey () {
	if (state == TRANSIENT) {
	    Thread.dumpStack ();
	    throw new RuntimeException ("getKey called on transient Node: "+this);
	}
	if (dbmap == null && prototype != null && nmgr != null)
	    dbmap = nmgr.getDbMapping (prototype);
	if (primaryKey == null)
	    primaryKey = new DbKey (dbmap, id);
	return primaryKey;
    }

    public NodeHandle getHandle () {
	if (handle == null)
	    handle = new NodeHandle (this);
	return handle;
    }

    public void setSubnodeRelation (String rel) {
	if ((rel == null && this.subnodeRelation == null)
	|| (rel != null && rel.equalsIgnoreCase (this.subnodeRelation)))
	    return;
	checkWriteLock ();
	this.subnodeRelation = rel;
	DbMapping smap = dbmap == null ? null : dbmap.getSubnodeMapping ();
	if (smap != null && smap.isRelational ()) {
	    subnodes = null;
	    subnodeCount = -1;
	}
    }

    public String getSubnodeRelation () {
	return subnodeRelation;
    }

    public void setName (String name) {
	// "/" is used as delimiter, so it's not a legal char
	if (name.indexOf('/') > -1)
	    return;
	//     throw new RuntimeException ("The name of the node must not contain \"/\".");
	if (name == null || name.trim().length() == 0)
	    this.name = id;   // use id as name
	else
	    this.name = name;
    }

    /**
     * Register a node as parent of the present node. We can't refer to the node directly, so we use
     * the ID + DB map combo.
     */
    public void setParent (Node parent) {
	parentHandle = parent == null ? null : parent.getHandle ();
    }

    /**
     * This version of setParent additionally marks the node as anonymous or non-anonymous,
     * depending on the string argument. This is the version called from the scripting framework,
     * while the one argument version is called from within the objectmodel classes only.
     */
    public void setParent (Node parent, String propertyName) {
	// we only do that for relational nodes.
	if (!isRelational ())
	    return;

	NodeHandle oldParentHandle = parentHandle;
	parentHandle = parent == null ? null : parent.getHandle ();
	// mark parent as set, otherwise getParent will try to
	// determine the parent again when called.
	lastParentSet = System.currentTimeMillis ();
	if (parentHandle == null || parentHandle.equals (oldParentHandle))
	    // nothing changed, no need to find access property
	    return;

	if (parent != null && propertyName == null) {
	    // see if we can find out the propertyName by ourselfes by looking at the
	    // parent's property relation
	    String newname = null;
	    DbMapping parentmap = parent.getDbMapping ();
	    if (parentmap != null) {
	        // first try to retrieve name via generic property relation of parent
	        Relation prel = parentmap.getPropertyRelation ();
	        if (prel != null && prel.otherType == dbmap && prel.accessor != null) {
	            // reverse look up property used to access this via parent
	            Relation proprel = dbmap.columnNameToRelation (prel.accessor);
	            if (proprel != null && proprel.propName != null)
	                newname = getString (proprel.propName, false);
	        }
	    }

	    // did we find a new name for this
	    if (newname == null) {
	        this.anonymous = true;
	    } else {
	        this.anonymous = false;
	        this.name = newname;
	    }
	} else {
	    this.anonymous = false;
	    this.name = propertyName;
	}
    }


    /**
     * Get parent, retrieving it if necessary.
     */
    public INode getParent () {

	// check what's specified in the type.properties for this node.
	ParentInfo[] parentInfo = null;
	if (isRelational () && (lastParentSet < dbmap.getLastTypeChange() || lastParentSet < lastmodified))
	    parentInfo = dbmap.getParentInfo ();

	// check if current parent candidate matches presciption, if not, try to get it
	if (parentInfo != null && state != TRANSIENT) {
	    for (int i=0; i<parentInfo.length; i++) {
	        ParentInfo pinfo = parentInfo[i];
	        INode pn = null;
	        // see if there is an explicit relation defined for this parent info
	        // we only try to fetch a node if an explicit relation is specified for the prop name
	        Relation rel = dbmap.propertyToRelation (pinfo.propname);
	        if (rel != null && rel.reftype == Relation.REFERENCE)
	            pn = getNode (pinfo.propname, false);
	        // the parent of this node is the app's root node...
	        if (pinfo.isroot && pn == null)
	            pn = nmgr.getNode ("0", nmgr.getDbMapping ("root"));
	        // if we found a parent node, check if we ought to use a virtual or groupby node as parent
	        if (pn != null) {
	            // see if dbmapping specifies anonymity for this node
	            if (pinfo.virtualname != null)
	                pn = pn.getNode (pinfo.virtualname, false);
	            DbMapping dbm = pn == null ? null : pn.getDbMapping ();
	            try {
	                if (dbm != null && dbm.getSubnodeGroupby () != null) {
	                    // check for groupby
	                    rel = dbmap.columnNameToRelation (dbm.getSubnodeGroupby());
	                    pn = pn.getSubnode (getString (rel.propName, false));
	                }
	                if (pn != null) {
	                    setParent ((Node) pn);
	                    anonymous = !pinfo.named;
	                    lastParentSet = System.currentTimeMillis ();
	                    return pn;
	                }
	            } catch (Exception ignore) {}
	        }
	    }
	}

	// fall back to heuristic parent (the node that fetched this one from db)
	if (parentHandle == null)
	    return null;
	return parentHandle.getNode (nmgr);
    }


    /**
     * Get parent, using cached info if it exists.
     */
    public Node getCachedParent () {
	if (parentHandle == null)
	    return null;
	return parentHandle.getNode (nmgr);
    }


    /**
     *  INode-related
     */

    public INode addNode (INode elem) {
	return addNode (elem, numberOfNodes ());
    }

    public INode addNode (INode elem, int where) {

    	Node node = null;
    	if (elem instanceof Node)
	    node = (Node) elem;
	else 
	    throw new RuntimeException ("Can't add fixed-transient node to a persistent node");
	
	// only lock node if it has to be modified for a change in subnodes
	if (!ignoreSubnodeChange ())
	    checkWriteLock ();
	node.checkWriteLock ();
	
	// if subnodes are defined via realation, make sure its constraints are enforced.
	if (dbmap != null && dbmap.getSubnodeRelation () != null)
	    dbmap.getSubnodeRelation ().setConstraints (this, node);
	
	// if the new node is marked as TRANSIENT and this node is not, mark new node as NEW
	if (state != TRANSIENT && node.state == TRANSIENT)
	    node.makePersistentCapable ();

	String n = node.getName();
	
	// if (n.indexOf('/') > -1)
	//     throw new RuntimeException ("\"/\" found in Node name.");

	// only mark this node as modified if subnodes are not in relational db
	// pointing to this node.
	if (!ignoreSubnodeChange () && (state == CLEAN || state == DELETED))
	    markAs (MODIFIED);
	if (node.state == CLEAN || node.state == DELETED)
	    node.markAs (MODIFIED);

	loadNodes ();

	// check if this node has a group-by subnode-relation
	if (dbmap != null) {
	    Relation srel = dbmap.getSubnodeRelation ();
	    if (srel != null && srel.groupby != null) try {
	        Relation groupbyRel = srel.otherType.columnNameToRelation (srel.groupby);
	        String groupbyProp = (groupbyRel != null) ?
	            groupbyRel.propName : srel.groupby;
	        String groupbyValue = node.getString (groupbyProp, false);
	        INode groupbyNode = getNode (groupbyValue, false);
	        // if group-by node doesn't exist, we'll create it
	        if (groupbyNode == null)
	            groupbyNode = getGroupbySubnode (groupbyValue, true);
	        groupbyNode.addNode (node);
	
	        return node;
	    } catch (Exception x) {
	        System.err.println ("Error adding groupby: "+x);
	        // x.printStackTrace ();
	        return null;
	    }
	}


	if (where < 0 || where > numberOfNodes ())
	    where = numberOfNodes ();

	NodeHandle nhandle = node.getHandle ();
	if (subnodes != null && subnodes.contains (nhandle)) {
	    // Node is already subnode of this - just move to new position
	    subnodes.remove (nhandle);
	    where = Math.min (where, numberOfNodes ());
	    subnodes.add (where, nhandle);
	} else {
	    if (subnodes == null)
	        subnodes = new ExternalizableVector ();
	    subnodes.add (where, nhandle);

	    // check if properties are subnodes (_properties.aresubnodes=true)
	    if (dbmap != null && node.dbmap != null) {
	        Relation prel = dbmap.getPropertyRelation();
	        if (prel != null && prel.accessor != null) {
	            Relation localrel = node.dbmap.columnNameToRelation (prel.accessor);
	            // if no relation from db column to prop name is found, assume that both are equal
	            String propname = localrel == null ? prel.accessor : localrel.propName;
	            String prop = node.getString (propname, false);
	            if (prop != null && prop.length() > 0) {
	                INode old = getNode (prop, false);
	                if (old != null && old != node) {
	                    unset (prop);
	                    removeNode (old);
	                }
	                setNode (prop, node);
	            }
	        }
	    }

	    if (!"root".equalsIgnoreCase (node.getPrototype ())) {
	        // avoid calling getParent() because it would return bogus results for the not-anymore transient node
	        Node nparent = node.parentHandle == null ? null : node.parentHandle.getNode (nmgr);
	        // if the node doesn't have a parent yet, or it has one but it's transient while we are
	        // persistent, make this the nodes new parent.
	        if (nparent == null || (state != TRANSIENT && nparent.getState () == TRANSIENT)) {
	            node.setParent (this);
	            node.anonymous = true;
	        } else if (nparent != null && (nparent != this || !node.anonymous)) {
	            // this makes the additional job of addLink, registering that we have a link to a node in our
	            // subnodes that actually sits somewhere else. This means that addLink and addNode
	            // are actually the same now.
	            node.registerLinkFrom (this);
	        }
	    }
	}

	lastmodified = System.currentTimeMillis ();
	lastSubnodeChange = lastmodified;
	// Server.throwNodeEvent (new NodeEvent (this, NodeEvent.SUBNODE_ADDED, node));
	return node;
    }


    public INode createNode () {
	// create new node at end of subnode array
	return createNode (null, numberOfNodes ());
    }

    public INode createNode (int where) {
	return createNode (null, where);
    }

    public INode createNode (String nm) {
	// parameter where is  ignored if nm != null so we try to avoid calling numberOfNodes()
	return createNode (nm, nm == null ? numberOfNodes () : 0);
    }

    public INode createNode (String nm, int where) {
	checkWriteLock ();
    	boolean anon = false;
	if (nm == null || "".equals (nm.trim ())) 
	    anon = true;
	Node n = new Node (nm, null, nmgr);
	if (anon)
	    addNode (n, where);
	else 
	    setNode (nm, n);
	return n;
    }


    /**
     * register a node that links to this node so we can notify it when we cease to exist.
     * this is only necessary if we are a non-relational node, since for relational nodes
     * the referring object will notice that we've gone at runtime.
     */
    protected void registerLinkFrom (Node from) {
	if (isRelational ())
	    return;
	if (from.getState () == TRANSIENT)
	    return;
	if (links == null)
	    links = new ExternalizableVector ();
	Object fromHandle = from.getHandle ();
	if (!links.contains (fromHandle))
	    links.add (fromHandle);
    }

    /**
     * This implements the getChild() method of the IPathElement interface
     */
    public IPathElement getChildElement (String name) {
	IPathElement child = (IPathElement) getSubnode (name);
	if (child == null)
	    child = (IPathElement) getNode (name, false);
	return child;
    }

    /**
     * This implements the getParentElement() method of the IPathElement interface
     */
    public IPathElement getParentElement () {
	return getParent ();
    }


    public INode getSubnode (String subid) {
	// System.err.println ("GETSUBNODE : "+this+" > "+subid);
	if ("".equals (subid)) {
	    return this;
	}
	
	Node retval = null;

	if (subid != null) {
	
	    loadNodes ();
	    if (subnodes == null || subnodes.size() == 0)
	        return null;
	
	    NodeHandle nhandle = null;
	    int l = subnodes.size ();
	    for (int i=0; i<l; i++) try {
	        NodeHandle shandle = (NodeHandle) subnodes.get (i);
	        if (subid.equals (shandle.getID ())) {
	            // System.err.println ("FOUND SUBNODE: "+shandle);
	            nhandle = shandle;
	            break;
	        }
	    } catch (Exception x) {
	        break;
	    }
	
	    if (nhandle != null) {
	        retval = nhandle.getNode (nmgr);
	    }
	
	    // This would be an alternative way to do it, without loading the subnodes:
	    //    if (dbmap != null && dbmap.getSubnodeRelation () != null)
	    //         retval = nmgr.getNode (this, subid, dbmap.getSubnodeRelation ());

	    if (retval != null && retval.parentHandle == null && !"root".equalsIgnoreCase (retval.getPrototype ())) {
	        retval.setParent (this);
	        retval.anonymous = true;
	    }
	
	}
	return retval;
    }


    public INode getSubnodeAt (int index) {
	loadNodes ();

	if (subnodes == null)
	    return null;

	DbMapping smap = null;
	if (dbmap != null)
	    smap = dbmap.getSubnodeMapping ();
	
	Node retval = null;
	if (subnodes.size () > index) {
	    // check if there is a group-by relation
	    retval = ((NodeHandle) subnodes.get (index)).getNode (nmgr);
	
	    if (retval != null && retval.parentHandle == null && !"root".equalsIgnoreCase (retval.getPrototype ())) {
	        retval.setParent (this);
	        retval.anonymous = true;
	    }
	}
	return retval;
    }

    public Node getGroupbySubnode (String sid, boolean create) {
	loadNodes ();
	if (subnodes == null)
	    subnodes = new ExternalizableVector ();

	NodeHandle ghandle = new NodeHandle (new SyntheticKey (getKey(), sid));
	if (subnodes.contains (ghandle) || create) try {
	    DbMapping groupbyMapping = dbmap.getGroupbyMapping ();
	    boolean relational = groupbyMapping.getSubnodeMapping ().isRelational ();

	    if (relational || create) {
	        Node node = relational ? new Node (this, sid, nmgr, null) : new Node ("groupby-"+sid, null, nmgr);
	        // set "groupname" property to value of groupby field
	        node.setString ("groupname", sid);

	        if (relational) {
	            node.setDbMapping (groupbyMapping);
	        } else {
	            setNode (sid, node);
	            subnodes.add (node.getHandle ());
	        }
	        node.setPrototype (groupbyMapping.getTypeName ());
	        nmgr.evictKey (node.getKey ());
	        return node;
	    }
	} catch (Exception noluck) {
	    nmgr.logEvent ("Error creating group-by node for "+sid+": "+noluck);
	    noluck.printStackTrace();
	}
	return null;
    }

    public boolean remove () {
	checkWriteLock ();
	try {
	    if (!anonymous)
	        getParent ().unset (name);
	    else
	        getParent ().removeNode (this);
	} catch (Exception x) {
	    return false;
	}
	return true;
    }


    public void removeNode (INode node) {
	// nmgr.logEvent ("removing: "+ node);
	Node n = (Node) node;
	checkWriteLock ();
	n.checkWriteLock ();
	
	// need to query parent before releaseNode is called, since this may change the parent
	// to the next option described in the type.properties _parent info
	INode parent = n.getParent ();

	releaseNode (n);

	if (parent == this) {
                 n.deepRemoveNode ();
	} else {
	    // removed just a link, not the main node.
	    if (n.links != null) {
	        n.links.remove (getHandle ());
	        if (n.state == CLEAN) n.markAs (MODIFIED);
	    }
	}
    }

    /**
     * "Locally" remove a subnode from the subnodes table.
     *  The logical stuff necessary for keeping data consistent is done in removeNode().
     */
    protected void releaseNode (Node node) {
	if (subnodes != null)
	    subnodes.remove (node.getHandle ());

	lastSubnodeChange = System.currentTimeMillis ();

	// check if the subnode is in relational db and has a link back to this
	// which needs to be unset
	if (dbmap != null) {
	    Relation srel = dbmap.getSubnodeRelation ();
	    /*if (srel != null && srel.reftype == Relation.BACKWARD) {
	        Relation backlink = srel.otherType.columnNameToRelation (srel.getRemoteField ());
	        if (backlink != null && id.equals (node.getString (backlink.propName, false)))
	            node.unset (backlink.propName);
	    } */
	}

	// check if subnodes are also accessed as properties. If so, also unset the property
	if (dbmap != null && node.dbmap != null) {
	    Relation prel = dbmap.getPropertyRelation();
	    if (prel != null && prel.accessor != null) {
	        Relation localrel = node.dbmap.columnNameToRelation (prel.accessor);
	        // if no relation from db column to prop name is found, assume that both are equal
	        String propname = localrel == null ? prel.accessor : localrel.propName;
	        String prop = node.getString (propname, false);
	        if (prop != null && getNode (prop, false) == node)
	            unset (prop);
	    }
	}

	// If subnodes are relational no need to mark this node as modified
	if (ignoreSubnodeChange ())
	    return;
	
	// Server.throwNodeEvent (new NodeEvent (node, NodeEvent.NODE_REMOVED));
	// Server.throwNodeEvent (new NodeEvent (this, NodeEvent.SUBNODE_REMOVED, node));
	lastmodified = System.currentTimeMillis ();
	// nmgr.logEvent ("released node "+node +" from "+this+"     oldobj = "+what);
	if (state == CLEAN) markAs (MODIFIED);
    }

   /**
    * Delete the node from the db. This mainly tries to notify all nodes referring to this that
    * it's going away. For nodes from the embedded db it also does a cascading delete, since
    * it can tell which nodes are actual children and which are just linked in.
    */
    protected void deepRemoveNode () {

	// notify nodes that link to this node being deleted.
	int l = links == null ? 0 : links.size ();
	for (int i = 0; i < l; i++) {
	    NodeHandle lhandle = (NodeHandle) links.get (i);
	    Node link = lhandle.getNode (nmgr);
	    if (link != null)
	        link.releaseNode (this);
	}

	// tell all nodes that are properties of n that they are no longer used as such
	if (propMap != null) {
	    for (Enumeration e2 = propMap.elements (); e2.hasMoreElements (); ) {
	        Property p = (Property) e2.nextElement ();
	        if (p != null && p.type == Property.NODE)
	            p.unregisterNode ();
	    }
	}

	// cascading delete of all subnodes. This is never done for relational subnodes, because
	// the parent info is not 100% accurate for them.
	if (subnodes != null) {
	    Vector v = new Vector ();
	    // remove modifies the Vector we are enumerating, so we are extra careful.
	    for (Enumeration e3 = getSubnodes (); e3.hasMoreElements(); ) {
	        v.add (e3.nextElement());
	    }
	    int m = v.size ();
	    for (int i=0; i<m; i++) {
	        // getParent() is heuristical/implicit for relational nodes, so we don't base
	        // a cascading delete on that criterium for relational nodes.
	        Node n = (Node) v.get (i);
	        if (!n.isRelational())
	            removeNode (n);
	    }
	}

	// mark the node as deleted
	setParent (null);
	markAs (DELETED);
    }

    public int contains (INode n) {
	if (n == null)
	    return -1;
	loadNodes ();
	if (subnodes == null)
	    return -1;
	// if the node contains relational groupby subnodes, the subnodes vector contains the names instead of ids.
	if (!(n instanceof Node))
	    return -1;
	Node node = (Node) n;
	return subnodes.indexOf (node.getHandle ());
    }


    /**
     * Count the subnodes of this node. If they're stored in a relational data source, we
     * may actually load their IDs in order to do this.
     */
    public int numberOfNodes () {
	// If the subnodes are loaded aggressively, we really just
	// do a count statement, otherwise we just return the size of the id index.
	// (after loading it, if it's coming from a relational data source).
	DbMapping smap = dbmap == null ? null : dbmap.getSubnodeMapping ();
	if (smap != null && smap.isRelational ()) {
	    // check if subnodes need to be rechecked
	    Relation subRel = dbmap.getSubnodeRelation ();
	    // do not fetch subnodes for nodes that haven't been persisted yet or are in
	    // the process of being persistified - except if "manual" subnoderelation is set.
	    if (subRel.aggressiveLoading && ((state != TRANSIENT && state != NEW) || subnodeRelation != null)) {
	        // we don't want to load *all* nodes if we just want to count them
	        long lastChange = subRel.aggressiveCaching ? lastSubnodeChange : smap.getLastDataChange ();
	        // also reload if the type mapping has changed.
	        lastChange = Math.max (lastChange, dbmap.getLastTypeChange ());
	        if (lastChange < lastSubnodeFetch && subnodes != null) {
	            // we can use the nodes vector to determine number of subnodes
	            subnodeCount = subnodes.size();
	            lastSubnodeCount = System.currentTimeMillis ();
	        } else if (lastChange >= lastSubnodeCount || subnodeCount < 0) {
	           // count nodes in db without fetching anything
	           subnodeCount = nmgr.countNodes (this, subRel);
	           lastSubnodeCount = System.currentTimeMillis ();
	        }
	        return subnodeCount;
	    }
	}
	loadNodes ();
	return subnodes == null ? 0 : subnodes.size ();
    }

    /**
     * Make sure the subnode index is loaded for subnodes stored in a relational data source.
     *  Depending on the subnode.loadmode specified in the type.properties, we'll load just the
     *  ID index or the actual nodes.
     */
    protected void loadNodes () {
	// Don't do this for transient nodes which don't have an explicit subnode relation set
	if ((state == TRANSIENT || state == NEW) && subnodeRelation == null)
	    return;
	
	DbMapping smap = dbmap == null ? null : dbmap.getSubnodeMapping ();
	if (smap != null && smap.isRelational ()) {
	    // check if subnodes need to be reloaded
	    Relation subRel = dbmap.getSubnodeRelation ();
	    synchronized (this) {
	        long lastChange = subRel.aggressiveCaching ? lastSubnodeChange : smap.getLastDataChange ();
	        // also reload if the type mapping has changed.
	        lastChange = Math.max (lastChange, dbmap.getLastTypeChange ());
	        if (lastChange >= lastSubnodeFetch || subnodes == null) {
	            if (subRel.aggressiveLoading)
	                subnodes = nmgr.getNodes (this, dbmap.getSubnodeRelation());
	            else
	                subnodes = nmgr.getNodeIDs (this, dbmap.getSubnodeRelation());
	            lastSubnodeFetch = System.currentTimeMillis ();
	        }
	    }
	}
    }

    public Enumeration getSubnodes () {
	loadNodes ();
	class Enum implements Enumeration {
	    int count = 0;
	    public boolean hasMoreElements () {
	        return count < numberOfNodes ();
	    }
	    public Object nextElement () {
	        return getSubnodeAt (count++);
	    }	
	}
	return new Enum ();
    }

    private boolean ignoreSubnodeChange () {
	// return true if a change in subnodes can be ignored because it is
	// stored in the subnodes themselves.
	Relation rel = dbmap == null ? null : dbmap.getSubnodeRelation();
	return (rel != null && rel.otherType != null && rel.otherType.isRelational());
    }

    /**
     *  Get all properties of this node.
     */
    public Enumeration properties () {

	if (dbmap != null && dbmap.getProp2DB ().size() > 0)
	    // return the properties defined in type.properties, if there are any
	    return new Enumeration () {
	        Iterator i = dbmap.getProp2DB().keySet().iterator();
	        public boolean hasMoreElements() {return i.hasNext();}
	        public Object nextElement () {return i.next();}
	    };

	Relation prel = dbmap == null ? null : dbmap.getPropertyRelation ();
	if (prel != null && prel.accessor != null && !prel.subnodesAreProperties
			&& prel.otherType != null && prel.otherType.isRelational ())
	    // return names of objects from a relational db table
	    return nmgr.getPropertyNames (this, prel).elements ();
	else if (propMap != null)
	    // return the actually explicitly stored properties
	    return propMap.keys ();

	// sorry, no properties for this Node
	return new EmptyEnumeration ();

	// NOTE: we don't enumerate node properties here
	// return propMap == null ? new Vector ().elements () : propMap.elements ();
    }



    public IProperty get (String propname, boolean inherit) {
	return getProperty (propname, inherit);
    }

    public String getParentInfo () {
	return "anonymous:"+anonymous+",parentHandle"+parentHandle+",parent:"+getParent();
    }

    protected Property getProperty (String propname, boolean inherit) {
	// nmgr.logEvent ("GETTING PROPERTY: "+propname);
	if (propname == null)
	    return null;
	Property prop = propMap == null ? null : (Property) propMap.get (propname.toLowerCase ());

	// See if this could be a relationally linked node which still doesn't know
	// (i.e, still thinks it's just the key as a string)
	DbMapping pmap = dbmap == null ? null : dbmap.getExactPropertyMapping (propname);
	if (pmap != null && prop != null && prop.type != IProperty.NODE) {
	    // this is a relational node stored by id but we still think it's just a string. fix it
	    prop.nhandle = new NodeHandle (new DbKey (pmap, prop.getStringValue ()));
	    prop.type = IProperty.NODE;
	}

	// the property does not exist in our propmap - see if we can create it on the fly,
	// either because it is mapped from a relational database or defined as virtual node
	if (prop == null && dbmap != null) {
	    Relation prel =  dbmap.getPropertyRelation (propname);
	    Relation srel = dbmap.getSubnodeRelation ();
	    if (prel == null && srel != null && srel.groupby != null)
	        prel = srel;
	    if (prel == null)
	        prel = dbmap.getPropertyRelation ();
	    if (prel != null) {
	        // if what we want is a virtual node, fetch anyway.
	        if (state == TRANSIENT && prel.virtual) {
	            INode node = new Node (propname, prel.getPrototype (), nmgr);
	            node.setDbMapping (prel.getVirtualMapping ());
	            setNode (propname, node);
	            prop = (Property) propMap.get (propname);
	        // if this is from relational database only fetch if this node is itself persistent.
	        } else if (state != TRANSIENT && (prel.virtual || prel.accessor != null || prel.groupby != null)) {
	            // this may be a relational node stored by property name
	            try {
	                Node pn = nmgr.getNode (this, propname, prel);
	                if (pn != null) {
	                    if (pn.parentHandle == null && !"root".equalsIgnoreCase (pn.getPrototype ())) {
	                        pn.setParent (this);
	                        pn.name = propname;
	                        pn.anonymous = false;
	                    }
	                    prop = new Property (propname, this, pn);
	                }
	            } catch (RuntimeException nonode) {
	                // wasn't a node after all
	            }
	        }
	    }
	}
	
	if (prop == null && inherit && getParent () != null && state != TRANSIENT) {
	    prop = ((Node) getParent ()).getProperty (propname, inherit);
	}
	return prop;
    }

    /* public String getString (String propname, String defaultValue, boolean inherit) {
	String propValue = getString (propname, inherit);
	return propValue == null ? defaultValue : propValue;
    } */

    public String getString (String propname, boolean inherit) {  
	// propname = propname.toLowerCase ();
	Property prop = getProperty (propname, inherit);
	try {
	    return prop.getStringValue ();
	} catch (Exception ignore) {}
	return null;
    }

    public long getInteger (String propname, boolean inherit) {  
	// propname = propname.toLowerCase ();
	Property prop = getProperty (propname, inherit);
	try {
	    return prop.getIntegerValue ();
	} catch (Exception ignore) {}
	return 0;
    }

    public double getFloat (String propname, boolean inherit) {  
	// propname = propname.toLowerCase ();
	Property prop = getProperty (propname, inherit);
	try {
	    return prop.getFloatValue ();
	} catch (Exception ignore) {}
	return 0.0;
    }

    public Date getDate (String propname, boolean inherit) {  
	// propname = propname.toLowerCase ();
	Property prop = getProperty (propname, inherit);
	try {
	    return prop.getDateValue ();
	} catch (Exception ignore) {}
	return null;
    }


    public boolean getBoolean (String propname, boolean inherit) {  
	// propname = propname.toLowerCase ();
	Property prop = getProperty (propname, inherit);
	try {
	    return prop.getBooleanValue ();
	} catch (Exception ignore) {}
	return false;
    }

    public INode getNode (String propname, boolean inherit) {  
	// propname = propname.toLowerCase ();
	Property prop = getProperty (propname, inherit);
	try {
	    return prop.getNodeValue ();
	} catch (Exception ignore) {}
	return null;
    }

    public Object getJavaObject (String propname, boolean inherit) {
	// propname = propname.toLowerCase ();
	Property prop = getProperty (propname, inherit);
	try {
	    return prop.getJavaObjectValue ();
	} catch (Exception ignore) {}
	return null;
    }

    public void setString (String propname, String value) {
	// nmgr.logEvent ("setting String prop");
	checkWriteLock ();

	if (propMap == null)
	    propMap = new Hashtable ();

	propname = propname.trim ();
	String p2 = propname.toLowerCase ();

	Property prop = (Property) propMap.get (p2);
	String oldvalue = null;

	if (prop != null) {
	    oldvalue = prop.getStringValue ();
	    // check if the value has changed
	    if (value != null && value.equals (oldvalue))
	        return;
	    prop.setStringValue (value);
	} else {
	    prop = new Property (propname, this);
	    prop.setStringValue (value);
	    propMap.put (p2, prop);
	}

	// check if this may have an effect on the node's URL when using subnodesAreProperties
	// but only do this if we already have a parent set, i.e. if we are already stored in the db
	Node parent = parentHandle == null ? null : (Node) getParent ();

	if (dbmap != null && parent != null && parent.getDbMapping() != null) {
	    // check if this node is already registered with the old name; if so, remove it.
	    // then set parent's property to this node for the new name value
	    DbMapping parentmap = parent.getDbMapping ();
	    Relation prel = parentmap.getPropertyRelation ();
	    String dbcolumn = dbmap.propertyToColumnName (propname);

	    if (prel != null && prel.accessor != null && prel.accessor.equals (dbcolumn)) {
	        INode n = parent.getNode (value, false);
	        if (n != null && n != this) {
	            parent.unset (value);
	            parent.removeNode (n);
	        }

	        if (oldvalue != null) {
	            n = parent.getNode (oldvalue, false);
	            if (n == this) {
	                parent.unset (oldvalue);
	                parent.addNode (this);
	                // let the node cache know this key's not for this node anymore.
	                nmgr.evictKey (new SyntheticKey (parent.getKey (), oldvalue));
	            }
	        }
	        parent.setNode (value, this);
	        setName (value);
	    }
	}

	// check if the property we're setting specifies the prototype of this object.
	if (dbmap != null && dbmap.getPrototypeField () != null) {
	    String pn = dbmap.columnNameToProperty (dbmap.getPrototypeField ());
	    if (propname.equals (pn)) {
	        DbMapping newmap = nmgr.getDbMapping (value);
	        if (newmap != null) {
	            // see if old and new prototypes have same storage - otherwise type change is ignored
	            String oldStorage = dbmap.getStorageTypeName ();
	            String newStorage = newmap.getStorageTypeName ();
	            if ((oldStorage == null && newStorage == null) ||
	            		(oldStorage != null && oldStorage.equals (newStorage))) {
	                dbmap.notifyDataChange ();
	                newmap.notifyDataChange ();
	                this.dbmap = newmap;
	                this.prototype = value;
	            }
	        }
	    }
	}

	// Server.throwNodeEvent (new NodeEvent (this, NodeEvent.PROPERTIES_CHANGED));
	lastmodified = System.currentTimeMillis ();
	if (state == CLEAN) markAs (MODIFIED);

    }

    public void setInteger (String propname, long value) {
	// nmgr.logEvent ("setting bool prop");
	checkWriteLock ();

	if (propMap == null)
	    propMap = new Hashtable ();
	propname = propname.trim ();
	String p2 = propname.toLowerCase ();

	Property prop = (Property) propMap.get (p2);
	if (prop != null) {
	    prop.setIntegerValue (value);
	} else {
	    prop = new Property (propname, this);
	    prop.setIntegerValue (value);
	    propMap.put (p2, prop);
	}

	// Server.throwNodeEvent (new NodeEvent (this, NodeEvent.PROPERTIES_CHANGED));
	lastmodified = System.currentTimeMillis ();
	if (state == CLEAN) markAs (MODIFIED);
    }

    public void setFloat (String propname, double value) {
	// nmgr.logEvent ("setting bool prop");
	checkWriteLock ();

	if (propMap == null)
	    propMap = new Hashtable ();
	propname = propname.trim ();
	String p2 = propname.toLowerCase ();

	Property prop = (Property) propMap.get (p2);
	if (prop != null) {
	    prop.setFloatValue (value);
	} else {
	    prop = new Property (propname, this);
	    prop.setFloatValue (value);
	    propMap.put (p2, prop);
	}

	// Server.throwNodeEvent (new NodeEvent (this, NodeEvent.PROPERTIES_CHANGED));
	lastmodified = System.currentTimeMillis ();
	if (state == CLEAN) markAs (MODIFIED);
    }

    public void setBoolean (String propname, boolean value) {
	// nmgr.logEvent ("setting bool prop");
	checkWriteLock ();

	if (propMap == null)
	    propMap = new Hashtable ();
	propname = propname.trim ();
	String p2 = propname.toLowerCase ();

	Property prop = (Property) propMap.get (p2);
	if (prop != null) {
	    prop.setBooleanValue (value);
	} else {
	    prop = new Property (propname, this);
	    prop.setBooleanValue (value);
	    propMap.put (p2, prop);
	}

	// Server.throwNodeEvent (new NodeEvent (this, NodeEvent.PROPERTIES_CHANGED));
	lastmodified = System.currentTimeMillis ();
	if (state == CLEAN) markAs (MODIFIED);
    }


    public void setDate (String propname, Date value) {
	// nmgr.logEvent ("setting date prop");
	checkWriteLock ();

	if (propMap == null)
	    propMap = new Hashtable ();
	propname = propname.trim ();
	String p2 = propname.toLowerCase ();

	Property prop = (Property) propMap.get (p2);
	if (prop != null) {
	    prop.setDateValue (value);
	} else {
	    prop = new Property (propname, this);
	    prop.setDateValue (value);
	    propMap.put (p2, prop);
	}

	// Server.throwNodeEvent (new NodeEvent (this, NodeEvent.PROPERTIES_CHANGED));
	lastmodified = System.currentTimeMillis ();
	if (state == CLEAN) markAs (MODIFIED);
    }

    public void setJavaObject (String propname, Object value) {
	// nmgr.logEvent ("setting jobject prop");
	checkWriteLock ();

	if (propMap == null)
	    propMap = new Hashtable ();
	propname = propname.trim ();
	String p2 = propname.toLowerCase ();

	Property prop = (Property) propMap.get (p2);
	if (prop != null) {
	    prop.setJavaObjectValue (value);
	} else {
	    prop = new Property (propname, this);
	    prop.setJavaObjectValue (value);
	    propMap.put (p2, prop);
	}

	// Server.throwNodeEvent (new NodeEvent (this, NodeEvent.PROPERTIES_CHANGED));
	lastmodified = System.currentTimeMillis ();
	if (state == CLEAN) markAs (MODIFIED);
    }

    public void setNode (String propname, INode value) {
	// nmgr.logEvent ("setting node prop");

	// check if types match, otherwise throw exception
	DbMapping nmap = dbmap == null ? null : dbmap.getPropertyMapping (propname);
	if (nmap != null && nmap != value.getDbMapping()) {
	    if (value.getDbMapping () == null)
	        value.setDbMapping (nmap);
	    else if (!nmap.isStorageCompatible (value.getDbMapping ()))
	        throw new RuntimeException ("Can't set "+propname+" to object with prototype "+value.getPrototype()+", was expecting "+nmap.getTypeName());
	}

	checkWriteLock ();

    	Node n = null;
    	if (value instanceof Node)
	    n = (Node) value;
	else 
	    throw new RuntimeException ("Can't add fixed-transient node to a persistent node");

	// if the new node is marked as TRANSIENT and this node is not, mark new node as NEW
	if (state != TRANSIENT && n.state == TRANSIENT)
	    n.makePersistentCapable ();

	n.checkWriteLock ();

	// check if the main identity of this node is as a named property
	// or as an anonymous node in a collection
	if (n.parentHandle == null && n.adoptName && !"root".equalsIgnoreCase (n.getPrototype ())) {
	    n.setParent (this);
	    n.name = propname;
	    n.anonymous = false;
	}

	propname = propname.trim ();
	String p2 = propname.toLowerCase ();

	Property prop = propMap == null ? null : (Property) propMap.get (p2);
	if (prop != null) {
	    if (prop.type == IProperty.NODE && n.getHandle ().equals (prop.nhandle)) {
	        // nothing to do, just clean up locks and return
	        if (state == CLEAN) clearWriteLock ();
	        if (n.state == CLEAN) n.clearWriteLock ();
	        return;
	    }
	} else {
	    prop = new Property (propname, this);
	}

	prop.setNodeValue (n);
	Relation rel = dbmap == null ? null : dbmap.getPropertyRelation (propname);

	if (rel == null || rel.reftype == Relation.REFERENCE || rel.virtual ||
			rel.otherType == null || !rel.otherType.isRelational()) {
	    // the node must be stored as explicit property
	    if (propMap == null)
	        propMap = new Hashtable ();
	    propMap.put (p2, prop);
	    if (state == CLEAN) markAs (MODIFIED);
	}
	
	/* if (rel != null && rel.reftype == Relation.REFERENCE && !rel.usesPrimaryKey ()) {
	    // if the relation for this property doesn't use the primary key of the value object, make a
	    // secondary key object with the proper db column
	    String kval = n.getString (rel.otherType.columnNameToProperty (rel.getRemoteField ()), false);
	    prop.nhandle = new NodeHandle (new DbKey (n.getDbMapping (), kval, rel.getRemoteField ()));
	} */
	
	// don't check node in transactor cache if node is transient -
	// this is done anyway when the node becomes persistent.
	if (n.state != TRANSIENT) {
	    // check node in with transactor cache
	    String nID = n.getID();
	    DbMapping dbm = n.getDbMapping ();
	    Transactor tx = (Transactor) Thread.currentThread ();
	    tx.visitCleanNode (new DbKey (dbm, nID), n);
	    // if the field is not the primary key of the property, also register it
	    if (rel != null && rel.accessor != null && state != TRANSIENT) {
	        Key secKey = new SyntheticKey (getKey (), propname);
	        nmgr.evictKey (secKey);
	        tx.visitCleanNode (secKey, n);
	    }
	}

	lastmodified = System.currentTimeMillis ();
	if (n.state == DELETED) n.markAs (MODIFIED);
    }

    /**
     * Remove a property. Note that this works only for explicitly set properties, not for those
     * specified via property relation.
     */
    public void unset (String propname) {
	if (propMap == null)
	    return;
	try {
	    Property p = (Property) propMap.remove (propname.toLowerCase ());
	    if (p != null) {
	        checkWriteLock ();
	        if (p.type == Property.NODE)
	            p.unregisterNode ();
	        // Server.throwNodeEvent (new NodeEvent (this, NodeEvent.PROPERTIES_CHANGED));
	        lastmodified = System.currentTimeMillis ();
	        if (state == CLEAN)
	            markAs (MODIFIED);
	    }
	} catch (Exception ignore) {}
    }

    public long lastModified () {
	return lastmodified;
    }

    public long created () {
	return created;
    }

    public String toString () {
	return "HopObject " + name;
    }

    /**
     * Tell whether this node is stored inside a relational db. This doesn't mean
     * it actually is stored in a relational db, just that it would be, if the node was
     * persistent
     */
    public boolean isRelational () {
	return dbmap != null && dbmap.isRelational ();
    }


    /**
     * Recursively turn node status from TRANSIENT to NEW so that the Transactor will
     * know it has to insert this node.
     */
    protected void makePersistentCapable () {
	if (state == TRANSIENT) {
	    state = NEW;
	    // generate a real, persistent ID for this object
	    id = nmgr.generateID (dbmap);
	    getHandle ().becomePersistent ();
	    Transactor current = (Transactor) Thread.currentThread ();
	    current.visitNode (this);
	    current.visitCleanNode (this);
	    for (Enumeration e = getSubnodes (); e.hasMoreElements (); ) {
	        Node n = (Node) e.nextElement ();
	        if (n.state == TRANSIENT)
	            n.makePersistentCapable ();
	    }
	    for (Enumeration e = properties (); e.hasMoreElements (); ) {
	        IProperty next = get ((String) e.nextElement (), false);
	        if (next != null && next.getType () == IProperty.NODE) {
	            Node n = (Node) next.getNodeValue ();
	            if (n != null && n.state == TRANSIENT)
	                n.makePersistentCapable ();
	        }
	    }
	}
    }


    /**
     * Get the cache node for this node. This can be
     * used to store transient cache data per node from Javascript.
     */
    public synchronized INode getCacheNode () {
	if (cacheNode == null)
	    cacheNode = new TransientNode();
	return cacheNode;
    }

    /**
     * Reset the cache node for this node.
     */
    public synchronized void clearCacheNode () {
	cacheNode = null;
    }


    /**
     * This method walks down node path to the first non-virtual node and return it.
     *  limit max depth to 3, since there shouldn't be more then 2 layers of virtual nodes.
     */
    public INode getNonVirtualParent () {
	INode node = this;
	for (int i=0; i<3; i++) {
	    if (node == null) break;
	    if (node.getState() != Node.VIRTUAL)
	        return node;
	    node = node.getParent ();
	}
	return null;
    }


    /**
     *  Instances of this class may be used to mark an entry in the object cache as null.
     *  This method tells the caller whether this is the case.
     */
    public boolean isNullNode () {
	return nmgr == null;
    }

    /**
     * We overwrite hashCode to make it dependant from the prototype. That way, when the prototype
     * changes, the node will automatically get a new ESNode wrapper, since they're cached in a hashtable.
     * You gotta love these hash code tricks ;-)
     */
    public int hashCode () {
	if (prototype == null)
	    return super.hashCode ();
	else
	    return super.hashCode () + prototype.hashCode ();
    }

    public void dump () {
	System.err.println ("subnodes: "+subnodes);
	System.err.println ("properties: "+propMap);
	System.err.println ("links: "+links);
    }

}


