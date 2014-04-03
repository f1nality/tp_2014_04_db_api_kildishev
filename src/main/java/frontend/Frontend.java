package frontend;

import api.Api;
import org.apache.commons.lang3.StringUtils;
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
    Api api = null;

    public Frontend() {
        api = new Api();
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
            String line;

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
                parameters.put(key, StringUtils.join(getParameters.get(key), ","));
            }
        }

        return parameters;
    }

    private void doApiMethod(HttpServletRequest httpRequest, HttpServletResponse httpResponse, HttpMethod httpMethod, String entity, String method) {
        Map<String, String> apiParameters = getApiParameters(httpRequest, httpMethod);
        JSONObject result = api.method(entity, method, apiParameters);

        try {
            httpResponse.getWriter().write(result.toString());
        } catch (IOException e) { }
    }
}
