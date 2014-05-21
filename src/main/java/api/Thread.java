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
public class Thread {
    Connection connection = null;

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public JSONObject close(int id) {
        return setClosed(id, true);
    }

    private JSONObject setClosed(int id, boolean isClosed) {
        JSONObject result = new JSONObject();
        JSONObject response = new JSONObject();

        int affected = 0;

        try {
            PreparedStatement stmt = connection.prepareStatement("UPDATE threads SET isClosed = ? WHERE id = ?");

            stmt.setBoolean(1, isClosed);
            stmt.setInt(2, id);

            affected = stmt.executeUpdate();
        } catch (SQLException e) { }

        try {
            if (affected != 0) {
                result.put("code", 0);
                result.put("response", response);
                response.put("thread", id);
            } else {
                result.put("code", 1);
                result.put("message", "thread not found: " + id);
            }
        } catch (JSONException e) { }

        return result;
    }

    public JSONObject create(String forumShortName, String title, String userEmail, String date, String message, String slug, boolean isClosed, boolean isDeleted) {
        JSONObject result = new JSONObject();
        JSONObject response = new JSONObject();

        long id = 0;

        try {
            PreparedStatement stmt = connection.prepareStatement("INSERT INTO threads (title, slug, message, date, isClosed, isDeleted, forums_short_name, users_email) VALUES (?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);

            stmt.setString(1, title);
            stmt.setString(2, slug);
            stmt.setString(3, message);
            stmt.setString(4, date);
            stmt.setBoolean(5, isClosed);
            stmt.setBoolean(6, isDeleted);
            stmt.setString(7, forumShortName);
            stmt.setString(8, userEmail);

            stmt.executeUpdate();
            id = DBUtil.getStatementGeneratedId(stmt);
        } catch (SQLException e) { }

        try {
            result.put("code", 0);
            result.put("response", response);
            response.put("id", id);
            response.put("title", title);
            response.put("slug", slug);
            response.put("message", message);
            response.put("date", date);
            response.put("isClosed", isClosed);
            response.put("isDeleted", isDeleted);
            response.put("forum", forumShortName);
            response.put("user", userEmail);
        } catch (JSONException e) { }

        return result;
    }

    public JSONObject details(int id, List<String> related) {
        JSONObject result = new JSONObject();
        JSONObject response = jsonObject(connection, id, related);

        try {
            if (response != null) {
                result.put("code", 0);
                result.put("response", response);
            } else {
                result.put("code", 1);
                result.put("message", "thread not found: " + id);
            }
        } catch (JSONException e) { }

        return result;
    }

    public JSONObject list(String clauseField, String clauseValue, String since, int limit, String order, List<String> related) {
        JSONObject result = new JSONObject();
        List<JSONObject> response = new ArrayList<JSONObject>();
        StringBuilder query = new StringBuilder("SELECT ");

        HashMap<String, Class> threadsColumns = new HashMap<String, Class>(){{
            put("id", Integer.class);
            put("title", String.class);
            put("slug", String.class);
            put("message", String.class);
            put("date", String.class);
            put("likes", Integer.class);
            put("dislikes", Integer.class);
            put("points", Integer.class);
            put("isClosed", Boolean.class);
            put("isDeleted", Boolean.class);
            put("posts", Integer.class);
        }};

        HashMap<String, Class> usersColumns = null;

        if (related != null && related.contains("user")) {
            usersColumns = new HashMap<String, Class>(){{
                put("id", Integer.class);
                put("username", String.class);
                put("about", String.class);
                put("name", String.class);
                put("email", String.class);
                put("isAnonymous", Boolean.class);
            }};
        } else {
            threadsColumns.put("users_email", String.class);
        }

        HashMap<String, Class> forumsColumns = null;

        if (related != null && related.contains("forum")) {
            forumsColumns = new HashMap<String, Class>(){{
                put("id", Integer.class);
                put("name", String.class);
                put("short_name", String.class);
                put("users_email", String.class);
            }};
        } else {
            threadsColumns.put("forums_short_name", String.class);
        }

        query.append(DBUtil.columnsOfTableToString("threads", threadsColumns.keySet().toArray(new String[threadsColumns.keySet().size()])));

        if (usersColumns != null) {
            query.append(", ");
            query.append(DBUtil.columnsOfTableToString("users", usersColumns.keySet().toArray(new String[usersColumns.keySet().size()])));
        }

        if (forumsColumns != null) {
            query.append(", ");
            query.append(DBUtil.columnsOfTableToString("forums", forumsColumns.keySet().toArray(new String[forumsColumns.keySet().size()])));
        }

        query.append(" FROM threads");

        if (related != null && related.contains("user")) {
            query.append(" JOIN users ON threads.users_email = users.email");
        }

        if (related != null && related.contains("forum")) {
            query.append(" JOIN forums ON threads.forums_short_name = forums.short_name");
        }

        query.append(" WHERE threads." + clauseField + " = ?");

        if (since != null) {
            query.append(" AND threads.date >= ?");
        }

        if (order != null && (order.equals("asc") || order.equals("desc"))) {
            query.append(" ORDER BY threads.date " + order.toUpperCase());
        }

        if (limit != 0) {
            query.append(" LIMIT 0, " + limit);
        }

        try {
            PreparedStatement stmt = connection.prepareStatement(query.toString());

            stmt.setString(1, clauseValue);

            if (since != null) {
                stmt.setString(2, since);
            }

            ResultSet resultSet = stmt.executeQuery();

            while (resultSet.next()) {
                JSONObject obj = new JSONObject();

                for (String column : threadsColumns.keySet()) {
                    String name = column;

                    if (column.equals("users_email")) {
                        name = "user";

                    } else if (column.equals("forums_short_name")) {
                        name = "forum";
                    }

                    DBUtil.jsonPutResultSetColumn(obj, name, resultSet, column, threadsColumns.get(column));
                }

                obj.put("date", obj.getString("date").split("\\.")[0]);

                if (usersColumns != null) {
                    JSONObject relatedObj = new JSONObject();

                    for (String column : usersColumns.keySet()) {
                        DBUtil.jsonPutResultSetColumn(relatedObj, column, resultSet, "users." + column, usersColumns.get(column));
                        relatedObj.put("followers", User.followers(connection, resultSet.getString("users.email")));
                        relatedObj.put("following", User.followings(connection, resultSet.getString("users.email")));
                        relatedObj.put("subscriptions", User.subscriptions(connection, resultSet.getString("users.email")));
                    }

                    relatedObj.put("username", DBUtil.jsonNullable(relatedObj.getString("username")));
                    relatedObj.put("about", DBUtil.jsonNullable(relatedObj.getString("about")));
                    relatedObj.put("name", DBUtil.jsonNullable(relatedObj.getString("name")));
                    obj.put("user", relatedObj);
                }

                if (forumsColumns != null) {
                    JSONObject relatedObj = new JSONObject();

                    for (String column : forumsColumns.keySet()) {
                        String name = column;

                        if (column.equals("users_email")) {
                            name = "user";
                        }

                        DBUtil.jsonPutResultSetColumn(relatedObj, name, resultSet, "forums." + column, forumsColumns.get(column));
                    }

                    obj.put("forum", relatedObj);
                }

                response.add(obj);
            }

            result.put("code", 0);
            result.put("response", response);
        } catch (SQLException | JSONException e) {
            e.printStackTrace();
        }

        return result;
    }

    public JSONObject open(int id) {
        return setClosed(id, false);
    }

    public JSONObject remove(int id) {
        return setDeleted(id, true);
    }

    public JSONObject restore(int id) {
        return setDeleted(id, false);
    }

    private JSONObject setDeleted(int id, boolean isDeleted) {
        JSONObject result = new JSONObject();
        JSONObject response = new JSONObject();

        int affected = 0;

        try {
            PreparedStatement stmt = connection.prepareStatement("UPDATE threads SET isDeleted = ? WHERE id = ?");

            stmt.setBoolean(1, isDeleted);
            stmt.setInt(2, id);

            affected = stmt.executeUpdate();
        } catch (SQLException e) { }

        try {
            if (affected != 0) {
                result.put("code", 0);
                result.put("response", response);
                response.put("thread", id);
            } else {
                result.put("code", 1);
                result.put("message", "thread not found: " + id);
            }
        } catch (JSONException e) { }

        return result;
    }

    public JSONObject subscribe(String userEmail, int id) {
        JSONObject result = new JSONObject();

        try {
            PreparedStatement stmt = connection.prepareStatement("INSERT INTO subscriptions (threads_id, users_email) VALUES (?, ?)");

            stmt.setInt(1, id);
            stmt.setString(2, userEmail);

            stmt.executeUpdate();
        } catch (SQLException e) { }

        try {
            result.put("code", 0);
            result.put("response", jsonObject(connection, id, null));
        } catch (JSONException e) { }

        return result;
    }

    public JSONObject unsubscribe(String userEmail, int id) {
        JSONObject result = new JSONObject();

        try {
            PreparedStatement stmt = connection.prepareStatement("DELETE FROM subscriptions WHERE threads_id = ? AND users_email = ?");

            stmt.setInt(1, id);
            stmt.setString(2, userEmail);

            stmt.executeUpdate();
        } catch (SQLException e) { }

        try {
            result.put("code", 0);
            result.put("response", jsonObject(connection, id, null));
        } catch (JSONException e) { }

        return result;
    }

    public JSONObject update(int id, String message, String slug) {
        JSONObject result = new JSONObject();

        int affected = 0;

        try {
            PreparedStatement stmt = connection.prepareStatement("UPDATE threads SET message = ?, slug = ? WHERE id = ?");

            stmt.setString(1, message);
            stmt.setString(2, slug);
            stmt.setInt(3, id);

            affected = stmt.executeUpdate();
        } catch (SQLException e) { }

        try {
            if (affected != 0) {
                result.put("code", 0);
                result.put("response", jsonObject(connection, id, null));
            } else {
                result.put("code", 1);
                result.put("message", "thread not found: " + id);
            }
        } catch (JSONException e) { }

        return result;
    }

    public JSONObject vote(int id, int vote) {
        JSONObject result = new JSONObject();

        int affected = 0;

        try {
            PreparedStatement stmt = null;

            if (vote == 1) {
                stmt = connection.prepareStatement("UPDATE threads SET likes = likes + 1, points = points + 1 WHERE id = ?");
            } else if (vote == -1) {
                stmt = connection.prepareStatement("UPDATE threads SET dislikes = dislikes + 1, points = points - 1 WHERE id = ?");
            }

            stmt.setInt(1, id);

            affected = stmt.executeUpdate();
        } catch (SQLException e) { }

        try {
            if (affected != 0) {
                result.put("code", 0);
                result.put("response", jsonObject(connection, id, null));
            } else {
                result.put("code", 1);
                result.put("message", "thread not found: " + id);
            }
        } catch (JSONException e) { }

        return result;
    }

    public static JSONObject jsonObject(Connection connection, int id, List<String> related) {
        JSONObject jsonObject = null;
        ResultSet thread = null;

        try {
            PreparedStatement stmt = connection.prepareStatement("SELECT id, title, slug, message, date, likes, dislikes, points, isClosed, isDeleted, posts, forums_short_name, users_email FROM threads WHERE id = ?");

            stmt.setInt(1, id);

            thread = DBUtil.getSingleResult(stmt);
        } catch (SQLException e) { }

        if (thread != null) {
            try {
                jsonObject = new JSONObject();

                jsonObject.put("id", id);
                jsonObject.put("title", thread.getString("title"));
                jsonObject.put("slug", thread.getString("slug"));
                jsonObject.put("message", thread.getString("message"));
                jsonObject.put("date", thread.getString("date").split("\\.")[0]);
                jsonObject.put("likes", thread.getInt("likes"));
                jsonObject.put("dislikes", thread.getInt("dislikes"));
                jsonObject.put("points", thread.getInt("points"));
                jsonObject.put("isClosed", thread.getBoolean("isClosed"));
                jsonObject.put("isDeleted", thread.getBoolean("isDeleted"));
                jsonObject.put("posts", thread.getInt("posts"));
                jsonObject.put("forum",  thread.getString("forums_short_name"));
                jsonObject.put("user",  thread.getString("users_email"));

                if (related != null && related.contains("forum")) {
                    JSONObject forum = Forum.jsonObject(connection, thread.getString("forums_short_name"), null);
                    jsonObject.put("forum", forum);
                }

                if (related != null && related.contains("user")) {
                    jsonObject.put("user", User.jsonObject(connection, thread.getString("users_email")));
                }
            } catch (JSONException | SQLException e) {
                e.printStackTrace();
            }
        }

        return jsonObject;
    }
}


