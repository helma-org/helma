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

    // TODO: explain hop mapping types
    public final static int INVALID = -1;
    public final static int PRIMITIVE = 0;
    public final static int FORWARD = 1;
    public final static int BACKWARD = 2;
    public final static int DIRECT = 3;

    public DbMapping home;
    public DbMapping other;
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
    public String prototype;
    public String groupbyprototype;
    public String filter;

    Relation subnoderelation = null; // additional relation used to filter subnodes

    /**
     * This constructor is used to directly construct a Relation, as opposed to reading it from a proerty file
     */
    public Relation (DbMapping other, String localField, String remoteField, int direction, boolean subnodesAreProperties) {
	this.other = other;
	this.localField = localField;
	this.remoteField = remoteField;
	this.direction = direction;
	this.subnodesAreProperties = subnodesAreProperties;
    }

    /**
     * Reads a relation entry from a line in a properties file.
     */
    public Relation (String desc, String propname, DbMapping home, Properties props) {

	this.home = home;
	this.propname = propname;
	other = null;
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
	        String otherType = dot < 0 ? desc.substring (1).trim () : desc.substring (1, dot).trim ();
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
     * Gets a key string to cache a node with a specific value for this relation. If the
     * Relation uses the primary key return just the key value, otherwise include info on the
     * used column or even the base node to avoid collisions.
     */
    public String getKeyID (INode home, String kval) {
	// if the column is not the primary key, we add the column name to the key
	if ((direction == DIRECT || direction == FORWARD) && !usesPrimaryKey ()) {
	    // check if the subnode relation also has to be considered
	    if (subnodesAreProperties)
	        return "["+home.getID()+"]"+remoteField+"="+kval; // HACK
	    else
	        return remoteField+"="+kval;
	} else {
	    return kval;
	}
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


    /**
     * Return a Relation that defines the subnodes of a virtual node.
     */
    public Relation getVirtualSubnodeRelation () {
	if (!virtual)
	    throw new RuntimeException ("getVirtualSubnodeRelation called on non-virtual relation");
	Relation vr = null;
	if (subnoderelation != null)
	    vr = subnoderelation.makeClone ();
	else
	    vr = makeClone ();
	vr.groupby = groupby;
	vr.groupbyorder = groupbyorder;
	vr.groupbyprototype = groupbyprototype;
	vr.order = order;
	vr.subnoderelation = subnoderelation;
	vr.aggressiveLoading = aggressiveLoading;
	vr.aggressiveCaching = aggressiveCaching;
	return vr;
    }

    /**
     * Return a Relation that defines the properties of a virtual node.
     */
    public Relation getVirtualPropertyRelation () {
	if (!virtual)
	    throw new RuntimeException ("getVirtualPropertyRelation called on non-virtual relation");
	Relation vr = makeClone ();
	vr.groupby = groupby;
	vr.groupbyorder = groupbyorder;
	vr.groupbyprototype = groupbyprototype;
	vr.order = order;
	vr.subnoderelation = subnoderelation;
	return vr;
    }

    /**
     * Return a Relation that defines the subnodes of a group-by node.
     */
    public Relation getGroupbySubnodeRelation () {
	if (groupby == null)
	    throw new RuntimeException ("getGroupbySubnodeRelation called on non-group-by relation");
	Relation vr = null;
	if (subnoderelation != null)
	    vr =  subnoderelation.makeClone ();
	else
	    vr =  makeClone ();
	vr.order = order;
	vr.prototype = groupbyprototype;
	return vr;
    }

    /**
     * Return a Relation that defines the properties of a group-by node.
     */
    public Relation getGroupbyPropertyRelation () {
	if (groupby == null)
	    throw new RuntimeException ("getGroupbyPropertyRelation called on non-group-by relation");
	Relation vr = makeClone ();
	vr.order = order;
	vr.prototype = groupbyprototype;
	return vr;
    }

    public Relation makeClone () {
	return new Relation (other, localField, remoteField, direction, subnodesAreProperties);
    }


    public String toString () {
	return "Relation["+home+">"+other+"]";
    }

}




















































