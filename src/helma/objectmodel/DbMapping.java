// DbMapping.java
// Copyright (c) Hannes Wallnöfer 1998-2000

package helma.objectmodel;

import helma.framework.core.Application;
import helma.objectmodel.db.WrappedNodeManager;
import helma.util.Updatable;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.sql.*;
import com.workingdogs.village.*;

/** 
  * A DbMapping describes how a certain type of  Nodes is to mapped to a 
  * relational database table. Basically it consists of a set of JavaScript property-to-
  * Database row bindings which are represented by instances of the Relation class.
  */
  
public class DbMapping implements Updatable {

    // DbMappings belong to an application
    Application app;
    // prototype name of this mapping
    String typename;

    // properties from where the mapping is read
    SystemProperties props;

    // name of data source to which this mapping writes
    DbSource source;
    // name of datasource
    String sourceName;
    // name of db table
    String table;

    // list of properties to try for parent
    ParentInfo[] parent;
    // list of properties to try as skinmanager
    String[] skinmgr;

    // DbMapping subnodes;
    // DbMapping properties;
    Relation subnodesRel;
    Relation propertiesRel;

    // if this defines a subnode mapping with groupby layer, we need a DbMapping for those groupby nodes
    DbMapping groupbyMapping;

     // Map of property names to Relations objects
    Hashtable prop2db;
     // Map of db columns to Relations objects
    Hashtable db2prop;

    // db field used as primary key
    String idField;
    // db field used as object name
    String nameField;
    // db field used to identify name of prototype to use for object instantiation
    String protoField;

    // name of parent prototype, if any
    String extendsProto;
    // dbmapping of parent prototype, if any
    DbMapping parentMapping;
    boolean inheritsMapping;

    // db field that specifies the prototype of an object
    String prototypeField;

    // descriptor for key generation method
    private String idgen;
    // remember last key generated for this table
    long lastID;

    // the (village) schema of the database table
    Schema schema = null;
    // the (village) keydef of the db table
    KeyDef keydef = null;

    // timestamp of last modification of the mapping (type.properties)
    long lastTypeChange;
    // timestamp of last modification of an object of this type
    long lastDataChange;


    /**
     * Create an empty DbMapping
     */
    public DbMapping () {

	prop2db = new Hashtable ();
	db2prop = new Hashtable ();

	parent = null;
	// subnodes = null;
	// properties = null;
	idField = "id";
    }

    /**
     * Create a DbMapping from a type.properties property file
     */
    public DbMapping (Application app, String typename, SystemProperties props) {

	this.app = app;
	this.typename = typename;

	prop2db = new Hashtable ();
	db2prop = new Hashtable ();

	parent = null;
	// subnodes = null;
	// properties = null;
	idField = "id";

	this.props = props;
	update ();

	app.putDbMapping (typename, this);
    }

    /**
     * Tell the type manager whether we need update() to be called
     */
    public boolean needsUpdate () {
	return props.lastModified () != lastTypeChange;
    }


    /**
     * Read the mapping from the Properties. Return true if the properties were changed.
     * The read is split in two, this method and the rewire method. The reason is that in order
     * for rewire to work, all other db mappings must have been initialized and registered.
     */
    public synchronized void update () {

	table = props.getProperty ("_tablename");
	idgen = props.getProperty ("_idgen");
	// see if there is a field which specifies the prototype of objects, if different prototypes
	// can be stored in this table
	prototypeField = props.getProperty ("_prototypefield");
	// see if this prototype extends (inherits from) any other prototype
	extendsProto = props.getProperty ("_extends");
	
	sourceName = props.getProperty ("_datasource");
	if (sourceName != null) {
	    source = app.getDbSource (sourceName);
	    if (source == null) {
	        app.logEvent ("*** Data Source for prototype "+typename+" does not exist: "+sourceName);
	        app.logEvent ("*** accessing or storing a "+typename+" object will cause an error.");
	    }
	}

	// id field must not be null, default is "id"
	idField = props.getProperty ("_id", "id");

	nameField = props.getProperty ("_name");

	protoField = props.getProperty ("_prototype");
	
	String parentMapping = props.getProperty ("_parent");
	if (parentMapping != null) {
	    // comma-separated list of properties to be used as parent
	    StringTokenizer st = new StringTokenizer (parentMapping, ",;");
	    parent = new ParentInfo[st.countTokens()];
	    for (int i=0; i<parent.length; i++)
	        parent[i] = new ParentInfo (st.nextToken().trim());
	} else
	    parent = null;
	
	String skm = props.getProperty ("_skinmanager");
	if (skm != null) {
	    StringTokenizer st = new StringTokenizer (skm, ",;");
	    skinmgr = new String[st.countTokens()];
	    for (int i=0; i<skinmgr.length; i++)
	        skinmgr[i] = st.nextToken().trim();
	} else
	    skinmgr = null;

	lastTypeChange = props.lastModified ();
	// set the cached schema & keydef to null so it's rebuilt the next time around
	schema = null;
	keydef = null;
    }

    /**
     * This is the second part of the property reading process, called after the first part has been
     * completed on all other mappings in this application
     */
    public synchronized void rewire () {

	if (extendsProto != null) {
	    parentMapping = app.getDbMapping (extendsProto);
	}
	
	// if (table != null && source != null) {
	// app.logEvent ("set data source for "+typename+" to "+source);
	Hashtable p2d = new Hashtable ();
	Hashtable d2p = new Hashtable ();

	for (Enumeration e=props.keys(); e.hasMoreElements(); ) {
	    String propName = (String) e.nextElement ();

	    try {
	        if (!propName.startsWith ("_") && propName.indexOf (".") < 0) {
	            String dbField = props.getProperty (propName);
	            // check if a relation for this propery already exists. If so, reuse it
	            Relation rel = propertyToRelation (propName);
	            if (rel == null)
	                rel = new Relation (dbField, propName, this, props);
	            else
	                rel.update (dbField, props);
	            p2d.put (propName, rel);
	            if (rel.localField != null &&
	            		(rel.direction == Relation.PRIMITIVE ||
	            		rel.direction == Relation.FORWARD))
	                d2p.put (rel.localField, rel);
	            // app.logEvent ("Mapping "+propName+" -> "+dbField);
	        }
	    } catch (Exception x) {
	        app.logEvent ("Error in type.properties: "+x.getMessage ());
	    }
	}

	prop2db = p2d;
	db2prop = d2p;

	String subnodeMapping = props.getProperty ("_subnodes");
	if (subnodeMapping != null) {
	    try {
	        // check if subnode relation already exists. If so, reuse it
	        if (subnodesRel == null)
	            subnodesRel = new Relation (subnodeMapping, "_subnodes", this, props);
	        else
	            subnodesRel.update (subnodeMapping, props);
	        // if (subnodesRel.isReference ())
	            // subnodes = subnodesRel.other;
	        // else
	        //     subnodes = (DbMapping) app.getDbMapping (subnodeMapping);
	    } catch (Exception x) {
	        app.logEvent ("Error reading _subnodes relation for "+typename+": "+x.getMessage ());
	        // subnodesRel = null;
	    }
	} else
	    subnodesRel = null;

	String propertiesMapping = props.getProperty ("_properties");
	if (propertiesMapping != null) {
	    try {
	        // check if property relation already exists. If so, reuse it
	        if (propertiesRel == null)
	            propertiesRel = new Relation (propertiesMapping, "_properties", this, props);
	        else
	            propertiesRel.update (propertiesMapping, props);
	        // if (propertiesRel.isReference ())
	        //     properties = propertiesRel.other;
	        // else
	        //     properties = (DbMapping) app.getDbMapping (propertiesMapping);
	        // take over groupby flag from subnodes, if properties are subnodes
	        if (propertiesRel.subnodesAreProperties && subnodesRel != null)
	            propertiesRel.groupby = subnodesRel.groupby;
	    } catch (Exception x) {
	        app.logEvent ("Error reading _properties relation for "+typename+": "+x.getMessage ());
	        // propertiesRel = null;
	    }
	} else
	    propertiesRel = null;

	if (groupbyMapping != null) {
	    groupbyMapping.subnodesRel = subnodesRel == null ? null : subnodesRel.getGroupbySubnodeRelation ();
	    groupbyMapping.propertiesRel = propertiesRel == null ? null : propertiesRel.getGroupbyPropertyRelation ();
	    groupbyMapping.lastTypeChange = this.lastTypeChange;
	}
    }



    public Connection getConnection () throws ClassNotFoundException, SQLException {
	// if source was previously not available, check again
	if (source == null && sourceName != null)
	    source = app.getDbSource (sourceName);
	if (sourceName == null && parentMapping != null)
	    return parentMapping.getConnection ();
	if (source == null) {
	    if (sourceName == null)
	        throw new SQLException ("Tried to get Connection from non-relational embedded data source.");
	    else
	        throw new SQLException ("Datasource is not defined: "+sourceName+".");
	}
	return source.getConnection ();
    }

    public DbSource getDbSource () {
	if (source == null && parentMapping != null)
	    return parentMapping.getDbSource ();
	return source;
    }

    public String getSourceID () {
	if (source == null && parentMapping != null)
	    return parentMapping.getSourceID ();
	return source == null ? "" : source.url;
    }

    public String getTableName () {
	if (source == null && parentMapping != null)
	    return parentMapping.getTableName ();
	return table;
    }

    public Application getApplication () {
	return app;
    }

    public String getAppName () {
	return app.getName();
    }

    public String getTypeName () {
	return typename;
    }

    public String getExtends () {
	return extendsProto;
    }

    /**
     * Get the primary key column name for objects using this mapping.
     */
    public String getIDField () {
    	if (idField == null && parentMapping != null)
	    return parentMapping.getIDField ();
	return idField;
    }

    /**
     * Get the column used for (internal) names of objects of this type.
     */
    public String getNameField () {
    	if (nameField == null && parentMapping != null)
	    return parentMapping.getNameField ();
	return nameField;
    }

    /**
     * Get the column used for names of prototype.
     */
    public String getPrototypeField () {
    	if (protoField == null && parentMapping != null)
	    return parentMapping.getPrototypeField ();
	return protoField;
    }


    /**
     * Translate a database column name to an object property name according to this mapping.
     */
    public String columnNameToProperty (String columnName) {
	if (table == null && parentMapping != null)
	    return parentMapping.columnNameToProperty (columnName);
	Relation rel = (Relation) db2prop.get (columnName);
	if (rel != null  && (rel.direction == Relation.PRIMITIVE || rel.direction == Relation.FORWARD))
	    return rel.propname;
	return null;
    }

    /**
     * Translate an object property name to a database column name according to this mapping.
     */
    public String propertyToColumnName (String propName) {
	if (table == null && parentMapping != null)
	    return parentMapping.propertyToColumnName (propName);
	Relation rel = (Relation) prop2db.get (propName);
	if (rel != null  && (rel.direction == Relation.PRIMITIVE || rel.direction == Relation.FORWARD))
	    return rel.localField;
	return null;
    }

    /**
     * Translate a database column name to an object property name according to this mapping.
     */
    public Relation columnNameToRelation (String columnName) {
	if (table == null && parentMapping != null)
	    return parentMapping.columnNameToRelation (columnName);
	return (Relation) db2prop.get (columnName);
    }

    /**
     * Translate an object property name to a database column name according to this mapping.
     */
    public Relation propertyToRelation (String propName) {
	if (table == null && parentMapping != null)
	    return parentMapping.propertyToRelation (propName);
	return (Relation) prop2db.get (propName);
    }


    public synchronized ParentInfo[] getParentInfo () {
    	if (parent == null && parentMapping != null)
    	    return parentMapping.getParentInfo ();
	return parent;
    }

    public String[] getSkinManagers () {
	return skinmgr;
    }


    public DbMapping getSubnodeMapping () {
	if (subnodesRel != null)
	    return subnodesRel.other;
    	if (parentMapping != null)
    	    return parentMapping.getSubnodeMapping ();
	return null;
    }


    public DbMapping getExactPropertyMapping (String propname) {
	if (propname == null)
	    return null;
	Relation rel = (Relation) prop2db.get (propname.toLowerCase());
	if (rel == null && parentMapping != null)
	    return parentMapping.getExactPropertyMapping (propname);
	return rel != null ? rel.other : null;
    }

    public DbMapping getPropertyMapping (String propname) {
	if (propname == null) {
	    if (propertiesRel != null)
	        return propertiesRel.other;
	    if (parentMapping != null)
	        return parentMapping.getPropertyMapping (null);
	}
	
	Relation rel = (Relation) prop2db.get (propname.toLowerCase());
	if (rel != null) {
	    // if this is a virtual node, it doesn't have a dbmapping
	    if (rel.virtual && rel.prototype == null)
	        return null;
	    else
	        return rel.other;
	}
	
	if (propertiesRel != null)
	    return propertiesRel.other;
	if (parentMapping != null)
	    return parentMapping.getPropertyMapping (propname);
	return null;
    }

    public DbMapping getGroupbyMapping () {
	if (subnodesRel == null || subnodesRel.groupby == null)
	    return null;
	if (groupbyMapping == null) {
	    groupbyMapping = new DbMapping ();
	    groupbyMapping.subnodesRel = subnodesRel.getGroupbySubnodeRelation ();
	    if (propertiesRel != null)
	        groupbyMapping.propertiesRel = propertiesRel.getGroupbyPropertyRelation ();
	    else
	        groupbyMapping.propertiesRel = subnodesRel.getGroupbyPropertyRelation ();
	    groupbyMapping.typename = subnodesRel.groupbyprototype;
	}
	return groupbyMapping;
    }

    /* public void setPropertyMapping (DbMapping pm) {
	properties = pm;
    } */

    public void setSubnodeRelation (Relation rel) {
	subnodesRel = rel;
    }

    public void setPropertyRelation (Relation rel) {
	propertiesRel = rel;
    }

    public Relation getSubnodeRelation () {
	if (subnodesRel == null && parentMapping != null)
	    return parentMapping.getSubnodeRelation ();
	return subnodesRel;
    }

    public Relation getPropertyRelation () {
	if (propertiesRel == null && parentMapping != null)
	    return parentMapping.getPropertyRelation ();
	return propertiesRel;
    }

    public Relation getPropertyRelation (String propname) {
	if (propname == null)
	    return getPropertyRelation ();
	Relation rel = (Relation) prop2db.get (propname.toLowerCase());
	if (rel == null && propertiesRel == null && parentMapping != null)
	    return parentMapping.getPropertyRelation (propname);
	return rel != null ? rel : propertiesRel;
    }

    public String getSubnodeGroupby () {
	if (subnodesRel == null && parentMapping != null)
	    return parentMapping.getSubnodeGroupby ();
	return subnodesRel == null ? null : subnodesRel.groupby;
    }

    public String getIDgen () {
	if (idgen == null && parentMapping != null)
	    return parentMapping.getIDgen ();
	return idgen;
    }


    public WrappedNodeManager getWrappedNodeManager () {
	if (app == null)
	   throw new RuntimeException ("Can't get node manager from internal db mapping");
	return app.getWrappedNodeManager ();
    }

    /**
     *  Tell whether this data mapping maps to a relational database table. This returns true
     *  if a datasource is specified, even if it is not a valid one. Otherwise, objects with invalid
     *  mappings would be stored in the embedded db instead of an error being thrown, which is
     *  not what we want.
     */
    public boolean isRelational () {
	if (sourceName != null)
	    return true;
	if (parentMapping != null)
	    return parentMapping.isRelational ();
	return false;
    }

    /**
     * Return a Village Schema object for this DbMapping.
     */
    public synchronized Schema getSchema () throws ClassNotFoundException, SQLException, DataSetException {
	if (!isRelational ())
	    throw new SQLException ("Can't get Schema for non-relational data mapping");
    	if (source == null && parentMapping != null)
	    return parentMapping.getSchema ();
	// Use local variable s to avoid synchronization (schema may be nulled elsewhere)
	Schema s = schema;
	if (s != null)
	    return s;
	schema = new Schema ().schema (getConnection (), table, "*");
	return schema;	
    }

    /**
     * Return a Village Schema object for this DbMapping.
     */
    public synchronized KeyDef getKeyDef () {
	if (!isRelational ())
	    throw new RuntimeException ("Can't get KeyDef for non-relational data mapping");
    	if (source == null && parentMapping != null)
	    return parentMapping.getKeyDef ();
	// Use local variable s to avoid synchronization (keydef may be nulled elsewhere)
	KeyDef k = keydef;
	if (k != null)
	    return k;
	keydef = new KeyDef ().addAttrib (idField);
	return keydef;	
    }

    public String toString () {
	if (app == null)
	    return "[unspecified internal DbMapping]";
	else
	    return ("["+app.getName()+"."+typename+"]");
    }

    public long getLastTypeChange () {
	return lastTypeChange;
    }


    public long getLastDataChange () {
	return lastDataChange;
    }

    public void notifyDataChange () {
	lastDataChange = System.currentTimeMillis ();
	if (parentMapping != null && source == null)
	    parentMapping.notifyDataChange ();
    }

    public synchronized long getNewID (long dbmax) {
	if (parentMapping != null && source == null)
	    return parentMapping.getNewID (dbmax);
	lastID = Math.max (dbmax+1, lastID+1);
	return lastID;
    }

    public Hashtable getProp2DB () {
	if (table == null && parentMapping != null)
	    return parentMapping.getProp2DB ();
	return prop2db;
    }

    public Hashtable getDB2Prop () {
	if (table == null && parentMapping != null)
	    return parentMapping.getDB2Prop ();
	return db2prop;
    }

    /**
     *  Return the name of the prototype which specifies the storage location
     * (dbsource + tablename) for this type, or null if it is stored in the embedded
     * db.
     */
    public String getStorageTypeName () {
	if (table == null && parentMapping != null)
	    return parentMapping.getStorageTypeName ();
	return sourceName == null ? null : typename;
    }

}

































































































































