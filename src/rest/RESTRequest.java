package rest;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.net.HttpURLConnection;

public class RESTRequest implements Runnable
{
    private String body;
    private String url;
    private String method;
    private Map<String, String> additionalHeaders = new HashMap<>();
    private IRESTable callback;
    private RESTResponse response;
    String contentType = "application/x-www-form-urlencoded";

    public RESTResponse Init(JSON body, HttpMethod method, IRESTable callback, String baseUrl, String ... params)
    {
        setBody(body);
        setMethod(method);
        setCallback(callback);
        setUrl(baseUrl, params);
        this.response = new RESTResponse();
        this.response.SetRequestType(method);
        return this.response;
    }
    public void setBody(JSON json) {
        this.body = json.GetValue();
    }

    public void setUrl(String base, String ... params) {
        this.url = base;
        for (String s:params)
        {
            this.url+=(s+"/");
        }
    }

    public void setMethod(HttpMethod method) {
        this.method = method.GetMethod();
    }
    public void setContentType(String contentType)
    {
        this.contentType = contentType;
    }
    public void setCallback(IRESTable callback) {
        this.callback = callback;
    }
    public void AddHeader(String key, String value)
    {
        this.additionalHeaders.put(key, value);
    }

    @Override
    public void run() {
        String urlParameters = body;
        String targetURL = this.url;
        HttpURLConnection connection = null;
        try {
            //Create connection
            URL url = new URL(targetURL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);
            for (Map.Entry<String,String> entry:additionalHeaders.entrySet()) {
                connection.setRequestProperty(entry.getKey(), entry.getValue());
            }
            if(!method.equals(HttpMethod.GET.GetMethod()))
            {
                connection.setRequestProperty("Content-Type",
                        contentType);

                connection.setRequestProperty("Content-Length",
                        Integer.toString(urlParameters.getBytes().length));
                connection.setRequestProperty("Content-Language", "en-US");

                connection.setUseCaches(false);
                connection.setDoOutput(true);

                //Send request
                DataOutputStream wr = new DataOutputStream (
                        connection.getOutputStream());
                wr.writeBytes(urlParameters);
                wr.close();
            }

            //Get Response
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            StringBuilder response = new StringBuilder(); // or StringBuffer if Java version 5+
            String line;
            while ((line = rd.readLine()) != null) {
                response.append(line);
                response.append(System.getProperty("line.separator"));
            }
            if(response.length() > 0) {
                response.deleteCharAt(response.length() - 1);
            }
            rd.close();
            //return response.toString();
            String result = response.toString();
            this.response.SetHeaders(connection.getHeaderFields());
            this.response.SetResponse(result);
            this.callback.Success(this.response);
        } catch (Exception e) {
            e.printStackTrace();
            //return null;
            this.response.SetResponse(e.getMessage());
            callback.Error(this.response);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
