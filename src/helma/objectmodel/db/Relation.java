// Relation.java
// Copyright (c) Hannes Wallnöfer 1997-2000

package helma.objectmodel.db;

import helma.objectmodel.*;
import helma.framework.core.Application;
import java.util.Properties;
import java.util.Vector;
import java.sql.SQLException;

/**
 * This describes how a property of a persistent Object is stored in a
 *  relational database table. This can be either a scalar property (string, date, number etc.)
 *  or a reference to one or more other objects.
 */
public final class Relation {

    // these constants define different type of property-to-db-mappings

    // there is an error in the description of this relation
    public final static int INVALID = -1;
     // a mapping of a non-object, scalar type
    public final static int PRIMITIVE = 0;
     // a 1-to-1 relation, i.e. a field in the table is a foreign key to another object
    public final static int REFERENCE = 1;
    // a 1-to-many relation, a field in another table points to objects of this type
    public final static int COLLECTION = 2;
    // direct mapping is a very powerful feature: objects of some types can be directly accessed
    // by one of their properties/db fields.
    // public final static int DIRECT = 3;

    // the DbMapping of the type we come from
    DbMapping ownType;
    // the DbMapping of the prototype we link to, unless this is a "primitive" (non-object) relation
    DbMapping otherType;

    // the column type, as defined in java.sql.Types
    int columnType;

    //  if this relation defines a virtual node, we need to provide a DbMapping for these virtual nodes
    DbMapping virtualMapping;

    String propName;
    String columnName;

    int reftype;

    Constraint[] constraints;

    boolean virtual;
    boolean readonly;
    boolean aggressiveLoading;
    boolean aggressiveCaching;
    boolean subnodesAreProperties;
    boolean isPrivate;

    String accessor; // db column used to access objects through this relation
    String order;
    String groupbyOrder;
    String groupby;
    String prototype;
    String groupbyPrototype;
    String filter;
    int maxSize = 0;


    /**
     * This constructor makes a copy of an existing relation. Not all fields are copied, just those
     * which are needed in groupby- and virtual nodes defined by this relation.
     */
    public Relation (Relation rel) {
	this.ownType = rel.ownType;
	this.otherType = rel.otherType;
	this.propName = rel.propName;
	this.columnName = rel.columnName;
	this.reftype = rel.reftype;
	this.constraints = rel.constraints;
	this.accessor = rel.accessor;
	this.maxSize = rel.maxSize;
	this.subnodesAreProperties = rel.subnodesAreProperties;
    }

    /**
     * Reads a relation entry from a line in a properties file.
     */
    public Relation (String desc, String propName, DbMapping ownType, Properties props) {
	this.ownType = ownType;
	this.propName = propName;
	otherType = null;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////
    // parse methods for new file format
    ////////////////////////////////////////////////////////////////////////////////////////////

    public void update (String desc, Properties props) {
	Application app = ownType.getApplication ();
	if (desc == null || "".equals (desc.trim ())) {
	    if (propName != null) {
	        reftype = PRIMITIVE;
	        columnName = propName;
	    } else {
	        reftype = INVALID;
	        columnName = propName;
	    }
	} else {
	    desc = desc.trim ();
	    int open = desc.indexOf ("(");
	    int close = desc.indexOf (")");
	    if (open > -1 && close > open) {
	        String ref = desc.substring (0, open).trim ();
	        String proto = desc.substring (open+1, close).trim ();
	        if ("collection".equalsIgnoreCase (ref)) {
	            virtual = !"_children".equalsIgnoreCase (propName);
	            reftype = COLLECTION;
	        } else if ("mountpoint".equalsIgnoreCase (ref)) {
	            virtual = true;
	            reftype = COLLECTION;
	            prototype = proto;
	        } else if ("object".equalsIgnoreCase (ref)) {
	            virtual = false;
	            reftype = REFERENCE;
	        } else {
	            throw new RuntimeException ("Invalid property Mapping: "+desc);
	        }
	        otherType = app.getDbMapping (proto);
	        if (otherType == null)
	            throw new RuntimeException ("DbMapping for "+proto+" not found from "+ownType.typename);
	    } else {
	        virtual = false;
	        columnName = desc;
	        reftype = PRIMITIVE;
	    }
	}
	String rdonly = props.getProperty (propName+".readonly");
	if (rdonly != null && "true".equalsIgnoreCase (rdonly)) {
	    readonly = true;
	} else {
	    readonly = false;
	}
	isPrivate = "true".equalsIgnoreCase (props.getProperty (propName+".private"));

	// the following options only apply to object and collection relations
	if (reftype != PRIMITIVE && reftype != INVALID) {

	    Vector newConstraints = new Vector ();
	    parseOptions (newConstraints, props);

	    constraints = new Constraint[newConstraints.size()];
	    newConstraints.copyInto (constraints);
	    // if DbMapping for virtual nodes has already been created,
	    // update its subnode relation.
	    // FIXME: needs to be synchronized?
	     if (virtualMapping != null) {
	        virtualMapping.lastTypeChange = ownType.lastTypeChange;
	        virtualMapping.subRelation = getVirtualSubnodeRelation ();
	        virtualMapping.propRelation = getVirtualPropertyRelation ();
	    }
	}
    }


    protected void parseOptions (Vector cnst, Properties props) {
	String loading = props.getProperty (propName+".loadmode");
	aggressiveLoading = loading != null && "aggressive".equalsIgnoreCase (loading.trim());
	String caching = props.getProperty (propName+".cachemode");
	aggressiveCaching = caching != null && "aggressive".equalsIgnoreCase (caching.trim());
	// get order property
	order = props.getProperty (propName+".order");
	if (order != null && order.trim().length() == 0)
	    order = null;
	// get additional filter property
	filter = props.getProperty (propName+".filter");
	if (filter != null && filter.trim().length() == 0)
	    filter = null;
	// get max size of collection
	String max = props.getProperty (propName+".maxSize");
	if (max != null) try {
	    maxSize = Integer.parseInt (max);
	} catch (NumberFormatException nfe) {
	    maxSize = 0;
	} else {
	    maxSize = 0;
	}
	// get group by property
	groupby = props.getProperty (propName+".group");
	if (groupby != null && groupby.trim().length() == 0)
	    groupby = null;
	if (groupby != null) {
	    groupbyOrder = props.getProperty (propName+".group.order");
	    if (groupbyOrder != null && groupbyOrder.trim().length() == 0)
	        groupbyOrder = null;
	    groupbyPrototype = props.getProperty (propName+".group.prototype");
	    if (groupbyPrototype != null && groupbyPrototype.trim().length() == 0)
	        groupbyPrototype = null;
	    // aggressive loading and caching is not supported for groupby-nodes
	    aggressiveLoading = aggressiveCaching = false;
	}
	// check if subnode condition should be applied for property relations
	accessor = props.getProperty (propName+".accessname");
	if (accessor != null)
	    subnodesAreProperties = true;
        // parse contstraints
	String local = props.getProperty (propName+".local");
	String foreign = props.getProperty (propName+".foreign");
	if (local != null && foreign != null) {
	    cnst.addElement (new Constraint (local, otherType.getTableName (), foreign, false));
	    columnName = local;
	}
    }


    ///////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Does this relation describe a virtual (collection) node?
     */
    public boolean isVirtual () {
	return virtual;
    }

    /**
     * Tell if this relation represents a primitive (scalar) value mapping.
     */
    public boolean isPrimitive () {
	return reftype == PRIMITIVE;
    }

    /**
     *  Returns true if this Relation describes an object reference property
     */
    public boolean isReference () {
	return reftype == REFERENCE;
    }

    /**
     *  Returns true if this Relation describes a collection object property
     */
    public boolean isCollection () {
	return reftype == COLLECTION;
    }

    /**
     *  Tell wether the property described by this relation is to be handled as private, i.e.
     *  a change on it should not result in any changed object/collection relations.
     */
    public boolean isPrivate () {
	return isPrivate;
    }

    /**
     *  Returns true if the object represented by this Relation has to be
     *  created dynamically by the Helma objectmodel runtime as a virtual
     *  node. Virtual nodes are objects which are only generated on demand
     *  and never stored to a persistent storage.
     */
    public boolean createPropertyOnDemand () {
	return virtual || accessor != null || groupby != null;
    }

    /**
     *  Returns true if the object represented by this Relation has to be
     *  persisted in the internal db in order to be functional. This is true if
     *  the subnodes contained in this collection are stored in the embedded
     *  database. In this case, the collection itself must also be an ordinary
     *  object stored in the db, since a virtual collection would lose its
     *  its content after restarts.
     */
    public boolean needsPersistence () {
	if (!virtual)
	    return false;
	if (prototype == null)
	    return !otherType.isRelational ();
	DbMapping sub = otherType.getSubnodeMapping ();
	return sub != null && !sub.isRelational ();
    }

    /**
     * Return the prototype to be used for object reached by this relation
     */
    public String getPrototype () {
	return prototype;
    }

    /**
     * Return the name of the local property this relation is defined for
     */
    public String getPropName () {
	return propName;
    }

    public void setColumnType (int ct) {
	columnType = ct;
    }

    public int getColumnType () {
	return columnType;
    }


    /**
     * Add a constraint to the current list of constraints
     */
    protected void addConstraint (Constraint c) {
 	if (constraints == null) {
 	    constraints = new Constraint[1];
 	    constraints[0] = c;
 	} else {
 	    Constraint[] nc = new Constraint[constraints.length+1];
 	    System.arraycopy (constraints, 0, nc, 0, constraints.length);
 	    nc[nc.length-1] = c;
 	    constraints = nc;
 	}
    }


    public boolean usesPrimaryKey () {
	if (otherType != null) {
	    if (reftype == REFERENCE)
	        return constraints.length == 1 && constraints[0].foreignKeyIsPrimary ();
	    if (reftype == COLLECTION)
	        return accessor == null || accessor.equalsIgnoreCase (otherType.getIDField ());
	}
	return false;
    }

    public String getAccessor () {
	return accessor;
    }

    public Relation getSubnodeRelation () {
	// return subnoderelation;
	return null;
    }


    /**
     * Return the local field name for updates.
     */
    public String getDbField () {
	return columnName;
    }


    public DbMapping getVirtualMapping () {
	if (!virtual)
	    return null;
	if (virtualMapping == null) {
	    virtualMapping = new DbMapping (ownType.app);
	    virtualMapping.subRelation = getVirtualSubnodeRelation ();
	    virtualMapping.propRelation = getVirtualPropertyRelation ();
	}
	return virtualMapping;
    }


    /**
     * Return a Relation that defines the subnodes of a virtual node.
     */
    Relation getVirtualSubnodeRelation () {
	if (!virtual)
	    throw new RuntimeException ("getVirtualSubnodeRelation called on non-virtual relation");
	Relation vr = new Relation (this);
	vr.groupby = groupby;
	vr.groupbyOrder = groupbyOrder;
	vr.groupbyPrototype = groupbyPrototype;
	vr.order = order;
	vr.filter = filter;
	vr.maxSize = maxSize;
	vr.constraints = constraints;
	vr.aggressiveLoading = aggressiveLoading;
	vr.aggressiveCaching = aggressiveCaching;
	return vr;
    }

    /**
     * Return a Relation that defines the properties of a virtual node.
     */
    Relation getVirtualPropertyRelation () {
	if (!virtual)
	    throw new RuntimeException ("getVirtualPropertyRelation called on non-virtual relation");
	Relation vr = new Relation (this);
	vr.groupby = groupby;
	vr.groupbyOrder = groupbyOrder;
	vr.groupbyPrototype = groupbyPrototype;
	vr.order = order;
	vr.filter = filter;
	vr.maxSize = maxSize;
	vr.constraints = constraints;
	return vr;
    }

    /**
     * Return a Relation that defines the subnodes of a group-by node.
     */
    Relation getGroupbySubnodeRelation () {
	if (groupby == null)
	    throw new RuntimeException ("getGroupbySubnodeRelation called on non-group-by relation");
	Relation vr = new Relation (this);
	vr.order = order;
	vr.prototype = groupbyPrototype;
	vr.filter = filter;
	vr.constraints = constraints;
	vr.addConstraint (new Constraint (null, null, groupby, true));
	vr.aggressiveLoading = aggressiveLoading;
	vr.aggressiveCaching = aggressiveCaching;
	return vr;
    }

    /**
     * Return a Relation that defines the properties of a group-by node.
     */
    Relation getGroupbyPropertyRelation () {
	if (groupby == null)
	    throw new RuntimeException ("getGroupbyPropertyRelation called on non-group-by relation");
	Relation vr = new Relation (this);
	vr.order = order;
	vr.prototype = groupbyPrototype;
	vr.filter = filter;
	vr.constraints = constraints;
	vr.addConstraint (new Constraint (null, null, groupby, true));
	return vr;
    }


    /**
     *  Build the second half of an SQL select statement according to this relation
     *  and a local object.
     */
    public String buildQuery (INode home, INode nonvirtual, String kstr, String pre, boolean useOrder) throws SQLException {
	StringBuffer q = new StringBuffer ();
	String prefix = pre;
	if (kstr != null) {
	    q.append (prefix);
	    String accessColumn = accessor == null ? otherType.getIDField () : accessor;
	    q.append (accessColumn);
	    q.append (" = ");
	    // check if column is string type and value needs to be quoted
	    if (otherType.needsQuotes (accessColumn)) {
	        q.append ("'");
	        q.append (escape (kstr));
	        q.append ("'");
	    } else
	        q.append (escape (kstr));
	    prefix = " AND ";
	}
	for (int i=0; i<constraints.length; i++) {
	    q.append (prefix);
	    constraints[i].addToQuery (q, home, nonvirtual);
	    prefix = " AND ";
	}

	if (filter != null) {
	    q.append (prefix);
	    q.append (filter);
	}
	if (groupby != null) {
	    q.append (" GROUP BY "+groupby);
	    if (useOrder && groupbyOrder != null)
	        q.append (" ORDER BY "+groupbyOrder);
	} else if (useOrder && order != null)
	    q.append (" ORDER BY "+order);
	return q.toString ();
    }

    public String renderConstraints (INode home, INode nonvirtual) throws SQLException {
	StringBuffer q = new StringBuffer ();
	String suffix = " AND ";
	for (int i=0; i<constraints.length; i++) {
	    constraints[i].addToQuery (q, home, nonvirtual);
	    q.append (suffix);
	}
	if (filter != null) {
	    q.append (filter);
	    q.append (suffix);
	}
	return q.toString ();
    }


    /**
     * Get the order section to use for this relation
     */
    public String getOrder () {
	if (groupby != null)
	    return groupbyOrder;
	else
	    return order;
    }

    /**
     *  Tell wether the property described by this relation is to be handled
     *  as readonly/write protected.
     */
    public boolean isReadonly () {
	return readonly;
    }


    /**
     * Check if the child node fullfills the constraints defined by this relation.
     */
    public boolean checkConstraints (Node parent, Node child) {
	// problem: if a filter property is defined for this relation,
	// i.e. a piece of static SQL-where clause, we'd have to evaluate it
	// in order to check the constraints. Because of this, if a filter
	// is defined, we return false as soon as the modified-time is greater
	// than the create-time of the child, i.e. if the child node has been
	// modified since it was first fetched from the db.
	if (filter != null && child.lastModified() > child.created())
	    return false;
	for (int i=0; i<constraints.length; i++) {
	    String propname = constraints[i].foreignProperty ();
	    if (propname != null) {
	        INode home = constraints[i].isGroupby ? parent : parent.getNonVirtualParent ();
	        String localName = constraints[i].localName;
	        String value = null;
	        if (localName == null  || localName.equalsIgnoreCase (ownType.getIDField ()))
	            value = home.getID ();
	        else if (ownType.isRelational ())
	            value = home.getString (ownType.columnNameToProperty (localName));
	        else
	            value = home.getString (localName);
	        if (value != null && !value.equals (child.getString (propname))) {
	            return false;
	        }
	    }
	}
	return true;
    }


    /**
     * Make sure that the child node fullfills the constraints defined by this relation by setting the
     * appropriate properties
     */
    public void setConstraints (Node parent, Node child) {
	INode home = parent.getNonVirtualParent ();
	for (int i=0; i<constraints.length; i++) {
	    // don't set groupby constraints since we don't know if the
	    // parent node is the base node or a group node
	    if (constraints[i].isGroupby)
	        continue;
	    Relation crel = otherType.columnNameToRelation (constraints[i].foreignName);
	    if (crel != null) {
	        // INode home = constraints[i].isGroupby ? parent : nonVirtual;
	        String localName = constraints[i].localName;
	        if (localName == null  || localName.equalsIgnoreCase (ownType.getIDField ())) {
	            // only set node if property in child object is defined as reference.
	            if (crel.reftype == REFERENCE) {
	                INode currentValue = child.getNode (crel.propName);
	                // we set the backwards reference iff the reference is currently unset, if
	                // is set to a transient object, or if the new target is not transient. This
	                // prevents us from overwriting a persistent refererence with a transient one,
	                // which would most probably not be what we want.
	                if (currentValue == null ||
				(currentValue != home &&
				(currentValue.getState() == Node.TRANSIENT ||
				home.getState() != Node.TRANSIENT)))
	                    child.setNode (crel.propName, home);
	            } else if (crel.reftype == PRIMITIVE) {
	                child.setString (crel.propName, home.getID ());
	            }
	        } else if (crel.reftype == PRIMITIVE) {
	            String value = null;
	            if (ownType.isRelational ())
	                value = home.getString (ownType.columnNameToProperty (localName));
	            else
	                value = home.getString (localName);
	            if (value != null) {
	                child.setString (crel.propName, value);
	            }
	        }
	    }
	}
    }


    // a utility method to escape single quotes
    String escape (String str) {
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

    public String toString () {
	String c = "";
	if (constraints != null) {
	    for (int i=0; i<constraints.length; i++)
	        c += constraints[i].toString ();
	}
	return "Relation["+ownType+"."+propName+">"+otherType+"]" + c;
    }

    /**
     * The Constraint class represents a part of the where clause in the query used to
     * establish a relation between database mapped objects.
     */
    class Constraint {

    	String localName;
    	String tableName;
    	String foreignName;
    	boolean isGroupby;

    	Constraint (String local, String table, String foreign, boolean groupby) {
    	    localName = local;
    	    tableName = table;
    	    foreignName = foreign;
    	    isGroupby = groupby;
    	}

    	public void addToQuery (StringBuffer q, INode home, INode nonvirtual) throws SQLException {
    	    String local = null;
    	    INode ref = isGroupby ? home : nonvirtual;
    	    if (localName == null || localName.equalsIgnoreCase (ref.getDbMapping ().getIDField ()))
    	        local = ref.getID ();
    	    else {
    	        String homeprop = ownType.columnNameToProperty (localName);
    	        local = ref.getString (homeprop);
    	    }
    	    q.append (foreignName);
    	    q.append (" = ");
    	    if (otherType.needsQuotes (foreignName)) {
    	        q.append ("'");
    	        q.append (escape (local));
    	        q.append ("'");
    	    } else
    	        q.append (escape (local));
    	}

    	public boolean foreignKeyIsPrimary () {
    	    return foreignName == null || foreignName.equalsIgnoreCase (otherType.getIDField ());
    	}

    	public String foreignProperty () {
    	    return  otherType.columnNameToProperty (foreignName);
    	}

    	public String localProperty () {
    	    return  ownType.columnNameToProperty (localName);
    	}

    	public String toString () {
    	    return ownType+"."+localName+"="+tableName+"."+foreignName;
    	}
    }

}


