// Relation.java
// Copyright (c) Hannes Wallnöfer 1997-2000
 
package helma.objectmodel.db;

import helma.objectmodel.*;
import helma.framework.core.Application;
import java.util.Properties;
import java.util.Vector;

/**
 * This describes how a property of a persistent Object is stored in a
 *  relational database table. This can be either a scalar property (string, date, number etc.)
 *  or a reference to one or more other objects.
 */
public class Relation {

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

    //  if this relation defines a virtual node, we need to provide a DbMapping for these virtual nodes
    DbMapping virtualMapping;

    Relation virtualRelation;
    Relation groupRelation;

    String propName;
    String columnName;

    int reftype;

    Constraint[] constraints;

    boolean virtual;
    boolean readonly;
    boolean aggressiveLoading;
    boolean aggressiveCaching;
    boolean subnodesAreProperties;
    String accessor; // db column used to access objects through this relation
    String order;
    String groupbyorder;
    String groupby;
    String prototype;
    String groupbyprototype;
    String filter;
    int maxSize = 0;

    // Relation subnoderelation = null; // additional relation used to filter subnodes for virtual nodes

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
	
	update (desc, props);
    }

    public void update (String desc, Properties props) {
	
	boolean mountpoint = false;
	Vector cnst = null;

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
	    String descLower = desc.toLowerCase ();
	    if (descLower.startsWith ("[virtual]")) {
	        desc = desc.substring (9).trim ();
	        virtual = true;
	    } else if (descLower.startsWith ("[collection]")) {
	        desc = desc.substring (12).trim ();
	        virtual = true;
	    } else if (descLower.startsWith ("[mountpoint]")) {
	        desc = desc.substring (12).trim ();
	        virtual = true;
	        mountpoint = true;
	    } else {
	        virtual = false;
	    }
	    if (descLower.startsWith ("[readonly]")) {
	        desc = desc.substring (10).trim ();
	        readonly = true;
	    } else {
	        readonly = false;
	    }
	}
	
	// parse the basic properties of this mapping
	parseMapping (desc, mountpoint);
	
	// the following options only apply to object relations
	if (reftype != PRIMITIVE && reftype != INVALID) {
	
	    cnst = new Vector ();
	
	    Constraint c = parseConstraint (desc);
	
	    if (c != null)
	        cnst.add (c);
	
	    parseOptions (cnst, props);
	
	    constraints = new Constraint[cnst.size()];
	    cnst.copyInto (constraints);

	    // System.err.println ("PARSED RELATION "+this);
	    // if (accessor != null)
	    //     System.err.println ("SET ACCESSOR: "+accessor);
	}
    }

    /**
     * Parse a line describing a mapping of a property field. If the mapping is a
     * object reference of a collection of objects, put any constraints in the Vector.
     */
    protected void parseMapping (String desc, boolean mountpoint) {
	
	Application app = ownType.getApplication ();
	
	if (desc.indexOf ("<") > -1) {
	    reftype = COLLECTION;
	    int lt = desc.indexOf ("<");
	    int dot = desc.indexOf (".");
	    String other = dot < 0 ? desc.substring (lt+1).trim () : desc.substring (lt+1, dot).trim ();
	    otherType = app.getDbMapping (other);
	    if (otherType == null)
	        throw new RuntimeException ("DbMapping for "+other+" not found from "+ownType.typename);
	    columnName = null;
	    if (mountpoint)
	        prototype = other;
	} else if (desc.indexOf (">") > -1) {
	    reftype = REFERENCE;
	    int bt = desc.indexOf (">");
	    int dot = desc.indexOf (".");
	    String other = dot > -1 ? desc.substring (bt+1, dot).trim () : desc.substring (bt+1).trim ();
	    otherType = app.getDbMapping (other);
	    if (otherType == null)
	        throw new RuntimeException ("DbMapping for "+other+" not found from "+ownType.typename);
	    columnName = desc.substring (0, bt).trim ();
	    if (mountpoint)
	        prototype = other;
	} else if (desc.indexOf (".") > -1) {
	    reftype = COLLECTION;
	    int dot = desc.indexOf (".");
	    String other = desc.substring (0, dot).trim ();
	    otherType = app.getDbMapping (other);
	    if (otherType == null)
	        throw new RuntimeException ("DbMapping for "+other+" not found from "+ownType.typename);
	    columnName = null;
	    // set accessor
	    accessor = desc.substring (dot+1).trim ();
	    if (mountpoint)
	        prototype = other;
	} else {
	    if (virtual) {
	        reftype = COLLECTION;
	        otherType = app.getDbMapping (desc);
	        if (otherType == null)
	            throw new RuntimeException ("DbMapping for "+desc+" not found from "+ownType.typename);
	        if (mountpoint)
	            prototype = desc;
	    } else {
	        reftype = PRIMITIVE;
	        columnName = desc.trim ();
	    }
	}
    }


    /**
     * Parse a line describing a mapping of a property field. If the mapping is a
     * object reference of a collection of objects, put any constraints in the Vector.
     */
    protected Constraint parseConstraint (String desc) {
	if (desc.indexOf ("<") > -1) {
	    int lt = desc.indexOf ("<");
	    int dot = desc.indexOf (".");
	    String remoteField = dot < 0 ? null : desc.substring (dot+1).trim ();
	    String localField = lt <= 0 ? null : desc.substring (0, lt).trim ();
	    return new Constraint (localField, otherType.getTableName (), remoteField, false);
	} else if (desc.indexOf (">") > -1) {
	    int bt = desc.indexOf (">");
	    int dot = desc.indexOf (".");
	    String localField = desc.substring (0, bt).trim ();
	    String remoteField = dot < 0 ? null : desc.substring (dot+1).trim ();
	    return new Constraint (localField, otherType.getTableName (), remoteField, false);
	}
	return null;
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
	}
	// get group by property
	groupby = props.getProperty (propName+".groupby");
	if (groupby != null && groupby.trim().length() == 0)
	    groupby = null;
	if (groupby != null) {
	    groupbyorder = props.getProperty (propName+".groupby.order");
	    if (groupbyorder != null && groupbyorder.trim().length() == 0)
	        groupbyorder = null;
	    groupbyprototype = props.getProperty (propName+".groupby.prototype");
	    if (groupbyprototype != null && groupbyprototype.trim().length() == 0)
	        groupbyprototype = null;
	    // aggressive loading and caching is not supported for groupby-nodes
	    aggressiveLoading = aggressiveCaching = false;
	}
	// check if subnode condition should be applied for property relations
	if ("_properties".equalsIgnoreCase (propName) || virtual) {
	    String subnodes2props = props.getProperty (propName+".aresubnodes");
	    subnodesAreProperties = "true".equalsIgnoreCase (subnodes2props);
	    if (virtual) {
	        String subnodefilter = props.getProperty (propName+".subnoderelation");
	        if (subnodefilter != null) {
	            Constraint c = parseConstraint (subnodefilter);
	            if (c != null) {
	                cnst.add (c);
	            }
	        }
	    }
	    // update virtual mapping, if it already exists
	    if (virtualMapping != null) {
	        virtualMapping.subnodesRel = getVirtualSubnodeRelation ();
	        virtualMapping.propertiesRel = getVirtualPropertyRelation ();
	        virtualMapping.lastTypeChange = ownType.lastTypeChange;
	    }
	}
    }

    /**
     * Does this relation describe a virtual (collection) node?
     */
    public boolean isVirtual () {
	return virtual;
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
	        return accessor == null || accessor.equals (otherType.getIDField ());
	}
	return false;
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
	    virtualMapping = new DbMapping ();
	    virtualMapping.subnodesRel = getVirtualSubnodeRelation ();
	    virtualMapping.propertiesRel = getVirtualPropertyRelation ();
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
	vr.groupbyorder = groupbyorder;
	vr.groupbyprototype = groupbyprototype;
	vr.order = order;
	vr.filter = filter;
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
	vr.groupbyorder = groupbyorder;
	vr.groupbyprototype = groupbyprototype;
	vr.order = order;
	vr.filter = filter;
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
	vr.prototype = groupbyprototype;
	vr.filter = filter;
	vr.constraints = constraints;
	vr.addConstraint (new Constraint (null, null, groupby, true));
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
	vr.prototype = groupbyprototype;
	vr.filter = filter;
	vr.constraints = constraints;
	vr.addConstraint (new Constraint (null, null, groupby, true));
	return vr;
    }


    /**
     *  Build the second half of an SQL select statement according to this relation
     *  and a local object.
     */
    public String buildQuery (INode home, INode nonvirtual, String kstr, String pre, boolean useOrder) {
	StringBuffer q = new StringBuffer ();
	String prefix = pre;
	if (kstr != null) {
	    q.append (prefix);
	    String accessColumn = accessor == null ? otherType.getIDField () : accessor;
	    q.append (accessColumn);
	    q.append (" = '");
	    q.append (escape (kstr));
	    q.append ("'");
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
	    if (useOrder && groupbyorder != null)
	        q.append (" ORDER BY "+groupbyorder);
	} else if (useOrder && order != null)
	    q.append (" ORDER BY "+order);
	return q.toString ();
    }

    /**
     * Get the order section to use for this relation
     */
    public String getOrder () {
	if (groupby != null)
	    return groupbyorder;
	else
	    return order;
    }


    /**
     * Check if the child node fullfills the constraints defined by this relation.
     */
    public boolean checkConstraints (Node parent, Node child) {
	for (int i=0; i<constraints.length; i++) {
	    String propname = constraints[i].foreignProperty ();
	    if (propname != null) {
	        INode home = constraints[i].isGroupby ? parent : parent.getNonVirtualParent ();
	        String localName = constraints[i].localName;
	        String value = null;
	        if (localName == null  || localName.equals (ownType.getIDField ()))
	            value = home.getID ();
	        else if (ownType.isRelational ())
	            value = home.getString (ownType.columnNameToProperty (localName), false);
	        else
	            value = home.getString (localName, false);
	        if (value != null && !value.equals (child.getString (propname, false))) {
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
	        if (localName == null  || localName.equals (ownType.getIDField ())) {
	            INode currentValue = child.getNode (crel.propName, false);
	            // we set the backwards reference iff the reference is currently unset, if
	            // is set to a transient object, or if the new target is not transient. This
	            // prevents us from overwriting a persistent refererence with a transient one,
	            // which would most probably not be what we want.
	            if (currentValue == null ||
				(currentValue != home &&
				(currentValue.getState() == Node.TRANSIENT ||
				home.getState() != Node.TRANSIENT)))
	                child.setNode (crel.propName, home);
	        } else {
	            String value = null;
	            if (ownType.isRelational ())
	                value = home.getString (ownType.columnNameToProperty (localName), false);
	            else
	                value = home.getString (localName, false);
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
	        sbuf.append ("\\");
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
    	
    	public void addToQuery (StringBuffer q, INode home, INode nonvirtual) {
    	    String local = null;
    	    INode ref = isGroupby ? home : nonvirtual;
    	    if (localName == null)
    	        local = ref.getID ();
    	    else {
    	        String homeprop = ownType.columnNameToProperty (localName);
    	        local = ref.getString (homeprop, false);
    	    }
    	    q.append (foreignName);
    	    q.append (" = '");
    	    q.append (escape (local));
    	    q.append ("'");
    	}
    	
    	public boolean foreignKeyIsPrimary () {
    	    return foreignName == null || foreignName.equals (otherType.getIDField ());
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




















































