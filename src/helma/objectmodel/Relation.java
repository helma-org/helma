// Relation.java
// Copyright (c) Hannes Wallnöfer 1997-2000
 
package helma.objectmodel;

import helma.framework.core.Application;
import java.util.Properties;

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
    public final static int FORWARD = 1;
    // a 1-to-many relation, a field in another table points to objects of this type
    public final static int BACKWARD = 2;
    // direct mapping is a very powerful feature: objects of some types can be directly accessed
    // by one of their properties/db fields.
    public final static int DIRECT = 3;

    // the DbMapping of the type we come from
    public DbMapping home;
    // the DbMapping of the prototype we link to, unless this is a "primitive" (non-object) relation
    public DbMapping other;
    //  if this relation defines a virtual node, we need a DbMapping for these virtual nodes
    DbMapping virtualMapping;

    public String propname;
    protected String localField, remoteField;
    public int direction;

    public boolean virtual;
    public boolean readonly;
    public boolean aggressiveLoading;
    public boolean aggressiveCaching;
    public boolean subnodesAreProperties;
    public String order;
    public String groupbyorder;
    public String groupby;
    public String dogroupby;
    public String prototype;
    public String groupbyprototype;
    public String filter;

    Relation subnoderelation = null; // additional relation used to filter subnodes for virtual nodes

    /**
     * This constructor makes a copy of an existing relation. Not all fields are copied, just those
     * which are needed in groupby- and virtual nodes defined by this relation.
     */
    public Relation (Relation rel) {
	this.home = rel.home;
	this.other = rel.other;
	this.localField = rel.localField;
	this.remoteField = rel.remoteField;
	this.direction = rel.direction;
	this.subnodesAreProperties = rel.subnodesAreProperties;
    }

    /**
     * Reads a relation entry from a line in a properties file.
     */
    public Relation (String desc, String propname, DbMapping home, Properties props) {
	this.home = home;
	this.propname = propname;
	other = null;
	
	update (desc, props);
    }

    public void update (String desc, Properties props) {
	
	Application app = home.getApplication ();
	boolean mountpoint = false;

	if (desc == null || "".equals (desc.trim ())) {
	    if (propname != null) {
	        direction = PRIMITIVE;
	        localField = propname;
	    } else {
	        direction = INVALID;
	        localField = propname;
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
	    if (desc.indexOf ("<") > -1) {
	        direction = BACKWARD;
	        int lt = desc.indexOf ("<");
	        int dot = desc.indexOf (".");
	        String otherType = dot < 0 ? desc.substring (lt+1).trim () : desc.substring (lt+1, dot).trim ();
	        other = app.getDbMapping (otherType);
	        if (other == null)
	            throw new RuntimeException ("DbMapping for "+otherType+" not found from "+home.typename);
	        remoteField = dot < 0 ? null : desc.substring (dot+1).trim ();
	        localField = lt < 0 ? null : desc.substring (0, lt).trim ();
	        if (mountpoint) prototype = otherType;
	    } else if (desc.indexOf (">") > -1) {
	        direction = FORWARD;
	        int bt = desc.indexOf (">");
	        int dot = desc.indexOf (".");
	        String otherType = dot > -1 ? desc.substring (bt+1, dot).trim () : desc.substring (bt+1).trim ();
	        other = app.getDbMapping (otherType);
	        if (other == null)
	            throw new RuntimeException ("DbMapping for "+otherType+" not found from "+home.typename);
	        localField = desc.substring (0, bt).trim ();
	        remoteField = dot < 0 ? null : desc.substring (dot+1).trim ();
	        if (mountpoint) prototype = otherType;
	    } else if (desc.indexOf (".") > -1) {
	        direction = DIRECT;
	        int dot = desc.indexOf (".");
	        String otherType = desc.substring (0, dot).trim ();
	        other = app.getDbMapping (otherType);
	        if (other == null)
	            throw new RuntimeException ("DbMapping for "+otherType+" not found from "+home.typename);
	        remoteField = desc.substring (dot+1).trim ();
	        localField = null;
	        if (mountpoint) prototype = otherType;
	    } else {
	        if (virtual) {
	            direction = DIRECT;
	            other = app.getDbMapping (desc);
	            if (other == null)
	                throw new RuntimeException ("DbMapping for "+desc+" not found from "+home.typename);
	            remoteField = localField = null;
	            if (mountpoint) prototype = desc;
	        } else {
	            direction = PRIMITIVE;
	            localField = desc.trim ();
	        }
	    }
	}
	
	// the following options only apply to object relations
	if (direction != PRIMITIVE && direction != INVALID) {
	    String loading = props.getProperty (propname+".loadmode");
	    aggressiveLoading = loading != null && "aggressive".equalsIgnoreCase (loading.trim());
	    String caching = props.getProperty (propname+".cachemode");
	    aggressiveCaching = caching != null && "aggressive".equalsIgnoreCase (caching.trim());
	    // get order property
	    order = props.getProperty (propname+".order");
	    if (order != null && order.trim().length() == 0)
	        order = null;
	    // get additional filter property
	    filter = props.getProperty (propname+".filter");
	    if (filter != null && filter.trim().length() == 0)
	        filter = null;
	    // get group by property
	    groupby = props.getProperty (propname+".groupby");
	    if (groupby != null && groupby.trim().length() == 0)
	        groupby = null;
	    if (groupby != null) {
	        groupbyorder = props.getProperty (propname+".groupby.order");
	        if (groupbyorder != null && groupbyorder.trim().length() == 0)
	            groupbyorder = null;
	        groupbyprototype = props.getProperty (propname+".groupby.prototype");
	        if (groupbyprototype != null && groupbyprototype.trim().length() == 0)
	            groupbyprototype = null;
	        // aggressive loading and caching is not supported for groupby-nodes
	        aggressiveLoading = aggressiveCaching = false;
	    }
	    // check if subnode condition should be applied for property relations
	    if ("_properties".equalsIgnoreCase (propname) || virtual) {
	        String subnodes2props = props.getProperty (propname+".aresubnodes");
	        subnodesAreProperties = "true".equalsIgnoreCase (subnodes2props);
	        if (virtual) {
	            String subnodefilter = props.getProperty (propname+".subnoderelation");
	            if (subnodefilter != null) {
	                subnoderelation = new Relation (subnodefilter, propname+".subnoderelation", home, props);
	                subnoderelation.groupby = groupby;
	                subnoderelation.order = order;
	            }
	        }
	        // update virtual mapping, if it already exists
	        if (virtualMapping != null) {
	            virtualMapping.subnodesRel = getVirtualSubnodeRelation ();
	            virtualMapping.propertiesRel = getVirtualPropertyRelation ();
	            virtualMapping.lastTypeChange = home.lastTypeChange;
	        }
	    }
	}
    }

    public boolean isReference () {
	return direction > PRIMITIVE;
    }

    public boolean usesPrimaryKey () {
	if (other == null)
	    return false;
	if (remoteField == null)
	    // if remote field is null, it is assumed that it points to the primary key
	    return true;
	return remoteField.equalsIgnoreCase (other.getIDField());
    }

    public Relation getSubnodeRelation () {
	return subnoderelation;
    }


    /**
     * Get the local column name for this relation to use in where clauses of select statements.
     * This uses the home node's id as fallback if local field is not specified.
     */
    public String getLocalField () {
	// only assume local field is primary key if other objects "point" to this object
	if (localField == null && direction == BACKWARD)
	    return home.getIDField ();
	return localField;
    }

    /**
     * Return the local field name for updates. This is the same as getLocalField, but does not return the
     * primary key name as a fallback.
     */
    public String getDbField () {
	return localField;
    }

    /**
     * Get the "remote" column name for this relation. Uses the remote node's id as fallback if the remote field is not specified.
     */
    public String getRemoteField () {
	// only assume remote field is primary key if this relation "points" to an object
	if (remoteField == null && direction == FORWARD)
	    return other.getIDField ();
	return remoteField;
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
	Relation vr = null;
	if (subnoderelation != null)
	    vr = new Relation (subnoderelation);
	else
	    vr = new Relation (this);
	vr.groupby = groupby;
	vr.groupbyorder = groupbyorder;
	vr.groupbyprototype = groupbyprototype;
	vr.order = order;
	vr.filter = filter;
	vr.subnoderelation = subnoderelation;
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
	vr.subnoderelation = subnoderelation;
	return vr;
    }

    /**
     * Return a Relation that defines the subnodes of a group-by node.
     */
    Relation getGroupbySubnodeRelation () {
	if (groupby == null)
	    throw new RuntimeException ("getGroupbySubnodeRelation called on non-group-by relation");
	Relation vr = null;
	if (subnoderelation != null)
	    vr =  new Relation (subnoderelation);
	else
	    vr =  new Relation (this);
	vr.order = order;
	vr.prototype = groupbyprototype;
	vr.filter = filter;
	vr.dogroupby = groupby;
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
	vr.dogroupby = groupby;
	return vr;
    }


    public String toString () {
	return "Relation["+home+">"+other+"]";
    }

}




















































