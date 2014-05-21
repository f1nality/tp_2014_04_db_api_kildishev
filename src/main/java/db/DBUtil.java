package db;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author d.kildishev
 */
public class DBUtil {
    private static ConcurrentLinkedQueue<Connection> connectionPool = new ConcurrentLinkedQueue<>();

    public static Connection openConnection() {

        Connection connection = null;

        try {
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/forums_db", "root", "");
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            System.out.println("Can't create connection");
            e.printStackTrace();
        }


        return connection;
    }

    public static void createConnectionPool(int connections) {
        connectionPool.clear();

        for (int i = 0; i < connections; ++i) {
            Connection connection = openConnection();

            if (connection != null) {
                connectionPool.add(connection);
            }
        }
    }

    public static Connection getPoolConnection() {
        while (connectionPool.size() == 0) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) { }
        }

        return connectionPool.poll();
    }

    public static void freePoolConnection(Connection connection) {
        connectionPool.add(connection);
    }

    public static void truncate(Connection connection) {
        try {
            connection.prepareStatement("DELETE FROM followers").executeUpdate();
            connection.prepareStatement("DELETE FROM forums").executeUpdate();
            connection.prepareStatement("DELETE FROM posts").executeUpdate();
            connection.prepareStatement("DELETE FROM subscriptions").executeUpdate();
            connection.prepareStatement("DELETE FROM threads").executeUpdate();
            connection.prepareStatement("DELETE FROM users").executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public static long getStatementGeneratedId(PreparedStatement stmt) throws SQLException {
        ResultSet resultSet = stmt.getGeneratedKeys();

        if (resultSet != null && resultSet.next()) {
            return resultSet.getLong(1);
        } else {
            return 0;
        }
    }

    public static ResultSet getSingleResult(PreparedStatement stmt) throws SQLException {
        ResultSet resultSet = stmt.executeQuery();

        if (!resultSet.next()) {
            return null;
        }

        return resultSet;
    }

    public static String columnsOfTableToString(String table, String[] columns) {
        for (int i = 0; i < columns.length; ++i) {
            columns[i] = table + "." + columns[i];
        }

        return StringUtils.join(columns, ", ");
    }

    public static void jsonPutResultSetColumn(JSONObject obj, String name, ResultSet resultSet, String columnName, Class type) {
        try {
            if (type == String.class) {
                obj.put(name, resultSet.getString(columnName));
            } else if (type == Integer.class) {
                obj.put(name, resultSet.getInt(columnName));
            } else if (type == Boolean.class) {
                obj.put(name, resultSet.getBoolean(columnName));
            }
        } catch (SQLException | JSONException e) {
            e.printStackTrace();
        }
    }

    public static Object jsonNullable(String value) {
        return (value.equals("null")) ? JSONObject.NULL : value;
    }

    public static Object jsonNullable(int value) {
        return (value == 0) ? JSONObject.NULL : value;
    }
}
