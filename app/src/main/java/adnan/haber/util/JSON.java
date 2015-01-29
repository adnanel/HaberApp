package adnan.haber.util;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by Adnan on 24.1.2015..
 */
public class JSON {
    public static UpdateInfo getUpdateInfo() throws Exception {
        try {
            JSONObject object = new JSONObject(downloadJSON("https://dl.dropboxusercontent.com/u/67764496/haber/version.json"));

            UpdateInfo info = new UpdateInfo();
            info.changeLog    = object.getString("changeLog");
            info.version      = object.getString("version");
            info.apkUrl       = object.getString("apkURL");
            info.changeLogUrl = object.getString("changeLogUrl");

            return info;
        } catch ( Exception e ) {
            Debug.log(e);
            return null;
        }
    }


    public static String downloadJSON(String url) throws Exception {
        StringBuilder builder = new StringBuilder();
        HttpClient client = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader("Authorization", "Client-ID f076662765695cf");
        try {
            HttpResponse response = client.execute(httpGet);
            StatusLine statusLine = response.getStatusLine();
            int statusCode = statusLine.getStatusCode();
            if (statusCode == 200) {
                HttpEntity entity = response.getEntity();
                InputStream content = entity.getContent();
                BufferedReader reader = new BufferedReader(new InputStreamReader(content));
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }
            } else {
                Debug.log("JSON: Failed to download file");
                if ( statusCode == 401 ) throw new Exception("Not allowed!");

            }
        } catch (ClientProtocolException e) {
            Debug.log("JSON: " + e.toString());
            e.printStackTrace();
        } catch (IOException e) {
            Debug.log("JSON: " + e.toString());
            e.printStackTrace();
        }

        return builder.toString();
    }

    public static class UpdateInfo {
        public String changeLog;
        public String version;
        public String apkUrl;
        public String changeLogUrl;
    }
}
