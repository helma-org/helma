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

import helma.util.ResourceProperties;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Hashtable;

/**
 *  This class describes a relational data source (URL, driver, user and password).
 */
public class DbSource {
    private String name;
    private int serialId = 0;
    private ResourceProperties props;
    protected String url;
    private String driver;
    private Properties conProps;
    private boolean isOracle, isMySQL, isPostgreSQL, isH2;
    private long lastRead = 0L;
    private Hashtable dbmappings = new Hashtable();
    // thread local connection holder for non-transactor threads
    private ThreadLocal connection;

    /**
     * Creates a new DbSource object.
     *
     * @param name the db source name
     * @param props the properties
     * @throws ClassNotFoundException if the JDBC driver couldn't be loaded
     */
    public DbSource(String name, ResourceProperties props)
             throws ClassNotFoundException {
        this.name = name;
        this.props = props;
        init();
    }

    /**
     * Get a JDBC connection to the db source.
     *
     * @return a JDBC connection
     *
     * @throws ClassNotFoundException if the JDBC driver couldn't be loaded
     * @throws SQLException if the connection couldn't be created
     */
    public synchronized Connection getConnection()
            throws ClassNotFoundException, SQLException {
        DbConnection con;
        Transactor tx = Transactor.getInstance();
        if (props.lastModified() != lastRead) {
            init();
        }
        if (tx != null) {
            con = tx.getDbConnection(name, serialId);
        } else {
            con = getThreadLocalDbConnection();
        }

        if (con == null) {
            con = new DbConnection(DriverManager.getConnection(url, conProps), serialId);

            // If we wanted to use SQL transactions, we'd set autoCommit to
            // false here and make commit/rollback invocations in Transactor methods;
            // System.err.println ("Created new Connection to "+url);
            if (tx != null) {
                tx.registerConnection(name, con);
            } else {
                connection.set(con);
            }
        }

        return con.getConnection();
    }

    /**
     * Used for connections not managed by a Helma transactor
     * @return a thread local tested connection, or null
     */
    private DbConnection getThreadLocalDbConnection() {
        if (connection == null) {
            connection = new ThreadLocal();
            return null;
        }
        DbConnection con = (DbConnection) connection.get();
        if (con != null && !con.isValid(serialId)) {            
            con.close();
            connection.remove();
            return null;
        }
        return con;
    }

    /**
     * Set the db properties to newProps, and return the old properties.
     * @param newProps the new properties to use for this db source
     * @return the old properties
     * @throws ClassNotFoundException if jdbc driver class couldn't be found
     */
    public synchronized ResourceProperties switchProperties(ResourceProperties newProps) 
            throws ClassNotFoundException {
        ResourceProperties oldProps = props;
        props = newProps;
        init();
        return oldProps;
    }

    /**
     * Initialize the db source from the properties
     *
     * @throws ClassNotFoundException if the JDBC driver couldn't be loaded
     */
    private synchronized void init() throws ClassNotFoundException {
        lastRead = props.lastModified();
        serialId ++;
        // refresh sub-properties for this DbSource
        ResourceProperties subProps = props.getSubProperties(name + '.');
        // get JDBC URL and driver class name
        url = subProps.getProperty("url");
        driver = subProps.getProperty("driver");
        // sanity checks
        if (url == null) {
            throw new NullPointerException(name+".url is not defined in db.properties");
        }
        if (driver == null) {
            throw new NullPointerException(name+".driver class not defined in db.properties");
        }
        // test if this is an Oracle or MySQL driver
        isOracle = driver.startsWith("oracle.jdbc.driver");
        isMySQL = driver.startsWith("com.mysql.jdbc") ||
                  driver.startsWith("org.gjt.mm.mysql");
        isPostgreSQL = driver.equals("org.postgresql.Driver");
        isH2 = driver.equals("org.h2.Driver");
        // test if driver class is available
        Class.forName(driver);

        // set up driver connection properties
        conProps=new Properties();
        String prop = subProps.getProperty("user");
        if (prop != null) {
            conProps.put("user", prop);
        }
        prop = subProps.getProperty("password");
        if (prop != null) {
            conProps.put("password", prop);
        }

        // read any remaining extra properties to be passed to the driver
        for (Enumeration e = subProps.keys(); e.hasMoreElements(); ) {
            String key = (String) e.nextElement();

            // filter out properties we alread have
            if ("url".equalsIgnoreCase(key) ||
                "driver".equalsIgnoreCase(key) ||
                "user".equalsIgnoreCase(key) ||
                "password".equalsIgnoreCase(key)) {
                continue;
            }
            conProps.setProperty(key, subProps.getProperty(key));
        }
    }

    /**
     * Return the class name of the JDBC driver
     *
     * @return the class name of the JDBC driver
     */
    public String getDriverName() {
        return driver;
    }

    /**
     * Return the name of the db dource
     *
     * @return the name of the db dource
     */
    public String getName() {
        return name;
    }

    /**
     * Check if this DbSource represents an Oracle database
     *
     * @return true if we're using an oracle JDBC driver
     */
    public boolean isOracle() {
        return isOracle;
    }

    /**
     * Check if this DbSource represents a MySQL database
     *
     * @return true if we're using a MySQL JDBC driver
     */
    public boolean isMySQL() {
        return isMySQL;
    }

    /**
     * Check if this DbSource represents a PostgreSQL database
     *
     * @return true if we're using a PostgreSQL JDBC driver
     */
    public boolean isPostgreSQL() {
        return isPostgreSQL;
    }

    /**
     * Check if this DbSource represents a H2 database
     *
     * @return true if we're using a H2 JDBC driver
     */
    public boolean isH2() {
        return isH2;
    }

    /**
     * Register a dbmapping by its table name.
     *
     * @param dbmap the DbMapping instance to register
     */
    protected void registerDbMapping(DbMapping dbmap) {
        if (!dbmap.inheritsStorage() && dbmap.getTableName() != null) {
            dbmappings.put(dbmap.getTableName().toUpperCase(), dbmap);
        }
    }

    /**
     * Look up a DbMapping instance for the given table name.
     *
     * @param tablename the table name
     * @return the matching DbMapping instance
     */
    protected DbMapping getDbMapping(String tablename) {
        return (DbMapping) dbmappings.get(tablename.toUpperCase());
    }

}
