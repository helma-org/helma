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


// Modified to use Helma database connections, Hannes Wallnoefer 2000-2003

package helma.scripting.rhino.extensions;

import helma.objectmodel.db.DbSource;
import java.util.Enumeration;
import java.util.Vector;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.sql.*;


/**
  * A Database object, representing a connection to a JDBC Driver
  */
public class DatabaseObject {

    private transient Connection connection = null; // Null if not connected
    private transient DatabaseMetaData databaseMetaData = null;
    private transient String driverName = null;
    private transient Exception lastError = null;
    private transient boolean driverOK = false;

    /**
     * Create a new database object based on a hop data source.
     *
     * @param dbsource The name of the DB source
     */

    public DatabaseObject(DbSource dbsource) {
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
     * @param  url the database URL
     * @param  userName the database user name
     * @param  password the database password
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
            connection.setReadOnly(true);
            statement = connection.createStatement();
            resultSet = statement.executeQuery(sql);     // will return true if first result is a result set

            return new RowSet(sql, this, statement, resultSet);
        } catch (SQLException e) {
            // System.err.println("##Cannot retrieve: " + e);
            // e.printStackTrace();
            lastError = e;
            try {
                if (statement != null) statement.close();
            } catch (Exception ignored) {
            }
            statement = null;
            return null;
        }
    }

    public RowSet executePreparedRetrieval(PreparedStatement statement) {
        ResultSet resultSet = null;

        try {
            resultSet = statement.executeQuery();
            return new RowSet(statement.toString(), this, statement, resultSet);
        } catch (SQLException e) {
            lastError = e;
            try {
                if (statement != null) statement.close();
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

            connection.setReadOnly(false);
            statement = connection.createStatement();
            count = statement.executeUpdate(sql);     // will return true if first result is a result set
        } catch (SQLException e) {
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

    /**
      * A RowSet object
      */
    public static class RowSet {

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
                try {
                    int index = Integer.parseInt(propertyName);
                    return getProperty(index);
                } catch (NumberFormatException e) {
                    int index = resultSet.findColumn(propertyName);
                    return getProperty(index);
                }
           } catch (SQLException e) {
              //System.err.println("##Cannot get property '" + propertyName + "' " + e);
              //e.printStackTrace();
              lastError = e;
           }
           return null;
        }

        /* FIXME: dunno if this method is still used somewhere
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
        */

        public Object getProperty(int index) {
            if (!firstRowSeen) {
                lastError = new SQLException("Attempt to access data before the first row is read");
                return null;
            }
            if (resultSet == null) {
                lastError = new SQLException("Attempt to access a released result set");
                return null;
            }

            lastError = null;
            try {
                int type = resultSetMetaData.getColumnType(index);
                switch (type) {
                    case Types.BIT:
                    case Types.BOOLEAN:
                        return resultSet.getBoolean(index) ? Boolean.TRUE : Boolean.FALSE;

                    case Types.TINYINT:
                    case Types.BIGINT:
                    case Types.SMALLINT:
                    case Types.INTEGER:
                        return new Long(resultSet.getLong(index));

                    case Types.REAL:
                    case Types.FLOAT:
                    case Types.DOUBLE:
                        return new Double(resultSet.getDouble(index));

                    case Types.DECIMAL:
                    case Types.NUMERIC:
                        BigDecimal num = resultSet.getBigDecimal(index);
                        if (num == null) {
                            break;
                        }

                        if (num.scale() > 0) {
                            return new Double(num.doubleValue());
                        } else {
                            return new Long(num.longValue());
                        }

                    case Types.VARBINARY:
                    case Types.BINARY:
                        return resultSet.getString(index);

                    case Types.LONGVARBINARY:
                    case Types.LONGVARCHAR:
                        try {
                            return resultSet.getString(index);
                        } catch (SQLException x) {
                            Reader in = resultSet.getCharacterStream(index);
                            char[] buffer = new char[2048];
                            int read = 0;
                            int r = 0;

                            while ((r = in.read(buffer, read, buffer.length - read)) > -1) {
                                read += r;

                                if (read == buffer.length) {
                                    // grow input buffer
                                    char[] newBuffer = new char[buffer.length * 2];

                                    System.arraycopy(buffer, 0, newBuffer, 0,
                                            buffer.length);
                                    buffer = newBuffer;
                                }
                            }
                            return new String(buffer, 0, read);
                        }

                    case Types.DATE:
                    case Types.TIME:
                    case Types.TIMESTAMP:
                        return resultSet.getTimestamp(index);

                    case Types.NULL:
                        return null;

                    case Types.CLOB:
                        Clob cl = resultSet.getClob(index);
                        if (cl == null) {
                            return null;
                        }
                        char[] c = new char[(int) cl.length()];
                        Reader isr = cl.getCharacterStream();
                        isr.read(c);
                        return String.copyValueOf(c);

                    default:
                        return resultSet.getString(index);
                }
            } catch (SQLException e) {
                // System.err.println("##Cannot get property: " + e);
                // e.printStackTrace();
                lastError = e;
            } catch (IOException ioe) {
                lastError = ioe;
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
            return new String[] {"length"};
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

}


