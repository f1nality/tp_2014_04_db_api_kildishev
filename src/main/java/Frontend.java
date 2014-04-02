import com.sun.org.apache.xpath.internal.operations.Bool;
import org.eclipse.jetty.http.HttpMethod;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author d.kildishev
 */
public class Frontend extends HttpServlet {
    User user = null;
    Forum forum = null;
    Thread thread = null;
    Post post = null;

    Frontend() {
        user = new User(DBUtil.openConnection());
        forum = new Forum(DBUtil.openConnection());
        thread = new Thread(DBUtil.openConnection());
        post = new Post(DBUtil.openConnection());
    }
    
    public void doGet(HttpServletRequest request, HttpServletResponse response) {
        parseApiRequest(request, response, HttpMethod.GET);
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) {
        parseApiRequest(request, response, HttpMethod.POST);
    }

    private void parseApiRequest(HttpServletRequest httpRequest, HttpServletResponse httpResponse, HttpMethod httpMethod) {
        Pattern p = Pattern.compile("/db/api/([^/]+)/([\\w]+)/([\\w]+)/");
        Matcher m = p.matcher(httpRequest.getRequestURI());

        if (!m.matches()) {
            try {
                httpResponse.getWriter().write("not api request");
            } catch (IOException e) { }

            return;
        }

        String entity = m.group(2);
        String method = m.group(3);

        doApiMethod(httpRequest, httpResponse, httpMethod, entity, method);
    }

    private Map<String, String> getApiParameters(HttpServletRequest httpRequest, HttpMethod httpMethod) {
        Map<String, String> parameters = new HashMap<String, String>();

        if (httpMethod == HttpMethod.POST) {
            StringBuffer body = new StringBuffer();
            String line = null;

            try {
                BufferedReader reader = httpRequest.getReader();
                while ((line = reader.readLine()) != null) {
                    body.append(line);
                }
            } catch (Exception e) { }

            try {
                JSONObject bodyJson = new JSONObject(body.toString());

                Iterator<String> iterator = bodyJson.keys();
                while (iterator.hasNext()) {
                    String key = iterator.next();

                    parameters.put(key, bodyJson.getString(key));
                }
            } catch (JSONException e) { }
        } else {
            Map<String, String[]> getParameters = httpRequest.getParameterMap();

            for (String key : getParameters.keySet()) {
                parameters.put(key, getParameters.get(key)[0]);
            }
        }

        return parameters;
    }

    private void doApiMethod(HttpServletRequest httpRequest, HttpServletResponse httpResponse, HttpMethod httpMethod, String entity, String method) {
        JSONObject result = null;
        Map<String, String> apiParameters = getApiParameters(httpRequest, httpMethod);
        //TODO:method check
        if (entity.equals("Forum")) {
            if (method.equals("create")) {
                result = forum.create(apiParameters.get("name"), apiParameters.get("short_name"), apiParameters.get("userEmail"));
            } else if (method.equals("details")) {
                List<String> related = (apiParameters.get("related") != null ? Arrays.asList(apiParameters.get("related").split(",")) : null);
                result = forum.details(apiParameters.get("forum"), related);
            } else if (method.equals("listPosts")) {
                List<String> related = (apiParameters.get("related") != null ? Arrays.asList(apiParameters.get("related").split(",")) : null);
                result = post.list("forums_short_name", apiParameters.get("forum"), apiParameters.get("since"), (apiParameters.get("limit") != null ? Integer.parseInt(apiParameters.get("limit")) : 0), apiParameters.get("order"), related);
            } else if (method.equals("listThreads")) {
                String field = (apiParameters.get("forum") != null) ? "forums_short_name" : "users_email";
                String value = (apiParameters.get("forum") != null) ? apiParameters.get("forum") : apiParameters.get("user");
                List<String> related = (apiParameters.get("related") != null ? Arrays.asList(apiParameters.get("related").split(",")) : null);
                result = thread.list(field, value, apiParameters.get("since"), (apiParameters.get("limit") != null ? Integer.parseInt(apiParameters.get("since")) : 0), apiParameters.get("order"), related);
            } else if (method.equals("listUsers")) {
                result = user.list("forums_short_name", apiParameters.get("forum"), (apiParameters.get("since_id") != null ? Integer.parseInt(apiParameters.get("since_id")) : 0), (apiParameters.get("limit") != null ? Integer.parseInt(apiParameters.get("since")) : 0), apiParameters.get("order"));
            }
        } else if (entity.equals("Post")) {
            if (method.equals("create")) {
                Boolean isApproved = (apiParameters.get("isApproved")) != null ? Boolean.parseBoolean(apiParameters.get("isApproved")) : false;
                Boolean isHighlighted = (apiParameters.get("isHighlighted")) != null ? Boolean.parseBoolean(apiParameters.get("isHighlighted")) : true;
                Boolean isEdited = (apiParameters.get("isEdited") != null) ? Boolean.parseBoolean(apiParameters.get("isEdited")) : true;
                Boolean isSpam = (apiParameters.get("isSpam") != null) ? Boolean.parseBoolean(apiParameters.get("isSpam")) : false;
                Boolean isDeleted = (apiParameters.get("isDeleted") != null) ? Boolean.parseBoolean(apiParameters.get("isDeleted")) : false;
                result = post.create(apiParameters.get("forum"), Integer.parseInt(apiParameters.get("thread")), apiParameters.get("user"), apiParameters.get("date"), apiParameters.get("message"), isApproved, isHighlighted, isEdited, isSpam, isDeleted, 0);
            } else if (method.equals("details")) {
                List<String> related = (apiParameters.get("related") != null ? Arrays.asList(apiParameters.get("related").split(",")) : null);
                result = post.details(Integer.parseInt(apiParameters.get("post")), related);
            } else if (method.equals("list")) {
                String field = (apiParameters.get("forum") != null) ? "forums_short_name" : "threads_id";
                String value = (apiParameters.get("forum") != null) ? apiParameters.get("forum") : apiParameters.get("thread");
                Integer limit = (apiParameters.get("limit") != null) ? Integer.parseInt(apiParameters.get("limit")) : 0;
                List<String> related = (apiParameters.get("related") != null ? Arrays.asList(apiParameters.get("related").split(",")) : null);
                result = post.list(field, value, apiParameters.get("since"), limit, apiParameters.get("order"), related);
            } else if (method.equals("remove")) {
                result = post.remove(Integer.parseInt(apiParameters.get("post")));
            } else if (method.equals("restore")) {
                result = post.restore(Integer.parseInt(apiParameters.get("post")));
            } else if (method.equals("update")) {
                result = post.update(Integer.parseInt(apiParameters.get("post")), apiParameters.get("message"));
            } else if (method.equals("vote")) {
                result = post.vote(Integer.parseInt(apiParameters.get("post")), Integer.parseInt(apiParameters.get("vote")));
            }
        } else if (entity.equals("User")) {
            if (method.equals("create")) {
                Boolean isAnonymous = (apiParameters.get("isAnonymous")) != null ? Boolean.parseBoolean(apiParameters.get("isAnonymous")) : false;
                result = user.create(apiParameters.get("username"), apiParameters.get("about"), apiParameters.get("name"), apiParameters.get("email"), isAnonymous);
            } else if (method.equals("details")) {
                result = user.details(apiParameters.get("user"));
            } else if (method.equals("follow")) {
                result = user.follow(apiParameters.get("follower"), apiParameters.get("followee"));
            } else if (method.equals("listFollowers")) {
                Integer limit = (apiParameters.get("limit") != null) ? Integer.parseInt(apiParameters.get("limit")) : 0;
                Integer since_id = (apiParameters.get("since_id") != null) ? Integer.parseInt(apiParameters.get("since_id")) : 0;
                result = user.listFollowers(apiParameters.get("user"), limit, apiParameters.get("order"), since_id);
            } else if (method.equals("listFollowing")) {
                Integer limit = (apiParameters.get("limit") != null) ? Integer.parseInt(apiParameters.get("limit")) : 0;
                Integer since_id = (apiParameters.get("since_id") != null) ? Integer.parseInt(apiParameters.get("since_id")) : 0;
                result = user.listFollowing(apiParameters.get("user"), limit, apiParameters.get("order"), since_id);
            } else if (method.equals("listPosts")) {
                Integer limit = (apiParameters.get("limit") != null) ? Integer.parseInt(apiParameters.get("limit")) : 0;
                List<String> related = (apiParameters.get("related") != null ? Arrays.asList(apiParameters.get("related").split(",")) : null);
                result = post.list("users_email", apiParameters.get("user"), apiParameters.get("since"), limit, apiParameters.get("order"), related);
            } else if (method.equals("unfollow")) {
                result = user.unfollow(apiParameters.get("follower"), apiParameters.get("followee"));
            } else if (method.equals("updateProfile")) {
                result = user.updateProfile(apiParameters.get("user"), apiParameters.get("about"), apiParameters.get("name"));
            }
        } else if (entity.equals("Thread")) {
            if (method.equals("close")) {
                result = thread.close(Integer.parseInt(apiParameters.get("thread")));
            } else if (method.equals("create")) {
                Boolean isDeleted = (apiParameters.get("isDeleted") != null) ? Boolean.parseBoolean(apiParameters.get("isDeleted")) : false;
                result = thread.create(apiParameters.get("forum"), apiParameters.get("title"), apiParameters.get("user"), apiParameters.get("date"), apiParameters.get("message"), apiParameters.get("slug"), Boolean.parseBoolean(apiParameters.get("isClosed")), isDeleted);
            } else if (method.equals("details")) {
                List<String> related = (apiParameters.get("related") != null ? Arrays.asList(apiParameters.get("related").split(",")) : null);
                result = thread.details(Integer.parseInt(apiParameters.get("thread")), related);
            } else if (method.equals("list")) {
                String field = (apiParameters.get("forum") != null) ? "forums_short_name" : "users_email";
                String value = (apiParameters.get("forum") != null) ? apiParameters.get("forum") : apiParameters.get("user");
                Integer limit = (apiParameters.get("limit") != null) ? Integer.parseInt(apiParameters.get("limit")) : 0;
                List<String> related = (apiParameters.get("related") != null ? Arrays.asList(apiParameters.get("related").split(",")) : null);
                result = thread.list(field, value, apiParameters.get("since"), limit, apiParameters.get("order"), related);
            } else if (method.equals("listPosts")) {
                Integer limit = (apiParameters.get("limit") != null) ? Integer.parseInt(apiParameters.get("limit")) : 0;
                result = post.list("threads_id", Integer.parseInt(apiParameters.get("thread")), apiParameters.get("since"), limit, apiParameters.get("order"), null);
            } else if (method.equals("open")) {
                result = thread.open(Integer.parseInt(apiParameters.get("thread")));
            } else if (method.equals("remove")) {
                result = thread.remove(Integer.parseInt(apiParameters.get("thread")));
            } else if (method.equals("restore")) {
                result = thread.restore(Integer.parseInt(apiParameters.get("thread")));
            } else if (method.equals("subscribe")) {
                result = thread.subscribe(apiParameters.get("user"), Integer.parseInt(apiParameters.get("thread")));
            } else if (method.equals("unsubscribe")) {
                result = thread.unsubscribe(apiParameters.get("user"), Integer.parseInt(apiParameters.get("thread")));
            } else if (method.equals("update")) {
                result = thread.update(Integer.parseInt(apiParameters.get("thread")), apiParameters.get("message"), apiParameters.get("slug"));
            } else if (method.equals("vote")) {
                result = thread.vote(Integer.parseInt(apiParameters.get("thread")), Integer.parseInt(apiParameters.get("vote")));
            }
        }
/*
        if (entity.equals("Forum")) {
            if (method.equals("create")) {
                result = forum.create("Forum With Sufficiently Large Name", "forumwithsufficientlylargename", "example@mail.ru");
            } else if (method.equals("details")) {
                result = forum.details("forumwithsufficientlylargename", Arrays.asList("user"));
            } else if (method.equals("listPosts")) {
                result = post.list("forums_short_name", "forumwithsufficientlylargename", null, 500, "desc", Arrays.asList("user", "forum", "thread"));
            } else if (method.equals("listThreads")) {
                result = thread.list("forums_short_name", "forumwithsufficientlylargename", null, 500, "desc", Arrays.asList("user", "forum"));
            } else if (method.equals("listUsers")) {
                result = user.list("forums_short_name", "forumwithsufficientlylargename", 1, 500, "desc");
            }
        } else if (entity.equals("Post")) {
            if (method.equals("create")) {
                result = post.create("forumwithsufficientlylargename", 8, "example@mail.ru", "2014-01-01 00:00:01", "my message 1", false, true, true, false, false, 0);
            } else if (method.equals("details")) {
                result = post.details(4, Arrays.asList("user", "thread", "forum"));
            } else if (method.equals("list")) {
                result = post.list("forums_short_name", "forumwithsufficientlylargename", null, 500, "desc", Arrays.asList("user", "thread", "forum"));
            } else if (method.equals("remove")) {
                result = post.remove(8);
            } else if (method.equals("restore")) {
                result = post.restore(8);
            } else if (method.equals("update")) {
                result = post.update(8, "new message");
            } else if (method.equals("vote")) {
                result = post.vote(8, -1);
            }
        } else if (entity.equals("User")) {
            if (method.equals("create")) {
                result = user.create("user1", "hello im user1", "user", "example@mail.ru", false);
            } else if (method.equals("details")) {
                result = user.details("example@mail.ru1");
            } else if (method.equals("follow")) {
                result = user.follow("example@mail.ru", "example2@mail.ru");
            } else if (method.equals("listFollowers")) {
                result = user.listFollowers("example@mail.ru", 0, "desc", 0);
            } else if (method.equals("listFollowing")) {
                result = user.listFollowing("example@mail.ru", 0, "desc", 0);
            } else if (method.equals("listPosts")) {
                result = post.list("users_email", "example@mail.ru", null, 500, "desc", null);
            } else if (method.equals("unfollow")) {
                result = user.unfollow("example@mail.ru", "example2@mail.ru");
            } else if (method.equals("updateProfile")) {
                result = user.updateProfile("example@mail.ru", "new about", "new name");
            }
        } else if (entity.equals("Thread")) {
            if (method.equals("close")) {
                result = thread.close(1);
            } else if (method.equals("create")) {
                result = thread.create("forumwithsufficientlylargename", "Thread With Sufficiently Large Title", "example@mail.ru", "2014-01-01 00:00:01", "hey hey hey hey!", "Threadwithsufficientlylargetitle", false, false);
            } else if (method.equals("details")) {
                result = thread.details(1, null);
            } else if (method.equals("list")) {
                result = thread.list("forums_short_name", "forumwithsufficientlylargename", null, 500, "desc", Arrays.asList("user", "forum"));
            } else if (method.equals("listPosts")) {
                result = post.list("threads_id", 8, null, 500, "desc", null);
            } else if (method.equals("open")) {
                result = thread.open(1);
            } else if (method.equals("remove")) {
                result = thread.remove(1);
            } else if (method.equals("restore")) {
                result = thread.restore(1);
            } else if (method.equals("subscribe")) {
                result = thread.subscribe("example@mail.ru", 1);
            } else if (method.equals("unsubscribe")) {
                result = thread.unsubscribe("example@mail.ru", 1);
            } else if (method.equals("update")) {
                result = thread.update(1, "new thread message", "new slug");
            } else if (method.equals("vote")) {
                result = thread.vote(1, -1);
            }
        }
*/
        if (result == null) {
            result = new JSONObject();

            try {
                result.put("code", 1);
                result.put("message", "unknown api entity or method");
            } catch (JSONException e) { }
        }

        try {
            httpResponse.getWriter().write(result.toString());
        } catch (IOException e) { }
    }
}
