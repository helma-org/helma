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

package helma.scripting.rhino;

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

    DatabaseObject(DbSource dbsource, int flag) {
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
    public boolean connect(String url, String userName, String password) throws SQLException  {
        if (!driverOK) {
            throw new SQLException("Driver not initialized properly - cannot connect");
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
    public boolean disconnect() throws SQLException {
        if (!driverOK) {
            throw new SQLException("Driver not initialized properly - cannot disconnect");
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
          try {
              disconnect();
          } catch (SQLException e) {
              // ignored
          }
       }
    }

    public RowSet executeRetrieval(String sql) throws SQLException {
        if (connection==null) {
            throw new SQLException("JDBC driver not connected");
        }
        Statement statement = null;
        ResultSet resultSet = null;

        try {
            statement = connection.createStatement();
            resultSet = statement.executeQuery(sql);     // will return true if first result is a result set
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
        RowSet rowSet = new RowSet(sql, this, statement, resultSet);
        return rowSet;
    }

    public int executeCommand(String sql) throws SQLException {
        int count = 0;

        if (connection==null) {
            throw new SQLException("JDBC driver not connected");
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

        if (sql==null) throw new NullPointerException();
        if (resultSet==null) throw new NullPointerException();
        if (statement==null) throw new NullPointerException();
        if (database==null) throw new NullPointerException();
        
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

    public String getColumnName(int idx) throws SQLException {
       if (resultSet == null) {
            throw new SQLException("Attempt to access a released result set");
       }
        if (idx>0 && idx <=colNames.size()) {
            return (String) colNames.elementAt(idx-1); // to base 0
        } else {
            throw new SQLException("Column index (base 1) " + idx +
                                        " out of range, max: " +colNames.size());
        }
    }


    public int getColumnDatatypeNumber(int idx) throws SQLException {
       if (resultSet == null) {
            throw new SQLException("Attempt to access a released result set");
       }
        if (idx>0 && idx <=colNames.size()) {
            try {
                return resultSetMetaData.getColumnType(idx);
            } catch (SQLException e) {
                lastError = e;
                return -1;
            }
        } else {
            throw new SQLException("Column index (base 1) " + idx +
                                        " out of range, max: " +colNames.size());
        }
    }


    public String getColumnDatatypeName(int idx) throws SQLException {
       if (resultSet == null) {
            throw new SQLException("Attempt to access a released result set");
       }
        if (idx>0 && idx <=colNames.size()) {
            try {
                return resultSetMetaData.getColumnTypeName(idx);
            } catch (SQLException e) {
                lastError = e;
                return null;
            }
        } else {
            throw new SQLException("Column index (base 1) " + idx +
                                        " out of range, max: " +colNames.size());
        }
    }


    public Object getColumnItem(String propertyName) throws SQLException {
       if (resultSet == null) {
            throw new SQLException("Attempt to access a released result set");
       }
       if (!firstRowSeen) {
            throw new SQLException("Attempt to access data before the first row is read");
       }
       int hash = propertyName.hashCode();
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

    public Object getProperty(String propertyName, int hash)
                                throws SQLException {
        //System.err.println(" &&& Getting property '" + propertyName + "'");

        // Length property is firsy checked

        // First return system or or prototype properties
        if (propertyName.equals("length")) {
             return new Integer(colNames.size());
        } else {
           if (resultSet == null) {
                throw new SQLException("Attempt to access a released result set");
           }
            if (!firstRowSeen) {
                throw new SQLException("Attempt to access data before the first row is read");
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

    public Object getProperty(int index)
                            throws SQLException {
        if (!firstRowSeen) {
            throw new SQLException("Attempt to access data before the first row is read");
        }
        if (resultSet == null) {
            throw new SQLException("Attempt to access a released result set");
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

    /**
     * Get all properties (including hidden ones), for the command
     * @listall of the interpreter. Include the visible properties of the
     * prototype (that is the one added by the user) but not the
     * hidden ones of the prototype (otherwise this would list
     * all functions for any object).
     *
     * @return An enumeration of all properties (visible and hidden).  
     */
    /* public Enumeration getAllProperties() {
         return new Enumeration() {
                String [] specialProperties = getSpecialPropertyNames();
                int specialEnumerator = 0;
                Enumeration props = getProperties(); // all of object properties
                String currentKey = null;
                int currentHash = 0;
                boolean inside = false;     // true when examing prototypes properties
                public boolean hasMoreElements() {
                    // OK if we already checked for a property and one exists
                    if (currentKey != null) return true;
                    // Loop on special properties first
                    if (specialEnumerator < specialProperties.length) {
                        currentKey = specialProperties[specialEnumerator];
                        currentHash = currentKey.hashCode();
                        specialEnumerator++;
                        return true;
                    }
                    // loop on standard or prototype properties
                    while (props.hasMoreElements()) {
                       currentKey = (String) props.nextElement();
                       currentHash = currentKey.hashCode();
                       if (inside) {
                           try {
                              if (hasProperty(currentKey, currentHash)) continue;
                           } catch (EcmaScriptException ignore) {
                           }
                           // SHOULD CHECK IF NOT IN SPECIAL
                       }
                       return true;
                    }
                    // If prototype properties have not yet been examined, look for them
                    if (!inside && getPrototype() != null) {
                        inside = true;
                        props = getPrototype().getProperties();
                        while (props.hasMoreElements()) {
                           currentKey = (String) props.nextElement();
                           currentHash = currentKey.hashCode();
                           try {
                               if (hasProperty(currentKey, currentHash)) continue;
                           } catch (EcmaScriptException ignore) {
                           }
                           return true;
                        }
                    }
                    return false;
                }
                public Object nextElement() {
                    if (hasMoreElements()) {
                       String key = currentKey;
                       currentKey = null;
                       return key;
                     } else {
                         throw new java.util.NoSuchElementException();
                     }
                 }
         };
    } */

    public String[] getSpecialPropertyNames() {
        String [] ns = {"length"};
        return ns;
    }


    public boolean next() throws SQLException {
        boolean status = false;
        if (lastRowSeen) {
            throw new SQLException("Attempt to access a next row after last row has been returned");
        }
        if (resultSet == null) {
            throw new SQLException("Attempt to access a released result set");
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
