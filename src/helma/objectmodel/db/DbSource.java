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
    private boolean isOracle;
    private long lastRead = 0L;

    /**
     * Creates a new DbSource object.
     *
     * @param name ...
     * @param props ...
     *
     * @throws ClassNotFoundException ...
     */
    public DbSource(String name, ResourceProperties props)
             throws ClassNotFoundException {
        this.name = name;
        this.props = props;
        init();
    }

    /**
     *
     *
     * @return ...
     *
     * @throws ClassNotFoundException ...
     * @throws SQLException ...
     */
    public Connection getConnection() throws ClassNotFoundException, SQLException {
        Transactor tx = (Transactor) Thread.currentThread();
        Connection con = tx.getConnection(this);
        boolean fileUpdated = props.lastModified() > lastRead;

        if (!fileUpdated && (defaultProps != null)) {
            fileUpdated = defaultProps.lastModified() > lastRead;
        }

        if ((con == null) || con.isClosed() || fileUpdated) {
            init();
            Class.forName(driver);
            con = DriverManager.getConnection(url, conProps);
            // con = DriverManager.getConnection(url, user, password);

            // If we wanted to use SQL transactions, we'd set autoCommit to
            // false here and make commit/rollback invocations in Transactor methods;
            // System.err.println ("Created new Connection to "+url);
            tx.registerConnection(this, con);
        }

        return con;
    }

    private void init() throws ClassNotFoundException {
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
        // test if this is an Oracle driver
        isOracle = driver.startsWith("oracle.jdbc.driver");
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
     *
     *
     * @return ...
     */
    public String getDriverName() {
        return driver;
    }

    /**
     *
     *
     * @return ...
     */
    public String getName() {
        return name;
    }

    /**
     *
     *
     * @param props ...
     */
    public static void setDefaultProps(ResourceProperties props) {
        defaultProps = props;
    }

    /**
     * Is this an Oracle database?
     *
     * @return true if we're using an oracle JDBC driver
     */
    public boolean isOracle() {
        return isOracle;
    }
}
