package api;

import db.DBUtil;
import org.json.JSONException;
import org.json.JSONObject;
import java.sql.*;
import java.util.*;

/**
 * @author d.kildishev
 */
public class Post {
    Connection connection = null;

    Post(Connection connection) {
        this.connection = connection;

        try {
            connection.setAutoCommit(true);
        } catch (SQLException e) { }
    }

    public JSONObject create(String forumShortName, int threadId, String userEmail, String date, String message, boolean isApproved, boolean isHighlighted, boolean isEdited, boolean isSpam, boolean isDeleted, int parent) {
        JSONObject result = new JSONObject();
        JSONObject response = new JSONObject();

        long id = 0;

        try {
            PreparedStatement stmt = connection.prepareStatement("INSERT INTO posts (message, date, isApproved, isHighlighted, isEdited, isSpam, isDeleted, parent, users_email, threads_id, forums_short_name) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);

            stmt.setString(1, message);
            stmt.setString(2, date);
            stmt.setBoolean(3, isApproved);
            stmt.setBoolean(4, isHighlighted);
            stmt.setBoolean(5, isEdited);
            stmt.setBoolean(6, isSpam);
            stmt.setBoolean(7, isDeleted);
            stmt.setInt(8, parent);
            stmt.setString(9, userEmail);
            stmt.setInt(10, threadId);
            stmt.setString(11, forumShortName);

            stmt.executeUpdate();
            id = DBUtil.getStatementGeneratedId(stmt);
        } catch (SQLException e) { }

        try {
            PreparedStatement stmt = connection.prepareStatement("UPDATE threads SET posts = posts + 1 WHERE id = ?");

            stmt.setInt(1, threadId);

            stmt.executeUpdate();
        } catch (SQLException e) { }

        try {
            result.put("code", 0);
            result.put("response", response);
            response.put("id", id);
            response.put("message", message);
            response.put("date", date);
            response.put("isApproved", isApproved);
            response.put("isHighlighted", isHighlighted);
            response.put("isEdited", isEdited);
            response.put("isSpam", isSpam);
            response.put("isDeleted", isDeleted);
            response.put("user", userEmail);
            response.put("thread", threadId);
            response.put("forum", forumShortName);
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
                result.put("message", "post not found: " + id);
            }
        } catch (JSONException e) { }

        return result;
    }

    public JSONObject list(String clauseField, Object clauseValue, String since, int limit, String order, List<String> related) {
        JSONObject result = new JSONObject();
        List<JSONObject> response = new ArrayList<JSONObject>();
        StringBuilder query = new StringBuilder("SELECT ");

        HashMap<String, Class> postsColumns = new HashMap<String, Class>(){{
            put("id", Integer.class);
            put("message", String.class);
            put("date", String.class);
            put("likes", Integer.class);
            put("dislikes", Integer.class);
            put("points", Integer.class);
            put("isApproved", Boolean.class);
            put("isHighlighted", Boolean.class);
            put("isEdited", Boolean.class);
            put("isSpam", Boolean.class);
            put("isDeleted", Boolean.class);
            put("parent", Integer.class);
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
            postsColumns.put("users_email", String.class);
        }

        HashMap<String, Class> threadsColumns = null;

        if (related != null && related.contains("thread")) {
            threadsColumns = new HashMap<String, Class>(){{
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
                put("forums_short_name", String.class);
                put("users_email", String.class);
            }};
        } else {
            postsColumns.put("threads_id", Integer.class);
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
            postsColumns.put("forums_short_name", String.class);
        }

        query.append(DBUtil.columnsOfTableToString("posts", postsColumns.keySet().toArray(new String[postsColumns.keySet().size()])));

        if (usersColumns != null) {
            query.append(", ");
            query.append(DBUtil.columnsOfTableToString("users", usersColumns.keySet().toArray(new String[usersColumns.keySet().size()])));
        }

        if (threadsColumns != null) {
            query.append(", ");
            query.append(DBUtil.columnsOfTableToString("threads", threadsColumns.keySet().toArray(new String[threadsColumns.keySet().size()])));
        }

        if (forumsColumns != null) {
            query.append(", ");
            query.append(DBUtil.columnsOfTableToString("forums", forumsColumns.keySet().toArray(new String[forumsColumns.keySet().size()])));
        }

        query.append(" FROM posts");

        if (related != null && related.contains("user")) {
            query.append(" JOIN users ON posts.users_email = users.email");
        }

        if (related != null && related.contains("thread")) {
            query.append(" JOIN threads ON posts.threads_id = threads.id");
        }

        if (related != null && related.contains("forum")) {
            query.append(" JOIN forums ON posts.forums_short_name = forums.short_name");
        }

        query.append(" WHERE posts." + clauseField + " = ?");

        if (since != null) {
            query.append(" AND posts.date >= ?");
        }

        if (order != null && (order.equals("asc") || order.equals("desc"))) {
            query.append(" ORDER BY posts.date " + order.toUpperCase());
        }

        if (limit != 0) {
            query.append(" LIMIT 0, " + limit);
        }

        try {
            PreparedStatement stmt = connection.prepareStatement(query.toString());

            if (clauseField.equals("forums_short_name") || clauseField.equals("users_email")) {
                stmt.setString(1, (String)clauseValue);
            } else if (clauseField.equals("threads_id")) {
                stmt.setInt(1, (Integer)clauseValue);
            }

            if (since != null) {
                stmt.setString(2, since);
            }

            ResultSet resultSet = stmt.executeQuery();

            while (resultSet.next()) {
                JSONObject obj = new JSONObject();

                for (String column : postsColumns.keySet()) {
                    String name = column;

                    if (column.equals("users_email")) {
                        name = "user";
                    } else if (column.equals("threads_id")) {
                        name = "thread";
                    } else if (column.equals("forums_short_name")) {
                        name = "forum";
                    }

                    DBUtil.jsonPutResultSetColumn(obj, name, resultSet, column, postsColumns.get(column));
                }

                obj.put("date", obj.getString("date").split("\\.")[0]);
                obj.put("parent", DBUtil.jsonNullable(obj.getInt("parent")));

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

                if (threadsColumns != null) {
                    JSONObject relatedObj = new JSONObject();

                    for (String column : threadsColumns.keySet()) {
                        String name = column;

                        if (column.equals("users_email")) {
                            name = "user";
                        } else if (column.equals("forums_short_name")) {
                            name = "forum";
                        }

                        DBUtil.jsonPutResultSetColumn(relatedObj, name, resultSet, "threads." + column, threadsColumns.get(column));
                    }

                    relatedObj.put("date", relatedObj.getString("date").split("\\.")[0]);
                    obj.put("thread", relatedObj);
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
            PreparedStatement stmt = connection.prepareStatement("UPDATE posts SET isDeleted = ? WHERE id = ?");

            stmt.setBoolean(1, isDeleted);
            stmt.setInt(2, id);

            affected = stmt.executeUpdate();
        } catch (SQLException e) { }

        try {
            if (affected != 0) {
                result.put("code", 0);
                result.put("response", response);
                response.put("post", id);
            } else {
                result.put("code", 1);
                result.put("message", "post not found: " + id);
            }
        } catch (JSONException e) { }

        return result;
    }

    public JSONObject update(int id, String message) {
        JSONObject result = new JSONObject();

        int affected = 0;

        try {
            PreparedStatement stmt = connection.prepareStatement("UPDATE posts SET message = ? WHERE id = ?");

            stmt.setString(1, message);
            stmt.setInt(2, id);

            affected = stmt.executeUpdate();
        } catch (SQLException e) { }

        try {
            if (affected != 0) {
                result.put("code", 0);
                result.put("response", jsonObject(connection, id, null));
            } else {
                result.put("code", 1);
                result.put("message", "post not found: " + id);
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
                stmt = connection.prepareStatement("UPDATE posts SET likes = likes + 1, points = points + 1 WHERE id = ?");
            } else if (vote == -1) {
                stmt = connection.prepareStatement("UPDATE posts SET dislikes = dislikes + 1, points = points - 1 WHERE id = ?");
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
                result.put("message", "post not found: " + id);
            }
        } catch (JSONException e) { }

        return result;
    }

    public static JSONObject jsonObject(Connection connection, int id, List<String> related) {
        JSONObject jsonObject = null;
        ResultSet post = null;

        try {
            PreparedStatement stmt = connection.prepareStatement("SELECT id, message, date, likes, dislikes, points, isApproved, isHighlighted, isEdited, isSpam, isDeleted, parent, users_email, threads_id, forums_short_name FROM posts WHERE id = ?");

            stmt.setInt(1, id);

            post = DBUtil.getSingleResult(stmt);
        } catch (SQLException e) { }

        if (post != null) {
            try {
                jsonObject = new JSONObject();

                jsonObject.put("id", id);
                jsonObject.put("message", post.getString("message"));
                jsonObject.put("date", post.getString("date").split("\\.")[0]);
                jsonObject.put("likes", post.getInt("likes"));
                jsonObject.put("dislikes", post.getInt("dislikes"));
                jsonObject.put("points", post.getInt("points"));
                jsonObject.put("isApproved", post.getBoolean("isApproved"));
                jsonObject.put("isHighlighted", post.getBoolean("isHighlighted"));
                jsonObject.put("isEdited", post.getBoolean("isEdited"));
                jsonObject.put("isSpam", post.getBoolean("isSpam"));
                jsonObject.put("isDeleted", post.getBoolean("isDeleted"));
                jsonObject.put("parent", DBUtil.jsonNullable(post.getInt("parent")));
                jsonObject.put("user", post.getString("users_email"));
                jsonObject.put("thread", post.getInt("threads_id"));
                jsonObject.put("forum", post.getString("forums_short_name"));

                if (related != null && related.contains("user")) {
                    jsonObject.put("user", User.jsonObject(connection, post.getString("users_email")));
                }

                if (related != null && related.contains("forum")) {
                    jsonObject.put("forum", Forum.jsonObject(connection, post.getString("forums_short_name"), null));
                }

                if (related != null && related.contains("thread")) {
                    jsonObject.put("thread", Thread.jsonObject(connection, post.getInt("threads_id"), null));
                }
            } catch (JSONException | SQLException e) { }
        }

        return jsonObject;
    }
}