// DbMapping.java
// Copyright (c) Hannes Wallnöfer 1998-2000

package helma.objectmodel;

import helma.framework.core.Application;
import helma.objectmodel.db.WrappedNodeManager;
import java.util.*;
import java.sql.*;
import com.workingdogs.village.*;

/** 
  * A DbMapping describes how a certain type of  Nodes is to mapped to a 
  * relational database table. Basically it consists of a set of JavaScript property-to-
  * Database row bindings which are represented by instances of the Relation class.
  */
  
public class DbMapping {

    Application app;
    String typename;

    SystemProperties props;

    DbSource source;
    String table;

    String[] parent;  // list of properties to try for parent
    Boolean[] anonymous;  // are parent relations anonymous or not?
    DbMapping subnodes;
    DbMapping properties;
    private Relation subnodesRel;
    private Relation propertiesRel;

     // Map of property names to Relations objects
    public Hashtable prop2db;
     // Map of db columns to Relations objects
    public Hashtable db2prop;

    String idField;
    String nameField;
    private String idgen;

    Schema schema = null;
    KeyDef keydef = null;

    private long lastTypeChange;
    public long lastDataChange;

    public DbMapping () {

	prop2db = new Hashtable ();
	db2prop = new Hashtable ();

	parent = null;
	subnodes = null;
	properties = null;
	idField = "id";
    }

    public DbMapping (Application app, String typename, SystemProperties props) {

	this.app = app;
	this.typename = typename;

	prop2db = new Hashtable ();
	db2prop = new Hashtable ();

	parent = null;
	subnodes = null;
	properties = null;
	idField = "id";

	this.props = props;
	read ();

	app.putDbMapping (typename, this);
    }

    /**
     * Read the mapping from the Properties. Return true if the properties were changed.
     */
    public synchronized boolean read () {

	long lastmod = props.lastModified ();
	if (lastmod == lastTypeChange)
	    return false;

	this.table = props.getProperty ("_tablename");
	this.idgen = props.getProperty ("_idgen");
	String sourceName = props.getProperty ("_datasource");
	if (sourceName != null)
	    source = (DbSource) IServer.dbSources.get (sourceName.toLowerCase ());
	lastTypeChange = lastmod;
	// set the cached schema & keydef to null so it's rebuilt the next time around
	schema = null;
	keydef = null;
	return true;
    }

    public synchronized void rewire () {

	// if (table != null && source != null) {
	// IServer.getLogger().log ("set data source for "+typename+" to "+source);
	Hashtable p2d = new Hashtable ();
	Hashtable d2p = new Hashtable ();

	for (Enumeration e=props.keys(); e.hasMoreElements(); ) {
	    String propName = (String) e.nextElement ();

	    if (!propName.startsWith ("_") && propName.indexOf (".") < 0) {
	        String dbField = props.getProperty (propName);
	        Relation rel = new Relation (dbField, propName, this, props);
	        p2d.put (propName, rel);
	        if (rel.localField != null)
	            d2p.put (rel.localField, rel);
	        // IServer.getLogger().log ("Mapping "+propName+" -> "+dbField);

	    }
	}
	prop2db = p2d;
	db2prop = d2p;

	idField = props.getProperty ("_id");

	nameField = props.getProperty ("_name");

	String ano = props.getProperty ("_anonymous");
	if (ano != null) {
	    // comma-separated list of true/false values
	    StringTokenizer st = new StringTokenizer (ano, ",; ");
	    anonymous = new Boolean[st.countTokens()];
	    for (int i=0; i<anonymous.length; i++)
	        anonymous[i] = "false".equalsIgnoreCase (st.nextToken().trim()) ? Boolean.FALSE : Boolean.TRUE;
	} else
	    anonymous = null;

	String parentMapping = props.getProperty ("_parent");
	if (parentMapping != null) {
	    // comma-separated list of properties to be used as parent
	    StringTokenizer st = new StringTokenizer (parentMapping, ",; ");
	    parent = new String[st.countTokens()];
	    for (int i=0; i<parent.length; i++)
	        parent[i] = st.nextToken().trim();
	} else
	    parent = null;

	String subnodeMapping = props.getProperty ("_subnodes");
	if (subnodeMapping != null) {
	    subnodesRel = new Relation (subnodeMapping, "_subnodes", this, props);
	    if (subnodesRel.isReference ())
	        subnodes = subnodesRel.other;
	    else
	        subnodes = (DbMapping) app.getDbMapping (subnodeMapping);
	} else
	    subnodesRel = null;

	String propertiesMapping = props.getProperty ("_properties");
	if (propertiesMapping != null) {
	    propertiesRel = new Relation (propertiesMapping, "_properties", this, props);
	    if (propertiesRel.isReference ())
	        properties = propertiesRel.other;
	    else
	        properties = (DbMapping) app.getDbMapping (propertiesMapping);
	    // take over groupby flag from subnodes, if properties are subnodes
	    if (propertiesRel.subnodesAreProperties && subnodesRel != null)
	        propertiesRel.groupby = subnodesRel.groupby;
	} else
	    propertiesRel = null;

	IServer.getLogger().log ("rewiring: "+parent+" -> "+this+" -> "+subnodes);
    }



    public Connection getConnection () throws ClassNotFoundException, SQLException {
	if (source == null)
	    throw new SQLException ("Tried to get Connection from non-relational embedded data source.");
	return source.getConnection ();
    }

    public DbSource getDbSource () {
	return source;
    }

    public String getSourceID () {
	return source == null ? "" : source.url;
    }

    public String getTableName () {
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

    /**
     * Get the primary key column name for objects using this mapping.
     */
    public String getIDField () {
	return idField;
    }

    /**
     * Get the column used for (internal) names of objects of this type.
     */
    public String getNameField () {
	return nameField;
    }

    /**
     * Translate a database column name to a JavaScript property name according to this mapping.
     */
    public Relation columnNameToProperty (String columnName) {
	return (Relation) db2prop.get (columnName);
    }

    /**
     * Translate a JavaScript property name to a database column name according to this mapping.
     */
    public Relation propertyToColumnName (String propName) {
	return (Relation) prop2db.get (propName);
    }

    public synchronized String[] getParentPropNames () {
	return parent;
    }

    public synchronized Boolean[] getAnonymous () {
	return anonymous;
    }

    public DbMapping getSubnodeMapping () {
	return subnodes;
    }

    public void setSubnodeMapping (DbMapping sm) {
	subnodes = sm;
    }

    public DbMapping getExactPropertyMapping (String propname) {
	if (propname == null)
	    return null;
	Relation rel = (Relation) prop2db.get (propname.toLowerCase());
	return rel != null ? rel.other : null;
    }

    public DbMapping getPropertyMapping (String propname) {
	if (propname == null)
	    return properties;
	Relation rel = (Relation) prop2db.get (propname.toLowerCase());
	if (rel != null) {
	    // if this is a virtual node, it doesn't have a dbmapping
	    if (rel.virtual && rel.prototype == null)
	        return null;
	    else
	        return rel.other;
	}
	return properties;
    }

    public void setPropertyMapping (DbMapping pm) {
	properties = pm;
    }

    public void setSubnodeRelation (Relation rel) {
	subnodesRel = rel;
    }

    public void setPropertyRelation (Relation rel) {
	propertiesRel = rel;
    }

    public Relation getSubnodeRelation () {
	return subnodesRel;
    }

    public Relation getPropertyRelation () {
	return propertiesRel;
    }

    public Relation getPropertyRelation (String propname) {
	if (propname == null)
	    return propertiesRel;
	Relation rel = (Relation) prop2db.get (propname.toLowerCase());
	return rel != null ? rel : propertiesRel;
    }


    public String getIDgen () {
	return idgen;
    }

    public WrappedNodeManager getWrappedNodeManager () {
	if (app == null)
	   throw new RuntimeException ("Can't get node manager from internal db mapping");
	return app.getWrappedNodeManager ();
    }

    public boolean isRelational () {
	return source != null;
    }

    /**
     * Return a Village Schema object for this DbMapping.
     */
    public synchronized Schema getSchema () throws ClassNotFoundException, SQLException, DataSetException {
	if (!isRelational ())
	    throw new SQLException ("Can't get Schema for non-relational data mapping");
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

}

































































































































