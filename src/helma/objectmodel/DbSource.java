// DbSource.java
// Copyright (c) Hannes Wallnöfer 1999-2000

package helma.objectmodel;

import java.sql.*;
import java.util.Hashtable;
import helma.objectmodel.db.Transactor;
 
/**
 *  This class describes a releational data source (URL, driver, user and password).
 */

public class DbSource {

    private String name;
    protected String url;
    private String driver;
    protected String user;
    private String password;

    public DbSource (String name) throws ClassNotFoundException {
	this.name = name;
	IServer.dbSources.put (name.toLowerCase (), this);
	url = IServer.dbProps.getProperty (name+".url");
	driver = IServer.dbProps.getProperty (name+".driver");
	Class.forName (driver);
	user = IServer.dbProps.getProperty (name+".user");
	password = IServer.dbProps.getProperty (name+".password");
	IServer.getLogger().log ("created db source ["+name+", "+url+", "+driver+", "+user+"]");
    }

    public Connection getConnection () throws ClassNotFoundException, SQLException {
	Transactor tx = (Transactor) Thread.currentThread ();
	Connection con = tx.getConnection (this);
	if (con == null || con.isClosed ()) {
	    Class.forName (driver);
	    con = DriverManager.getConnection (url, user, password);
	    // If we wanted to use SQL transactions, we'd set autoCommit to
	    // false here and make commit/rollback invocations in Transactor methods;
	    IServer.getLogger().log ("Created new Connection to "+url);
	    tx.registerConnection (this, con);
	}
	return con;
    }

    public String getDriverName () {
	return driver;
    }

    public String getName () {
	return name;
    }

}








































