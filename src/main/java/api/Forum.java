package api;

import db.DBUtil;
import org.json.JSONException;
import org.json.JSONObject;
import java.sql.*;
import java.util.List;

/**
 * @author d.kildishev
 */
public class Forum {
    Connection connection = null;

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public JSONObject create(String name, String shortName, String userEmail) {
        JSONObject result = new JSONObject();
        JSONObject response = new JSONObject();

        long id;

        try {
            PreparedStatement stmt = connection.prepareStatement("INSERT INTO forums (name, short_name, users_email) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS);

            stmt.setString(1, name);
            stmt.setString(2, shortName);
            stmt.setString(3, userEmail);

            stmt.executeUpdate();
            id = DBUtil.getStatementGeneratedId(stmt);
        } catch (SQLException e) {
            e.printStackTrace();
            return details(shortName, null);
        }

        try {
            result.put("code", 0);
            result.put("response", response);
            response.put("id", id);
            response.put("name", name);
            response.put("short_name", shortName);
            response.put("user", userEmail);
        } catch (JSONException e) { }

        return result;
    }

    public JSONObject details(String shortName, List<String> related) {
        JSONObject result = new JSONObject();
        JSONObject response = jsonObject(connection, shortName, related);

        try {
            if (response != null) {
                result.put("code", 0);
                result.put("response", response);
            } else {
                result.put("code", 1);
                result.put("message", "forum not found: " + shortName);
            }
        } catch (JSONException e) { }

        return result;
    }

    public static JSONObject jsonObject(Connection connection, String shortName, List<String> related) {
        JSONObject jsonObject = null;
        ResultSet forum = null;

        try {
            PreparedStatement stmt = connection.prepareStatement("SELECT id, name, short_name, users_email FROM forums WHERE short_name = ?");

            stmt.setString(1, shortName);

            forum = DBUtil.getSingleResult(stmt);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (forum != null) {
            try {
                jsonObject = new JSONObject();

                jsonObject.put("id", forum.getInt("id"));
                jsonObject.put("name", forum.getString("name"));
                jsonObject.put("short_name", forum.getString("short_name"));
                jsonObject.put("user", forum.getString("users_email"));

                if (related != null && related.contains("user")) {
                    jsonObject.put("user", User.jsonObject(connection, forum.getString("users_email")));
                }
            } catch (JSONException | SQLException e) { }
        }

        return jsonObject;
    }
}