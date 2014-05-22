package api;

import db.DBUtil;
import org.json.JSONException;
import org.json.JSONObject;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author d.kildishev
 */
public class User {
    Connection connection = null;

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public JSONObject create(String username, String about, String name, String email, boolean isAnonymous) {
        JSONObject result = new JSONObject();
        JSONObject response = new JSONObject();

        long id;

        try {
            PreparedStatement stmt = connection.prepareStatement("INSERT INTO users (username, about, name, email, isAnonymous) VALUES (?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);

            stmt.setString(1, username);
            stmt.setString(2, about);
            stmt.setString(3, name);
            stmt.setString(4, email);
            stmt.setBoolean(5, isAnonymous);

            stmt.executeUpdate();
            id = DBUtil.getStatementGeneratedId(stmt);
        } catch (SQLException e) {
            return details(email);
        }

        try {
            result.put("code", 0);
            result.put("response", response);
            response.put("id", id);
            response.put("username", username);
            response.put("about", about);
            response.put("name", name);
            response.put("email", email);
            response.put("isAnonymous", isAnonymous);
        } catch (JSONException e) { }

        return result;
    }

    public JSONObject details(String userEmail) {
        JSONObject result = new JSONObject();
        JSONObject response = jsonObject(connection, userEmail);

        try {
            if (response != null) {
                result.put("code", 0);
                result.put("response", response);
            } else {
                result.put("code", 1);
                result.put("message", "user not found: " + userEmail);
            }
        } catch (JSONException e) { }

        return result;
    }

    public JSONObject list(String clauseField, String clauseValue, int since, int limit, String order) {
        JSONObject result = new JSONObject();
        List<JSONObject> response = new ArrayList<JSONObject>();
        StringBuilder query = new StringBuilder("SELECT ");

        HashMap<String, Class> usersColumns = new HashMap<String, Class>(){{
            put("id", Integer.class);
            put("username", String.class);
            put("about", String.class);
            put("name", String.class);
            put("email", String.class);
            put("isAnonymous", Boolean.class);
        }};

        query.append(DBUtil.columnsOfTableToString("users", usersColumns.keySet().toArray(new String[usersColumns.keySet().size()])));

        query.append(", GROUP_CONCAT(DISTINCT following.users_email_following) AS following");
        query.append(", GROUP_CONCAT(DISTINCT followers.users_email_follower) AS followers");
        query.append(", GROUP_CONCAT(DISTINCT subscriptions.threads_id) AS subscriptions");

        query.append(" FROM users JOIN posts ON users.email = posts.users_email");

        query.append(" LEFT JOIN followers AS following ON following.users_email_follower = users.email" +
                " LEFT JOIN followers AS followers ON followers.users_email_following = users.email" +
                " LEFT JOIN subscriptions ON subscriptions.users_email = users.email");

        query.append(" WHERE posts." + clauseField + " = ?");

        if (since != 0) {
            query.append(" AND users.id >= ?");
        }

        query.append(" GROUP BY " + DBUtil.columnsOfTableToString("users", usersColumns.keySet().toArray(new String[usersColumns.keySet().size()])));

        if (order != null && (order.equals("asc") || order.equals("desc"))) {
            query.append(" ORDER BY users.id " + order.toUpperCase());
        }

        if (limit != 0) {
            query.append(" LIMIT 0, " + limit);
        }

        try {
            PreparedStatement stmt = connection.prepareStatement(query.toString());

            stmt.setString(1, clauseValue);

            if (since != 0) {
                stmt.setInt(2, since);
            }

            ResultSet resultSet = stmt.executeQuery();

            while (resultSet.next()) {
                JSONObject obj = new JSONObject();

                for (String column : usersColumns.keySet()) {
                    DBUtil.jsonPutResultSetColumn(obj, column, resultSet, column, usersColumns.get(column));
                }

                String[] following = (resultSet.getString("following") != null) ? resultSet.getString("following").split(",") : new String[] { };
                String[] followers = (resultSet.getString("followers") != null) ? resultSet.getString("followers").split(",") : new String[] { };
                String[] subscriptions = (resultSet.getString("subscriptions") != null) ? resultSet.getString("subscriptions").split(",") : new String[] { };

                obj.put("following", following);
                obj.put("followers", followers);
                obj.put("subscriptions", subscriptions);
                obj.put("username", DBUtil.jsonNullable(obj.getString("username")));
                obj.put("about", DBUtil.jsonNullable(obj.getString("about")));
                obj.put("name", DBUtil.jsonNullable(obj.getString("name")));
                response.add(obj);
            }

            result.put("code", 0);
            result.put("response", response);
        } catch (SQLException | JSONException e) {
            e.printStackTrace();
        }

        return result;
    }

    public JSONObject follow(String followerEmail, String followeeEmail) {
        JSONObject result = new JSONObject();

        try {
            PreparedStatement stmt = connection.prepareStatement("INSERT INTO followers (users_email_following, users_email_follower) VALUES (?, ?)");

            stmt.setString(1, followeeEmail);
            stmt.setString(2, followerEmail);

            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        try {
            result.put("code", 0);
            result.put("response", jsonObject(connection, followerEmail));
        } catch (JSONException e) { }

        return result;
    }

    public JSONObject listFollowers(String userEmail, int limit, String order, int since_id) {
        return listFollowersWithArguments("SELECT id, username, about, name, email, isAnonymous FROM followers JOIN users ON email = users_email_follower WHERE users_email_following = ?", userEmail, limit, order, since_id);
    }

    public JSONObject listFollowing(String userEmail, int limit, String order, int since_id) {
        return listFollowersWithArguments("SELECT id, username, about, name, email, isAnonymous FROM followers JOIN users ON email = users_email_following WHERE users_email_follower = ?", userEmail, limit, order, since_id);
    }

    private JSONObject listFollowersWithArguments(String initialQuery, String userEmail, int limit, String order, int since_id) {
        StringBuilder queryBuilder = new StringBuilder(initialQuery);

        if (since_id != 0) {
            queryBuilder.append(" AND id >= ?");
        }

        if (order != null && (order.equals("asc") || order.equals("desc"))) {
            queryBuilder.append(" ORDER BY name " + order.toUpperCase());
        }

        if (limit != 0) {
            queryBuilder.append(" LIMIT 0, " + limit);
        }

        try {
            PreparedStatement stmt = connection.prepareStatement(queryBuilder.toString());

            stmt.setString(1, userEmail);

            if (since_id != 0) {
                stmt.setInt(2, since_id);
            }

            return listFollowersWithStatement(stmt);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    private JSONObject listFollowersWithStatement(PreparedStatement stmt) {
        JSONObject result = new JSONObject();
        List<JSONObject> response = new ArrayList<JSONObject>();

        try {
            ResultSet user = stmt.executeQuery();

            while (user.next()) {
                JSONObject jsonObject = new JSONObject();

                jsonObject.put("id", user.getInt("id"));
                jsonObject.put("username", DBUtil.jsonNullable(user.getString("username")));
                jsonObject.put("about", DBUtil.jsonNullable(user.getString("about")));
                jsonObject.put("name", DBUtil.jsonNullable(user.getString("name")));
                jsonObject.put("email", user.getString("email"));
                jsonObject.put("isAnonymous", user.getBoolean("isAnonymous"));
                jsonObject.put("followers", followers(connection, user.getString("email")));
                jsonObject.put("following", followings(connection, user.getString("email")));
                jsonObject.put("subscriptions", subscriptions(connection, user.getString("email")));

                response.add(jsonObject);
            }

            result.put("code", 0);
            result.put("response", response);
        } catch (SQLException | JSONException e) { }

        return result;
    }

    public JSONObject unfollow(String followerEmail, String followeeEmail) {
        JSONObject result = new JSONObject();

        try {
            PreparedStatement stmt = connection.prepareStatement("DELETE FROM followers WHERE users_email_following = ? AND users_email_follower = ?");

            stmt.setString(1, followeeEmail);
            stmt.setString(2, followerEmail);

            stmt.executeUpdate();
        } catch (SQLException e) { }

        try {
            result.put("code", 0);
            result.put("response", jsonObject(connection, followerEmail));
        } catch (JSONException e) { }

        return result;
    }

    public JSONObject updateProfile(String userEmail, String about, String name) {
        JSONObject result = new JSONObject();

        int affected = 0;

        try {
            PreparedStatement stmt = connection.prepareStatement("UPDATE users SET about = ?, name = ? WHERE email = ?");

            stmt.setString(1, about);
            stmt.setString(2, name);
            stmt.setString(3, userEmail);

            affected = stmt.executeUpdate();
        } catch (SQLException e) { }

        try {
            if (affected != 0) {
                result.put("code", 0);
                result.put("response", jsonObject(connection, userEmail));
            } else {
                result.put("code", 1);
                result.put("message", "user not found: " + userEmail);
            }
        } catch (JSONException e) { }

        return result;
    }

    public static JSONObject jsonObject(Connection connection, String userEmail) {
        JSONObject jsonObject = null;
        ResultSet user = null;

        try {
            PreparedStatement stmt = connection.prepareStatement("SELECT id, username, about, name, email, isAnonymous FROM users WHERE email = ?");

            stmt.setString(1, userEmail);

            user = DBUtil.getSingleResult(stmt);
        } catch (SQLException e) { }

        if (user != null) {
            try {
                jsonObject = new JSONObject();

                jsonObject.put("id", user.getInt("id"));
                jsonObject.put("username", DBUtil.jsonNullable(user.getString("username")));
                jsonObject.put("about", DBUtil.jsonNullable(user.getString("about")));
                jsonObject.put("name", DBUtil.jsonNullable(user.getString("name")));
                jsonObject.put("email", user.getString("email"));
                jsonObject.put("isAnonymous", user.getBoolean("isAnonymous"));
                jsonObject.put("followers", followers(connection, userEmail));
                jsonObject.put("following", followings(connection, userEmail));
                jsonObject.put("subscriptions", subscriptions(connection, userEmail));
            } catch (JSONException | SQLException e) { }
        }

        return jsonObject;
    }

    public static List<String> followers(Connection connection, String userEmail) {
        List<String> followers = new ArrayList<String>();

        try {
            PreparedStatement stmt = connection.prepareStatement("SELECT users_email_follower FROM followers WHERE users_email_following = ?");

            stmt.setString(1, userEmail);

            ResultSet resultSet = stmt.executeQuery();

            while (resultSet.next()) {
                followers.add(resultSet.getString(1));
            }
        } catch (SQLException e) { }

        return followers;
    }

    public static List<String> followings(Connection connection, String userEmail) {
        List<String> followings = new ArrayList<String>();

        try {
            PreparedStatement stmt = connection.prepareStatement("SELECT users_email_following FROM followers WHERE users_email_follower = ?");

            stmt.setString(1, userEmail);

            ResultSet resultSet = stmt.executeQuery();

            while (resultSet.next()) {
                followings.add(resultSet.getString(1));
            }
        } catch (SQLException e) { }

        return followings;
    }

    public static List<Integer> subscriptions(Connection connection, String userEmail) {
        List<Integer> subscriptions = new ArrayList<Integer>();

        try {
            PreparedStatement stmt = connection.prepareStatement("SELECT threads_id FROM subscriptions WHERE users_email = ?");

            stmt.setString(1, userEmail);

            ResultSet resultSet = stmt.executeQuery();

            while (resultSet.next()) {
                subscriptions.add(resultSet.getInt(1));
            }
        } catch (SQLException e) { }

        return subscriptions;
    }


}


