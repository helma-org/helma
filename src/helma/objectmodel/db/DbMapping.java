// DbMapping.java
// Copyright (c) Hannes Wallnöfer 1998-2000

package helma.objectmodel.db;

import helma.framework.core.Application;
import helma.util.Updatable;
import helma.util.SystemProperties;
import java.util.HashMap;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.sql.*;

/**
  * A DbMapping describes how a certain type of  Nodes is to mapped to a
  * relational database table. Basically it consists of a set of JavaScript property-to-
  * Database row bindings which are represented by instances of the Relation class.
  */

public final class DbMapping implements Updatable {

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

    // Relations describing subnodes and properties.
    Relation subRelation;
    Relation propRelation;

    // if this defines a subnode mapping with groupby layer,
    // we need a DbMapping for those groupby nodes
    DbMapping groupbyMapping;

     // Map of property names to Relations objects
    HashMap prop2db;
    // Map of db columns to Relations objects.
    // Case insensitive, keys are stored in upper case so 
    // lookups must do a toUpperCase().
    HashMap db2prop;

    // list of columns to fetch from db
    DbColumn[] columns = null;
    // Map of db columns by name
    HashMap columnMap;

    // pre-rendered select statement
    String selectString = null;
    String insertString = null;
    String updateString = null;

    // db field used as primary key
    private String idField;
    // db field used as object name
    String nameField;
    // db field used to identify name of prototype to use for object instantiation
    String protoField;

    // name of parent prototype, if any
    String extendsProto;
    // dbmapping of parent prototype, if any
    DbMapping parentMapping;

    // db field that specifies the prototype of an object
    String prototypeField;

    // descriptor for key generation method
    private String idgen;
    // remember last key generated for this table
    long lastID;

    // timestamp of last modification of the mapping (type.properties)
    long lastTypeChange;
    // timestamp of last modification of an object of this type
    long lastDataChange;

    /**
     * Create an empty DbMapping
     */
    public DbMapping (Application app) {

	this.app = app;
	this.typename = null;

	prop2db = new HashMap ();
	db2prop = new HashMap ();

	parent = null;

	idField = null;
    }

    /**
     * Create a DbMapping from a type.properties property file
     */
    public DbMapping (Application app, String typename, SystemProperties props) {

	this.app = app;
	this.typename = typename;
	// create a unique instance of the string. This is useful so 
	// we can compare types just by using == instead of equals.
	if (typename != null)
	    typename = typename.intern ();

	prop2db = new HashMap ();
	db2prop = new HashMap ();
	
	columnMap = new HashMap ();

	parent = null;

	idField = null;

	this.props = props;
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
	// read in properties
	table = props.getProperty ("_table");
	idgen = props.getProperty ("_idgen");
	// see if there is a field which specifies the prototype of objects, if different prototypes
	// can be stored in this table
	prototypeField = props.getProperty ("_prototypefield");
	// see if this prototype extends (inherits from) any other prototype
	extendsProto = props.getProperty ("_extends");

	sourceName = props.getProperty ("_db");
	if (sourceName != null) {
	    source = app.getDbSource (sourceName);
	    if (source == null) {
	        app.logEvent ("*** Data Source for prototype "+typename+" does not exist: "+sourceName);
	        app.logEvent ("*** accessing or storing a "+typename+" object will cause an error.");
	    } else if (table == null) {
	        app.logEvent ("*** No table name specified for prototype "+typename);
	        app.logEvent ("*** accessing or storing a "+typename+" object will cause an error.");
	        // mark mapping as invalid by nulling the source field
	        source = null;
	    }
	}

	// if id field is null, we assume "ID" as default. We don't set it
	// however, so that if null we check the parent prototype first.
	idField = props.getProperty ("_id");

	nameField = props.getProperty ("_name");

	protoField = props.getProperty ("_prototype");

	String parentSpec = props.getProperty ("_parent");
	if (parentSpec != null) {
	    // comma-separated list of properties to be used as parent
	    StringTokenizer st = new StringTokenizer (parentSpec, ",;");
	    parent = new ParentInfo[st.countTokens()];
	    for (int i=0; i<parent.length; i++)
	        parent[i] = new ParentInfo (st.nextToken().trim());
	} else {
	    parent = null;
	}

	lastTypeChange = props.lastModified ();
	// null the cached columns and select string
	columns = null;
	columnMap.clear();
	selectString = insertString = updateString = null;


	if (extendsProto != null) {
	    parentMapping = app.getDbMapping (extendsProto);
	}

	// if (table != null && source != null) {
	// app.logEvent ("set data source for "+typename+" to "+source);
	HashMap p2d = new HashMap ();
	HashMap d2p = new HashMap ();

	for (Enumeration e=props.keys(); e.hasMoreElements(); ) {
	    String propName = (String) e.nextElement ();

	    try {
	        // ignore internal properties (starting with "_") and sub-options (containing a ".")
	        if (!propName.startsWith ("_") && propName.indexOf (".") < 0) {
	            String dbField = props.getProperty (propName);
	            // check if a relation for this propery already exists. If so, reuse it
	            Relation rel = propertyToRelation (propName);
	            if (rel == null)
	                rel = new Relation (dbField, propName, this, props);
	            rel.update (dbField, props);
	            p2d.put (propName, rel);
	            if (rel.columnName != null &&
	                    (rel.reftype == Relation.PRIMITIVE ||
	                     rel.reftype == Relation.REFERENCE))
	                d2p.put (rel.columnName.toUpperCase (), rel);
	            // app.logEvent ("Mapping "+propName+" -> "+dbField);
	        }
	    } catch (Exception x) {
	        app.logEvent ("Error in type.properties: "+x.getMessage ());
	    }
	}

	prop2db = p2d;
	db2prop = d2p;

	String subnodeMapping = props.getProperty ("_children");
	if (subnodeMapping != null) {
	    try {
	        // check if subnode relation already exists. If so, reuse it
	        if (subRelation == null)
	            subRelation = new Relation (subnodeMapping, "_children", this, props);
	        subRelation.update (subnodeMapping, props);
	        // if subnodes are accessed via access name or group name,
	        // the subnode relation is also the property relation.
	        if (subRelation.accessor != null || subRelation.groupby != null)
	            propRelation = subRelation;
	    } catch (Exception x) {
	        app.logEvent ("Error reading _subnodes relation for "+typename+": "+x.getMessage ());
	        // subRelation = null;
	    }
	} else {
	    subRelation = propRelation = null;
	}

	if (groupbyMapping != null) {
	    initGroupbyMapping ();
	    groupbyMapping.lastTypeChange = this.lastTypeChange;
	}
    }


    /**
     * Method in interface Updatable.
     */
    public void remove () {
	// do nothing, removing of type properties is not implemented.
    }

    /**
     * Get a JDBC connection for this DbMapping.
     */
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

    /**
     * Get the DbSource object for this DbMapping. The DbSource describes a JDBC
     * data source including URL, JDBC driver, username and password.
     */
    public DbSource getDbSource () {
	if (source == null && parentMapping != null)
	    return parentMapping.getDbSource ();
	return source;
    }

    /**
     * Get the URL of the data source used for this mapping.
     */
    public String getSourceID () {
	if (source == null && parentMapping != null)
	    return parentMapping.getSourceID ();
	return source == null ? "" : source.url;
    }

    /**
     * Get the table name used for this type mapping.
     */
    public String getTableName () {
	if (source == null && parentMapping != null)
	    return parentMapping.getTableName ();
	return table;
    }

    /**
     * Get the application this DbMapping belongs to.
     */
    public Application getApplication () {
	return app;
    }

    /**
     * Get the name of this mapping's application
     */
    public String getAppName () {
	return app.getName();
    }

    /**
     * Get the name of the object type this DbMapping belongs to.
     */
    public String getTypeName () {
	return typename;
    }

    /**
     * Get the name of this type's parent type, if any.
     */
    public String getExtends () {
	return extendsProto;
    }

    /**
     * Get the primary key column name for objects using this mapping.
     */
    public String getIDField () {
	if (idField == null && parentMapping != null)
	    return parentMapping.getIDField ();
	return idField == null ? "ID" : idField;
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
	if (columnName == null)
	    return null;
	if (table == null && parentMapping != null)
	    return parentMapping.columnNameToProperty (columnName);
	Relation rel = (Relation) db2prop.get (columnName.toUpperCase ());
	if (rel != null  && (rel.reftype == Relation.PRIMITIVE || rel.reftype == Relation.REFERENCE))
	    return rel.propName;
	return null;
    }

    /**
     * Translate an object property name to a database column name according to this mapping.
     */
    public String propertyToColumnName (String propName) {
	if (propName == null)
	    return null;
	if (table == null && parentMapping != null)
	    return parentMapping.propertyToColumnName (propName);
	// FIXME: prop2db stores keys in lower case, because it gets them
	// from a SystemProperties object which converts keys to lower case.
	Relation rel = (Relation) prop2db.get (propName.toLowerCase());
	if (rel != null  && (rel.reftype == Relation.PRIMITIVE || rel.reftype == Relation.REFERENCE))
	    return rel.columnName;
	return null;
    }

    /**
     * Translate a database column name to an object property name according to this mapping.
     */
    public Relation columnNameToRelation (String columnName) {
	if (columnName == null)
	    return null;
	if (table == null && parentMapping != null)
	    return parentMapping.columnNameToRelation (columnName);
	return (Relation) db2prop.get (columnName.toUpperCase ());
    }

    /**
     * Translate an object property name to a database column name according to this mapping.
     */
    public Relation propertyToRelation (String propName) {
	if (propName == null)
	    return null;
	if (table == null && parentMapping != null)
	    return parentMapping.propertyToRelation (propName);
	// FIXME: prop2db stores keys in lower case, because it gets them
	// from a SystemProperties object which converts keys to lower case.
	return (Relation) prop2db.get (propName.toLowerCase());
    }


    /**
     * This returns the parent info array, which tells an object of this type how to
     * determine its parent object.
     */
    public synchronized ParentInfo[] getParentInfo () {
    	if (parent == null && parentMapping != null)
    	    return parentMapping.getParentInfo ();
	return parent;
    }


    public DbMapping getSubnodeMapping () {
	if (subRelation != null)
	    return subRelation.otherType;
    	if (parentMapping != null)
    	    return parentMapping.getSubnodeMapping ();
	return null;
    }


    public DbMapping getExactPropertyMapping (String propname) {
	Relation rel = getExactPropertyRelation (propname);
	return rel != null ? rel.otherType : null;
    }

    public DbMapping getPropertyMapping (String propname) {
	Relation rel = getPropertyRelation (propname);
	if (rel != null) {
	    // if this is a virtual node, it doesn't have a dbmapping
	    if (rel.virtual && rel.prototype == null)
	        return null;
	    else
	        return rel.otherType;
	}
	return null;
    }

    /**
     * If subnodes are grouped by one of their properties, return the
     * db-mapping with the right relations to create the group-by nodes
     */
    public synchronized DbMapping getGroupbyMapping () {
	if (subRelation == null || subRelation.groupby == null)
	    return null;
	if (groupbyMapping == null) {
	    initGroupbyMapping ();
	}
	return groupbyMapping;
    }

    /**
     * Initialize the dbmapping used for group-by nodes.
     */
    private void initGroupbyMapping () {
	// if a prototype is defined for groupby nodes, use that
	// if mapping doesn' exist or isn't defined, create a new (anonymous internal) one
	groupbyMapping = new DbMapping (app);
	// If a mapping is defined, make the internal mapping inherit from
	// the defined named prototype.
	if (subRelation.groupbyPrototype != null)
	    groupbyMapping.parentMapping = app.getDbMapping (subRelation.groupbyPrototype);
	groupbyMapping.subRelation = subRelation.getGroupbySubnodeRelation ();
	if (propRelation != null)
	    groupbyMapping.propRelation = propRelation.getGroupbyPropertyRelation ();
	else
	    groupbyMapping.propRelation = subRelation.getGroupbyPropertyRelation ();
	groupbyMapping.typename = subRelation.groupbyPrototype;
    }


    public void setPropertyRelation (Relation rel) {
	propRelation = rel;
    }

    public Relation getSubnodeRelation () {
	if (subRelation == null && parentMapping != null)
	    return parentMapping.getSubnodeRelation ();
	return subRelation;
    }

    public Relation getPropertyRelation () {
	if (propRelation == null && parentMapping != null)
	    return parentMapping.getPropertyRelation ();
	return propRelation;
    }

    public Relation getPropertyRelation (String propname) {
	if (propname == null)
	    return getPropertyRelation ();
	// first try finding an exact match for the property name
	Relation rel = getExactPropertyRelation (propname);
	// if not defined, return the generic property mapping
	if (rel == null)
	    rel = getPropertyRelation ();
	return rel;
    }

    public Relation getExactPropertyRelation (String propname) {
	if (propname == null)
	    return null;
	Relation rel = (Relation) prop2db.get (propname.toLowerCase());
	if (rel == null && parentMapping != null)
	    rel = parentMapping.getExactPropertyRelation (propname);
	return rel;
    }

    public String getSubnodeGroupby () {
	if (subRelation == null && parentMapping != null)
	    return parentMapping.getSubnodeGroupby ();
	return subRelation == null ? null : subRelation.groupby;
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
     * Return an array of DbColumns for the relational table mapped by this DbMapping.
     */
    public synchronized DbColumn[] getColumns() throws ClassNotFoundException, SQLException {
	if (!isRelational ())
	    throw new SQLException ("Can't get columns for non-relational data mapping "+this);
	if (source == null && parentMapping != null)
	    return parentMapping.getColumns ();
	// Use local variable cols to avoid synchronization (schema may be nulled elsewhere)
	if (columns == null) {
	    // we do two things here: set the SQL type on the Relation mappings
	    // and build a string of column names.
	    Connection con = getConnection ();
	    Statement stmt = con.createStatement ();
	    String t = getTableName();
	    if (t == null)
	        throw new SQLException ("Table name is null in getColumns() for "+this);
	    ResultSet rs = stmt.executeQuery (
	        new StringBuffer("SELECT * FROM ")
	        .append(t).append(" WHERE 1 = 0").toString());
	    if (rs == null)
	        throw new SQLException ("Error retrieving columns for "+this);
	    ResultSetMetaData meta = rs.getMetaData ();
	    // ok, we have the meta data, now loop through mapping...
	    int ncols = meta.getColumnCount ();
	    columns = new DbColumn[ncols];
	    for (int i=0; i<ncols; i++) {
	        String colName = meta.getColumnName (i+1);
	        Relation rel = columnNameToRelation (colName);
	        columns[i] = new DbColumn (colName, meta.getColumnType (i+1), rel);
	    }
	}
	return columns;
    }

    public DbColumn getColumn (String columnName) throws ClassNotFoundException, SQLException {
	DbColumn col = (DbColumn) columnMap.get(columnName);
	if (col == null) {
	    DbColumn[] cols = columns;
	    if (cols == null)
	        cols = getColumns();
	    for (int i=0; i<cols.length; i++) {
	        if (columnName.equalsIgnoreCase (cols[i].getName())) {
	            col = cols[i];
	            break;
	        }
	    }
	    if (col == null)
	        throw new SQLException ("Column "+columnName+" not found in "+this);
	    columnMap.put (columnName, col);
	}
	return col;
    }

    public StringBuffer getSelect () throws SQLException, ClassNotFoundException {
	String sel = selectString;
	if (sel != null)
	    return new StringBuffer (sel);
	StringBuffer s = new StringBuffer ("SELECT * FROM ");
	s.append (getTableName ());
	s.append (" ");
	// cache rendered string for later calls.
	selectString = s.toString();
	return s;
    }


    public StringBuffer getInsert () {
	String ins = insertString;
	if (ins != null)
	    return new StringBuffer (ins);
	StringBuffer s = new StringBuffer ("INSERT INTO ");
	s.append (getTableName ());
	s.append (" ( ");
	s.append (getIDField());
	// cache rendered string for later calls.
	insertString = s.toString();
	return s;
    }

    public StringBuffer getUpdate () {
	String upd = updateString;
	if (upd != null)
	    return new StringBuffer (upd);
	StringBuffer s = new StringBuffer ("UPDATE ");
	s.append (getTableName ());
	s.append (" SET ");
	// cache rendered string for later calls.
	updateString = s.toString();
	return s;
    }

    /**
     *  Return true if values for the column identified by the parameter need
     *  to be quoted in SQL queries.
     */
    public boolean needsQuotes (String columnName) throws SQLException {
	if (table == null && parentMapping != null)
	    return parentMapping.needsQuotes (columnName);
	try {
	    DbColumn col = getColumn (columnName);
	    switch (col.getType()) {
	        case Types.CHAR:
	        case Types.VARCHAR:
	        case Types.LONGVARCHAR:
	        case Types.BINARY:
	        case Types.VARBINARY:
	        case Types.LONGVARBINARY:
	        case Types.DATE:
	        case Types.TIME:
	        case Types.TIMESTAMP:
	            return true;
	        default:
	            return false;
	    }
	} catch (Exception x) {
	    throw new SQLException (x.getMessage ());
	}
    }


    public String toString () {
	if (typename == null)
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

    public HashMap getProp2DB () {
	if (table == null && parentMapping != null)
	    return parentMapping.getProp2DB ();
	return prop2db;
    }

    public Iterator getDBPropertyIterator () {
	if (table == null && parentMapping != null)
	    return parentMapping.getDBPropertyIterator ();
	return db2prop.values ().iterator ();
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

    /**
     *  Tell if another DbMapping is storage-compatible to this one, i.e. it is stored in the same table or
     *  embedded database.
     */
    public boolean isStorageCompatible (DbMapping other) {
	if (other == null)
	    return !isRelational ();
	if (isRelational ())
	    return getTableName().equals (other.getTableName ()) &&
	    		getDbSource().equals (other.getDbSource ());
	return !other.isRelational ();
    }

    /**
     *  Return true if this db mapping represents the prototype indicated
     *  by the string argument, either itself or via one of its parent prototypes.
     */
    public boolean isInstanceOf (String other) {
	if (typename != null && typename.equals (other))
	    return true;
	DbMapping p = parentMapping;
	while (p != null) {
	    if (p.typename != null && p.typename.equals (other))
	        return true;
	    p = p.parentMapping;
	}
	return false;
    }


    public DbMapping getParentMapping () {
	return parentMapping;
    }
    
    public SystemProperties getProperties () {
	return props;
    }

}

