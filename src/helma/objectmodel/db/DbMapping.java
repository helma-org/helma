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
import helma.util.SystemProperties;
import helma.util.Updatable;

import java.sql.*;
import java.util.*;

/**
 * A DbMapping describes how a certain type of  Nodes is to mapped to a
 * relational database table. Basically it consists of a set of JavaScript property-to-
 * Database row bindings which are represented by instances of the Relation class.
 */
public final class DbMapping implements Updatable {
    // DbMappings belong to an application
    protected Application app;

    // prototype name of this mapping
    private String typename;

    // properties from where the mapping is read
    private SystemProperties props;

    // name of data dbSource to which this mapping writes
    private DbSource dbSource;

    // name of datasource
    private String dbSourceName;

    // name of db table
    private String tableName;

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
    // Case insensitive, keys are stored in upper case so 
    // lookups must do a toUpperCase().
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

    // name of parent prototype, if any
    private String extendsProto;

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
    long lastDataChange;

    /**
     * Create an empty DbMapping
     */
    public DbMapping(Application app) {
        this.app = app;
        this.typename = null;

        prop2db = new HashMap();
        db2prop = new HashMap();

        parentInfo = null;

        idField = null;
    }

    /**
     * Create a DbMapping from a type.properties property file
     */
    public DbMapping(Application app, String typename, SystemProperties props) {
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
    }

    /**
     * Tell the type manager whether we need update() to be called
     */
    public boolean needsUpdate() {
        return props.lastModified() != lastTypeChange;
    }


    /**
     * Read the mapping from the Properties. Return true if the properties were changed.
     * The read is split in two, this method and the rewire method. The reason is that in order
     * for rewire to work, all other db mappings must have been initialized and registered.
     */
    public synchronized void update() {
        // read in properties
        tableName = props.getProperty("_table");
        idgen = props.getProperty("_idgen");

        dbSourceName = props.getProperty("_db");

        if (dbSourceName != null) {
            dbSource = app.getDbSource(dbSourceName);

            if (dbSource == null) {
                app.logEvent("*** Data Source for prototype " + typename +
                             " does not exist: " + dbSourceName);
                app.logEvent("*** accessing or storing a " + typename +
                             " object will cause an error.");
            } else if (tableName == null) {
                app.logEvent("*** No table name specified for prototype " + typename);
                app.logEvent("*** accessing or storing a " + typename +
                             " object will cause an error.");

                // mark mapping as invalid by nulling the dbSource field
                dbSource = null;
            }
        }

        // if id field is null, we assume "ID" as default. We don't set it
        // however, so that if null we check the parent prototype first.
        idField = props.getProperty("_id");

        nameField = props.getProperty("_name");

        protoField = props.getProperty("_prototype");

        String parentSpec = props.getProperty("_parent");

        if (parentSpec != null) {
            // comma-separated list of properties to be used as parent
            StringTokenizer st = new StringTokenizer(parentSpec, ",;");

            parentInfo = new ParentInfo[st.countTokens()];

            for (int i = 0; i < parentInfo.length; i++)
                parentInfo[i] = new ParentInfo(st.nextToken().trim());
        } else {
            parentInfo = null;
        }

        lastTypeChange = props.lastModified();

        // see if this prototype extends (inherits from) any other prototype
        extendsProto = props.getProperty("_extends");

        if (extendsProto != null) {
            parentMapping = app.getDbMapping(extendsProto);
            if (parentMapping != null && parentMapping.needsUpdate()) {
                parentMapping.update();
            }
        } else {
            parentMapping = null;
        }

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

        for (Enumeration e = props.keys(); e.hasMoreElements();) {
            String propName = (String) e.nextElement();

            try {
                // ignore internal properties (starting with "_") and sub-options (containing a ".")
                if (!propName.startsWith("_") && (propName.indexOf(".") < 0)) {
                    String dbField = props.getProperty(propName);

                    // check if a relation for this propery already exists. If so, reuse it
                    Relation rel = (Relation) prop2db.get(propName.toLowerCase());

                    if (rel == null) {
                        rel = new Relation(propName, this);
                    }

                    rel.update(dbField, props);

                    // key enumerations from SystemProperties are all lower case, which is why
                    // even though we don't do a toLowerCase() here,
                    // we have to when we lookup things in p2d later.
                    p2d.put(propName, rel);

                    if ((rel.columnName != null) &&
                            ((rel.reftype == Relation.PRIMITIVE) ||
                            (rel.reftype == Relation.REFERENCE))) {
                        Relation old = (Relation) d2p.put(rel.columnName.toUpperCase(), rel);
                        // check if we're overwriting another relation
                        // if so, primitive relations get precendence to references
                        if (old != null) {
                            app.logEvent("*** Duplicate mapping for "+typename+"."+rel.columnName);
                            if (old.reftype == Relation.PRIMITIVE) {
                                d2p.put(old.columnName.toUpperCase(), old);
                            }
                        }
                    }

                    // check if a reference is aggressively fetched
                    if ((rel.reftype == Relation.REFERENCE ||
                             rel.reftype == Relation.COMPLEX_REFERENCE) &&
                             rel.aggressiveLoading) {
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

        String subnodeMapping = props.getProperty("_children");

        if (subnodeMapping != null) {
            try {
                // check if subnode relation already exists. If so, reuse it
                if (subRelation == null) {
                    subRelation = new Relation("_children", this);
                }

                subRelation.update(subnodeMapping, props);

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
            throw new SQLException("Datasource is not defined: " + dbSourceName + ".");
        }

        return dbSource.getConnection();
    }

    /**
     * Get the DbSource object for this DbMapping. The DbSource describes a JDBC
     * data source including URL, JDBC driver, username and password.
     */
    public DbSource getDbSource() {
        if (dbSource == null) {
            if ((tableName != null) && (dbSourceName != null)) {
                dbSource = app.getDbSource(dbSourceName);
            } else if (parentMapping != null) {
                return parentMapping.getDbSource();
            }
        }

        return dbSource;
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
        return extendsProto;
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

        return _columnNameToProperty(columnName.toUpperCase());
    }

    private String _columnNameToProperty(final String columnName) {
        Relation rel = (Relation) db2prop.get(columnName);

        if ((rel == null) && (parentMapping != null)) {
            return parentMapping._columnNameToProperty(columnName);
        }

        if ((rel != null) &&
                ((rel.reftype == Relation.PRIMITIVE) ||
                (rel.reftype == Relation.REFERENCE))) {
            return rel.propName;
        }

        return null;
    }

    /**
     * Translate an object property name to a database column name according to this mapping.
     */
    public String propertyToColumnName(String propName) {
        if (propName == null) {
            return null;
        }

        // FIXME: prop2db stores keys in lower case, because it gets them
        // from a SystemProperties object which converts keys to lower case.
        return _propertyToColumnName(propName.toLowerCase());
    }

    private String _propertyToColumnName(final String propName) {
        Relation rel = (Relation) prop2db.get(propName);

        if ((rel == null) && (parentMapping != null)) {
            return parentMapping._propertyToColumnName(propName);
        }

        if ((rel != null) &&
                ((rel.reftype == Relation.PRIMITIVE) ||
                (rel.reftype == Relation.REFERENCE))) {
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

        return _columnNameToRelation(columnName.toUpperCase());
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

        // FIXME: prop2db stores keys in lower case, because it gets them
        // from a SystemProperties object which converts keys to lower case.
        return _propertyToRelation(propName.toLowerCase());
    }

    private Relation _propertyToRelation(String propName) {
        Relation rel = (Relation) prop2db.get(propName);

        if ((rel == null) && (parentMapping != null)) {
            return parentMapping._propertyToRelation(propName);
        }

        return rel;
    }

    /**
     * This returns the parent info array, which tells an object of this type how to
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
    public DbMapping getExactPropertyMapping(String propname) {
        Relation rel = getExactPropertyRelation(propname);

        return (rel != null) ? rel.otherType : null;
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
        if ((subRelation == null) || (subRelation.groupby == null)) {
            return null;
        }

        if (groupbyMapping == null) {
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
        groupbyMapping = new DbMapping(app);

        // If a mapping is defined, make the internal mapping inherit from
        // the defined named prototype.
        if (subRelation.groupbyPrototype != null) {
            groupbyMapping.parentMapping = app.getDbMapping(subRelation.groupbyPrototype);
        }

        groupbyMapping.subRelation = subRelation.getGroupbySubnodeRelation();

        if (propRelation != null) {
            groupbyMapping.propRelation = propRelation.getGroupbyPropertyRelation();
        } else {
            groupbyMapping.propRelation = subRelation.getGroupbyPropertyRelation();
        }

        groupbyMapping.typename = subRelation.groupbyPrototype;
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

        Relation rel = (Relation) prop2db.get(propname.toLowerCase());

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
        if (dbSourceName != null) {
            return true;
        }

        if (parentMapping != null) {
            return parentMapping.isRelational();
        }

        return false;
    }

    /**
     * Return an array of DbColumns for the relational table mapped by this DbMapping.
     */
    public synchronized DbColumn[] getColumns()
                                       throws ClassNotFoundException, SQLException {
        if (!isRelational()) {
            throw new SQLException("Can't get columns for non-relational data mapping " +
                                   this);
        }

        /* if ((dbSource == null) && (parentMapping != null)) {
            return parentMapping.getColumns();
        } */

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
            columns = new DbColumn[list.size()];
            columns = (DbColumn[]) list.toArray(columns);
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
     *
     * @throws SQLException if the table meta data could not be retrieved
     * @throws ClassNotFoundException if the JDBC driver class was not found
     */
    public StringBuffer getSelect(Relation rel) throws SQLException, ClassNotFoundException {
        // assign to local variable first so we are thread safe
        // (selectString may be reset by other threads)
        String sel = selectString;

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
        s.append(" ");

        for (int i = 0; i < joins.length; i++) {
            if (!joins[i].otherType.isRelational()) {
                continue;
            }
            s.append("LEFT OUTER JOIN ");
            s.append(joins[i].otherType.getTableName());
            s.append(" AS ");
            s.append(Relation.JOIN_PREFIX);
            s.append(joins[i].propName);
            s.append(" ON ");
            joins[i].renderJoinConstraints(s);
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
    public StringBuffer getInsert() {
        String ins = insertString;

        if (ins != null) {
            return new StringBuffer(ins);
        }

        StringBuffer s = new StringBuffer("INSERT INTO ");

        s.append(getTableName());
        s.append(" ( ");
        s.append(getIDField());

        // cache rendered string for later calls.
        insertString = s.toString();

        return s;
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
    public boolean needsQuotes(String columnName) throws SQLException {
        if ((tableName == null) && (parentMapping != null)) {
            return parentMapping.needsQuotes(columnName);
        }

        try {
            DbColumn col = getColumn(columnName);

            // This is not a mapped column. In case of doubt, add quotes.
            if (col == null) {
                return true;
            }

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
            throw new SQLException(x.getMessage());
        }
    }

    /**
     *
     *
     * @return ...
     */
    public String toString() {
        if (typename == null) {
            return "[unspecified internal DbMapping]";
        } else {
            return ("[" + app.getName() + "." + typename + "]");
        }
    }

    /**
     *
     *
     * @return ...
     */
    public long getLastTypeChange() {
        return lastTypeChange;
    }

    /**
     *
     *
     * @return ...
     */
    public long getLastDataChange() {
        return lastDataChange;
    }

    /**
     *
     */
    public void setLastDataChange(long t) {
        lastDataChange = t;

        // propagate data change timestamp to parent mapping
        if ((parentMapping != null) && (dbSource == null)) {
            parentMapping.setLastDataChange(t);
        }
    }

    /**
     *
     *
     * @param dbmax ...
     *
     * @return ...
     */
    public synchronized long getNewID(long dbmax) {
        if ((parentMapping != null) && (dbSource == null)) {
            return parentMapping.getNewID(dbmax);
        }

        lastID = Math.max(dbmax + 1, lastID + 1);

        return lastID;
    }

    /**
     *
     *
     * @return ...
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
     *  Return the name of the prototype which specifies the storage location
     * (dbsource + tablename) for this type, or null if it is stored in the embedded
     * db.
     */
    public String getStorageTypeName() {
        if ((tableName == null) && (parentMapping != null)) {
            return parentMapping.getStorageTypeName();
        }

        return (dbSourceName == null) ? null : typename;
    }

    /**
     *  Tell if another DbMapping is storage-compatible to this one, i.e. it is stored in the same table or
     *  embedded database.
     */
    public boolean isStorageCompatible(DbMapping other) {
        if (other == null) {
            return !isRelational();
        }

        if (isRelational()) {
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
     *
     *
     * @return ...
     */
    public DbMapping getParentMapping() {
        return parentMapping;
    }

    /**
     *
     *
     * @return ...
     */
    public SystemProperties getProperties() {
        return props;
    }
}
