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
import com.workingdogs.village.*;


/**
 * An implementation of INode that can be stored in the internal database or
 * an external relational database. 
 */
 
public class Node implements INode, Serializable {

    // The ID of this node's parent node
    protected String parentID;
    // Ordered list of subnodes of this node
    private List subnodes;
    // Named subnodes (properties) of this node
    private Hashtable propMap;
    // Other nodes that link to this node. Used for reference counting/checking
    private List links;
    // Other nodes that refer to this node as property. Used for reference counting/checking
    private List proplinks;

    // the name of the (Hop) prototype - this is stored as standard property instead.
    // private String prototype;

    private String contentType;
    private byte content[];

    private long created;
    private long lastmodified;

    private String id, name;
    // is this node's main identity as a named property or an
    // anonymous node in a subnode collection?
    protected boolean anonymous = false;

    private void readObject (ObjectInputStream in) throws IOException {
	try {
	    // as a general rule of thumb, if a string can bu null use read/writeObject,
	    // if not it's save to use read/writeUTF.
	    // version indicates the serialization version
	    int version = in.readShort ();
	    id = in.readUTF ();
	    name = in.readUTF ();
	    parentID = (String) in.readObject ();
	    created = in.readLong ();
	    lastmodified = in.readLong ();
	    content = (byte[]) in.readObject ();
	    contentType = (String) in.readObject ();
	    subnodes = (ExternalizableVector) in.readObject ();
	    links = (ExternalizableVector) in.readObject ();
	    proplinks = (ExternalizableVector) in.readObject ();
	    propMap = (Hashtable) in.readObject ();
	    anonymous = in.readBoolean ();
	    if (version == 2)
	        prototype = in.readUTF ();
	    else if (version == 3)
	        prototype = (String) in.readObject ();
	} catch (ClassNotFoundException x) {
	    throw new IOException (x.toString ());
	}
    }

    private void writeObject (ObjectOutputStream out) throws IOException {
	out.writeShort (3);  // serialization version
	out.writeUTF (id);
	out.writeUTF (name);
	out.writeObject (parentID);
	out.writeLong (created);
	out.writeLong (lastmodified);
	out.writeObject (content);
	out.writeObject (contentType);
	DbMapping smap = dbmap == null ? null : dbmap.getSubnodeMapping ();
	if (smap != null && smap.isRelational ())
	    out.writeObject (null);
	else
	    out.writeObject (subnodes);
	out.writeObject (links);
	out.writeObject (proplinks);
	out.writeObject (propMap);
	out.writeBoolean (anonymous);
	out.writeObject (prototype);
    }

    transient String prototype;
    transient INode cacheNode;
    transient WrappedNodeManager nmgr;
    transient DbMapping dbmap;
    transient Key primaryKey = null;
    transient DbMapping parentmap;
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
     * This constructor is only used for instances of the NullNode subclass. Do not use for ordinary Nodes!
     */
    public Node () {
	created = lastmodified = System.currentTimeMillis ();
    }

    /**
     * Constructor used for virtual nodes.
     */
    public Node (Node home, String propname, WrappedNodeManager nmgr, String prototype) {
	this.nmgr = nmgr;
	setParent (home);
	// this.dbmap = null;
	// generate a key for the virtual node that can't be mistaken for a JDBC-URL
	this.id = Key.makeVirtualID (parentmap, parentID, propname);
	this.name = propname;
	this.anonymous = false;
	setPrototype (prototype);
	this.state = VIRTUAL;
    }

    /**
     * Constructor used for nodes being stored in a relational database table.
     */
    public Node (DbMapping dbmap, Record rec, WrappedNodeManager nmgr) throws DataSetException {
	
	// see what prototype/DbMapping this object should use
	DbMapping m = dbmap;
	String protoField= dbmap.getPrototypeField ();
	if (protoField != null) {
	    Value val = rec.getValue (protoField);
	    if (val != null && !val.isNull ()) {
	        String protoName = val.asString ();
	        m = nmgr.getDbMapping (protoName);
	        if (m == null) {
	            // invalid prototype name!
	            System.err.println ("Warning: Invalid prototype name: "+protoName+" - using default");
	            m = dbmap;
	        }
	    }
	}
	setPrototype (m.getTypeName ());
	this.dbmap = m;
	
	this.nmgr = nmgr;
	id = rec.getValue (dbmap.getIDField ()).asString ();
	checkWriteLock ();
	String nameField =  dbmap.getNameField ();
	name = nameField == null ? id : rec.getValue (nameField).asString ();
	if (name == null || name.length() == 0)
	    name = m.getTypeName() + " " + id;
	// set parent for user objects to internal userroot node
	if ("user".equals (prototype)) {
	    this.parentID = "1";
	    this.parentmap =  nmgr.getDbMapping("__userroot__");
	    anonymous = false;
	}

	created = lastmodified = System.currentTimeMillis ();
	
	for (Enumeration e=dbmap.getDB2Prop ().elements (); e.hasMoreElements();  ) {

	    Relation rel = (Relation) e.nextElement ();
	    // NOTE: this should never be the case, since we're just looping through
	    // mappnigs with a local db column
	    if (rel.direction != Relation.PRIMITIVE && rel.direction != Relation.FORWARD)
	        continue;

                 Value val = rec.getValue (rel.getDbField ());

	    if (val.isNull ())
	        continue;

	    Property newprop = new Property (rel.propname, this);

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
	    propMap.put (rel.propname.toLowerCase(), newprop);
	    // mark property as clean, since it's fresh from the db
	    newprop.dirty = false;

	    // if the property is a pointer to another node, change the property type to NODE
	    if (rel.direction == Relation.FORWARD) {
	        newprop.nvalueID = newprop.getStringValue ();
	        newprop.type = IProperty.NODE;
	    }
	}
	markAs (CLEAN);
    }


    /**
     * Creates a new Node with the given name.
     */
    public Node (String n, WrappedNodeManager nmgr) {
	this.nmgr = nmgr;
	id = nmgr.generateID (dbmap);
	checkWriteLock ();
	this.name = n == null || "".equals (n) ? id : n;
	created = lastmodified = System.currentTimeMillis ();
	adoptName = true;
	markAs (TRANSIENT);
	// nmgr.registerNode (this);
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
    *  Creates a new instance of Node, transforming from another implementation of
    *  interface INode. This Constructor is used when a transient
    *  node is converted into a persistent-capable one, hence the status is set to NEW.
    */ 
    private Node (INode node, Hashtable ntable, boolean conversionRoot, WrappedNodeManager nmgr) {
	this.nmgr = nmgr;
	this.dbmap = node.getDbMapping ();
	this.id = nmgr.generateID (dbmap);
	checkWriteLock ();
	this.name = node.getName ();
	this.prototype = node.getPrototype ();
	created = lastmodified = System.currentTimeMillis ();
	setContent (node.getContent (), node.getContentType ());
	created = lastmodified = System.currentTimeMillis ();
	ntable.put (node, this);
	// only take over name from property if this is not the root of the current node conversion
	adoptName = !conversionRoot;
	for (Enumeration e = node.getSubnodes (); e.hasMoreElements (); ) {
	    INode next = (INode) e.nextElement ();
	    Node nextc = (next instanceof Node) ? (Node) next : (Node) ntable.get (next);  // is this a Node already?
	    if (nextc == null)
	        nextc = new Node (next, ntable, true, nmgr);
	    if (nextc.state == INVALID)
	        nextc = nmgr.getNode (nextc.getID (), nextc.getDbMapping ());
	    addNode (nextc);
	}
 	for (Enumeration e = node.properties (); e.hasMoreElements (); ) {
	    IProperty next = node.get ((String) e.nextElement (), false);
	    if (next == null)
	        continue;
	    int t = next.getType ();
	    if (t == IProperty.NODE) {
	        INode n = next.getNodeValue ();
	        Node nextc = (n instanceof Node) ? (Node) n : (Node) ntable.get (n);  // is this a Node already?
	        if (nextc == null)
	            nextc = new Node (n, ntable, true, nmgr);
	        if (nextc.state == INVALID)
	            nextc = nmgr.getNode (nextc.getID (), nextc.getDbMapping ());
	        setNode (next.getName (), nextc);
	    } else if (t == IProperty.STRING) {
	        setString (next.getName (), next.getStringValue ());
	    } else if (t == IProperty.INTEGER) {
	        setInteger (next.getName (), next.getIntegerValue ());
	    } else if (t == IProperty.FLOAT) {
	        setFloat (next.getName (), next.getFloatValue ());
	    } else if (t == IProperty.BOOLEAN) {
	        setBoolean (next.getName (), next.getBooleanValue ());
	    } else if (t == IProperty.DATE) {
	        setDate (next.getName (), next.getDateValue ());
	    } else if (t == IProperty.JAVAOBJECT) {
	        setJavaObject (next.getName (), next.getJavaObjectValue ());
	    }
	}
	adoptName = true; // switch back to normal name adoption behaviour
	markAs (NEW);
	// nmgr.registerNode (this);
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
	// check if the subnodes are relational.
	// If so, clear the subnode vector.
	// DbMapping smap = dbmap == null ? null : dbmap.getSubnodeMapping ();
	// if (smap != null && smap.isRelational ())
	//     subnodes = null;
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

    /* Mark node as invalid so it is re-fetched from the database */
    public void invalidate () {
	checkWriteLock ();
	nmgr.evictNode (this);
    }


    /** 
     *  navigation-related
     */

    public String getID () {
	return id;
    }

    protected void setID (String id) {
	this.id = id;
	((Transactor) Thread.currentThread()).visitCleanNode (this);
    }

    public boolean isAnonymous () {
 	return anonymous;
    }

    public String getName () {
	return name; 
    }
    
    /**
     * Get something to identify this node within  a URL. This is the ID for anonymous nodes
     * and a property value for named properties.
     */
    public String getNameOrID () {
	// if subnodes are also mounted as properties, try to get the "nice" prop value
	// instead of the id by turning the anonymous flag off.
	// HACK: work around this for user objects to alsways return a URL like /users/username
             if ("user".equalsIgnoreCase (prototype)) {
	    anonymous = false;
	} else if (parentmap != null) {
	    Relation prel = parentmap.getPropertyRelation();
	    if (prel != null && prel.subnodesAreProperties && !prel.usesPrimaryKey ()) try {
	        Relation localrel = dbmap.columnNameToProperty (prel.getRemoteField ());
	        String propvalue = getString (localrel.propname, false);
	        if (propvalue != null && propvalue.length() > 0) {
	            setName (propvalue);
	            anonymous = false;
	            // nameProp = localrel.propname;
	        } else {
	            anonymous = true;
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
	    b.insert (0, p.getNameOrID ());
	    p = p.getParent ();

	    loopWatch++;
	    if (loopWatch > 10) {
	        b.insert (0, "...");
	        break;
	    }
	}
	return b.toString ();
    }

    public INode[] getPath () {
	int pathSize = 1;
	INode p = getParent ();

	while  (p != null) {
	    pathSize +=1;
	    p = p.getParent ();
	    if (pathSize > 100) // sanity check
	        break;
	}
	INode path[] = new INode[pathSize];
	p = this;
	for (int i = pathSize-1; i>=0; i--) {
	    path[i] = p;
	    p = p.getParent ();
	}
	return path;
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
	    primaryKey = null;
	    try {
	        ((Transactor) Thread.currentThread()).visitCleanNode (this);
	    } catch (ClassCastException ignore) {}
	}
    }

    public DbMapping getDbMapping () {
	return dbmap;
    }

    public Key getKey () {
	if (primaryKey == null)
	    primaryKey = new Key (dbmap, id);
	return primaryKey;
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
	    throw new RuntimeException ("The name of the node must not contain \"/\".");
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
	this.parentID = parent == null ? null : parent.getID();
	this.parentmap = parent == null ? null : parent.getDbMapping();
    }

    /**
     * This version of setParent additionally marks the node as anonymous or non-anonymous,
     * depending on the string argument. This is the version called from the scripting framework,
     * while the one argument version is called from within the objectmodel classes only.
     */
    public void setParent (Node parent, String propertyName) {
	// we only do that for relational nodes.
	if (dbmap == null || !dbmap.isRelational ())
	    return;

	String oldParentID = parentID;
	parentID = parent == null ? null : parent.getID();
	parentmap = parent == null ? null : parent.getDbMapping();

	if (parentID == null || parentID.equals (oldParentID))
	    // nothing changed, no need to find access property
	    return;

	if (propertyName == null) {
	    // see if we can find out the propertyName by ourselfes by looking at the
	    // parent's property relation
	    String newname = null;
	    if (parentmap != null) {
	        // first try to retrieve name via generic property relation of parent
	        Relation prel = parentmap.getPropertyRelation ();
	        if (prel != null && prel.other == dbmap && prel.direction == Relation.DIRECT) {
	            // reverse look up property used to access this via parent
	            String dbfield = prel.getRemoteField ();
	            if (dbfield != null) {
	                Relation proprel = (Relation) dbmap.getDB2Prop ().get (dbfield);
	                if (proprel != null && proprel.propname != null)
	                    newname = getString (proprel.propname, false);
	            }
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
	if (dbmap != null && dbmap.isRelational () &&
		(lastParentSet < dbmap.getLastTypeChange() || lastParentSet < lastmodified))
	    parentInfo = dbmap.getParentInfo ();

	// check if current parent candidate matches presciption, if not, try to get it
	if (parentInfo != null) {
	    for (int i=0; i<parentInfo.length; i++) {
	        ParentInfo pinfo = parentInfo[i];
	        INode pn = getNode (pinfo.propname, false);
	        if (pinfo.isroot && pn == null)
	            pn = nmgr.getNode ("0", nmgr.getDbMapping ("root"));
	        if (pn != null) {
	            // see if dbmapping specifies anonymity for this node
	            if (pinfo.virtualname != null)
	                pn = pn.getNode (pinfo.virtualname, false);
	            DbMapping dbm = pn == null ? null : pn.getDbMapping ();
	            try {
	                if (dbm != null && dbm.getSubnodeGroupby () != null) {
	                    // check for groupby
	                    Relation rel = (Relation) dbmap.getDB2Prop ().get (dbm.getSubnodeGroupby());
	                    pn = pn.getSubnode (getString (rel.propname, false));
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
	if (parentID == null) {
	    return null;
	}
	return nmgr.getNode (parentID, parentmap);
    }


    /**
     * Get parent, using cached info if it exists.
     */
    public Node getCachedParent () {
	if (parentID == null)
	    return null;
	return nmgr.getNode (parentID, parentmap);
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
	    node = convert (elem);
	// if the new node is marked as TRANSIENT and this node is not, mark new node as NEW
	if (state != TRANSIENT && node.state == TRANSIENT)
	    node.makePersistentCapable ();

	String n = node.getName();
	if (n.indexOf('/') > -1)
	    throw new RuntimeException ("\"/\" found in Node name.");

	// only lock node if it has to be modified for a change in subnodes
	if (!ignoreSubnodeChange ())
	    checkWriteLock ();
	node.checkWriteLock ();

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
	        Relation groupbyRel = (Relation) srel.other.getDB2Prop ().get (srel.groupby);
	        String groupbyProp = (groupbyRel != null) ?
	            groupbyRel.propname : srel.groupby;
	        String groupbyValue = node.getString (groupbyProp, false);
	        INode groupbyNode = getNode (groupbyValue, false);
	        // if group-by node doesn't exist, we'll create it
	        if (groupbyNode == null)
	            groupbyNode = getGroupbySubnode (groupbyValue, true);
	        groupbyNode.addNode (node);
	        checkBackLink (node);
	        return node;
	    } catch (Exception x) {
	        System.err.println ("Error adding groupby: "+x);
	        // x.printStackTrace ();
	        return null;
	    }
	}


	if (where < 0 || where > numberOfNodes ())
	    where = numberOfNodes ();
	if (node.parentID != null) {
	    // this makes the job of addLink, which means that addLink and addNode
	    // are functionally equivalent now.
	    if (!node.parentID.equals (id) || !node.anonymous) {
	        node.registerLink (this);
	    }
	}

	if (subnodes != null && subnodes.contains (node.getID ())) {
	    // Node is already subnode of this - just move to new position
	    subnodes.remove (node.getID ());
	    where = Math.min (where, numberOfNodes ());
	    subnodes.add (where, node.getID ());
	} else {
	    if (subnodes == null) subnodes = new ExternalizableVector ();
	    subnodes.add (where, node.getID ());

	    // check if properties are subnodes (_properties.aresubnodes=true)
	    if (dbmap != null && node.dbmap != null) {
	        Relation prel = dbmap.getPropertyRelation();
	        if (prel != null && prel.subnodesAreProperties && !prel.usesPrimaryKey ()) {
	            Relation localrel = node.dbmap.columnNameToProperty (prel.getRemoteField ());
	            // if no relation from db column to prop name is found, assume that both are equal
	            String propname = localrel == null ? prel.getRemoteField() : localrel.propname;
	            String prop = node.getString (propname, false);
	            if (prop != null && prop.length() > 0) {
	                INode old = getNode (prop, false);
	                if (old != null && old != node) {
	                    unset (prop);
	                    removeNode (old);
	                }
	                //     throw new RuntimeException ("Property "+prop+" is already defined for "+this);
	                setNode (prop, node);
	            }
	        }
	    }

	    if (node.parentID == null  && !"root".equalsIgnoreCase (node.getPrototype ())) {
	        node.setParent (this);
	        node.anonymous = true;
	    }
	}

	checkBackLink (node);

	lastmodified = System.currentTimeMillis ();
	lastSubnodeChange = lastmodified;
	// Server.throwNodeEvent (new NodeEvent (this, NodeEvent.SUBNODE_ADDED, node));
	return node;
    }

    private void checkBackLink (Node node) {
	// check if the subnode is in relational db and needs to link back to this
	// in order to make it a subnode
	if (dbmap != null) {
	    Relation srel = dbmap.getSubnodeRelation ();
	    if (srel != null && srel.direction == Relation.BACKWARD) {
	        Relation backlink = srel.other.columnNameToProperty (srel.getRemoteField());
	        if (backlink != null && backlink.propname != null) {
	            if (node.get (backlink.propname, false) == null) {
	                if (this.state == VIRTUAL)
	                    node.setString (backlink.propname, getNonVirtualHomeID());
	                else
	                    node.setString (backlink.propname, getID());
	            }
	        }
	    }
	}
    }

    public INode createNode () {
	return createNode (null, numberOfNodes ()); // create new node at end of subnode array
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
	Node n = new Node (nm, nmgr);
	if (anon)
	    addNode (n, where);
	else 
	    setNode (nm, n);
	return n;
    }


    /**
     * register a node that links to this node.
     */
    protected void registerLink (Node from) {
	if (links == null)
	    links = new ExternalizableVector ();
	Object fromID = from.getID ();
	if (!links.contains (fromID))
	    links.add (fromID);
    }

    public INode getSubnode (String path) {
	StringTokenizer st = new StringTokenizer (path, "/");
	Node retval = this, runner;

	while (st.hasMoreTokens () && retval != null) {
	    runner = retval;
	    String next = st.nextToken().trim().toLowerCase ();

	    if ("".equals (next)) {
	        retval = this;
	    } else {
	        runner.loadNodes ();
	        boolean found = runner.subnodes == null ? false : runner.subnodes.contains (next);

	        if (!found)
	            retval = null;
	        else {
	            Relation srel = null;
	            DbMapping smap = null;
	            if (runner.dbmap != null) {
	                srel = runner.dbmap.getSubnodeRelation ();
	                smap = runner.dbmap.getSubnodeMapping ();
	            }
	            // check if there is a group-by relation
	            if (srel != null && srel.groupby != null)
	                retval = nmgr.getNode (this, next, srel);
	            else
	                retval =  nmgr.getNode (next, smap);
	        }

	        if (retval != null && retval.parentID == null && !"root".equalsIgnoreCase (retval.getPrototype ())) {
	            retval.setParent (runner);
	            retval.anonymous = true;
	        }
	    }
	    if (retval == null) {
	        retval = (Node) runner.getNode (next, false);
	    }
	}
	return retval;
    }

    public INode getSubnodeAt (int index) {
	loadNodes ();

	if (subnodes == null)
	    return null;

	Relation srel = null;
	DbMapping smap = null;
	if (dbmap != null) {
	    srel = dbmap.getSubnodeRelation ();
	    smap = dbmap.getSubnodeMapping ();
	}
	Node retval = null;
	if (subnodes.size () > index) {
	    // check if there is a group-by relation
	    if (srel != null && srel.groupby != null)
	        retval = nmgr.getNode (this, (String) subnodes.get (index), srel);
	    else
	        retval =  nmgr.getNode ((String) subnodes.get (index), smap);
	    if (retval != null && retval.parentID == null && !"root".equalsIgnoreCase (retval.getPrototype ())) {
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

	if (subnodes.contains (sid) || create) try {
	    Relation srel = dbmap.getSubnodeRelation ();
	    Relation prel = dbmap.getPropertyRelation ();
	    boolean relational = srel.other != null && srel.other.isRelational ();

	    if (relational || create) {
	        Node node = relational ? new Node (this, sid, nmgr, null) : new Node ("groupby-"+sid, nmgr);
	        // set "groupname" property to value of groupby field
	        node.setString ("groupname", sid);

	        if (relational) {
	            DbMapping dbm = new DbMapping ();
	            Relation gsrel = srel.getGroupbySubnodeRelation();
	            dbm.setSubnodeMapping (srel.other);
	            dbm.setSubnodeRelation (gsrel);
	            if (prel != null) {
	                dbm.setPropertyMapping (prel.other);
	                dbm.setPropertyRelation (prel.getGroupbyPropertyRelation());
	            }
	            node.setDbMapping (dbm);
	            String snrel = "WHERE "+srel.groupby +"='"+sid+"'";
	            if (gsrel.direction == Relation.BACKWARD)
	                snrel += " AND "+gsrel.getRemoteField()+"='"+getNonVirtualHomeID()+"'";
	            if (gsrel.order != null)
	                snrel += " ORDER BY "+gsrel.order;
	            node.setSubnodeRelation (snrel);
	            // set prototype of groupby-node
	            node.setPrototype (gsrel.prototype);
	        } else {
	            setNode (sid, node);
	            subnodes.add (node.getID ());
	        }
	        nmgr.evictKey (node.getKey ());
	        return node;
	    }
	} catch (Exception noluck) {
	    nmgr.logEvent ("Error creating group-by node for "+sid+": "+noluck);
	}
	return null;
    }

    public boolean remove () {
	checkWriteLock ();
    	if (anonymous)
    	    getParent ().unset (name);
    	else 
	    getParent ().removeNode (this);
	return true;
    }


    public void removeNode (INode node) {
	nmgr.logEvent ("removing: "+ node);
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
	        n.links.remove (this.id);
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
	    subnodes.remove (node.getID ());

	lastSubnodeChange = System.currentTimeMillis ();

	// check if the subnode is in relational db and has a link back to this
	// which needs to be unset
	if (dbmap != null) {
	    Relation srel = dbmap.getSubnodeRelation ();
	    if (srel != null && srel.direction == Relation.BACKWARD) {
	        Relation backlink = srel.other.columnNameToProperty (srel.getRemoteField ());
	        if (backlink != null && id.equals (node.getString (backlink.propname, false)))
	            node.unset (backlink.propname);
	    }
	}

	// check if subnodes are handled as virtual fs
	if (dbmap != null && node.dbmap != null) {
	    Relation prel = dbmap.getPropertyRelation();
	    if (prel != null && prel.subnodesAreProperties && !prel.usesPrimaryKey ()) {
	        Relation localrel = node.dbmap.columnNameToProperty (prel.getRemoteField());
	        // if no relation from db column to prop name is found, assume that both are equal
	        String propname = localrel == null ? prel.getRemoteField () : localrel.propname;
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

	// notify nodes that link to this node that it is being deleted.
	int l = links == null ? 0 : links.size ();
	for (int i = 0; i < l; i++) {
	    // TODO: solve dbmap problem
	    Node link = nmgr.getNode ((String) links.get (i),  null);
	    if (link != null) link.releaseNode (this);
	}

	// clean up all nodes that use n as a property
	if (proplinks != null) {
	    for (Iterator e1 = proplinks.iterator (); e1.hasNext ();  ) try {
	        String pid = (String) e1.next ();
	        Node pnode = nmgr.getNode (pid, null);
	        if (pnode != null) {
	            nmgr.logEvent("Warning: Can't unset node property of "+pnode.getFullName ());
	        }
	    } catch (Exception ignore) {}
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
	        if (n.dbmap == null || !n.dbmap.isRelational())
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
	Relation srel = dbmap == null ? null : dbmap.getSubnodeRelation ();
	if (srel != null && srel.groupby != null && srel.other != null && srel.other.isRelational ()) {
	    if (n.getParent () != this)
	        return -1;
	    else
	        return subnodes.indexOf (n.getName ());
	} else
	    return subnodes.indexOf (n.getID ());
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
	    if (subRel.aggressiveLoading) {
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
	return (rel != null && rel.other != null && rel.other.isRelational() && rel.direction == Relation.BACKWARD);
    }

    /**
     *  Get all properties of this node.
     */
    public Enumeration properties () {

	if (dbmap != null && dbmap.getProp2DB ().size() > 0)
	    // return the properties defined in type.properties, if there are any
	    return dbmap.getProp2DB ().keys();

	Relation prel = dbmap == null ? null : dbmap.getPropertyRelation ();
	if (prel != null && prel.direction == Relation.DIRECT && !prel.subnodesAreProperties
			&& prel.other != null && prel.other.isRelational ())
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
	return "anonymous:"+anonymous+",parentID:"+parentID+",parentmap:"+parentmap+",parent:"+getParent();
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
	    prop.nvalueID = prop.getStringValue ();
	    prop.type = IProperty.NODE;
	}


	if (prop == null && dbmap != null) {
	    Relation prel =  dbmap.getPropertyRelation (propname);
	    if (prel == null)
	        prel = dbmap.getPropertyRelation ();
	    if (prel != null && (prel.direction == Relation.DIRECT || prel.virtual)) {
	        // this *may* be a relational node stored by property name
	        try {
	            Node pn = nmgr.getNode (this, propname, prel);
	            if (pn != null) {
	                if (pn.parentID == null && !"root".equalsIgnoreCase (pn.getPrototype ())) {
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
	if (prop == null && inherit && getParent () != null) {
	    prop = ((Node) getParent ()).getProperty (propname, inherit);
	}
	return prop;
    }

    public String getString (String propname, String defaultValue, boolean inherit) {
	String propValue = getString (propname, inherit);
	return propValue == null ? defaultValue : propValue;
    }

    public String getString (String propname, boolean inherit) {  
	propname = propname.toLowerCase ();
	Property prop = getProperty (propname, inherit);
	try {
	    return prop.getStringValue ();
	} catch (Exception ignore) {}
	return null;
    }

    public long getInteger (String propname, boolean inherit) {  
	propname = propname.toLowerCase ();
	Property prop = getProperty (propname, inherit);
	try {
	    return prop.getIntegerValue ();
	} catch (Exception ignore) {}
	return 0;
    }

    public double getFloat (String propname, boolean inherit) {  
	propname = propname.toLowerCase ();
	Property prop = getProperty (propname, inherit);
	try {
	    return prop.getFloatValue ();
	} catch (Exception ignore) {}
	return 0.0;
    }

    public Date getDate (String propname, boolean inherit) {  
	propname = propname.toLowerCase ();
	Property prop = getProperty (propname, inherit);
	try {
	    return prop.getDateValue ();
	} catch (Exception ignore) {}
	return null;
    }


    public boolean getBoolean (String propname, boolean inherit) {  
	propname = propname.toLowerCase ();
	Property prop = getProperty (propname, inherit);
	try {
	    return prop.getBooleanValue ();
	} catch (Exception ignore) {}
	return false;
    }

    public INode getNode (String propname, boolean inherit) {  
	propname = propname.toLowerCase ();
	Property prop = getProperty (propname, inherit);
	try {
	    return prop.getNodeValue ();
	} catch (Exception ignore) {}
	return null;
    }

    public Object getJavaObject (String propname, boolean inherit) {
	propname = propname.toLowerCase ();
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
	INode parent = parentID == null ? null : getParent ();

	if (parent != null && parent.getDbMapping() != null) {
	    // check if this node is already registered with the old name; if so, remove it.
	    // then set parent's property to this node for the new name value
	    parentmap = parent.getDbMapping ();
	    Relation prel = parentmap.getPropertyRelation ();

	    if (prel != null && prel.subnodesAreProperties && propname.equals (prel.getRemoteField())) {
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
	                nmgr.evictKey (new Key (prel.other, prel.getKeyID (parent, oldvalue)));
	            }
	        }
	        parent.setNode (value, this);
	        setName (value);
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
	    else
	        throw new RuntimeException ("Can't set "+propname+" to object with prototype "+value.getPrototype()+", was expecting "+nmap.getTypeName());
	}

	checkWriteLock ();

    	Node n = null;
    	if (value instanceof Node)
	    n = (Node) value;
	else 
	    n = convert (value);
	// if the new node is marked as TRANSIENT and this node is not, mark new node as NEW
	if (state != TRANSIENT && n.state == TRANSIENT)
	    n.makePersistentCapable ();

	n.checkWriteLock ();

	// check if the main identity of this node is as a named property
	// or as an anonymous node in a collection
	if (n.parentID == null && n.adoptName && !"root".equalsIgnoreCase (n.getPrototype ())) {
	    n.setParent (this);
	    n.name = propname;
	    n.anonymous = false;
	    // nmgr.logEvent ("adopted named node: "+n.getFullName ());
	} // else nmgr.logEvent ("not adopted: "+n.getFullName ());

	if (propMap == null)
	    propMap = new Hashtable ();
	propname = propname.trim ();
	String p2 = propname.toLowerCase ();

	Property prop = (Property) propMap.get (p2);
	if (prop != null) {
	    if (prop.type == IProperty.NODE && n.getID ().equals (prop.nvalueID)) {
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

	if (rel == null || rel.direction == Relation.FORWARD || rel.virtual || rel.other == null || !rel.other.isRelational()) {
	    // the node must be stored as explicit property
	    propMap.put (p2, prop);
	} 
	String nID = n.getID();

	// check node in with transactor cache
	Transactor tx = (Transactor) Thread.currentThread ();
	tx.visitCleanNode (new Key (nmap, nID), n);
	// if the field is not the primary key of the property, also register it
	if (rel != null && rel.direction == Relation.DIRECT && !rel.getKeyID(this, p2).equals (nID)) {
	    Key secKey = new Key (rel.other, rel.getKeyID(this, p2));
	    nmgr.evictKey (secKey);
	    tx.visitCleanNode (secKey, n);
	}

	// Server.throwNodeEvent (new NodeEvent (this, NodeEvent.SUBNODE_ADDED, n));
	// Server.throwNodeEvent (new NodeEvent (this, NodeEvent.PROPERTIES_CHANGED));
	lastmodified = System.currentTimeMillis ();
	if (state == CLEAN) markAs (MODIFIED);
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

    protected void registerPropLink (INode n) {
	if (proplinks == null)
	    proplinks = new ExternalizableVector ();
	String plid = n.getID ();
	if (!proplinks.contains (plid))
	    proplinks.add (n.getID ());
	if (state == CLEAN || state == DELETED)
	    markAs (MODIFIED);
    }

    protected void unregisterPropLink (INode n) {
	if (proplinks != null)
	    proplinks.remove (n.getID ());
	// Server.throwNodeEvent (new NodeEvent (this, NodeEvent.NODE_REMOVED));
	// Server.throwNodeEvent (new NodeEvent (n, NodeEvent.SUBNODE_REMOVED, this));
	if (state == CLEAN)
	    markAs (MODIFIED);
    }


   /* public void sanityCheck () {
	checkSubnodes ();
	checkProperties ();
	checkLinks ();
	checkPropLinks ();
	if (getParent () == null && !"root".equals (name)) {
	    System.out.println ("*** parent: "+parentID+": "+this.getName ());
	}
    }


    private void checkLinks () {
	Vector v = links == null ? null : (Vector) links.clone ();
	int l = v == null ? 0 : v.size ();
	for (int i = 0; i < l; i++) {
	    String k = (String) v.get (i);
	    Node link = nmgr.getNode (k,  null);
	    if (link == null) {
	        links.remove (k);
	        System.out.println ("**** link "+k+": "+this.getFullName ());
	        markAs (MODIFIED);
	    }
	}
    }

    private void checkPropLinks () {
	Vector v = proplinks == null ? null : (Vector) proplinks.clone ();
	int l = v == null ? 0 : v.size ();
	for (int i = 0; i < l; i++) {
	    String k = (String) v.get (i);
	    Node link = nmgr.getNode (k,  null);
	    if (link == null) {
	        proplinks.remove (k);
	        System.out.println ("**** proplink "+k+": "+this.getFullName ());
	        markAs (MODIFIED);
	    }
	}
    }

    private void checkSubnodes () {
	Vector v = subnodes == null ? null : (Vector) subnodes.clone ();
	int l = v == null ? 0 : v.size ();
	for (int i = 0; i < l; i++) {
	    String k = (String) v.get (i);
	    Node link = nmgr.getNode (k,  null);
	    if (link == null) {
	        subnodes.remove (k);
	        System.out.println ("**** subnode "+k+": "+this.getFullName ());
	        markAs (MODIFIED);
	    }
	}
    }

    private void checkProperties () {
	Hashtable ht = propMap == null ? new Hashtable () : (Hashtable) propMap.clone ();
	for (Enumeration ps = ht.elements (); ps.hasMoreElements (); ) {
	    Property p = (Property) ps.nextElement ();
	    if (p.getType () == IProperty.NODE && p.getNodeValue () == null) {
	        System.out.println ("**** property "+p.propname+"->"+p.nvalueID+": "+this.getFullName ());
	        // INode par = getParent ();
	        // if (par != null)
	        //     par.removeNode (this);
	        // markAs (DELETED);
	    }
	}

    }  */

    /**
     *  content-related
     */ 

    public boolean isText () throws IOException {
	return getContentType().indexOf ("text/") == 0;
    }

    public boolean isBinary () throws IOException {
	return getContentType().indexOf ("text/") != 0;
    }

    public String getContentType () {
	if (contentType == null)
	    return "text/plain";
	return contentType;
    }

    public void setContentType (String type) {
	checkWriteLock ();
	contentType = type;
	lastmodified = System.currentTimeMillis ();
	if (state == CLEAN) markAs (MODIFIED);
    }

    public int getContentLength () {
	if (content == null)
	    return 0;
	return content.length;
    }

    public void setContent (byte cnt[], String type) {
	checkWriteLock ();

	if (type != null)
	    contentType = type;
	content = cnt;
	lastmodified = System.currentTimeMillis ();
	// Server.throwNodeEvent (new NodeEvent (this, NodeEvent.CONTENT_CHANGED));
	if (state == CLEAN) markAs (MODIFIED);
    }


    public void setContent (String cstr) {
	checkWriteLock ();

	content = cstr.getBytes ();
	lastmodified = System.currentTimeMillis ();
	// Server.throwNodeEvent (new NodeEvent (this, NodeEvent.CONTENT_CHANGED));
	if (state == CLEAN) markAs (MODIFIED);
    }

    public byte[] getContent () {

	if (content == null || content.length == 0)
	    return "".getBytes ();

	byte retval[] = new byte[content.length];
	System.arraycopy (content, 0, retval, 0, content.length);
	// nmgr.logEvent ("copied "+retval.length+ " bytes");
	return retval;
    }

    public String getText () {
	if (content != null) {
	    if (getContentType ().startsWith ("text/")) {
	        return new String (content);
	    } else {
	        return null;
	    }
	}
	return null;
    }


    /**
     *  Get the path to eiter the general data-root or the user root, depending on 
     *  where this node is located.
     */
    public String getUrl (INode root, INode users, String tmpname, String rootproto) {
	
	String divider = "/";
	StringBuffer b = new StringBuffer ();
	INode p = this;
	int loopWatch = 0;
	
	while  (p != null && p.getParent () != null && p != root) {
	
	    if (rootproto != null && rootproto.equals (p.getPrototype ()))
	        break;
	
	    b.insert (0, divider);
	
	    // users always have a canonical URL like /users/username
	    if ("user".equals (p.getPrototype ())) {
	        b.insert (0, UrlEncoder.encode (p.getName ()));
	        p = users;
	        break;
	    }
	
	    b.insert (0, UrlEncoder.encode (p.getNameOrID ()));
	    	
	    p = p.getParent ();

	    if (loopWatch++ > 20)
	        break;
	}
	
	if (p == users) {
	    b.insert (0, divider);
	    b.insert (0, "users");
	}
	return b.toString()+UrlEncoder.encode (tmpname);
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
     * Recursively convert other implementations of INode into helma.objectmodel.db.Node.
     */
    protected Node convert (INode n) {
    	Hashtable ntable = new Hashtable ();
	Node converted = new Node (n, ntable, false, nmgr);
	return converted;
    }

    /**
     * Recursively turn node status from TRANSIENT to NEW so that the Transactor will
     * know it has to insert this node.
     */
    protected void makePersistentCapable () {
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
	state = NEW;
    }


    /**
     * Get the cache node for this node. This can be used to store transient cache data per node from Javascript.
     */
    public synchronized INode getCacheNode () {
	if (cacheNode == null)
	    cacheNode = new helma.objectmodel.Node();
	return cacheNode;
    }

    // walk down node path to the first non-virtual node and return its id.
    // limit max depth to 3, since there shouldn't be more then 2 layers of virtual nodes.
    public String getNonVirtualHomeID () {
	INode node = this;
	for (int i=0; i<3; i++) {
	    if (node == null) break;
	    if (node.getState() != Node.VIRTUAL)
	        return node.getID ();
	    node = node.getParent ();
	}
	return null;
    }

    public void dumpSubnodes () {
	System.err.println (subnodes);
    }

}

