package frontend;

import api.Api;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpMethod;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
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

                    //parameters.put(key, bodyJson.getString(key));
                    String value = bodyJson.getString(key);
                    parameters.put(key, parseApiRequestArray(value));
                }
            } catch (JSONException e) { }
        } else {
            Map<String, String[]> getParameters = httpRequest.getParameterMap();

            for (String key : getParameters.keySet()) {
                //parameters.put(key, StringUtils.join(getParameters.get(key), ","));

                String value = getParameters.get(key)[0];
                parameters.put(key, parseApiRequestArray(value));
            }
        }

        return parameters;
    }

    private String parseApiRequestArray(String array) {
        if (array.startsWith("[") && array.endsWith("]")) {
            StringBuilder result = new StringBuilder();
            String[] parsed = array.substring(1).substring(0, array.length() - 2).split(",");

            for (int i = 0; i < parsed.length; ++i) {
                parsed[i] = parsed[i].trim();
                String element = parsed[i].substring(1).substring(0, parsed[i].length() - 2);

                if (result.length() != 0) {
                    result.append(",");
                }

                result.append(element);
            }

            return result.toString();
        } else {
            return array;
        }
    }

    private String makeUnicodeCharactersEscaped(String str) {
        StringBuilder b = new StringBuilder();

        for (char c : str.toCharArray()) {
            if ((1024 <= c && c <= 1279) || (1280 <= c && c <= 1327) || (11744 <= c && c <= 11775) || (42560 <= c && c <= 42655)) {
                b.append("\\u").append("0").append(Integer.toHexString(c));
            } else {
                b.append(c);
            }
        }

        return b.toString();
    }

    private void doApiMethod(HttpServletRequest httpRequest, HttpServletResponse httpResponse, HttpMethod httpMethod, String entity, String method) {
        Map<String, String> apiParameters = getApiParameters(httpRequest, httpMethod);
        JSONObject result = api.method(entity, method, apiParameters);
        String response = makeUnicodeCharactersEscaped(result.toString());

        try {

            httpResponse.getWriter().write(response);
        } catch (IOException e) { }

        System.out.println("req:" + httpRequest.getRequestURI() + " with:" + apiParameters);
        System.out.println("resp:" + response);
    }
}
