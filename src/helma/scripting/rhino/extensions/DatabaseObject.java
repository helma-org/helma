// Database.java
// FESI Copyright (c) Jean-Marc Lugrin, 1999
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2 of the License, or (at your option) any later version.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.

// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA


// Modified to use Helma database connections, Hannes Wallnöfer 2000-2003

package helma.scripting.rhino.extensions;

import helma.framework.core.Application;
import helma.objectmodel.db.DbSource;
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;
import java.sql.*;


/**
  * A Database object, representing a connection to a JDBC Driver
  */
public class DatabaseObject {

    private transient Connection connection = null; // Null if not connected
    private transient DatabaseMetaData databaseMetaData = null;
    private transient String driverName = null;
    private transient ClassLoader driverLoader = null;
    private transient Exception lastError = null;
    private transient boolean driverOK = false;

    /**
     * Create a new database object based on a hop data source.
     *
     * @param dbsource The name of the DB source
     */

    public DatabaseObject(DbSource dbsource, int flag) {
        try {
            connection = dbsource.getConnection ();
            driverName = dbsource.getDriverName ();
        } catch (Exception e) {
            // System.err.println("##Cannot find driver class: " + e);
            // e.printStackTrace();
            lastError = e;
        }
        driverOK = true;
    }

    /**
     * Create a new database object based on a driver name, with driver on the classpath
     *
     * @param driverName The class name of the JDBC driver
     */

    DatabaseObject(String driverName) {
        this.driverName = driverName;
        try {
            Class driverClass = Class.forName(driverName);
            if (!Driver.class.isAssignableFrom(driverClass)) {

                // System.err.println("##Bad class " + driverClass);
                lastError = new RuntimeException("Class " + driverClass + " is not a JDBC driver");
            }
            driverClass.newInstance(); // may be needed by some drivers, harmless for others
        } catch (ClassNotFoundException e) {
            // System.err.println("##Cannot find driver class: " + e);
            // e.printStackTrace();
            lastError = e;
        } catch (InstantiationException e) {

            // System.err.println("##Cannot instantiate driver class: " + e);
            // e.printStackTrace();
            lastError = e;
        } catch (IllegalAccessException e) {
            // ignore as this may happen depending on driver, it may not be severe
            // for example because the instance was created at class initialization time
        }
        driverOK = true;
    }


    /**
     * Create the database prototype object which cannot be connected
     *
     */

    DatabaseObject() {
        this.driverName = null;
        driverOK = false; // Avoid usage of this object
    }

    public String getClassName() {
        return "DatabaseObject";
    }

    public String toString() {
         if (driverName==null) return "[database protoype]";
         return "[Database: '" + driverName +
                 (driverOK ?
                     (connection==null ? "' - disconnected] " : " - connected]")
                 : " - in error]");
    }

    public String toDetailString() {
        return "ES:[Object: builtin " + this.getClass().getName() + ":" +
            this.toString() + "]";
    }

    public Object getLastError() {
        if (lastError == null) {
            return null;
        } else {
            return lastError;
        }
    }


    /**
     * Connect to the database, using the specific url, optional user name and password
     *
     * @param   arguments  The argument list
     * @return  true if successful, false otherwise
     */
    public boolean connect(String url, String userName, String password) {
        if (!driverOK) {
            lastError = new SQLException("Driver not initialized properly - cannot connect");
            return false;
        }
        lastError = null;
        try {
            if (userName == null) {
                connection = DriverManager.getConnection(url);
            } else {
                connection = DriverManager.getConnection(url,userName,password);
            }
        } catch(Exception e) {
            // System.err.println("##Cannot connect: " + e);
            // e.printStackTrace();
            lastError = e;
            return false;
        }
        return true;
    }


    /**
     * Disconnect from the database, nop if not conected
     *
     * @return  true if successful, false if error during idsconnect
     */
    public boolean disconnect() {
        if (!driverOK) {
            lastError = new SQLException("Driver not initialized properly - cannot disconnect");
            return false;
        }
        lastError = null;
        if (connection != null) {
             try {
                connection.close();
                connection = null;
                lastError = null;
           } catch (SQLException e) {
                // System.err.println("##Cannot disonnect: " + e);
                // e.printStackTrace();
                lastError = e;
                return false;
            }
        }
        return true;
    }

    public void release()  {
        if (driverOK) {
            disconnect();
        }
    }

    public RowSet executeRetrieval(String sql) {
        if (connection==null) {
            lastError = new SQLException("JDBC driver not connected");
            return null;
        }
        Statement statement = null;
        ResultSet resultSet = null;

        try {
            statement = connection.createStatement();
            resultSet = statement.executeQuery(sql);     // will return true if first result is a result set

            RowSet rowSet = new RowSet(sql, this, statement, resultSet);

            return rowSet;
       } catch(SQLException e) {
            // System.err.println("##Cannot retrieve: " + e);
            // e.printStackTrace();
            lastError = e;
            try {
                if (statement!=null) statement.close();
            } catch (Exception ignored) {
            }
            statement = null;
            return null;
        }
    }

    public int executeCommand(String sql) {
        int count = 0;

        if (connection==null) {
            lastError = new SQLException("JDBC driver not connected");
            return -1;
        }

        Statement statement = null;
         try {

            statement = connection.createStatement();
            count = statement.executeUpdate(sql);     // will return true if first result is a result set
       } catch(SQLException e) {
            // System.err.println("##Cannot retrieve: " + e);
            // e.printStackTrace();
            lastError = e;
            try {
                if (statement != null) statement.close();
            } catch (Exception ignored) {
            }
            statement = null;
            return -1;
        }
        if (statement!=null) try {
            statement.close();
        } catch (SQLException e) {
            // ignored
        }
        return count;
    }

    public Object getMetaData()
    {
      if (databaseMetaData == null)
         try {
            databaseMetaData = connection.getMetaData();
         } catch (SQLException e) {
            // ignored
         }
      return databaseMetaData;
    }
}


/**
  * A RowSet object
  */
class RowSet {

    private transient DatabaseObject database = null;
    private transient String sql = null;
    private transient Statement statement = null;
    private transient ResultSet resultSet = null;
    private transient ResultSetMetaData resultSetMetaData = null;
    private transient Vector colNames = null;
    private transient boolean lastRowSeen = false;
    private transient boolean firstRowSeen = false;
    private transient Exception lastError = null;

    RowSet(String sql,
                DatabaseObject database,
                Statement statement,
                ResultSet resultSet) throws SQLException {
        this.sql = sql;
        this.database = database;
        this.statement = statement;
        this.resultSet = resultSet;

        if (sql==null) throw new NullPointerException("sql");
        if (resultSet==null) throw new NullPointerException("resultSet");
        if (statement==null) throw new NullPointerException("statement");
        if (database==null) throw new NullPointerException("database");
        
        try {
            
            this.resultSetMetaData = resultSet.getMetaData();
            int numcols = resultSetMetaData.getColumnCount();
            //IServer.getLogger().log("$$NEXT : " + numcols);
            colNames = new Vector(numcols);
            for (int i=0; i<numcols; i++) {
               String colName = resultSetMetaData.getColumnLabel(i+1);
               //IServer.getLogger().log("$$COL : " + colName);
               colNames.addElement(colName);
            }
        } catch(SQLException e) {
            colNames = new Vector(); // An empty one
            throw new SQLException("Could not get column names: "+e);

            // System.err.println("##Cannot get column names: " + e);
            // e.printStackTrace();
        }
    }


    public String getClassName() {
        return "RowSet";
    }

    public String toDetailString() {
        return "ES:[Object: builtin " + this.getClass().getName() + ":" +
            this.toString() + "]";
    }

    public int getColumnCount() {
        return colNames.size();
    }

    public Object getMetaData()
    {
      return resultSetMetaData;
    }

    public Object getLastError() {
        if (lastError == null) {
            return null;
        } else {
            return lastError;
        }
    }


    public void release() {
        try {
            if (statement!= null) statement.close();
            if (resultSet != null) resultSet.close();
        } catch (SQLException e) {
            // ignored
        }
        statement = null;
        resultSet = null;
        resultSetMetaData = null;
    }

    public boolean hasMoreRows() {
        return !lastRowSeen;   // Simplistic implementation
    }

    public String getColumnName(int idx) {
       if (resultSet == null) {
            lastError = new SQLException("Attempt to access a released result set");
            return null;
       }
        if (idx>0 && idx <=colNames.size()) {
            return (String) colNames.elementAt(idx-1); // to base 0
        } else {
            lastError = new SQLException("Column index (base 1) " + idx +
                                        " out of range, max: " +colNames.size());
            return null;
        }
    }


    public int getColumnDatatypeNumber(int idx) {
       if (resultSet == null) {
            lastError = new SQLException("Attempt to access a released result set");
            return -1;
       }
        if (idx>0 && idx <=colNames.size()) {
            try {
                return resultSetMetaData.getColumnType(idx);
            } catch (SQLException e) {
                lastError = e;
                return -1;
            }
        } else {
            lastError = new SQLException("Column index (base 1) " + idx +
                                        " out of range, max: " +colNames.size());
            return -1;
        }
    }


    public String getColumnDatatypeName(int idx) {
       if (resultSet == null) {
            lastError = new SQLException("Attempt to access a released result set");
            return null;
       }
        if (idx>0 && idx <=colNames.size()) {
            try {
                return resultSetMetaData.getColumnTypeName(idx);
            } catch (SQLException e) {
                lastError = e;
                return null;
            }
        } else {
            lastError = new SQLException("Column index (base 1) " + idx +
                                        " out of range, max: " +colNames.size());
            return null;
        }
    }


    public Object getColumnItem(String propertyName) {
       if (resultSet == null) {
            lastError = new SQLException("Attempt to access a released result set");
            return null;
       }
       if (!firstRowSeen) {
            lastError = new SQLException("Attempt to access data before the first row is read");
            return null;
       }
       try {
            int index = -1; // indicates not a valid index value
            try {
                char c = propertyName.charAt(0);
                if ('0' <= c && c <= '9') {
                   index = Integer.parseInt(propertyName);
                }
            } catch (NumberFormatException e) {
            } catch (StringIndexOutOfBoundsException e) { // for charAt
            }
            if (index>=0) {
                return getProperty(index);
            }
           Object value = resultSet.getObject(propertyName);
           // IServer.getLogger().log("&& @VALUE : " + value);
           lastError = null;
           return value;
       } catch (SQLException e) {
          //System.err.println("##Cannot get property '" + propertyName + "' " + e);
          //e.printStackTrace();
          lastError = e;
       }
       return null;
    }

    public Object getProperty(String propertyName, int hash) {
        //System.err.println(" &&& Getting property '" + propertyName + "'");

        // Length property is firsy checked

        // First return system or or prototype properties
        if (propertyName.equals("length")) {
             return new Integer(colNames.size());
        } else {
           if (resultSet == null) {
                lastError = new SQLException("Attempt to access a released result set");
                return null;
           }
            if (!firstRowSeen) {
                lastError = new SQLException("Attempt to access data before the first row is read");
                return null;
            }
           try {
                int index = -1; // indicates not a valid index value
                try {
                    char c = propertyName.charAt(0);
                    if ('0' <= c && c <= '9') {
                       index = Integer.parseInt(propertyName);
                    }
                } catch (NumberFormatException e) {
                } catch (StringIndexOutOfBoundsException e) { // for charAt
                }
                if (index>=0) {
                    return getProperty(index);
                }
               Object value = resultSet.getObject(propertyName);
               // IServer.getLogger().log("&& @VALUE : " + value);
               lastError = null;
               return value;
           } catch (SQLException e) {
              // System.err.println("##Cannot get property '" + propertyName + "' " + e);
              // e.printStackTrace();
              lastError = e;
           } 
        }
        return null;
     }

    public Object getProperty(int index) {
        if (!firstRowSeen) {
            lastError = new SQLException("Attempt to access data before the first row is read");
            return null;
        }
        if (resultSet == null) {
            lastError = new SQLException("Attempt to access a released result set");
            return null;
        }

        try {
           Object value = resultSet.getObject(index);
           lastError = null;
           return value;
        } catch (SQLException e) {
           // System.err.println("##Cannot get property: " + e);
           // e.printStackTrace();
           lastError = e;
        }
        return null;
    }

    /*
     * Returns an enumerator for the key elements of this object.
     *
     * @return the enumerator - may have 0 length of coulmn names where not found
     */
   public Enumeration getProperties() {
       if (resultSet == null) {
            return (new Vector()).elements();
       }
       return colNames.elements();
   }


    public String[] getSpecialPropertyNames() {
        String [] ns = {"length"};
        return ns;
    }


    public boolean next() {
        boolean status = false;
        if (lastRowSeen) {
            lastError = new SQLException("Attempt to access a next row after last row has been returned");
            return false;
        }
        if (resultSet == null) {
            lastError = new SQLException("Attempt to access a released result set");
            return false;
        }   
        try {
            status = resultSet.next();
            lastError = null;
        } catch (SQLException e) {
            // System.err.println("##Cannot do next:" + e); 
            // e.printStackTrace();
            lastError = e;
        } 
        if (status) firstRowSeen = true;
        else lastRowSeen = true;
        return status;
   }

    public String toString() {
        return "[RowSet: '"+sql+"'" +
               (resultSet==null ? " - released]" :
                   (lastRowSeen ? " - at end]" :
                   (firstRowSeen ? "]" : " - at start]")));
    }

}
