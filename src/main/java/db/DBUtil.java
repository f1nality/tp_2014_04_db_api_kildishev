package db;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.*;

/**
 * @author d.kildishev
 */
public class DBUtil {
    public static Connection openConnection() {
        Connection connection = null;

        try {
            connection = DriverManager.getConnection("jdbc:mysql://F1NAL-MOBILE:3306/forums_db", "root", "");
        } catch (SQLException e) { }

        return connection;
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
}
