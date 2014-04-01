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

    private void doApiMethod(HttpServletRequest httpRequest, HttpServletResponse httpResponse, HttpMethod httpMethod, String entity, String method) {
        JSONObject result = null;
        //TODO:arguments
        //TODO:method check

        HashMap<String, String> arguments = new HashMap<String, String>();

        if (httpMethod.equals("POST")) {
            try {
                BufferedReader reader = httpRequest.getReader();
                JSONObject json = new JSONObject(reader.readLine());
                
                System.out.println(json);
            } catch (IOException | JSONException e) { }
        }

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
