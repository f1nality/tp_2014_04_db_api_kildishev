package api;

import db.DBUtil;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

/**
 * @author d.kildishev
 */
public class Api {
    User user = null;
    Forum forum = null;
    Thread thread = null;
    Post post = null;

    public Api() {
        user = new User();
        forum = new Forum();
        thread = new Thread();
        post = new Post();

        DBUtil.createConnectionPool(100);

        method("clear", null, null);
    }

    private JSONObject forumMethod(String method, Map<String, String> apiParameters) {
        JSONObject result = null;
        List<String> related;

        switch (method) {
            case "create":
                result = forum.create(apiParameters.get("name"), apiParameters.get("short_name"), apiParameters.get("user"));
                break;
            case "details":
                related = (apiParameters.get("related") != null ? Arrays.asList(apiParameters.get("related").split(",")) : null);
                result = forum.details(apiParameters.get("forum"), related);
                break;
            case "listPosts":
                related = (apiParameters.get("related") != null ? Arrays.asList(apiParameters.get("related").split(",")) : null);
                result = post.list("forums_short_name", apiParameters.get("forum"), apiParameters.get("since"), (apiParameters.get("limit") != null ? Integer.parseInt(apiParameters.get("limit")) : 0), apiParameters.get("order"), related);
                break;
            case "listThreads":
                String field = (apiParameters.get("forum") != null) ? "forums_short_name" : "users_email";
                String value = (apiParameters.get("forum") != null) ? apiParameters.get("forum") : apiParameters.get("user");
                related = (apiParameters.get("related") != null ? Arrays.asList(apiParameters.get("related").split(",")) : null);
                result = thread.list(field, value, apiParameters.get("since"), (apiParameters.get("limit") != null ? Integer.parseInt(apiParameters.get("limit")) : 0), apiParameters.get("order"), related);
                break;
            case "listUsers":
                result = user.list("forums_short_name", apiParameters.get("forum"), (apiParameters.get("since_id") != null ? Integer.parseInt(apiParameters.get("since_id")) : 0), (apiParameters.get("limit") != null ? Integer.parseInt(apiParameters.get("limit")) : 0), apiParameters.get("order"));
                break;
        }

        return result;
    }

    private JSONObject postMethod(String method, Map<String, String> apiParameters) {
        JSONObject result = null;
        List<String> related;

        switch (method) {
                case "create":
                    Boolean isApproved = (apiParameters.get("isApproved")) != null ? Boolean.parseBoolean(apiParameters.get("isApproved")) : false;
                    Boolean isHighlighted = (apiParameters.get("isHighlighted")) != null ? Boolean.parseBoolean(apiParameters.get("isHighlighted")) : true;
                    Boolean isEdited = (apiParameters.get("isEdited") != null) ? Boolean.parseBoolean(apiParameters.get("isEdited")) : true;
                    Boolean isSpam = (apiParameters.get("isSpam") != null) ? Boolean.parseBoolean(apiParameters.get("isSpam")) : false;
                    Boolean isDeleted = (apiParameters.get("isDeleted") != null) ? Boolean.parseBoolean(apiParameters.get("isDeleted")) : false;
                    Integer parent = (apiParameters.get("parent") != null) ? Integer.parseInt(apiParameters.get("parent")) : 0;
                    result = post.create(apiParameters.get("forum"), Integer.parseInt(apiParameters.get("thread")), apiParameters.get("user"), apiParameters.get("date"), apiParameters.get("message"), isApproved, isHighlighted, isEdited, isSpam, isDeleted, parent);
                    break;
                case "details":
                    related = (apiParameters.get("related") != null ? Arrays.asList(apiParameters.get("related").split(",")) : null);
                    result = post.details(Integer.parseInt(apiParameters.get("post")), related);
                    break;
                case "list":
                    String field = (apiParameters.get("forum") != null) ? "forums_short_name" : "threads_id";
                    Object value = (apiParameters.get("forum") != null) ? apiParameters.get("forum") : Integer.parseInt(apiParameters.get("thread"));
                    Integer limit = (apiParameters.get("limit") != null) ? Integer.parseInt(apiParameters.get("limit")) : 0;
                    related = (apiParameters.get("related") != null ? Arrays.asList(apiParameters.get("related").split(",")) : null);
                    result = post.list(field, value, apiParameters.get("since"), limit, apiParameters.get("order"), related);
                    break;
                case "remove":
                    result = post.remove(Integer.parseInt(apiParameters.get("post")));
                    break;
                case "restore":
                    result = post.restore(Integer.parseInt(apiParameters.get("post")));
                    break;
                case "update":
                    result = post.update(Integer.parseInt(apiParameters.get("post")), apiParameters.get("message"));
                    break;
                case "vote":
                    result = post.vote(Integer.parseInt(apiParameters.get("post")), Integer.parseInt(apiParameters.get("vote")));
                    break;
        }

        return result;
    }

    private JSONObject userMethod(String method, Map<String, String> apiParameters) {
        JSONObject result = null;
        Integer limit, since_id;

        switch (method) {
            case "create":
                Boolean isAnonymous = (apiParameters.get("isAnonymous")) != null ? Boolean.parseBoolean(apiParameters.get("isAnonymous")) : false;
                result = user.create(apiParameters.get("username"), apiParameters.get("about"), apiParameters.get("name"), apiParameters.get("email"), isAnonymous);
                break;
            case "details":
                result = user.details(apiParameters.get("user"));
                break;
            case "follow":
                result = user.follow(apiParameters.get("follower"), apiParameters.get("followee"));
                break;
            case "listFollowers":
                limit = (apiParameters.get("limit") != null) ? Integer.parseInt(apiParameters.get("limit")) : 0;
                since_id = (apiParameters.get("since_id") != null) ? Integer.parseInt(apiParameters.get("since_id")) : 0;
                result = user.listFollowers(apiParameters.get("user"), limit, apiParameters.get("order"), since_id);
                break;
            case "listFollowing":
                limit = (apiParameters.get("limit") != null) ? Integer.parseInt(apiParameters.get("limit")) : 0;
                since_id = (apiParameters.get("since_id") != null) ? Integer.parseInt(apiParameters.get("since_id")) : 0;
                result = user.listFollowing(apiParameters.get("user"), limit, apiParameters.get("order"), since_id);
                break;
            case "listPosts":
                limit = (apiParameters.get("limit") != null) ? Integer.parseInt(apiParameters.get("limit")) : 0;
                List<String> related = (apiParameters.get("related") != null ? Arrays.asList(apiParameters.get("related").split(",")) : null);
                result = post.list("users_email", apiParameters.get("user"), apiParameters.get("since"), limit, apiParameters.get("order"), related);
                break;
            case "unfollow":
                result = user.unfollow(apiParameters.get("follower"), apiParameters.get("followee"));
                break;
            case "updateProfile":
                result = user.updateProfile(apiParameters.get("user"), apiParameters.get("about"), apiParameters.get("name"));
                break;
        }

        return result;
    }

    private JSONObject threadMethod(String method, Map<String, String> apiParameters) {
        JSONObject result = null;
        Integer limit;
        List<String> related;

        switch (method) {
            case "close":
                result = thread.close(Integer.parseInt(apiParameters.get("thread")));
                break;
            case "create":
                Boolean isDeleted = (apiParameters.get("isDeleted") != null) ? Boolean.parseBoolean(apiParameters.get("isDeleted")) : false;
                result = thread.create(apiParameters.get("forum"), apiParameters.get("title"), apiParameters.get("user"), apiParameters.get("date"), apiParameters.get("message"), apiParameters.get("slug"), Boolean.parseBoolean(apiParameters.get("isClosed")), isDeleted);
                break;
            case "details":
                related = (apiParameters.get("related") != null ? Arrays.asList(apiParameters.get("related").split(",")) : null);
                result = thread.details(Integer.parseInt(apiParameters.get("thread")), related);
                break;
            case "list":
                String field = (apiParameters.get("forum") != null) ? "forums_short_name" : "users_email";
                String value = (apiParameters.get("forum") != null) ? apiParameters.get("forum") : apiParameters.get("user");
                limit = (apiParameters.get("limit") != null) ? Integer.parseInt(apiParameters.get("limit")) : 0;
                related = (apiParameters.get("related") != null ? Arrays.asList(apiParameters.get("related").split(",")) : null);
                result = thread.list(field, value, apiParameters.get("since"), limit, apiParameters.get("order"), related);
                break;
            case "listPosts":
                limit = (apiParameters.get("limit") != null) ? Integer.parseInt(apiParameters.get("limit")) : 0;
                result = post.list("threads_id", Integer.parseInt(apiParameters.get("thread")), apiParameters.get("since"), limit, apiParameters.get("order"), null);
                break;
            case "open":
                result = thread.open(Integer.parseInt(apiParameters.get("thread")));
                break;
            case "remove":
                result = thread.remove(Integer.parseInt(apiParameters.get("thread")));
                break;
            case "restore":
                result = thread.restore(Integer.parseInt(apiParameters.get("thread")));
                break;
            case "subscribe":
                result = thread.subscribe(apiParameters.get("user"), Integer.parseInt(apiParameters.get("thread")));
                break;
            case "unsubscribe":
                result = thread.unsubscribe(apiParameters.get("user"), Integer.parseInt(apiParameters.get("thread")));
                break;
            case "update":
                result = thread.update(Integer.parseInt(apiParameters.get("thread")), apiParameters.get("message"), apiParameters.get("slug"));
                break;
            case "vote":
                result = thread.vote(Integer.parseInt(apiParameters.get("thread")), Integer.parseInt(apiParameters.get("vote")));
                break;
        }

        return result;
    }

    public JSONObject method(String entity, String method, Map<String, String> apiParameters) {
        JSONObject result = null;

        Connection connection = DBUtil.getPoolConnection();

        forum.setConnection(connection);
        post.setConnection(connection);
        user.setConnection(connection);
        thread.setConnection(connection);

        switch (entity) {
            case "clear":
                DBUtil.truncate(connection);
                result = new JSONObject();
                break;
            case "forum":
                result = forumMethod(method, apiParameters);
                break;
            case "post":
                result = postMethod(method, apiParameters);
                break;
            case "user":
                result = userMethod(method, apiParameters);
                break;
            case "thread":
                result = threadMethod(method, apiParameters);
                break;
        }

        DBUtil.freePoolConnection(connection);
        
        if (result == null) {
            result = new JSONObject();

            try {
                result.put("code", 1);
                result.put("message", "unknown api entity or method");
            } catch (JSONException e) { }
        }

        return result;
    }
}
