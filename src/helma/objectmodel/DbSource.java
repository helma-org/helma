// DbSource.java
// Copyright (c) Hannes Wallnöfer 1999-2000

package helma.objectmodel;

import java.sql.*;
import java.util.Hashtable;
import helma.objectmodel.db.*;
 
/**
 *  This class describes a releational data source (URL, driver, user and password).
 */

public class DbSource {

    private String name;
    private SystemProperties props;
    private static SystemProperties defaultProps = null;
    protected String url;
    private String driver;
    protected String user;
    private String password;

    private long lastRead = 0l;

    public DbSource (String name, SystemProperties props) throws ClassNotFoundException {
	this.name = name;
	this.props = props;
	init ();
	IServer.getLogger().log ("created db source ["+name+", "+url+", "+driver+", "+user+"]");
    }

    public Connection getConnection () throws ClassNotFoundException, SQLException {
	Transactor tx = (Transactor) Thread.currentThread ();
	Connection con = tx.getConnection (this);
	boolean fileUpdated = props.lastModified () > lastRead;
	if (!fileUpdated && defaultProps != null)
	    fileUpdated = defaultProps.lastModified () > lastRead;
	if (con == null || con.isClosed () || fileUpdated) {
	    init ();
	    Class.forName (driver);
	    con = DriverManager.getConnection (url, user, password);
	    // If we wanted to use SQL transactions, we'd set autoCommit to
	    // false here and make commit/rollback invocations in Transactor methods;
	    IServer.getLogger().log ("Created new Connection to "+url);
	    tx.registerConnection (this, con);
	}
	return con;
    }

    private void init () throws ClassNotFoundException {
	lastRead = defaultProps == null ? props.lastModified () : Math.max (props.lastModified (), defaultProps.lastModified ());
	url = props.getProperty (name+".url");
	driver = props.getProperty (name+".driver");
	Class.forName (driver);
	user = props.getProperty (name+".user");
	password = props.getProperty (name+".password");
    }

    public String getDriverName () {
	return driver;
    }

    public String getName () {
	return name;
    }

    public static void setDefaultProps (SystemProperties props) {
	defaultProps = props;
    }

}








































