package helma.objectmodel.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * A thin wrapper around a java.sql.Connection, providing utility methods
 * for connection validation and closing.
 */
public class DbConnection {

    private final Connection connection;
    private final int serialId;

    public DbConnection(Connection connection, int serialId) {
        if(connection == null) {
            throw new NullPointerException("connection parameter null in DbConnection");
        }
        this.connection = connection;
        this.serialId = serialId;
    }

    public Connection getConnection() {
        return connection;
    }

    public int getSerialId() {
        return serialId;
    }

    public void close() {
        try {
            if (!connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException x) {
            System.err.println("Error closing DB connection: " + x);
        }
    }

    public boolean isValid(int id) {
        if (id != serialId) {
            return false;
        }
        // test if connection is still ok
        try {
            Statement stmt = connection.createStatement();
            stmt.execute("SELECT 1");
            stmt.close();
        } catch (SQLException sx) {
            return false;
        }
        return true;
    }

    public String toString() {
        return "DbConnection[" + connection.toString() + "]";
    }
}
