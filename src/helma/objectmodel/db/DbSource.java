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

import helma.util.SystemProperties;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Properties;

/**
 *  This class describes a releational data source (URL, driver, user and password).
 */
public class DbSource {
    private static SystemProperties defaultProps = null;
    private Properties conProps;
    private String name;
    private SystemProperties props;
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
    public DbSource(String name, SystemProperties props)
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
        conProps=new Properties();
        for (Enumeration e = props.keys(); e.hasMoreElements(); ) {
            String key = (String) e.nextElement();
            if (!key.toLowerCase().startsWith(name.toLowerCase()))
                continue;
            if (key.equalsIgnoreCase(name + ".url")) {
                url = props.getProperty(key);
                continue;
            }
            if (key.equalsIgnoreCase(name + ".driver")) {
                driver = props.getProperty(key);
                isOracle = driver != null && driver.startsWith("oracle.jdbc");
                Class.forName(driver);
                continue;
            }
            conProps.setProperty(key.substring(name.length()+1), props.getProperty(key));
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
    public static void setDefaultProps(SystemProperties props) {
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
