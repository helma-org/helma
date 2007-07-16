/*
 * Helma License Notice
 *
 * The contents of this file are subject to the Helma License
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://adele.helma.org/download/helma/license.txt
 *
 * Copyright 1998-2006 Helma Software. All Rights Reserved.
 *
 * $RCSfile: Database.js,v $
 * $Author: michi $
 * $Revision: 1.6 $
 * $Date: 2007/06/05 13:20:28 $
 */

/**
 * @fileoverview Properties and methods of the helma.Database prototype.
 */

if (!global.helma) {
    global.helma = {};
}

/**
 * Constructor for Database objects, providing access through relational
 * databases through JDBC. It is usually simpler to use one of the factory
 * methods {@link #createInstance} or {@link #getInstance}.
 * @class <p>This class provides access to  a relational database through JDBC.
 * There are two convenient ways to create instances of this class.</p>
 *
 * <p>The first is to use {@link #getInstance helma.Database.getInstance()}
 * to obtain a connection to a DB that is defined in the application's
 * db.properties and managed by Helma. The second way is to define and create
 * a database connection locally using
 * {@link #createInstance helma.Database.createInstance()} and passing it
 * all necessary parameters.</p>
 *
 * <p>This class provides two ways of interaction:
 * The {@link #query} method allows to issue SQL queries, returning a result set.
 * The {@link #execute} provides a way to issue SQL statements that do not
 * return a result set.</p>
 *
 * <p>Database connections allocated by this class are be managed and eventually
 * disposed by Helma.</p>
 *
 * @param {DbSource} source instance of a helma.objectmodel.db.DbSource
 * @constructor
 */
helma.Database = function(source) {
    var Types = java.sql.Types;
    var DbSource = Packages.helma.objectmodel.db.DbSource;

    if (typeof(source) == "string")
        source = app.getDbSource(source);
    if (!(source instanceof DbSource))
        throw "helma.Database requires a helma.objectmodel.db.DbSource argument";

    var connection = source.getConnection();

    /**
     * Get the java.sql.Connection for this Database instance. This can be used
     * to operate on the connection directly, without going through the helma.Database
     * class.
     * @return {java.sql.Connection} the JDBC connection
     */
    this.getConnection = function() {
        return connection;
    };

    /**
     * Returns the lower case name of the underlying database product.
     * @return {String} the name of the DB product
     */
    this.getProductName = function() {
        return connection.getMetaData().getDatabaseProductName().toLowerCase();
    };

    /**
     * Returns true if this is an Oracle database.
     * @return {boolean} true if this is an Oracle database.
     */
    this.isOracle = function() {
        return source.isOracle();
    };

    /**
     * Returns true if this is a MySQL database.
     * @return {boolean} true if this is an MySQL database. 
     */
    this.isMySql = function() {
        return source.isMySQL();
    };

    /**
     * Returns true if this is a PostgreSQL database.
     * @return {boolean} true if this is a PostgreSQL database.
     */
    this.isPostgreSql = function() {
        return source.isPostgreSQL();
    };

    /**
     * Executes the given SQL statement. The result set is returned
     * as JavaScript Array containing a JavaScript Object for each result.
     * @param {String} sql an SQL query statement
     * @return {Array} an Array containing the result set
     */
    this.query = function(sql) {
        connection.setReadOnly(true);
        var statement = connection.createStatement();
        var resultSet = statement.executeQuery(sql);
        var metaData = resultSet.getMetaData();
        var max = metaData.getColumnCount();
        var types = [];
        for (var i=1; i <= max; i++) {
            types[i] = metaData.getColumnType(i);
        }
        var result = [];
        while (resultSet.next()) {
            var row = {}
            for (var i=1; i<=max; i+=1) {
                switch (types[i]) {
                    case Types.BIT:
                    case Types.BOOLEAN:
                        row[metaData.getColumnLabel(i)] = resultSet.getBoolean(i);
                        break;
                    case Types.TINYINT:
                    case Types.BIGINT:
                    case Types.SMALLINT:
                    case Types.INTEGER:
                        row[metaData.getColumnLabel(i)] = resultSet.getLong(i);
                        break;
                    case Types.REAL:
                    case Types.FLOAT:
                    case Types.DOUBLE:
                    case Types.DECIMAL:
                    case Types.NUMERIC:
                        row[metaData.getColumnLabel(i)] = resultSet.getDouble(i);
                        break;
                    case Types.VARBINARY:
                    case Types.BINARY:
                    case Types.LONGVARBINARY:
                    case Types.LONGVARCHAR:
                    case Types.CHAR:
                    case Types.VARCHAR:
                    case Types.CLOB:
                    case Types.OTHER:
                        row[metaData.getColumnLabel(i)] = resultSet.getString(i);
                        break;
                    case Types.DATE:
                    case Types.TIME:
                    case Types.TIMESTAMP:
                        row[metaData.getColumnLabel(i)] = resultSet.getTimestamp(i);
                        break;
                    case Types.NULL:
                        row[metaData.getColumnLabel(i)] = null;
                        break;
                    default:
                        row[metaData.getColumnLabel(i)] = resultSet.getString(i);
                        break;
                }
            }
            result[result.length] = row;
        }
        try {
            statement.close();
            resultSet.close();
        } catch (error) {
            // ignore
        }
        return result;
    };

    /**
     * Executes the given SQL statement, which may be an INSERT, UPDATE,
     * or DELETE statement or an SQL statement that returns nothing,
     * such as an SQL data definition statement. The return value is an integer that
     * indicates the number of rows that were affected by the statement.
     * @param {String} sql an SQL statement
     * @return {int} either the row count for INSERT, UPDATE or
     * DELETE statements, or 0 for SQL statements that return nothing
     */
    this.execute = function(sql) {
        connection.setReadOnly(false);
        var statement = connection.createStatement();
        try {
            return statement.executeUpdate(sql);
        } finally {
            try {
                statement.close();
            } catch (error) {
                // ignore
            }
        }
    };

    /**
     * Return the name of the Helma DbSource object.
     * @return {String} the DbSource name
     */
    this.getName = function() {
        return source.getName();
    };

    /**
     * Return the name of the JDBC driver used by this Database instance.
     * @return {String} the JDBC driver name
     */
    this.getDriverName = function() {
        return source.getDriverName();
    };

    /**
     * @ignore
     */
    this.toString = function() {
        return "[helma.Database " + this.getName() + "]";
    };

    for (var i in this)
        this.dontEnum(i);

    return this;
};

/**
 * Create a new Database instance using the given parameters.
 * <p>Some of the parameters support shortcuts for known database products.
 * The <code>url</code> parameter recognizes the values "mysql", "oracle" and
 * "postgresql". For those databases, it is also possible to pass just
 * <code>hostname</code> or <code>hostname:port</code> as <code>url</code>
 * parameters instead of the full JDBC URL.</p>
 * @param {String} driver the class name of the JDBC driver. As
 * shortcuts, the values "mysql", "oracle" and "postgresql" are
 * recognized.
 * @param {String} url the JDBC URL.
 * @param {String} name the name of the database to use
 * @param {String} user the the username
 * @param {String} password the password
 * @return {helma.Database} a helma.Database instance
 */
helma.Database.createInstance = function(driver, url, name, user, password) {
    var DbSource = Packages.helma.objectmodel.db.DbSource;

    if (!driver || !url || !name)
        throw("Insufficient arguments to create helma.db.Connection");
    if (typeof password != "string")
        password = "";

    var MYSQL = "mysql";
    var ORACLE = "oracle";
    var POSTGRESQL = "postgresql";
    var JDBC = "jdbc:";
    var DRIVER_MYSQL = "com.mysql.jdbc.Driver";
    var DRIVER_ORACLE = "oracle.jdbc.driver.OracleDriver";
    var DRIVER_POSTGRESQL = "org.postgresql.Driver";

    if (driver == MYSQL) {
        driver = DRIVER_MYSQL;
        if (url.indexOf(JDBC) != 0)
            url = "jdbc:mysql://" + url + "/" + name;
    } else if (driver == ORACLE) {
        driver = DRIVER_ORACLE;
        if (url.indexOf(JDBC) != 0)
            url = "jdbc:oracle:thin:@" + url + ":" + name;
    } else if (driver == POSTGRESQL) {
        driver = DRIVER_POSTGRESQL;
        if (url.indexOf(JDBC) != 0)
            url = "jdbc:postgresql://" + url + "/" + name;
    }
    var props = new Packages.helma.util.ResourceProperties();
    props.put(name + ".url", url);
    props.put(name + ".driver", driver);
    if (user) {
        props.put(name + ".user", user)
    }
    if (password) {
        props.put(name + ".password", password);
    }
    return new helma.Database(new DbSource(name, props));
}

/**
 * Get a Database instance using the Database source defined in the
 * application's db.properties file with the given name.
 * @param {String} name the name of the DB source as defined in db.properties
 * @return {helma.Database} a helma.Database instance
 */
helma.Database.getInstance = function(name) {
    return new helma.Database(app.getDbSource(name));
}

/**
 * @ignore
 */
helma.Database.toString = function() {
    return "[helma.Database]";
};

/**
 * @ignore
 */
helma.Database.example = function() {
    var type = "mysql";
    var host = "localhost";
    var user = "root";
    var pw = "";
    var name = "mysql";
    var db = new helma.Database(type, host, user, pw, name);
    var result = db.query("select count(*) from db");
    res.write(result.toSource());
    return;
};


helma.lib = "Database";
helma.dontEnum(helma.lib);
for (var i in helma[helma.lib])
    helma[helma.lib].dontEnum(i);
for (var i in helma[helma.lib].prototype)
    helma[helma.lib].prototype.dontEnum(i);
delete helma.lib;
