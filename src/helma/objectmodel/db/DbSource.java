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
import java.util.Enumeration;
import java.util.Properties;
import java.util.Hashtable;

/**
 *  This class describes a releational data source (URL, driver, user and password).
 */
public class DbSource {
    private static ResourceProperties defaultProps = null;
    private Properties conProps;
    private String name;
    private ResourceProperties props;
    protected String url;
    private String driver;
    private boolean isOracle, isMySQL;
    private long lastRead = 0L;
    private Hashtable dbmappings = new Hashtable();

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
        Connection con = null;
        Transactor tx = null;
        if (Thread.currentThread() instanceof Transactor) {
            tx = (Transactor) Thread.currentThread();
            con = tx.getConnection(this);
        }

        boolean fileUpdated = props.lastModified() > lastRead;
        if (!fileUpdated && (defaultProps != null)) {
            fileUpdated = defaultProps.lastModified() > lastRead;
        }

        if (con == null || con.isClosed() || fileUpdated) {
            init();
            Class.forName(driver);
            con = DriverManager.getConnection(url, conProps);
            // con = DriverManager.getConnection(url, user, password);

            // If we wanted to use SQL transactions, we'd set autoCommit to
            // false here and make commit/rollback invocations in Transactor methods;
            // System.err.println ("Created new Connection to "+url);
            if (tx != null) {
                tx.registerConnection(this, con);
            }
        }

        return con;
    }

    /**
     * Initialize the db source from the properties
     *
     * @throws ClassNotFoundException if the JDBC driver couldn't be loaded
     */
    private synchronized void init() throws ClassNotFoundException {
        lastRead = (defaultProps == null) ? props.lastModified()
                                          : Math.max(props.lastModified(),
                                                     defaultProps.lastModified());
        // get JDBC URL and driver class name
        url = props.getProperty(name + ".url");
        driver = props.getProperty(name + ".driver");
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
        // test if driver class is available
        Class.forName(driver);

        // set up driver connection properties
        conProps=new Properties();
        String prop = props.getProperty(name + ".user");
        if (prop != null) {
            conProps.put("user", prop);
        }
        prop = props.getProperty(name + ".password");
        if (prop != null) {
            conProps.put("password", prop);
        }

        // read any remaining extra properties to be passed to the driver
        for (Enumeration e = props.keys(); e.hasMoreElements(); ) {
            String fullkey = (String) e.nextElement();

            int dot = fullkey.indexOf('.');
            // filter out properties that don't belong to this data source
            if (dot < 0 || !fullkey.substring(0, dot).equalsIgnoreCase(name)) {
                continue;
            }
            String key = fullkey.substring(dot+1);
            // filter out properties we alread have
            if ("url".equalsIgnoreCase(key) ||
                "driver".equalsIgnoreCase(key) ||
                "user".equalsIgnoreCase(key) ||
                "password".equalsIgnoreCase(key)) {
                continue;
            }
            conProps.setProperty(key, props.getProperty(fullkey));
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
     * Set the default (server-wide) properties
     *
     * @param props server default db.properties
     */
    public static void setDefaultProps(ResourceProperties props) {
        defaultProps = props;
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
