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

package helma.objectmodel.db;

import helma.framework.IPathElement;
import helma.objectmodel.*;
import helma.util.*;
import java.io.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * An implementation of INode that can be stored in the internal database or
 * an external relational database.
 */
public final class Node implements INode, Serializable {
    static final long serialVersionUID = -3740339688506633675L;

    // The handle to the node's parent
    protected NodeHandle parentHandle;

    // Ordered list of subnodes of this node
    private List subnodes;

    // Named subnodes (properties) of this node
    private Hashtable propMap;

    // Other nodes that link to this node. Used for reference counting/checking
    private List links;
    protected long created;
    protected long lastmodified;
    private String id;
    private String name;

    // is this node's main identity as a named property or an
    // anonymous node in a subnode collection?
    protected boolean anonymous = false;

    // the serialization version this object was read from (see readObject())
    protected short version = 0;
    private transient String prototype;
    private transient NodeHandle handle;
    private transient INode cacheNode;
    transient WrappedNodeManager nmgr;
    transient DbMapping dbmap;
    transient Key primaryKey = null;
    transient String subnodeRelation = null;
    transient long lastSubnodeFetch = 0;
    transient long lastSubnodeChange = 0;
    transient long lastNameCheck = 0;
    transient long lastParentSet = 0;
    transient long lastSubnodeCount = 0; // these two are only used
    transient int subnodeCount = -1; // for aggressive loading relational subnodes
    transient private volatile Transactor lock;
    transient private int state;

    /**
     * This constructor is only used for instances of the NullNode subclass. Do not use for ordinary Nodes.
     */
    Node() {
        created = lastmodified = System.currentTimeMillis();
        nmgr = null;
    }

    /**
     * Creates a new Node with the given name. Only used by NodeManager for "root nodes" and
     * not in a Transaction context, which is why we can immediately mark it as CLEAN.
     * ADD: used by wrapped database to re-create an existing Node.
     */
    public Node(String name, String id, String prototype, WrappedNodeManager nmgr) {
        this.nmgr = nmgr;
        this.id = id;
        this.name = ((name == null) || "".equals(name)) ? id : name;

        if (prototype != null) {
            setPrototype(prototype);
        }

        created = lastmodified = System.currentTimeMillis();
        markAs(CLEAN);
    }

    /**
     * Constructor used to create a Node with a given name from a wrapped database.
     */
    public Node(String name, String id, String prototype, WrappedNodeManager nmgr,
                long created, long lastmodified) {
        this(name, id, prototype, nmgr);
        this.created = created;
        this.lastmodified = lastmodified;
    }

    /**
     * Constructor used for virtual nodes.
     */
    public Node(Node home, String propname, WrappedNodeManager nmgr, String prototype) {
        this.nmgr = nmgr;
        setParent(home);

        // this.dbmap = null;
        // generate a key for the virtual node that can't be mistaken for a Database Key
        primaryKey = new SyntheticKey(home.getKey(), propname);
        this.id = primaryKey.getID();
        this.name = propname;
        this.anonymous = false;
        setPrototype(prototype);
        this.state = VIRTUAL;
    }

    /**
     * Creates a new Node with the given name. This is used for ordinary transient nodes.
     */
    public Node(String n, String prototype, WrappedNodeManager nmgr) {
        this.nmgr = nmgr;
        this.prototype = prototype;
        dbmap = nmgr.getDbMapping(prototype);

        // the id is only generated when the node is actually checked into db,
        // or when it's explicitly requested.
        id = null;
        this.name = (n == null) ? "" : n;
        created = lastmodified = System.currentTimeMillis();
        state = TRANSIENT;
    }

    /**
     * Constructor used for nodes being stored in a relational database table.
     */
    public Node(DbMapping dbm, ResultSet rs, DbColumn[] columns, WrappedNodeManager nmgr)
         throws SQLException, IOException {
        this.nmgr = nmgr;

        // see what prototype/DbMapping this object should use
        dbmap = dbm;

        String protoField = dbmap.getPrototypeField();

        if (protoField != null) {
            String protoName = rs.getString(protoField);

            if (protoName != null) {
                dbmap = nmgr.getDbMapping(protoName);

                if (dbmap == null) {
                    // invalid prototype name!
                    System.err.println("Warning: Invalid prototype name: " + protoName +
                                       " - using default");
                    dbmap = dbm;
                }
            }
        }

        setPrototype(dbmap.getTypeName());

        id = rs.getString(dbmap.getIDField());

        // checkWriteLock ();
        String nameField = dbmap.getNameField();

        name = (nameField == null) ? id : rs.getString(nameField);

        if ((name == null) || (name.length() == 0)) {
            name = dbmap.getTypeName() + " " + id;
        }

        created = lastmodified = System.currentTimeMillis();

        for (int i = 0; i < columns.length; i++) {
            Relation rel = columns[i].getRelation();

            if ((rel == null) ||
                    ((rel.reftype != Relation.PRIMITIVE) &&
                    (rel.reftype != Relation.REFERENCE))) {
                continue;
            }

            Property newprop = new Property(rel.propName, this);

            switch (columns[i].getType()) {
                case Types.BIT:
                    newprop.setBooleanValue(rs.getBoolean(columns[i].getName()));

                    break;

                case Types.TINYINT:
                case Types.BIGINT:
                case Types.SMALLINT:
                case Types.INTEGER:
                    newprop.setIntegerValue(rs.getLong(columns[i].getName()));

                    break;

                case Types.REAL:
                case Types.FLOAT:
                case Types.DOUBLE:
                    newprop.setFloatValue(rs.getDouble(columns[i].getName()));

                    break;

                case Types.DECIMAL:
                case Types.NUMERIC:

                    BigDecimal num = rs.getBigDecimal(columns[i].getName());

                    if (num == null) {
                        break;
                    }

                    if (num.scale() > 0) {
                        newprop.setFloatValue(num.doubleValue());
                    } else {
                        newprop.setIntegerValue(num.longValue());
                    }

                    break;

                case Types.VARBINARY:
                case Types.BINARY:
                    newprop.setStringValue(rs.getString(columns[i].getName()));

                    break;

                case Types.LONGVARBINARY:
                case Types.LONGVARCHAR:

                    try {
                        newprop.setStringValue(rs.getString(columns[i].getName()));
                    } catch (SQLException x) {
                        Reader in = rs.getCharacterStream(columns[i].getName());
                        char[] buffer = new char[2048];
                        int read = 0;
                        int r = 0;

                        while ((r = in.read(buffer, read, buffer.length - read)) > -1) {
                            read += r;

                            if (read == buffer.length) {
                                // grow input buffer
                                char[] newBuffer = new char[buffer.length * 2];

                                System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
                                buffer = newBuffer;
                            }
                        }

                        newprop.setStringValue(new String(buffer, 0, read));
                    }

                    break;

                case Types.CHAR:
                case Types.VARCHAR:
                case Types.OTHER:
                    newprop.setStringValue(rs.getString(columns[i].getName()));

                    break;

                case Types.DATE:
                    newprop.setDateValue(rs.getDate(columns[i].getName()));

                    break;

                case Types.TIME:
                    newprop.setDateValue(rs.getTime(columns[i].getName()));

                    break;

                case Types.TIMESTAMP:
                    newprop.setDateValue(rs.getTimestamp(columns[i].getName()));

                    break;

                case Types.NULL:
                    newprop.setStringValue(null);

                    break;

                // continue;
                default:
                    newprop.setStringValue(rs.getString(columns[i].getName()));

                    break;
            }

            if (rs.wasNull()) {
                newprop.setStringValue(null);
            }

            if (propMap == null) {
                propMap = new Hashtable();
            }

            propMap.put(rel.propName.toLowerCase(), newprop);

            // if the property is a pointer to another node, change the property type to NODE
            if ((rel.reftype == Relation.REFERENCE) && rel.usesPrimaryKey()) {
                // FIXME: References to anything other than the primary key are not supported
                newprop.convertToNodeReference(rel.otherType);

                // newprop.nhandle = new NodeHandle (new DbKey (rel.otherType, newprop.getStringValue ()));
                // newprop.type = IProperty.NODE;
            }

            // mark property as clean, since it's fresh from the db
            newprop.dirty = false;
        }

        // again set created and lastmodified. This is because
        // lastmodified has been been updated, and we want both values to
        // be identical to show that the node hasn't been changed since
        // it was first created.
        created = lastmodified = System.currentTimeMillis();
        markAs(CLEAN);
    }

    /**
     * Read this object instance from a stream. This does some smart conversion to
     * update from previous serialization formats.
     */
    private void readObject(ObjectInputStream in) throws IOException {
        try {
            // as a general rule of thumb, if a string can be null use read/writeObject,
            // if not it's save to use read/writeUTF.
            // version indicates the serialization version
            version = in.readShort();

            String rawParentID = null;

            id = in.readUTF();
            name = in.readUTF();

            if (version < 5) {
                rawParentID = (String) in.readObject();
            } else {
                parentHandle = (NodeHandle) in.readObject();
            }

            created = in.readLong();
            lastmodified = in.readLong();

            if (version < 4) {
                // read away content and contentType, which were dropped
                in.readObject();
                in.readObject();
            }

            subnodes = (ExternalizableVector) in.readObject();
            links = (ExternalizableVector) in.readObject();

            if (version < 6) {
                // read away obsolete proplinks list
                in.readObject();
            }

            propMap = (Hashtable) in.readObject();
            anonymous = in.readBoolean();

            if (version == 2) {
                prototype = in.readUTF();
            } else if (version >= 3) {
                prototype = (String) in.readObject();
            }

            // if the input version is < 5, we have to do some conversion to make this object work
            if (version < 5) {
                if (rawParentID != null) {
                    parentHandle = new NodeHandle(new DbKey(null, rawParentID));
                }

                if (subnodes != null) {
                    for (int i = 0; i < subnodes.size(); i++) {
                        String s = (String) subnodes.get(i);

                        subnodes.set(i, new NodeHandle(new DbKey(null, s)));
                    }
                }

                if (links != null) {
                    for (int i = 0; i < links.size(); i++) {
                        String s = (String) links.get(i);

                        links.set(i, new NodeHandle(new DbKey(null, s)));
                    }
                }
            }
        } catch (ClassNotFoundException x) {
            throw new IOException(x.toString());
        }
    }

    /**
     * Write out this instance to a stream
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeShort(7); // serialization version
        out.writeUTF(id);
        out.writeUTF(name);
        out.writeObject(parentHandle);
        out.writeLong(created);
        out.writeLong(lastmodified);

        DbMapping smap = (dbmap == null) ? null : dbmap.getSubnodeMapping();

        if ((smap != null) && smap.isRelational()) {
            out.writeObject(null);
        } else {
            out.writeObject(subnodes);
        }

        out.writeObject(links);
        out.writeObject(propMap);
        out.writeBoolean(anonymous);
        out.writeObject(prototype);
    }

    /**
     * used by Xml deserialization
     */
    public void setPropMap(Hashtable propMap) {
        this.propMap = propMap;
    }

    /**
     * used by Xml deserialization
     */
    public void setSubnodes(List subnodes) {
        this.subnodes = subnodes;
    }

    protected synchronized void checkWriteLock() {
        // System.err.println ("registering writelock for "+this.getName ()+" ("+lock+") to "+Thread.currentThread ());
        if (state == TRANSIENT) {
            return; // no need to lock transient node
        }

        Transactor current = (Transactor) Thread.currentThread();

        if (!current.isActive()) {
            throw new helma.framework.TimeoutException();
        }

        if (state == INVALID) {
            nmgr.logEvent("Got Invalid Node: " + this);
            Thread.dumpStack();
            throw new ConcurrencyException("Node " + this +
                                           " was invalidated by another thread.");
        }

        if ((lock != null) && (lock != current) && lock.isAlive() && lock.isActive()) {
            nmgr.logEvent("Concurrency conflict for " + this + ", lock held by " + lock);
            throw new ConcurrencyException("Tried to modify " + this +
                                           " from two threads at the same time.");
        }

        current.visitNode(this);
        lock = current;
    }

    protected synchronized void clearWriteLock() {
        lock = null;
    }

    protected void markAs(int s) {
        if ((state == INVALID) || (state == VIRTUAL) || (state == TRANSIENT)) {
            return;
        }

        state = s;

        if (Thread.currentThread() instanceof Transactor) {
            Transactor tx = (Transactor) Thread.currentThread();

            if (s == CLEAN) {
                clearWriteLock();
                tx.dropNode(this);
            } else {
                tx.visitNode(this);

                if (s == NEW) {
                    clearWriteLock();
                    tx.visitCleanNode(this);
                }
            }
        }
    }

    /**
     *
     *
     * @return ...
     */
    public int getState() {
        return state;
    }

    /**
     *
     *
     * @param s ...
     */
    public void setState(int s) {
        this.state = s;
    }

    /**
     *  Mark node as invalid so it is re-fetched from the database
     */
    public void invalidate() {
        // This doesn't make sense for transient nodes
        if ((state == TRANSIENT) || (state == NEW)) {
            return;
        }

        checkWriteLock();
        nmgr.evictNode(this);
    }

    /**
     *  Check for a child mapping and evict the object specified by key from the cache
     */
    public void invalidateNode(String key) {
        // This doesn't make sense for transient nodes
        if ((state == TRANSIENT) || (state == NEW)) {
            return;
        }

        Relation rel = getDbMapping().getSubnodeRelation();

        if (rel != null) {
            if (rel.usesPrimaryKey()) {
                nmgr.evictNodeByKey(new DbKey(getDbMapping().getSubnodeMapping(), key));
            } else {
                nmgr.evictNodeByKey(new SyntheticKey(getKey(), key));
            }
        }
    }

    /**
     *  Get the ID of this Node. This is the primary database key and used as part of the
     *  key for the internal node cache.
     */
    public String getID() {
        // if we are transient, we generate an id on demand. It's possible that we'll never need
        // it, but if we do it's important to keep the one we have.
        if ((state == TRANSIENT) && (id == null)) {
            id = TransientNode.generateID();
        }

        return id;
    }

    /**
     * Returns true if this node is accessed by id from its aprent, false if it
     * is accessed by name
     */
    public boolean isAnonymous() {
        return anonymous;
    }

    /**
     * Return this node' name, which may or may not have some meaning
     */
    public String getName() {
        return name;
    }

    /**
     * Get something to identify this node within a URL. This is the ID for anonymous nodes
     * and a property value for named properties.
     */
    public String getElementName() {
        // if subnodes are also mounted as properties, try to get the "nice" prop value
        // instead of the id by turning the anonymous flag off.
        if ((parentHandle != null) &&
                (lastNameCheck < Math.max(dbmap.getLastTypeChange(), lastmodified))) {
            try {
                Node p = parentHandle.getNode(nmgr);
                DbMapping parentmap = p.getDbMapping();
                Relation prel = parentmap.getPropertyRelation();

                if ((prel != null) && prel.hasAccessName()) {
                    String propname = dbmap.columnNameToProperty(prel.accessName);
                    String propvalue = getString(propname);

                    if ((propvalue != null) && (propvalue.length() > 0)) {
                        setName(propvalue);
                        anonymous = false;

                        // nameProp = localrel.propName;
                    } else {
                        anonymous = true;
                    }
                } else if (!anonymous && (p.contains(this) > -1)) {
                    anonymous = true;
                }
            } catch (Exception ignore) {
                // just fall back to default method
            }

            lastNameCheck = System.currentTimeMillis();
        }

        return (anonymous || (name == null) || (name.length() == 0)) ? id : name;
    }

    /**
     *
     *
     * @return ...
     */
    public String getFullName() {
        return getFullName(null);
    }

    /**
     *
     *
     * @param root ...
     *
     * @return ...
     */
    public String getFullName(INode root) {
        String fullname = "";
        String divider = null;
        StringBuffer b = new StringBuffer();
        INode p = this;
        int loopWatch = 0;

        while ((p != null) && (p.getParent() != null) && (p != root)) {
            if (divider != null) {
                b.insert(0, divider);
            } else {
                divider = "/";
            }

            b.insert(0, p.getElementName());
            p = p.getParent();

            loopWatch++;

            if (loopWatch > 10) {
                b.insert(0, "...");

                break;
            }
        }

        return b.toString();
    }

    /**
     *
     *
     * @return ...
     */
    public String getPrototype() {
        // if prototype is null, it's a vanilla HopObject.
        if (prototype == null) {
            return "hopobject";
        }

        return prototype;
    }

    /**
     *
     *
     * @param proto ...
     */
    public void setPrototype(String proto) {
        this.prototype = proto;
    }

    /**
     *
     *
     * @param dbmap ...
     */
    public void setDbMapping(DbMapping dbmap) {
        if (this.dbmap != dbmap) {
            this.dbmap = dbmap;

            // primaryKey = null;
        }
    }

    /**
     *
     *
     * @return ...
     */
    public DbMapping getDbMapping() {
        return dbmap;
    }

    /**
     *
     *
     * @return ...
     */
    public Key getKey() {
        if (state == TRANSIENT) {
            Thread.dumpStack();
            throw new RuntimeException("getKey called on transient Node: " + this);
        }

        if ((dbmap == null) && (prototype != null) && (nmgr != null)) {
            dbmap = nmgr.getDbMapping(prototype);
        }

        if (primaryKey == null) {
            primaryKey = new DbKey(dbmap, id);
        }

        return primaryKey;
    }

    /**
     *
     *
     * @return ...
     */
    public NodeHandle getHandle() {
        if (handle == null) {
            handle = new NodeHandle(this);
        }

        return handle;
    }

    /**
     *
     *
     * @param rel ...
     */
    public void setSubnodeRelation(String rel) {
        if (((rel == null) && (this.subnodeRelation == null)) ||
                ((rel != null) && rel.equalsIgnoreCase(this.subnodeRelation))) {
            return;
        }

        checkWriteLock();
        this.subnodeRelation = rel;

        DbMapping smap = (dbmap == null) ? null : dbmap.getSubnodeMapping();

        if ((smap != null) && smap.isRelational()) {
            subnodes = null;
            subnodeCount = -1;
        }
    }

    /**
     *
     *
     * @return ...
     */
    public String getSubnodeRelation() {
        return subnodeRelation;
    }

    /**
     *
     *
     * @param name ...
     */
    public void setName(String name) {
        // "/" is used as delimiter, so it's not a legal char
        if (name.indexOf('/') > -1) {
            return;
        }

        //     throw new RuntimeException ("The name of the node must not contain \"/\".");
        if ((name == null) || (name.trim().length() == 0)) {
            this.name = id; // use id as name
        } else {
            this.name = name;
        }
    }

    /**
     * Register a node as parent of the present node. We can't refer to the node directly, so we use
     * the ID + DB map combo.
     */
    protected void setParent(Node parent) {
        parentHandle = (parent == null) ? null : parent.getHandle();
    }

    /**
     *  Set the parent handle which can be used to get the actual parent node.
     */
    public void setParentHandle(NodeHandle parent) {
        parentHandle = parent;
    }

    /**
     * This version of setParent additionally marks the node as anonymous or non-anonymous,
     * depending on the string argument. This is the version called from the scripting framework,
     * while the one argument version is called from within the objectmodel classes only.
     */
    public void setParent(Node parent, String propertyName) {
        // we only do that for relational nodes.
        if (!isRelational()) {
            return;
        }

        NodeHandle oldParentHandle = parentHandle;

        parentHandle = (parent == null) ? null : parent.getHandle();

        // mark parent as set, otherwise getParent will try to
        // determine the parent again when called.
        lastParentSet = System.currentTimeMillis();

        if ((parentHandle == null) || parentHandle.equals(oldParentHandle)) {
            // nothing changed, no need to find access property
            return;
        }

        if ((parent != null) && (propertyName == null)) {
            // see if we can find out the propertyName by ourselfes by looking at the
            // parent's property relation
            String newname = null;
            DbMapping parentmap = parent.getDbMapping();

            if (parentmap != null) {
                // first try to retrieve name via generic property relation of parent
                Relation prel = parentmap.getPropertyRelation();

                if ((prel != null) && (prel.otherType == dbmap) &&
                        (prel.accessName != null)) {
                    // reverse look up property used to access this via parent
                    Relation proprel = dbmap.columnNameToRelation(prel.accessName);

                    if ((proprel != null) && (proprel.propName != null)) {
                        newname = getString(proprel.propName);
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
    public INode getParent() {
        // check what's specified in the type.properties for this node.
        ParentInfo[] parentInfo = null;

        if (isRelational() &&
                (lastParentSet < Math.max(dbmap.getLastTypeChange(), lastmodified))) {
            parentInfo = dbmap.getParentInfo();
        }

        // check if current parent candidate matches presciption,
        // if not, try to get one that does.
        if ((parentInfo != null) && (state != TRANSIENT)) {
            for (int i = 0; i < parentInfo.length; i++) {
                ParentInfo pinfo = parentInfo[i];
                INode pn = null;

                // see if there is an explicit relation defined for this parent info
                // we only try to fetch a node if an explicit relation is specified for the prop name
                Relation rel = dbmap.propertyToRelation(pinfo.propname);

                if ((rel != null) && (rel.reftype == Relation.REFERENCE)) {
                    pn = getNode(pinfo.propname);
                }

                // the parent of this node is the app's root node...
                if ((pn == null) && pinfo.isroot) {
                    pn = nmgr.getNode("0", nmgr.getDbMapping("root"));
                }

                // if we found a parent node, check if we ought to use a virtual or groupby node as parent
                if (pn != null) {
                    // see if dbmapping specifies anonymity for this node
                    if (pinfo.virtualname != null) {
                        pn = pn.getNode(pinfo.virtualname);
                    }

                    DbMapping dbm = (pn == null) ? null : pn.getDbMapping();

                    try {
                        if ((dbm != null) && (dbm.getSubnodeGroupby() != null)) {
                            // check for groupby
                            rel = dbmap.columnNameToRelation(dbm.getSubnodeGroupby());
                            pn = pn.getSubnode(getString(rel.propName));
                        }

                        if (pn != null) {
                            setParent((Node) pn);
                            anonymous = !pinfo.named;
                            lastParentSet = System.currentTimeMillis();

                            return pn;
                        }
                    } catch (Exception ignore) {
                    }
                }
            }
        }

        // fall back to heuristic parent (the node that fetched this one from db)
        if (parentHandle == null) {
            return null;
        }

        return parentHandle.getNode(nmgr);
    }

    /**
     * Get parent, using cached info if it exists.
     */
    public Node getCachedParent() {
        if (parentHandle == null) {
            return null;
        }

        return parentHandle.getNode(nmgr);
    }

    /**
     *  INode-related
     */
    public INode addNode(INode elem) {
        return addNode(elem, numberOfNodes());
    }

    /**
     *
     *
     * @param elem ...
     * @param where ...
     *
     * @return ...
     */
    public INode addNode(INode elem, int where) {
        Node node = null;

        if (elem instanceof Node) {
            node = (Node) elem;
        } else {
            throw new RuntimeException("Can't add fixed-transient node to a persistent node");
        }

        // only lock nodes if parent node is not transient
        if (state != TRANSIENT) {
            // only lock parent if it has to be modified for a change in subnodes
            if (!ignoreSubnodeChange()) {
                checkWriteLock();
            }

            node.checkWriteLock();
        }

        // if subnodes are defined via realation, make sure its constraints are enforced.
        if ((dbmap != null) && (dbmap.getSubnodeRelation() != null)) {
            dbmap.getSubnodeRelation().setConstraints(this, node);
        }

        // if the new node is marked as TRANSIENT and this node is not, mark new node as NEW
        if ((state != TRANSIENT) && (node.state == TRANSIENT)) {
            node.makePersistentCapable();
        }

        String n = node.getName();

        // if (n.indexOf('/') > -1)
        //     throw new RuntimeException ("\"/\" found in Node name.");
        // only mark this node as modified if subnodes are not in relational db
        // pointing to this node.
        if (!ignoreSubnodeChange() && ((state == CLEAN) || (state == DELETED))) {
            markAs(MODIFIED);
        }

        if ((node.state == CLEAN) || (node.state == DELETED)) {
            node.markAs(MODIFIED);
        }

        loadNodes();

        // check if this node has a group-by subnode-relation
        if (dbmap != null) {
            Relation srel = dbmap.getSubnodeRelation();

            if ((srel != null) && (srel.groupby != null)) {
                try {
                    Relation groupbyRel = srel.otherType.columnNameToRelation(srel.groupby);
                    String groupbyProp = (groupbyRel != null) ? groupbyRel.propName
                                                              : srel.groupby;
                    String groupbyValue = node.getString(groupbyProp);
                    INode groupbyNode = getNode(groupbyValue);

                    // if group-by node doesn't exist, we'll create it
                    if (groupbyNode == null) {
                        groupbyNode = getGroupbySubnode(groupbyValue, true);
                    }

                    groupbyNode.addNode(node);

                    return node;
                } catch (Exception x) {
                    System.err.println("Error adding groupby: " + x);

                    // x.printStackTrace ();
                    return null;
                }
            }
        }

        if ((where < 0) || (where > numberOfNodes())) {
            where = numberOfNodes();
        }

        NodeHandle nhandle = node.getHandle();

        if ((subnodes != null) && subnodes.contains(nhandle)) {
            // Node is already subnode of this - just move to new position
            subnodes.remove(nhandle);
            where = Math.min(where, numberOfNodes());
            subnodes.add(where, nhandle);
        } else {
            if (subnodes == null) {
                subnodes = new ExternalizableVector();
            }

            subnodes.add(where, nhandle);

            // check if properties are subnodes (_properties.aresubnodes=true)
            if ((dbmap != null) && (node.dbmap != null)) {
                Relation prel = dbmap.getPropertyRelation();

                if ((prel != null) && (prel.accessName != null)) {
                    Relation localrel = node.dbmap.columnNameToRelation(prel.accessName);

                    // if no relation from db column to prop name is found, assume that both are equal
                    String propname = (localrel == null) ? prel.accessName
                                                         : localrel.propName;
                    String prop = node.getString(propname);

                    if ((prop != null) && (prop.length() > 0)) {
                        INode old = getNode(prop);

                        if ((old != null) && (old != node)) {
                            unset(prop);
                            removeNode(old);
                        }

                        setNode(prop, node);
                    }
                }
            }

            if (!"root".equalsIgnoreCase(node.getPrototype())) {
                // avoid calling getParent() because it would return bogus results for the not-anymore transient node
                Node nparent = (node.parentHandle == null) ? null
                                                           : node.parentHandle.getNode(nmgr);

                // if the node doesn't have a parent yet, or it has one but it's transient while we are
                // persistent, make this the nodes new parent.
                if ((nparent == null) ||
                        ((state != TRANSIENT) && (nparent.getState() == TRANSIENT))) {
                    node.setParent(this);
                    node.anonymous = true;
                } else if ((nparent != null) && ((nparent != this) || !node.anonymous)) {
                    // this makes the additional job of addLink, registering that we have a link to a node in our
                    // subnodes that actually sits somewhere else. This means that addLink and addNode
                    // are actually the same now.
                    node.registerLinkFrom(this);
                }
            }
        }

        lastmodified = System.currentTimeMillis();
        lastSubnodeChange = lastmodified;

        // Server.throwNodeEvent (new NodeEvent (this, NodeEvent.SUBNODE_ADDED, node));
        return node;
    }

    /**
     *
     *
     * @return ...
     */
    public INode createNode() {
        // create new node at end of subnode array
        return createNode(null, numberOfNodes());
    }

    /**
     *
     *
     * @param where ...
     *
     * @return ...
     */
    public INode createNode(int where) {
        return createNode(null, where);
    }

    /**
     *
     *
     * @param nm ...
     *
     * @return ...
     */
    public INode createNode(String nm) {
        // parameter where is  ignored if nm != null so we try to avoid calling numberOfNodes()
        return createNode(nm, (nm == null) ? numberOfNodes() : 0);
    }

    /**
     *
     *
     * @param nm ...
     * @param where ...
     *
     * @return ...
     */
    public INode createNode(String nm, int where) {
        checkWriteLock();

        boolean anon = false;

        if ((nm == null) || "".equals(nm.trim())) {
            anon = true;
        }

        Node n = new Node(nm, null, nmgr);

        if (anon) {
            addNode(n, where);
        } else {
            setNode(nm, n);
        }

        return n;
    }

    /**
     * register a node that links to this node so we can notify it when we cease to exist.
     * this is only necessary if we are a non-relational node, since for relational nodes
     * the referring object will notice that we've gone at runtime.
     */
    protected void registerLinkFrom(Node from) {
        if (isRelational()) {
            return;
        }

        if (from.getState() == TRANSIENT) {
            return;
        }

        if (links == null) {
            links = new ExternalizableVector();
        }

        Object fromHandle = from.getHandle();

        if (!links.contains(fromHandle)) {
            links.add(fromHandle);
        }
    }

    /**
     * This implements the getChild() method of the IPathElement interface
     */
    public IPathElement getChildElement(String name) {
        if (dbmap != null) {
            // if a dbmapping is provided, check what it tells us about
            // getting this specific child element
            Relation rel = dbmap.getExactPropertyRelation(name);

            if (rel != null) {
                return (IPathElement) getNode(name);
            }

            rel = dbmap.getSubnodeRelation();

            if ((rel != null) && (rel.groupby == null) && (rel.accessName != null)) {
                if ((rel.otherType != null) && rel.otherType.isRelational()) {
                    return (IPathElement) nmgr.getNode(this, name, rel);
                } else {
                    return (IPathElement) getNode(name);
                }
            }

            return (IPathElement) getSubnode(name);
        } else {
            // no dbmapping - just try child collection first, then named property.
            IPathElement child = (IPathElement) getSubnode(name);

            if (child == null) {
                child = (IPathElement) getNode(name);
            }

            return child;
        }
    }

    /**
     * This implements the getParentElement() method of the IPathElement interface
     */
    public IPathElement getParentElement() {
        return getParent();
    }

    /**
     *
     *
     * @param subid ...
     *
     * @return ...
     */
    public INode getSubnode(String subid) {
        // System.err.println ("GETSUBNODE : "+this+" > "+subid);
        if ("".equals(subid)) {
            return this;
        }

        Node retval = null;

        if (subid != null) {
            loadNodes();

            if ((subnodes == null) || (subnodes.size() == 0)) {
                return null;
            }

            NodeHandle nhandle = null;
            int l = subnodes.size();

            for (int i = 0; i < l; i++)
                try {
                    NodeHandle shandle = (NodeHandle) subnodes.get(i);

                    if (subid.equals(shandle.getID())) {
                        // System.err.println ("FOUND SUBNODE: "+shandle);
                        nhandle = shandle;

                        break;
                    }
                } catch (Exception x) {
                    break;
                }

            if (nhandle != null) {
                retval = nhandle.getNode(nmgr);
            }

            // This would be an alternative way to do it, without loading the subnodes:
            //    if (dbmap != null && dbmap.getSubnodeRelation () != null)
            //         retval = nmgr.getNode (this, subid, dbmap.getSubnodeRelation ());
            if ((retval != null) && (retval.parentHandle == null) &&
                    !"root".equalsIgnoreCase(retval.getPrototype())) {
                retval.setParent(this);
                retval.anonymous = true;
            }
        }

        return retval;
    }

    /**
     *
     *
     * @param index ...
     *
     * @return ...
     */
    public INode getSubnodeAt(int index) {
        loadNodes();

        if (subnodes == null) {
            return null;
        }

        DbMapping smap = null;

        if (dbmap != null) {
            smap = dbmap.getSubnodeMapping();
        }

        Node retval = null;

        if (subnodes.size() > index) {
            // check if there is a group-by relation
            retval = ((NodeHandle) subnodes.get(index)).getNode(nmgr);

            if ((retval != null) && (retval.parentHandle == null) &&
                    !"root".equalsIgnoreCase(retval.getPrototype())) {
                retval.setParent(this);
                retval.anonymous = true;
            }
        }

        return retval;
    }

    /**
     *
     *
     * @param sid ...
     * @param create ...
     *
     * @return ...
     */
    public Node getGroupbySubnode(String sid, boolean create) {
        loadNodes();

        if (subnodes == null) {
            subnodes = new ExternalizableVector();
        }

        NodeHandle ghandle = new NodeHandle(new SyntheticKey(getKey(), sid));

        if (subnodes.contains(ghandle) || create) {
            try {
                DbMapping groupbyMapping = dbmap.getGroupbyMapping();
                boolean relational = groupbyMapping.getSubnodeMapping().isRelational();

                if (relational || create) {
                    Node node = relational ? new Node(this, sid, nmgr, null)
                                           : new Node("groupby-" + sid, null, nmgr);

                    // set "groupname" property to value of groupby field
                    node.setString("groupname", sid);

                    if (relational) {
                        node.setDbMapping(groupbyMapping);
                    } else {
                        setNode(sid, node);
                        subnodes.add(node.getHandle());
                    }

                    node.setPrototype(groupbyMapping.getTypeName());
                    nmgr.evictKey(node.getKey());

                    return node;
                }
            } catch (Exception noluck) {
                nmgr.logEvent("Error creating group-by node for " + sid + ": " + noluck);
                noluck.printStackTrace();
            }
        }

        return null;
    }

    /**
     *
     *
     * @return ...
     */
    public boolean remove() {
        checkWriteLock();

        try {
            if (!anonymous) {
                getParent().unset(name);
            } else {
                getParent().removeNode(this);
            }
        } catch (Exception x) {
            return false;
        }

        return true;
    }

    /**
     *
     *
     * @param node ...
     */
    public void removeNode(INode node) {
        // nmgr.logEvent ("removing: "+ node);
        Node n = (Node) node;

        checkWriteLock();
        n.checkWriteLock();

        // need to query parent before releaseNode is called, since this may change the parent
        // to the next option described in the type.properties _parent info
        INode parent = n.getParent();

        releaseNode(n);

        if (parent == this) {
            n.deepRemoveNode();
        } else {
            // removed just a link, not the main node.
            if (n.links != null) {
                n.links.remove(getHandle());

                if (n.state == CLEAN) {
                    n.markAs(MODIFIED);
                }
            }
        }
    }

    /**
     * "Locally" remove a subnode from the subnodes table.
     *  The logical stuff necessary for keeping data consistent is done in removeNode().
     */
    protected void releaseNode(Node node) {
        if (subnodes != null) {
            subnodes.remove(node.getHandle());
        }

        lastSubnodeChange = System.currentTimeMillis();

        // check if the subnode is in relational db and has a link back to this
        // which needs to be unset
        if (dbmap != null) {
            Relation srel = dbmap.getSubnodeRelation();
        }

        // check if subnodes are also accessed as properties. If so, also unset the property
        if ((dbmap != null) && (node.dbmap != null)) {
            Relation prel = dbmap.getPropertyRelation();

            if ((prel != null) && (prel.accessName != null)) {
                Relation localrel = node.dbmap.columnNameToRelation(prel.accessName);

                // if no relation from db column to prop name is found, assume that both are equal
                String propname = (localrel == null) ? prel.accessName : localrel.propName;
                String prop = node.getString(propname);

                if ((prop != null) && (getNode(prop) == node)) {
                    unset(prop);
                }
            }
        }

        // If subnodes are relational no need to mark this node as modified
        if (ignoreSubnodeChange()) {
            return;
        }

        // Server.throwNodeEvent (new NodeEvent (node, NodeEvent.NODE_REMOVED));
        // Server.throwNodeEvent (new NodeEvent (this, NodeEvent.SUBNODE_REMOVED, node));
        lastmodified = System.currentTimeMillis();

        // nmgr.logEvent ("released node "+node +" from "+this+"     oldobj = "+what);
        if (state == CLEAN) {
            markAs(MODIFIED);
        }
    }

    /**
     * Delete the node from the db. This mainly tries to notify all nodes referring to this that
     * it's going away. For nodes from the embedded db it also does a cascading delete, since
     * it can tell which nodes are actual children and which are just linked in.
     */
    protected void deepRemoveNode() {
        // notify nodes that link to this node being deleted.
        int l = (links == null) ? 0 : links.size();

        for (int i = 0; i < l; i++) {
            NodeHandle lhandle = (NodeHandle) links.get(i);
            Node link = lhandle.getNode(nmgr);

            if (link != null) {
                link.releaseNode(this);
            }
        }

        // tell all nodes that are properties of n that they are no longer used as such
        if (propMap != null) {
            for (Enumeration e2 = propMap.elements(); e2.hasMoreElements();) {
                Property p = (Property) e2.nextElement();

                if ((p != null) && (p.getType() == Property.NODE)) {
                    p.unregisterNode();
                }
            }
        }

        // cascading delete of all subnodes. This is never done for relational subnodes, because
        // the parent info is not 100% accurate for them.
        if (subnodes != null) {
            Vector v = new Vector();

            // remove modifies the Vector we are enumerating, so we are extra careful.
            for (Enumeration e3 = getSubnodes(); e3.hasMoreElements();) {
                v.add(e3.nextElement());
            }

            int m = v.size();

            for (int i = 0; i < m; i++) {
                // getParent() is heuristical/implicit for relational nodes, so we don't base
                // a cascading delete on that criterium for relational nodes.
                Node n = (Node) v.get(i);

                if (!n.isRelational()) {
                    removeNode(n);
                }
            }
        }

        // mark the node as deleted
        setParent(null);
        markAs(DELETED);
    }

    /**
     *
     *
     * @param n ...
     *
     * @return ...
     */
    public int contains(INode n) {
        if (n == null) {
            return -1;
        }

        loadNodes();

        if (subnodes == null) {
            return -1;
        }

        // if the node contains relational groupby subnodes, the subnodes vector contains the names instead of ids.
        if (!(n instanceof Node)) {
            return -1;
        }

        Node node = (Node) n;

        return subnodes.indexOf(node.getHandle());
    }

    /**
     * Count the subnodes of this node. If they're stored in a relational data source, we
     * may actually load their IDs in order to do this.
     */
    public int numberOfNodes() {
        // If the subnodes are loaded aggressively, we really just
        // do a count statement, otherwise we just return the size of the id index.
        // (after loading it, if it's coming from a relational data source).
        DbMapping smap = (dbmap == null) ? null : dbmap.getSubnodeMapping();

        if ((smap != null) && smap.isRelational()) {
            // check if subnodes need to be rechecked
            Relation subRel = dbmap.getSubnodeRelation();

            // do not fetch subnodes for nodes that haven't been persisted yet or are in
            // the process of being persistified - except if "manual" subnoderelation is set.
            if (subRel.aggressiveLoading &&
                    (((state != TRANSIENT) && (state != NEW)) ||
                    (subnodeRelation != null))) {
                // we don't want to load *all* nodes if we just want to count them
                long lastChange = subRel.aggressiveCaching ? lastSubnodeChange
                                                           : smap.getLastDataChange();

                // also reload if the type mapping has changed.
                lastChange = Math.max(lastChange, dbmap.getLastTypeChange());

                if ((lastChange < lastSubnodeFetch) && (subnodes != null)) {
                    // we can use the nodes vector to determine number of subnodes
                    subnodeCount = subnodes.size();
                    lastSubnodeCount = System.currentTimeMillis();
                } else if ((lastChange >= lastSubnodeCount) || (subnodeCount < 0)) {
                    // count nodes in db without fetching anything
                    subnodeCount = nmgr.countNodes(this, subRel);
                    lastSubnodeCount = System.currentTimeMillis();
                }

                return subnodeCount;
            }
        }

        loadNodes();

        return (subnodes == null) ? 0 : subnodes.size();
    }

    /**
     * Make sure the subnode index is loaded for subnodes stored in a relational data source.
     *  Depending on the subnode.loadmode specified in the type.properties, we'll load just the
     *  ID index or the actual nodes.
     */
    protected void loadNodes() {
        // Don't do this for transient nodes which don't have an explicit subnode relation set
        if (((state == TRANSIENT) || (state == NEW)) && (subnodeRelation == null)) {
            return;
        }

        DbMapping smap = (dbmap == null) ? null : dbmap.getSubnodeMapping();

        if ((smap != null) && smap.isRelational()) {
            // check if subnodes need to be reloaded
            Relation subRel = dbmap.getSubnodeRelation();

            synchronized (this) {
                long lastChange = subRel.aggressiveCaching ? lastSubnodeChange
                                                           : smap.getLastDataChange();

                // also reload if the type mapping has changed.
                lastChange = Math.max(lastChange, dbmap.getLastTypeChange());

                if ((lastChange >= lastSubnodeFetch) || (subnodes == null)) {
                    if (subRel.aggressiveLoading) {
                        subnodes = nmgr.getNodes(this, dbmap.getSubnodeRelation());
                    } else {
                        subnodes = nmgr.getNodeIDs(this, dbmap.getSubnodeRelation());
                    }

                    lastSubnodeFetch = System.currentTimeMillis();
                }
            }
        }
    }

    /**
     *
     *
     * @param startIndex ...
     * @param length ...
     *
     * @throws Exception ...
     */
    public void prefetchChildren(int startIndex, int length)
                          throws Exception {
        if (length < 1) {
            return;
        }

        if (startIndex < 0) {
            return;
        }

        loadNodes();

        if (subnodes == null) {
            return;
        }

        if (startIndex >= subnodes.size()) {
            return;
        }

        int l = Math.min(subnodes.size() - startIndex, length);

        if (l < 1) {
            return;
        }

        Key[] keys = new Key[l];

        for (int i = 0; i < l; i++) {
            keys[i] = ((NodeHandle) subnodes.get(i + startIndex)).getKey();
        }

        nmgr.nmgr.prefetchNodes(this, dbmap.getSubnodeRelation(), keys);
    }

    /**
     *
     *
     * @return ...
     */
    public Enumeration getSubnodes() {
        loadNodes();
        class Enum implements Enumeration {
            int count = 0;

            public boolean hasMoreElements() {
                return count < numberOfNodes();
            }

            public Object nextElement() {
                return getSubnodeAt(count++);
            }
        }

        return new Enum();
    }

    /**
     *
     *
     * @return ...
     */
    public List getSubnodeList() {
        return subnodes;
    }

    private boolean ignoreSubnodeChange() {
        // return true if a change in subnodes can be ignored because it is
        // stored in the subnodes themselves.
        Relation rel = (dbmap == null) ? null : dbmap.getSubnodeRelation();

        return ((rel != null) && (rel.otherType != null) && rel.otherType.isRelational());
    }

    /**
     *  Get all properties of this node.
     */
    public Enumeration properties() {
        if ((dbmap != null) && dbmap.isRelational()) {
            // return the properties defined in type.properties, if there are any
            return dbmap.getPropertyEnumeration();
        }

        Relation prel = (dbmap == null) ? null : dbmap.getPropertyRelation();

        if ((prel != null) && prel.hasAccessName() && (prel.otherType != null) &&
                prel.otherType.isRelational()) {
            // return names of objects from a relational db table
            return nmgr.getPropertyNames(this, prel).elements();
        } else if (propMap != null) {
            // return the actually explicitly stored properties
            return propMap.keys();
        }

        // sorry, no properties for this Node
        return new EmptyEnumeration();

        // NOTE: we don't enumerate node properties here
        // return propMap == null ? new Vector ().elements () : propMap.elements ();
    }

    /**
     *
     *
     * @return ...
     */
    public Hashtable getPropMap() {
        return propMap;
    }

    /**
     *
     *
     * @param propname ...
     *
     * @return ...
     */
    public IProperty get(String propname) {
        return getProperty(propname);
    }

    /**
     *
     *
     * @return ...
     */
    public String getParentInfo() {
        return "anonymous:" + anonymous + ",parentHandle" + parentHandle + ",parent:" +
               getParent();
    }

    protected Property getProperty(String propname) {
        // nmgr.logEvent ("GETTING PROPERTY: "+propname);
        if (propname == null) {
            return null;
        }

        Property prop = (propMap == null) ? null
                                          : (Property) propMap.get(propname.toLowerCase());

        // See if this could be a relationally linked node which still doesn't know
        // (i.e, still thinks it's just the key as a string)
        DbMapping pmap = (dbmap == null) ? null : dbmap.getExactPropertyMapping(propname);

        if ((pmap != null) && (prop != null) && (prop.getType() != IProperty.NODE)) {
            // this is a relational node stored by id but we still think it's just a string. Fix it
            prop.convertToNodeReference(pmap);
        }

        // the property does not exist in our propmap - see if we should create it on the fly,
        // either because it is mapped to an object from relational database or defined as 
        // collection aka virtual node
        if ((prop == null) && (dbmap != null)) {
            Relation propRel = dbmap.getPropertyRelation(propname);

            // if no property relation is defined for this specific property name, 
            // use the generic property relation, if one is defined.
            if (propRel == null) {
                propRel = dbmap.getPropertyRelation();
            }

            // so if we have a property relation and it does in fact link to another object...
            if ((propRel != null) && (propRel.isCollection() || propRel.isComplexReference())) {
                // in some cases we just want to create and set a generic node without consulting
                // the NodeManager if it exists: When we get a collection (aka virtual node)
                // from a transient node for the first time, or when we get a collection whose
                // content objects are stored in the embedded XML data storage.
                if ((state == TRANSIENT) && propRel.virtual) {
                    INode node = new Node(propname, propRel.getPrototype(), nmgr);

                    node.setDbMapping(propRel.getVirtualMapping());
                    setNode(propname, node);
                    prop = (Property) propMap.get(propname);
                }
                // if this is from relational database only fetch if this node
                // is itself persistent.
                else if ((state != TRANSIENT) && propRel.createOnDemand()) {
                    // this may be a relational node stored by property name
                    // try {
                        Node pn = nmgr.getNode(this, propname, propRel);

                        if (pn != null) {
                            if ((pn.parentHandle == null) &&
                                    !"root".equalsIgnoreCase(pn.getPrototype())) {
                                pn.setParent(this);
                                pn.name = propname;
                                pn.anonymous = false;
                            }

                            prop = new Property(propname, this, pn);
                        }
                    // } catch (RuntimeException nonode) {
                        // wasn't a node after all
                    // }
                }
            }
        }

        return prop;
    }

    /**
     *
     *
     * @param propname ...
     *
     * @return ...
     */
    public String getString(String propname) {
        // propname = propname.toLowerCase ();
        Property prop = getProperty(propname);

        try {
            return prop.getStringValue();
        } catch (Exception ignore) {
        }

        return null;
    }

    /**
     *
     *
     * @param propname ...
     *
     * @return ...
     */
    public long getInteger(String propname) {
        // propname = propname.toLowerCase ();
        Property prop = getProperty(propname);

        try {
            return prop.getIntegerValue();
        } catch (Exception ignore) {
        }

        return 0;
    }

    /**
     *
     *
     * @param propname ...
     *
     * @return ...
     */
    public double getFloat(String propname) {
        // propname = propname.toLowerCase ();
        Property prop = getProperty(propname);

        try {
            return prop.getFloatValue();
        } catch (Exception ignore) {
        }

        return 0.0;
    }

    /**
     *
     *
     * @param propname ...
     *
     * @return ...
     */
    public Date getDate(String propname) {
        // propname = propname.toLowerCase ();
        Property prop = getProperty(propname);

        try {
            return prop.getDateValue();
        } catch (Exception ignore) {
        }

        return null;
    }

    /**
     *
     *
     * @param propname ...
     *
     * @return ...
     */
    public boolean getBoolean(String propname) {
        // propname = propname.toLowerCase ();
        Property prop = getProperty(propname);

        try {
            return prop.getBooleanValue();
        } catch (Exception ignore) {
        }

        return false;
    }

    /**
     *
     *
     * @param propname ...
     *
     * @return ...
     */
    public INode getNode(String propname) {
        // propname = propname.toLowerCase ();
        Property prop = getProperty(propname);

        try {
            return prop.getNodeValue();
        } catch (Exception ignore) {
        }

        return null;
    }

    /**
     *
     *
     * @param propname ...
     *
     * @return ...
     */
    public Object getJavaObject(String propname) {
        // propname = propname.toLowerCase ();
        Property prop = getProperty(propname);

        try {
            return prop.getJavaObjectValue();
        } catch (Exception ignore) {
        }

        return null;
    }

    /**
     * Directly set a property on this node
     *
     * @param propname ...
     * @param value ...
     */
    protected void set(String propname, Object value, int type) {
        checkWriteLock();

        if (propMap == null) {
            propMap = new Hashtable();
        }

        propname = propname.trim();

        String p2 = propname.toLowerCase();

        Property prop = (Property) propMap.get(p2);

        if (prop != null) {
            prop.setValue(value, type);
        } else {
            prop = new Property(propname, this);
            prop.setValue(value, type);
            propMap.put(p2, prop);
        }

        // Server.throwNodeEvent (new NodeEvent (this, NodeEvent.PROPERTIES_CHANGED));
        lastmodified = System.currentTimeMillis();

        if (state == CLEAN) {
            markAs(MODIFIED);
        }
    }

    /**
     *
     *
     * @param propname ...
     * @param value ...
     */
    public void setString(String propname, String value) {
        // nmgr.logEvent ("setting String prop");
        checkWriteLock();

        if (propMap == null) {
            propMap = new Hashtable();
        }

        propname = propname.trim();

        String p2 = propname.toLowerCase();

        Property prop = (Property) propMap.get(p2);
        String oldvalue = null;

        if (prop != null) {
            oldvalue = prop.getStringValue();

            // check if the value has changed
            if ((value != null) && value.equals(oldvalue)) {
                return;
            }

            prop.setStringValue(value);
        } else {
            prop = new Property(propname, this);
            prop.setStringValue(value);
            propMap.put(p2, prop);
        }

        // check if this may have an effect on the node's URL when using accessname
        // but only do this if we already have a parent set, i.e. if we are already stored in the db
        Node parent = (parentHandle == null) ? null : (Node) getParent();

        if ((dbmap != null) && (parent != null) && (parent.getDbMapping() != null)) {
            // check if this node is already registered with the old name; if so, remove it.
            // then set parent's property to this node for the new name value
            DbMapping parentmap = parent.getDbMapping();
            Relation propRel = parentmap.getPropertyRelation();
            String dbcolumn = dbmap.propertyToColumnName(propname);

            if ((propRel != null) && (propRel.accessName != null) &&
                    propRel.accessName.equals(dbcolumn)) {
                INode n = parent.getNode(value);

                if ((n != null) && (n != this)) {
                    parent.unset(value);
                    parent.removeNode(n);
                }

                if (oldvalue != null) {
                    n = parent.getNode(oldvalue);

                    if (n == this) {
                        parent.unset(oldvalue);
                        parent.addNode(this);

                        // let the node cache know this key's not for this node anymore.
                        nmgr.evictKey(new SyntheticKey(parent.getKey(), oldvalue));
                    }
                }

                parent.setNode(value, this);
                setName(value);
            }
        }

        // check if the property we're setting specifies the prototype of this object.
        if ((dbmap != null) && (dbmap.getPrototypeField() != null)) {
            String pn = dbmap.columnNameToProperty(dbmap.getPrototypeField());

            if (propname.equals(pn)) {
                DbMapping newmap = nmgr.getDbMapping(value);

                if (newmap != null) {
                    // see if old and new prototypes have same storage - otherwise type change is ignored
                    String oldStorage = dbmap.getStorageTypeName();
                    String newStorage = newmap.getStorageTypeName();

                    if (((oldStorage == null) && (newStorage == null)) ||
                            ((oldStorage != null) && oldStorage.equals(newStorage))) {
                        dbmap.notifyDataChange();
                        newmap.notifyDataChange();
                        this.dbmap = newmap;
                        this.prototype = value;
                    }
                }
            }
        }

        // Server.throwNodeEvent (new NodeEvent (this, NodeEvent.PROPERTIES_CHANGED));
        lastmodified = System.currentTimeMillis();

        if (state == CLEAN) {
            markAs(MODIFIED);
        }
    }

    /**
     *
     *
     * @param propname ...
     * @param value ...
     */
    public void setInteger(String propname, long value) {
        // nmgr.logEvent ("setting bool prop");
        checkWriteLock();

        if (propMap == null) {
            propMap = new Hashtable();
        }

        propname = propname.trim();

        String p2 = propname.toLowerCase();

        Property prop = (Property) propMap.get(p2);

        if (prop != null) {
            prop.setIntegerValue(value);
        } else {
            prop = new Property(propname, this);
            prop.setIntegerValue(value);
            propMap.put(p2, prop);
        }

        // Server.throwNodeEvent (new NodeEvent (this, NodeEvent.PROPERTIES_CHANGED));
        lastmodified = System.currentTimeMillis();

        if (state == CLEAN) {
            markAs(MODIFIED);
        }
    }

    /**
     *
     *
     * @param propname ...
     * @param value ...
     */
    public void setFloat(String propname, double value) {
        // nmgr.logEvent ("setting bool prop");
        checkWriteLock();

        if (propMap == null) {
            propMap = new Hashtable();
        }

        propname = propname.trim();

        String p2 = propname.toLowerCase();

        Property prop = (Property) propMap.get(p2);

        if (prop != null) {
            prop.setFloatValue(value);
        } else {
            prop = new Property(propname, this);
            prop.setFloatValue(value);
            propMap.put(p2, prop);
        }

        // Server.throwNodeEvent (new NodeEvent (this, NodeEvent.PROPERTIES_CHANGED));
        lastmodified = System.currentTimeMillis();

        if (state == CLEAN) {
            markAs(MODIFIED);
        }
    }

    /**
     *
     *
     * @param propname ...
     * @param value ...
     */
    public void setBoolean(String propname, boolean value) {
        // nmgr.logEvent ("setting bool prop");
        checkWriteLock();

        if (propMap == null) {
            propMap = new Hashtable();
        }

        propname = propname.trim();

        String p2 = propname.toLowerCase();

        Property prop = (Property) propMap.get(p2);

        if (prop != null) {
            prop.setBooleanValue(value);
        } else {
            prop = new Property(propname, this);
            prop.setBooleanValue(value);
            propMap.put(p2, prop);
        }

        // Server.throwNodeEvent (new NodeEvent (this, NodeEvent.PROPERTIES_CHANGED));
        lastmodified = System.currentTimeMillis();

        if (state == CLEAN) {
            markAs(MODIFIED);
        }
    }

    /**
     *
     *
     * @param propname ...
     * @param value ...
     */
    public void setDate(String propname, Date value) {
        // nmgr.logEvent ("setting date prop");
        checkWriteLock();

        if (propMap == null) {
            propMap = new Hashtable();
        }

        propname = propname.trim();

        String p2 = propname.toLowerCase();

        Property prop = (Property) propMap.get(p2);

        if (prop != null) {
            prop.setDateValue(value);
        } else {
            prop = new Property(propname, this);
            prop.setDateValue(value);
            propMap.put(p2, prop);
        }

        // Server.throwNodeEvent (new NodeEvent (this, NodeEvent.PROPERTIES_CHANGED));
        lastmodified = System.currentTimeMillis();

        if (state == CLEAN) {
            markAs(MODIFIED);
        }
    }

    /**
     *
     *
     * @param propname ...
     * @param value ...
     */
    public void setJavaObject(String propname, Object value) {
        // nmgr.logEvent ("setting jobject prop");
        checkWriteLock();

        if (propMap == null) {
            propMap = new Hashtable();
        }

        propname = propname.trim();

        String p2 = propname.toLowerCase();

        Property prop = (Property) propMap.get(p2);

        if (prop != null) {
            prop.setJavaObjectValue(value);
        } else {
            prop = new Property(propname, this);
            prop.setJavaObjectValue(value);
            propMap.put(p2, prop);
        }

        // Server.throwNodeEvent (new NodeEvent (this, NodeEvent.PROPERTIES_CHANGED));
        lastmodified = System.currentTimeMillis();

        if (state == CLEAN) {
            markAs(MODIFIED);
        }
    }

    /**
     *
     *
     * @param propname ...
     * @param value ...
     */
    public void setNode(String propname, INode value) {
        // nmgr.logEvent ("setting node prop");
        // check if types match, otherwise throw exception
        DbMapping nmap = (dbmap == null) ? null : dbmap.getPropertyMapping(propname);

        if ((nmap != null) && (nmap != value.getDbMapping())) {
            if (value.getDbMapping() == null) {
                value.setDbMapping(nmap);
            } else if (!nmap.isStorageCompatible(value.getDbMapping())) {
                throw new RuntimeException("Can't set " + propname +
                                           " to object with prototype " +
                                           value.getPrototype() + ", was expecting " +
                                           nmap.getTypeName());
            }
        }

        if (state != TRANSIENT) {
            checkWriteLock();
        }

        Node n = null;

        if (value instanceof Node) {
            n = (Node) value;
        } else {
            throw new RuntimeException("Can't add fixed-transient node to a persistent node");
        }

        // if the new node is marked as TRANSIENT and this node is not, mark new node as NEW
        if ((state != TRANSIENT) && (n.state == TRANSIENT)) {
            n.makePersistentCapable();
        }

        if (state != TRANSIENT) {
            n.checkWriteLock();
        }

        // check if the main identity of this node is as a named property
        // or as an anonymous node in a collection
        if ((n.parentHandle == null) && !"root".equalsIgnoreCase(n.getPrototype())) {
            n.setParent(this);
            n.name = propname;
            n.anonymous = false;
        }

        propname = propname.trim();

        String p2 = propname.toLowerCase();

        Relation rel = (dbmap == null) ? null : dbmap.getPropertyRelation(propname);

        if (rel != null && (rel.countConstraints() > 1 || rel.isComplexReference())) {
            rel.setConstraints(this, n);
            if (rel.isComplexReference()) {
                Key key = new MultiKey(n.getDbMapping(), rel.getKeyParts(this));
                nmgr.nmgr.registerNode(n, key);
                return;
            }
        }

        Property prop = (propMap == null) ? null : (Property) propMap.get(p2);

        if (prop != null) {
            if ((prop.getType() == IProperty.NODE) &&
                    n.getHandle().equals(prop.getNodeHandle())) {
                // nothing to do, just clean up locks and return
                if (state == CLEAN) {
                    clearWriteLock();
                }

                if (n.state == CLEAN) {
                    n.clearWriteLock();
                }

                return;
            }
        } else {
            prop = new Property(propname, this);
        }

        prop.setNodeValue(n);

        if ((rel == null) || (rel.reftype == Relation.REFERENCE) || rel.virtual ||
                (rel.otherType == null) || !rel.otherType.isRelational()) {
            // the node must be stored as explicit property
            if (propMap == null) {
                propMap = new Hashtable();
            }

            propMap.put(p2, prop);

            if (state == CLEAN) {
                markAs(MODIFIED);
            }
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
            // String nID = n.getID();
            // DbMapping dbm = n.getDbMapping ();
            Transactor tx = (Transactor) Thread.currentThread();

            // tx.visitCleanNode (new DbKey (dbm, nID), n);
            // UPDATE: using n.getKey() instead of manually constructing key. HW 2002/09/13
            tx.visitCleanNode(n.getKey(), n);

            // if the field is not the primary key of the property, also register it
            if ((rel != null) && (rel.accessName != null) && (state != TRANSIENT)) {
                Key secKey = new SyntheticKey(getKey(), propname);

                nmgr.evictKey(secKey);
                tx.visitCleanNode(secKey, n);
            }
        }

        lastmodified = System.currentTimeMillis();

        if (n.state == DELETED) {
            n.markAs(MODIFIED);
        }
    }

    /**
     * Remove a property. Note that this works only for explicitly set properties, not for those
     * specified via property relation.
     */
    public void unset(String propname) {
        if (propMap == null) {
            return;
        }

        try {
            // if node is relational, leave a null property so that it is 
            // updated in the DB. Otherwise, remove the property.
            Property p;
            boolean relational = (dbmap != null) && dbmap.isRelational();

            if (relational) {
                p = (Property) propMap.get(propname.toLowerCase());
            } else {
                p = (Property) propMap.remove(propname.toLowerCase());
            }

            if (p != null) {
                checkWriteLock();

                if (p.getType() == Property.NODE) {
                    p.unregisterNode();
                }

                if (relational) {
                    p.setStringValue(null);
                }

                // Server.throwNodeEvent (new NodeEvent (this, NodeEvent.PROPERTIES_CHANGED));
                lastmodified = System.currentTimeMillis();

                if (state == CLEAN) {
                    markAs(MODIFIED);
                }
            } else if (dbmap != null) {
                // check if this is a complex constraint and we have to
                // unset constraints.
                Relation rel = dbmap.getExactPropertyRelation(propname);

                if (rel != null && (rel.isComplexReference() || rel.countConstraints() > 1)) {
                    p = getProperty(propname);
                    System.err.println ("NEED TO UNSET: "+p.getNodeValue());
                }
            }
        } catch (Exception ignore) {
        }
    }

    /**
     *
     *
     * @return ...
     */
    public long lastModified() {
        return lastmodified;
    }

    /**
     *
     *
     * @return ...
     */
    public long created() {
        return created;
    }

    /**
     *
     *
     * @return ...
     */
    public String toString() {
        return "HopObject " + name;
    }

    /**
     * Tell whether this node is stored inside a relational db. This doesn't mean
     * it actually is stored in a relational db, just that it would be, if the node was
     * persistent
     */
    public boolean isRelational() {
        return (dbmap != null) && dbmap.isRelational();
    }

    /**
     * Recursively turn node status from TRANSIENT to NEW so that the Transactor will
     * know it has to insert this node.
     */
    protected void makePersistentCapable() {
        if (state == TRANSIENT) {
            state = NEW;

            // generate a real, persistent ID for this object
            id = nmgr.generateID(dbmap);
            getHandle().becomePersistent();

            Transactor current = (Transactor) Thread.currentThread();

            current.visitNode(this);
            current.visitCleanNode(this);

            for (Enumeration e = getSubnodes(); e.hasMoreElements();) {
                Node n = (Node) e.nextElement();

                if (n.state == TRANSIENT) {
                    n.makePersistentCapable();
                }
            }

            for (Enumeration e = properties(); e.hasMoreElements();) {
                IProperty next = get((String) e.nextElement());

                if ((next != null) && (next.getType() == IProperty.NODE)) {
                    Node n = (Node) next.getNodeValue();

                    if ((n != null) && (n.state == TRANSIENT)) {
                        n.makePersistentCapable();
                    }
                }
            }
        }
    }

    /**
     * Get the cache node for this node. This can be
     * used to store transient cache data per node from Javascript.
     */
    public synchronized INode getCacheNode() {
        if (cacheNode == null) {
            cacheNode = new TransientNode();
        }

        return cacheNode;
    }

    /**
     * Reset the cache node for this node.
     */
    public synchronized void clearCacheNode() {
        cacheNode = null;
    }

    /**
     * This method walks down node path to the first non-virtual node and return it.
     *  limit max depth to 3, since there shouldn't be more then 2 layers of virtual nodes.
     */
    public Node getNonVirtualParent() {
        Node node = this;

        for (int i = 0; i < 5; i++) {
            if (node == null) {
                break;
            }

            if (node.getState() != Node.VIRTUAL) {
                return node;
            }

            node = (Node) node.getParent();
        }

        return null;
    }

    /**
     *  Instances of this class may be used to mark an entry in the object cache as null.
     *  This method tells the caller whether this is the case.
     */
    public boolean isNullNode() {
        return nmgr == null;
    }

    /**
     * We overwrite hashCode to make it dependant from the prototype. That way, when the prototype
     * changes, the node will automatically get a new ESNode wrapper, since they're cached in a hashtable.
     * You gotta love these hash code tricks ;-)
     */
    public int hashCode() {
        if (prototype == null) {
            return super.hashCode();
        } else {
            return super.hashCode() + prototype.hashCode();
        }
    }

    /**
     *
     */
    public void dump() {
        System.err.println("subnodes: " + subnodes);
        System.err.println("properties: " + propMap);
        System.err.println("links: " + links);
    }
}
