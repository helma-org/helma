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

import helma.framework.core.Application;
import helma.framework.core.Prototype;
import helma.util.ResourceProperties;

import java.sql.*;
import java.util.*;

/**
 * A DbMapping describes how a certain type of  Nodes is to mapped to a
 * relational database table. Basically it consists of a set of JavaScript property-to-
 * Database row bindings which are represented by instances of the Relation class.
 */
public final class DbMapping {
    // DbMappings belong to an application
    protected final Application app;

    // prototype name of this mapping
    private final String typename;

    // properties from where the mapping is read
    private final Properties props;

    // name of data dbSource to which this mapping writes
    private DbSource dbSource;

    // name of datasource
    private String dbSourceName;

    // name of db table
    private String tableName;

    // the verbatim, unparsed _parent specification
    private String parentSetting;

    // list of properties to try for parent
    private ParentInfo[] parentInfo;

    // Relations describing subnodes and properties.
    protected Relation subRelation;
    protected Relation propRelation;

    // if this defines a subnode mapping with groupby layer,
    // we need a DbMapping for those groupby nodes
    private DbMapping groupbyMapping;

    // Map of property names to Relations objects
    private HashMap prop2db;

    // Map of db columns to Relations objects.
    // Case insensitive, keys are stored in lower case so
    // lookups must do a toLowerCase().
    private HashMap db2prop;
    
    // list of columns to fetch from db
    private DbColumn[] columns = null;

    // Map of db columns by name
    private HashMap columnMap;

    // Array of aggressively loaded references
    private Relation[] joins;

    // pre-rendered select statement
    private String selectString = null;
    private String insertString = null;
    private String updateString = null;

    // db field used as primary key
    private String idField;

    // db field used as object name
    private String nameField;

    // db field used to identify name of prototype to use for object instantiation
    private String protoField;

    // Used to map prototype ids to prototype names for
    // prototypes which extend the prototype represented by
    // this DbMapping.
    private ResourceProperties extensionMap;

    // a numeric or literal id used to represent this type in db
    private String extensionId;

    // dbmapping of parent prototype, if any
    private DbMapping parentMapping;

    // descriptor for key generation method
    private String idgen;

    // remember last key generated for this table
    private long lastID;

    // timestamp of last modification of the mapping (type.properties)
    // init value is -1 so we know we have to run update once even if
    // the underlying properties file is non-existent
    long lastTypeChange = -1;

    // timestamp of last modification of an object of this type
    long lastDataChange = 0;

    // Set of mappings that depend on us and should be forwarded last data change events
    HashSet dependentMappings = new HashSet();

    // does this DbMapping describe a virtual node (collection, mountpoint, groupnode)?
    private boolean isVirtual = false;

    // does this Dbmapping describe a group node?
    private boolean isGroup = false;

    /**
     * Create an internal DbMapping used for "virtual" mappings aka collections, mountpoints etc.
     */
    public DbMapping(Application app, String parentTypeName) {
        this(app, parentTypeName, null);
        // DbMappings created with this constructor always define virtual nodes
        isVirtual = true;
        if (parentTypeName != null) {
            parentMapping = app.getDbMapping(parentTypeName);
            if (parentMapping == null) {
                throw new IllegalArgumentException("Unknown parent mapping: " + parentTypeName);
            }
        }
    }

    /**
     * Create a DbMapping from a type.properties property file
     */
    public DbMapping(Application app, String typename, Properties props, boolean virtual) {
        this(app,  typename, props);
        isVirtual = virtual;
    }

    /**
     * Create a DbMapping from a type.properties property file
     */
    public DbMapping(Application app, String typename, Properties props) {
        this.app = app;
        // create a unique instance of the string. This is useful so
        // we can compare types just by using == instead of equals.
        this.typename = typename == null ? null : typename.intern();

        prop2db = new HashMap();
        db2prop = new HashMap();
        columnMap = new HashMap();
        parentInfo = null;
        idField = null;
        this.props = props;

        if (props != null) {
            readBasicProperties();
        }
    }

    /**
     * Tell the type manager whether we need update() to be called
     */
    public boolean needsUpdate() {
        if (props instanceof ResourceProperties) {
            return ((ResourceProperties) props).lastModified() != lastTypeChange;
        }
        return false;
    }

    /**
     * Read in basic properties and register dbmapping with the
     * dbsource.
     */
    private void readBasicProperties() {
        tableName = props.getProperty("_table");
        dbSourceName = props.getProperty("_db");

        if (dbSourceName != null) {
            dbSource = app.getDbSource(dbSourceName);

            if (dbSource == null) {
                app.logError("Data Source for prototype " + typename +
                             " does not exist: " + dbSourceName);
                app.logError("Accessing or storing a " + typename +
                             " object will cause an error.");
            } else if (tableName == null) {
                app.logError("No table name specified for prototype " + typename);
                app.logError("Accessing or storing a " + typename +
                             " object will cause an error.");

                // mark mapping as invalid by nulling the dbSource field
                dbSource = null;
            } else {
                // dbSource and tableName not null - register this instance
                dbSource.registerDbMapping(this);
            }
        }
    }



    /**
     * Read the mapping from the Properties. Return true if the properties were changed.
     * The read is split in two, this method and the rewire method. The reason is that in order
     * for rewire to work, all other db mappings must have been initialized and registered.
     */
    public synchronized void update() {
        // read in properties
        readBasicProperties();
        idgen = props.getProperty("_idgen");
        // if id field is null, we assume "ID" as default. We don't set it
        // however, so that if null we check the parent prototype first.
        idField = props.getProperty("_id");
        nameField = props.getProperty("_name");
        protoField = props.getProperty("_prototype");

        parentSetting = props.getProperty("_parent");
        if (parentSetting != null) {
            // comma-separated list of properties to be used as parent
            StringTokenizer st = new StringTokenizer(parentSetting, ",;");
            parentInfo = new ParentInfo[st.countTokens()];

            for (int i = 0; i < parentInfo.length; i++) {
                parentInfo[i] = new ParentInfo(st.nextToken().trim());
            }
        } else {
            parentInfo = null;
        }

        lastTypeChange = props instanceof ResourceProperties ?
                ((ResourceProperties) props).lastModified() : System.currentTimeMillis();

        // see if this prototype extends (inherits from) any other prototype
        String extendsProto = props.getProperty("_extends");

        if (extendsProto != null) {
            parentMapping = app.getDbMapping(extendsProto);
            if (parentMapping == null) {
                app.logError("Parent mapping for prototype " + typename +
                             " does not exist: " + extendsProto);
            } else {
                if (parentMapping.needsUpdate()) {
                    parentMapping.update();
                }
                // if tableName or DbSource are inherited from the parent mapping
                // set them to null so we are aware of the fact.
                if (tableName != null &&
                        tableName.equals(parentMapping.getTableName())) {
                    tableName = null;
                }
                if (dbSourceName != null &&
                        dbSourceName.equals(parentMapping.getDbSourceName())) {
                    dbSourceName = null;
                    dbSource = null;
                }
            }
        } else {
            parentMapping = null;
        }

        if (inheritsStorage() && getPrototypeField() == null) {
            app.logError("No _prototype mapping in extended prototype " + typename);
            app.logError("Objects fetched from db will have base prototype " + extendsProto);
        }

        // check if there is an extension-id specified inside the type.properties
        extensionId = props.getProperty("_extensionId", typename);
        registerExtension(extensionId, typename);

        // set the parent prototype in the corresponding Prototype object!
        // this was previously done by TypeManager, but we need to do it
        // ourself because DbMapping.update() may be called by other code than
        // the TypeManager.
        if (typename != null &&
                !"global".equalsIgnoreCase(typename) &&
                !"hopobject".equalsIgnoreCase(typename)) {
            Prototype proto = app.getPrototypeByName(typename);
            if (proto != null) {
                if (extendsProto != null) {
                    proto.setParentPrototype(app.getPrototypeByName(extendsProto));
                } else if (!app.isJavaPrototype(typename)) {
                    proto.setParentPrototype(app.getPrototypeByName("hopobject"));
                }
            }
        }

        // null the cached columns and select string
        columns = null;
        columnMap.clear();
        selectString = insertString = updateString = null;

        HashMap p2d = new HashMap();
        HashMap d2p = new HashMap();
        ArrayList joinList = new ArrayList();

        for (Iterator it = props.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry =  (Map.Entry) it.next();

            try {
                String propName = (String) entry.getKey();

                // ignore internal properties (starting with "_") and sub-options (containing a ".")
                if (!propName.startsWith("_") && propName.indexOf(".") < 0) {
                    Object propValue = entry.getValue();
                    propName = app.correctPropertyName(propName);

                    // check if a relation for this propery already exists. If so, reuse it
                    Relation rel = (Relation) prop2db.get(propName);

                    if (rel == null) {
                        rel = new Relation(propName, this);
                    }

                    rel.update(propValue, getSubProperties(propName));
                    p2d.put(propName, rel);

                    if ((rel.columnName != null) && rel.isPrimitiveOrReference()) {
                        Relation old = (Relation) d2p.put(rel.columnName.toLowerCase(), rel);
                        // check if we're overwriting another relation
                        // if so, primitive relations get precendence to references
                        if (old != null) {
                            if (rel.isPrimitive() && old.isPrimitive()) {
                                app.logEvent("Duplicate mapping for " + typename + "." + rel.columnName);
                            } else if (rel.isReference() && old.isPrimitive()) {
                                // if a column is used both in a primitive and a reference mapping,
                                // use primitive mapping as primary one and mark reference as
                                // complex so it will be fetched separately
                                d2p.put(old.columnName.toLowerCase(), old);
                                rel.reftype = Relation.COMPLEX_REFERENCE;
                            } else if (rel.isPrimitive() && old.isReference()) {
                                old.reftype = Relation.COMPLEX_REFERENCE;
                            }
                        }
                    }

                    // check if a reference is aggressively fetched
                    if (rel.aggressiveLoading &&
                            (rel.isReference() || rel.isComplexReference())) {
                        joinList.add(rel);
                    }

                    // app.logEvent ("Mapping "+propName+" -> "+dbField);
                }
            } catch (Exception x) {
                app.logEvent("Error in type.properties: " + x.getMessage());
            }
        }

        prop2db = p2d;
        db2prop = d2p;

        joins = new Relation[joinList.size()];
        joins = (Relation[]) joinList.toArray(joins);

        Object subnodeMapping = props.get("_children");

        if (subnodeMapping != null) {
            try {
                // check if subnode relation already exists. If so, reuse it
                if (subRelation == null) {
                    subRelation = new Relation("_children", this);
                }

                subRelation.update(subnodeMapping, getSubProperties("_children"));

                // if subnodes are accessed via access name or group name,
                // the subnode relation is also the property relation.
                if ((subRelation.accessName != null) || (subRelation.groupby != null)) {
                    propRelation = subRelation;
                } else {
                    propRelation = null;
                }
            } catch (Exception x) {
                app.logEvent("Error reading _subnodes relation for " + typename + ": " +
                             x.getMessage());

                // subRelation = null;
            }
        } else {
            subRelation = propRelation = null;
        }

        if (groupbyMapping != null) {
            initGroupbyMapping();
            groupbyMapping.lastTypeChange = this.lastTypeChange;
        }
    }

    /**
     * Add the given extensionId and the coresponding prototypename
     * to extensionMap for later lookup.
     * @param extID the id mapping to the prototypename recogniced by helma
     * @param extName the name of the extending prototype
     */
    private void registerExtension(String extID, String extName) {
        // lazy initialization of extensionMap
        if (extID == null) {
            return;
        }
        if (extensionMap == null) {
            extensionMap = new ResourceProperties();
            extensionMap.setIgnoreCase(true);
        } else if (extensionMap.containsValue(extName)) {
            // remove any preexisting mapping for the given childmapping
            extensionMap.values().remove(extName);
        }
        extensionMap.setProperty(extID, extName);
        if (inheritsStorage()) {
            parentMapping.registerExtension(extID, extName);
        }
    }

    /**
     * Returns the Set of Prototypes extending this prototype
     * @return the Set of Prototypes extending this prototype
     */
    public String[] getExtensions() {
        return extensionMap == null
                ? new String[] { extensionId }
                : (String[]) extensionMap.keySet().toArray(new String[0]);
    }
    
    /**
     * Looks up the prototype name identified by the given id, returing
     * our own type name if it can't be resolved
     * @param id the id specified for the prototype
     * @return the name of the extending prototype
     */
    public String getPrototypeName(String id) {
        if (inheritsStorage()) {
            return parentMapping.getPrototypeName(id);
        }
        // fallback to base-prototype if the proto isn't recogniced
        if (id == null) {
            return typename;
        }
        return extensionMap.getProperty(id, typename);
    }

    /**
     * get the id-value of this extension
     */
    public String getExtensionId() {
        return extensionId;
    }

    /**
     * Method in interface Updatable.
     */
    public void remove() {
        // do nothing, removing of type properties is not implemented.
    }

    /**
     * Get a JDBC connection for this DbMapping.
     */
    public Connection getConnection() throws ClassNotFoundException, SQLException {
        if (dbSourceName == null) {
            if (parentMapping != null) {
                return parentMapping.getConnection();
            } else {
                throw new SQLException("Tried to get Connection from non-relational embedded data source.");
            }
        }

        if (tableName == null) {
            throw new SQLException("Invalid DbMapping, _table not specified: " + this);
        }

        // if dbSource was previously not available, check again
        if (dbSource == null) {
            dbSource = app.getDbSource(dbSourceName);
        }

        if (dbSource == null) {
            throw new SQLException("Datasource not defined or unable to load driver: " + dbSourceName + ".");
        }

        return dbSource.getConnection();
    }

    /**
     * Get the DbSource object for this DbMapping. The DbSource describes a JDBC
     * data source including URL, JDBC driver, username and password.
     */
    public DbSource getDbSource() {
        if (dbSource == null) {
            if (dbSourceName != null) {
                dbSource = app.getDbSource(dbSourceName);
            } else if (parentMapping != null) {
                return parentMapping.getDbSource();
            }
        }

        return dbSource;
    }

    /**
     * Get the dbsource name used for this type mapping.
     */
    public String getDbSourceName() {
        if ((dbSourceName == null) && (parentMapping != null)) {
            return parentMapping.getDbSourceName();
        }

        return dbSourceName;
    }

    /**
     * Get the table name used for this type mapping.
     */
    public String getTableName() {
        if ((tableName == null) && (parentMapping != null)) {
            return parentMapping.getTableName();
        }

        return tableName;
    }

    /**
     * Get the application this DbMapping belongs to.
     */
    public Application getApplication() {
        return app;
    }

    /**
     * Get the name of this mapping's application
     */
    public String getAppName() {
        return app.getName();
    }

    /**
     * Get the name of the object type this DbMapping belongs to.
     */
    public String getTypeName() {
        return typename;
    }

    /**
     * Get the name of this type's parent type, if any.
     */
    public String getExtends() {
        return parentMapping == null ? null : parentMapping.getTypeName();
    }

    /**
     * Get the primary key column name for objects using this mapping.
     */
    public String getIDField() {
        if ((idField == null) && (parentMapping != null)) {
            return parentMapping.getIDField();
        }

        return (idField == null) ? "ID" : idField;
    }

    /**
     * Get the column used for (internal) names of objects of this type.
     */
    public String getNameField() {
        if ((nameField == null) && (parentMapping != null)) {
            return parentMapping.getNameField();
        }

        return nameField;
    }

    /**
     * Get the column used for names of prototype.
     */
    public String getPrototypeField() {
        if ((protoField == null) && (parentMapping != null)) {
            return parentMapping.getPrototypeField();
        }

        return protoField;
    }

    /**
     * Translate a database column name to an object property name according to this mapping.
     */
    public String columnNameToProperty(String columnName) {
        if (columnName == null) {
            return null;
        }

        // SEMIHACK: If columnName is a function call, try to extract actual
        // column name from it
        int open = columnName.indexOf('(');
        int close = columnName.indexOf(')');
        if (open > -1 && close > open) {
            columnName = columnName.substring(open + 1, close);
        }

        return _columnNameToProperty(columnName.toLowerCase());
    }

    private String _columnNameToProperty(final String columnName) {
        Relation rel = (Relation) db2prop.get(columnName);

        if ((rel == null) && (parentMapping != null)) {
            return parentMapping._columnNameToProperty(columnName);
        }

        if ((rel != null) && rel.isPrimitiveOrReference()) {
            return rel.propName;
        }

        return null;
    }

    /**
     * Translate an object property name to a database column name according
     * to this mapping. If no mapping is found, the property name is returned,
     * assuming property and column names are equal.
     */
    public String propertyToColumnName(String propName) {
        if (propName == null) {
            return null;
        }

        return _propertyToColumnName(propName);
    }

    private String _propertyToColumnName(final String propName) {
        Relation rel = (Relation) prop2db.get(app.correctPropertyName(propName));

        if ((rel == null) && (parentMapping != null)) {
            return parentMapping._propertyToColumnName(propName);
        }

        if ((rel != null) && (rel.isPrimitiveOrReference())) {
            return rel.columnName;
        }

        return null;
    }

    /**
     * Translate a database column name to an object property name according to this mapping.
     */
    public Relation columnNameToRelation(String columnName) {
        if (columnName == null) {
            return null;
        }

        return _columnNameToRelation(columnName.toLowerCase());
    }

    private Relation _columnNameToRelation(final String columnName) {
        Relation rel = (Relation) db2prop.get(columnName);

        if ((rel == null) && (parentMapping != null)) {
            return parentMapping._columnNameToRelation(columnName);
        }

        return rel;
    }

    /**
     * Translate an object property name to a database column name according to this mapping.
     */
    public Relation propertyToRelation(String propName) {
        if (propName == null) {
            return null;
        }

        return _propertyToRelation(propName);
    }

    private Relation _propertyToRelation(String propName) {
        Relation rel = (Relation) prop2db.get(app.correctPropertyName(propName));

        if ((rel == null) && (parentMapping != null)) {
            return parentMapping._propertyToRelation(propName);
        }

        return rel;
    }

    /**
     * @return the parent info as unparsed string.
     */
    public String getParentSetting() {
        if ((parentSetting == null) && (parentMapping != null)) {
            return parentMapping.getParentSetting();
        }
        return parentSetting;
    }

    /**
     * @return the parent info array, which tells an object of this type how to
     * determine its parent object.
     */
    public synchronized ParentInfo[] getParentInfo() {
        if ((parentInfo == null) && (parentMapping != null)) {
            return parentMapping.getParentInfo();
        }

        return parentInfo;
    }

    /**
     *
     *
     * @return ...
     */
    public DbMapping getSubnodeMapping() {
        if (subRelation != null) {
            return subRelation.otherType;
        }

        if (parentMapping != null) {
            return parentMapping.getSubnodeMapping();
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
    public DbMapping getPropertyMapping(String propname) {
        Relation rel = getPropertyRelation(propname);

        if (rel != null) {
            // if this is a virtual node, it doesn't have a dbmapping
            if (rel.virtual && (rel.prototype == null)) {
                return null;
            } else {
                return rel.otherType;
            }
        }

        return null;
    }

    /**
     * If subnodes are grouped by one of their properties, return the
     * db-mapping with the right relations to create the group-by nodes
     */
    public synchronized DbMapping getGroupbyMapping() {
        if (subRelation == null && parentMapping != null) {
            return parentMapping.getGroupbyMapping();
        } else if (subRelation == null || subRelation.groupby == null) {
            return null;
        } else if (groupbyMapping == null) {
            initGroupbyMapping();
        }

        return groupbyMapping;
    }

    /**
     * Initialize the dbmapping used for group-by nodes.
     */
    private void initGroupbyMapping() {
        // if a prototype is defined for groupby nodes, use that
        // if mapping doesn' exist or isn't defined, create a new (anonymous internal) one
        groupbyMapping = new DbMapping(app, subRelation.groupbyPrototype);
        groupbyMapping.lastTypeChange = this.lastTypeChange;
        groupbyMapping.isGroup = true;

        // set subnode and property relations
        groupbyMapping.subRelation = subRelation.getGroupbySubnodeRelation();

        if (propRelation != null) {
            groupbyMapping.propRelation = propRelation.getGroupbyPropertyRelation();
        } else {
            groupbyMapping.propRelation = subRelation.getGroupbyPropertyRelation();
        }
    }

    /**
     *
     *
     * @param rel ...
     */
    public void setPropertyRelation(Relation rel) {
        propRelation = rel;
    }

    /**
     *
     *
     * @return ...
     */
    public Relation getSubnodeRelation() {
        if ((subRelation == null) && (parentMapping != null)) {
            return parentMapping.getSubnodeRelation();
        }

        return subRelation;
    }

    /**
     * Return the list of defined property names as String array.
     */
    public String[] getPropertyNames() {
        return (String[]) prop2db.keySet().toArray(new String[prop2db.size()]);
    }

    /**
     *
     *
     * @return ...
     */
    private Relation getPropertyRelation() {
        if ((propRelation == null) && (parentMapping != null)) {
            return parentMapping.getPropertyRelation();
        }

        return propRelation;
    }

    /**
     *
     *
     * @param propname ...
     *
     * @return ...
     */
    public Relation getPropertyRelation(String propname) {
        if (propname == null) {
            return getPropertyRelation();
        }

        // first try finding an exact match for the property name
        Relation rel = getExactPropertyRelation(propname);

        // if not defined, return the generic property mapping
        if (rel == null) {
            rel = getPropertyRelation();
        }

        return rel;
    }

    /**
     *
     *
     * @param propname ...
     *
     * @return ...
     */
    public Relation getExactPropertyRelation(String propname) {
        if (propname == null) {
            return null;
        }

        Relation rel = (Relation) prop2db.get(app.correctPropertyName(propname));

        if ((rel == null) && (parentMapping != null)) {
            rel = parentMapping.getExactPropertyRelation(propname);
        }

        return rel;
    }

    /**
     *
     *
     * @return ...
     */
    public String getSubnodeGroupby() {
        if ((subRelation == null) && (parentMapping != null)) {
            return parentMapping.getSubnodeGroupby();
        }

        return (subRelation == null) ? null : subRelation.groupby;
    }

    /**
     *
     *
     * @return ...
     */
    public String getIDgen() {
        if ((idgen == null) && (parentMapping != null)) {
            return parentMapping.getIDgen();
        }

        return idgen;
    }

    /**
     *
     *
     * @return ...
     */
    public WrappedNodeManager getWrappedNodeManager() {
        if (app == null) {
            throw new RuntimeException("Can't get node manager from internal db mapping");
        }

        return app.getWrappedNodeManager();
    }

    /**
     *  Tell whether this data mapping maps to a relational database table. This returns true
     *  if a datasource is specified, even if it is not a valid one. Otherwise, objects with invalid
     *  mappings would be stored in the embedded db instead of an error being thrown, which is
     *  not what we want.
     */
    public boolean isRelational() {
        return dbSourceName != null || (parentMapping != null && parentMapping.isRelational());
    }

    /**
     * Return an array of DbColumns for the relational table mapped by this DbMapping.
     */
    public synchronized DbColumn[] getColumns()
                                       throws ClassNotFoundException, SQLException {
        if (!isRelational()) {
            throw new SQLException("Can't get columns for non-relational data mapping " + this);
        }

        // Use local variable cols to avoid synchronization (schema may be nulled elsewhere)
        if (columns == null) {
            // we do two things here: set the SQL type on the Relation mappings
            // and build a string of column names.
            Connection con = getConnection();
            Statement stmt = con.createStatement();
            String table = getTableName();

            if (table == null) {
                throw new SQLException("Table name is null in getColumns() for " + this);
            }

            ResultSet rs = stmt.executeQuery(new StringBuffer("SELECT * FROM ").append(table)
                                                                               .append(" WHERE 1 = 0")
                                                                               .toString());

            if (rs == null) {
                throw new SQLException("Error retrieving columns for " + this);
            }

            ResultSetMetaData meta = rs.getMetaData();

            // ok, we have the meta data, now loop through mapping...
            int ncols = meta.getColumnCount();
            ArrayList list = new ArrayList(ncols);

            for (int i = 0; i < ncols; i++) {
                String colName = meta.getColumnName(i + 1);
                Relation rel = columnNameToRelation(colName);

                DbColumn col = new DbColumn(colName, meta.getColumnType(i + 1), rel, this);
                list.add(col);
            }
            columns = (DbColumn[]) list.toArray(new DbColumn[list.size()]);
        }

        return columns;
    }

    /**
     *  Return the array of relations that are fetched with objects of this type.
     */
    public Relation[] getJoins() {
        return joins;
    }

    /**
     *
     *
     * @param columnName ...
     *
     * @return ...
     *
     * @throws ClassNotFoundException ...
     * @throws SQLException ...
     */
    public DbColumn getColumn(String columnName)
                       throws ClassNotFoundException, SQLException {
        DbColumn col = (DbColumn) columnMap.get(columnName);
        if (col == null) {
            DbColumn[] cols = columns;
            if (cols == null) {
                cols = getColumns();
            }
            for (int i = 0; i < cols.length; i++) {
                if (columnName.equalsIgnoreCase(cols[i].getName())) {
                    col = cols[i];
                    break;
                }
            }
            columnMap.put(columnName, col);
        }
        return col;
    }

    /**
     *  Get a StringBuffer initialized to the first part of the select statement
     *  for objects defined by this DbMapping
     *
     * @param rel the Relation we use to select. Currently only used for optimizer hints.
     *            Is null if selecting by primary key.
     * @return the StringBuffer containing the first part of the select query
     */
    public StringBuffer getSelect(Relation rel) {
        // assign to local variable first so we are thread safe
        // (selectString may be reset by other threads)
        String sel = selectString;
        boolean isOracle = isOracle();

        if (rel == null && sel != null) {
            return new StringBuffer(sel);
        }

        StringBuffer s = new StringBuffer("SELECT ");

        if (rel != null && rel.queryHints != null) {
            s.append(rel.queryHints).append(" ");
        }

        String table = getTableName();

        // all columns from the main table
        s.append(table);
        s.append(".*");

        for (int i = 0; i < joins.length; i++) {
            if (!joins[i].otherType.isRelational()) {
                continue;
            }
            s.append(", ");
            s.append(Relation.JOIN_PREFIX);
            s.append(joins[i].propName);
            s.append(".*");
        }

        s.append(" FROM ");

        s.append(table);

        if (rel != null) {
            rel.appendAdditionalTables(s);
        }

        s.append(" ");

        for (int i = 0; i < joins.length; i++) {
            if (!joins[i].otherType.isRelational()) {
                continue;
            }
            if (isOracle) {
                // generate an old-style oracle left join - see
                // http://www.praetoriate.com/oracle_tips_outer_joins.htm
                s.append(", ");
                s.append(joins[i].otherType.getTableName());
                s.append(" ");
                s.append(Relation.JOIN_PREFIX);
                s.append(joins[i].propName);
                s.append(" ");
            } else {
                s.append("LEFT OUTER JOIN ");
                s.append(joins[i].otherType.getTableName());
                s.append(" ");
                s.append(Relation.JOIN_PREFIX);
                s.append(joins[i].propName);
                s.append(" ON ");
                joins[i].renderJoinConstraints(s, isOracle);
            }
        }

        // cache rendered string for later calls, but only if it wasn't
        // built for a particular Relation
        if (rel == null) {
            selectString = s.toString();
        }

        return s;
    }

    /**
     *
     *
     * @return ...
     */
    public String getInsert() throws ClassNotFoundException, SQLException {
        String ins = insertString;

        if (ins != null) {
            return ins;
        }

        StringBuffer b1 = new StringBuffer("INSERT INTO ");
        StringBuffer b2 = new StringBuffer(" ) VALUES ( ");
        b1.append(getTableName());
        b1.append(" ( ");

        DbColumn[] cols = getColumns();
        boolean needsComma = false;
        
        for (int i = 0; i < cols.length; i++) {
            if (cols[i].isMapped()) {
                if (needsComma) {
                    b1.append(", ");
                    b2.append(", ");
                }
                b1.append(cols[i].getName());
                b2.append("?");
                needsComma = true;
            }
        }

        b1.append(b2.toString());
        b1.append(" )");

        // cache rendered string for later calls.
        ins = insertString = b1.toString();

        return ins;
    }


    /**
     *
     *
     * @return ...
     */
    public StringBuffer getUpdate() {
        String upd = updateString;

        if (upd != null) {
            return new StringBuffer(upd);
        }

        StringBuffer s = new StringBuffer("UPDATE ");

        s.append(getTableName());
        s.append(" SET ");

        // cache rendered string for later calls.
        updateString = s.toString();

        return s;
    }

    /**
     *  Return true if values for the column identified by the parameter need
     *  to be quoted in SQL queries.
     */
    public boolean needsQuotes(String columnName) throws SQLException, ClassNotFoundException {
        if ((tableName == null) && (parentMapping != null)) {
            return parentMapping.needsQuotes(columnName);
        }
        DbColumn col = getColumn(columnName);
        // This is not a mapped column. In case of doubt, add quotes.
        if (col == null) {
            return true;
        } else {
            return col.needsQuotes();
        }
    }

    /**
     * Add constraints to select query string to join object references
     */
    public void addJoinConstraints(StringBuffer s, String pre) {
        boolean isOracle = isOracle();
        String prefix = pre;

        if (!isOracle) {
            // constraints have already been rendered by getSelect()
            return;
        }

        for (int i = 0; i < joins.length; i++) {
            if (!joins[i].otherType.isRelational()) {
                continue;
            }
            s.append(prefix);
            joins[i].renderJoinConstraints(s, isOracle);
            prefix = " AND ";
        }
    }

    /**
     * Is the database behind this an Oracle db?
     *
     * @return true if the dbsource is using an oracle JDBC driver
     */
    public boolean isOracle() {
        if (dbSource != null) {
            return dbSource.isOracle();
        }
        if (parentMapping != null) {
            return parentMapping.isOracle();
        }
        return false;
    }

    /**
     * Is the database behind this a MySQL db?
     *
     * @return true if the dbsource is using a MySQL JDBC driver
     */
    public boolean isMySQL() {
        if (dbSource != null) {
            return dbSource.isMySQL();
        }
        if (parentMapping != null) {
            return parentMapping.isMySQL();
        }
        return false;
    }

    /**
     * Is the database behind this a PostgreSQL db?
     *
     * @return true if the dbsource is using a PostgreSQL JDBC driver
     */
    public boolean isPostgreSQL() {
        if (dbSource != null) {
            return dbSource.isPostgreSQL();
        }
        if (parentMapping != null) {
            return parentMapping.isPostgreSQL();
        }
        return false;
    }

    /**
     * Is the database behind this a H2 db?
     *
     * @return true if the dbsource is using a H2 JDBC driver
     */
    public boolean isH2() {
        if (dbSource != null) {
            return dbSource.isH2();
        }
        if (parentMapping != null) {
            return parentMapping.isH2();
        }
        return false;
    }

    /**
     * Return a string representation for this DbMapping
     *
     * @return a string representation
     */
    public String toString() {
        if (typename == null) {
            return "[unspecified internal DbMapping]";
        } else {
            return ("[" + app.getName() + "." + typename + "]");
        }
    }

    /**
     * Get the last time something changed in the Mapping
     *
     * @return time of last mapping change
     */
    public long getLastTypeChange() {
        return lastTypeChange;
    }

    /**
     * Get the last time something changed in our data
     *
     * @return time of last data change
     */
    public long getLastDataChange() {
        // refer to parent mapping if it uses the same db/table
        if (inheritsStorage()) {
            return parentMapping.getLastDataChange();
        } else {
            return lastDataChange;
        }
    }

    /**
     * Set the last time something changed in the data, propagating the event
     * to mappings that depend on us through an additionalTables switch.
     */
    public void setLastDataChange() {
        // forward data change timestamp to storage-compatible parent mapping
        if (inheritsStorage()) {
            parentMapping.setLastDataChange();
        } else {
            lastDataChange += 1;
            // propagate data change timestamp to mappings that depend on us
            if (!dependentMappings.isEmpty()) {
                Iterator it = dependentMappings.iterator();
                while(it.hasNext()) {
                    DbMapping dbmap = (DbMapping) it.next();
                    dbmap.setIndirectDataChange();
                }
            }
        }
    }

    /**
     * Set the last time something changed in the data. This is already an indirect
     * data change triggered by a mapping we depend on, so we don't propagate it to
     * mappings that depend on us through an additionalTables switch.
     */
    protected void setIndirectDataChange() {
        // forward data change timestamp to storage-compatible parent mapping
        if (inheritsStorage()) {
            parentMapping.setIndirectDataChange();
        } else {
            lastDataChange += 1;
        }
    }

    /**
     * Helper method to generate a new ID. This is only used in the special case
     * when using the select(max) method and the underlying table is still empty.
     *
     * @param dbmax the maximum value already stored in db
     * @return a new and hopefully unique id
     */
    protected synchronized long getNewID(long dbmax) {
        // refer to parent mapping if it uses the same db/table
        if (inheritsStorage()) {
            return parentMapping.getNewID(dbmax);
        } else {
            lastID = Math.max(dbmax + 1, lastID + 1);
            return lastID;
        }
    }

    /**
     * Return an enumeration of all properties defined by this db mapping.
     *
     * @return the property enumeration
     */
    public Enumeration getPropertyEnumeration() {
        HashSet set = new HashSet();

        collectPropertyNames(set);

        final Iterator it = set.iterator();

        return new Enumeration() {
                public boolean hasMoreElements() {
                    return it.hasNext();
                }

                public Object nextElement() {
                    return it.next();
                }
            };
    }

    /**
     * Collect a set of all properties defined by this db mapping
     *
     * @param basket the set to put properties into
     */
    private void collectPropertyNames(HashSet basket) {
        // fetch propnames from parent mapping first, than add our own.
        if (parentMapping != null) {
            parentMapping.collectPropertyNames(basket);
        }

        if (!prop2db.isEmpty()) {
            basket.addAll(prop2db.keySet());
        }
    }

    /**
     * Return the name of the prototype which specifies the storage location
     * (dbsource + tablename) for this type, or null if it is stored in the embedded
     * db.
     */
    public String getStorageTypeName() {
        if (inheritsStorage()) {
            return parentMapping.getStorageTypeName();
        }
        return (getDbSourceName() == null) ? null : typename;
    }

    /**
     * Check whether this DbMapping inherits its storage location from its
     * parent mapping. The raison d'etre for this is that we need to detect
     * inherited storage even if the dbsource and table are explicitly set
     * in the extended mapping.
     *
     * @return true if this mapping shares its parent mapping storage
     */
    protected boolean inheritsStorage() {
        // note: tableName and dbSourceName are nulled out in update() if they
        // are inherited from the parent mapping. This way we know that
        // storage is not inherited if either of them is not null.
        return isRelational() && parentMapping != null
                && tableName == null && dbSourceName == null;
    }

    /**
     * Static utility method to check whether two DbMappings use the same storage.
     *
     * @return true if both use the embedded database or the same relational table.
     */
    public static boolean areStorageCompatible(DbMapping dbm1, DbMapping dbm2) {
        if (dbm1 == null)
            return dbm2 == null || !dbm2.isRelational();
        return dbm1.isStorageCompatible(dbm2);        
    }

    /**
     * Tell if this DbMapping uses the same storage as the given DbMapping.
     *
     * @return true if both use the embedded database or the same relational table.
     */
    public boolean isStorageCompatible(DbMapping other) {
        if (other == null) {
            return !isRelational();
        } else if (other == this) {
            return true;
        } else if (isRelational()) {
            return getTableName().equals(other.getTableName()) &&
                   getDbSource().equals(other.getDbSource());
        }

        return !other.isRelational();
    }

    /**
     *  Return true if this db mapping represents the prototype indicated
     *  by the string argument, either itself or via one of its parent prototypes.
     */
    public boolean isInstanceOf(String other) {
        if ((typename != null) && typename.equals(other)) {
            return true;
        }

        DbMapping p = parentMapping;

        while (p != null) {
            if ((p.typename != null) && p.typename.equals(other)) {
                return true;
            }

            p = p.parentMapping;
        }

        return false;
    }

    /**
     * Get the mapping we inherit from, or null
     *
     * @return the parent DbMapping, or null
     */
    public DbMapping getParentMapping() {
        return parentMapping;
    }

    /**
     * Get our ResourceProperties
     *
     * @return our properties
     */
    public Properties getProperties() {
        return props;
    }

    public Properties getSubProperties(String prefix) {
        if (props.get(prefix) instanceof Properties) {
            return (Properties) props.get(prefix);
        } else if (props instanceof ResourceProperties) {
            return ((ResourceProperties) props).getSubProperties(prefix + ".");
        } else {
            Properties subprops = new Properties();
            prefix = prefix + ".";
            Iterator it = props.entrySet().iterator();
            int prefixLength = prefix.length();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                String key = entry.getKey().toString();
                if (key.regionMatches(false, 0, prefix, 0, prefixLength)) {
                    subprops.put(key.substring(prefixLength), entry.getValue());
                }
            }
            return subprops;
        }
    }

    /**
     * Register a DbMapping that depends on this DbMapping, so that collections of other mapping
     * should be reloaded if data on this mapping is updated.
     *
     * @param dbmap the DbMapping that depends on us
     */
    protected void addDependency(DbMapping dbmap) {
        this.dependentMappings.add(dbmap);
    }
    
    /**
     * Append a sql-condition for the given column which must have
     * one of the values contained inside the given Set to the given
     * StringBuffer. 
     * @param q the StringBuffer to append to
     * @param column the column which must match one of the values
     * @param values the list of values
     * @throws SQLException
     */
    protected void appendCondition(StringBuffer q, String column, String[] values)
            throws SQLException, ClassNotFoundException {
        if (values.length == 1) {
            appendCondition(q, column, values[0]);
            return;
        }
        if (column.indexOf('(') == -1 && column.indexOf('.') == -1) {
            q.append(getTableName()).append(".");
        }
        q.append(column).append(" in (");

        if (needsQuotes(column)) {
            for (int i = 0; i < values.length; i++) {
                if (i > 0)
                    q.append(", ");
                q.append("'").append(escapeString(values[i])).append("'");
            }
        } else {
            for (int i = 0; i < values.length; i++) {
                if (i > 0)
                    q.append(", ");
                q.append(checkNumber(values[i]));
            }
        }
        q.append(")");
    }

    /**
     * Append a sql-condition for the given column which must have
     * the value given to the given StringBuffer. 
     * @param q the StringBuffer to append to
     * @param column the column which must match one of the values
     * @param val the value
     * @throws SQLException
     */
    protected void appendCondition(StringBuffer q, String column, String val)
            throws SQLException, ClassNotFoundException {
        if (column.indexOf('(') == -1 && column.indexOf('.') == -1) {
            q.append(getTableName()).append(".");
        }
        q.append(column).append(" = ");
        
        if (needsQuotes(column)) {
            q.append("'").append(escapeString(val)).append("'");
        } else {
            q.append(checkNumber(val));
        }
    }

    /**
     * a utility method to escape single quotes used for inserting
     * string-values into relational databases.
     * Searches for "'" characters and escapes them by duplicating them (= "''")
     * @param value the string to escape
     * @return the escaped string
     */
    static String escapeString(Object value) {
        String str = value == null ? null : value.toString();        
        if (str == null) {
            return null;
        } else if (str.indexOf('\'') < 0 && str.indexOf('\\') < 0) {
            return str;
        }

        int l = str.length();
        StringBuffer sbuf = new StringBuffer(l + 10);

        for (int i = 0; i < l; i++) {
            char c = str.charAt(i);

            if (c == '\'') {
                sbuf.append("\\'");
            } else if (c == '\\') {
                sbuf.append("\\\\");
            } else {
            	sbuf.append(c);
            }
        }
        return sbuf.toString();
    }

    /**
     * Utility method to check whether the argument is a number literal.
     * @param value a string representing a number literal
     * @return the argument, if it conforms to the number literal syntax
     * @throws IllegalArgumentException if the argument does not represent a number
     */
    static String checkNumber(Object value) throws IllegalArgumentException {
        String str = value == null ? null : value.toString();
        if (str == null) {
            return null;
        } else {
            str = str.trim();
            if (str.matches("(?:\\+|\\-)??\\d+(?:\\.\\d+)??")) {
                return str;
            }
        }
        throw new IllegalArgumentException("Illegal numeric literal: " + str);
    }

    /**
     * Find if this DbMapping describes a virtual node (collection, mountpoint, groupnode)
     * @return true if this instance describes a virtual node.
     */
    public boolean isVirtual() {
        return isVirtual;
    }

    /**
     * Find if this DbMapping describes a group node.
     * @return true if this instance describes a group node.
     */
    public boolean isGroup() {
        return isGroup;
    }

    /**
     * Find whether a node with this DbMapping must be stored in the database.
     * This is true if this mapping defines a non-virtual node, or a virtual
     * node with non-relational child objects.
     * @return true if this node needs to be stored in the db, false otherwise
     */
    public boolean needsPersistence() {
        DbMapping submap = getSubnodeMapping();
        return !isVirtual || (submap != null && !submap.isRelational());
    }
}

